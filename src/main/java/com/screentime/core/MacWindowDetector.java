package com.screentime.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * macOS active application & window detector.
 * Uses native macOS LaunchServices utility `lsappinfo` for instant, permission-free
 * foreground application detection, with AppleScript fallback for window titles.
 */
public class MacWindowDetector implements WindowDetector {

    private static final Logger logger = LoggerFactory.getLogger(MacWindowDetector.class);

    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\"LSDisplayName\"=\"([^\"]+)\"");
    private static final Pattern PID_PATTERN = Pattern.compile("\"pid\"=(\\d+)");
    private static final Pattern BUNDLE_ID_PATTERN = Pattern.compile("\"CFBundleIdentifier\"=\"([^\"]+)\"");

    @Override
    public WindowInfo getActiveWindow() {
        WindowInfo info = detectViaLsappinfo();
        if (info != null && !"Unknown".equalsIgnoreCase(info.getAppName())) {
            return info;
        }

        // Fallback to AppleScript
        return detectViaAppleScript();
    }

    /**
     * Primary strategy: lsappinfo (built into all macOS releases).
     * Requires no Accessibility/Assistive permissions and returns instantly.
     */
    private WindowInfo detectViaLsappinfo() {
        try {
            // lsappinfo front returns the ASN identifier of the frontmost app
            Process frontProcess = new ProcessBuilder("lsappinfo", "front").start();
            boolean finished = frontProcess.waitFor(1, TimeUnit.SECONDS);
            if (!finished) {
                frontProcess.destroyForcibly();
                return null;
            }

            String frontAsn;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(frontProcess.getInputStream()))) {
                frontAsn = reader.readLine();
            }

            if (frontAsn == null || frontAsn.isBlank()) {
                return null;
            }

            // Query details for this frontmost ASN
            Process infoProcess = new ProcessBuilder("lsappinfo", "info", "-only", "name,bundleid,pid", frontAsn.trim()).start();
            boolean infoFinished = infoProcess.waitFor(1, TimeUnit.SECONDS);
            if (!infoFinished) {
                infoProcess.destroyForcibly();
                return null;
            }

            String appName = "Unknown";
            long pid = 0;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(infoProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher nameMatcher = DISPLAY_NAME_PATTERN.matcher(line);
                    if (nameMatcher.find()) {
                        appName = nameMatcher.group(1).trim();
                    }

                    Matcher pidMatcher = PID_PATTERN.matcher(line);
                    if (pidMatcher.find()) {
                        try {
                            pid = Long.parseLong(pidMatcher.group(1));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (!"Unknown".equals(appName)) {
                return new WindowInfo(appName, appName, pid, Instant.now());
            }
        } catch (Exception e) {
            logger.debug("lsappinfo detection failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fallback strategy: osascript (AppleScript).
     */
    private WindowInfo detectViaAppleScript() {
        try {
            String script = "tell application \"System Events\" to get name of first process whose frontmost is true";
            Process process = new ProcessBuilder("osascript", "-e", script).start();
            boolean finished = process.waitFor(1500, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new WindowInfo("Unknown", "Unknown Window", 0, Instant.now());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String appName = reader.readLine();
                if (appName != null && !appName.isBlank()) {
                    return new WindowInfo(appName.trim(), appName.trim(), 0, Instant.now());
                }
            }
        } catch (Exception e) {
            logger.warn("macOS AppleScript detection error: {}", e.getMessage());
        }

        return new WindowInfo("Unknown", "Unknown Window", 0, Instant.now());
    }

    @Override
    public String getDetectorName() {
        return "macOS (LaunchServices lsappinfo / AppleScript)";
    }
}
