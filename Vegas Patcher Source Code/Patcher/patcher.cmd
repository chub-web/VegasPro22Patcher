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
title Vegas Pro 22 Patcher [By danbenba]

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
echo               vPatch v0.5
echo             For Vegas Pro 22 
echo =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
echo.
echo %RESET%

REM ============================================
REM Check for Administrative Privileges
REM ============================================
:: Check if the script is running as Administrator
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo %FG_YELLOW%Requesting administrative privileges...%RESET%
    powershell -Command "Start-Process '%~f0' -Verb runAs"
    exit /b
)

REM ============================================
REM Extract patch.zip to the target directory
REM ============================================
echo.
echo %FG_CYAN%Patching Vegas Pro 22...%RESET%
REM Use PowerShell to reliably extract the ZIP file
powershell -Command "Expand-Archive -Path 'C:\Program Files\VEGAS\VEGAS Pro 22.0\patch.zip' -DestinationPath 'C:\Program Files\VEGAS\VEGAS Pro 22.0' -Force"

if %errorlevel% neq 0 (
    echo.
    echo %FG_RED%An error occurred while patching Vegas.%RESET%
    pause
    exit /b 1
) else (
    echo %FG_GREEN%Vegas Pro patched successfully.%RESET%
)

REM ============================================
REM Add patcher.exe to RunOnce for a one-time launch after restart
REM ============================================
echo %FG_CYAN%Registering patch in Vegas Pro...%RESET%
reg add "HKLM\Software\Microsoft\Windows\CurrentVersion\RunOnce" /v Patcher /t REG_SZ /d "C:\Program Files\VEGAS\VEGAS Pro 22.0\patch.exe" /f

if %errorlevel% neq 0 (
    echo.
    echo %FG_RED%Failed to register the patch in Vegas Pro.%RESET%
) else (
    echo %FG_GREEN%Patch registered successfully.%RESET%
)

REM ============================================
REM Delete patch.zip after extraction
REM ============================================
echo.
echo %FG_CYAN%Deleting temporary files...%RESET%
del "C:\Program Files\VEGAS\VEGAS Pro 22.0\patch.zip"

if %errorlevel% neq 0 (
    echo.
    echo %FG_RED%Failed to delete temporary files.%RESET%
) else (
    echo %FG_GREEN%Temporary files deleted successfully.%RESET%
)

echo %FG_GREEN%CLEANING SUCCESSFUL%RESET%

REM ============================================
REM Prompt for Confirmation to Restart the Computer
REM ============================================
:ask_restart
echo.
echo %FG_YELLOW%Reboot Required.%RESET%
set /p RESTART=Do you want to restart the computer now? (y/n) : 

REM Trim spaces and take up to the first 10 characters
set "RESTART=%RESTART:~0,10%"
for /f "tokens=* delims= " %%a in ("%RESTART%") do set "RESTART=%%a"

REM Check the user's response
if /i "%RESTART%"=="Y" (
    echo %FG_GREEN%Restarting the computer...%RESET%
    shutdown /r /t 0
    goto end
) else if /i "%RESTART%"=="O" (
    echo %FG_GREEN%Restarting the computer...%RESET%
    shutdown /r /t 0
    goto end
) else if /i "%RESTART%"=="OUI" (
    echo %FG_GREEN%Restarting the computer...%RESET%
    shutdown /r /t 0
    goto end
) else if /i "%RESTART%"=="N" (
    echo %FG_YELLOW%Restart canceled.%RESET%
    goto end
) else if /i "%RESTART%"=="NO" (
    echo %FG_YELLOW%Restart canceled.%RESET%
    goto end
) else if /i "%RESTART%"=="NON" (
    echo %FG_YELLOW%Restart canceled.%RESET%
    goto end
) else if /i "%RESTART%"=="y" (
    echo %FG_GREEN%Restarting the computer...%RESET%
    shutdown /r /t 0
    goto end
) else if /i "%RESTART%"=="yes" (
    echo %FG_GREEN%Restarting the computer...%RESET%
    shutdown /r /t 0
    goto end
) else if /i "%RESTART%"=="oui" (
    echo %FG_GREEN%Restarting the computer...%RESET%
    shutdown /r /t 0
    goto end
) else if /i "%RESTART%"=="o" (
    echo %FG_GREEN%Restarting the computer...%RESET%
    shutdown /r /t 0
    goto end
) else if /i "%RESTART%"=="n" (
    echo %FG_YELLOW%Restart canceled.%RESET%
    goto end
) else if /i "%RESTART%"=="no" (
    echo %FG_YELLOW%Restart canceled.%RESET%
    goto end
) else if /i "%RESTART%"=="non" (
    echo %FG_YELLOW%Restart canceled.%RESET%
    goto end
) else (
    echo %FG_RED%Invalid response. Please type 'y' for restart or 'no' to exit without rebooting.%RESET%
    goto ask_restart
)

:end
exit /b 0
