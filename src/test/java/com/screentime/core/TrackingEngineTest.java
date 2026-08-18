package com.screentime.core;

import com.screentime.data.DatabaseManager;
import com.screentime.data.UsageDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TrackingEngineTest {

    @TempDir
    Path tempDir;

    private TestWindowDetector mockWindowDetector;
    private TestIdleDetector mockIdleDetector;
    private UsageDao testUsageDao;
    private TrackingEngine engine;
    private List<TrackingSession> closedSessions;
    private List<ActivityState> stateTransitions;
    private List<Long> ticks;

    @BeforeEach
    void setUp() {
        mockWindowDetector = new TestWindowDetector();
        mockIdleDetector = new TestIdleDetector();
        testUsageDao = new UsageDao(new DatabaseManager(tempDir.resolve("test_tracking.db")));
        engine = new TrackingEngine(mockWindowDetector, mockIdleDetector, testUsageDao, 5, 60);

        closedSessions = new ArrayList<>();
        stateTransitions = new ArrayList<>();
        ticks = new ArrayList<>();

        engine.addListener(new TrackingListener() {
            @Override
            public void onSessionClosed(TrackingSession session) {
                closedSessions.add(session);
            }

            @Override
            public void onStateChanged(ActivityState oldState, ActivityState newState) {
                stateTransitions.add(newState);
            }

            @Override
            public void onActiveSecondsTick(long totalActiveSecondsToday) {
                ticks.add(totalActiveSecondsToday);
            }
        });
    }

    @Test
    void testActiveTrackingAndAccumulation() {
        mockIdleDetector.setIdle(false);
        mockWindowDetector.setActiveWindow(new WindowInfo("Visual Studio Code", "TrackingEngine.java", 1234));

        engine.start(false);
        engine.poll();

        assertEquals(ActivityState.ACTIVE, engine.getCurrentState());
        assertNotNull(engine.getCurrentSession());
        assertEquals("Visual Studio Code", engine.getCurrentSession().getAppName());
        assertTrue(engine.getTodayActiveSeconds() > 0);
        assertTrue(closedSessions.isEmpty());

        engine.stop();
        // Stopping should close the active session
        assertEquals(1, closedSessions.size());
        assertEquals("Visual Studio Code", closedSessions.get(0).getAppName());
    }

    @Test
    void testAppSwitchingEmitsClosedSession() {
        mockIdleDetector.setIdle(false);
        mockWindowDetector.setActiveWindow(new WindowInfo("IntelliJ IDEA", "Main.java", 100));

        engine.start(false);
        engine.poll();

        assertEquals("IntelliJ IDEA", engine.getCurrentSession().getAppName());
        assertEquals(0, closedSessions.size());

        // Switch to Chrome
        mockWindowDetector.setActiveWindow(new WindowInfo("Google Chrome", "Google Search", 200));
        engine.poll();

        // Previous session for IntelliJ IDEA should be closed and emitted
        assertEquals(1, closedSessions.size());
        assertEquals("IntelliJ IDEA", closedSessions.get(0).getAppName());
        assertEquals("Google Chrome", engine.getCurrentSession().getAppName());

        engine.stop();
        assertEquals(2, closedSessions.size());
        assertEquals("Google Chrome", closedSessions.get(1).getAppName());
    }

    @Test
    void testIdleTransitionStopsAccumulationAndClosesSession() {
        mockIdleDetector.setIdle(false);
        mockWindowDetector.setActiveWindow(new WindowInfo("Slack", "General Channel", 300));

        engine.start(false);
        engine.poll();

        assertEquals(ActivityState.ACTIVE, engine.getCurrentState());
        long activeSecondsBeforeIdle = engine.getTodayActiveSeconds();

        // User goes idle
        mockIdleDetector.setIdle(true);
        engine.poll();

        assertEquals(ActivityState.IDLE, engine.getCurrentState());
        assertNull(engine.getCurrentSession());
        assertEquals(1, closedSessions.size());
        assertEquals("Slack", closedSessions.get(0).getAppName());

        // Further idle polls should not increment active seconds
        engine.poll();
        assertEquals(activeSecondsBeforeIdle, engine.getTodayActiveSeconds());

        // User becomes active again
        mockIdleDetector.setIdle(false);
        mockWindowDetector.setActiveWindow(new WindowInfo("Figma", "Design Specs", 400));
        engine.poll();

        assertEquals(ActivityState.ACTIVE, engine.getCurrentState());
        assertEquals("Figma", engine.getCurrentSession().getAppName());
        assertTrue(engine.getTodayActiveSeconds() > activeSecondsBeforeIdle);

        engine.stop();
    }

    @Test
    void testPauseAndResume() {
        mockIdleDetector.setIdle(false);
        mockWindowDetector.setActiveWindow(new WindowInfo("Terminal", "zsh", 500));

        engine.start(false);
        engine.poll();

        long activeBeforePause = engine.getTodayActiveSeconds();
        engine.pause();
        assertTrue(engine.isPaused());
        assertEquals(1, closedSessions.size());

        // Poll while paused should do nothing
        engine.poll();
        assertEquals(activeBeforePause, engine.getTodayActiveSeconds());

        engine.resume();
        assertFalse(engine.isPaused());

        engine.stop();
    }

    // --- Mock Helpers ---

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
        private final AtomicBoolean idle = new AtomicBoolean(false);

        public void setIdle(boolean isIdle) {
            idle.set(isIdle);
        }

        @Override
        public boolean isIdle(int thresholdSeconds) {
            return idle.get();
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}
    }
}
