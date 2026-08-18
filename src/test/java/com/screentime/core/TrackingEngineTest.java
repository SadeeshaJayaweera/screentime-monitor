package com.screentime.core;
import com.screentime.data.DatabaseManager;
import com.screentime.data.UsageDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class TrackingEngineTest {
    private TrackingEngine engine;
    @BeforeEach void setUp(@TempDir Path tempDir) {
        engine = new TrackingEngine(() -> new WindowInfo("Test", "T", 1), new IdleDetector(), new UsageDao(new DatabaseManager(tempDir.resolve("e.db"))), 1, 5);
    }
    @Test void testActiveTracking() {
        engine.start();
        engine.poll();
        assertEquals(ActivityState.ACTIVE, engine.getCurrentState());
    }
}
