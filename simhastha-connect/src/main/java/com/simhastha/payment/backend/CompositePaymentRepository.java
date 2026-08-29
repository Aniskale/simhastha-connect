package com.simhastha.payment.backend;

import java.util.Optional;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentRecord;

public final class CompositePaymentRepository implements PaymentRepository {

    private static final Logger LOGGER = Logger.getLogger(CompositePaymentRepository.class.getName());

    private final PaymentRepository primary;
    private final PaymentRepository secondary;

    public CompositePaymentRepository(PaymentRepository primary, PaymentRepository secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public PaymentRecord save(PaymentRecord record) throws PaymentException {
        PaymentRecord saved = primary.save(record);
        try {
            secondary.save(record);
        } catch (PaymentException exception) {
            LOGGER.log(Level.WARNING, "Payment Firestore persistence failed for " + record.internalPaymentId(), exception);
        }
        return saved;
    }

    @Override
    public Optional<PaymentRecord> findByInternalPaymentId(String internalPaymentId) throws PaymentException {
        Optional<PaymentRecord> primaryRecord = primary.findByInternalPaymentId(internalPaymentId);
        if (primaryRecord.isPresent()) {
            return primaryRecord;
        }
        return secondary.findByInternalPaymentId(internalPaymentId);
    }

    @Override
    public Optional<PaymentRecord> findByBookingId(String bookingId, String userId) throws PaymentException {
        Optional<PaymentRecord> primaryRecord = primary.findByBookingId(bookingId, userId);
        if (primaryRecord.isPresent()) {
            return primaryRecord;
        }
        return secondary.findByBookingId(bookingId, userId);
    }

    @Override
    public Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) throws PaymentException {
        Optional<PaymentRecord> primaryRecord = primary.findByIdempotencyKey(idempotencyKey);
        if (primaryRecord.isPresent()) {
            return primaryRecord;
        }
        return secondary.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Optional<PaymentRecord> findByRazorpayOrderId(String razorpayOrderId) throws PaymentException {
        Optional<PaymentRecord> primaryRecord = primary.findByRazorpayOrderId(razorpayOrderId);
        if (primaryRecord.isPresent()) {
            return primaryRecord;
        }
        return secondary.findByRazorpayOrderId(razorpayOrderId);
    }

    @Override
    public Optional<PaymentRecord> findByRazorpayPaymentId(String razorpayPaymentId) throws PaymentException {
        Optional<PaymentRecord> primaryRecord = primary.findByRazorpayPaymentId(razorpayPaymentId);
        if (primaryRecord.isPresent()) {
            return primaryRecord;
        }
        return secondary.findByRazorpayPaymentId(razorpayPaymentId);
    }

    Collection<PaymentRecord> snapshot() {
        if (primary instanceof InMemoryPaymentRepository memory) {
            return memory.snapshot();
        }
        return java.util.List.of();
    }
}
