param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\17\bin',
    [string]$JavaHome = 'C:\Program Files\Java\jdk-21.0.11',
    [string[]]$MavenArguments = @('verify'),
    [string]$ArtifactDirectory = '.verification',
    [switch]$SmokeTest
)

$ErrorActionPreference = 'Stop'
if (@($MavenArguments | Where-Object { $_ -match '(^|:)clean($|@)' }).Count -gt 0) {
    throw 'Run clean before this script. The isolated PostgreSQL cluster is retained in the verification artifacts directory.'
}
$arcRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$arcArtifacts = [IO.Path]::GetFullPath((Join-Path $arcRoot $ArtifactDirectory))
[void][IO.Directory]::CreateDirectory($arcArtifacts)
$arcRun = Join-Path $arcArtifacts ('postgres-check-' + [Guid]::NewGuid().ToString('N'))
$arcData = Join-Path $arcRun 'data'
$arcPasswordFile = Join-Path $arcRun 'password.txt'
$arcVariables = @('JAVA_HOME', 'PATH', 'SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME',
    'POSTGRES_PASSWORD', 'ARC_SECURITY_ADMIN_USERNAME', 'ARC_SECURITY_ADMIN_PASSWORD',
    'SPRING_PROFILES_ACTIVE', 'ARC_TEST_POSTGRES')
$arcOriginal = @{}
foreach ($arcName in $arcVariables) {
    $arcOriginal[$arcName] = [Environment]::GetEnvironmentVariable($arcName, 'Process')
}
$arcStarted = $false
$arcApplication = $null
$arcExit = 1
try {
    if (-not (Test-Path -LiteralPath (Join-Path $PostgresBin 'initdb.exe'))) {
        throw 'PostgreSQL binaries were not found. Supply -PostgresBin.'
    }
    New-Item -ItemType Directory -Path $arcRun | Out-Null
    $arcPassword = 'ArcTest!' + [Guid]::NewGuid().ToString('N')
    [IO.File]::WriteAllText($arcPasswordFile, $arcPassword)
    & (Join-Path $PostgresBin 'initdb.exe') -D $arcData -U arc_test `
        --auth-host=scram-sha-256 --auth-local=scram-sha-256 `
        --pwfile=$arcPasswordFile --encoding=UTF8 --locale=C *> (Join-Path $arcRun 'initdb.log')
    if ($LASTEXITCODE -ne 0) {
        throw "initdb failed; inspect $arcRun\initdb.log"
    }
    Remove-Item -LiteralPath $arcPasswordFile
    $arcListener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $arcListener.Start()
    $arcPort = $arcListener.LocalEndpoint.Port
    $arcListener.Stop()
    # Wait only for pg_ctl; PowerShell's native pipeline can wait on the persistent server process too.
    $arcPgStart = Start-Process -FilePath (Join-Path $PostgresBin 'pg_ctl.exe') `
        -ArgumentList @('-D', ('"' + $arcData + '"'), '-w', '-t', '30', '-l',
            ('"' + (Join-Path $arcRun 'postgres.log') + '"'), '-o', ('"-h 127.0.0.1 -p ' + $arcPort + '"'), 'start') `
        -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $arcRun 'pg-start.log') `
        -RedirectStandardError (Join-Path $arcRun 'pg-start-error.log')
    if (-not $arcPgStart.WaitForExit(45000) -or $arcPgStart.ExitCode -ne 0) {
        throw "PostgreSQL startup failed; inspect $arcRun\postgres.log"
    }
    $arcStarted = $true
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;" + $arcOriginal['PATH']
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$arcPort/postgres"
    $env:SPRING_DATASOURCE_USERNAME = 'arc_test'
    $env:POSTGRES_PASSWORD = $arcPassword
    $env:ARC_SECURITY_ADMIN_USERNAME = 'admin'
    $env:ARC_SECURITY_ADMIN_PASSWORD = $arcPassword
    $env:SPRING_PROFILES_ACTIVE = 'test'
    $env:ARC_TEST_POSTGRES = 'true'
    Push-Location $arcRoot
    try {
        & .\mvnw.cmd @MavenArguments *> (Join-Path $arcRun 'maven.log')
        $arcExit = $LASTEXITCODE
        Get-Content -LiteralPath (Join-Path $arcRun 'maven.log') -Tail 70
        if ($arcExit -eq 0 -and $SmokeTest) {
            $arcJar = Join-Path $arcRoot 'target\arc-transit-system-0.0.1-SNAPSHOT.jar'
            if (-not (Test-Path -LiteralPath $arcJar)) {
                throw 'Build the executable JAR before requesting -SmokeTest.'
            }
            $arcRunJar = Join-Path $arcRun 'arc-transit-system.jar'
            Copy-Item -LiteralPath $arcJar -Destination $arcRunJar
            $arcListener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
            $arcListener.Start()
            $arcWebPort = $arcListener.LocalEndpoint.Port
            $arcListener.Stop()
            $arcApplication = Start-Process -FilePath (Join-Path $JavaHome 'bin\java.exe') `
                -ArgumentList @('-jar', ('"' + $arcRunJar + '"'), "--server.port=$arcWebPort", '--server.address=127.0.0.1',
                    '--vaadin.productionMode=true', '--vaadin.launch-browser=false') `
                -WorkingDirectory $arcRoot -WindowStyle Hidden -PassThru `
                -RedirectStandardOutput (Join-Path $arcRun 'application.log') `
                -RedirectStandardError (Join-Path $arcRun 'application-error.log')
            $arcDeadline = [DateTime]::UtcNow.AddSeconds(90)
            $arcResponse = $null
            while ([DateTime]::UtcNow -lt $arcDeadline -and -not $arcApplication.HasExited) {
                try {
                    $arcResponse = Invoke-WebRequest -Uri "http://127.0.0.1:$arcWebPort/login" -UseBasicParsing -TimeoutSec 5
                    if ($arcResponse.StatusCode -eq 200) { break }
                } catch {
                    Start-Sleep -Seconds 1
                }
            }
            if ($null -eq $arcResponse -or $arcResponse.StatusCode -ne 200) {
                throw "Application smoke test failed; inspect $arcRun\application.log"
            }
            Write-Output "VERIFIED: executable JAR started and GET /login returned HTTP 200 on loopback port $arcWebPort."
        }
    } finally {
        Pop-Location
    }
} finally {
    if ($null -ne $arcApplication -and -not $arcApplication.HasExited) {
        Stop-Process -Id $arcApplication.Id
    }
    if ($arcStarted) {
        & (Join-Path $PostgresBin 'pg_ctl.exe') -D $arcData -w -t 30 stop -m fast
    }
    if (Test-Path -LiteralPath $arcPasswordFile) {
        Remove-Item -LiteralPath $arcPasswordFile
    }
    foreach ($arcName in $arcVariables) {
        [Environment]::SetEnvironmentVariable($arcName, $arcOriginal[$arcName], 'Process')
    }
    Write-Output "Verification artifacts: $arcRun"
}
exit $arcExit
