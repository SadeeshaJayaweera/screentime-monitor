package com.screentime.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowDetectorFactoryTest {
    @Test void testCreateDetector() { assertNotNull(WindowDetectorFactory.createDetector()); }
}
