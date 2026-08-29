package com.simhastha.payment.backend;

import java.util.Optional;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentRecord;

public interface PaymentRepository {
    PaymentRecord save(PaymentRecord record) throws PaymentException;

    Optional<PaymentRecord> findByInternalPaymentId(String internalPaymentId) throws PaymentException;

    Optional<PaymentRecord> findByBookingId(String bookingId, String userId) throws PaymentException;

    Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) throws PaymentException;

    Optional<PaymentRecord> findByRazorpayOrderId(String razorpayOrderId) throws PaymentException;

    Optional<PaymentRecord> findByRazorpayPaymentId(String razorpayPaymentId) throws PaymentException;
}
