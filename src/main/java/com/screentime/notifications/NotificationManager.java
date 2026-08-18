package com.screentime.notifications;

import com.screentime.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Manages system tray integration and desktop notifications across platforms.
 */
public class NotificationManager {

    private static final Logger logger = LoggerFactory.getLogger(NotificationManager.class);
    private static volatile NotificationManager instance;

    private TrayIcon trayIcon;
    private boolean systemTraySupported = false;

    public NotificationManager() {
        initSystemTray();
    }

    public static NotificationManager getInstance() {
        if (instance == null) {
            synchronized (NotificationManager.class) {
                if (instance == null) {
                    instance = new NotificationManager();
                }
            }
        }
        return instance;
    }

    private void initSystemTray() {
        try {
            if (SystemTray.isSupported()) {
                systemTraySupported = true;
                logger.info("System tray is supported on this platform.");
            } else {
                logger.warn("System tray is not supported on this platform.");
            }
        } catch (Throwable t) {
            logger.warn("Could not check system tray support in headless or restricted environment: {}", t.getMessage());
            systemTraySupported = false;
        }
    }

    /**
     * Displays a desktop notification alert.
     */
    public void showAlert(String title, String message, AlertLevel level) {
        NotificationLevel notifLevel = switch (level) {
            case INFO -> NotificationLevel.INFO;
            case BREAK_REMINDER, WARNING_APPROACHING_LIMIT -> NotificationLevel.WARNING;
            case LIMIT_REACHED -> NotificationLevel.CRITICAL;
        };
        NotificationService.getInstance().notify(notifLevel, title, message);
    }

    public boolean isSystemTraySupported() {
        return systemTraySupported;
    }
}
