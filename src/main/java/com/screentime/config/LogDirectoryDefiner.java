package com.screentime.config;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * Custom Logback property definer that resolves the OS-appropriate log directory
 * so log files are always placed in the application's app-data directory.
 */
public class LogDirectoryDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        return ConfigManager.resolveAppDataDir().toAbsolutePath().toString();
    }
}
