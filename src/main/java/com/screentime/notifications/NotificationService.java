package com.screentime.notifications;
import java.awt.TrayIcon;

public class NotificationService {
    private static volatile NotificationService instance;
    private TrayIcon trayIcon;
    public void setTrayIcon(TrayIcon icon) { this.trayIcon = icon; }
    public static NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }
}
