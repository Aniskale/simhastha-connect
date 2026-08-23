package com.simhastha.payment.backend;

import java.util.Optional;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.simhastha.payment.PaymentRecord;

public final class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentMap<String, PaymentRecord> byInternalId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> byBookingAndUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> byIdempotency = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> byRazorpayOrderId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> byRazorpayPaymentId = new ConcurrentHashMap<>();

    @Override
    public PaymentRecord save(PaymentRecord record) {
        byInternalId.put(record.internalPaymentId(), record);
        if (!record.bookingId().isBlank() && !record.userId().isBlank()) {
            byBookingAndUser.put(bookingKey(record.bookingId(), record.userId()), record.internalPaymentId());
        }
        if (!record.idempotencyKey().isBlank()) {
            byIdempotency.put(record.idempotencyKey(), record.internalPaymentId());
        }
        if (!record.razorpayOrderId().isBlank()) {
            byRazorpayOrderId.put(record.razorpayOrderId(), record.internalPaymentId());
        }
        if (!record.razorpayPaymentId().isBlank()) {
            byRazorpayPaymentId.put(record.razorpayPaymentId(), record.internalPaymentId());
        }
        return record;
    }

    @Override
    public Optional<PaymentRecord> findByInternalPaymentId(String internalPaymentId) {
        return Optional.ofNullable(byInternalId.get(internalPaymentId));
    }

    @Override
    public Optional<PaymentRecord> findByBookingId(String bookingId, String userId) {
        return Optional.ofNullable(byBookingAndUser.get(bookingKey(bookingId, userId)))
                .map(byInternalId::get);
    }

    @Override
    public Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(byIdempotency.get(idempotencyKey))
                .map(byInternalId::get);
    }

    @Override
    public Optional<PaymentRecord> findByRazorpayOrderId(String razorpayOrderId) {
        return Optional.ofNullable(byRazorpayOrderId.get(razorpayOrderId))
                .map(byInternalId::get);
    }

    @Override
    public Optional<PaymentRecord> findByRazorpayPaymentId(String razorpayPaymentId) {
        return Optional.ofNullable(byRazorpayPaymentId.get(razorpayPaymentId))
                .map(byInternalId::get);
    }

    private String bookingKey(String bookingId, String userId) {
        return userId + ":" + bookingId;
    }

    Collection<PaymentRecord> snapshot() {
        return java.util.List.copyOf(byInternalId.values());
    }
}
