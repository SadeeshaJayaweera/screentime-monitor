package com.screentime.notifications;

/**
 * Listener interface for notifications emitted by NotificationService.
 */
public interface NotificationListener {

    /**
     * Fired whenever a notification is dispatched.
     *
     * @param level Severity level (INFO, WARNING, CRITICAL).
     * @param title Title of the notification.
     * @param body Body content of the notification.
     */
    void onNotification(NotificationLevel level, String title, String body);
}
