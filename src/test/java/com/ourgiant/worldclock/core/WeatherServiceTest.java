package com.ourgiant.worldclock.core;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherServiceTest {

    private static final String BASE_URL_PROPERTY = "worldclock.weatherApiBaseUrl";

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        System.setProperty(BASE_URL_PROPERTY, server.url("/v1/forecast").toString());
        WeatherService.clearCache();
    }

    @AfterEach
    void tearDown() throws IOException {
        WeatherService.clearCache();
        System.clearProperty(BASE_URL_PROPERTY);
        server.shutdown();
    }

    private static String forecastBody(double temp, int weatherCode, int humidity, double windSpeed) {
        return String.format(
                "{\"current\": {\"temperature_2m\": %s, \"weather_code\": %d, " +
                "\"relative_humidity_2m\": %d, \"wind_speed_10m\": %s}}",
                temp, weatherCode, humidity, windSpeed);
    }

    @Test
    void getWeatherParsesSuccessfulResponse() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(forecastBody(21.5, 0, 55, 10.2)));

        WeatherService.WeatherData data = WeatherService.getWeather(ZoneId.of("America/New_York"));

        assertNotNull(data);
        assertEquals(21.5, data.temperature);
        assertEquals("Clear", data.condition);
        assertEquals("0", data.weatherCode);
        assertEquals(55, data.humidity);
        assertEquals(10.2, data.windSpeed);
        assertEquals("America/New_York", data.location);

        RecordedRequest request = server.takeRequest();
        assertTrue(request.getPath().contains("latitude=40.71"));
        assertTrue(request.getPath().contains("longitude=-74.01"));
    }

    @Test
    void getWeatherReturnsNullOnNonSuccessResponse() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("server error"));

        WeatherService.WeatherData data = WeatherService.getWeather(ZoneId.of("America/New_York"));

        assertNull(data);
    }

    @Test
    void getWeatherReturnsNullOnMalformedResponse() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("not json"));

        WeatherService.WeatherData data = WeatherService.getWeather(ZoneId.of("America/New_York"));

        assertNull(data);
    }

    @Test
    void getWeatherCachesResultAndDoesNotRefetchWithinCacheWindow() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(forecastBody(21.5, 0, 55, 10.2)));

        WeatherService.getWeather(ZoneId.of("America/New_York"));
        WeatherService.WeatherData second = WeatherService.getWeather(ZoneId.of("America/New_York"));

        assertNotNull(second);
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void clearCacheForcesRefetch() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(forecastBody(21.5, 0, 55, 10.2)));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(forecastBody(22.0, 1, 50, 8.0)));

        WeatherService.getWeather(ZoneId.of("America/New_York"));
        WeatherService.clearCache();
        WeatherService.WeatherData second = WeatherService.getWeather(ZoneId.of("America/New_York"));

        assertNotNull(second);
        assertEquals(22.0, second.temperature);
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void getWeatherMapsWeatherCodesToDescriptions() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(forecastBody(0, 61, 50, 5.0)));
        WeatherService.WeatherData rain = WeatherService.getWeather(ZoneId.of("America/New_York"));
        assertEquals("Rain", rain.condition);

        WeatherService.clearCache();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(forecastBody(0, 95, 50, 5.0)));
        WeatherService.WeatherData storm = WeatherService.getWeather(ZoneId.of("America/New_York"));
        assertEquals("Thunderstorm", storm.condition);

        WeatherService.clearCache();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(forecastBody(0, 999, 50, 5.0)));
        WeatherService.WeatherData unknown = WeatherService.getWeather(ZoneId.of("America/New_York"));
        assertEquals("Unknown", unknown.condition);
    }

    @Test
    void getWeatherUsesDefaultCoordinatesForUnmappedTimezone() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(forecastBody(15.0, 3, 60, 4.0)));

        WeatherService.WeatherData data = WeatherService.getWeather(ZoneId.of("Africa/Cairo"));

        assertNotNull(data);
        RecordedRequest request = server.takeRequest();
        assertTrue(request.getPath().contains("latitude=0.00"));
        assertTrue(request.getPath().contains("longitude=0.00"));
    }

    @Test
    void weatherDataGetTemperatureDisplayFormatsBothUnits() {
        WeatherService.WeatherData data = new WeatherService.WeatherData(21.5, "Clear", "0", 55, 10.2, "America/New_York");

        assertEquals("21°C / 71°F", data.getTemperatureDisplay());
    }
}
