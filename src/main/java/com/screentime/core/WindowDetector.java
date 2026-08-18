package com.screentime.core;

/**
 * Platform-independent interface for querying the currently active/focused window.
 */
public interface WindowDetector {

    /**
     * Inspects the operating system for the current foreground window and application.
     *
     * @return WindowInfo representing the active application and window metadata.
     */
    WindowInfo getActiveWindow();

    /**
     * Name of the detection strategy or platform.
     */
    String getDetectorName();
}
