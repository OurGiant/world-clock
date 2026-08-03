package com.ourgiant.worldclock.core;

import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching weather data from Open-Meteo API (free, no API key required).
 */
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();
    
    // Simple in-memory cache with timestamp
    private static final Map<String, CachedWeather> weatherCache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 15 * 60 * 1000; // 15 minutes
    
    public static class WeatherData {
        private static String currentLocation = null;
        public double temperature;
        public String condition;
        public String weatherCode;
        public int humidity;
        public double windSpeed;
        public String location;
        
        public WeatherData(double temperature, String condition, String weatherCode, 
                          int humidity, double windSpeed, String location) {
            this.temperature = temperature;
            this.condition = condition;
            this.weatherCode = weatherCode;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.location = location;
        }
        
        /**
         * Get temperature in both Celsius and Fahrenheit
         */
        public String getTemperatureDisplay() {
            int celsius = (int) temperature;
            int fahrenheit = (int) Math.round((temperature * 9.0 / 5.0) + 32);
            return String.format("%d°C / %d°F", celsius, fahrenheit);
        }
        
        @Override
        public String toString() {
            return String.format("%d°C - %s", (int)temperature, condition);
        }

        public static void setCurrentLocation(String location) {
            currentLocation = (location != null && !location.isEmpty()) ? location : null;
        } 
    }


    
    private static class CachedWeather {
        WeatherData data;
        long timestamp;
        
        CachedWeather(WeatherData data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
    
    /**
     * Fetch weather for a given timezone
     * @param zoneId The timezone to get weather for
     * @return WeatherData object, or null if fetch fails
     */
    public static WeatherData getWeather(ZoneId zoneId) {
        String cacheKey = zoneId.getId();
        
        // Check cache
        if (weatherCache.containsKey(cacheKey)) {
            CachedWeather cached = weatherCache.get(cacheKey);
            if (!cached.isExpired()) {
                return cached.data;
            }
        }
        
        try {
            // Get approximate coordinates for timezone (simplified approach)
            // In production, you'd want a proper timezone -> lat/lon mapping
            double[] coords = getApproximateCoordinates(zoneId);
            if (coords == null) return null;
            
            double latitude = coords[0];
            double longitude = coords[1];
            
            String url = String.format(
                "%s?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m&timezone=%s",
                OPEN_METEO_URL, latitude, longitude, zoneId.getId()
            );
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;
                
                JSONObject json = new JSONObject(response.body().string());
                JSONObject current = json.getJSONObject("current");
                
                double temp = current.getDouble("temperature_2m");
                int weatherCode = current.getInt("weather_code");
                int humidity = current.getInt("relative_humidity_2m");
                double windSpeed = current.getDouble("wind_speed_10m");
                String condition = getWeatherDescription(weatherCode);
                
                WeatherData data = new WeatherData(temp, condition, String.valueOf(weatherCode), 
                                                  humidity, windSpeed, zoneId.getId());
                
                // Cache the result
                weatherCache.put(cacheKey, new CachedWeather(data));
                return data;
            }
        } catch (Exception e) {
            logger.error("Error fetching weather: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get approximate coordinates for a timezone (simplified mapping)
     */
    private static double[] getApproximateCoordinates(ZoneId zoneId) {
        Map<String, double[]> timezoneCoords = new HashMap<>();
        timezoneCoords.put("UTC", new double[]{0, 0});
        timezoneCoords.put("America/New_York", new double[]{40.7128, -74.0060});
        timezoneCoords.put("America/Los_Angeles", new double[]{34.0522, -118.2437});
        timezoneCoords.put("America/Chicago", new double[]{41.8781, -87.6298});
        timezoneCoords.put("Europe/London", new double[]{51.5074, -0.1278});
        timezoneCoords.put("Europe/Paris", new double[]{48.8566, 2.3522});
        timezoneCoords.put("Europe/Berlin", new double[]{52.5200, 13.4050});
        timezoneCoords.put("Asia/Tokyo", new double[]{35.6762, 139.6503});
        timezoneCoords.put("Asia/Shanghai", new double[]{31.2304, 121.4737});
        timezoneCoords.put("Asia/Hong_Kong", new double[]{22.3193, 114.1694});
        timezoneCoords.put("Asia/Singapore", new double[]{1.3521, 103.8198});
        timezoneCoords.put("Asia/Dubai", new double[]{25.2048, 55.2708});
        timezoneCoords.put("Australia/Sydney", new double[]{-33.8688, 151.2093});
        timezoneCoords.put("Australia/Melbourne", new double[]{-37.8136, 144.9631});
        timezoneCoords.put("Pacific/Auckland", new double[]{-41.2865, 174.8860});
        
        return timezoneCoords.getOrDefault(zoneId.getId(), new double[]{0, 0});
    }
    
    /**
     * Convert WMO weather code to human-readable description
     */
    private static String getWeatherDescription(int code) {
        return switch (code) {
            case 0 -> "Clear";
            case 1, 2 -> "Partly Cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow Grains";
            case 80, 81, 82 -> "Rain Shower";
            case 85, 86 -> "Snow Shower";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown";
        };
    }
    
    /**
     * Clear the weather cache
     */
    public static void clearCache() {
        weatherCache.clear();
    }
}
