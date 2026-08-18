@echo off
REM ScreenTime Monitor Windows jpackage packaging script
set APP_NAME=ScreenTime Monitor
set APP_VERSION=1.0.0
set MAIN_JAR=screentime-monitor-1.0.0-SNAPSHOT.jar
set MAIN_CLASS=com.screentime.Launcher
set INPUT_DIR=target
set OUTPUT_DIR=dist\win

echo === Building ScreenTime Monitor for Windows (.msi / .exe) ===

REM 1. Clean & package fat JAR
call mvn clean package -DskipTests

REM 2. Prepare output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM 3. Invoke jpackage (produces MSI installer)
jpackage ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest "%OUTPUT_DIR%" ^
  --type msi ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --vendor "Sadeesha Jayaweera" ^
  --copyright "Copyright (c) 2026 ScreenTime Monitor" ^
  --description "Cross-Platform Desktop Screen Time Monitor & Health Companion"

echo === Packaging complete! Installer created at: %OUTPUT_DIR% ===
