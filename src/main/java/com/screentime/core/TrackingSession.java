package com.screentime.core;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TrackingSession {
    private final String appName;
    private final String windowTitle;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final boolean idle;

    public TrackingSession(String app, String win, LocalDateTime st, LocalDateTime et, boolean idl) {
        this.appName = app; this.windowTitle = win; this.startTime = st; this.endTime = et; this.idle = idl;
    }
    public String getAppName() { return appName; }
    public String getWindowTitle() { return windowTitle; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public boolean isIdle() { return idle; }
    public LocalDate getDate() { return startTime.toLocalDate(); }
}
