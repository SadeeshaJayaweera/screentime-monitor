package com.screentime.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @TempDir
    Path tempDir;

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager(tempDir);
    }

    @Test
    void testDefaultConfigCreation() {
        AppConfig config = configManager.getConfig();
        assertNotNull(config);
        assertEquals(480, config.getDailyLimitMinutes());
        assertTrue(config.isAiEnabled());
        assertFalse(config.isOnboardingCompleted());
        assertTrue(Files.exists(configManager.getConfigFilePath()));
    }

    @Test
    void testSaveAndReloadConfig() {
        AppConfig config = configManager.getConfig();
        config.setDailyLimitMinutes(360);
        config.setGeminiApiKey("test-key-12345");
        config.setOnboardingCompleted(true);
        configManager.saveConfig();

        // Create new instance on same temp directory
        ConfigManager reloadedManager = new ConfigManager(tempDir);
        AppConfig reloaded = reloadedManager.getConfig();

        assertEquals(360, reloaded.getDailyLimitMinutes());
        assertEquals("test-key-12345", reloaded.getGeminiApiKey());
        assertTrue(reloaded.isOnboardingCompleted());
    }

    @Test
    void testEffectiveGeminiApiKey() {
        AppConfig config = configManager.getConfig();
        config.setGeminiApiKey("custom-api-key");
        configManager.saveConfig();

        assertEquals("custom-api-key", configManager.getEffectiveGeminiApiKey());

        // When blank, falls back to property/env
        config.setGeminiApiKey("");
        configManager.saveConfig();

        System.setProperty("gemini.api.key", "fallback-prop-key");
        assertEquals("fallback-prop-key", configManager.getEffectiveGeminiApiKey());
        System.clearProperty("gemini.api.key");
    }

    @Test
    void testResolveAppDataDirNotNull() {
        Path resolved = ConfigManager.resolveAppDataDir();
        assertNotNull(resolved);
        assertTrue(resolved.isAbsolute());
    }
}
