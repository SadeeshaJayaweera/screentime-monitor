package com.screentime.data;
import com.screentime.core.TrackingSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UsageDaoTest {
    private UsageDao usageDao;
    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        usageDao = new UsageDao(new DatabaseManager(tempDir.resolve("test_usage.db")));
    }
    @Test
    void testRecordAndGetExtensionStats() {
        LocalDate today = LocalDate.now();
        usageDao.recordExtension(today, 15, "Finish task");
        assertEquals(1, usageDao.getTodayExtensionStats().count());
        assertEquals(15, usageDao.getTodayExtensionStats().totalMinutes());
    }
}
