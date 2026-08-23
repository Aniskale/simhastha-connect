package com.simhastha.payment.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.simhastha.payment.PaymentConfig;
import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentOrder;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentVerificationRequest;

public final class HttpPaymentBackendGateway implements PaymentBackendGateway {

    private final PaymentConfig config;
    private final HttpClient client;

    public HttpPaymentBackendGateway(PaymentConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public PaymentOrder createOrder(PaymentRequest request) throws PaymentException {
        if (!config.isEnabled()) {
            throw new PaymentException("Payments are not configured yet.");
        }

        URI uri = config.backendBaseUri().resolve("/api/payments/order");
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", request.idempotencyKey())
                .POST(HttpRequest.BodyPublishers.ofString(PaymentJson.orderRequest(request)))
                .build();

        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new PaymentException("Payment order could not be created. Please try again.");
            }
            PaymentOrder order = PaymentJson.parseOrder(response.body());
            if (order.razorpayOrderId().isBlank() || order.keyId().isBlank() || order.amount() <= 0) {
                throw new PaymentException("Payment order response was incomplete.");
            }
            return order;
        } catch (IOException exception) {
            throw new PaymentException("Payment service is currently unavailable.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Payment order creation was interrupted.", exception);
        }
    }

    @Override
    public PaymentResult verifyPayment(PaymentVerificationRequest request) throws PaymentException {
        request.validate();
        String response = post("/api/payments/verify", PaymentJson.verificationRequest(request), request.internalPaymentId());
        return PaymentJson.parsePaymentResult(response);
    }

    @Override
    public PaymentResult markCancelled(String internalPaymentId, String bookingId, String userId) throws PaymentException {
        String response = post("/api/payments/cancel",
                PaymentJson.cancellationRequest(internalPaymentId, bookingId, userId), internalPaymentId);
        return PaymentJson.parsePaymentResult(response);
    }

    @Override
    public PaymentRecord getPaymentStatus(String internalPaymentId) throws PaymentException {
        if (internalPaymentId == null || internalPaymentId.isBlank()) {
            throw new PaymentException("Payment reference is missing.");
        }
        String response = get("/api/payments/status?internalPaymentId=" + encode(internalPaymentId));
        return PaymentJson.parsePaymentRecord(response);
    }

    @Override
    public PaymentRecord getPaymentByBookingId(String bookingId, String userId) throws PaymentException {
        if (bookingId == null || bookingId.isBlank() || userId == null || userId.isBlank()) {
            throw new PaymentException("Booking and user references are required.");
        }
        String response = get("/api/payments/status?bookingId=" + encode(bookingId) + "&userId=" + encode(userId));
        return PaymentJson.parsePaymentRecord(response);
    }

    private String post(String path, String body, String idempotencyKey) throws PaymentException {
        if (!config.isEnabled()) {
            throw new PaymentException("Payments are not configured yet.");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(config.backendBaseUri().resolve(path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        return send(builder.build(), "Payment request could not be completed. Please try again.");
    }

    private String get(String path) throws PaymentException {
        if (!config.isEnabled()) {
            throw new PaymentException("Payments are not configured yet.");
        }
        HttpRequest request = HttpRequest.newBuilder(config.backendBaseUri().resolve(path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return send(request, "Payment status could not be loaded.");
    }

    private String send(HttpRequest request, String userMessage) throws PaymentException {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new PaymentException(userMessage);
            }
            return response.body();
        } catch (IOException exception) {
            throw new PaymentException("Payment service is currently unavailable.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Payment request was interrupted.", exception);
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
