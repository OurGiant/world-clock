package com.ourgiant.worldclock.gui;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AboutDialogTest {

    @Test
    void trustsHttpsGithubReleaseUrl() {
        assertTrue(AboutDialog.isTrustedReleaseUrl(URI.create("https://github.com/OurGiant/world-clock/releases/tag/v1.0.11")));
    }

    @Test
    void rejectsNonGithubHost() {
        assertFalse(AboutDialog.isTrustedReleaseUrl(URI.create("https://evil.example.com/releases/tag/v1.0.11")));
    }

    @Test
    void rejectsNonHttpsScheme() {
        assertFalse(AboutDialog.isTrustedReleaseUrl(URI.create("http://github.com/OurGiant/world-clock/releases/tag/v1.0.11")));
    }

    @Test
    void rejectsLookalikeHost() {
        assertFalse(AboutDialog.isTrustedReleaseUrl(URI.create("https://github.com.evil.example.com/x")));
    }
}
