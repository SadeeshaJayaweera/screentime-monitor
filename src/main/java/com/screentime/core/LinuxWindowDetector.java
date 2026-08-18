package com.screentime.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Linux active window detector.
 * Uses `xdotool` and `xprop` when available, with graceful fallback to "Unknown"
 * if xdotool is not installed (e.g. Wayland or minimal desktop environments).
 */
public class LinuxWindowDetector implements WindowDetector {

    private static final Logger logger = LoggerFactory.getLogger(LinuxWindowDetector.class);
    private static final AtomicBoolean xdotoolMissingWarned = new AtomicBoolean(false);

    @Override
    public WindowInfo getActiveWindow() {
        try {
            // Step 1: Get active window ID using xdotool
            Process idProcess = new ProcessBuilder("xdotool", "getactivewindow").start();
            boolean idFinished = idProcess.waitFor(1, TimeUnit.SECONDS);
            if (!idFinished) {
                idProcess.destroyForcibly();
                return getFallbackWindowInfo();
            }

            String windowId;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(idProcess.getInputStream()))) {
                windowId = reader.readLine();
            }

            if (windowId == null || windowId.isBlank()) {
                return getFallbackWindowInfo();
            }

            windowId = windowId.trim();

            // Step 2: Get window name/title
            String title = "Unknown Window";
            Process titleProcess = new ProcessBuilder("xdotool", "getwindowname", windowId).start();
            if (titleProcess.waitFor(1, TimeUnit.SECONDS)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(titleProcess.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        title = line.trim();
                    }
                }
            } else {
                titleProcess.destroyForcibly();
            }

            // Step 3: Get PID or WM_CLASS for app name
            String appName = resolveLinuxAppName(windowId, title);

            return new WindowInfo(appName, title, 0, Instant.now());
        } catch (java.io.IOException e) {
            if (xdotoolMissingWarned.compareAndSet(false, true)) {
                logger.warn("xdotool is not installed or not in PATH. Active window tracking on Linux requires xdotool (e.g. `sudo apt install xdotool`). Falling back gracefully to 'Unknown'.");
            }
            return getFallbackWindowInfo();
        } catch (Exception e) {
            logger.debug("Linux window detection error: {}", e.getMessage());
            return getFallbackWindowInfo();
        }
    }

    private String resolveLinuxAppName(String windowId, String title) {
        try {
            Process classProcess = new ProcessBuilder("xprop", "-id", windowId, "WM_CLASS").start();
            if (classProcess.waitFor(1, TimeUnit.SECONDS)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(classProcess.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.contains("=")) {
                        // WM_CLASS(STRING) = "google-chrome", "Google-chrome"
                        String[] parts = line.split("=");
                        if (parts.length > 1) {
                            String raw = parts[1].replace("\"", "").trim();
                            String[] names = raw.split(",");
                            if (names.length > 0 && !names[names.length - 1].trim().isBlank()) {
                                return formatLinuxAppName(names[names.length - 1].trim());
                            }
                        }
                    }
                }
            } else {
                classProcess.destroyForcibly();
            }
        } catch (Exception ignored) {}

        // Fallback: Infer from title if available
        if (title != null && !title.isBlank() && !"Unknown Window".equals(title)) {
            if (title.contains(" - ")) {
                String[] parts = title.split(" - ");
                return parts[parts.length - 1].trim();
            }
            return title;
        }

        return "Application";
    }

    private String formatLinuxAppName(String raw) {
        if (raw == null || raw.isBlank()) return "Application";
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private WindowInfo getFallbackWindowInfo() {
        return new WindowInfo("Unknown", "Unknown Window", 0, Instant.now());
    }

    @Override
    public String getDetectorName() {
        return "Linux (xdotool / xprop)";
    }
}
