package com.screentime.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing application-wide settings.
 * Serialized to and deserialized from JSON in the OS-specific app-data folder.
 */
public class AppConfig {

    private int dailyLimitMinutes = 480; // Default 8 hours
    private List<Integer> warningThresholds = new ArrayList<>(List.of(50, 75, 90, 100)); // Percentages of limit
    private int idleThresholdSeconds = 60; // 60 seconds idle time threshold
    private int breakReminderIntervalMinutes = 60; // Remind break every 1 hour

    // Restriction and extension rules
    private boolean hardBlockEnabled = false; // Always-on-top overlay warning when limit is reached
    private int maxExtensionsPerDay = 3; // Maximum allowed extension requests per day
    private int maxExtensionMinutesPerDay = 120; // Maximum cumulative extension minutes per day
    private int extensionReminderCadenceMinutes = 5; // Escalated reminder interval after extension

    // AI & Features
    private boolean aiEnabled = true;
    private String geminiApiKey = "";

    // Application state
    private boolean onboardingCompleted = false;
    private boolean startMinimizedToTray = false;
    private boolean notificationsEnabled = true;
    private boolean autostartOnLogin = false;

    public AppConfig() {
    }

    public boolean isAutostartOnLogin() {
        return autostartOnLogin;
    }

    public void setAutostartOnLogin(boolean autostartOnLogin) {
        this.autostartOnLogin = autostartOnLogin;
    }

    public int getDailyLimitMinutes() {
        return dailyLimitMinutes;
    }

    public void setDailyLimitMinutes(int dailyLimitMinutes) {
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    public List<Integer> getWarningThresholds() {
        if (warningThresholds == null || warningThresholds.isEmpty()) {
            warningThresholds = new ArrayList<>(List.of(50, 75, 90, 100));
        }
        return warningThresholds;
    }

    public void setWarningThresholds(List<Integer> warningThresholds) {
        this.warningThresholds = warningThresholds != null ? new ArrayList<>(warningThresholds) : new ArrayList<>(List.of(50, 75, 90, 100));
    }

    public int getIdleThresholdSeconds() {
        return idleThresholdSeconds;
    }

    public void setIdleThresholdSeconds(int idleThresholdSeconds) {
        this.idleThresholdSeconds = idleThresholdSeconds;
    }

    public int getBreakReminderIntervalMinutes() {
        return breakReminderIntervalMinutes;
    }

    public void setBreakReminderIntervalMinutes(int breakReminderIntervalMinutes) {
        this.breakReminderIntervalMinutes = breakReminderIntervalMinutes;
    }

    public boolean isHardBlockEnabled() {
        return hardBlockEnabled;
    }

    public void setHardBlockEnabled(boolean hardBlockEnabled) {
        this.hardBlockEnabled = hardBlockEnabled;
    }

    public int getMaxExtensionsPerDay() {
        return maxExtensionsPerDay;
    }

    public void setMaxExtensionsPerDay(int maxExtensionsPerDay) {
        this.maxExtensionsPerDay = maxExtensionsPerDay;
    }

    public int getMaxExtensionMinutesPerDay() {
        return maxExtensionMinutesPerDay;
    }

    public void setMaxExtensionMinutesPerDay(int maxExtensionMinutesPerDay) {
        this.maxExtensionMinutesPerDay = maxExtensionMinutesPerDay;
    }

    public int getExtensionReminderCadenceMinutes() {
        return extensionReminderCadenceMinutes;
    }

    public void setExtensionReminderCadenceMinutes(int extensionReminderCadenceMinutes) {
        this.extensionReminderCadenceMinutes = extensionReminderCadenceMinutes;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public boolean isStartMinimizedToTray() {
        return startMinimizedToTray;
    }

    public void setStartMinimizedToTray(boolean startMinimizedToTray) {
        this.startMinimizedToTray = startMinimizedToTray;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    @Override
    public String toString() {
        return "AppConfig{" +
                "dailyLimitMinutes=" + dailyLimitMinutes +
                ", warningThresholds=" + warningThresholds +
                ", idleThresholdSeconds=" + idleThresholdSeconds +
                ", hardBlockEnabled=" + hardBlockEnabled +
                ", maxExtensionsPerDay=" + maxExtensionsPerDay +
                ", maxExtensionMinutesPerDay=" + maxExtensionMinutesPerDay +
                ", extensionReminderCadenceMinutes=" + extensionReminderCadenceMinutes +
                ", aiEnabled=" + aiEnabled +
                ", geminiApiKeyConfigured=" + (geminiApiKey != null && !geminiApiKey.isBlank()) +
                ", onboardingCompleted=" + onboardingCompleted +
                ", startMinimizedToTray=" + startMinimizedToTray +
                ", notificationsEnabled=" + notificationsEnabled +
                '}';
    }
}
