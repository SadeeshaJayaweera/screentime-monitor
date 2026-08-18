package com.screentime.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TrackingSessionTest {

    @Test
    void testSessionLifecycleAndDuration() throws InterruptedException {
        Instant start = Instant.now().minusSeconds(10);
        TrackingSession session = new TrackingSession("Google Chrome", "GitHub - Dashboard", start);

        assertTrue(session.isActive());
        assertFalse(session.isClosed());
        assertEquals("Google Chrome", session.getAppName());
        assertEquals("GitHub - Dashboard", session.getWindowTitle());
        assertTrue(session.getDurationSeconds() >= 10);

        Instant end = start.plusSeconds(15);
        session.close(end);

        assertTrue(session.isClosed());
        assertFalse(session.isActive());
        assertEquals(15, session.getDurationSeconds());
        assertEquals(end, session.getEndTime());
    }

    @Test
    void testNullOrBlankHandling() {
        TrackingSession session = new TrackingSession(null, "");
        assertEquals("Unknown", session.getAppName());
        assertEquals("Untitled", session.getWindowTitle());
        assertNotNull(session.getStartTime());
    }
}
