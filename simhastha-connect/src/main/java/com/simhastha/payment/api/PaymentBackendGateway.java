package com.simhastha.payment.api;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentOrder;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentVerificationRequest;

public interface PaymentBackendGateway {
    PaymentOrder createOrder(PaymentRequest request) throws PaymentException;

    PaymentResult verifyPayment(PaymentVerificationRequest request) throws PaymentException;

    PaymentResult markCancelled(String internalPaymentId, String bookingId, String userId) throws PaymentException;

    PaymentRecord getPaymentStatus(String internalPaymentId) throws PaymentException;

    PaymentRecord getPaymentByBookingId(String bookingId, String userId) throws PaymentException;
}
