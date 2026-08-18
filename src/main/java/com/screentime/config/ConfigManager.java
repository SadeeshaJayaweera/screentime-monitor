package com.screentime.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static volatile ConfigManager instance;
    private AppConfig config;

    public static Path resolveAppDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData != null ? Paths.get(appData, "ScreenTimeMonitor") : Paths.get(System.getProperty("user.home"), ".screentime-monitor");
        }
        return Paths.get(System.getProperty("user.home"), ".screentime-monitor").toAbsolutePath().normalize();
    }
}
