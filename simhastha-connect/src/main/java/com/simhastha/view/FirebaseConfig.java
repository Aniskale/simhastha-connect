package com.simhastha.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class FirebaseConfig {

    private final boolean enabled;
    private final String projectId;
    private final String apiKey;

    private FirebaseConfig(boolean enabled, String projectId, String apiKey) {
        this.enabled = enabled;
        this.projectId = projectId;
        this.apiKey = apiKey;
    }

    public static FirebaseConfig load() {
        Properties properties = new Properties();
        try (InputStream input = FirebaseConfig.class.getResourceAsStream("/firebase.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // The app can still run with the local seed store.
        }

        String enabledValue = firstNonBlank(
                System.getProperty("firebase.enabled"),
                System.getenv("FIREBASE_ENABLED"),
                properties.getProperty("firebase.enabled"));
        String projectId = firstNonBlank(
                System.getProperty("firebase.projectId"),
                System.getenv("FIREBASE_PROJECT_ID"),
                properties.getProperty("firebase.projectId"));
        String apiKey = firstNonBlank(
                System.getProperty("firebase.apiKey"),
                System.getenv("FIREBASE_API_KEY"),
                properties.getProperty("firebase.apiKey"));

        boolean enabled = "true".equalsIgnoreCase(enabledValue)
                && projectId != null
                && apiKey != null;
        return new FirebaseConfig(enabled, projectId, apiKey);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String projectId() {
        return projectId;
    }

    public String apiKey() {
        return apiKey;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
