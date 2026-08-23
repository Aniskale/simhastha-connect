package com.simhastha.payment.backend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.simhastha.payment.MoneyUtil;
import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentOrder;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentStatus;
import com.simhastha.payment.api.PaymentJson;

public final class RazorpayOrderCreator {

    private static final URI RAZORPAY_ORDERS_URI = URI.create("https://api.razorpay.com/v1/orders");

    private final PaymentBackendConfig config;
    private final PaymentAmountValidator amountValidator;
    private final HttpClient client;

    public RazorpayOrderCreator(PaymentBackendConfig config, PaymentAmountValidator amountValidator) {
        this.config = config;
        this.amountValidator = amountValidator;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public PaymentOrder createOrder(PaymentRequest request) throws PaymentException {
        if (!config.isEnabled()) {
            throw new PaymentException("Razorpay backend is not configured.");
        }
        amountValidator.validateTrustedAmount(request);

        long amountInPaise = MoneyUtil.toSmallestUnit(request.amount(), request.currency());
        String internalPaymentId = "pay_" + UUID.randomUUID().toString().replace("-", "");
        String receipt = safeReceipt(request.moduleType().name() + "_" + request.bookingId());
        String payload = razorpayOrderPayload(request, amountInPaise, receipt, internalPaymentId);

        HttpRequest httpRequest = HttpRequest.newBuilder(RAZORPAY_ORDERS_URI)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + basicAuth())
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new PaymentException("Razorpay order creation failed.");
            }
            String razorpayOrderId = PaymentJson.value(response.body(), "id");
            if (razorpayOrderId.isBlank()) {
                throw new PaymentException("Razorpay order response was incomplete.");
            }
            return new PaymentOrder(internalPaymentId, razorpayOrderId, config.keyId(), amountInPaise,
                    request.currency(), PaymentStatus.CREATED, receipt, Instant.now());
        } catch (IOException exception) {
            throw new PaymentException("Razorpay is currently unavailable.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Razorpay order creation was interrupted.", exception);
        }
    }

    private String razorpayOrderPayload(PaymentRequest request, long amountInPaise, String receipt,
            String internalPaymentId) {
        return "{"
                + "\"amount\":" + amountInPaise + ","
                + PaymentJson.field("currency", request.currency()) + ","
                + PaymentJson.field("receipt", receipt) + ","
                + "\"payment_capture\":1,"
                + "\"notes\":{"
                + PaymentJson.field("bookingId", request.bookingId()) + ","
                + PaymentJson.field("moduleType", request.moduleType().name()) + ","
                + PaymentJson.field("internalPaymentId", internalPaymentId)
                + "}"
                + "}";
    }

    private String basicAuth() {
        String credentials = config.keyId() + ":" + config.keySecret();
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String safeReceipt(String value) {
        String safe = value.replaceAll("[^A-Za-z0-9_-]", "_");
        return safe.length() > 40 ? safe.substring(0, 40) : safe;
    }
}
