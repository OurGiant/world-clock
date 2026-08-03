package com.ourgiant.worldclock.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferencesManagerTest {

    @BeforeEach
    void clearPreferences() throws IOException {
        Files.deleteIfExists(PreferencesManager.getPreferencesPath());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(PreferencesManager.getPreferencesPath());
    }

    @Test
    void preferencesPathIsRedirectedAwayFromRealUserHome() {
        Path realPrefsDir = Path.of(System.getProperty("user.home"), ".worldclock");
        assertNotEquals(realPrefsDir, PreferencesManager.getPreferencesPath().getParent(),
                "test run must not touch the real ~/.worldclock (see worldclock.prefsDir surefire property)");
    }

    @Test
    void loadTimeZonePreferencesReturnsDefaultsWhenFileMissing() {
        assertEquals(List.of("America/New_York", "Europe/London", "Asia/Tokyo"),
                PreferencesManager.loadTimeZonePreferences());
    }

    @Test
    void saveThenLoadTimeZonePreferencesRoundTrips() {
        List<String> saved = List.of("UTC", "Asia/Tokyo", "Europe/Paris");
        assertTrue(PreferencesManager.saveTimeZonePreferences(saved));
        assertEquals(saved, PreferencesManager.loadTimeZonePreferences());
    }

    @Test
    void loadTimeZonePreferencesSkipsInvalidZonesAndKeepsValidOnes() throws IOException {
        Path path = PreferencesManager.getPreferencesPath();
        Files.createDirectories(path.getParent());
        Files.writeString(path, "{\"timezones\": [\"UTC\", \"Not/AZone\", \"Asia/Tokyo\"]}");

        assertEquals(List.of("UTC", "Asia/Tokyo"), PreferencesManager.loadTimeZonePreferences());
    }

    @Test
    void loadTimeZonePreferencesFallsBackToDefaultsWhenAllZonesInvalid() throws IOException {
        Path path = PreferencesManager.getPreferencesPath();
        Files.createDirectories(path.getParent());
        Files.writeString(path, "{\"timezones\": [\"Not/AZone\"]}");

        assertEquals(List.of("America/New_York", "Europe/London", "Asia/Tokyo"),
                PreferencesManager.loadTimeZonePreferences());
    }

    @Test
    void loadDisplaySecondsDefaultsToTrueWhenUnset() {
        assertTrue(PreferencesManager.loadDisplaySeconds());
    }

    @Test
    void loadApiNinjaKeyDefaultsToEmptyWhenUnset() {
        assertEquals("", PreferencesManager.loadApiNinjaKey());
    }

    @Test
    void loadCurrentLocationDefaultsWhenUnset() {
        assertEquals("38.8909853,-77.026671", PreferencesManager.loadCurrentLocation());
    }

    @Test
    void savePreferencesRoundTripsAllFields() {
        List<String> zones = List.of("UTC", "America/Chicago", "Europe/Berlin");
        assertTrue(PreferencesManager.savePreferences(zones, false, "test-api-key", "1.0,2.0"));

        assertEquals(zones, PreferencesManager.loadTimeZonePreferences());
        assertFalse(PreferencesManager.loadDisplaySeconds());
        assertEquals("test-api-key", PreferencesManager.loadApiNinjaKey());
        assertEquals("1.0,2.0", PreferencesManager.loadCurrentLocation());
    }

    @Test
    void savePreferencesFallsBackToDefaultsForNullApiKeyAndLocation() {
        List<String> zones = List.of("UTC", "America/Chicago", "Europe/Berlin");
        assertTrue(PreferencesManager.savePreferences(zones, true, null, null));

        assertEquals("", PreferencesManager.loadApiNinjaKey());
        assertEquals("38.8909853,-77.026671", PreferencesManager.loadCurrentLocation());
    }
}
