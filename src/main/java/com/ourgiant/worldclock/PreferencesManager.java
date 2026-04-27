package com.ourgiant.worldclock;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages application preferences and persists them to a JSON file.
 */
public class PreferencesManager {
    private static final Path PREFS_DIR = Paths.get(System.getProperty("user.home"), ".worldclock");
    private static final String PREFS_FILE = "preferences.json";
    private static final Path PREFS_PATH = PREFS_DIR.resolve(PREFS_FILE);

    private static final String[] DEFAULT_ZONES = {"America/New_York", "Europe/London", "Asia/Tokyo"};
    private static final String TIMEZONES_KEY = "timezones";
    private static final String DISPLAY_SECONDS_KEY = "displaySeconds";
    private static final String API_NINJA_KEY = "apiNinjaKey";
    private static final String CURRENT_LOCATION = "currentLocation";
    private static final boolean DEFAULT_DISPLAY_SECONDS = true;
    private static final String DEFAULT_API_KEY = "";
    private static final String DEFAULT_CURRENT_LOCATION = "38.8909853,-77.026671";

    /**
     * Load saved timezone preferences from file.
     * Returns default zones if file doesn't exist or is invalid.
     */
    public static List<String> loadTimeZonePreferences() {
        List<String> zones = new ArrayList<>();

        try {
            if (Files.exists(PREFS_PATH)) {
                String content = Files.readString(PREFS_PATH);
                JSONObject json = new JSONObject(content);

                if (json.has(TIMEZONES_KEY)) {
                    var tzArray = json.getJSONArray(TIMEZONES_KEY);
                    for (int i = 0; i < tzArray.length(); i++) {
                        String zone = tzArray.getString(i);
                        // Validate timezone exists
                        try {
                            ZoneId.of(zone);
                            zones.add(zone);
                        } catch (ZoneRulesException e) {
                            System.err.println("Invalid timezone in preferences: " + zone + ". Using default.");
                        }
                    }
                    
                    // If we loaded valid zones, return them
                    if (!zones.isEmpty()) {
                        return zones;
                    }
                }
            }
        } catch (IOException | org.json.JSONException e) {
            System.err.println("Error loading preferences: " + e.getMessage());
        }

        // Return defaults if file doesn't exist, is invalid, or contained no valid zones
        for (String zone : DEFAULT_ZONES) {
            zones.add(zone);
        }
        return zones;
    }

    /**
     * Save timezone preferences to file with restricted permissions.
     * Returns true if successful, false otherwise.
     */
    public static boolean saveTimeZonePreferences(List<String> timeZones) {
        try {
            // Create directory if it doesn't exist
            Files.createDirectories(PREFS_DIR);

            // Create JSON object
            JSONObject json = new JSONObject();
            json.put(TIMEZONES_KEY, timeZones);

            // Write to file
            Files.writeString(PREFS_PATH, json.toString(2));
            
            // Set restrictive file permissions (read/write for owner only)
            try {
                Files.setPosixFilePermissions(PREFS_PATH, 
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException e) {
                // Windows doesn't support POSIX permissions, silently skip
                // The file is still created with default Windows permissions
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Error saving preferences: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the preferences file path (useful for debugging/testing)
     */
    public static Path getPreferencesPath() {
        return PREFS_PATH;
    }

    /**
     * Load display seconds preference.
     * Returns default if not set or preference file doesn't exist.
     */
    public static boolean loadDisplaySeconds() {
        try {
            if (Files.exists(PREFS_PATH)) {
                String content = Files.readString(PREFS_PATH);
                JSONObject json = new JSONObject(content);

                if (json.has(DISPLAY_SECONDS_KEY)) {
                    return json.getBoolean(DISPLAY_SECONDS_KEY);
                }
            }
        } catch (IOException | org.json.JSONException e) {
            System.err.println("Error loading display seconds preference: " + e.getMessage());
        }

        return DEFAULT_DISPLAY_SECONDS;
    }

    /**
     * Load API Ninja API key from preferences.
     * Returns empty string if not set or preference file doesn't exist.
     */
    public static String loadApiNinjaKey() {
        try {
            if (Files.exists(PREFS_PATH)) {
                String content = Files.readString(PREFS_PATH);
                JSONObject json = new JSONObject(content);

                if (json.has(API_NINJA_KEY)) {
                    return json.getString(API_NINJA_KEY);
                }
            }
        } catch (IOException | org.json.JSONException e) {
            System.err.println("Error loading API key: " + e.getMessage());
        }

        return DEFAULT_API_KEY;
    }

    /**
     * Load current location from preferences.
     * Returns empty string if not set or preference file doesn't exist.
     */
    public static String loadCurrentLocation() {
        try {
            if (Files.exists(PREFS_PATH)) {
                String content = Files.readString(PREFS_PATH);
                JSONObject json = new JSONObject(content);

                if (json.has(CURRENT_LOCATION)) {
                    return json.getString(CURRENT_LOCATION);
                }
            }
        } catch (IOException | org.json.JSONException e) {
            System.err.println("Error loading current location: " + e.getMessage());
        }

        return DEFAULT_CURRENT_LOCATION;
    }    

    /**
     * Save all preferences (timezones, display settings, and API key).
     * Returns true if successful, false otherwise.
     */
    public static boolean savePreferences(List<String> timeZones, boolean displaySeconds, String apiNinjaKey, String currentLocation) {
        try {
            // Create directory if it doesn't exist
            Files.createDirectories(PREFS_DIR);

            // Create JSON object
            JSONObject json = new JSONObject();
            json.put(TIMEZONES_KEY, timeZones);
            json.put(DISPLAY_SECONDS_KEY, displaySeconds);
            json.put(API_NINJA_KEY, apiNinjaKey != null ? apiNinjaKey : DEFAULT_API_KEY);
            json.put(CURRENT_LOCATION, currentLocation != null ? currentLocation : DEFAULT_CURRENT_LOCATION);

            // Write to file
            Files.writeString(PREFS_PATH, json.toString(2));
            
            // Set restrictive file permissions (read/write for owner only)
            try {
                Files.setPosixFilePermissions(PREFS_PATH, 
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException e) {
                // Windows doesn't support POSIX permissions, silently skip
                // The file is still created with default Windows permissions
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Error saving preferences: " + e.getMessage());
            return false;
        }
    }
}
