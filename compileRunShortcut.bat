@echo off
REM This script must be run in a Windows terminal, not bash or WSL.
REM Run in PowerShell with: .\compileRunShortcut.bat
REM Run in Command Prompt with: compileRunShortcut.bat

setlocal EnableDelayedExpansion

echo Compiling...
if exist classes rmdir /s /q classes
mkdir classes

set "FILES="
for /r src %%f in (*.java) do (
    set "FILES=!FILES! "%%f""
)

javac -d classes !FILES!
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

echo Starting Splendor UI chooser...
java -cp classes Main

endlocal