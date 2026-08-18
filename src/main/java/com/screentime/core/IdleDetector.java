package com.screentime.core;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener;
import java.util.concurrent.atomic.AtomicLong;

public class IdleDetector implements NativeKeyListener, NativeMouseListener, NativeMouseMotionListener {
    private final AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());
    public void recordInputEvent() { lastActivity.set(System.currentTimeMillis()); }
    @Override public void nativeKeyPressed(NativeKeyEvent e) { recordInputEvent(); }
    @Override public void nativeKeyReleased(NativeKeyEvent e) { recordInputEvent(); }
    @Override public void nativeKeyTyped(NativeKeyEvent e) { recordInputEvent(); }
    @Override public void nativeMouseClicked(NativeMouseEvent e) { recordInputEvent(); }
    @Override public void nativeMousePressed(NativeMouseEvent e) { recordInputEvent(); }
    @Override public void nativeMouseReleased(NativeMouseEvent e) { recordInputEvent(); }
    @Override public void nativeMouseMoved(NativeMouseEvent e) { recordInputEvent(); }
    @Override public void nativeMouseDragged(NativeMouseEvent e) { recordInputEvent(); }
}
