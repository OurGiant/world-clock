package com.ourgiant.worldclock.gui;

import com.ourgiant.worldclock.core.TimezoneCatalog;

import javax.swing.*;

/**
 * Swing wrapper for timezone selection; the underlying timezone list comes from TimezoneCatalog.
 */
public class TimeZoneSelector {

    private TimeZoneSelector() {
    }

    /**
     * Create a configured JComboBox for timezone selection
     */
    public static JComboBox<String> createTimeZoneComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();

        // Add common zones first
        for (String zone : TimezoneCatalog.getCommonTimeZones()) {
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

        for (String zone : TimezoneCatalog.getAllTimeZones()) {
            comboBox.addItem(zone);
        }

        comboBox.setSelectedItem("UTC");

        return comboBox;
    }
}
