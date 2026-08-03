package com.ourgiant.worldclock.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Checks GitHub's releases API for a newer World Clock version. Ported from doc-scrubber's /
 * kiro-control-panel's same feature, adapted to this project's okhttp + org.json stack.
 */
public final class UpdateChecker {
    private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);
    private static final String RELEASES_URL = "https://api.github.com/repos/OurGiant/world-clock/releases/latest";
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(10))
        .build();

    public record ReleaseInfo(String version, String htmlUrl) {
    }

    private UpdateChecker() {
    }

    /**
     * Does a real network call — run this off the EDT (e.g. from a SwingWorker).
     * @return empty on a generic failure (offline, rate-limited, no releases yet, ...)
     * @throws NetworkFetchException on a TLS handshake failure specifically, with a user-facing message.
     */
    public static Optional<ReleaseInfo> fetchLatestRelease() {
        Request request = new Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "world-clock")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return Optional.empty();
            }
            JSONObject root = new JSONObject(response.body().string());
            String tagName = root.optString("tag_name", null);
            String htmlUrl = root.optString("html_url", null);
            if (tagName == null || htmlUrl == null) {
                return Optional.empty();
            }
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            return Optional.of(new ReleaseInfo(version, htmlUrl));
        } catch (SSLHandshakeException e) {
            logger.warn("TLS handshake failed fetching latest release from GitHub (possible TLS-inspecting proxy)", e);
            throw new NetworkFetchException("Couldn't verify the secure connection (possible corporate network proxy)", e);
        } catch (IOException | JSONException e) {
            logger.warn("Failed to fetch latest release from GitHub", e);
            return Optional.empty();
        }
    }

    /** @return true if {@code latest} is a strictly newer dotted-numeric version than {@code current}; false (not an exception) on any non-numeric segment. */
    public static boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");
            int len = Math.max(latestParts.length, currentParts.length);
            for (int i = 0; i < len; i++) {
                int l = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (l > c) {
                    return true;
                }
                if (l < c) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            logger.debug("Could not compare versions: {} vs {}", latest, current);
        }
        return false;
    }
}
