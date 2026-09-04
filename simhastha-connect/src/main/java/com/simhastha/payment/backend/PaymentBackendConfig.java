package com.simhastha.payment.backend;

public final class PaymentBackendConfig {

    private final boolean enabled;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String firestoreProjectId;
    private final String serviceAccountPath;

    private PaymentBackendConfig(boolean enabled, String keyId, String keySecret, String webhookSecret,
            String firestoreProjectId, String serviceAccountPath) {
        this.enabled = enabled;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.firestoreProjectId = firestoreProjectId;
        this.serviceAccountPath = serviceAccountPath;
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
        String serviceAccountPath = firstNonBlank(
                System.getProperty("payments.firebase.serviceAccountPath"),
                System.getenv("PAYMENTS_FIREBASE_SERVICE_ACCOUNT_PATH"),
                System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
        boolean enabled = "true".equalsIgnoreCase(firstNonBlank(
                System.getProperty("payments.backend.enabled"),
                System.getenv("PAYMENTS_BACKEND_ENABLED"),
                "false"));
        return new PaymentBackendConfig(enabled && keyId != null && keySecret != null, keyId, keySecret,
                webhookSecret, firestoreProjectId, serviceAccountPath);
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

    public String serviceAccountPath() {
        return serviceAccountPath;
    }

    /** @deprecated The legacy REST repository is not used by the backend server. */
    @Deprecated
    public String firestoreBearerToken() { return ""; }

    public boolean hasFirestoreConfig() {
        return serviceAccountPath != null && !serviceAccountPath.isBlank();
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
