package com.screentime.restriction;

/**
 * Encapsulates the outcome of a screen time extension request.
 */
public class ExtensionResult {

    private final boolean granted;
    private final String message;
    private final int grantedMinutes;
    private final int cumulativeExtensionMinutes;
    private final int newLimitMinutes;
    private final int remainingMinutes;
    private final int extensionCountToday;

    public ExtensionResult(boolean granted, String message, int grantedMinutes, int cumulativeExtensionMinutes, int newLimitMinutes, int remainingMinutes, int extensionCountToday) {
        this.granted = granted;
        this.message = message != null ? message : "";
        this.grantedMinutes = grantedMinutes;
        this.cumulativeExtensionMinutes = cumulativeExtensionMinutes;
        this.newLimitMinutes = newLimitMinutes;
        this.remainingMinutes = remainingMinutes;
        this.extensionCountToday = extensionCountToday;
    }

    public static ExtensionResult success(int grantedMinutes, int cumulativeExtensionMinutes, int newLimitMinutes, int remainingMinutes, int countToday) {
        String msg = String.format("Extended by %d minutes. New limit: %d mins. Remaining: %d minutes.",
                grantedMinutes, newLimitMinutes, remainingMinutes);
        return new ExtensionResult(true, msg, grantedMinutes, cumulativeExtensionMinutes, newLimitMinutes, remainingMinutes, countToday);
    }

    public static ExtensionResult failure(String reason, int currentLimit, int remainingMinutes, int countToday) {
        return new ExtensionResult(false, reason, 0, 0, currentLimit, remainingMinutes, countToday);
    }

    public boolean isGranted() {
        return granted;
    }

    public String getMessage() {
        return message;
    }

    public int getGrantedMinutes() {
        return grantedMinutes;
    }

    public int getCumulativeExtensionMinutes() {
        return cumulativeExtensionMinutes;
    }

    public int getNewLimitMinutes() {
        return newLimitMinutes;
    }

    public int getRemainingMinutes() {
        return remainingMinutes;
    }

    public int getExtensionCountToday() {
        return extensionCountToday;
    }

    @Override
    public String toString() {
        return "ExtensionResult{" +
                "granted=" + granted +
                ", message='" + message + '\'' +
                ", grantedMinutes=" + grantedMinutes +
                ", newLimitMinutes=" + newLimitMinutes +
                ", remainingMinutes=" + remainingMinutes +
                ", extensionCountToday=" + extensionCountToday +
                '}';
    }
}
