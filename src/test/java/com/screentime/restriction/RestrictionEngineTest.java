package com.screentime.restriction;

import com.screentime.data.DatabaseManager;
import com.screentime.data.UsageDao;
import com.screentime.notifications.NotificationLevel;
import com.screentime.notifications.NotificationListener;
import com.screentime.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestrictionEngineTest {

    @TempDir
    Path tempDir;

    private RestrictionConfig config;
    private UsageDao usageDao;
    private NotificationService notificationService;
    private RestrictionEngine engine;
    private List<TestNotification> receivedNotifications;

    private record TestNotification(NotificationLevel level, String title, String body) {}

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test_restriction.db");
        DatabaseManager databaseManager = new DatabaseManager(dbPath);
        usageDao = new UsageDao(databaseManager);
        notificationService = new NotificationService();

        receivedNotifications = new ArrayList<>();
        notificationService.addListener((level, title, body) ->
                receivedNotifications.add(new TestNotification(level, title, body)));

        config = new RestrictionConfig();
        config.setDailyLimitMinutes(120); // 2 hours = 7200 seconds
        config.setWarningThresholds(List.of(50, 75, 90, 100));
        config.setMaxExtensionsPerDay(2);
        config.setMaxExtensionMinutesPerDay(60);
        config.setExtensionReminderCadenceMinutes(5);

        engine = new RestrictionEngine(config, usageDao, notificationService);
    }

    @Test
    void testThresholdCrossingNotifications() {
        // 1. Below 50% (30 mins = 1800s) -> No notification
        engine.checkUsage(1800);
        assertTrue(receivedNotifications.isEmpty());

        // 2. Cross 50% (60 mins = 3600s) -> Warning notification for 50%
        engine.checkUsage(3600);
        assertEquals(1, receivedNotifications.size());
        assertEquals(NotificationLevel.WARNING, receivedNotifications.get(0).level());
        assertTrue(receivedNotifications.get(0).title().contains("50%"));
        assertTrue(receivedNotifications.get(0).body().contains("60 minutes remaining"));

        // 3. Re-check 50% -> Should not duplicate notification
        engine.checkUsage(3610);
        assertEquals(1, receivedNotifications.size());

        // 4. Cross 75% (90 mins = 5400s) -> Warning notification for 75%
        engine.checkUsage(5400);
        assertEquals(2, receivedNotifications.size());
        assertEquals(NotificationLevel.WARNING, receivedNotifications.get(1).level());
        assertTrue(receivedNotifications.get(1).title().contains("75%"));
        assertTrue(receivedNotifications.get(1).body().contains("30 minutes remaining"));

        // 5. Cross 90% (108 mins = 6480s)
        engine.checkUsage(6480);
        assertEquals(3, receivedNotifications.size());
        assertTrue(receivedNotifications.get(2).title().contains("90%"));

        // 6. Cross 100% (120 mins = 7200s) -> Critical notification
        engine.checkUsage(7200);
        assertEquals(4, receivedNotifications.size());
        assertEquals(NotificationLevel.CRITICAL, receivedNotifications.get(3).level());
        assertTrue(receivedNotifications.get(3).title().contains("Limit Reached"));
    }

    @Test
    void testExtensionRequestFlow() {
        // Run to 100% limit
        engine.checkUsage(7200);
        assertEquals(1, receivedNotifications.stream().filter(n -> n.level() == NotificationLevel.CRITICAL).count());

        // Request 30 mins extension
        ExtensionResult result = engine.requestExtension(30, "Need to finish urgent email");
        assertTrue(result.isGranted());
        assertEquals(30, result.getGrantedMinutes());
        assertEquals(150, result.getNewLimitMinutes());
        assertEquals(30, result.getRemainingMinutes());
        assertEquals(1, result.getExtensionCountToday());

        // Verify confirmation notification was sent
        TestNotification last = receivedNotifications.get(receivedNotifications.size() - 1);
        assertEquals(NotificationLevel.INFO, last.level());
        assertTrue(last.title().contains("Extended"));
        assertTrue(last.body().contains("30 minutes"));
        assertTrue(last.body().contains("150 mins"));

        // Effective limit updated
        assertEquals(150, engine.getEffectiveDailyLimit());
        assertEquals(30, engine.getRemainingMinutes(120));
        assertFalse(engine.isLimitExceeded(120));
    }

    @Test
    void testEscalatedReminderCadenceAfterExtension() {
        engine.checkUsage(7200); // 120 mins
        engine.requestExtension(30, "Work"); // New limit 150 mins

        int initialNotifs = receivedNotifications.size();

        // 4 minutes later (240s) -> Less than 5 min cadence, no reminder yet
        engine.checkUsage(7200 + 240);
        assertEquals(initialNotifs, receivedNotifications.size());

        // 5 minutes later (300s) -> Triggers escalated reminder
        engine.checkUsage(7200 + 300);
        assertEquals(initialNotifs + 1, receivedNotifications.size());

        TestNotification reminder = receivedNotifications.get(receivedNotifications.size() - 1);
        assertEquals(NotificationLevel.WARNING, reminder.level());
        assertTrue(reminder.title().contains("Extension #1"));
        assertTrue(reminder.body().contains("25 minutes remaining"));
    }

    @Test
    void testDailyExtensionCountAndMinuteCaps() {
        // 1. First extension (30 mins) -> OK
        ExtensionResult r1 = engine.requestExtension(30, "First extension");
        assertTrue(r1.isGranted());

        // 2. Second extension (30 mins) -> OK (count=2, total=60 mins)
        ExtensionResult r2 = engine.requestExtension(30, "Second extension");
        assertTrue(r2.isGranted());

        // 3. Third extension (15 mins) -> Rejected (maxExtensionsPerDay = 2)
        ExtensionResult r3 = engine.requestExtension(15, "Third extension");
        assertFalse(r3.isGranted());
        assertTrue(r3.getMessage().contains("maximum 2 extensions"));

        // Test minute cap with a new engine configured with max 3 extensions but max 45 minutes
        config.setMaxExtensionsPerDay(3);
        config.setMaxExtensionMinutesPerDay(45);
        RestrictionEngine minuteCapEngine = new RestrictionEngine(config, usageDao, notificationService);

        ExtensionResult m1 = minuteCapEngine.requestExtension(30, "Ext 1");
        assertTrue(m1.isGranted());

        // Requesting another 30 mins would exceed 45 mins cap (30 + 30 = 60 > 45)
        ExtensionResult m2 = minuteCapEngine.requestExtension(30, "Ext 2");
        assertFalse(m2.isGranted());
        assertTrue(m2.getMessage().contains("cap exceeded"));
    }

    @Test
    void testMidnightDayRolloverResetsState() {
        engine.checkUsage(7200); // Trigger 100% threshold
        engine.requestExtension(30, "Testing");
        assertEquals(1, engine.getExtensionCountToday());
        assertTrue(engine.getFiredThresholds().contains(50));

        // Simulate midnight rollover to next day
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        engine.resetForNewDate(tomorrow);

        assertEquals(tomorrow, engine.getCurrentDate());
        assertEquals(0, engine.getExtensionCountToday());
        assertEquals(0, engine.getCumulativeExtensionMinutesToday());
        assertFalse(engine.isExtensionMode());
        assertTrue(engine.getFiredThresholds().isEmpty());
    }
}
