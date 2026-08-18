package com.screentime.notifications;

import com.screentime.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for delivering native cross-platform desktop notifications (Windows, macOS, Linux).
 * Wraps java.awt.SystemTray and TrayIcon with graceful fallbacks.
 */
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static volatile NotificationService instance;

    private final List<NotificationListener> listeners = new CopyOnWriteArrayList<>();
    private TrayIcon trayIcon;
    private boolean systemTraySupported = false;

    public NotificationService() {
        initSystemTray();
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) {
                    instance = new NotificationService();
                }
            }
        }
        return instance;
    }

    private void initSystemTray() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                logger.info("Running in headless environment. SystemTray is disabled.");
                systemTraySupported = false;
                return;
            }

            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = createDefaultTrayImage();
                trayIcon = new TrayIcon(image, "ScreenTime Monitor");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
                systemTraySupported = true;
                logger.info("SystemTray initialized successfully.");
            } else {
                logger.warn("SystemTray is not supported on this platform.");
                systemTraySupported = false;
            }
        } catch (Throwable t) {
            logger.warn("Could not initialize SystemTray (graceful fallback active): {}", t.getMessage());
            systemTraySupported = false;
        }
    }

    private Image createDefaultTrayImage() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(56, 189, 248)); // Sky blue circle
        g2.fillOval(1, 1, size - 2, size - 2);
        g2.setColor(Color.WHITE);
        g2.drawOval(1, 1, size - 2, size - 2);
        g2.dispose();
        return image;
    }

    /**
     * Dispatches a notification to the native OS desktop and all registered listeners.
     *
     * @param level Severity level (INFO, WARNING, CRITICAL).
     * @param title Title of the notification.
     * @param body Body text of the notification.
     */
    public void notify(NotificationLevel level, String title, String body) {
        if (!ConfigManager.getInstance().getConfig().isNotificationsEnabled()) {
            logger.debug("Notifications are disabled in configuration. Suppressed: [{}] {}", level, title);
            return;
        }

        logger.info("[NOTIFICATION - {}] {}: {}", level, title, body);

        // Native SystemTray notification
        if (systemTraySupported && trayIcon != null) {
            try {
                TrayIcon.MessageType messageType = switch (level) {
                    case INFO -> TrayIcon.MessageType.INFO;
                    case WARNING -> TrayIcon.MessageType.WARNING;
                    case CRITICAL -> TrayIcon.MessageType.ERROR;
                };
                trayIcon.displayMessage(title, body, messageType);
            } catch (Throwable t) {
                logger.warn("Failed to display TrayIcon message: {}", t.getMessage());
            }
        }

        // Notify all registered in-app listeners
        for (NotificationListener listener : listeners) {
            try {
                listener.onNotification(level, title, body);
            } catch (Throwable t) {
                logger.error("Error in NotificationListener callback", t);
            }
        }
    }

    public void addListener(NotificationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(NotificationListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public boolean isSystemTraySupported() {
        return systemTraySupported;
    }
}
