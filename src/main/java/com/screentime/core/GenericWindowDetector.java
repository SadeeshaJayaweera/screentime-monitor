package com.screentime.core;

import java.time.Instant;

/**
 * Generic fallback window detector for unknown or unsupported operating systems.
 */
public class GenericWindowDetector implements WindowDetector {

    @Override
    public WindowInfo getActiveWindow() {
        return new WindowInfo("Desktop Application", "Active Session", 0, Instant.now());
    }

    @Override
    public String getDetectorName() {
        return "Generic Fallback";
    }
}
