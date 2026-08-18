package com.screentime.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatUtilsTest {

    @Test
    void testFormatDurationSeconds() {
        assertEquals("0m", TimeFormatUtils.formatDurationSeconds(0));
        assertEquals("45s", TimeFormatUtils.formatDurationSeconds(45));
        assertEquals("5m 30s", TimeFormatUtils.formatDurationSeconds(330));
        assertEquals("1h 00m", TimeFormatUtils.formatDurationSeconds(3600));
        assertEquals("2h 15m", TimeFormatUtils.formatDurationSeconds(8100));
    }

    @Test
    void testFormatDurationMinutes() {
        assertEquals("0m", TimeFormatUtils.formatDurationMinutes(0));
        assertEquals("45m", TimeFormatUtils.formatDurationMinutes(45));
        assertEquals("1h 00m", TimeFormatUtils.formatDurationMinutes(60));
        assertEquals("8h 30m", TimeFormatUtils.formatDurationMinutes(510));
    }
}
