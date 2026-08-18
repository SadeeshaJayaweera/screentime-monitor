package com.screentime.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages operating system startup registration for Windows, macOS, and Linux.
 */
public class AutostartManager {

    private static final Logger logger = LoggerFactory.getLogger(AutostartManager.class);
    private static volatile AutostartManager instance;

    public static AutostartManager getInstance() {
        if (instance == null) {
            synchronized (AutostartManager.class) {
                if (instance == null) {
                    instance = new AutostartManager();
                }
            }
        }
        return instance;
    }

    /**
     * Enables or disables autostart on system login for the current OS.
     */
    public boolean setAutostartEnabled(boolean enabled) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            boolean success;
            if (os.contains("mac")) {
                success = configureMacAutostart(enabled);
            } else if (os.contains("win")) {
                success = configureWindowsAutostart(enabled);
            } else {
                success = configureLinuxAutostart(enabled);
            }

            if (success) {
                ConfigManager.getInstance().getConfig().setAutostartOnLogin(enabled);
                ConfigManager.getInstance().saveConfig();
            }
            return success;
        } catch (Throwable t) {
            logger.error("Failed to configure autostart on login: {}", t.getMessage(), t);
            return false;
        }
    }

    /**
     * Checks if autostart registration currently exists on the filesystem/registry.
     */
    public boolean isAutostartEnabled() {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                Path plistPath = getMacPlistPath();
                return Files.exists(plistPath);
            } else if (os.contains("win")) {
                return isWindowsAutostartConfigured();
            } else {
                Path desktopPath = getLinuxDesktopPath();
                return Files.exists(desktopPath);
            }
        } catch (Throwable t) {
            logger.warn("Error checking autostart status: {}", t.getMessage());
            return ConfigManager.getInstance().getConfig().isAutostartOnLogin();
        }
    }

    // --- macOS Implementation (LaunchAgents plist) ---

    private Path getMacPlistPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "Library", "LaunchAgents", "com.screentime.monitor.plist");
    }

    private boolean configureMacAutostart(boolean enable) {
        Path plistPath = getMacPlistPath();
        try {
            if (enable) {
                Files.createDirectories(plistPath.getParent());
                String appPath = resolveExecutableCommand();
                String plistContent = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                    <plist version="1.0">
                    <dict>
                        <key>Label</key>
                        <string>com.screentime.monitor</string>
                        <key>ProgramArguments</key>
                        <array>
                            <string>%s</string>
                        </array>
                        <key>RunAtLoad</key>
                        <true/>
                        <key>ProcessType</key>
                        <string>Interactive</string>
                    </dict>
                    </plist>
                    """, escapeXml(appPath));

                Files.writeString(plistPath, plistContent);
                logger.info("Registered macOS LaunchAgent at {}", plistPath);
            } else {
                if (Files.exists(plistPath)) {
                    Files.delete(plistPath);
                    logger.info("Unregistered macOS LaunchAgent at {}", plistPath);
                }
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to update macOS LaunchAgent: {}", e.getMessage());
            return false;
        }
    }

    // --- Linux Implementation (~/.config/autostart/.desktop) ---

    private Path getLinuxDesktopPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".config", "autostart", "screentime-monitor.desktop");
    }

    private boolean configureLinuxAutostart(boolean enable) {
        Path desktopPath = getLinuxDesktopPath();
        try {
            if (enable) {
                Files.createDirectories(desktopPath.getParent());
                String execCmd = resolveExecutableCommand();
                String content = String.format("""
                    [Desktop Entry]
                    Type=Application
                    Name=ScreenTime Monitor
                    Comment=Cross-platform screen time & health monitor
                    Exec=%s
                    Terminal=false
                    X-GNOME-Autostart-enabled=true
                    Categories=Utility;Health;
                    """, execCmd);

                Files.writeString(desktopPath, content);
                logger.info("Registered Linux autostart desktop entry at {}", desktopPath);
            } else {
                if (Files.exists(desktopPath)) {
                    Files.delete(desktopPath);
                    logger.info("Unregistered Linux autostart desktop entry at {}", desktopPath);
                }
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to update Linux autostart entry: {}", e.getMessage());
            return false;
        }
    }

    // --- Windows Implementation (HKCU\Software\Microsoft\Windows\CurrentVersion\Run) ---

    private boolean configureWindowsAutostart(boolean enable) {
        try {
            String appPath = resolveExecutableCommand();
            ProcessBuilder pb;
            if (enable) {
                pb = new ProcessBuilder("reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "ScreenTimeMonitor", "/t", "REG_SZ", "/d", appPath, "/f");
            } else {
                pb = new ProcessBuilder("reg", "delete", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "ScreenTimeMonitor", "/f");
            }
            Process process = pb.start();
            boolean done = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return done && process.exitValue() == 0;
        } catch (Exception e) {
            logger.warn("Could not modify Windows registry for autostart: {}", e.getMessage());
            return false;
        }
    }

    private boolean isWindowsAutostartConfigured() {
        try {
            Process process = new ProcessBuilder("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", "ScreenTimeMonitor").start();
            boolean done = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            return done && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolves the executable path or command for launching this application.
     */
    public String resolveExecutableCommand() {
        try {
            String jarPath = AutostartManager.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            File jarFile = new File(jarPath);
            if (jarFile.getName().endsWith(".jar")) {
                return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java -jar \"" + jarFile.getAbsolutePath() + "\"";
            }
            return jarFile.getAbsolutePath();
        } catch (Exception e) {
            return "screentime-monitor";
        }
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
