::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAnk
::fBw5plQjdG8=
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdF+5
::cxAkpRVqdFKZSjk=
::cBs/ulQjdF+5
::ZR41oxFsdFKZSDk=
::eBoioBt6dFKZSDk=
::cRo6pxp7LAbNWATEpCI=
::egkzugNsPRvcWATEpCI=
::dAsiuh18IRvcCxnZtBJQ
::cRYluBh/LU+EWAnk
::YxY4rhs+aU+IeA==
::cxY6rQJ7JhzQF1fEqQJgZk40
::ZQ05rAF9IBncCkqN+0xwdVs0
::ZQ05rAF9IAHYFVzEqQJQ
::eg0/rx1wNQPfEVWB+kM9LVsJDGQ=
::fBEirQZwNQPfEVWB+kM9LVsJDGQ=
::cRolqwZ3JBvQF1fEqQITOh5VWAGGfGmjRrEd5ur+4eiG4l0cQOE3OInUyPSYJecS+QX0Z5lthCg6
::dhA7uBVwLU+EWDk=
::YQ03rBFzNR3SWATElA==
::dhAmsQZ3MwfNWATElA==
::ZQ0/vhVqMQ3MEVWAtB9wSA==
::Zg8zqx1/OA3MEVWAtB9wSA==
::dhA7pRFwIByZRRnk
::Zh4grVQjdCyDJGyX8VAjFD9VQh2+GG6pDaET+NTr7v6EqkgPGucnfe8=
::YB416Ek+ZW8=
::
::
::978f952a14a936cc963da21a135fa983
@echo off
title VMagix vPatch v0.5

REM ============================================
REM Define ANSI Escape Codes for Colors
REM ============================================
:: Enable ANSI escape sequences
:: This is required for Windows 10 and later
:: No action needed as ANSI is enabled by default in modern Windows

:: Define escape character
for /f "delims=" %%a in ('echo prompt $E^| cmd') do set "ESC=%%a"

:: Define color codes
set "RESET=%ESC%[0m"
set "BOLD=%ESC%[1m"
set "UNDERLINE=%ESC%[4m"
set "INVERSE=%ESC%[7m"

:: Foreground Colors
set "FG_BLACK=%ESC%[30m"
set "FG_RED=%ESC%[31m"
set "FG_GREEN=%ESC%[32m"
set "FG_YELLOW=%ESC%[33m"
set "FG_BLUE=%ESC%[34m"
set "FG_MAGENTA=%ESC%[35m"
set "FG_CYAN=%ESC%[36m"
set "FG_WHITE=%ESC%[37m"

:: Strong Foreground Colors
set "FG_STRONG_RED=%ESC%[91m"
set "FG_STRONG_GREEN=%ESC%[92m"
set "FG_STRONG_YELLOW=%ESC%[93m"
set "FG_STRONG_BLUE=%ESC%[94m"
set "FG_STRONG_MAGENTA=%ESC%[95m"
set "FG_STRONG_CYAN=%ESC%[96m"
set "FG_STRONG_WHITE=%ESC%[97m"

REM ============================================
REM Display ASCII Art
REM ============================================
echo %FG_STRONG_WHITE%
echo =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
echo             Magix vPatch v0.5
echo        Compatible with Vegas Pro 22 
echo            Created by danbenba
echo =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
echo %RESET%

REM ============================================
REM Check for Administrative Privileges
REM ============================================
echo %FG_STRONG_MAGENTA%[+] Checking for administrative privileges...%RESET%
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [+] %FG_YELLOW%Requesting administrative privileges...%RESET%
    powershell -Command "Start-Process '%~f0' -Verb runAs"
    exit /b
)
echo %FG_STRONG_MAGENTA%[+] Run as Administrator.%RESET%

echo.
REM ============================================
REM Extract patch.zip to the target directory
REM ============================================
echo %FG_CYAN%=-=-=-=-=-=-=-=-=-=-=-=Patching=-=-=-=-=-=-=-=-=-=-=-=%RESET%
echo.
echo [+] Extracting files...
powershell -Command "Expand-Archive -Path 'C:\Program Files\VEGAS\VEGAS Pro 22.0\patch.zip' -DestinationPath 'C:\Program Files\VEGAS\VEGAS Pro 22.0' -Force"

if %errorlevel% neq 0 (
    echo [+] %FG_RED%An error occurred while extracting patch.zip.%RESET%
    pause
    exit /b 1
) else (
    echo [+] %FG_GREEN%Done%RESET%
    echo.
)

REM Apply registry changes and configure settings
echo [+] Applying registry changes...
reg add "HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Image File Execution Options" /v "DevOverrideEnable" /t REG_DWORD /d 1 /f

if %errorlevel% neq 0 (
    echo [+] %FG_RED%Failed to apply registry changes.%RESET%
) else (
    echo [+] %FG_GREEN%Done%RESET%
    echo.
)

REM Modify configuration files
echo [+] Registering patcher on Vegas Pro 22...
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\VEGAS_Pro_22\installation.ini" "Serial" "string" "P3-64979-27462-07906-32757-21318-38872"
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\VEGAS_Pro_22\installation.ini" "VersionUnlock" "NumberOfStarts" "0"
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\VEGAS_Pro_22\installation.ini" "VersionUnlock" "DontShowNagBox" "1"
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\VEGAS_Pro_22\installation.ini" "VersionUnlock" "IsRegisteredUser" "1"
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\VEGAS_Pro_22\installation.ini" "VersionUnlock" "UserEMail" "uBusHTShXjdIakxgck01PRO5nuh8YfF4BDS17GWS/So3BnxxO66uwQ3meU0PEMwM"

"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\DVD_Architect_Pro_7\installation.ini" "Serial" "string" "P3-77020-98979-63411-51090-66867-08191"
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\DVD_Architect_Pro_7\installation.ini" "VersionUnlock" "NumberOfStarts" "0"
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\DVD_Architect_Pro_7\installation.ini" "VersionUnlock" "DontShowNagBox" "1"
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\DVD_Architect_Pro_7\installation.ini" "VersionUnlock" "IsRegisteredUser" "1"
"C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe" inisetval "c:\ProgramData\VEGAS\DVD_Architect_Pro_7\installation.ini" "VersionUnlock" "UserEMail" "uBusHTShXjdIakxgck01PRO5nuh8YfF4BDS17GWS/So3BnxxO66uwQ3meU0PEMwM"

if %errorlevel% neq 0 (
    echo [+] %FG_RED%Failed to modify installation.ini.%RESET%
    pause
    exit /b 1
) else (
    echo [+] %FG_GREEN%Done%RESET%
    echo.
)

REM Register patcher for one-time execution
echo [+] Finishing up...
reg add "HKLM\Software\Microsoft\Windows\CurrentVersion\RunOnce" /v Patcher /t REG_SZ /d "C:\Program Files\VEGAS\VEGAS Pro 22.0\vpatch.exe" /f

if %errorlevel% neq 0 (
    echo [+] %FG_RED%Failed to register the patcher.%RESET%
) else (
    echo [+] %FG_GREEN%Done%RESET%
    echo.
    echo %FG_GREEN%Vegas Pro 22 is Patched !%RESET%
)

echo.
echo =-=-=-=-=-=-=-=-=-=-=-==-=-=-=-=-=-=-=-=-=-=-=-=-=-=
REM Delete temporary files
echo.
echo [+] Cleaning up temporary files...
del "C:\Program Files\VEGAS\VEGAS Pro 22.0\patch.zip"
del "C:\Program Files\VEGAS\VEGAS Pro 22.0\nircmd.exe"

if %errorlevel% neq 0 (
    echo [+] %FG_RED%Failed to delete temporary files.%RESET%
) else (
    echo [+] %FG_GREEN%Done%RESET%
    echo.
)

REM Prompt for reboot
echo %FG_YELLOW%Reboot Required.%RESET%
:ask_restart
echo Please type 'y' to restart or 'n' to exit.
set /p RESTART=Do you want to restart the computer now? (y/n) : 
if /i "%RESTART%"=="y" (
    echo [+] %FG_GREEN%Restarting the computer...%RESET%
    shutdown /r /t 0
) else if /i "%RESTART%"=="n" (
    echo [+] %FG_YELLOW%Restart canceled. Exiting...%RESET%
    exit /b 0
) else (
    echo %FG_RED%Invalid input. Please enter 'y' or 'n'.%RESET%
    echo.
    goto ask_restart
)

exit /b 0
