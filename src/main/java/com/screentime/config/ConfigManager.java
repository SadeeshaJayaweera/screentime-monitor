package com.screentime.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileReader;
import java.io.FileWriter;
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

    public String getEffectiveGeminiApiKey() {
        if (config != null && config.getGeminiApiKey() != null && !config.getGeminiApiKey().isBlank()) return config.getGeminiApiKey().trim();
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey.trim();
        String propKey = System.getProperty("gemini.api.key");
        if (propKey != null && !propKey.isBlank()) return propKey.trim();
        return "";
    }
}
