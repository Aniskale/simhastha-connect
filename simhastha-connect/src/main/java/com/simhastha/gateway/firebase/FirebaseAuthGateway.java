package com.simhastha.gateway.firebase;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FirebaseAuthGateway {

    private static final String SIGN_UP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=%s";

    private static final String SIGN_IN_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=%s";

    private static final String PASSWORD_RESET_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=%s";

    private static final String SIGN_IN_WITH_IDP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=%s";

    private static final String ACCOUNT_UPDATE_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:update?key=%s";

    private final FirebaseConfig config;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public FirebaseAuthGateway(FirebaseConfig config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config != null && config.isEnabled();
    }

    public AuthResult register(String email, String password)
            throws IOException, InterruptedException {

        String json = """
                {
                  "email":"%s",
                  "password":"%s",
                  "returnSecureToken":true
                }
                """.formatted(escape(email), escape(password));

        return sendAuthRequest(
                String.format(SIGN_UP_URL, config.apiKey()),
                json);
    }

    public AuthResult login(String email, String password)
            throws IOException, InterruptedException {

        String json = """
                {
                  "email":"%s",
                  "password":"%s",
                  "returnSecureToken":true
                }
                """.formatted(escape(email), escape(password));

        return sendAuthRequest(
                String.format(SIGN_IN_URL, config.apiKey()),
                json);
    }

    public boolean sendPasswordReset(String email)
            throws IOException, InterruptedException {

        String json = """
                {
                  "requestType":"PASSWORD_RESET",
                  "email":"%s"
                }
                """.formatted(escape(email));

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(String.format(PASSWORD_RESET_URL, config.apiKey())))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    public AuthResult loginWithGoogleIdToken(String googleIdToken)
            throws IOException, InterruptedException {

        String json = """
                {
                  "postBody":"id_token=%s&providerId=google.com",
                  "requestUri":"http://localhost",
                  "returnIdpCredential":true,
                  "returnSecureToken":true
                }
                """.formatted(escape(googleIdToken));

        return sendAuthRequest(
                String.format(SIGN_IN_WITH_IDP_URL, config.apiKey()),
                json);
    }

    public AuthResult updatePassword(String idToken, String newPassword)
            throws IOException, InterruptedException {

        String json = """
                {
                  "idToken":"%s",
                  "password":"%s",
                  "returnSecureToken":true
                }
                """.formatted(escape(idToken), escape(newPassword));

        return sendAuthRequest(
                String.format(ACCOUNT_UPDATE_URL, config.apiKey()),
                json);
    }

    private AuthResult sendAuthRequest(String url, String json)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        String body = response.body();

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return new AuthResult(
                    true,
                    extract(body, "localId"),
                    extract(body, "email"),
                    extract(body, "idToken"),
                    ""
            );
        }

        return new AuthResult(
                false,
                "",
                "",
                "",
                extractFirebaseError(body)
        );
    }

    private String extractFirebaseError(String json) {
        String message = extract(json, "message");

        return switch (message) {
            case "EMAIL_EXISTS" ->
                    "This email is already registered.";

            case "EMAIL_NOT_FOUND" ->
                    "No account found with this email.";

            case "INVALID_PASSWORD",
                 "INVALID_LOGIN_CREDENTIALS" ->
                    "Invalid email or password.";

            case "WEAK_PASSWORD : Password should be at least 6 characters" ->
                    "Password must be at least 6 characters.";

            case "USER_DISABLED" ->
                    "This account has been disabled.";

            case "TOO_MANY_ATTEMPTS_TRY_LATER" ->
                    "Too many attempts. Please try again later.";

            default ->
                    message == null || message.isBlank()
                            ? "Firebase Authentication failed."
                            : message;
        };
    }

    private String extract(String json, String key) {
        if (json == null) {
            return "";
        }

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"");

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return unescape(matcher.group(1));
        }

        return "";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String unescape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    public static final class AuthResult {

        public final boolean success;
        public final String uid;
        public final String email;
        public final String idToken;
        public final String errorMessage;

        public AuthResult(
                boolean success,
                String uid,
                String email,
                String idToken,
                String errorMessage) {

            this.success = success;
            this.uid = uid;
            this.email = email;
            this.idToken = idToken;
            this.errorMessage = errorMessage;
        }
    }
}
