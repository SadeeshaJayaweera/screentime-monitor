package com.screentime.ui;

/**
 * Utility methods for formatting screen time durations and timestamps for the UI.
 */
public final class TimeFormatUtils {

    private TimeFormatUtils() {}

    /**
     * Formats seconds into "Xh Ym" or "Ym Zs" or "Xs".
     */
    public static String formatDurationSeconds(long totalSeconds) {
        if (totalSeconds <= 0) return "0m";

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Formats minutes into "Xh Ym" or "Ym".
     */
    public static String formatDurationMinutes(long totalMinutes) {
        if (totalMinutes <= 0) return "0m";

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
