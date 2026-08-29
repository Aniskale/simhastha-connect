package com.simhastha.payment.backend;

public final class PaymentBackendConfig {

    private final boolean enabled;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String firestoreProjectId;
    private final String firestoreBearerToken;

    private PaymentBackendConfig(boolean enabled, String keyId, String keySecret, String webhookSecret,
            String firestoreProjectId, String firestoreBearerToken) {
        this.enabled = enabled;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.firestoreProjectId = firestoreProjectId;
        this.firestoreBearerToken = firestoreBearerToken;
    }

    public static PaymentBackendConfig load() {
        String keyId = firstNonBlank(System.getProperty("razorpay.keyId"), System.getenv("RAZORPAY_KEY_ID"));
        String keySecret = firstNonBlank(System.getProperty("razorpay.keySecret"), System.getenv("RAZORPAY_KEY_SECRET"));
        String webhookSecret = firstNonBlank(
                System.getProperty("razorpay.webhookSecret"),
                System.getenv("RAZORPAY_WEBHOOK_SECRET"));
        String firestoreProjectId = firstNonBlank(
                System.getProperty("payments.firestore.projectId"),
                System.getenv("PAYMENTS_FIRESTORE_PROJECT_ID"),
                System.getenv("FIREBASE_PROJECT_ID"));
        String firestoreBearerToken = firstNonBlank(
                System.getProperty("payments.firestore.bearerToken"),
                System.getenv("PAYMENTS_FIRESTORE_BEARER_TOKEN"));
        boolean enabled = "true".equalsIgnoreCase(firstNonBlank(
                System.getProperty("payments.backend.enabled"),
                System.getenv("PAYMENTS_BACKEND_ENABLED"),
                "false"));
        return new PaymentBackendConfig(enabled && keyId != null && keySecret != null, keyId, keySecret,
                webhookSecret, firestoreProjectId, firestoreBearerToken);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String keyId() {
        return keyId;
    }

    public String keySecret() {
        return keySecret;
    }

    public String webhookSecret() {
        return webhookSecret;
    }

    public boolean hasWebhookSecret() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public String firestoreProjectId() {
        return firestoreProjectId;
    }

    public String firestoreBearerToken() {
        return firestoreBearerToken;
    }

    public boolean hasFirestoreConfig() {
        return firestoreProjectId != null && !firestoreProjectId.isBlank()
                && firestoreBearerToken != null && !firestoreBearerToken.isBlank();
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
