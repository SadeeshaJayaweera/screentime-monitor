package com.screentime.data;

import com.screentime.core.TrackingSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UsageDaoTest {

    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;
    private UsageDao usageDao;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test_usage.db");
        databaseManager = new DatabaseManager(dbPath);
        usageDao = new UsageDao(databaseManager);
    }

    @Test
    void testRecordSessionAndDailyAggregation() {
        Instant now = Instant.now();
        TrackingSession session1 = new TrackingSession("IntelliJ IDEA", "UsageDao.java", now.minusSeconds(120));
        session1.close(now);

        usageDao.recordSession(session1);

        LocalDate today = LocalDate.now();
        DailyUsageSummary summary = usageDao.getUsageForDate(today);

        assertNotNull(summary);
        assertEquals(today, summary.getDate());
        assertEquals(120, summary.getTotalActiveSeconds());
        assertEquals(1, summary.getAppBreakdown().size());
        assertEquals("IntelliJ IDEA", summary.getAppBreakdown().get(0).getAppName());
        assertEquals(120, summary.getAppBreakdown().get(0).getSecondsUsed());
    }

    @Test
    void testMultipleSessionsSameAndDifferentApps() {
        Instant now = Instant.now();

        // 2 sessions of IntelliJ (60s + 40s)
        TrackingSession intellij1 = new TrackingSession("IntelliJ IDEA", "Main.java", now.minusSeconds(200));
        intellij1.close(now.minusSeconds(140));
        usageDao.recordSession(intellij1);

        TrackingSession intellij2 = new TrackingSession("IntelliJ IDEA", "UsageDao.java", now.minusSeconds(130));
        intellij2.close(now.minusSeconds(90));
        usageDao.recordSession(intellij2);

        // 1 session of Chrome (100s)
        TrackingSession chrome = new TrackingSession("Google Chrome", "StackOverflow", now.minusSeconds(100));
        chrome.close(now);
        usageDao.recordSession(chrome);

        LocalDate today = LocalDate.now();
        DailyUsageSummary summary = usageDao.getUsageForDate(today);

        assertEquals(200, summary.getTotalActiveSeconds()); // 60 + 40 + 100
        assertEquals(2, summary.getAppBreakdown().size());

        // Top app should be either IntelliJ (100s) or Chrome (100s)
        List<AppUsage> topApps = usageDao.getTopAppsForDate(today, 5);
        assertEquals(2, topApps.size());
        assertEquals(100, topApps.get(0).getSecondsUsed());
        assertEquals(100, topApps.get(1).getSecondsUsed());
    }

    @Test
    void testGetUsageForDateRange() {
        LocalDate day1 = LocalDate.of(2026, 8, 10);
        LocalDate day2 = LocalDate.of(2026, 8, 11);
        LocalDate day3 = LocalDate.of(2026, 8, 12);
        ZoneId zone = ZoneId.systemDefault();

        // Record session on Day 1
        Instant start1 = day1.atTime(10, 0).atZone(zone).toInstant();
        Instant end1 = day1.atTime(11, 0).atZone(zone).toInstant();
        TrackingSession s1 = new TrackingSession("Visual Studio Code", "project", start1);
        s1.close(end1);
        usageDao.recordSession(s1);

        // Record session on Day 2
        Instant start2 = day2.atTime(14, 0).atZone(zone).toInstant();
        Instant end2 = day2.atTime(15, 30).atZone(zone).toInstant();
        TrackingSession s2 = new TrackingSession("Figma", "wireframes", start2);
        s2.close(end2);
        usageDao.recordSession(s2);

        List<DailyUsageSummary> range = usageDao.getUsageForDateRange(day1, day3);
        assertEquals(3, range.size());

        // Day 1: 3600s
        assertEquals(day1, range.get(0).getDate());
        assertEquals(3600, range.get(0).getTotalActiveSeconds());
        assertEquals("Visual Studio Code", range.get(0).getAppBreakdown().get(0).getAppName());

        // Day 2: 5400s
        assertEquals(day2, range.get(1).getDate());
        assertEquals(5400, range.get(1).getTotalActiveSeconds());
        assertEquals("Figma", range.get(1).getAppBreakdown().get(0).getAppName());

        // Day 3: 0s
        assertEquals(day3, range.get(2).getDate());
        assertEquals(0, range.get(2).getTotalActiveSeconds());
        assertTrue(range.get(2).getAppBreakdown().isEmpty());
    }

    @Test
    void testMultiDaySessionSplittingAcrossMidnight() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate day1 = LocalDate.of(2026, 8, 15);
        LocalDate day2 = LocalDate.of(2026, 8, 16);

        // Session starts at 23:50 on Day 1 and ends at 00:20 on Day 2 (30 minutes total: 10m on Day 1, 20m on Day 2)
        Instant start = day1.atTime(23, 50).atZone(zone).toInstant();
        Instant end = day2.atTime(0, 20).atZone(zone).toInstant();

        TrackingSession midnightSession = new TrackingSession("Terminal", "build", start);
        midnightSession.close(end);

        usageDao.recordSession(midnightSession);

        DailyUsageSummary day1Summary = usageDao.getUsageForDate(day1);
        DailyUsageSummary day2Summary = usageDao.getUsageForDate(day2);

        // Day 1 should have 600s (10 min)
        assertEquals(600, day1Summary.getTotalActiveSeconds());
        assertEquals(1, day1Summary.getAppBreakdown().size());
        assertEquals(600, day1Summary.getAppBreakdown().get(0).getSecondsUsed());

        // Day 2 should have 1200s (20 min)
        assertEquals(1200, day2Summary.getTotalActiveSeconds());
        assertEquals(1, day2Summary.getAppBreakdown().size());
        assertEquals(1200, day2Summary.getAppBreakdown().get(0).getSecondsUsed());
    }

    @Test
    void testPeriodicDailySnapshot() {
        LocalDate today = LocalDate.now();
        usageDao.savePeriodicDailyActiveSnapshot(today, 500);
        assertEquals(500, usageDao.getActiveSecondsForDate(today));

        // Saving higher value updates it
        usageDao.savePeriodicDailyActiveSnapshot(today, 600);
        assertEquals(600, usageDao.getActiveSecondsForDate(today));

        // Saving lower value does NOT decrease it (MAX logic)
        usageDao.savePeriodicDailyActiveSnapshot(today, 400);
        assertEquals(600, usageDao.getActiveSecondsForDate(today));
    }

    @Test
    void testSettingsAndLimitExtensions() {
        usageDao.setSetting("theme", "dark");
        assertEquals("dark", usageDao.getSetting("theme", "light"));
        assertEquals("default-val", usageDao.getSetting("nonexistent", "default-val"));

        LimitExtensionRecord extension = new LimitExtensionRecord(
                LocalDate.now(),
                30,
                Instant.now(),
                "Coding sprint"
        );
        usageDao.recordLimitExtension(extension);
    }
}
