package com.screentime.data;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Model representing aggregated usage for a specific application on a given date.
 */
public class AppUsage {

    private long id;
    private LocalDate date;
    private String appName;
    private long secondsUsed;

    public AppUsage() {
    }

    public AppUsage(LocalDate date, String appName, long secondsUsed) {
        this(0, date, appName, secondsUsed);
    }

    public AppUsage(long id, LocalDate date, String appName, long secondsUsed) {
        this.id = id;
        this.date = date;
        this.appName = appName != null ? appName : "Unknown";
        this.secondsUsed = Math.max(0, secondsUsed);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getSecondsUsed() {
        return secondsUsed;
    }

    public void setSecondsUsed(long secondsUsed) {
        this.secondsUsed = secondsUsed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppUsage appUsage = (AppUsage) o;
        return secondsUsed == appUsage.secondsUsed &&
                Objects.equals(date, appUsage.date) &&
                Objects.equals(appName, appUsage.appName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, appName, secondsUsed);
    }

    @Override
    public String toString() {
        return "AppUsage{" +
                "date=" + date +
                ", appName='" + appName + '\'' +
                ", secondsUsed=" + secondsUsed +
                '}';
    }
}
