package com.simhastha.payment;

import java.time.Instant;

public final class PaymentOrder {

    private final String internalPaymentId;
    private final String razorpayOrderId;
    private final String keyId;
    private final long amount;
    private final String currency;
    private final PaymentStatus status;
    private final String receipt;
    private final Instant createdAt;

    public PaymentOrder(String internalPaymentId, String razorpayOrderId, String keyId, long amount,
            String currency, PaymentStatus status, String receipt, Instant createdAt) {
        this.internalPaymentId = internalPaymentId == null ? "" : internalPaymentId;
        this.razorpayOrderId = razorpayOrderId == null ? "" : razorpayOrderId;
        this.keyId = keyId == null ? "" : keyId;
        this.amount = amount;
        this.currency = currency == null ? "INR" : currency;
        this.status = status == null ? PaymentStatus.CREATED : status;
        this.receipt = receipt == null ? "" : receipt;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String internalPaymentId() {
        return internalPaymentId;
    }

    public String razorpayOrderId() {
        return razorpayOrderId;
    }

    public String keyId() {
        return keyId;
    }

    public long amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public PaymentStatus status() {
        return status;
    }

    public String receipt() {
        return receipt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
