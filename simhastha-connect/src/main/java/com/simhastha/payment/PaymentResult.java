package com.simhastha.payment;

public final class PaymentResult {

    private final String internalPaymentId;
    private final String razorpayOrderId;
    private final String razorpayPaymentId;
    private final String razorpaySignature;
    private final PaymentStatus paymentStatus;
    private final String message;
    private final String errorCode;

    public PaymentResult(String internalPaymentId, String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature, PaymentStatus paymentStatus, String message, String errorCode) {
        this.internalPaymentId = internalPaymentId == null ? "" : internalPaymentId;
        this.razorpayOrderId = razorpayOrderId == null ? "" : razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId == null ? "" : razorpayPaymentId;
        this.razorpaySignature = razorpaySignature == null ? "" : razorpaySignature;
        this.paymentStatus = paymentStatus == null ? PaymentStatus.PENDING : paymentStatus;
        this.message = message == null ? "" : message;
        this.errorCode = errorCode == null ? "" : errorCode;
    }

    public static PaymentResult failed(String message, String errorCode) {
        return new PaymentResult("", "", "", "", PaymentStatus.FAILED, message, errorCode);
    }

    public static PaymentResult cancelled(String message) {
        return new PaymentResult("", "", "", "", PaymentStatus.CANCELLED, message, "CHECKOUT_CANCELLED");
    }

    public String internalPaymentId() {
        return internalPaymentId;
    }

    public String razorpayOrderId() {
        return razorpayOrderId;
    }

    public String razorpayPaymentId() {
        return razorpayPaymentId;
    }

    public String razorpaySignature() {
        return razorpaySignature;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public String message() {
        return message;
    }

    public String errorCode() {
        return errorCode;
    }
}
