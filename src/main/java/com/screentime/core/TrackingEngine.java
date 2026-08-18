package com.screentime.core;
import com.screentime.data.UsageDao;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrackingEngine {
    private final WindowDetector windowDetector;
    private final IdleDetector idleDetector;
    private final UsageDao usageDao;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TrackingEngine() { this(WindowDetectorFactory.createDetector(), new IdleDetector(), new UsageDao(), 5, 60); }
    public TrackingEngine(WindowDetector wd, IdleDetector id, UsageDao dao, int poll, int idle) {
        this.windowDetector = wd; this.idleDetector = id; this.usageDao = dao;
    }
}
