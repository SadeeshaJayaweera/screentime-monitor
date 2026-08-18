package com.screentime.core;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrackingEngine {
    private final AtomicBoolean paused = new AtomicBoolean(false);
    public void pause() { paused.set(true); }
    public void resume() { paused.set(false); }
    public boolean isPaused() { return paused.get(); }
}
