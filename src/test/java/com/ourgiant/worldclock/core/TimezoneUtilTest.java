package com.ourgiant.worldclock.core;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TimezoneUtilTest {

    @Test
    void utcOffsetStringForUtcIsZero() {
        assertEquals("UTC +0:00", TimezoneUtil.getUTCOffsetString(ZoneId.of("UTC")));
    }

    @Test
    void utcOffsetStringFormatsPositiveHalfHourOffset() {
        assertEquals("UTC +5:30", TimezoneUtil.getUTCOffsetString(ZoneId.of("+05:30")));
    }

    @Test
    void utcOffsetStringFormatsNegativeOffset() {
        assertEquals("UTC -8:00", TimezoneUtil.getUTCOffsetString(ZoneId.of("-08:00")));
    }

    @Test
    void utcOffsetHoursForUtcIsZero() {
        assertEquals(0.0, TimezoneUtil.getUTCOffsetHours(ZoneId.of("UTC")));
    }

    @Test
    void utcOffsetHoursHandlesHalfHourOffset() {
        assertEquals(5.5, TimezoneUtil.getUTCOffsetHours(ZoneId.of("+05:30")));
    }

    @Test
    void utcOffsetHoursHandlesNegativeOffset() {
        assertEquals(-8.0, TimezoneUtil.getUTCOffsetHours(ZoneId.of("-08:00")));
    }

    @Test
    void fixedOffsetZoneIsNeverDaylightSaving() {
        assertFalse(TimezoneUtil.isDaylightSavingTime(ZoneId.of("UTC")));
        assertFalse(TimezoneUtil.isDaylightSavingTime(ZoneId.of("+05:30")));
    }
}
