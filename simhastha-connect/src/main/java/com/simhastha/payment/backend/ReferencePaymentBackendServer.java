package com.simhastha.payment.backend;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentOrder;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentVerificationRequest;
import com.simhastha.payment.api.PaymentJson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class ReferencePaymentBackendServer {

    private static final Logger LOGGER = Logger.getLogger(ReferencePaymentBackendServer.class.getName());
    private final PaymentBackendProcessor processor;

    public ReferencePaymentBackendServer(PaymentBackendProcessor processor) {
        this.processor = processor;
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("payments.backend.port",
                System.getenv().getOrDefault("PAYMENTS_BACKEND_PORT", "8080")));
        PaymentBackendConfig config = PaymentBackendConfig.load();
        PaymentRepository memory = new InMemoryPaymentRepository();
        PaymentRepository repository = config.hasFirestoreConfig()
                ? new CompositePaymentRepository(memory, new FirestorePaymentRepository(config))
                : memory;
        PaymentBackendProcessor processor = new PaymentBackendProcessor(
                new RazorpayOrderCreator(config, new DefaultPaymentAmountValidator()),
                config,
                repository,
                new NoOpBookingPaymentConfirmationService());
        ReferencePaymentBackendServer backend = new ReferencePaymentBackendServer(processor);
        backend.start(port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/payments/order", this::handleCreateOrder);
        server.createContext("/api/payments/verify", this::handleVerifyPayment);
        server.createContext("/api/payments/cancel", this::handleCancelPayment);
        server.createContext("/api/payments/status", this::handlePaymentStatus);
        server.createContext("/api/payments/webhook", this::handleWebhook);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Payment backend listening on http://localhost:" + port);
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, PaymentJson.error("Method not allowed."));
            return;
        }

        try {
            String idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            PaymentRequest request = PaymentJson.parseRequest(body);
            PaymentOrder order = processor.createOrder(request, idempotencyKey);
            send(exchange, 200, PaymentJson.orderResponse(order));
        } catch (PaymentException exception) {
            LOGGER.warning("Payment order creation failed: " + exception.getMessage());
            send(exchange, 400, PaymentJson.error(exception.getMessage()));
        } catch (RuntimeException exception) {
            LOGGER.warning("Unexpected order creation failure.");
            send(exchange, 500, PaymentJson.error("Payment order could not be created."));
        }
    }

    private void handleVerifyPayment(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, PaymentJson.error("Method not allowed."));
            return;
        }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            PaymentVerificationRequest request = PaymentJson.parseVerificationRequest(body);
            PaymentResult result = processor.verifyPayment(request);
            send(exchange, 200, PaymentJson.paymentResult(result));
        } catch (PaymentException exception) {
            LOGGER.warning("Payment verification failed: " + exception.getMessage());
            send(exchange, 400, PaymentJson.error("Payment verification failed."));
        } catch (RuntimeException exception) {
            LOGGER.warning("Unexpected payment verification failure.");
            send(exchange, 500, PaymentJson.error("Payment verification could not be completed."));
        }
    }

    private void handleCancelPayment(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, PaymentJson.error("Method not allowed."));
            return;
        }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            PaymentResult result = processor.markCancelled(
                    PaymentJson.value(body, "internalPaymentId"),
                    PaymentJson.value(body, "bookingId"),
                    PaymentJson.value(body, "userId"));
            send(exchange, 200, PaymentJson.paymentResult(result));
        } catch (PaymentException exception) {
            LOGGER.warning("Payment cancellation update failed: " + exception.getMessage());
            send(exchange, 400, PaymentJson.error("Payment cancellation could not be recorded."));
        }
    }

    private void handlePaymentStatus(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, PaymentJson.error("Method not allowed."));
            return;
        }
        try {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            PaymentRecord record;
            if (query.containsKey("internalPaymentId")) {
                record = processor.getPaymentStatus(query.get("internalPaymentId"));
            } else {
                record = processor.getPaymentByBookingId(query.getOrDefault("bookingId", ""),
                        query.getOrDefault("userId", ""));
            }
            send(exchange, 200, PaymentJson.paymentRecord(record));
        } catch (PaymentException exception) {
            send(exchange, 404, PaymentJson.error("Payment record was not found."));
        }
    }

    private void handleWebhook(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, PaymentJson.error("Method not allowed."));
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String signature = exchange.getRequestHeaders().getFirst("X-Razorpay-Signature");
        try {
            String response = processor.processWebhook(body, signature);
            send(exchange, 200, response);
        } catch (PaymentException exception) {
            LOGGER.warning("Webhook rejected: " + exception.getMessage());
            send(exchange, 400, PaymentJson.error("Webhook rejected."));
        }
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        for (String part : rawQuery.split("&")) {
            int equals = part.indexOf('=');
            if (equals > 0) {
                query.put(decode(part.substring(0, equals)), decode(part.substring(equals + 1)));
            }
        }
        return query;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
