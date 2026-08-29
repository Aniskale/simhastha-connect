package com.simhastha.payment;

import java.time.Instant;

public final class PaymentRecord {

    private final String internalPaymentId;
    private final String bookingId;
    private final String userId;
    private final PaymentModuleType moduleType;
    private final String itemId;
    private final String businessId;
    private final String title;
    private final long amount;
    private final String currency;
    private final String razorpayOrderId;
    private final String razorpayPaymentId;
    private final PaymentStatus paymentStatus;
    private final VerificationStatus verificationStatus;
    private final String receipt;
    private final String idempotencyKey;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant paidAt;
    private final String failureReason;
    private final RefundStatus refundStatus;
    private final String refundId;
    private final long refundedAmount;
    private final Instant refundRequestedAt;
    private final Instant refundedAt;
    private final String source;
    private final String paymentProvider;
    private final boolean bookingConfirmationTriggered;

    public PaymentRecord(String internalPaymentId, String bookingId, String userId, PaymentModuleType moduleType,
            String itemId, String businessId, String title, long amount, String currency, String razorpayOrderId,
            String razorpayPaymentId, PaymentStatus paymentStatus, VerificationStatus verificationStatus,
            String receipt, String idempotencyKey, Instant createdAt, Instant updatedAt, Instant paidAt,
            String failureReason, RefundStatus refundStatus, String refundId, long refundedAmount,
            Instant refundRequestedAt, Instant refundedAt, String source, String paymentProvider,
            boolean bookingConfirmationTriggered) {
        this.internalPaymentId = clean(internalPaymentId);
        this.bookingId = clean(bookingId);
        this.userId = clean(userId);
        this.moduleType = moduleType;
        this.itemId = clean(itemId);
        this.businessId = clean(businessId);
        this.title = clean(title);
        this.amount = amount;
        this.currency = clean(currency).isBlank() ? "INR" : clean(currency);
        this.razorpayOrderId = clean(razorpayOrderId);
        this.razorpayPaymentId = clean(razorpayPaymentId);
        this.paymentStatus = paymentStatus == null ? PaymentStatus.PENDING : paymentStatus;
        this.verificationStatus = verificationStatus == null ? VerificationStatus.NOT_STARTED : verificationStatus;
        this.receipt = clean(receipt);
        this.idempotencyKey = clean(idempotencyKey);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.paidAt = paidAt;
        this.failureReason = clean(failureReason);
        this.refundStatus = refundStatus == null ? RefundStatus.NONE : refundStatus;
        this.refundId = clean(refundId);
        this.refundedAmount = refundedAmount;
        this.refundRequestedAt = refundRequestedAt;
        this.refundedAt = refundedAt;
        this.source = clean(source).isBlank() ? "javafx" : clean(source);
        this.paymentProvider = clean(paymentProvider).isBlank() ? "razorpay" : clean(paymentProvider);
        this.bookingConfirmationTriggered = bookingConfirmationTriggered;
    }

    public static PaymentRecord created(PaymentRequest request, PaymentOrder order) {
        return new PaymentRecord(order.internalPaymentId(), request.bookingId(), request.userId(), request.moduleType(),
                request.itemId(), request.businessId(), request.title(), order.amount(), order.currency(),
                order.razorpayOrderId(), "", PaymentStatus.PENDING, VerificationStatus.PENDING, order.receipt(),
                request.idempotencyKey(), order.createdAt(), Instant.now(), null, "", RefundStatus.NONE, "", 0,
                null, null, "javafx", "razorpay", false);
    }

    public PaymentRecord withStatus(PaymentStatus status, VerificationStatus verification, String paymentId,
            String failureReason) {
        Instant now = Instant.now();
        return new PaymentRecord(internalPaymentId, bookingId, userId, moduleType, itemId, businessId, title, amount,
                currency, razorpayOrderId, clean(paymentId).isBlank() ? razorpayPaymentId : paymentId, status,
                verification, receipt, idempotencyKey, createdAt, now,
                status == PaymentStatus.PAID || status == PaymentStatus.VERIFIED ? now : paidAt,
                failureReason, refundStatus, refundId, refundedAmount, refundRequestedAt, refundedAt, source,
                paymentProvider, bookingConfirmationTriggered);
    }

    public PaymentRecord withRefund(String refundId, long refundedAmount, RefundStatus refundStatus) {
        Instant now = Instant.now();
        return new PaymentRecord(internalPaymentId, bookingId, userId, moduleType, itemId, businessId, title, amount,
                currency, razorpayOrderId, razorpayPaymentId, paymentStatus, verificationStatus, receipt,
                idempotencyKey, createdAt, now, paidAt, failureReason, refundStatus, refundId, refundedAmount,
                refundRequestedAt == null ? now : refundRequestedAt,
                refundStatus == RefundStatus.REFUNDED ? now : refundedAt, source, paymentProvider,
                bookingConfirmationTriggered);
    }

    public PaymentRecord withBookingConfirmationTriggered() {
        return new PaymentRecord(internalPaymentId, bookingId, userId, moduleType, itemId, businessId, title, amount,
                currency, razorpayOrderId, razorpayPaymentId, paymentStatus, verificationStatus, receipt,
                idempotencyKey, createdAt, Instant.now(), paidAt, failureReason, refundStatus, refundId,
                refundedAmount, refundRequestedAt, refundedAt, source, paymentProvider, true);
    }

    public String internalPaymentId() { return internalPaymentId; }
    public String bookingId() { return bookingId; }
    public String userId() { return userId; }
    public PaymentModuleType moduleType() { return moduleType; }
    public String itemId() { return itemId; }
    public String businessId() { return businessId; }
    public String title() { return title; }
    public long amount() { return amount; }
    public String currency() { return currency; }
    public String razorpayOrderId() { return razorpayOrderId; }
    public String razorpayPaymentId() { return razorpayPaymentId; }
    public PaymentStatus paymentStatus() { return paymentStatus; }
    public VerificationStatus verificationStatus() { return verificationStatus; }
    public String receipt() { return receipt; }
    public String idempotencyKey() { return idempotencyKey; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant paidAt() { return paidAt; }
    public String failureReason() { return failureReason; }
    public RefundStatus refundStatus() { return refundStatus; }
    public String refundId() { return refundId; }
    public long refundedAmount() { return refundedAmount; }
    public Instant refundRequestedAt() { return refundRequestedAt; }
    public Instant refundedAt() { return refundedAt; }
    public String source() { return source; }
    public String paymentProvider() { return paymentProvider; }
    public boolean bookingConfirmationTriggered() { return bookingConfirmationTriggered; }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
