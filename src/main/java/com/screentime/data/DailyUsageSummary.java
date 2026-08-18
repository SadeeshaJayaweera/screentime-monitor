package com.screentime.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Model representing aggregated screen time metrics for a specific date,
 * including total active seconds, total idle seconds, and per-app breakdown.
 */
public class DailyUsageSummary {

    private LocalDate date;
    private long totalActiveSeconds;
    private long totalIdleSeconds;
    private List<AppUsage> appBreakdown;

    public DailyUsageSummary() {
        this.appBreakdown = new ArrayList<>();
    }

    public DailyUsageSummary(LocalDate date, long totalActiveSeconds, long totalIdleSeconds, List<AppUsage> appBreakdown) {
        this.date = date;
        this.totalActiveSeconds = Math.max(0, totalActiveSeconds);
        this.totalIdleSeconds = Math.max(0, totalIdleSeconds);
        this.appBreakdown = appBreakdown != null ? new ArrayList<>(appBreakdown) : new ArrayList<>();
    }

    public DailyUsageSummary(LocalDate date, long totalActiveSeconds, long totalIdleSeconds) {
        this(date, totalActiveSeconds, totalIdleSeconds, new ArrayList<>());
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getTotalActiveSeconds() {
        return totalActiveSeconds;
    }

    public void setTotalActiveSeconds(long totalActiveSeconds) {
        this.totalActiveSeconds = totalActiveSeconds;
    }

    public long getTotalIdleSeconds() {
        return totalIdleSeconds;
    }

    public void setTotalIdleSeconds(long totalIdleSeconds) {
        this.totalIdleSeconds = totalIdleSeconds;
    }

    public List<AppUsage> getAppBreakdown() {
        return Collections.unmodifiableList(appBreakdown);
    }

    public void setAppBreakdown(List<AppUsage> appBreakdown) {
        this.appBreakdown = appBreakdown != null ? new ArrayList<>(appBreakdown) : new ArrayList<>();
    }

    public void addAppUsage(AppUsage appUsage) {
        if (appUsage != null) {
            this.appBreakdown.add(appUsage);
        }
    }

    public long getTotalActiveMinutes() {
        return totalActiveSeconds / 60;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyUsageSummary that = (DailyUsageSummary) o;
        return totalActiveSeconds == that.totalActiveSeconds &&
                totalIdleSeconds == that.totalIdleSeconds &&
                Objects.equals(date, that.date) &&
                Objects.equals(appBreakdown, that.appBreakdown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, totalActiveSeconds, totalIdleSeconds, appBreakdown);
    }

    @Override
    public String toString() {
        return "DailyUsageSummary{" +
                "date=" + date +
                ", totalActiveSeconds=" + totalActiveSeconds +
                ", totalIdleSeconds=" + totalIdleSeconds +
                ", appCount=" + appBreakdown.size() +
                '}';
    }
}
