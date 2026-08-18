package com.screentime.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that selects the appropriate WindowDetector based on the runtime operating system.
 */
public final class WindowDetectorFactory {

    private static final Logger logger = LoggerFactory.getLogger(WindowDetectorFactory.class);

    private WindowDetectorFactory() {}

    /**
     * Creates and returns a platform-specific WindowDetector instance.
     */
    public static WindowDetector createDetector() {
        return createDetectorForOs(System.getProperty("os.name", ""));
    }

    /**
     * Package-private factory method allowing OS string injection for unit testing.
     */
    static WindowDetector createDetectorForOs(String osName) {
        String os = osName.toLowerCase();
        if (os.contains("win")) {
            logger.info("Selected WindowsWindowDetector for OS: {}", osName);
            return new WindowsWindowDetector();
        } else if (os.contains("mac") || os.contains("darwin")) {
            logger.info("Selected MacWindowDetector for OS: {}", osName);
            return new MacWindowDetector();
        } else if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            logger.info("Selected LinuxWindowDetector for OS: {}", osName);
            return new LinuxWindowDetector();
        } else {
            logger.warn("Unrecognized OS '{}', falling back to GenericWindowDetector.", osName);
            return new GenericWindowDetector();
        }
    }
}
