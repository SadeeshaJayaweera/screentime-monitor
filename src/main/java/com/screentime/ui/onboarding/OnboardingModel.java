package com.screentime.ui.onboarding;

import com.screentime.config.AppConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data and validation model for the first-run onboarding wizard.
 */
public class OnboardingModel {

    private int dailyLimitMinutes = 480; // 8 hours
    private List<Integer> warningThresholds = new ArrayList<>(List.of(50, 75, 90, 100));
    private int idleThresholdSeconds = 60;
    private boolean allowExtensions = true;
    private int maxExtensionsPerDay = 3;
    private int maxExtensionMinutesPerDay = 120;
    private boolean aiEnabled = true;
    private String geminiApiKey = "";

    public OnboardingModel() {}

    public static ValidationResult validateDailyLimit(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.error("Daily limit cannot be empty.");
        }
        try {
            int minutes = Integer.parseInt(input.trim());
            if (minutes <= 0) {
                return ValidationResult.error("Daily limit must be greater than 0 minutes.");
            }
            if (minutes > 1440) {
                return ValidationResult.error("Daily limit cannot exceed 1440 minutes (24 hours).");
            }
            return ValidationResult.ok();
        } catch (NumberFormatException e) {
            return ValidationResult.error("Please enter a valid whole number of minutes.");
        }
    }

    public static ValidationResult validateThresholds(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.error("Warning thresholds cannot be empty.");
        }
        try {
            List<Integer> list = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            if (list.isEmpty()) {
                return ValidationResult.error("Please provide at least one warning threshold percentage.");
            }

            for (int val : list) {
                if (val <= 0 || val > 100) {
                    return ValidationResult.error("Threshold percentages must be between 1 and 100.");
                }
            }

            // Check sorted order and no duplicates
            for (int i = 0; i < list.size() - 1; i++) {
                if (list.get(i) >= list.get(i + 1)) {
                    return ValidationResult.error("Thresholds must be strictly ascending (e.g. 50, 75, 90, 100).");
                }
            }

            return ValidationResult.ok();
        } catch (NumberFormatException e) {
            return ValidationResult.error("Thresholds must be comma-separated integers (e.g. 50, 75, 90, 100).");
        }
    }

    public static ValidationResult validateIdleThreshold(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.error("Idle sensitivity cannot be empty.");
        }
        try {
            int seconds = Integer.parseInt(input.trim());
            if (seconds < 10) {
                return ValidationResult.error("Idle threshold must be at least 10 seconds.");
            }
            if (seconds > 1800) {
                return ValidationResult.error("Idle threshold cannot exceed 1800 seconds (30 minutes).");
            }
            return ValidationResult.ok();
        } catch (NumberFormatException e) {
            return ValidationResult.error("Please enter a valid whole number of seconds.");
        }
    }

    public static ValidationResult validateExtensions(String maxCountInput, String maxMinutesInput) {
        try {
            int count = Integer.parseInt(maxCountInput.trim());
            int minutes = Integer.parseInt(maxMinutesInput.trim());

            if (count < 0 || count > 20) {
                return ValidationResult.error("Max extensions per day must be between 0 and 20.");
            }
            if (minutes < 0 || minutes > 720) {
                return ValidationResult.error("Max extension minutes must be between 0 and 720 (12 hours).");
            }
            return ValidationResult.ok();
        } catch (NumberFormatException e) {
            return ValidationResult.error("Please enter valid integers for extension caps.");
        }
    }

    /**
     * Applies the validated onboarding choices to the central AppConfig.
     */
    public void applyToConfig(AppConfig config) {
        if (config == null) return;

        config.setDailyLimitMinutes(dailyLimitMinutes);
        config.setWarningThresholds(new ArrayList<>(warningThresholds));
        config.setIdleThresholdSeconds(idleThresholdSeconds);
        config.setMaxExtensionsPerDay(allowExtensions ? maxExtensionsPerDay : 0);
        config.setMaxExtensionMinutesPerDay(allowExtensions ? maxExtensionMinutesPerDay : 0);
        config.setAiEnabled(aiEnabled);
        config.setGeminiApiKey(geminiApiKey != null ? geminiApiKey.trim() : "");
        config.setOnboardingCompleted(true);
    }

    public record ValidationResult(boolean valid, String errorMessage) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }

    // --- Getters and Setters ---

    public int getDailyLimitMinutes() {
        return dailyLimitMinutes;
    }

    public void setDailyLimitMinutes(int dailyLimitMinutes) {
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    public List<Integer> getWarningThresholds() {
        return warningThresholds;
    }

    public void setWarningThresholds(List<Integer> warningThresholds) {
        this.warningThresholds = warningThresholds;
    }

    public int getIdleThresholdSeconds() {
        return idleThresholdSeconds;
    }

    public void setIdleThresholdSeconds(int idleThresholdSeconds) {
        this.idleThresholdSeconds = idleThresholdSeconds;
    }

    public boolean isAllowExtensions() {
        return allowExtensions;
    }

    public void setAllowExtensions(boolean allowExtensions) {
        this.allowExtensions = allowExtensions;
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
}
