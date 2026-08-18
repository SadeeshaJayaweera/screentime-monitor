package com.screentime.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages configuration persistence and cross-platform app data directory resolution.
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String APP_DIR_NAME_UNIX = ".screentime-monitor";
    private static final String APP_DIR_NAME_WIN = "ScreenTimeMonitor";
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String DB_FILE_NAME = "screentime.db";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static volatile ConfigManager instance;

    private final Path appDataDir;
    private final Path configFilePath;
    private AppConfig config;

    public ConfigManager() {
        this(resolveAppDataDir());
    }

    public ConfigManager(Path appDataDir) {
        this.appDataDir = appDataDir;
        this.configFilePath = appDataDir.resolve(CONFIG_FILE_NAME);
        ensureAppDataDirExists();
        this.config = loadConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    /**
     * Resolves the OS-appropriate app-data directory:
     * - Windows: %APPDATA%\ScreenTimeMonitor (or ~/.screentime-monitor fallback)
     * - macOS: ~/.screentime-monitor
     * - Linux/Other: ~/.screentime-monitor
     */
    public static Path resolveAppDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        Path basePath;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                basePath = Paths.get(appData, APP_DIR_NAME_WIN);
            } else {
                basePath = Paths.get(System.getProperty("user.home"), APP_DIR_NAME_UNIX);
            }
        } else {
            // Linux and macOS
            basePath = Paths.get(System.getProperty("user.home"), APP_DIR_NAME_UNIX);
        }

        return basePath.toAbsolutePath().normalize();
    }

    private void ensureAppDataDirExists() {
        try {
            if (!Files.exists(appDataDir)) {
                Files.createDirectories(appDataDir);
                logger.info("Created application data directory at: {}", appDataDir);
            }
        } catch (IOException e) {
            logger.error("Failed to create app data directory: {}", appDataDir, e);
        }
    }

    /**
     * Loads the configuration from disk, or creates default settings if not present.
     */
    public synchronized AppConfig loadConfig() {
        if (Files.exists(configFilePath)) {
            try (FileReader reader = new FileReader(configFilePath.toFile())) {
                AppConfig loaded = gson.fromJson(reader, AppConfig.class);
                if (loaded != null) {
                    this.config = loaded;
                    logger.info("Loaded configuration from: {}", configFilePath);
                    return this.config;
                }
            } catch (Exception e) {
                logger.warn("Failed to parse config file at {}, backing up and resetting to defaults.", configFilePath, e);
                try {
                    Path backupPath = configFilePath.resolveSibling("config.json.corrupted." + System.currentTimeMillis());
                    Files.move(configFilePath, backupPath);
                    logger.warn("Moved corrupted config file to: {}", backupPath);
                } catch (IOException ignored) {}
            }
        }

        // Initialize default configuration
        this.config = new AppConfig();
        saveConfig();
        return this.config;
    }

    /**
     * Saves the current configuration to disk.
     */
    public synchronized boolean saveConfig() {
        ensureAppDataDirExists();
        try (FileWriter writer = new FileWriter(configFilePath.toFile())) {
            gson.toJson(config != null ? config : new AppConfig(), writer);
            logger.info("Saved configuration to: {}", configFilePath);
            return true;
        } catch (IOException e) {
            logger.error("Failed to save configuration to: {}", configFilePath, e);
            return false;
        }
    }

    public synchronized AppConfig getConfig() {
        if (config == null) {
            config = loadConfig();
        }
        return config;
    }

    public synchronized void setConfig(AppConfig config) {
        this.config = config;
        saveConfig();
    }

    /**
     * Resolves the effective Gemini API key:
     * 1. Checks config.json geminiApiKey
     * 2. Falls back to GEMINI_API_KEY environment variable
     * 3. Falls back to gemini.api.key system property
     */
    public String getEffectiveGeminiApiKey() {
        if (config != null && config.getGeminiApiKey() != null && !config.getGeminiApiKey().isBlank()) {
            return config.getGeminiApiKey().trim();
        }

        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }

        String propKey = System.getProperty("gemini.api.key");
        if (propKey != null && !propKey.isBlank()) {
            return propKey.trim();
        }

        return "";
    }

    public Path getAppDataDir() {
        return appDataDir;
    }

    public Path getConfigFilePath() {
        return configFilePath;
    }

    public Path getDatabasePath() {
        return appDataDir.resolve(DB_FILE_NAME);
    }
}
