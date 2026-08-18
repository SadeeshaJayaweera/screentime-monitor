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
    void testRecordSessionAggregatesCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        usageDao.recordSession(new TrackingSession("Google Chrome", "GitHub", now.minusMinutes(10), now, false));
        assertEquals(600, usageDao.getTodayUsageSeconds());
    }
    @Test
    void testGetUsageForDateRange() {
        LocalDate day1 = LocalDate.of(2026, 8, 1);
        usageDao.savePeriodicDailyActiveSnapshot(day1, 3600);
        assertEquals(1, usageDao.getUsageForDateRange(day1, day1).size());
    }
}
