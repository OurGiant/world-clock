package com.ourgiant.worldclock;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for getting holiday information from API Ninjas.
 * Requires API key to be set via setApiNinjaKey().
 */
public class HolidayService {
    
    private static String apiNinjaKey = null;
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    private static final Map<String, Long> apiCacheTime = new HashMap<>();
    private static final Map<String, List<Holiday>> holidayCache = new HashMap<>();
    
    public static class Holiday {
        public LocalDate date;
        public String name;
        public String country;
        
        public Holiday(LocalDate date, String name, String country) {
            this.date = date;
            this.name = name;
            this.country = country;
        }
        
        @Override
        public String toString() {
            return name + " (" + date + ")";
        }
    }
    
    /**
     * Set the API Ninja key for fetching holiday data from the API
     */
    public static void setApiNinjaKey(String key) {
        apiNinjaKey = (key != null && !key.isEmpty()) ? key : null;
    }
    
    /**
     * Get holidays for a given timezone
     * @param zoneId The timezone
     * @return List of holidays for that timezone's region (empty if no key or API fails)
     */
    public static List<Holiday> getHolidaysForTimezone(ZoneId zoneId) {
        if (apiNinjaKey == null) {
            return new ArrayList<>();
        }
        
        String countryCode = getCountryCodeFromTimezone(zoneId);
        
        // Check cache (both success and failure are cached)
        if (isCached(countryCode)) {
            return holidayCache.getOrDefault(countryCode, new ArrayList<>());
        }
        
        // Fetch from API
        List<Holiday> holidays = fetchHolidaysFromApiNinjas(countryCode);
        
        // Cache result (null, empty list, or actual holidays) - don't retry on next call
        List<Holiday> cachedResult = (holidays != null) ? holidays : new ArrayList<>();
        holidayCache.put(countryCode, cachedResult);
        apiCacheTime.put(countryCode, System.currentTimeMillis());
        
        return cachedResult;
    }
    
    private static boolean isCached(String country) {
        if (!apiCacheTime.containsKey(country)) {
            return false;
        }
        long lastUpdate = apiCacheTime.get(country);
        return System.currentTimeMillis() - lastUpdate < CACHE_DURATION;
    }
    
    /**
     * Fetch holidays from API Ninjas with exponential backoff retry logic
     * Max 3 attempts with failure on any non-2XX response
     * 
     * Note: Free tier returns only current year holidays.
     * The 'year' parameter is premium-only and will be omitted for free tier compatibility.
     */
    private static List<Holiday> fetchHolidaysFromApiNinjas(String countryCode) {
        final int MAX_RETRIES = 3;
        final long INITIAL_BACKOFF_MS = 1000; // 1 second
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Build URL without year parameter (free tier limitation)
                // Free tier defaults to current year, premium tier needs year parameter
                String url = String.format(
                    "https://api.api-ninjas.com/v1/holidays?country=%s&type=public_holiday",
                    countryCode
                );
                
                Request request = new Request.Builder()
                        .url(url)
                        .header("X-Api-Key", apiNinjaKey)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    int code = response.code();
                    
                    // Success (2XX)
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        List<Holiday> holidays = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            LocalDate date = LocalDate.parse(obj.getString("date"));
                            String name = obj.getString("name");
                            holidays.add(new Holiday(date, name, countryCode));
                        }

                        return holidays;
                    }
                    
                    // Non-2XX response - log details and stop retrying
                    String errorBody = "";
                    if (response.body() != null) {
                        errorBody = response.body().string();
                        if (errorBody.length() > 200) {
                            errorBody = errorBody.substring(0, 200) + "...";
                        }
                    }
                    
                    System.err.println("API Ninjas Holidays API returned " + code + " for " + countryCode);
                    System.err.println("  URL: " + response.request().url());
                    if (!errorBody.isEmpty()) {
                        System.err.println("  Response: " + errorBody);
                    }
                    String rateLimitRemaining = response.header("X-RateLimit-Requests-Remaining");
                    if (rateLimitRemaining != null) {
                        System.err.println("  Requests remaining: " + rateLimitRemaining);
                    }
                    
                    // Check if it's an auth issue (401, 403) or quota (400 with "premium" in message)
                    if ((code == 401 || code == 403) || 
                        (code == 400 && errorBody.toLowerCase().contains("premium"))) {
                        System.err.println("  Tip: This is a premium-only feature. Free tier returns current year only.");
                        System.err.println("  Upgrade at https://www.api-ninjas.com/pricing for multiple years");
                    }
                    
                    System.err.println("  Stopping retries to preserve API quota");
                    return null;
                }
                
            } catch (Exception e) {
                // Network or parsing error - retry with backoff
                if (attempt < MAX_RETRIES) {
                    long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    System.err.println("Holiday API attempt " + attempt + " failed: " + e.getMessage() + 
                                     ". Retrying in " + (backoffMs / 1000) + "s...");
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } else {
                    System.err.println("Holiday API failed after " + MAX_RETRIES + " attempts: " + e.getMessage());
                    return null;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get upcoming holidays (next 30 days)
     */
    public static List<Holiday> getUpcomingHolidays(ZoneId zoneId) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(30);
        
        return getHolidaysForTimezone(zoneId).stream()
                .filter(h -> !h.date.isBefore(today) && !h.date.isAfter(futureDate))
                .sorted(Comparator.comparing(h -> h.date))
                .toList();
    }
    
    /**
     * Check if a specific date is a holiday in a timezone
     */
    public static Holiday getHolidayForDate(ZoneId zoneId, LocalDate date) {
        return getHolidaysForTimezone(zoneId).stream()
                .filter(h -> h.date.equals(date))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Map timezone to country code for API Ninjas
     */
    private static String getCountryCodeFromTimezone(ZoneId zoneId) {
        return switch (zoneId.getId()) {
            case "America/New_York", "America/Chicago", "America/Los_Angeles", "America/Denver" -> "US";
            case "Europe/London" -> "GB";
            case "Europe/Paris" -> "FR";
            case "Europe/Berlin" -> "DE";
            case "Asia/Tokyo" -> "JP";
            case "Asia/Shanghai", "Asia/Hong_Kong" -> "CN";
            case "Australia/Sydney", "Australia/Melbourne" -> "AU";
            case "UTC" -> "US"; // Default to US for UTC
            default -> "US";
        };
    }
}
