package com.screentime.data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class UsageDaoTest {
    private UsageDao usageDao;
    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        usageDao = new UsageDao(new DatabaseManager(tempDir.resolve("test_usage.db")));
    }
}
