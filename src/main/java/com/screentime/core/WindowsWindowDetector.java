package com.screentime.core;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

/**
 * Windows active window detector using JNA (User32 / GetForegroundWindow).
 */
public class WindowsWindowDetector implements WindowDetector {

    private static final Logger logger = LoggerFactory.getLogger(WindowsWindowDetector.class);
    private static final int MAX_TITLE_LENGTH = 1024;

    @Override
    public WindowInfo getActiveWindow() {
        try {
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) {
                return new WindowInfo("System", "Desktop / No Active Window", 0, Instant.now());
            }

            char[] buffer = new char[MAX_TITLE_LENGTH * 2];
            int length = User32.INSTANCE.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH);
            String title = (length > 0) ? new String(buffer, 0, length).trim() : "Untitled";

            IntByReference processIdRef = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, processIdRef);
            long processId = processIdRef.getValue();

            String appName = resolveProcessName(processId);

            return new WindowInfo(appName, title, processId, Instant.now());
        } catch (Throwable t) {
            logger.warn("Failed to retrieve active window on Windows: {}", t.getMessage());
            return new WindowInfo("Unknown", "Unknown Window", 0, Instant.now());
        }
    }

    private String resolveProcessName(long processId) {
        if (processId <= 0) {
            return "System";
        }

        try {
            Optional<ProcessHandle> handle = ProcessHandle.of(processId);
            if (handle.isPresent()) {
                Optional<String> command = handle.get().info().command();
                if (command.isPresent()) {
                    File file = new File(command.get());
                    String name = file.getName();
                    if (name.toLowerCase().endsWith(".exe")) {
                        name = name.substring(0, name.length() - 4);
                    }
                    return formatApplicationName(name);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not resolve process name for PID {}: {}", processId, e.getMessage());
        }

        return "Application (" + processId + ")";
    }

    private String formatApplicationName(String name) {
        if (name == null || name.isBlank()) return "Unknown";
        return switch (name.toLowerCase()) {
            case "chrome" -> "Google Chrome";
            case "msedge" -> "Microsoft Edge";
            case "firefox" -> "Mozilla Firefox";
            case "code" -> "Visual Studio Code";
            case "idea64", "idea" -> "IntelliJ IDEA";
            case "explorer" -> "Windows Explorer";
            case "slack" -> "Slack";
            case "spotify" -> "Spotify";
            case "discord" -> "Discord";
            case "notion" -> "Notion";
            default -> Character.toUpperCase(name.charAt(0)) + name.substring(1);
        };
    }

    @Override
    public String getDetectorName() {
        return "Windows (JNA User32)";
    }
}
