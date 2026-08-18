package com.screentime.notifications;

import com.screentime.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
        ConfigManager.getInstance().getConfig().setNotificationsEnabled(true);
    }

    @Test
    void testNotifyDispatchesToListeners() {
        List<String> received = new ArrayList<>();
        NotificationListener listener = (level, title, body) -> received.add(level + ":" + title + ":" + body);

        service.addListener(listener);
        service.notify(NotificationLevel.INFO, "Test Title", "Test Body");

        assertEquals(1, received.size());
        assertEquals("INFO:Test Title:Test Body", received.get(0));

        service.removeListener(listener);
        service.notify(NotificationLevel.WARNING, "Ignored Title", "Ignored Body");
        assertEquals(1, received.size());
    }

    @Test
    void testDisabledNotificationsSuppressesDispatch() {
        List<String> received = new ArrayList<>();
        service.addListener((level, title, body) -> received.add(title));

        ConfigManager.getInstance().getConfig().setNotificationsEnabled(false);
        service.notify(NotificationLevel.CRITICAL, "Critical Title", "Critical Body");

        assertTrue(received.isEmpty());

        ConfigManager.getInstance().getConfig().setNotificationsEnabled(true);
    }
}
