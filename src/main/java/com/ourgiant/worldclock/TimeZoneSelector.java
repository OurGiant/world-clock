package com.ourgiant.worldclock;

import javax.swing.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class for timezone selection and management.
 */
public class TimeZoneSelector {
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
                "Brazil/São Paulo",
                "America/Mexico_City",
                "America/Toronto"
        ));
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

    /**
     * Create a configured JComboBox for timezone selection
     */
    public static JComboBox<String> createTimeZoneComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        
        // Add common zones first
        for (String zone : getCommonTimeZones()) {
            comboBox.addItem(zone);
        }

        // Set a default selection
        comboBox.setSelectedItem("UTC");

        return comboBox;
    }

    /**
     * Create a JComboBox with all available timezones
     */
    public static JComboBox<String> createFullTimeZoneComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        
        for (String zone : getAllTimeZones()) {
            comboBox.addItem(zone);
        }

        comboBox.setSelectedItem("UTC");

        return comboBox;
    }
}
