package com.screentime.core;

/**
 * Listener interface for tracking engine lifecycle, session transitions, and state updates.
 */
public interface TrackingListener {

    /**
     * Fired when a tracking session is completed (e.g. user went idle, switched apps, or app exited).
     *
     * @param session The closed TrackingSession.
     */
    void onSessionClosed(TrackingSession session);

    /**
     * Fired when the user's activity state changes (ACTIVE, IDLE, LOCKED).
     */
    default void onStateChanged(ActivityState oldState, ActivityState newState) {}

    /**
     * Fired when the active foreground window or application changes.
     */
    default void onWindowChanged(WindowInfo windowInfo) {}

    /**
     * Fired periodically on each active tracking poll with the updated running total of active seconds.
     */
    default void onActiveSecondsTick(long totalActiveSecondsToday) {}
}
