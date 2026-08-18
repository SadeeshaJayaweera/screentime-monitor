package com.screentime.restriction;

import com.screentime.config.AppConfig;
import com.screentime.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration model for screen time restrictions, warning thresholds, and extension caps.
 */
public class RestrictionConfig {

    private int dailyLimitMinutes;
    private List<Integer> warningThresholds;
    private boolean hardBlockEnabled;
    private int maxExtensionsPerDay;
    private int maxExtensionMinutesPerDay;
    private int extensionReminderCadenceMinutes;

    public RestrictionConfig() {
        this(ConfigManager.getInstance().getConfig());
    }

    public RestrictionConfig(AppConfig appConfig) {
        if (appConfig != null) {
            this.dailyLimitMinutes = appConfig.getDailyLimitMinutes();
            this.warningThresholds = new ArrayList<>(appConfig.getWarningThresholds());
            this.hardBlockEnabled = appConfig.isHardBlockEnabled();
            this.maxExtensionsPerDay = appConfig.getMaxExtensionsPerDay();
            this.maxExtensionMinutesPerDay = appConfig.getMaxExtensionMinutesPerDay();
            this.extensionReminderCadenceMinutes = appConfig.getExtensionReminderCadenceMinutes();
        } else {
            this.dailyLimitMinutes = 480;
            this.warningThresholds = new ArrayList<>(List.of(50, 75, 90, 100));
            this.hardBlockEnabled = false;
            this.maxExtensionsPerDay = 3;
            this.maxExtensionMinutesPerDay = 120;
            this.extensionReminderCadenceMinutes = 5;
        }
    }

    public int getDailyLimitMinutes() {
        return dailyLimitMinutes;
    }

    public void setDailyLimitMinutes(int dailyLimitMinutes) {
        this.dailyLimitMinutes = Math.max(1, dailyLimitMinutes);
    }

    public List<Integer> getWarningThresholds() {
        return warningThresholds;
    }

    public void setWarningThresholds(List<Integer> warningThresholds) {
        this.warningThresholds = warningThresholds != null ? new ArrayList<>(warningThresholds) : new ArrayList<>(List.of(50, 75, 90, 100));
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
        this.maxExtensionsPerDay = Math.max(0, maxExtensionsPerDay);
    }

    public int getMaxExtensionMinutesPerDay() {
        return maxExtensionMinutesPerDay;
    }

    public void setMaxExtensionMinutesPerDay(int maxExtensionMinutesPerDay) {
        this.maxExtensionMinutesPerDay = Math.max(0, maxExtensionMinutesPerDay);
    }

    public int getExtensionReminderCadenceMinutes() {
        return extensionReminderCadenceMinutes;
    }

    public void setExtensionReminderCadenceMinutes(int extensionReminderCadenceMinutes) {
        this.extensionReminderCadenceMinutes = Math.max(1, extensionReminderCadenceMinutes);
    }
}
