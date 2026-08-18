package com.screentime.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static volatile ConfigManager instance;
    private final Path appDataDir;
    private final Path configFilePath;
    private AppConfig config;

    public ConfigManager() { this(resolveAppDataDir()); }
    public ConfigManager(Path appDataDir) {
        this.appDataDir = appDataDir;
        this.configFilePath = appDataDir.resolve("config.json");
    }

    public static Path resolveAppDataDir() {
        return Paths.get(System.getProperty("user.home"), ".screentime-monitor").toAbsolutePath().normalize();
    }

    public synchronized AppConfig loadConfig() {
        if (Files.exists(configFilePath)) {
            try (FileReader reader = new FileReader(configFilePath.toFile())) {
                AppConfig loaded = gson.fromJson(reader, AppConfig.class);
                if (loaded != null) { this.config = loaded; return this.config; }
            } catch (Exception ignored) {}
        }
        this.config = new AppConfig();
        return this.config;
    }
}
