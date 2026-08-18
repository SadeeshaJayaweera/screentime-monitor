package com.screentime.restriction;

import com.screentime.core.ActivityState;
import com.screentime.core.TrackingListener;
import com.screentime.core.TrackingSession;
import com.screentime.core.WindowInfo;
import com.screentime.data.LimitExtensionRecord;
import com.screentime.data.UsageDao;
import com.screentime.notifications.NotificationLevel;
import com.screentime.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces daily screen time limits, evaluates warning thresholds,
 * handles extension requests with daily caps, and dispatches escalated reminders.
 */
public class RestrictionEngine implements TrackingListener {

    private static final Logger logger = LoggerFactory.getLogger(RestrictionEngine.class);

    private final RestrictionConfig config;
    private final UsageDao usageDao;
    private final NotificationService notificationService;

    private final Set<Integer> firedThresholdsForDate = ConcurrentHashMap.newKeySet();
    private LocalDate currentDate;

    private int cumulativeExtensionMinutesToday = 0;
    private int extensionCountToday = 0;
    private boolean extensionMode = false;
    private long lastEscalatedReminderActiveSeconds = 0;
    private long lastCheckedActiveSeconds = 0;

    public RestrictionEngine() {
        this(new RestrictionConfig(), new UsageDao(), NotificationService.getInstance());
    }

    public RestrictionEngine(RestrictionConfig config, UsageDao usageDao, NotificationService notificationService) {
        this.config = Objects.requireNonNull(config, "RestrictionConfig must not be null");
        this.usageDao = Objects.requireNonNull(usageDao, "UsageDao must not be null");
        this.notificationService = Objects.requireNonNull(notificationService, "NotificationService must not be null");
        this.currentDate = LocalDate.now();
    }

    @Override
    public void onActiveSecondsTick(long totalActiveSecondsToday) {
        checkUsage(totalActiveSecondsToday);
    }

    @Override
    public void onSessionClosed(TrackingSession session) {
        // Handled directly by UsageDao
    }

    @Override
    public void onStateChanged(ActivityState oldState, ActivityState newState) {}

    @Override
    public void onWindowChanged(WindowInfo windowInfo) {}

    /**
     * Checks the current active seconds against configured thresholds and extension cadences.
     */
    public synchronized void checkUsage(long activeSecondsToday) {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            resetForNewDate(today);
        }

        this.lastCheckedActiveSeconds = activeSecondsToday;
        long usedMinutes = activeSecondsToday / 60;
        int effectiveLimitMinutes = getEffectiveDailyLimit();
        long limitSeconds = effectiveLimitMinutes * 60L;

        if (limitSeconds <= 0) return;

        int percentUsed = (int) ((activeSecondsToday * 100) / limitSeconds);

        // 1. Check warning thresholds
        List<Integer> thresholds = new ArrayList<>(config.getWarningThresholds());
        Collections.sort(thresholds);

        for (int threshold : thresholds) {
            if (percentUsed >= threshold && !firedThresholdsForDate.contains(threshold)) {
                firedThresholdsForDate.add(threshold);
                fireThresholdNotification(threshold, usedMinutes, effectiveLimitMinutes);
            }
        }

        // 2. Check escalated reminder cadence after an extension has been granted
        if (extensionMode && activeSecondsToday >= (config.getDailyLimitMinutes() * 60L)) {
            long cadenceSeconds = config.getExtensionReminderCadenceMinutes() * 60L;
            if (activeSecondsToday - lastEscalatedReminderActiveSeconds >= cadenceSeconds) {
                fireEscalatedExtensionReminder(usedMinutes, effectiveLimitMinutes);
                lastEscalatedReminderActiveSeconds = activeSecondsToday;
            }
        }
    }

    private void fireThresholdNotification(int threshold, long usedMinutes, int effectiveLimitMinutes) {
        long remainingMinutes = Math.max(0, effectiveLimitMinutes - usedMinutes);

        if (threshold >= 100) {
            String title = "🚨 Daily Screen Time Limit Reached";
            String body = String.format("You have reached %d%% of your daily limit (%d minutes). Please take a break!",
                    threshold, effectiveLimitMinutes);

            logger.warn("Limit reached: {} mins used of {} mins limit", usedMinutes, effectiveLimitMinutes);
            notificationService.notify(NotificationLevel.CRITICAL, title, body);

            if (config.isHardBlockEnabled()) {
                HardWarningOverlay.show(effectiveLimitMinutes, usedMinutes, minutes -> requestExtension(minutes, "Hard block extension request"));
            }
        } else {
            String title = String.format("⏱️ Screen Time Warning (%d%% Used)", threshold);
            String body = String.format("You have used %d minutes today. %d minutes remaining before your daily limit.",
                    usedMinutes, remainingMinutes);

            logger.info("Threshold reached: {}% ({} mins remaining)", threshold, remainingMinutes);
            notificationService.notify(NotificationLevel.WARNING, title, body);
        }
    }

    private void fireEscalatedExtensionReminder(long usedMinutes, int effectiveLimitMinutes) {
        long remainingMinutes = Math.max(0, effectiveLimitMinutes - usedMinutes);
        String title = String.format("⚠️ Extension #%d Active", extensionCountToday);
        String body = String.format("%d minutes remaining under today's extended limit (%d mins total).",
                remainingMinutes, effectiveLimitMinutes);

        logger.info("Escalated extension reminder: {} (remaining: {} mins)", title, remainingMinutes);
        notificationService.notify(NotificationLevel.WARNING, title, body);
    }

    /**
     * Handles user extension request with daily cap validation.
     *
     * @param minutes Extension duration in minutes.
     * @param reason Reason for requesting extension.
     * @return ExtensionResult indicating success or refusal reason.
     */
    public synchronized ExtensionResult requestExtension(int minutes, String reason) {
        long usedMinutes = lastCheckedActiveSeconds / 60;
        int currentLimit = getEffectiveDailyLimit();
        int remainingMinutes = (int) Math.max(0, currentLimit - usedMinutes);

        if (minutes <= 0) {
            return ExtensionResult.failure("Extension minutes must be greater than zero.", currentLimit, remainingMinutes, extensionCountToday);
        }

        // Cap check 1: Maximum number of extensions per day
        if (extensionCountToday >= config.getMaxExtensionsPerDay()) {
            String msg = String.format("Daily extension limit reached (maximum %d extensions per day).", config.getMaxExtensionsPerDay());
            logger.warn("Extension rejected: {}", msg);
            return ExtensionResult.failure(msg, currentLimit, remainingMinutes, extensionCountToday);
        }

        // Cap check 2: Maximum total extension minutes per day
        if (cumulativeExtensionMinutesToday + minutes > config.getMaxExtensionMinutesPerDay()) {
            String msg = String.format("Daily extension time cap exceeded (maximum %d minutes per day, %d used).",
                    config.getMaxExtensionMinutesPerDay(), cumulativeExtensionMinutesToday);
            logger.warn("Extension rejected: {}", msg);
            return ExtensionResult.failure(msg, currentLimit, remainingMinutes, extensionCountToday);
        }

        // Grant extension
        cumulativeExtensionMinutesToday += minutes;
        extensionCountToday++;
        extensionMode = true;
        lastEscalatedReminderActiveSeconds = lastCheckedActiveSeconds;

        int newLimit = getEffectiveDailyLimit();
        int newRemaining = (int) Math.max(0, newLimit - usedMinutes);

        // Record in SQLite limit_extensions table
        LimitExtensionRecord record = new LimitExtensionRecord(
                currentDate,
                minutes,
                Instant.now(),
                reason != null ? reason : "User requested extension"
        );
        usageDao.recordLimitExtension(record);

        // If 100% was previously fired and new limit is now higher, allow 100% threshold to fire again
        if (newRemaining > 0) {
            firedThresholdsForDate.remove(100);
            HardWarningOverlay.close();
        }

        // Send confirmation notification
        String title = "✅ Screen Time Extended";
        String body = String.format("Extended by %d minutes (Extension #%d today). New limit: %d mins. Remaining: %d mins.",
                minutes, extensionCountToday, newLimit, newRemaining);
        notificationService.notify(NotificationLevel.INFO, title, body);

        logger.info("Extension granted: +{} mins. New limit: {} mins (Extension #{})", minutes, newLimit, extensionCountToday);

        return ExtensionResult.success(minutes, cumulativeExtensionMinutesToday, newLimit, newRemaining, extensionCountToday);
    }

    /**
     * Resets tracking state for a new day.
     */
    public synchronized void resetForNewDate(LocalDate newDate) {
        logger.info("Resetting restriction monitor for new date: {}", newDate);
        currentDate = newDate;
        firedThresholdsForDate.clear();
        cumulativeExtensionMinutesToday = 0;
        extensionCountToday = 0;
        extensionMode = false;
        lastEscalatedReminderActiveSeconds = 0;
        HardWarningOverlay.close();
    }

    public int getEffectiveDailyLimit() {
        return config.getDailyLimitMinutes() + cumulativeExtensionMinutesToday;
    }

    public long getRemainingMinutes(long activeMinutesToday) {
        return Math.max(0, getEffectiveDailyLimit() - activeMinutesToday);
    }

    public boolean isLimitExceeded(long activeMinutesToday) {
        return activeMinutesToday >= getEffectiveDailyLimit();
    }

    public RestrictionConfig getConfig() {
        return config;
    }

    public Set<Integer> getFiredThresholds() {
        return Collections.unmodifiableSet(firedThresholdsForDate);
    }

    public int getCumulativeExtensionMinutesToday() {
        return cumulativeExtensionMinutesToday;
    }

    public int getExtensionCountToday() {
        return extensionCountToday;
    }

    public boolean isExtensionMode() {
        return extensionMode;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    void setCurrentDate(LocalDate date) {
        this.currentDate = date;
    }
}
