package com.screentime.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdleDetectorTest {

    @Test
    void testIdleDetection() {
        IdleDetector idleDetector = new IdleDetector();
        idleDetector.recordInputEvent();

        // Right after input, shouldn't be idle for threshold >= 1s
        assertFalse(idleDetector.isIdle(5));
        assertTrue(idleDetector.getIdleDurationSeconds() >= 0);
    }
}
