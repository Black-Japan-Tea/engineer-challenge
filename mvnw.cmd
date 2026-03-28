@REM Maven Wrapper for Windows
@echo off
setlocal
set MAVEN_PROJECTBASEDIR=%~dp0
if not "%MAVEN_PROJECTBASEDIR%"=="" cd /d "%MAVEN_PROJECTBASEDIR%"

set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if exist "%JAVA_HOME%\bin\java.exe" (
  set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
) else (
  set JAVA_CMD=java
)

%JAVA_CMD% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%." %WRAPPER_LAUNCHER% %*
exit /b %ERRORLEVEL%
