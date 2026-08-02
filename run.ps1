$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.11"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd dependency:build-classpath "-Dmdep.outputFile=cp.txt"
$cp = Get-Content cp.txt
$cp.Split(';') | Where-Object { $_ -match 'vaadin-spring' } | ForEach-Object { jar tf $_ } | Select-String 'VaadinWebSecurity'
