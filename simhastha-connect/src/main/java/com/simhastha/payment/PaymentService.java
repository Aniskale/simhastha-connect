package com.simhastha.payment;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.simhastha.payment.api.PaymentBackendGateway;

import javafx.stage.Window;

public final class PaymentService {

    private final PaymentBackendGateway backendGateway;
    private final CheckoutHandler checkoutHandler;
    private final Executor paymentExecutor;
    private final Set<String> ordersInProgress = ConcurrentHashMap.newKeySet();

    public PaymentService(PaymentBackendGateway backendGateway, CheckoutHandler checkoutHandler) {
        this(backendGateway, checkoutHandler, Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "simhastha-payment-worker");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public PaymentService(PaymentBackendGateway backendGateway, CheckoutHandler checkoutHandler, Executor paymentExecutor) {
        this.backendGateway = Objects.requireNonNull(backendGateway, "backendGateway");
        this.checkoutHandler = Objects.requireNonNull(checkoutHandler, "checkoutHandler");
        this.paymentExecutor = Objects.requireNonNull(paymentExecutor, "paymentExecutor");
    }

    public CompletableFuture<PaymentResult> startPayment(Window owner, PaymentRequest request) {
        try {
            request.validate();
        } catch (PaymentException exception) {
            return CompletableFuture.completedFuture(PaymentResult.failed(exception.getMessage(), "INVALID_PAYMENT_REQUEST"));
        }

        String idempotencyKey = request.idempotencyKey();
        if (!ordersInProgress.add(idempotencyKey)) {
            return CompletableFuture.completedFuture(PaymentResult.failed(
                    "A payment is already being prepared for this booking.", "DUPLICATE_PAYMENT_ATTEMPT"));
        }

        return CompletableFuture.supplyAsync(() -> createOrder(request), paymentExecutor)
                .thenCompose(order -> checkoutHandler.openCheckout(owner, request, order)
                        .thenCompose(result -> finalizeCheckout(request, order, result)))
                .exceptionally(this::failureResult)
                .whenComplete((result, throwable) -> ordersInProgress.remove(idempotencyKey));
    }

    private PaymentOrder createOrder(PaymentRequest request) {
        try {
            return backendGateway.createOrder(request);
        } catch (PaymentException exception) {
            throw new PaymentRuntimeException(exception);
        }
    }

    private CompletableFuture<PaymentResult> finalizeCheckout(PaymentRequest request, PaymentOrder order,
            PaymentResult checkoutResult) {
        if (checkoutResult.paymentStatus() == PaymentStatus.AUTHORIZED) {
            PaymentVerificationRequest verificationRequest = new PaymentVerificationRequest(
                    order.internalPaymentId(),
                    request.bookingId(),
                    request.userId(),
                    checkoutResult.razorpayOrderId(),
                    checkoutResult.razorpayPaymentId(),
                    checkoutResult.razorpaySignature());
            return CompletableFuture.supplyAsync(() -> verifyPayment(verificationRequest), paymentExecutor);
        }

        if (checkoutResult.paymentStatus() == PaymentStatus.CANCELLED) {
            return CompletableFuture.supplyAsync(() -> markCancelled(order, request, checkoutResult), paymentExecutor);
        }

        return CompletableFuture.completedFuture(checkoutResult);
    }

    private PaymentResult verifyPayment(PaymentVerificationRequest request) {
        try {
            return backendGateway.verifyPayment(request);
        } catch (PaymentException exception) {
            return new PaymentResult(request.internalPaymentId(), request.razorpayOrderId(),
                    request.razorpayPaymentId(), "", PaymentStatus.PENDING,
                    "Payment is being verified. Please do not pay again.", "VERIFICATION_PENDING");
        }
    }

    private PaymentResult markCancelled(PaymentOrder order, PaymentRequest request, PaymentResult checkoutResult) {
        try {
            return backendGateway.markCancelled(order.internalPaymentId(), request.bookingId(), request.userId());
        } catch (PaymentException exception) {
            return new PaymentResult(order.internalPaymentId(), order.razorpayOrderId(), "", "",
                    checkoutResult.paymentStatus(), checkoutResult.message(), checkoutResult.errorCode());
        }
    }

    private PaymentResult failureResult(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        String message = cause instanceof PaymentException
                ? cause.getMessage()
                : "Payment could not be completed. Please try again.";
        return PaymentResult.failed(message, "PAYMENT_FLOW_FAILED");
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null && cause.getCause() != null
                && (cause instanceof java.util.concurrent.CompletionException
                        || cause instanceof PaymentRuntimeException)) {
            cause = cause.getCause();
        }
        return cause == null ? throwable : cause;
    }

    private static final class PaymentRuntimeException extends RuntimeException {
        private PaymentRuntimeException(Throwable cause) {
            super(cause);
        }
    }
}
