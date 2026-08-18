package com.screentime.ui;

import com.screentime.core.TrackingEngine;
import com.screentime.restriction.RestrictionEngine;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Manages the native OS System Tray icon and context menu.
 */
public class TrayMenuManager {

    private static final Logger logger = LoggerFactory.getLogger(TrayMenuManager.class);

    private final Stage primaryStage;
    private final TrackingEngine trackingEngine;
    private final RestrictionEngine restrictionEngine;
    private final MainViewController mainViewController;
    private final Runnable quitHandler;

    private MenuItem pauseResumeItem;
    private TrayIcon trayIcon;

    public TrayMenuManager(Stage primaryStage,
                           TrackingEngine trackingEngine,
                           RestrictionEngine restrictionEngine,
                           MainViewController mainViewController,
                           Runnable quitHandler) {
        this.primaryStage = primaryStage;
        this.trackingEngine = trackingEngine;
        this.restrictionEngine = restrictionEngine;
        this.mainViewController = mainViewController;
        this.quitHandler = quitHandler;
    }

    public void setupTray() {
        if (!SystemTray.isSupported()) {
            logger.warn("SystemTray is not supported on this platform.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image iconImage = createTrayIconImage();

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Open Dashboard");
            openItem.addActionListener(e -> showDashboard());
            popup.add(openItem);

            pauseResumeItem = new MenuItem("Pause Tracking");
            pauseResumeItem.addActionListener(e -> togglePauseResume());
            popup.add(pauseResumeItem);

            MenuItem extItem = new MenuItem("Request Extension...");
            extItem.addActionListener(e -> Platform.runLater(() -> ExtensionDialog.show(restrictionEngine, () -> {
                if (mainViewController != null) mainViewController.refreshTodayView();
            })));
            popup.add(extItem);

            MenuItem settingsItem = new MenuItem("Settings");
            settingsItem.addActionListener(e -> {
                showDashboard();
                if (mainViewController != null) {
                    Platform.runLater(mainViewController::showSettingsTab);
                }
            });
            popup.add(settingsItem);

            popup.addSeparator();

            MenuItem quitItem = new MenuItem("Quit ScreenTime Monitor");
            quitItem.addActionListener(e -> {
                if (quitHandler != null) {
                    quitHandler.run();
                } else {
                    Platform.exit();
                    System.exit(0);
                }
            });
            popup.add(quitItem);

            trayIcon = new TrayIcon(iconImage, "ScreenTime Monitor", popup);
            trayIcon.setImageAutoSize(true);

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() >= 1 && e.getButton() == MouseEvent.BUTTON1) {
                        showDashboard();
                    }
                }
            });

            tray.add(trayIcon);
            logger.info("System Tray menu successfully registered.");
        } catch (Throwable t) {
            logger.warn("Could not register system tray icon: {}", t.getMessage());
        }
    }

    public void showDashboard() {
        Platform.runLater(() -> {
            if (primaryStage != null) {
                primaryStage.show();
                primaryStage.toFront();
                primaryStage.requestFocus();
            }
        });
    }

    private void togglePauseResume() {
        if (trackingEngine == null) return;

        if (trackingEngine.isPaused()) {
            trackingEngine.resume();
            if (pauseResumeItem != null) pauseResumeItem.setLabel("Pause Tracking");
        } else {
            trackingEngine.pause();
            if (pauseResumeItem != null) pauseResumeItem.setLabel("Resume Tracking");
        }
        if (mainViewController != null) {
            Platform.runLater(mainViewController::refreshTrackingStatusBadge);
        }
    }

    private Image createTrayIconImage() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(56, 189, 248)); // Sky blue
        g2.fillOval(1, 1, size - 2, size - 2);
        g2.setColor(Color.WHITE);
        g2.drawOval(1, 1, size - 2, size - 2);
        g2.dispose();
        return image;
    }
}
