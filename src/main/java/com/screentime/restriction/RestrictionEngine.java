package com.screentime.restriction;
import com.screentime.core.TrackingListener;
import com.screentime.data.UsageDao;
import com.screentime.notifications.NotificationService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RestrictionEngine implements TrackingListener {
    private final RestrictionConfig config;
    private final Set<Integer> triggered = ConcurrentHashMap.newKeySet();
    public RestrictionEngine() { this(new RestrictionConfig(), new UsageDao(), NotificationService.getInstance()); }
    public RestrictionEngine(RestrictionConfig c, UsageDao d, NotificationService n) { this.config = c; }
    public RestrictionConfig getConfig() { return config; }
    public Set<Integer> getTriggeredThresholdsToday() { return triggered; }
    @Override public void onTick(long active, long idle) {
        long limitSecs = config.getDailyLimitMinutes() * 60L;
        if (limitSecs <= 0) return;
        double pct = ((double) active / limitSecs) * 100.0;
        for (int t : config.getWarningThresholds()) {
            if (pct >= t && !triggered.contains(t)) triggered.add(t);
        }
    }
}
