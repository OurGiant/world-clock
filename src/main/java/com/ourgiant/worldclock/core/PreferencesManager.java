package com.ourgiant.worldclock.core;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(PreferencesManager.class);

    /** System property override for tests -- without it, every test that saves/loads
     * preferences would touch the real developer's {@code ~/.worldclock} on every
     * {@code mvn test} run. Set via pom.xml's surefire config to a build-directory-relative
     * path, mirroring kiro-control-panel's {@code kiro.control.panel.changeLogFile} precedent. */
    private static final String PREFS_DIR_OVERRIDE_PROPERTY = "worldclock.prefsDir";
    private static final String PREFS_FILE = "preferences.json";

    private static final String[] DEFAULT_ZONES = {"America/New_York", "Europe/London", "Asia/Tokyo"};
    private static final String TIMEZONES_KEY = "timezones";
    private static final String DISPLAY_SECONDS_KEY = "displaySeconds";
    private static final String API_NINJA_KEY = "apiNinjaKey";
    private static final String CURRENT_LOCATION = "currentLocation";
    private static final String LAST_NOTIFIED_UPDATE_VERSION_KEY = "lastNotifiedUpdateVersion";
    private static final boolean DEFAULT_DISPLAY_SECONDS = true;
    private static final String DEFAULT_API_KEY = "";
    private static final String DEFAULT_CURRENT_LOCATION = "38.8909853,-77.026671";
    private static final String DEFAULT_LAST_NOTIFIED_UPDATE_VERSION = "";

    private static Path prefsDir() {
        String override = System.getProperty(PREFS_DIR_OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        return Paths.get(System.getProperty("user.home"), ".worldclock");
    }

    private static Path prefsPath() {
        return prefsDir().resolve(PREFS_FILE);
    }

    /**
     * Load saved timezone preferences from file.
     * Returns default zones if file doesn't exist or is invalid.
     */
    public static List<String> loadTimeZonePreferences() {
        List<String> zones = new ArrayList<>();

        try {
            if (Files.exists(prefsPath())) {
                String content = Files.readString(prefsPath());
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
                            logger.warn("Invalid timezone in preferences: {}. Using default.", zone);
                        }
                    }
                    
                    // If we loaded valid zones, return them
                    if (!zones.isEmpty()) {
                        return zones;
                    }
                }
            }
        } catch (IOException | org.json.JSONException e) {
            logger.error("Error loading preferences: {}", e.getMessage());
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
            Files.createDirectories(prefsDir());

            // Create JSON object
            JSONObject json = new JSONObject();
            json.put(TIMEZONES_KEY, timeZones);

            // Write to file
            Files.writeString(prefsPath(), json.toString(2));
            
            // Set restrictive file permissions (read/write for owner only)
            try {
                Files.setPosixFilePermissions(prefsPath(), 
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException e) {
                // Windows doesn't support POSIX permissions, silently skip
                // The file is still created with default Windows permissions
            }
            
            return true;
        } catch (IOException e) {
            logger.error("Error saving preferences: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the preferences file path (useful for debugging/testing)
     */
    public static Path getPreferencesPath() {
        return prefsPath();
    }

    /**
     * Load display seconds preference.
     * Returns default if not set or preference file doesn't exist.
     */
    public static boolean loadDisplaySeconds() {
        try {
            if (Files.exists(prefsPath())) {
                String content = Files.readString(prefsPath());
                JSONObject json = new JSONObject(content);

                if (json.has(DISPLAY_SECONDS_KEY)) {
                    return json.getBoolean(DISPLAY_SECONDS_KEY);
                }
            }
        } catch (IOException | org.json.JSONException e) {
            logger.error("Error loading display seconds preference: {}", e.getMessage());
        }

        return DEFAULT_DISPLAY_SECONDS;
    }

    /**
     * Load API Ninja API key from preferences.
     * Returns empty string if not set or preference file doesn't exist.
     */
    public static String loadApiNinjaKey() {
        try {
            if (Files.exists(prefsPath())) {
                String content = Files.readString(prefsPath());
                JSONObject json = new JSONObject(content);

                if (json.has(API_NINJA_KEY)) {
                    return json.getString(API_NINJA_KEY);
                }
            }
        } catch (IOException | org.json.JSONException e) {
            logger.error("Error loading API key: {}", e.getMessage());
        }

        return DEFAULT_API_KEY;
    }

    /**
     * Load current location from preferences.
     * Returns empty string if not set or preference file doesn't exist.
     */
    public static String loadCurrentLocation() {
        try {
            if (Files.exists(prefsPath())) {
                String content = Files.readString(prefsPath());
                JSONObject json = new JSONObject(content);

                if (json.has(CURRENT_LOCATION)) {
                    return json.getString(CURRENT_LOCATION);
                }
            }
        } catch (IOException | org.json.JSONException e) {
            logger.error("Error loading current location: {}", e.getMessage());
        }

        return DEFAULT_CURRENT_LOCATION;
    }    

    /**
     * Load the version string of the last release the silent startup update check already
     * notified the user about, so that check doesn't re-notify for the same release every launch.
     * Returns empty string if not set or preference file doesn't exist.
     */
    public static String loadLastNotifiedUpdateVersion() {
        try {
            if (Files.exists(prefsPath())) {
                String content = Files.readString(prefsPath());
                JSONObject json = new JSONObject(content);

                if (json.has(LAST_NOTIFIED_UPDATE_VERSION_KEY)) {
                    return json.getString(LAST_NOTIFIED_UPDATE_VERSION_KEY);
                }
            }
        } catch (IOException | org.json.JSONException e) {
            logger.error("Error loading last notified update version: {}", e.getMessage());
        }

        return DEFAULT_LAST_NOTIFIED_UPDATE_VERSION;
    }

    /**
     * Record that the user has already been notified about {@code version}, so the silent
     * startup check doesn't show it again on the next launch. Merges into the existing
     * preferences file rather than overwriting it, unlike {@link #savePreferences}.
     * Returns true if successful, false otherwise.
     */
    public static boolean saveLastNotifiedUpdateVersion(String version) {
        try {
            Files.createDirectories(prefsDir());

            JSONObject json = new JSONObject();
            if (Files.exists(prefsPath())) {
                json = new JSONObject(Files.readString(prefsPath()));
            }
            json.put(LAST_NOTIFIED_UPDATE_VERSION_KEY, version != null ? version : DEFAULT_LAST_NOTIFIED_UPDATE_VERSION);

            Files.writeString(prefsPath(), json.toString(2));

            try {
                Files.setPosixFilePermissions(prefsPath(),
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException e) {
                // Windows doesn't support POSIX permissions, silently skip
                // The file is still created with default Windows permissions
            }

            return true;
        } catch (IOException | org.json.JSONException e) {
            logger.error("Error saving last notified update version: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Save all preferences (timezones, display settings, and API key).
     * Returns true if successful, false otherwise.
     */
    public static boolean savePreferences(List<String> timeZones, boolean displaySeconds, String apiNinjaKey, String currentLocation) {
        try {
            // Create directory if it doesn't exist
            Files.createDirectories(prefsDir());

            // Create JSON object
            JSONObject json = new JSONObject();
            json.put(TIMEZONES_KEY, timeZones);
            json.put(DISPLAY_SECONDS_KEY, displaySeconds);
            json.put(API_NINJA_KEY, apiNinjaKey != null ? apiNinjaKey : DEFAULT_API_KEY);
            json.put(CURRENT_LOCATION, currentLocation != null ? currentLocation : DEFAULT_CURRENT_LOCATION);

            // Write to file
            Files.writeString(prefsPath(), json.toString(2));
            
            // Set restrictive file permissions (read/write for owner only)
            try {
                Files.setPosixFilePermissions(prefsPath(), 
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException e) {
                // Windows doesn't support POSIX permissions, silently skip
                // The file is still created with default Windows permissions
            }
            
            return true;
        } catch (IOException e) {
            logger.error("Error saving preferences: {}", e.getMessage());
            return false;
        }
    }
}
