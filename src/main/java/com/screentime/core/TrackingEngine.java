package com.screentime.core;
import com.screentime.data.UsageDao;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrackingEngine {
    private final List<TrackingListener> listeners = new CopyOnWriteArrayList<>();
    public void addListener(TrackingListener l) { listeners.add(l); }
    public void removeListener(TrackingListener l) { listeners.remove(l); }
}
