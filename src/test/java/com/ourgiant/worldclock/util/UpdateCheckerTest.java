package com.ourgiant.worldclock.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void isNewerVersionTrueWhenPatchIsHigher() {
        assertTrue(UpdateChecker.isNewerVersion("1.0.11", "1.0.10"));
    }

    @Test
    void isNewerVersionTrueWhenMinorIsHigher() {
        assertTrue(UpdateChecker.isNewerVersion("1.1.0", "1.0.99"));
    }

    @Test
    void isNewerVersionFalseWhenEqual() {
        assertFalse(UpdateChecker.isNewerVersion("1.0.10", "1.0.10"));
    }

    @Test
    void isNewerVersionFalseWhenOlder() {
        assertFalse(UpdateChecker.isNewerVersion("1.0.9", "1.0.10"));
    }

    @Test
    void isNewerVersionHandlesDifferentSegmentCounts() {
        assertTrue(UpdateChecker.isNewerVersion("1.1", "1.0.99"));
        assertFalse(UpdateChecker.isNewerVersion("1.0", "1.0.0"));
    }

    @Test
    void isNewerVersionFalseOnNonNumericSegments() {
        assertFalse(UpdateChecker.isNewerVersion("dev", "1.0.10"));
        assertFalse(UpdateChecker.isNewerVersion("1.0.10", "dev"));
    }
}
