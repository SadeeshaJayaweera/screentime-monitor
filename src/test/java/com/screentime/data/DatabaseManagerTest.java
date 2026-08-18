package com.screentime.data;

import com.screentime.core.TrackingSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;
    private UsageDao usageDao;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test_screentime.db");
        databaseManager = new DatabaseManager(dbPath);
        usageDao = new UsageDao(databaseManager);
    }

    @Test
    void testSchemaInitializationAndSessionPersistence() {
        Instant now = Instant.now();
        TrackingSession session = new TrackingSession(
                "IntelliJ IDEA",
                "screentime-monitor - Main.java",
                now.minusSeconds(120)
        );
        session.close(now);

        usageDao.recordSession(session);

        LocalDate today = LocalDate.now();
        DailyUsageSummary summary = usageDao.getUsageForDate(today);
        assertNotNull(summary);
        assertEquals(120, summary.getTotalActiveSeconds());
        assertEquals(1, summary.getAppBreakdown().size());
        assertEquals("IntelliJ IDEA", summary.getAppBreakdown().get(0).getAppName());
    }

    @Test
    void testDailyUsageSnapshotAndQuery() {
        LocalDate testDate = LocalDate.of(2026, 8, 17);
        usageDao.savePeriodicDailyActiveSnapshot(testDate, 300);

        long activeSeconds = usageDao.getActiveSecondsForDate(testDate);
        assertEquals(300, activeSeconds);

        // Snapshot with higher value updates it
        usageDao.savePeriodicDailyActiveSnapshot(testDate, 500);
        assertEquals(500, usageDao.getActiveSecondsForDate(testDate));
    }
}
