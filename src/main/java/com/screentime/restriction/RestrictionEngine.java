package com.screentime.restriction;
import com.screentime.core.TrackingListener;
import com.screentime.data.UsageDao;
import com.screentime.notifications.NotificationService;

public class RestrictionEngine implements TrackingListener {
    private final RestrictionConfig config;
    public RestrictionEngine() { this(new RestrictionConfig(), new UsageDao(), NotificationService.getInstance()); }
    public RestrictionEngine(RestrictionConfig c, UsageDao d, NotificationService n) { this.config = c; }
    public ExtensionResult requestExtension(int minutes, String reason) {
        config.setDailyLimitMinutes(config.getDailyLimitMinutes() + minutes);
        return ExtensionResult.approved(minutes, config.getDailyLimitMinutes());
    }
}
