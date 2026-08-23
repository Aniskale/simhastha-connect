package com.simhastha.payment;

import java.util.concurrent.CompletableFuture;

import javafx.stage.Window;

public interface CheckoutHandler {
    CompletableFuture<PaymentResult> openCheckout(Window owner, PaymentRequest request, PaymentOrder order);
}
