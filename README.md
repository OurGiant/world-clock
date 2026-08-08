# World Clock Application

A multi-timezone desktop world clock built with Java Swing. Shows a
primary UTC clock plus three user-selectable timezone clocks, each with
live weather and today's public holiday for that zone.

## Features

- [x] Display current time for a primary UTC clock and three
      user-selectable timezones
- [x] Change timezones live via dropdown selectors
- [x] Digital clock display with optional seconds
- [x] Per-clock weather (temperature in °C/°F, condition) via Open-Meteo
- [x] Per-clock public holiday lookup via API Ninjas (requires a free API key)
- [x] Preferences dialog for timezone selection, display options, API
      key, and current location
- [x] Preferences persisted to `~/.worldclock/preferences.json`
- [x] FlatLaf dark theme

## Project Setup

This is a Maven project configured to build a fat (uber) JAR with all
dependencies included.

### Build

```bash
mvn clean package
```

This generates `worldclock.jar` (thin) and `worldclock-all.jar` (fat,
runnable) in the `target/` directory.

### Test

```bash
mvn test
```

Tests never touch your real `~/.worldclock` — see
[Configuration](#configuration) below.

### Run

```bash
java -jar target/worldclock-all.jar
```

## Project Structure

```
world-clock/
├── src/
│   ├── main/
│   │   ├── java/com/ourgiant/worldclock/
│   │   │   ├── core/                  domain logic, no Swing dependency
│   │   │   │   ├── HolidayService.java
│   │   │   │   ├── PreferencesManager.java
│   │   │   │   ├── TimezoneCatalog.java
│   │   │   │   ├── TimezoneUtil.java
│   │   │   │   └── WeatherService.java
│   │   │   ├── gui/                   all Swing UI
│   │   │   │   ├── DigitalClock.java
│   │   │   │   ├── PreferencesDialog.java
│   │   │   │   ├── TimeZoneSelector.java
│   │   │   │   └── WorldClockApp.java (entry point)
│   │   │   └── util/
│   │   │       └── AppVersion.java
│   │   └── resources/
│   │       ├── logback.xml
│   │       └── version.properties     filtered at build time
│   └── test/
│       └── java/com/ourgiant/worldclock/core/
│           ├── PreferencesManagerTest.java
│           ├── TimezoneCatalogTest.java
│           └── TimezoneUtilTest.java
├── .claude/skills/                    verify + ship-issue workflow docs
├── pom.xml
└── README.md
```

`gui/` depends one-way on `core/` — the domain logic has zero
`javax.swing.*` dependency, so it's testable headlessly.

## Dependencies

- **[FlatLaf](https://www.formdev.com/flatlaf/)** (+ `flatlaf-intellij-themes`, `flatlaf-extras`) — look and feel
- **[SLF4J](https://www.slf4j.org/) + [Logback](https://logback.qos.ch/)** — logging (console + rolling file under `~/.worldclock/logs`)
- **[OkHttp](https://square.github.io/okhttp/)** — HTTP client for the weather/holiday APIs
- **[org.json](https://github.com/stleary/JSON-java)** — JSON parsing
- **JUnit 5 + Mockito** (test scope) — testing

## Configuration

Preferences (selected timezones, display-seconds toggle, API Ninja key,
current location) are persisted to `~/.worldclock/preferences.json` with
owner-only file permissions where the OS supports POSIX permissions.

Tests never touch this real file: `PreferencesManager` honors a
`worldclock.prefsDir` system property override, which `pom.xml`'s
surefire config points at a build-directory-relative path during
`mvn test`.

## Hardening / outbound calls

This app makes outbound HTTP calls to two third-party APIs:

- **Open-Meteo** (`WeatherService`) — no API key required. 5s connect/read
  timeouts. Responses are cached in memory per timezone for 15 minutes.
  On failure, the clock simply omits the weather line (`getWeather`
  returns `null`, checked before rendering).
- **API Ninjas** (`HolidayService`) — requires a user-supplied API key,
  entered via the Preferences dialog and stored in
  `~/.worldclock/preferences.json`. Requests retry up to 3 times with
  exponential backoff on network/parse errors, but stop immediately (no
  retry) on a non-2xx HTTP response to avoid burning API quota. Results
  (including "no holidays" and failures) are cached per country code for
  24 hours. With no key configured, holiday lookups are skipped entirely
  (`getHolidaysForTimezone` returns an empty list without making a
  request).

Neither service's key material is logged; `HolidayService` logs response
bodies on error but truncates them to 200 characters.

- **GitHub Releases** (`UpdateChecker`) — bare `GET` to the public
  `api.github.com/repos/OurGiant/world-clock/releases/latest` endpoint, no
  auth, no user data in the request. 5s connect / 10s read timeouts. A TLS
  handshake failure is distinguished from a generic failure and surfaced
  with a "possible corporate network proxy" message; any other failure or
  non-2xx response is silent (`Optional.empty()`, logged at WARN). Before
  the release URL from the response is opened in the user's browser,
  `AboutDialog.isTrustedReleaseUrl` validates it's exactly
  `https://github.com/...` — defense-in-depth against a tampered/MITM'd
  API response.

  **Accepted deviation:** the sibling-project standard specifies
  `java.net.http.HttpClient` for this call; `UpdateChecker` here uses
  OkHttp instead, matching `WeatherService`/`HolidayService` above. This
  keeps World Clock on a single HTTP stack rather than splitting a
  three-call surface across two libraries for no functional gain — all
  three calls get the same timeout/retry/logging discipline either way.
  Reassessed and confirmed 2026-08-08 (issue #20); not planned for
  migration.

## License

MIT — see [LICENSE](LICENSE).
