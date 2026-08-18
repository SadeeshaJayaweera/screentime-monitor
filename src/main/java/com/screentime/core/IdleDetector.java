package com.screentime.core;
import java.util.concurrent.atomic.AtomicLong;

public class IdleDetector {
    private final AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());
    public void recordInputEvent() { lastActivity.set(System.currentTimeMillis()); }
}
