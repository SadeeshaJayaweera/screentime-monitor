package com.screentime.core;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MacWindowDetector implements WindowDetector {
    @Override public WindowInfo getActiveWindow() {
        try {
            Process process = new ProcessBuilder("osascript", "-e", "tell application \"System Events\" to get name of first process whose frontmost is true").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String name = r.readLine();
                if (name != null && !name.isBlank()) return new WindowInfo(name.trim(), name.trim(), 0);
            }
        } catch (Exception ignored) {}
        return new WindowInfo("Unknown", "Unknown", 0);
    }
}
