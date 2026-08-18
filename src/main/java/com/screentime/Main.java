package com.screentime;

import com.screentime.ai.HealthSuggestionService;
import com.screentime.config.ConfigManager;
import com.screentime.core.TrackingEngine;
import com.screentime.data.DatabaseManager;
import com.screentime.data.UsageDao;
import com.screentime.notifications.NotificationService;
import com.screentime.restriction.RestrictionEngine;
import com.screentime.ui.MainViewController;
import com.screentime.ui.TrayMenuManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Main JavaFX Application lifecycle entry point.
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private TrackingEngine trackingEngine;
    private UsageDao usageDao;
    private RestrictionEngine restrictionEngine;
    private HealthSuggestionService healthSuggestionService;
    private TrayMenuManager trayMenuManager;
    private MainViewController mainViewController;

    @Override
    public void init() throws Exception {
        logger.info("Initializing ScreenTime Monitor application...");

        // Ensure config is loaded
        ConfigManager configManager = ConfigManager.getInstance();
        logger.info("Application Data Directory: {}", configManager.getAppDataDir());
        logger.info("Configuration File: {}", configManager.getConfigFilePath());
        logger.info("Database File: {}", configManager.getDatabasePath());

        // Initialize SQLite database
        DatabaseManager.getInstance();

        // Initialize core DAOs and engines
        usageDao = new UsageDao();
        restrictionEngine = new RestrictionEngine(new com.screentime.restriction.RestrictionConfig(), usageDao, NotificationService.getInstance());
        healthSuggestionService = new HealthSuggestionService();

        // Initialize and start tracking engine
        trackingEngine = new TrackingEngine();
        trackingEngine.addListener(restrictionEngine);
        trackingEngine.start();

        // Register JVM shutdown hook for clean resource release
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM Shutdown hook triggered: stopping TrackingEngine cleanly...");
            if (trackingEngine != null) {
                trackingEngine.stop();
            }
        }, "ScreenTime-Shutdown-Hook"));
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting JavaFX UI...");

        // Prevent JVM from exiting when dashboard window is closed (minimize to system tray)
        Platform.setImplicitExit(false);

        try {
            URL fxmlUrl = getClass().getResource("/com/screentime/ui/main-view.fxml");
            if (fxmlUrl == null) {
                throw new IllegalStateException("Cannot find /com/screentime/ui/main-view.fxml in classpath");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            mainViewController = loader.getController();
            mainViewController.injectServices(trackingEngine, usageDao, restrictionEngine, healthSuggestionService);

            Scene scene = new Scene(root, 940, 660);
            primaryStage.setTitle("ScreenTime Monitor");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(580);

            try {
                var iconStream = getClass().getResourceAsStream("/com/screentime/ui/assets/icon.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
                }
            } catch (Exception e) {
                logger.warn("Could not load application window icon: {}", e.getMessage());
            }

            // Handle window close request: hide to tray rather than exiting
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                primaryStage.hide();
                logger.info("Dashboard minimized to System Tray.");
            });

            // Initialize System Tray Menu
            trayMenuManager = new TrayMenuManager(
                    primaryStage,
                    trackingEngine,
                    restrictionEngine,
                    mainViewController,
                    this::quitApplication
            );
            trayMenuManager.setupTray();

            // Check for first-run onboarding
            boolean onboardingComplete = ConfigManager.getInstance().getConfig().isOnboardingCompleted();
            if (!onboardingComplete) {
                logger.info("First run detected: launching Onboarding Wizard...");
                com.screentime.ui.onboarding.OnboardingWizard.show(null, restrictionEngine, () -> {
                    logger.info("Onboarding completed: transitioning to Main Dashboard.");
                    mainViewController.refreshTodayView();
                    primaryStage.show();
                });
            } else {
                // If SystemTray is supported, start primarily in system tray (window opens via 'Open Dashboard')
                if (java.awt.SystemTray.isSupported()) {
                    logger.info("ScreenTime Monitor started in System Tray. Open dashboard from tray icon.");
                } else {
                    primaryStage.show();
                    logger.info("SystemTray not supported; showing Dashboard window directly.");
                }
            }

        } catch (IOException e) {
            logger.error("Failed to load JavaFX FXML view", e);
        }
    }

    public void quitApplication() {
        logger.info("Quitting ScreenTime Monitor...");
        try {
            if (trackingEngine != null) {
                trackingEngine.stop();
            }
        } catch (Throwable t) {
            logger.warn("Error while stopping tracking engine: {}", t.getMessage());
        }
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void stop() throws Exception {
        if (trackingEngine != null) {
            trackingEngine.stop();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
