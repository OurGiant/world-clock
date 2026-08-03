package com.ourgiant.worldclock.core;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TimezoneCatalogTest {

    @Test
    void commonTimeZonesIncludesUtcAndIsSorted() {
        List<String> zones = TimezoneCatalog.getCommonTimeZones();
        assertTrue(zones.contains("UTC"));

        List<String> sorted = new ArrayList<>(zones);
        Collections.sort(sorted);
        assertEquals(sorted, zones);
    }

    @Test
    void allTimeZonesIsSortedAndIncludesCommonEntries() {
        List<String> all = TimezoneCatalog.getAllTimeZones();
        assertTrue(all.contains("America/New_York"));

        List<String> sorted = new ArrayList<>(all);
        Collections.sort(sorted);
        assertEquals(sorted, all);
    }

    @Test
    void allTimeZonesIsSupersetOfCommonZones() {
        List<String> all = TimezoneCatalog.getAllTimeZones();
        for (String zone : TimezoneCatalog.getCommonTimeZones()) {
            assertTrue(all.contains(zone), zone + " missing from full zone list");
        }
    }

    @Test
    void everyCommonZoneIsAValidZoneId() {
        for (String zone : TimezoneCatalog.getCommonTimeZones()) {
            assertDoesNotThrow(() -> ZoneId.of(zone), zone + " is not a valid ZoneId");
        }
    }
}
