package com.simhastha.payment.backend;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.api.PaymentJson;

public final class FirestorePaymentRepository implements PaymentRepository {

    private static final String ROOT = "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents";

    private final PaymentBackendConfig config;
    private final HttpClient client;

    public FirestorePaymentRepository(PaymentBackendConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public PaymentRecord save(PaymentRecord record) throws PaymentException {
        if (!config.hasFirestoreConfig()) {
            return record;
        }
        String json = toFirestoreJson(record);
        HttpRequest request = authorizedBuilder(documentUri(record.internalPaymentId()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();
        send(request);
        return record;
    }

    @Override
    public Optional<PaymentRecord> findByInternalPaymentId(String internalPaymentId) throws PaymentException {
        if (!config.hasFirestoreConfig()) {
            return Optional.empty();
        }
        HttpRequest request = authorizedBuilder(documentUri(internalPaymentId))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        return Optional.of(fromFirestoreJson(response.body()));
    }

    @Override
    public Optional<PaymentRecord> findByBookingId(String bookingId, String userId) throws PaymentException {
        if (!config.hasFirestoreConfig()) {
            return Optional.empty();
        }
        return queryFirst("""
                {
                  "structuredQuery": {
                    "from": [{"collectionId": "payments"}],
                    "where": {
                      "compositeFilter": {
                        "op": "AND",
                        "filters": [
                          {"fieldFilter": {"field": {"fieldPath": "bookingId"}, "op": "EQUAL", "value": {"stringValue": "%s"}}},
                          {"fieldFilter": {"field": {"fieldPath": "userId"}, "op": "EQUAL", "value": {"stringValue": "%s"}}}
                        ]
                      }
                    },
                    "limit": 1
                  }
                }
                """.formatted(PaymentJson.escape(bookingId), PaymentJson.escape(userId)));
    }

    @Override
    public Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) throws PaymentException {
        if (!config.hasFirestoreConfig()) {
            return Optional.empty();
        }
        return queryFirst("""
                {
                  "structuredQuery": {
                    "from": [{"collectionId": "payments"}],
                    "where": {
                      "fieldFilter": {
                        "field": {"fieldPath": "idempotencyKey"},
                        "op": "EQUAL",
                        "value": {"stringValue": "%s"}
                      }
                    },
                    "limit": 1
                  }
                }
                """.formatted(PaymentJson.escape(idempotencyKey)));
    }

    @Override
    public Optional<PaymentRecord> findByRazorpayOrderId(String razorpayOrderId) throws PaymentException {
        return queryByStringField("razorpayOrderId", razorpayOrderId);
    }

    @Override
    public Optional<PaymentRecord> findByRazorpayPaymentId(String razorpayPaymentId) throws PaymentException {
        return queryByStringField("razorpayPaymentId", razorpayPaymentId);
    }

    private HttpResponse<String> send(HttpRequest request) throws PaymentException {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && response.statusCode() != 404) {
                throw new PaymentException("Firestore payment persistence failed.");
            }
            return response;
        } catch (IOException exception) {
            throw new PaymentException("Firestore payment persistence is unavailable.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Firestore payment persistence was interrupted.", exception);
        }
    }

    private HttpRequest.Builder authorizedBuilder(URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + config.firestoreBearerToken());
    }

    private URI documentUri(String documentId) {
        return URI.create(String.format(ROOT, enc(config.firestoreProjectId())) + "/payments/" + enc(documentId));
    }

    private String toFirestoreJson(PaymentRecord record) {
        return "{\"fields\":{"
                + field("internalPaymentId", record.internalPaymentId()) + ","
                + field("bookingId", record.bookingId()) + ","
                + field("userId", record.userId()) + ","
                + field("moduleType", record.moduleType() == null ? "" : record.moduleType().name()) + ","
                + field("itemId", record.itemId()) + ","
                + field("businessId", record.businessId()) + ","
                + field("title", record.title()) + ","
                + intField("amount", record.amount()) + ","
                + field("currency", record.currency()) + ","
                + field("razorpayOrderId", record.razorpayOrderId()) + ","
                + field("razorpayPaymentId", record.razorpayPaymentId()) + ","
                + field("paymentStatus", record.paymentStatus().name()) + ","
                + field("verificationStatus", record.verificationStatus().name()) + ","
                + field("receipt", record.receipt()) + ","
                + field("idempotencyKey", record.idempotencyKey()) + ","
                + field("createdAt", record.createdAt().toString()) + ","
                + field("updatedAt", record.updatedAt().toString()) + ","
                + field("paidAt", record.paidAt() == null ? "" : record.paidAt().toString()) + ","
                + field("failureReason", record.failureReason()) + ","
                + field("refundStatus", record.refundStatus().name()) + ","
                + field("refundId", record.refundId()) + ","
                + intField("refundedAmount", record.refundedAmount()) + ","
                + field("refundRequestedAt", record.refundRequestedAt() == null ? "" : record.refundRequestedAt().toString()) + ","
                + field("refundedAt", record.refundedAt() == null ? "" : record.refundedAt().toString()) + ","
                + field("source", record.source()) + ","
                + field("paymentProvider", record.paymentProvider()) + ","
                + boolField("bookingConfirmationTriggered", record.bookingConfirmationTriggered())
                + "}}";
    }

    private PaymentRecord fromFirestoreJson(String json) {
        String fields = extractFields(json);
        return PaymentJson.parsePaymentRecord("{"
                + PaymentJson.field("internalPaymentId", stringValue(fields, "internalPaymentId")) + ","
                + PaymentJson.field("bookingId", stringValue(fields, "bookingId")) + ","
                + PaymentJson.field("userId", stringValue(fields, "userId")) + ","
                + PaymentJson.field("moduleType", stringValue(fields, "moduleType")) + ","
                + PaymentJson.field("itemId", stringValue(fields, "itemId")) + ","
                + PaymentJson.field("businessId", stringValue(fields, "businessId")) + ","
                + PaymentJson.field("title", stringValue(fields, "title")) + ","
                + "\"amount\":" + integerValue(fields, "amount") + ","
                + PaymentJson.field("currency", stringValue(fields, "currency")) + ","
                + PaymentJson.field("razorpayOrderId", stringValue(fields, "razorpayOrderId")) + ","
                + PaymentJson.field("razorpayPaymentId", stringValue(fields, "razorpayPaymentId")) + ","
                + PaymentJson.field("paymentStatus", stringValue(fields, "paymentStatus")) + ","
                + PaymentJson.field("verificationStatus", stringValue(fields, "verificationStatus")) + ","
                + PaymentJson.field("receipt", stringValue(fields, "receipt")) + ","
                + PaymentJson.field("idempotencyKey", stringValue(fields, "idempotencyKey")) + ","
                + PaymentJson.field("createdAt", stringValue(fields, "createdAt")) + ","
                + PaymentJson.field("updatedAt", stringValue(fields, "updatedAt")) + ","
                + PaymentJson.field("paidAt", stringValue(fields, "paidAt")) + ","
                + PaymentJson.field("failureReason", stringValue(fields, "failureReason")) + ","
                + PaymentJson.field("refundStatus", stringValue(fields, "refundStatus")) + ","
                + PaymentJson.field("refundId", stringValue(fields, "refundId")) + ","
                + "\"refundedAmount\":" + integerValue(fields, "refundedAmount") + ","
                + PaymentJson.field("refundRequestedAt", stringValue(fields, "refundRequestedAt")) + ","
                + PaymentJson.field("refundedAt", stringValue(fields, "refundedAt")) + ","
                + PaymentJson.field("source", stringValue(fields, "source")) + ","
                + PaymentJson.field("paymentProvider", stringValue(fields, "paymentProvider")) + ","
                + "\"bookingConfirmationTriggered\":" + booleanValue(fields, "bookingConfirmationTriggered")
                + "}");
    }

    private Optional<PaymentRecord> queryFirst(String structuredQuery) throws PaymentException {
        HttpRequest request = authorizedBuilder(runQueryUri())
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(structuredQuery))
                .build();
        HttpResponse<String> response = send(request);
        String document = firstDocumentJson(response.body());
        if (document.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(fromFirestoreJson(document));
    }

    private Optional<PaymentRecord> queryByStringField(String field, String value) throws PaymentException {
        if (!config.hasFirestoreConfig() || value == null || value.isBlank()) {
            return Optional.empty();
        }
        return queryFirst("""
                {
                  "structuredQuery": {
                    "from": [{"collectionId": "payments"}],
                    "where": {
                      "fieldFilter": {
                        "field": {"fieldPath": "%s"},
                        "op": "EQUAL",
                        "value": {"stringValue": "%s"}
                      }
                    },
                    "limit": 1
                  }
                }
                """.formatted(PaymentJson.escape(field), PaymentJson.escape(value)));
    }

    private URI runQueryUri() {
        return URI.create(String.format(ROOT, enc(config.firestoreProjectId())) + ":runQuery");
    }

    private String firstDocumentJson(String json) {
        Matcher matcher = Pattern.compile("\"document\"\\s*:\\s*(\\{.*?\"fields\"\\s*:\\s*\\{.*?\\}\\s*(?:,\\s*\"createTime\"|,\\s*\"updateTime\"|\\}))",
                Pattern.DOTALL).matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String field(String name, String value) {
        return "\"" + PaymentJson.escape(name) + "\":{\"stringValue\":\"" + PaymentJson.escape(value) + "\"}";
    }

    private String intField(String name, long value) {
        return "\"" + PaymentJson.escape(name) + "\":{\"integerValue\":\"" + value + "\"}";
    }

    private String boolField(String name, boolean value) {
        return "\"" + PaymentJson.escape(name) + "\":{\"booleanValue\":" + value + "}";
    }

    private String extractFields(String json) {
        Matcher matcher = Pattern.compile("\"fields\"\\s*:\\s*\\{(.*)\\}\\s*(?:,\\s*\"createTime\"|,\\s*\"updateTime\"|\\})",
                Pattern.DOTALL).matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String stringValue(String fields, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\\{\\s*\"stringValue\"\\s*:\\s*\"(.*?)\"\\s*\\}",
                Pattern.DOTALL).matcher(fields == null ? "" : fields);
        return matcher.find() ? matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : "";
    }

    private long integerValue(String fields, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\\{\\s*\"integerValue\"\\s*:\\s*\"?(\\d+)\"?\\s*\\}",
                Pattern.DOTALL).matcher(fields == null ? "" : fields);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0;
    }

    private boolean booleanValue(String fields, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\\{\\s*\"booleanValue\"\\s*:\\s*(true|false)\\s*\\}",
                Pattern.DOTALL).matcher(fields == null ? "" : fields);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
