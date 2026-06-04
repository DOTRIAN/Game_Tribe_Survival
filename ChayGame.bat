@echo off
setlocal
cd /d "%~dp0"

set "JAVA21_HOME="

if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /c:"21." >nul
    if not errorlevel 1 set "JAVA21_HOME=%JAVA_HOME%"
)

if not defined JAVA21_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin\java.exe" set "JAVA21_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
)

:java_found
if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

if not defined JAVA21_HOME (
    echo Khong tim thay JDK 21.
    echo.
    echo Project nay can Java 21 vi pom.xml dang dung release 21.
    echo Hay cai JDK 21 roi chay lai file nay.
    echo Goi y duong dan thuong gap:
    echo   C:\Program Files\Java\jdk-21
    echo   C:\Program Files\Amazon Corretto\jdk21...
    echo   C:\Program Files\Eclipse Adoptium\jdk-21...
    echo.
    pause
    exit /b 1
)

"%JAVA21_HOME%\bin\java.exe" -version 2>&1 | findstr /c:"21." >nul
if errorlevel 1 (
    echo JDK 21 da duoc tim thay nhung khong chay dung.
    echo Kiem tra lai cai dat Java 21.
    echo.
    pause
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo Khong tim thay Maven trong PATH.
    echo Cai Maven hoac them lenh mvn vao PATH roi chay lai.
    echo.
    pause
    exit /b 1
)

echo Dang mo game...
call mvn javafx:run
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo Game dung voi ma loi %EXIT_CODE%.
    pause
)

exit /b %EXIT_CODE%
