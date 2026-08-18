package com.screentime;

import com.screentime.config.ConfigManager;
import com.screentime.core.*;
import com.screentime.data.DatabaseManager;
import com.screentime.restriction.RestrictionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Bootstrap entry point for fat JAR execution without module-path flags.
 * Also supports command-line diagnostic flags (--info, --version, --check, --debug-tracking).
 */
public class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);
    public static final String VERSION = "1.0.0-SNAPSHOT";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        logger.info("=================================================");
        logger.info("ScreenTime Monitor v{}", VERSION);
        logger.info("OS: {} ({}, {})", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
        logger.info("Java: {} ({})", System.getProperty("java.version"), System.getProperty("java.vendor"));
        logger.info("=================================================");

        // Check for debug tracking smoke test flag
        if (Arrays.asList(args).contains("--debug-tracking")) {
            runDebugTrackingSmokeTest();
            return;
        }

        // Diagnostic / CLI flags support
        boolean cliMode = Arrays.stream(args).anyMatch(arg ->
                arg.equalsIgnoreCase("--info") ||
                arg.equalsIgnoreCase("--version") ||
                arg.equalsIgnoreCase("--check") ||
                arg.equalsIgnoreCase("-v") ||
                arg.equalsIgnoreCase("-h") ||
                arg.equalsIgnoreCase("--help"));

        if (cliMode) {
            ConfigManager configManager = ConfigManager.getInstance();
            System.out.println("ScreenTime Monitor v" + VERSION);
            System.out.println("App Data Directory: " + configManager.getAppDataDir());
            System.out.println("Config File:        " + configManager.getConfigFilePath());
            System.out.println("Database File:      " + configManager.getDatabasePath());
            System.out.println("Config Loaded:      " + configManager.getConfig());
            System.out.println("Gemini API Key:     " + (configManager.getEffectiveGeminiApiKey().isBlank() ? "Not configured" : "[CONFIGURED]"));

            // Initialize DB check
            DatabaseManager.getInstance();
            System.out.println("Database Status:    Initialized SQLite schema successfully.");
            System.out.println("Startup check completed successfully.");
            return;
        }

        // Launch JavaFX Application
        try {
            Main.main(args);
        } catch (Throwable t) {
            logger.error("Failed to launch JavaFX application: {}", t.getMessage(), t);
            System.err.println("ScreenTime Monitor error: " + t.getMessage());
        }
    }

    /**
     * Diagnostic smoke test mode that runs the tracking engine for 30 seconds,
     * printing live window changes, idle transitions, closed sessions, and running totals.
     */
    private static void runDebugTrackingSmokeTest() {
        System.out.println("\n=======================================================");
        System.out.println("🔍 STARTING TRACKING ENGINE SMOKE TEST (30 seconds)");
        System.out.println("   Poll Interval: 5s | Idle Threshold: 15s (debug mode)");
        System.out.println("   Switch windows or leave mouse/keyboard untouched to test!");
        System.out.println("=======================================================\n");

        WindowDetector detector = WindowDetectorFactory.createDetector();
        IdleDetector idleDetector = new IdleDetector();
        // Use 15s idle threshold for quicker smoke test feedback
        TrackingEngine engine = new TrackingEngine(detector, idleDetector, 5, 15);
        RestrictionEngine restrictionEngine = new RestrictionEngine();
        engine.addListener(restrictionEngine);

        engine.addListener(new TrackingListener() {
            @Override
            public void onSessionClosed(TrackingSession session) {
                System.out.printf("[%s] 📦 [SESSION CLOSED] App: '%s' | Window: '%s' | Duration: %ds%n",
                        LocalTime.now().format(TIME_FMT),
                        session.getAppName(),
                        session.getWindowTitle(),
                        session.getDurationSeconds());
            }

            @Override
            public void onStateChanged(ActivityState oldState, ActivityState newState) {
                System.out.printf("[%s] ⚡ [STATE CHANGE] %s -> %s (Idle duration: %ds)%n",
                        LocalTime.now().format(TIME_FMT),
                        oldState, newState, idleDetector.getIdleDurationSeconds());
            }

            @Override
            public void onWindowChanged(WindowInfo windowInfo) {
                System.out.printf("[%s] 🪟 [WINDOW CHANGED] App: '%s' (PID %d) | Title: '%s'%n",
                        LocalTime.now().format(TIME_FMT),
                        windowInfo.getAppName(),
                        windowInfo.getProcessId(),
                        windowInfo.getWindowTitle());
            }

            @Override
            public void onActiveSecondsTick(long totalActiveSecondsToday) {
                System.out.printf("[%s] ⏱️ [TICK] Today's Active Screen Time: %ds (Current App: '%s')%n",
                        LocalTime.now().format(TIME_FMT),
                        totalActiveSecondsToday,
                        engine.getCurrentWindow().getAppName());
            }
        });

        engine.start();

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            engine.stop();
            latch.countDown();
        }));

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        engine.stop();
        System.out.println("\n=======================================================");
        System.out.println("✅ SMOKE TEST COMPLETED");
        System.out.printf("   Total Active Seconds Tracked: %ds%n", engine.getTodayActiveSeconds());
        System.out.println("=======================================================\n");
    }
}
