package com.screentime.core;

import com.screentime.data.DailyUsageSummary;
import com.screentime.data.DatabaseManager;
import com.screentime.data.UsageDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TrackingEnginePersistenceTest {

    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;
    private UsageDao usageDao;
    private TestWindowDetector windowDetector;
    private TestIdleDetector idleDetector;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test_engine_persist.db");
        databaseManager = new DatabaseManager(dbPath);
        usageDao = new UsageDao(databaseManager);
        windowDetector = new TestWindowDetector();
        idleDetector = new TestIdleDetector();
    }

    @Test
    void testStartupCounterInitializesFromDatabase() {
        LocalDate today = LocalDate.now();
        usageDao.savePeriodicDailyActiveSnapshot(today, 1800); // 30 minutes existing usage

        TrackingEngine engine = new TrackingEngine(windowDetector, idleDetector, usageDao, 5, 60);
        assertEquals(1800, engine.getTodayActiveSeconds());
    }

    @Test
    void testSessionAutoPersistedOnClose() throws InterruptedException {
        TrackingEngine engine = new TrackingEngine(windowDetector, idleDetector, usageDao, 5, 60);
        windowDetector.setActiveWindow(new WindowInfo("Postman", "API Testing", 999));
        idleDetector.setIdle(false);

        engine.start(false);
        engine.poll(); // starts session

        // Wait 1.1s so session duration is >= 1s
        Thread.sleep(1100);

        // Switch window to trigger session close
        windowDetector.setActiveWindow(new WindowInfo("Firefox", "Documentation", 888));
        engine.poll();

        LocalDate today = LocalDate.now();
        DailyUsageSummary summary = usageDao.getUsageForDate(today);

        // Postman session should now be persisted in SQLite
        assertFalse(summary.getAppBreakdown().isEmpty());
        assertEquals("Postman", summary.getAppBreakdown().get(0).getAppName());
        assertTrue(summary.getAppBreakdown().get(0).getSecondsUsed() >= 1);

        engine.stop();
    }

    @Test
    void testMidnightDayRollover() {
        TrackingEngine engine = new TrackingEngine(windowDetector, idleDetector, usageDao, 5, 60);
        LocalDate yesterday = LocalDate.of(2026, 8, 10);
        LocalDate today = LocalDate.of(2026, 8, 11);

        engine.start(false);
        engine.setCurrentTrackingDate(yesterday);
        engine.setTodayActiveSeconds(3600); // 1 hour yesterday
        engine.poll(); // starts session under yesterday

        // Trigger midnight rollover by simulating date change
        engine.handleDayRollover(today);

        assertEquals(today, engine.getCurrentTrackingDate());
        assertEquals(0, engine.getTodayActiveSeconds()); // reset for new day

        // Yesterday's usage should be preserved in DB
        DailyUsageSummary yesterdaySummary = usageDao.getUsageForDate(yesterday);
        assertTrue(yesterdaySummary.getTotalActiveSeconds() >= 3600);

        engine.stop();
    }

    // --- Mocks ---

    private static class TestWindowDetector implements WindowDetector {
        private WindowInfo activeWindow = new WindowInfo("TestApp", "TestWindow", 1, Instant.now());

        public void setActiveWindow(WindowInfo info) {
            this.activeWindow = info;
        }

        @Override
        public WindowInfo getActiveWindow() {
            return activeWindow;
        }

        @Override
        public String getDetectorName() {
            return "Test Window Detector";
        }
    }

    private static class TestIdleDetector extends IdleDetector {
        private boolean isIdle = false;

        public void setIdle(boolean idle) {
            this.isIdle = idle;
        }

        @Override
        public boolean isIdle(int thresholdSeconds) {
            return isIdle;
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}
    }
}
