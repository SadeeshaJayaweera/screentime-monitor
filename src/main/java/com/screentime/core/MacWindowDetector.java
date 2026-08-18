package com.screentime.core;

public class MacWindowDetector implements WindowDetector {
    @Override public WindowInfo getActiveWindow() { return new WindowInfo("macOS App", "Active", 0); }
}
