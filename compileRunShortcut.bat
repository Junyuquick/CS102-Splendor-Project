@REM @echo off
@REM REM This script must be run in a Windows terminal, not bash or WSL.
@REM REM Run in PowerShell with: .\compileRunShortcut.bat
@REM REM Run in Command Prompt with: compileRunShortcut.bat

@REM setlocal EnableDelayedExpansion

@REM echo Compiling...
@REM if exist classes rmdir /s /q classes
@REM mkdir classes

@REM set "FILES="
@REM for /r src %%f in (*.java) do (
@REM     set "FILES=!FILES! "%%f""
@REM )

@REM javac -d classes !FILES!
@REM if errorlevel 1 (
@REM     echo Compilation failed.
@REM     exit /b 1
@REM )

@REM echo Starting Splendor UI chooser...
@REM java -cp classes Main

@REM endlocal

javac -d classes -cp src src\Main.java && java -cp classes Main