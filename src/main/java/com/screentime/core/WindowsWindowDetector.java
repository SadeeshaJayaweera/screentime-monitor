package com.screentime.core;

public class WindowsWindowDetector implements WindowDetector {
    @Override public WindowInfo getActiveWindow() { return new WindowInfo("Windows App", "Active", 0); }
}
