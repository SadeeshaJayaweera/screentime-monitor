package com.screentime.core;
import com.screentime.data.UsageDao;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrackingEngine {
    private final WindowDetector windowDetector;
    private final IdleDetector idleDetector;
    private final UsageDao usageDao;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    public TrackingEngine() { this(WindowDetectorFactory.createDetector(), new IdleDetector(), new UsageDao(), 5, 60); }
    public TrackingEngine(WindowDetector wd, IdleDetector id, UsageDao dao, int poll, int idle) {
        this.windowDetector = wd; this.idleDetector = id; this.usageDao = dao;
    }
    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
            scheduler.scheduleAtFixedRate(this::poll, 1, 5, TimeUnit.SECONDS);
        }
    }
    synchronized void poll() {}
}
