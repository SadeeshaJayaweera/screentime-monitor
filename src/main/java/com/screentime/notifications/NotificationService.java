package com.screentime.notifications;

public class NotificationService {
    private static volatile NotificationService instance;
    public static NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }
}
