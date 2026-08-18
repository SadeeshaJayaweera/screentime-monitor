# ScreenTime Monitor — Manual Testing & Verification Guide

This guide details manual test procedures to thoroughly verify tracking accuracy, idle detection, notification rules, extension policies, AI insights, autostart on system login, and resilience to corrupt files.

---

## 1. Tracking Accuracy & Active Window Polling

### Objective
Ensure that the foreground application is polled accurately every 5 seconds and recorded into the local SQLite database.

### Test Steps
1. Launch the application:
   ```bash
   java -jar target/screentime-monitor-1.0.0-SNAPSHOT.jar
   ```
2. Open the **Dashboard** -> **Today** tab.
3. Switch focus to a known application (e.g. `Google Chrome`, `Visual Studio Code`, or `Terminal`) and interact with it for 30 seconds.
4. Switch focus back to ScreenTime Monitor.
5. **Expected Result**:
   - The active application indicator displays the focused app name.
   - The Today active time ring updates by ~30 seconds.
   - The "Top Applications Today" horizontal bar chart displays the tested application with the corresponding duration.

---

## 2. Idle Detection Sensitivity

### Objective
Verify that tracking pauses when user input ceases and resumes immediately upon input.

### Test Steps
1. Navigate to the **Settings** tab.
2. Set **Idle Timeout Threshold** to `10` seconds. Click **💾 Save Settings**.
3. Open the **Today** tab and note the current active seconds.
4. Stop touching the mouse and keyboard for 15 seconds.
5. **Expected Result**:
   - After 10 seconds of no input, active time accumulation halts.
   - Idle time begins accumulating in the persistence layer.
6. Move the mouse or press any key.
7. **Expected Result**:
   - Tracking immediately transitions back to `ACTIVE`.
   - The active time resumes incrementing on the next 5-second tick.

---

## 3. Warning Threshold Notifications

### Objective
Verify desktop notifications fire progressively as daily limit percentages are crossed.

### Test Steps
1. Navigate to **Settings**.
2. Set **Daily Screen Time Limit** to `2` minutes (120 seconds).
3. Ensure **Warning Thresholds** are set to `50, 75, 90, 100`.
4. Enable **Desktop Notifications**. Click **💾 Save Settings**.
5. Keep the screen active for 2 minutes.
6. **Expected Result**:
   - At 1 minute (50%): An informational desktop notification appears: `⏱️ Screen Time Warning (50% Used): You have used 1 minute today. 1 minute remaining...`
   - At 1.5 minutes (75%): A warning notification appears with remaining time.
   - At 1.8 minutes (90%): A warning notification appears.
   - At 2 minutes (100%): A critical notification alerts the user: `🚨 Daily Screen Time Limit Reached`.

---

## 4. Extension Requests & Escalated Reminder Cadence

### Objective
Verify temporary screen time extensions can be granted up to the daily cap and enforce 5-minute reminder escalations.

### Test Steps
1. Right-click the system tray icon (or use the limit warning prompt) and select **Request Extension...**.
2. Choose `+15 Mins` and click **Confirm Extension**.
3. **Expected Result**:
   - Notification confirms: `✅ Screen Time Extended: Extended by 15 minutes (Extension #1 today). New limit: 17 mins.`
   - Active screen time limit increases to 17 minutes.
4. Request additional extensions until exceeding **Max Extensions Allowed Per Day** (e.g. 3).
5. **Expected Result**:
   - Further requests are blocked with message: `Daily extension limit reached (maximum 3 extensions per day).`
6. When remaining extension time elapses, recurring reminders notify the user every 5 minutes.

---

## 5. AI Wellness Insights (Live & Offline Fallback)

### Objective
Verify AI insights generate meaningful ergonomic and posture suggestions under both online and offline/keyless conditions.

### Test Steps: Offline Heuristic Mode
1. In **Settings**, ensure the **Gemini API Key** field is empty and **Enable AI Health Suggestions** is checked.
2. Switch to the **AI Insights** tab and click **🔄 Refresh Insights**.
3. **Expected Result**:
   - AI status badge displays: `Offline Rules Engine (Free Tier)`.
   - Comprehensive ergonomic guidelines, eye-strain warnings (20-20-20 rule), and posture suggestions are rendered without network calls.

### Test Steps: Live Gemini API Mode
1. In **Settings**, paste a valid Google Gemini API key and click **💾 Save Settings**.
2. Switch to **AI Insights** and click **🔄 Refresh Insights**.
3. **Expected Result**:
   - Loading indicator appears while calling Gemini 1.5.
   - AI status badge displays: `Gemini 1.5 Pro AI`.
   - Personalized assessment, concerns, and suggestions based on today's active hours and top applications are displayed.

---

## 6. Autostart on System Login

### Objective
Verify OS startup entries are created and removed cleanly.

### Test Steps (macOS)
1. Open **Settings** and check **Launch on System Startup**. Click **💾 Save Settings**.
2. Verify in terminal:
   ```bash
   ls -la ~/Library/LaunchAgents/com.screentime.monitor.plist
   ```
   **Expected Result**: Plist file exists and points to the application jar.
3. Uncheck **Launch on System Startup** and save.
4. Verify:
   ```bash
   ls ~/Library/LaunchAgents/com.screentime.monitor.plist
   ```
   **Expected Result**: File is cleanly deleted.

### Test Steps (Linux)
1. Toggle autostart on in Settings.
2. Verify `~/.config/autostart/screentime-monitor.desktop` exists.

### Test Steps (Windows)
1. Toggle autostart on in Settings.
2. Verify registry key with `reg query HKCU\Software\Microsoft\Windows\CurrentVersion\Run /v ScreenTimeMonitor`.

---

## 7. Fault Tolerance & Self-Healing Database Recovery

### Objective
Verify that corrupt configuration or database files are safely backed up and recreated without crashing the application.

### Test Steps
1. Corrupt the configuration file:
   ```bash
   echo "{ invalid json " > ~/.screentime-monitor/config.json
   ```
2. Launch `java -jar target/screentime-monitor-1.0.0-SNAPSHOT.jar --info`.
3. **Expected Result**:
   - Application logs a warning, moves corrupted file to `config.json.corrupted.<timestamp>`, creates a fresh default `config.json`, and boots cleanly.
4. Corrupt the SQLite database file:
   ```bash
   echo "CORRUPTED SQLITE HEADER" > ~/.screentime-monitor/screentime.db
   ```
5. Launch `java -jar target/screentime-monitor-1.0.0-SNAPSHOT.jar --info`.
6. **Expected Result**:
   - DatabaseManager detects the corrupt database, moves it to `screentime.db.corrupted.<timestamp>`, recreates all tables, and starts successfully.
