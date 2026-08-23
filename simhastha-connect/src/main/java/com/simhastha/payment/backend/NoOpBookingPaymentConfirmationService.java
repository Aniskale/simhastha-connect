package com.simhastha.payment.backend;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.simhastha.payment.PaymentModuleType;
import com.simhastha.payment.PaymentStatus;

public final class NoOpBookingPaymentConfirmationService implements BookingPaymentConfirmationService {

    private static final Logger LOGGER = Logger.getLogger(NoOpBookingPaymentConfirmationService.class.getName());
    private final Set<String> confirmedKeys = ConcurrentHashMap.newKeySet();

    @Override
    public boolean confirmPayment(String bookingId, PaymentModuleType moduleType, String internalPaymentId,
            PaymentStatus paymentStatus) {
        if (paymentStatus != PaymentStatus.PAID && paymentStatus != PaymentStatus.VERIFIED) {
            return false;
        }
        String key = moduleType + ":" + bookingId + ":" + internalPaymentId;
        if (!confirmedKeys.add(key)) {
            LOGGER.info("Duplicate booking confirmation ignored for payment " + internalPaymentId);
            return false;
        }
        LOGGER.info("Booking confirmation hook triggered for payment " + internalPaymentId);
        return true;
    }
}
