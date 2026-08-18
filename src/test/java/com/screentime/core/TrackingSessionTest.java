package com.screentime.core;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TrackingSessionTest {
    @Test void testDuration() {
        LocalDateTime s = LocalDateTime.of(2026, 8, 18, 10, 0, 0);
        LocalDateTime e = LocalDateTime.of(2026, 8, 18, 10, 5, 0);
        assertEquals(300, new TrackingSession("A", "W", s, e, false).getDurationSeconds());
    }
}
