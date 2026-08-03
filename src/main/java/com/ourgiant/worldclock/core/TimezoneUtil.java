package com.ourgiant.worldclock.core;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Utility class for timezone-related calculations and formatting.
 */
public class TimezoneUtil {
    
    /**
     * Get the UTC offset as a formatted string (e.g., "+5:30", "-8:00")
     * @param zoneId The ZoneId to calculate offset for
     * @return Formatted UTC offset string
     */
    public static String getUTCOffsetString(ZoneId zoneId) {
        ZonedDateTime now = LocalDateTime.now().atZone(zoneId);
        int totalSeconds = now.getOffset().getTotalSeconds();
        
        int hours = totalSeconds / 3600;
        int minutes = Math.abs((totalSeconds % 3600) / 60);
        
        String sign = hours >= 0 ? "+" : "";
        return String.format("UTC %s%d:%02d", sign, hours, minutes);
    }
    
    /**
     * Get the UTC offset in hours (can be decimal for half-hour offsets)
     * @param zoneId The ZoneId to calculate offset for
     * @return UTC offset in hours
     */
    public static double getUTCOffsetHours(ZoneId zoneId) {
        ZonedDateTime now = LocalDateTime.now().atZone(zoneId);
        int totalSeconds = now.getOffset().getTotalSeconds();
        return totalSeconds / 3600.0;
    }
    
    /**
     * Check if a timezone is currently observing daylight saving time
     * @param zoneId The ZoneId to check
     * @return true if DST is active, false otherwise
     */
    public static boolean isDaylightSavingTime(ZoneId zoneId) {
        ZonedDateTime now = LocalDateTime.now().atZone(zoneId);
        return now.getOffset().getTotalSeconds() != 
               LocalDateTime.now().atZone(zoneId.normalized()).getOffset().getTotalSeconds();
    }
}
