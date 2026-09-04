package com.simhastha.payment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public final class PaymentConfig {

    private final boolean enabled;
    private final URI backendBaseUri;

    private PaymentConfig(boolean enabled, URI backendBaseUri) {
        this.enabled = enabled;
        this.backendBaseUri = backendBaseUri;
    }

    public static PaymentConfig load() {
        Properties properties = new Properties();
        try (InputStream input = PaymentConfig.class.getResourceAsStream("/payment.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Payment configuration is optional for the JavaFX app.
        }

        String enabledValue = firstNonBlank(
                System.getProperty("payments.enabled"),
                System.getenv("PAYMENTS_ENABLED"),
                properties.getProperty("payments.enabled"));
        String backendUrl = firstNonBlank(
                System.getProperty("payments.backendBaseUrl"),
                System.getenv("PAYMENTS_BACKEND_BASE_URL"),
                properties.getProperty("payments.backendBaseUrl"));

        URI backendBaseUri = null;
        if (backendUrl != null) {
            backendBaseUri = URI.create(trimTrailingSlash(backendUrl));
        }

        boolean enabled = "true".equalsIgnoreCase(enabledValue) && backendBaseUri != null;
        return new PaymentConfig(enabled, backendBaseUri);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Safe diagnostic only; never includes backend credentials or Razorpay secrets. */
    public String configurationIssue() {
        if (enabled) {
            return "";
        }
        if (backendBaseUri == null) {
            return "PAYMENTS_BACKEND_BASE_URL / payments.backendBaseUrl is missing.";
        }
        return "PAYMENTS_ENABLED / payments.enabled must be set to true.";
    }

    public URI backendBaseUri() {
        return backendBaseUri;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
