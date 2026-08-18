package com.screentime.core;

public class LinuxWindowDetector implements WindowDetector {
    @Override public WindowInfo getActiveWindow() { return new WindowInfo("Linux App", "Active", 0); }
}
