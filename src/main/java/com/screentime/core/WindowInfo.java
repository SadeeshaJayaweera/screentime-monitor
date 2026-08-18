package com.screentime.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Data container representing active application and window metadata.
 */
public class WindowInfo {

    private final String appName;
    private final String windowTitle;
    private final long processId;
    private final Instant timestamp;

    public WindowInfo(String appName, String windowTitle, long processId, Instant timestamp) {
        this.appName = appName != null ? appName : "Unknown";
        this.windowTitle = windowTitle != null ? windowTitle : "Unknown";
        this.processId = processId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public WindowInfo(String appName, String windowTitle, long processId) {
        this(appName, windowTitle, processId, Instant.now());
    }

    public String getAppName() {
        return appName;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public long getProcessId() {
        return processId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WindowInfo that = (WindowInfo) o;
        return processId == that.processId &&
                Objects.equals(appName, that.appName) &&
                Objects.equals(windowTitle, that.windowTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, windowTitle, processId);
    }

    @Override
    public String toString() {
        return "WindowInfo{" +
                "appName='" + appName + '\'' +
                ", windowTitle='" + windowTitle + '\'' +
                ", processId=" + processId +
                ", timestamp=" + timestamp +
                '}';
    }
}
