package com.simhastha.payment.backend;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentModuleType;
import com.simhastha.payment.PaymentStatus;

public interface BookingPaymentConfirmationService {
    boolean confirmPayment(String bookingId, PaymentModuleType moduleType, String internalPaymentId,
            PaymentStatus paymentStatus) throws PaymentException;
}
