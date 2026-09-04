package com.simhastha.service;

import com.simhastha.payment.PaymentConfig;
import com.simhastha.payment.PaymentService;
import com.simhastha.payment.api.HttpPaymentBackendGateway;
import com.simhastha.payment.ui.RazorpayWebViewCheckoutHandler;

public final class PaymentServiceFactory {

    private static PaymentService paymentService;

    private PaymentServiceFactory() {
    }

    public static PaymentService get() {
        if (paymentService == null) {
            paymentService = new PaymentService(
                    new HttpPaymentBackendGateway(PaymentConfig.load()),
                    new RazorpayWebViewCheckoutHandler());
        }
        return paymentService;
    }
}
