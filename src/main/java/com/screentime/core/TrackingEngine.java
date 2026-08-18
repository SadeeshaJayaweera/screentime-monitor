package com.screentime.core;
import java.time.LocalDate;

public class TrackingEngine {
    private LocalDate currentTrackingDate = LocalDate.now();
    public LocalDate getCurrentTrackingDate() { return currentTrackingDate; }
}
