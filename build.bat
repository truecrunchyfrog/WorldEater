@echo off

if "%1"=="" (
    echo Usage: build.bat JDK_PATH
    exit /b 1
)

set JAVA_HOME=%1

REM Create a temporary directory
set "TEMP_DIR=.\tempdir-%RANDOM%"
mkdir "%TEMP_DIR%"

REM Clone the git repository
git clone --branch main https://github.com/javaveryhot/WorldEater.git "%TEMP_DIR%\WorldEater"

REM Build the jar file
cd "%TEMP_DIR%\WorldEater"

mvn -Dmaven.compiler.fork=true -Dmaven.compiler.executable=%JAVA_HOME%\bin\javac.exe clean package && cd "%~dp0" && mkdir "plugins" 2>nul && move "%TEMP_DIR%\WorldEater\target\WorldEater*.jar" "%~dp0plugins" && rd /s /q "%TEMP_DIR%"