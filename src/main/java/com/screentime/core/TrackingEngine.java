package com.screentime.core;

import com.screentime.config.ConfigManager;
import com.screentime.data.UsageDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core tracking orchestrator.
 * Periodically polls active window and user idle state on a background thread,
 * managing session transitions, daily limits, SQLite persistence, and midnight day-rollovers.
 */
public class TrackingEngine {

    private static final Logger logger = LoggerFactory.getLogger(TrackingEngine.class);
    public static final int DEFAULT_POLL_INTERVAL_SECONDS = 5;
    public static final long SNAPSHOT_INTERVAL_MILLIS = 60_000L; // 60 seconds periodic snapshot

    private final WindowDetector windowDetector;
    private final IdleDetector idleDetector;
    private final UsageDao usageDao;
    private final int pollIntervalSeconds;
    private volatile int idleThresholdSeconds;

    private final List<TrackingListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicLong todayActiveSeconds = new AtomicLong(0);

    private ScheduledExecutorService scheduler;
    private TrackingSession currentSession;
    private WindowInfo currentWindow;
    private ActivityState currentState = ActivityState.IDLE;
    private LocalDate currentTrackingDate;
    private long lastPollTimestampMillis = 0;
    private long lastSnapshotTimestampMillis = 0;

    public TrackingEngine() {
        this(
                WindowDetectorFactory.createDetector(),
                new IdleDetector(),
                new UsageDao(),
                DEFAULT_POLL_INTERVAL_SECONDS,
                ConfigManager.getInstance().getConfig().getIdleThresholdSeconds()
        );
    }

    public TrackingEngine(WindowDetector windowDetector, IdleDetector idleDetector, int pollIntervalSeconds, int idleThresholdSeconds) {
        this(windowDetector, idleDetector, new UsageDao(), pollIntervalSeconds, idleThresholdSeconds);
    }

    public TrackingEngine(WindowDetector windowDetector, IdleDetector idleDetector, UsageDao usageDao, int pollIntervalSeconds, int idleThresholdSeconds) {
        this.windowDetector = Objects.requireNonNull(windowDetector, "WindowDetector must not be null");
        this.idleDetector = Objects.requireNonNull(idleDetector, "IdleDetector must not be null");
        this.usageDao = Objects.requireNonNull(usageDao, "UsageDao must not be null");
        this.pollIntervalSeconds = Math.max(1, pollIntervalSeconds);
        this.idleThresholdSeconds = Math.max(5, idleThresholdSeconds);
        this.currentWindow = new WindowInfo("System", "Initializing", 0, Instant.now());
        this.currentTrackingDate = LocalDate.now();

        // Load today's initial active seconds from database
        loadTodayUsageFromDatabase();

        logger.info("TrackingEngine initialized with detector: '{}', poll interval: {}s, idle threshold: {}s, initial today usage: {}s",
                windowDetector.getDetectorName(), this.pollIntervalSeconds, this.idleThresholdSeconds, todayActiveSeconds.get());
    }

    /**
     * Loads today's persisted screen time from SQLite.
     */
    public void loadTodayUsageFromDatabase() {
        long recordedSeconds = usageDao.getTodayUsageSeconds();
        todayActiveSeconds.set(recordedSeconds);
    }

    /**
     * Starts the tracking scheduler and idle detection hooks.
     */
    public synchronized void start() {
        start(true);
    }

    /**
     * Starts the tracking engine, optionally launching the background scheduler.
     *
     * @param enableBackgroundScheduler If true, runs periodic polls via ScheduledExecutorService.
     */
    public synchronized void start(boolean enableBackgroundScheduler) {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting TrackingEngine (scheduler={})...", enableBackgroundScheduler);
            idleDetector.start();
            lastPollTimestampMillis = System.currentTimeMillis();
            lastSnapshotTimestampMillis = System.currentTimeMillis();
            if (currentTrackingDate == null) {
                currentTrackingDate = LocalDate.now();
            }

            if (enableBackgroundScheduler) {
                scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread thread = new Thread(r, "ScreenTime-Tracker-Worker");
                    thread.setDaemon(true);
                    return thread;
                });
                scheduler.scheduleAtFixedRate(this::poll, pollIntervalSeconds, pollIntervalSeconds, TimeUnit.SECONDS);
            }
            logger.info("TrackingEngine started successfully.");
        }
    }

    /**
     * Stops the tracking engine, closes any open session, persists snapshots, and cleans up resources.
     */
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping TrackingEngine...");

            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                scheduler = null;
            }

            idleDetector.stop();

            // Close active session on exit
            if (currentSession != null && currentSession.isActive()) {
                currentSession.close(Instant.now());
                notifySessionClosed(currentSession);
                currentSession = null;
            }

            // Save final snapshot
            usageDao.savePeriodicDailyActiveSnapshot(currentTrackingDate, todayActiveSeconds.get());

            ActivityState previousState = currentState;
            currentState = ActivityState.IDLE;
            if (previousState != ActivityState.IDLE) {
                notifyStateChanged(previousState, ActivityState.IDLE);
            }

            logger.info("TrackingEngine stopped. Today's active seconds: {}", todayActiveSeconds.get());
        }
    }

    /**
     * Pauses screen time tracking without terminating scheduler.
     */
    public void pause() {
        if (paused.compareAndSet(false, true)) {
            logger.info("TrackingEngine paused.");
            if (currentSession != null && currentSession.isActive()) {
                currentSession.close(Instant.now());
                notifySessionClosed(currentSession);
                currentSession = null;
            }
            usageDao.savePeriodicDailyActiveSnapshot(currentTrackingDate, todayActiveSeconds.get());

            ActivityState prev = currentState;
            currentState = ActivityState.IDLE;
            if (prev != ActivityState.IDLE) {
                notifyStateChanged(prev, ActivityState.IDLE);
            }
        }
    }

    /**
     * Resumes screen time tracking.
     */
    public void resume() {
        if (paused.compareAndSet(true, false)) {
            logger.info("TrackingEngine resumed.");
            lastPollTimestampMillis = System.currentTimeMillis();
            lastSnapshotTimestampMillis = System.currentTimeMillis();
            idleDetector.recordInputEvent();
        }
    }

    public boolean isPaused() {
        return paused.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Periodic poll execution. Package-private to allow direct unit testing without scheduling delay.
     */
    synchronized void poll() {
        if (!running.get() || paused.get()) {
            return;
        }

        try {
            LocalDate nowLocalDate = LocalDate.now();
            if (!nowLocalDate.equals(currentTrackingDate)) {
                handleDayRollover(nowLocalDate);
            }

            long now = System.currentTimeMillis();
            long elapsedSeconds = lastPollTimestampMillis > 0 ? Math.max(1, (now - lastPollTimestampMillis) / 1000L) : pollIntervalSeconds;
            lastPollTimestampMillis = now;

            boolean idle = idleDetector.isIdle(idleThresholdSeconds);

            if (idle) {
                handleIdleState();
            } else {
                handleActiveState(elapsedSeconds);
            }

            // Periodic snapshot check (every 60 seconds)
            if (now - lastSnapshotTimestampMillis >= SNAPSHOT_INTERVAL_MILLIS) {
                usageDao.savePeriodicDailyActiveSnapshot(currentTrackingDate, todayActiveSeconds.get());
                lastSnapshotTimestampMillis = now;
            }
        } catch (Throwable t) {
            logger.error("Unexpected error during tracking poll cycle (tracking continues): {}", t.getMessage(), t);
        }
    }

    /**
     * Handles midnight day-rollover cleanly without dropping or double-counting seconds.
     */
    void handleDayRollover(LocalDate newDate) {
        logger.info("Midnight rollover detected! Transitioning from date {} to {}", currentTrackingDate, newDate);

        // Save previous day's snapshot
        usageDao.savePeriodicDailyActiveSnapshot(currentTrackingDate, todayActiveSeconds.get());

        if (currentSession != null && currentSession.isActive()) {
            // Close session at previous day's end
            Instant midnightInstant = currentTrackingDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            currentSession.close(midnightInstant);
            logger.info("Closed previous day's session at midnight boundary: {}", currentSession);
            notifySessionClosed(currentSession);

            // Start new session for new day at midnight
            currentSession = new TrackingSession(currentSession.getAppName(), currentSession.getWindowTitle(), midnightInstant);
        }

        // Reset today's running total to the new day's accumulated time in database (usually 0)
        long newDaySeconds = usageDao.getActiveSecondsForDate(newDate);
        todayActiveSeconds.set(newDaySeconds);
        currentTrackingDate = newDate;

        logger.info("Day rollover complete. Tracking under new date: {}, initial active seconds: {}", newDate, newDaySeconds);
    }

    private void handleIdleState() {
        if (currentSession != null && currentSession.isActive()) {
            // Close active session at the time of last detected input
            Instant lastInputInstant = Instant.ofEpochMilli(idleDetector.getLastInputTimestampMillis());
            if (lastInputInstant.isBefore(currentSession.getStartTime())) {
                lastInputInstant = currentSession.getStartTime();
            }
            currentSession.close(lastInputInstant);
            logger.info("User went idle. Closed session: {}", currentSession);
            notifySessionClosed(currentSession);
            currentSession = null;
        }

        if (currentState != ActivityState.IDLE) {
            ActivityState previous = currentState;
            currentState = ActivityState.IDLE;
            notifyStateChanged(previous, ActivityState.IDLE);
        }
    }

    private void handleActiveState(long elapsedSeconds) {
        WindowInfo activeWindow = windowDetector.getActiveWindow();
        if (activeWindow == null) {
            activeWindow = new WindowInfo("Unknown", "Active Window", 0, Instant.now());
        }

        boolean windowChanged = !Objects.equals(activeWindow.getAppName(), currentWindow.getAppName()) ||
                !Objects.equals(activeWindow.getWindowTitle(), currentWindow.getWindowTitle());

        WindowInfo previousWindow = this.currentWindow;
        this.currentWindow = activeWindow;

        if (windowChanged) {
            notifyWindowChanged(activeWindow);
        }

        if (currentState != ActivityState.ACTIVE) {
            ActivityState previousState = currentState;
            currentState = ActivityState.ACTIVE;
            notifyStateChanged(previousState, ActivityState.ACTIVE);
        }

        // Manage session lifecycle
        if (currentSession == null) {
            // Start fresh session
            currentSession = new TrackingSession(activeWindow.getAppName(), activeWindow.getWindowTitle(), Instant.now());
            logger.debug("Started new tracking session: {}", currentSession);
        } else if (!isSameApplication(currentSession.getAppName(), activeWindow.getAppName())) {
            // App switched: close previous session and start new
            currentSession.close(Instant.now());
            logger.debug("Application switched from '{}' to '{}'. Closed session: {}",
                    currentSession.getAppName(), activeWindow.getAppName(), currentSession);
            notifySessionClosed(currentSession);

            currentSession = new TrackingSession(activeWindow.getAppName(), activeWindow.getWindowTitle(), Instant.now());
            logger.debug("Started new tracking session for switched app: {}", currentSession);
        }

        // Increment running active seconds
        long newTotal = todayActiveSeconds.addAndGet(elapsedSeconds);
        notifyActiveSecondsTick(newTotal);
    }

    private boolean isSameApplication(String app1, String app2) {
        if (app1 == null && app2 == null) return true;
        if (app1 == null || app2 == null) return false;
        return app1.trim().equalsIgnoreCase(app2.trim());
    }

    // --- Listener Management & Persistence Dispatch ---

    public void addListener(TrackingListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(TrackingListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifySessionClosed(TrackingSession session) {
        // Automatically persist closed session to database
        try {
            usageDao.recordSession(session);
        } catch (Throwable t) {
            logger.error("Failed to auto-persist closed session in UsageDao", t);
        }

        for (TrackingListener listener : listeners) {
            try {
                listener.onSessionClosed(session);
            } catch (Throwable t) {
                logger.error("Error invoking onSessionClosed listener", t);
            }
        }
    }

    private void notifyStateChanged(ActivityState oldState, ActivityState newState) {
        for (TrackingListener listener : listeners) {
            try {
                listener.onStateChanged(oldState, newState);
            } catch (Throwable t) {
                logger.error("Error invoking onStateChanged listener", t);
            }
        }
    }

    private void notifyWindowChanged(WindowInfo windowInfo) {
        for (TrackingListener listener : listeners) {
            try {
                listener.onWindowChanged(windowInfo);
            } catch (Throwable t) {
                logger.error("Error invoking onWindowChanged listener", t);
            }
        }
    }

    private void notifyActiveSecondsTick(long totalActiveSeconds) {
        for (TrackingListener listener : listeners) {
            try {
                listener.onActiveSecondsTick(totalActiveSeconds);
            } catch (Throwable t) {
                logger.error("Error invoking onActiveSecondsTick listener", t);
            }
        }
    }

    // --- Getters and Setters ---

    public long getTodayActiveSeconds() {
        return todayActiveSeconds.get();
    }

    public void setTodayActiveSeconds(long seconds) {
        todayActiveSeconds.set(Math.max(0, seconds));
    }

    public TrackingSession getCurrentSession() {
        return currentSession;
    }

    public WindowInfo getCurrentWindow() {
        return currentWindow;
    }

    public ActivityState getCurrentState() {
        return currentState;
    }

    public LocalDate getCurrentTrackingDate() {
        return currentTrackingDate;
    }

    void setCurrentTrackingDate(LocalDate date) {
        this.currentTrackingDate = date;
    }

    public int getIdleThresholdSeconds() {
        return idleThresholdSeconds;
    }

    public void setIdleThresholdSeconds(int idleThresholdSeconds) {
        this.idleThresholdSeconds = Math.max(5, idleThresholdSeconds);
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public WindowDetector getWindowDetector() {
        return windowDetector;
    }

    public IdleDetector getIdleDetector() {
        return idleDetector;
    }

    public UsageDao getUsageDao() {
        return usageDao;
    }
}
