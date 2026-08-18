package com.screentime.config;
import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    private int dailyLimitMinutes = 480;
    private List<Integer> warningThresholds = new ArrayList<>(List.of(50, 75, 90, 100));
    private int idleThresholdSeconds = 60;
    private int breakReminderIntervalMinutes = 60;
    private boolean hardBlockEnabled = false;
    private int maxExtensionsPerDay = 3;
    private int maxExtensionMinutesPerDay = 120;
    private int extensionReminderCadenceMinutes = 5;
    private boolean aiEnabled = true;
    private String geminiApiKey = "";

    public AppConfig() {}
    public int getDailyLimitMinutes() { return dailyLimitMinutes; }
    public void setDailyLimitMinutes(int dailyLimitMinutes) { this.dailyLimitMinutes = dailyLimitMinutes; }
    public List<Integer> getWarningThresholds() { return warningThresholds; }
    public void setWarningThresholds(List<Integer> warningThresholds) { this.warningThresholds = warningThresholds; }
    public int getIdleThresholdSeconds() { return idleThresholdSeconds; }
    public void setIdleThresholdSeconds(int idleThresholdSeconds) { this.idleThresholdSeconds = idleThresholdSeconds; }
    public int getBreakReminderIntervalMinutes() { return breakReminderIntervalMinutes; }
    public void setBreakReminderIntervalMinutes(int breakReminderIntervalMinutes) { this.breakReminderIntervalMinutes = breakReminderIntervalMinutes; }
    public boolean isHardBlockEnabled() { return hardBlockEnabled; }
    public void setHardBlockEnabled(boolean hardBlockEnabled) { this.hardBlockEnabled = hardBlockEnabled; }
    public int getMaxExtensionsPerDay() { return maxExtensionsPerDay; }
    public void setMaxExtensionsPerDay(int maxExtensionsPerDay) { this.maxExtensionsPerDay = maxExtensionsPerDay; }
    public int getMaxExtensionMinutesPerDay() { return maxExtensionMinutesPerDay; }
    public void setMaxExtensionMinutesPerDay(int maxExtensionMinutesPerDay) { this.maxExtensionMinutesPerDay = maxExtensionMinutesPerDay; }
    public int getExtensionReminderCadenceMinutes() { return extensionReminderCadenceMinutes; }
    public void setExtensionReminderCadenceMinutes(int extensionReminderCadenceMinutes) { this.extensionReminderCadenceMinutes = extensionReminderCadenceMinutes; }
    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }
    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String geminiApiKey) { this.geminiApiKey = geminiApiKey; }
}
