# ScreenTime Monitor ⏱️

**ScreenTime Monitor** is a modern, cross-platform desktop application (Windows, macOS, Linux) built with Java 17, JavaFX, SQLite, and Google Gemini AI that tracks active screen time, monitors application habits, enforces healthy daily limits, and provides ergonomic health suggestions.

---

## 🌟 Key Features

- **Accurate Cross-Platform Tracking**:
  - Automatically identifies foreground applications and window titles across macOS (Cocoa/AppleScript), Windows (User32 JNA), and Linux (X11/xdotool).
  - Global native keyboard/mouse idle detection using JNativeHook with configurable inactivity timeout (default 60s).
  - Atomic midnight day-rollover splitting and 60-second periodic SQLite state snapshotting.

- **Data Privacy & Local SQLite Persistence**:
  - 100% private offline storage (`screentime.db`). No telemetry or personal application history leaves the machine.
  - Self-healing database corruption recovery and schema migrations.

- **Intelligent Limit Enforcement & Notifications**:
  - Configurable daily screen time goals (e.g. 4h, 6h, 8h).
  - Progressive warning thresholds (default 50%, 75%, 90%, 100%) delivering native OS desktop alerts.
  - Temporary extension requests (+15m, +30m, +60m) with daily limits and escalated 5-minute reminder intervals.

- **AI-Powered Health & Ergonomic Suggestions**:
  - Analyzes daily screen time patterns to generate tailored posture checks, eye fatigue tips (20-20-20 rule), and break reminders.
  - Integrates with **Google Gemini 1.5** when an API key is provided, with a robust offline heuristic rules engine fallback when offline.

- **System Tray & Modern JavaFX UI**:
  - Full-featured background system tray presence (`Open Dashboard`, `Pause/Resume Tracking`, `Request Extension...`, `Settings`, `Quit`).
  - Dark-mode dashboard featuring live SVG-style progress rings, historical bar charts, and detailed per-app usage tables.

- **First-Run Onboarding Wizard**:
  - 7-step guided setup on first launch with input validation and instant configuration.

- **System Autostart on Login**:
  - Native launch on system login for macOS (`LaunchAgents` plist), Windows (`Run` Registry), and Linux (`autostart` desktop entry).

---

## 🏗️ Architecture & Package Structure

```
com.screentime
├── ai              # GeminiClient, HealthSuggestionService, HealthAdvisorOutput
├── config          # AppConfig, ConfigManager, AutostartManager
├── core            # TrackingEngine, WindowDetector, IdleDetector, TrackingSession
├── data            # DatabaseManager, UsageDao, AppUsage, DailyUsageSummary
├── notifications   # NotificationService, NotificationLevel
├── restriction     # RestrictionEngine, RestrictionConfig, HardWarningOverlay
├── ui              # MainViewController, ProgressRing, ExtensionDialog, TimeFormatUtils
│   └── onboarding  # OnboardingWizard, OnboardingModel
├── Launcher.java   # Shaded JAR bootstrap entry point
└── Main.java       # JavaFX application lifecycle & tray orchestration
```

---

## 🛠️ Build & Run Instructions

### Prerequisites
- **Java JDK 17+** (e.g. OpenJDK 17, 21, Homebrew, Temurin)
- **Apache Maven 3.8+**

### 1. Build and Run via Maven
```bash
# Clone the repository
git clone https://github.com/SadeeshaJayaweera/screentime-monitor.git
cd screentime-monitor

# Run all unit tests
mvn clean test

# Run the application directly
mvn javafx:run
```

### 2. Package Executable Fat JAR
```bash
mvn clean package
java -jar target/screentime-monitor-1.0.0-SNAPSHOT.jar
```

### 3. CLI Diagnostics Mode
Verify database connectivity, configuration paths, and OS window detector without opening the GUI:
```bash
java -jar target/screentime-monitor-1.0.0-SNAPSHOT.jar --info
```

---

## 📦 Native Installers Packaging (`jpackage`)

Automated packaging scripts are provided in the `scripts/` directory:

### macOS (.dmg installer)
```bash
./scripts/package-mac.sh
# Outputs: dist/mac/ScreenTime Monitor-1.0.0.dmg
```

### Windows (.msi installer)
```cmd
scripts\package-win.bat
:: Outputs: dist\win\ScreenTime Monitor-1.0.0.msi
```

### Linux (.deb package)
```bash
./scripts/package-linux.sh
# Outputs: dist/linux/screentime-monitor_1.0.0_amd64.deb
```

---

## ⚠️ Platform-Specific Caveats & Permissions

### macOS
1. **Accessibility & Input Monitoring**:
   - macOS requires explicit permission for global keyboard/mouse idle detection and frontmost application titles.
   - When prompted, grant permissions under **System Settings** -> **Privacy & Security** -> **Accessibility** and **Input Monitoring**.

### Linux
1. **Window Detection on Wayland vs X11**:
   - Under X11, window detection uses standard X11 protocols / `xdotool`.
   - On Wayland sessions (GNOME/KDE), global window title query is sandboxed by default. Install `xdotool` or enable XWayland compatibility for per-window granularity.
2. **Desktop Notifications**:
   - Ensure a standard notification daemon (`dunst`, `mako`, `notify-osd`, or GNOME Shell) is running.

### Windows
1. **System Tray Visibility**:
   - Windows may place the tray icon in the overflow "Chevron" menu (^). Drag the icon to the taskbar tray for pinned visibility.
2. **User Account Control (UAC)**:
   - Modifying autostart settings uses the current user registry hive (`HKCU`) and does not require administrator elevation.

---

## 🧪 Testing

Refer to [TESTING.md](file:///Users/macboookpro/screentime-monitor/TESTING.md) for step-by-step manual test procedures.

---

## 📄 License
This project is licensed under the Apache 2.0 License.