package com.screentime.core;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Monitors user activity using JNativeHook global keyboard and mouse listeners.
 * Determines if the user is currently idle based on the elapsed time since the last input.
 */
public class IdleDetector implements NativeKeyListener, NativeMouseInputListener, NativeMouseWheelListener {

    private static final Logger logger = LoggerFactory.getLogger(IdleDetector.class);

    private final AtomicLong lastInputTimestampMillis = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean hookRegistered = new AtomicBoolean(false);

    public IdleDetector() {
        silenceJNativeHookLogger();
    }

    /**
     * Starts listening for global keyboard and mouse events.
     */
    public synchronized void start() {
        if (hookRegistered.compareAndSet(false, true)) {
            recordInputEvent();
            try {
                if (!GlobalScreen.isNativeHookRegistered()) {
                    GlobalScreen.registerNativeHook();
                }
                GlobalScreen.addNativeKeyListener(this);
                GlobalScreen.addNativeMouseListener(this);
                GlobalScreen.addNativeMouseMotionListener(this);
                GlobalScreen.addNativeMouseWheelListener(this);
                logger.info("IdleDetector successfully registered global input hooks.");
            } catch (NativeHookException e) {
                logger.warn("Failed to register JNativeHook (Accessibility permissions may be needed on macOS): {}", e.getMessage());
            } catch (Throwable t) {
                logger.warn("Could not start IdleDetector global hook in this environment: {}", t.getMessage());
            }
        }
    }

    /**
     * Stops listening and removes native hooks.
     */
    public synchronized void stop() {
        if (hookRegistered.compareAndSet(true, false)) {
            try {
                GlobalScreen.removeNativeKeyListener(this);
                GlobalScreen.removeNativeMouseListener(this);
                GlobalScreen.removeNativeMouseMotionListener(this);
                GlobalScreen.removeNativeMouseWheelListener(this);
                if (GlobalScreen.isNativeHookRegistered()) {
                    GlobalScreen.unregisterNativeHook();
                }
                logger.info("IdleDetector unregistered global hooks.");
            } catch (NativeHookException e) {
                logger.debug("Failed to unregister JNativeHook: {}", e.getMessage());
            } catch (Throwable t) {
                logger.debug("Error stopping IdleDetector: {}", t.getMessage());
            }
        }
    }

    /**
     * Records an input event to reset the idle timer.
     */
    public void recordInputEvent() {
        lastInputTimestampMillis.set(System.currentTimeMillis());
    }

    /**
     * Checks if the user is considered idle based on the specified threshold in seconds.
     *
     * @param thresholdSeconds Number of seconds of inactivity before considered idle.
     * @return true if no input was detected for thresholdSeconds or longer.
     */
    public boolean isIdle(int thresholdSeconds) {
        return getIdleDurationSeconds() >= thresholdSeconds;
    }

    /**
     * Returns the elapsed inactivity duration in seconds.
     */
    public long getIdleDurationSeconds() {
        long elapsedMillis = System.currentTimeMillis() - lastInputTimestampMillis.get();
        return Math.max(0, elapsedMillis / 1000L);
    }

    public long getLastInputTimestampMillis() {
        return lastInputTimestampMillis.get();
    }

    // --- Native Listeners ---

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
        recordInputEvent();
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
        recordInputEvent();
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeEvent) {
        recordInputEvent();
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent nativeEvent) {
        recordInputEvent();
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent nativeEvent) {
        recordInputEvent();
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent nativeEvent) {
        recordInputEvent();
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent nativeEvent) {
        recordInputEvent();
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent nativeEvent) {
        recordInputEvent();
    }

    @Override
    public void nativeMouseWheelMoved(NativeMouseWheelEvent nativeEvent) {
        recordInputEvent();
    }

    private void silenceJNativeHookLogger() {
        try {
            java.util.logging.Logger hookLogger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
            hookLogger.setLevel(Level.WARNING);
            hookLogger.setUseParentHandlers(false);
        } catch (Throwable ignored) {}
    }
}
