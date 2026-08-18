package com.screentime.config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    @Test
    void testDefaultConfigInitialization(@TempDir Path tempDir) {
        ConfigManager manager = new ConfigManager(tempDir);
        assertNotNull(manager.getConfig());
    }
}
