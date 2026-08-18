#!/usr/bin/env bash
set -e

# ScreenTime Monitor Linux jpackage packaging script (.deb / .rpm)
APP_NAME="screentime-monitor"
APP_VERSION="1.0.0"
MAIN_JAR="screentime-monitor-1.0.0-SNAPSHOT.jar"
MAIN_CLASS="com.screentime.Launcher"
INPUT_DIR="target"
OUTPUT_DIR="dist/linux"

echo "=== Building ScreenTime Monitor for Linux (.deb) ==="

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
  --type deb \
  --icon src/main/resources/com/screentime/ui/assets/icon.png \
  --linux-shortcut \
  --linux-menu-group "Utility;Health;" \
  --linux-app-category "Utility" \
  --vendor "Sadeesha Jayaweera" \
  --copyright "Copyright © 2026 ScreenTime Monitor" \
  --description "Cross-Platform Desktop Screen Time Monitor & Health Companion"

echo "=== Packaging complete! Installer created at: ${OUTPUT_DIR} ==="
