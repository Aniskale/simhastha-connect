package com.simhastha.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Configuration only: a Google Maps key is never embedded in application source. */
public final class GoogleMapsConfig {
    private final String apiKey;

    private GoogleMapsConfig(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey.trim(); }

    public static GoogleMapsConfig load() {
        Properties properties = new Properties();
        try (InputStream stream = GoogleMapsConfig.class.getResourceAsStream("/google-maps.properties")) {
            if (stream != null) properties.load(stream);
        } catch (IOException ignored) {
            // Environment configuration remains sufficient.
        }
        return new GoogleMapsConfig(first(System.getProperty("google.maps.apiKey"), System.getenv("GOOGLE_MAPS_API_KEY"),
                properties.getProperty("google.maps.apiKey")));
    }

    /** Useful for dependency injection and non-network tests. */
    public static GoogleMapsConfig forApiKey(String apiKey) { return new GoogleMapsConfig(apiKey); }

    public boolean configured() { return !apiKey.isBlank(); }
    public String apiKey() { return apiKey; }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }
}
