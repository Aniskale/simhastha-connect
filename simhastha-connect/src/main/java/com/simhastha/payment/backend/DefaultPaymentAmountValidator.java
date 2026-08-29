package com.simhastha.payment.backend;

import com.simhastha.payment.MoneyUtil;
import com.simhastha.payment.PaymentCatalog;
import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentRequest;

public final class DefaultPaymentAmountValidator implements PaymentAmountValidator {

    @Override
    public void validateTrustedAmount(PaymentRequest request) throws PaymentException {
        request.validate();
        MoneyUtil.validateAmount(request.amount(), request.currency());
        if (request.amount().compareTo(PaymentCatalog.expectedAmount(request)) != 0) {
            throw new PaymentException("Payment amount does not match the approved booking price.");
        }
    }
}
