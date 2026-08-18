package com.screentime.restriction;
import com.screentime.data.DatabaseManager;
import com.screentime.data.UsageDao;
import com.screentime.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RestrictionEngineTest {
    private RestrictionEngine engine;
    @BeforeEach void setUp(@TempDir Path tempDir) {
        RestrictionConfig c = new RestrictionConfig();
        c.setDailyLimitMinutes(120);
        c.setMaxExtensionsPerDay(2);
        c.setMaxExtensionMinutesPerDay(60);
        engine = new RestrictionEngine(c, new UsageDao(new DatabaseManager(tempDir.resolve("r.db"))), NotificationService.getInstance());
    }
    @Test void testExtensionAllowedWithinCaps() {
        assertTrue(engine.requestExtension(30, "Test").granted());
    }
}
