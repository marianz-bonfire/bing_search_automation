@echo off
setlocal enabledelayedexpansion

:: ==============================
:: CONFIGURATION
:: ==============================
set "ROOT_DIR=%~dp0"
set "RELEASE_DIR=%ROOT_DIR%release"
if not exist "%RELEASE_DIR%" mkdir "%RELEASE_DIR%"

:: Flutter & Dart SDK configuration
set "FLUTTER_HOME=D:\Android\Flutter\flutter_windows_3.29.2\bin"
set "FLUTTER=%FLUTTER_HOME%\flutter.bat"
set "DART=%FLUTTER_HOME%\cache\dart-sdk\bin\dart.exe"

:: Build output paths
set "APK_PATH=build\app\outputs\flutter-apk\app-release.apk"
set "WIN_PATH=build\windows\x64\runner\Release"

:: Inno Setup compiler
set "INNO=C:\Program Files (x86)\Inno Setup 6\ISCC.exe"

:: ==============================
:: HELP / USAGE
:: ==============================
if "%~1"=="" (
    echo.
    echo "Usage: build.bat [debug^|release^|reset] [android^|windows^|both]"
    echo.
    echo "Examples:"
    echo "  build.bat debug android    :: Build a debug APK for Android"
    echo "  build.bat release windows  :: Build a release EXE for Windows (runs Inno Setup)"
    echo "  build.bat release both     :: Build release builds for both Android and Windows"
    echo "  build.bat reset            :: Reset build number in pubspec.yaml"
    echo.
    exit /b 1
)

:: ==============================
:: RESET BUILD NUMBER
:: ==============================
if /i "%~1"=="reset" (
    "%DART%" run tool/increment_build.dart --reset
    exit /b 0
)

:: ==============================
:: INCREMENT BUILD NUMBER
:: ==============================
"%DART%" run tool/increment_build.dart

:: Extract version and app name from pubspec.yaml
for /f "tokens=2 delims=:" %%v in ('findstr /b "version:" pubspec.yaml') do set "APP_VERSION=%%v"
for /f "tokens=* delims= " %%a in ("%APP_VERSION%") do set "APP_VERSION=%%a"
set "APP_VERSION=%APP_VERSION:"=%"

for /f "tokens=2 delims=:" %%n in ('findstr /b "name:" pubspec.yaml') do set "APP_NAME=%%n"
for /f "tokens=* delims= " %%a in ("%APP_NAME%") do set "APP_NAME=%%a"
set "APP_NAME=%APP_NAME:"=%"

:: Timestamp for filenames
::for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /value') do set ldt=%%i
::set "DATESTAMP=%ldt:~0,8%.%ldt:~8,6%"

:: Safe timestamp without WMIC
for /f %%i in ('powershell -NoLogo -NoProfile -Command "(Get-Date).ToString(\"yyyyMMdd.HHmmss\")"') do set DATESTAMP=%%i
::echo %DATESTAMP%


:: Filename pattern
set "APK_NAME=%APP_NAME%(%APP_VERSION%)_%DATESTAMP%.apk"
set "EXE_NAME=%APP_NAME%(%APP_VERSION%)_%DATESTAMP%.exe"

:: ==============================
:: BUILD TYPE & TARGET
:: ==============================
set "BUILD_TYPE=%~1"
set "TARGET=%~2"

if /i "%BUILD_TYPE%"=="debug" (
    set "BUILD_FLAG=--debug"
) else if /i "%BUILD_TYPE%"=="release" (
    set "BUILD_FLAG=--release"
) else (
    echo "Invalid build type. Use: debug, release, or reset"
    exit /b 1
)

if "%TARGET%"=="" set "TARGET=both"

:: ==============================
:: BUILD LOGIC
:: ==============================

if /i "%TARGET%"=="android" (
    echo "🏗️ Building Android APK (%BUILD_TYPE%)..."
    "%FLUTTER%" build apk %BUILD_FLAG%

    if /i "%BUILD_TYPE%"=="release" (
        if exist "%APK_PATH%" (
            echo "📦 Copying APK to release folder..."
            copy "%APK_PATH%" "%RELEASE_DIR%\%APK_NAME%" >nul
            echo "✅ APK copied → %RELEASE_DIR%\%APK_NAME%"
        ) else (
            echo "⚠️ APK not found at %APK_PATH%"
        )
    )
)

if /i "%TARGET%"=="windows" (
    echo "🏗️ Building Windows app (%BUILD_TYPE%)..."
    "%FLUTTER%" build windows %BUILD_FLAG%

    if /i "%BUILD_TYPE%"=="release" (
        echo "📦 Running Inno Setup..."
        "%INNO%" setup.iss

        :: Move generated installer (.exe) into release folder
        for %%f in ("%ROOT_DIR%%APP_NAME%(*).exe") do (
            move "%%f" "%RELEASE_DIR%\%EXE_NAME%" >nul 2>nul
        )
        echo "✅ Windows installer copied → %RELEASE_DIR%\%EXE_NAME%"
    )
)

if /i "%TARGET%"=="both" (
    echo -e "🏗️ Building both Android and Windows (%BUILD_TYPE%)..."
    "%FLUTTER%" build apk %BUILD_FLAG%
    "%FLUTTER%" build windows %BUILD_FLAG%

    if /i "%BUILD_TYPE%"=="release" (
        if exist "%APK_PATH%" (
            echo "📦 Copying APK to release folder..."
            copy "%APK_PATH%" "%RELEASE_DIR%\%APK_NAME%" >nul
            echo "✅ APK copied → %RELEASE_DIR%\%APK_NAME%"
        )

        echo "📦 Running Inno Setup..."
        "%INNO%" setup.iss

        for %%f in ("%ROOT_DIR%%APP_NAME%(*).exe") do (
            move "%%f" "%RELEASE_DIR%\%EXE_NAME%" >nul 2>nul
        )
        echo "✅ Windows installer copied → %RELEASE_DIR%\%EXE_NAME%"
    )
)

:: ==============================
:: DONE
:: ==============================
echo.
echo "🎉 Build complete → Version %APP_VERSION%"
echo.

if exist "%RELEASE_DIR%" (
    echo "📂 Open release folder:"
    explorer "%RELEASE_DIR%"
)

endlocal
