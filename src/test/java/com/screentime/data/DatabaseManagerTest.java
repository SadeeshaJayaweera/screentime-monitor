package com.screentime.data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {
    @Test
    void testDatabaseInitialization(@TempDir Path tempDir) {
        Path dbPath = tempDir.resolve("test_screentime.db");
        DatabaseManager dbManager = new DatabaseManager(dbPath);
        assertNotNull(dbManager);
    }
}
