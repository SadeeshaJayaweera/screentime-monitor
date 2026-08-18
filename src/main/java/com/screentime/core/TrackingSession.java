package com.screentime.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents one continuous block of active, non-idle screen time usage.
 */
public class TrackingSession {

    private final String appName;
    private final String windowTitle;
    private final Instant startTime;
    private Instant endTime;
    private boolean closed;

    public TrackingSession(String appName, String windowTitle, Instant startTime) {
        this.appName = (appName != null && !appName.isBlank()) ? appName : "Unknown";
        this.windowTitle = (windowTitle != null && !windowTitle.isBlank()) ? windowTitle : "Untitled";
        this.startTime = (startTime != null) ? startTime : Instant.now();
        this.closed = false;
    }

    public TrackingSession(String appName, String windowTitle) {
        this(appName, windowTitle, Instant.now());
    }

    /**
     * Closes this tracking session with the given end time.
     */
    public synchronized void close(Instant endTime) {
        if (!closed) {
            this.endTime = (endTime != null && !endTime.isBefore(startTime)) ? endTime : Instant.now();
            this.closed = true;
        }
    }

    /**
     * Closes this tracking session with the current timestamp.
     */
    public synchronized void close() {
        close(Instant.now());
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isActive() {
        return !closed;
    }

    public String getAppName() {
        return appName;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return (endTime != null) ? endTime : Instant.now();
    }

    /**
     * Calculates the duration of this session in seconds.
     */
    public long getDurationSeconds() {
        Instant effectiveEnd = (endTime != null) ? endTime : Instant.now();
        return Math.max(0, Duration.between(startTime, effectiveEnd).getSeconds());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingSession that = (TrackingSession) o;
        return Objects.equals(appName, that.appName) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, startTime, endTime);
    }

    @Override
    public String toString() {
        return "TrackingSession{" +
                "appName='" + appName + '\'' +
                ", windowTitle='" + windowTitle + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", durationSeconds=" + getDurationSeconds() +
                ", closed=" + closed +
                '}';
    }
}
