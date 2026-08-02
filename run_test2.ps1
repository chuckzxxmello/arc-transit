$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.11"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:POSTGRES_PASSWORD="ArcTransit123"
$env:ARC_DEV_ADMIN_PASSWORD="ArcTransit123"
.\mvnw.cmd test -Dtest=WebSecurityIT
