package com.ourgiant.worldclock.core;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolidayServiceTest {

    private static final String BASE_URL_PROPERTY = "worldclock.holidayApiBaseUrl";

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        System.setProperty(BASE_URL_PROPERTY, server.url("/v1/holidays").toString());
        HolidayService.clearCache();
        HolidayService.setApiNinjaKey("test-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        HolidayService.setApiNinjaKey(null);
        HolidayService.clearCache();
        System.clearProperty(BASE_URL_PROPERTY);
        server.shutdown();
    }

    @Test
    void getHolidaysForTimezoneReturnsEmptyListWithoutMakingARequestWhenNoKeyConfigured() throws InterruptedException {
        HolidayService.setApiNinjaKey(null);

        List<HolidayService.Holiday> holidays = HolidayService.getHolidaysForTimezone(ZoneId.of("America/New_York"));

        assertTrue(holidays.isEmpty());
        assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void getHolidaysForTimezoneParsesSuccessfulResponse() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[{\"date\": \"2026-07-04\", \"name\": \"Independence Day\", \"country\": \"US\"}]"));

        List<HolidayService.Holiday> holidays = HolidayService.getHolidaysForTimezone(ZoneId.of("America/New_York"));

        assertEquals(1, holidays.size());
        assertEquals(LocalDate.of(2026, 7, 4), holidays.get(0).date);
        assertEquals("Independence Day", holidays.get(0).name);

        RecordedRequest request = server.takeRequest();
        assertTrue(request.getPath().contains("country=US"));
        assertEquals("test-key", request.getHeader("X-Api-Key"));
    }

    @Test
    void getHolidaysForTimezoneReturnsEmptyListAndDoesNotRetryOnNon2xxResponse() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("unauthorized"));

        List<HolidayService.Holiday> holidays = HolidayService.getHolidaysForTimezone(ZoneId.of("America/New_York"));

        assertTrue(holidays.isEmpty());
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void getHolidaysForTimezoneCachesResultAndDoesNotRefetchWithinCacheWindow() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[{\"date\": \"2026-07-04\", \"name\": \"Independence Day\", \"country\": \"US\"}]"));

        HolidayService.getHolidaysForTimezone(ZoneId.of("America/New_York"));
        List<HolidayService.Holiday> secondCall = HolidayService.getHolidaysForTimezone(ZoneId.of("America/New_York"));

        assertEquals(1, secondCall.size());
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void fetchRetriesAndEventuallySucceedsAfterTransientFailures() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("not valid json"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("still not valid json"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[{\"date\": \"2026-12-25\", \"name\": \"Christmas\", \"country\": \"US\"}]"));

        List<HolidayService.Holiday> holidays = HolidayService.getHolidaysForTimezone(ZoneId.of("America/New_York"));

        assertEquals(1, holidays.size());
        assertEquals("Christmas", holidays.get(0).name);
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void fetchGivesUpAndReturnsEmptyListAfterMaxRetriesExhausted() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("not valid json"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("still not valid json"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("nope"));

        List<HolidayService.Holiday> holidays = HolidayService.getHolidaysForTimezone(ZoneId.of("America/New_York"));

        assertTrue(holidays.isEmpty());
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void getUpcomingHolidaysFiltersToNext30Days() {
        LocalDate today = LocalDate.now();
        String inRange = today.plusDays(5).toString();
        String tooFar = today.plusDays(60).toString();
        String inPast = today.minusDays(5).toString();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(String.format(
                        "[{\"date\": \"%s\", \"name\": \"Soon\", \"country\": \"US\"}, " +
                        "{\"date\": \"%s\", \"name\": \"TooFar\", \"country\": \"US\"}, " +
                        "{\"date\": \"%s\", \"name\": \"Past\", \"country\": \"US\"}]",
                        inRange, tooFar, inPast)));

        List<HolidayService.Holiday> upcoming = HolidayService.getUpcomingHolidays(ZoneId.of("America/New_York"));

        assertEquals(1, upcoming.size());
        assertEquals("Soon", upcoming.get(0).name);
    }

    @Test
    void getHolidayForDateFindsMatchingHoliday() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[{\"date\": \"2026-07-04\", \"name\": \"Independence Day\", \"country\": \"US\"}]"));

        HolidayService.Holiday match = HolidayService.getHolidayForDate(ZoneId.of("America/New_York"), LocalDate.of(2026, 7, 4));
        HolidayService.Holiday noMatch = HolidayService.getHolidayForDate(ZoneId.of("America/New_York"), LocalDate.of(2026, 7, 5));

        assertEquals("Independence Day", match.name);
        assertNull(noMatch);
    }

    @Test
    void getHolidaysForTimezoneMapsKnownTimezonesToExpectedCountryCode() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        HolidayService.getHolidaysForTimezone(ZoneId.of("Europe/London"));
        assertTrue(server.takeRequest().getPath().contains("country=GB"));

        HolidayService.getHolidaysForTimezone(ZoneId.of("Asia/Tokyo"));
        assertTrue(server.takeRequest().getPath().contains("country=JP"));
    }
}
