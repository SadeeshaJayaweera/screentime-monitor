package com.screentime.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowDetectorFactoryTest {

    @Test
    void testWindowsDetectorSelection() {
        WindowDetector detector = WindowDetectorFactory.createDetectorForOs("Windows 11");
        assertTrue(detector instanceof WindowsWindowDetector);
    }

    @Test
    void testMacDetectorSelection() {
        WindowDetector detector = WindowDetectorFactory.createDetectorForOs("Mac OS X");
        assertTrue(detector instanceof MacWindowDetector);
    }

    @Test
    void testLinuxDetectorSelection() {
        WindowDetector detector = WindowDetectorFactory.createDetectorForOs("Linux");
        assertTrue(detector instanceof LinuxWindowDetector);
    }

    @Test
    void testUnknownOsFallback() {
        WindowDetector detector = WindowDetectorFactory.createDetectorForOs("FreeBSD");
        assertTrue(detector instanceof GenericWindowDetector);
    }

    @Test
    void testCurrentRuntimeDetectorNonNull() {
        WindowDetector detector = WindowDetectorFactory.createDetector();
        assertNotNull(detector);
        assertNotNull(detector.getDetectorName());
    }
}
