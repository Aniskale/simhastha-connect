package com.simhastha.payment.backend;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentOrder;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentStatus;
import com.simhastha.payment.PaymentVerificationRequest;
import com.simhastha.payment.RefundStatus;
import com.simhastha.payment.VerificationStatus;
import com.simhastha.payment.api.PaymentJson;

public final class PaymentBackendProcessor {

    private static final Logger LOGGER = Logger.getLogger(PaymentBackendProcessor.class.getName());

    private final RazorpayOrderCreator orderCreator;
    private final PaymentBackendConfig config;
    private final PaymentRepository repository;
    private final BookingPaymentConfirmationService confirmationService;
    private final BusinessInventoryHoldService inventoryHoldService;
    private final Set<String> processedWebhooks = ConcurrentHashMap.newKeySet();

    public PaymentBackendProcessor(RazorpayOrderCreator orderCreator, PaymentBackendConfig config,
            PaymentRepository repository, BookingPaymentConfirmationService confirmationService) {
        this(orderCreator, config, repository, confirmationService, new UnavailableBusinessInventoryHoldService());
    }

    public PaymentBackendProcessor(RazorpayOrderCreator orderCreator, PaymentBackendConfig config,
            PaymentRepository repository, BookingPaymentConfirmationService confirmationService,
            BusinessInventoryHoldService inventoryHoldService) {
        this.orderCreator = orderCreator;
        this.config = config;
        this.repository = repository;
        this.confirmationService = confirmationService;
        this.inventoryHoldService = inventoryHoldService;
    }

    public PaymentOrder createOrder(PaymentRequest request, String idempotencyKey) throws PaymentException {
        request.validate();
        String key = notBlank(idempotencyKey) ? idempotencyKey : request.idempotencyKey();
        Optional<PaymentRecord> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent() && unresolved(existing.get())) {
            LOGGER.info("Existing active payment order reused for " + key);
            return toOrder(existing.get());
        }

        inventoryHoldService.createHold(request);
        try {
            PaymentOrder order = orderCreator.createOrder(request);
            PaymentRecord record = PaymentRecord.created(request, order);
            repository.save(record);
            LOGGER.info("Payment order created: " + record.internalPaymentId());
            return order;
        } catch (PaymentException exception) {
            inventoryHoldService.releaseHold(request, "Razorpay order creation failed.");
            LOGGER.warning("Order creation failed and inventory hold was released for " + request.bookingId()
                    + ": " + exception.getMessage());
            throw exception;
        }
    }

    public PaymentResult verifyPayment(PaymentVerificationRequest request) throws PaymentException {
        request.validate();
        PaymentRecord record = findOwnedRecord(request.internalPaymentId(), request.bookingId(), request.userId());
        if (record.paymentStatus() == PaymentStatus.PAID || record.paymentStatus() == PaymentStatus.VERIFIED) {
            LOGGER.info("Duplicate verify request ignored for " + record.internalPaymentId());
            return successResult(record, "Payment already verified.");
        }
        if (!record.razorpayOrderId().equals(request.razorpayOrderId())) {
            return fail(record, request.razorpayPaymentId(), "Payment order mismatch.", "ORDER_MISMATCH");
        }
        if (record.amount() <= 0) {
            return fail(record, request.razorpayPaymentId(), "Payment amount is invalid.", "AMOUNT_INVALID");
        }

        boolean valid = PaymentSignatureVerifier.verifyCheckoutSignature(request.razorpayOrderId(),
                request.razorpayPaymentId(), request.razorpaySignature(), config.keySecret());
        if (!valid) {
            LOGGER.warning("Checkout signature verification failed for " + record.internalPaymentId());
            return fail(record, request.razorpayPaymentId(), "Payment verification failed.", "SIGNATURE_INVALID");
        }

        PaymentRecord paid = record.withStatus(PaymentStatus.PAID,
                VerificationStatus.VERIFIED, request.razorpayPaymentId(), "");
        inventoryHoldService.confirmHold(paid);
        paid = repository.save(paid);
        triggerConfirmation(paid);
        LOGGER.info("Payment verification succeeded for " + paid.internalPaymentId());
        return successResult(paid, "Payment verified successfully.");
    }

    public PaymentResult markCancelled(String internalPaymentId, String bookingId, String userId) throws PaymentException {
        PaymentRecord record = findOwnedRecord(internalPaymentId, bookingId, userId);
        if (record.paymentStatus() == PaymentStatus.PAID || record.paymentStatus() == PaymentStatus.VERIFIED) {
            return successResult(record, "Payment already verified.");
        }
        PaymentRecord cancelled = repository.save(record.withStatus(PaymentStatus.CANCELLED,
                VerificationStatus.NOT_STARTED, "", "Checkout cancelled by user."));
        inventoryHoldService.releaseHold(cancelled, "Checkout cancelled by user.");
        LOGGER.info("Payment checkout cancelled for " + cancelled.internalPaymentId());
        return new PaymentResult(cancelled.internalPaymentId(), cancelled.razorpayOrderId(),
                cancelled.razorpayPaymentId(), "", PaymentStatus.CANCELLED,
                "Payment checkout was cancelled.", "CHECKOUT_CANCELLED");
    }

    public PaymentRecord getPaymentStatus(String internalPaymentId) throws PaymentException {
        return repository.findByInternalPaymentId(internalPaymentId)
                .orElseThrow(() -> new PaymentException("Payment record was not found."));
    }

    public PaymentRecord getPaymentByBookingId(String bookingId, String userId) throws PaymentException {
        return repository.findByBookingId(bookingId, userId)
                .orElseThrow(() -> new PaymentException("Payment record was not found."));
    }

    public String processWebhook(String rawPayload, String signature) throws PaymentException {
        if (!config.hasWebhookSecret()) {
            throw new PaymentException("Razorpay webhook secret is not configured.");
        }
        boolean valid = PaymentSignatureVerifier.verifyWebhookSignature(rawPayload, signature, config.webhookSecret());
        if (!valid) {
            LOGGER.warning("Invalid Razorpay webhook signature rejected.");
            throw new PaymentException("Invalid webhook signature.");
        }

        String eventId = PaymentJson.value(rawPayload, "id");
        String event = PaymentJson.value(rawPayload, "event");
        String dedupeKey = notBlank(eventId) ? eventId : signature;
        if (!processedWebhooks.add(dedupeKey)) {
            LOGGER.info("Duplicate webhook ignored: " + dedupeKey);
            return PaymentJson.error("Duplicate webhook ignored.");
        }

        LOGGER.info("Verified Razorpay webhook received: " + event);
        switch (event) {
            case "payment.authorized" -> updateFromWebhook(rawPayload, PaymentStatus.AUTHORIZED, VerificationStatus.WEBHOOK_VERIFIED);
            case "payment.captured", "order.paid" -> updateFromWebhook(rawPayload, PaymentStatus.PAID, VerificationStatus.WEBHOOK_VERIFIED);
            case "payment.failed" -> updateFromWebhook(rawPayload, PaymentStatus.FAILED, VerificationStatus.FAILED);
            case "refund.processed" -> updateRefundFromWebhook(rawPayload);
            default -> LOGGER.info("Unsupported Razorpay webhook event ignored: " + event);
        }
        return "{\"status\":\"ok\"}";
    }

    private void updateFromWebhook(String rawPayload, PaymentStatus status, VerificationStatus verification)
            throws PaymentException {
        String orderId = firstNonBlank(PaymentJson.value(rawPayload, "order_id"), PaymentJson.value(rawPayload, "orderId"));
        String paymentId = firstNonBlank(PaymentJson.value(rawPayload, "entity_id"), PaymentJson.value(rawPayload, "payment_id"),
                PaymentJson.value(rawPayload, "id"));
        long amount = parseLong(PaymentJson.value(rawPayload, "amount"));

        Optional<PaymentRecord> maybeRecord = findByOrderId(orderId);
        if (maybeRecord.isEmpty()) {
            LOGGER.warning("Webhook received for unknown order: " + orderId);
            return;
        }
        PaymentRecord record = maybeRecord.get();
        if (amount > 0 && amount != record.amount()) {
            PaymentRecord failed = repository.save(record.withStatus(PaymentStatus.FAILED, VerificationStatus.FAILED,
                    paymentId, "Webhook amount mismatch."));
            LOGGER.warning("Webhook amount mismatch for " + failed.internalPaymentId());
            return;
        }
        if (record.paymentStatus() == PaymentStatus.PAID && status == PaymentStatus.PAID) {
            LOGGER.info("Duplicate paid webhook ignored for " + record.internalPaymentId());
            return;
        }

        PaymentRecord updated = record.withStatus(status, verification, paymentId,
                status == PaymentStatus.FAILED ? "Razorpay webhook reported payment failure." : "");
        if (status == PaymentStatus.PAID) {
            inventoryHoldService.confirmHold(updated);
            updated = repository.save(updated);
            triggerConfirmation(updated);
        } else if (status == PaymentStatus.FAILED) {
            inventoryHoldService.releaseHold(updated, "Razorpay webhook reported payment failure.");
            updated = repository.save(updated);
        } else {
            updated = repository.save(updated);
        }
        LOGGER.info("Payment status transitioned from webhook: " + updated.internalPaymentId() + " -> " + status);
    }

    private void updateRefundFromWebhook(String rawPayload) throws PaymentException {
        String paymentId = firstNonBlank(PaymentJson.value(rawPayload, "payment_id"), PaymentJson.value(rawPayload, "paymentId"));
        String refundId = firstNonBlank(PaymentJson.value(rawPayload, "entity_id"), PaymentJson.value(rawPayload, "id"));
        long amount = parseLong(PaymentJson.value(rawPayload, "amount"));
        Optional<PaymentRecord> maybeRecord = findByPaymentId(paymentId);
        if (maybeRecord.isEmpty()) {
            LOGGER.warning("Refund webhook received for unknown payment: " + paymentId);
            return;
        }
        PaymentRecord updated = repository.save(maybeRecord.get().withRefund(refundId, amount, RefundStatus.REFUNDED));
        LOGGER.info("Refund webhook processed for " + updated.internalPaymentId());
    }

    private PaymentRecord findOwnedRecord(String internalPaymentId, String bookingId, String userId) throws PaymentException {
        PaymentRecord record = getPaymentStatus(internalPaymentId);
        if (!record.bookingId().equals(bookingId) || !record.userId().equals(userId)) {
            LOGGER.warning("Payment ownership validation failed for " + internalPaymentId);
            throw new PaymentException("Payment ownership could not be verified.");
        }
        return record;
    }

    private PaymentResult fail(PaymentRecord record, String paymentId, String message, String errorCode)
            throws PaymentException {
        PaymentRecord failed = repository.save(record.withStatus(PaymentStatus.FAILED, VerificationStatus.FAILED,
                paymentId, message));
        inventoryHoldService.releaseHold(failed, message);
        return new PaymentResult(failed.internalPaymentId(), failed.razorpayOrderId(), failed.razorpayPaymentId(),
                "", PaymentStatus.FAILED, message, errorCode);
    }

    private PaymentResult successResult(PaymentRecord record, String message) {
        return new PaymentResult(record.internalPaymentId(), record.razorpayOrderId(), record.razorpayPaymentId(),
                "", record.paymentStatus(), message, "");
    }

    private void triggerConfirmation(PaymentRecord record) throws PaymentException {
        if (record.bookingConfirmationTriggered()) {
            LOGGER.info("Booking confirmation already triggered for " + record.internalPaymentId());
            return;
        }
        if (confirmationService.confirmPayment(record.bookingId(), record.moduleType(), record.internalPaymentId(),
                record.paymentStatus())) {
            repository.save(record.withBookingConfirmationTriggered());
        }
    }

    private Optional<PaymentRecord> findByOrderId(String orderId) throws PaymentException {
        if (!notBlank(orderId)) {
            return Optional.empty();
        }
        return repository.findByRazorpayOrderId(orderId);
    }

    private Optional<PaymentRecord> findByPaymentId(String paymentId) throws PaymentException {
        if (!notBlank(paymentId)) {
            return Optional.empty();
        }
        return repository.findByRazorpayPaymentId(paymentId);
    }

    private PaymentOrder toOrder(PaymentRecord record) {
        return new PaymentOrder(record.internalPaymentId(), record.razorpayOrderId(), config.keyId(), record.amount(),
                record.currency(), record.paymentStatus(), record.receipt(), record.createdAt());
    }

    private boolean unresolved(PaymentRecord record) {
        return record.paymentStatus() == PaymentStatus.CREATED
                || record.paymentStatus() == PaymentStatus.PENDING
                || record.paymentStatus() == PaymentStatus.AUTHORIZED;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
