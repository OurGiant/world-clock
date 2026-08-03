package com.ourgiant.worldclock.core;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Catalog of timezone IDs available for selection. Pure domain logic, no Swing dependency.
 */
public class TimezoneCatalog {
    private static final Set<String> COMMON_ZONES = new TreeSet<>();

    static {
        // Add commonly used timezones
        COMMON_ZONES.addAll(List.of(
                "UTC",
                "America/New_York",
                "America/Chicago",
                "America/Denver",
                "America/Los_Angeles",
                "America/Anchorage",
                "Pacific/Honolulu",
                "Europe/London",
                "Europe/Paris",
                "Europe/Berlin",
                "Europe/Moscow",
                "Asia/Dubai",
                "Asia/Kolkata",
                "Asia/Bangkok",
                "Asia/Singapore",
                "Asia/Tokyo",
                "Asia/Shanghai",
                "Asia/Hong_Kong",
                "Australia/Sydney",
                "Australia/Brisbane",
                "Pacific/Auckland",
                "Pacific/Fiji",
                "America/Sao_Paulo",
                "America/Mexico_City",
                "America/Toronto"
        ));
    }

    private TimezoneCatalog() {
    }

    /**
     * Get sorted list of common timezones
     */
    public static List<String> getCommonTimeZones() {
        return new ArrayList<>(COMMON_ZONES);
    }

    /**
     * Get all available timezones (comprehensive list)
     */
    public static List<String> getAllTimeZones() {
        return ZoneId.getAvailableZoneIds().stream()
                .sorted()
                .toList();
    }
}
