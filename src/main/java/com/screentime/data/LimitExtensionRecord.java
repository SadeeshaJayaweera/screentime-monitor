package com.screentime.data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Model representing a persisted limit extension request in SQLite.
 */
public class LimitExtensionRecord {

    private long id;
    private LocalDate date;
    private int requestedMinutes;
    private Instant requestedAt;
    private String reason;

    public LimitExtensionRecord() {
    }

    public LimitExtensionRecord(LocalDate date, int requestedMinutes, Instant requestedAt, String reason) {
        this(0, date, requestedMinutes, requestedAt, reason);
    }

    public LimitExtensionRecord(long id, LocalDate date, int requestedMinutes, Instant requestedAt, String reason) {
        this.id = id;
        this.date = date;
        this.requestedMinutes = requestedMinutes;
        this.requestedAt = requestedAt != null ? requestedAt : Instant.now();
        this.reason = reason;
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

    public int getRequestedMinutes() {
        return requestedMinutes;
    }

    public void setRequestedMinutes(int requestedMinutes) {
        this.requestedMinutes = requestedMinutes;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "LimitExtensionRecord{" +
                "id=" + id +
                ", date=" + date +
                ", requestedMinutes=" + requestedMinutes +
                ", requestedAt=" + requestedAt +
                ", reason='" + reason + '\'' +
                '}';
    }
}
