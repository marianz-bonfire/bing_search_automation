@echo off
setlocal

set FLUTTER=D:\Android\Flutter\flutter_windows_3.29.2\bin\flutter.bat

:: Go to project folder
cd /d "C:\Users\Bonfire\Projects\Flutter\bing_search_automation\example"

echo --------------------------------------
echo Cleaning project...
call "%FLUTTER%" clean -v

echo --------------------------------------
echo Getting pub packages...
call "%FLUTTER%" pub get -v

::echo --------------------------------------
::echo Upgrading packages...
:: call "%FLUTTER%" pub upgrade

echo --------------------------------------
echo Done!
::pause
