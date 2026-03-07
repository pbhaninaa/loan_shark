@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if not exist "%WRAPPER_JAR%" (
  echo Downloading Maven Wrapper...
  powershell -NoProfile -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%' -UseBasicParsing }"
  if not exist "%WRAPPER_JAR%" (
    echo Failed to download Maven Wrapper. Install Maven from https://maven.apache.org and add to PATH, or run the app from your IDE.
    exit /b 1
  )
)

java %MAVEN_OPTS% -jar "%WRAPPER_JAR%" %*
endlocal
