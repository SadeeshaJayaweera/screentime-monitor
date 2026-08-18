package com.screentime.config;
import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    private int dailyLimitMinutes = 480;
    private List<Integer> warningThresholds = new ArrayList<>(List.of(50, 75, 90, 100));
    private int idleThresholdSeconds = 60;
    private int breakReminderIntervalMinutes = 60;

    public AppConfig() {}
    public int getDailyLimitMinutes() { return dailyLimitMinutes; }
    public void setDailyLimitMinutes(int dailyLimitMinutes) { this.dailyLimitMinutes = dailyLimitMinutes; }
    public List<Integer> getWarningThresholds() { return warningThresholds; }
    public void setWarningThresholds(List<Integer> warningThresholds) { this.warningThresholds = warningThresholds; }
    public int getIdleThresholdSeconds() { return idleThresholdSeconds; }
    public void setIdleThresholdSeconds(int idleThresholdSeconds) { this.idleThresholdSeconds = idleThresholdSeconds; }
    public int getBreakReminderIntervalMinutes() { return breakReminderIntervalMinutes; }
    public void setBreakReminderIntervalMinutes(int breakReminderIntervalMinutes) { this.breakReminderIntervalMinutes = breakReminderIntervalMinutes; }
}
