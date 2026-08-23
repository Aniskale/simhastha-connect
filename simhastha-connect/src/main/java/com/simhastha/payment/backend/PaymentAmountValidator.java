package com.simhastha.payment.backend;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentRequest;

public interface PaymentAmountValidator {
    void validateTrustedAmount(PaymentRequest request) throws PaymentException;
}
