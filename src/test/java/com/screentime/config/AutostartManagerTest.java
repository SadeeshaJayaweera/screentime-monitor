package com.screentime.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutostartManagerTest {

    @Test
    void testAutostartManagerInstance() {
        AutostartManager manager = AutostartManager.getInstance();
        assertNotNull(manager);
    }

    @Test
    void testResolveExecutableCommand() {
        AutostartManager manager = AutostartManager.getInstance();
        String command = manager.resolveExecutableCommand();
        assertNotNull(command);
        assertFalse(command.isBlank());
    }

    @Test
    void testAutostartToggle() {
        AutostartManager manager = AutostartManager.getInstance();
        // Toggle on
        boolean enabled = manager.setAutostartEnabled(true);
        if (enabled) {
            assertTrue(manager.isAutostartEnabled());
        }

        // Toggle off
        boolean disabled = manager.setAutostartEnabled(false);
        if (disabled) {
            assertFalse(manager.isAutostartEnabled());
        }
    }
}
