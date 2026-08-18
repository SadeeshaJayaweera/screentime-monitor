#!/usr/bin/env bash
set -e

# ScreenTime Monitor macOS jpackage packaging script
APP_NAME="ScreenTime Monitor"
APP_VERSION="1.0.0"
MAIN_JAR="screentime-monitor-1.0.0-SNAPSHOT.jar"
MAIN_CLASS="com.screentime.Launcher"
INPUT_DIR="target"
OUTPUT_DIR="dist/mac"

echo "=== Building ScreenTime Monitor for macOS (.dmg) ==="

# 1. Clean & package fat JAR
mvn clean package -DskipTests

# 2. Prepare output directory
mkdir -p "${OUTPUT_DIR}"

# 3. Invoke jpackage
jpackage \
  --name "${APP_NAME}" \
  --app-version "${APP_VERSION}" \
  --input "${INPUT_DIR}" \
  --main-jar "${MAIN_JAR}" \
  --main-class "${MAIN_CLASS}" \
  --dest "${OUTPUT_DIR}" \
  --type dmg \
  --icon src/main/resources/com/screentime/ui/assets/icon.png \
  --java-options "-Dprism.lcdtext=false" \
  --vendor "Sadeesha Jayaweera" \
  --copyright "Copyright © 2026 ScreenTime Monitor" \
  --description "Cross-Platform Desktop Screen Time Monitor & Health Companion" \
  --mac-package-name "ScreenTime Monitor" \
  --mac-package-identifier "com.screentime.monitor"

echo "=== Packaging complete! Installer created at: ${OUTPUT_DIR} ==="
