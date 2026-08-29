package com.simhastha.payment;

public final class PaymentVerificationRequest {

    private final String internalPaymentId;
    private final String bookingId;
    private final String userId;
    private final String razorpayOrderId;
    private final String razorpayPaymentId;
    private final String razorpaySignature;

    public PaymentVerificationRequest(String internalPaymentId, String bookingId, String userId,
            String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        this.internalPaymentId = clean(internalPaymentId);
        this.bookingId = clean(bookingId);
        this.userId = clean(userId);
        this.razorpayOrderId = clean(razorpayOrderId);
        this.razorpayPaymentId = clean(razorpayPaymentId);
        this.razorpaySignature = clean(razorpaySignature);
    }

    public void validate() throws PaymentException {
        require(internalPaymentId, "Payment reference is missing.");
        require(bookingId, "Booking reference is missing.");
        require(userId, "User reference is missing.");
        require(razorpayOrderId, "Razorpay order reference is missing.");
        require(razorpayPaymentId, "Razorpay payment reference is missing.");
        require(razorpaySignature, "Payment signature is missing.");
    }

    public String internalPaymentId() { return internalPaymentId; }
    public String bookingId() { return bookingId; }
    public String userId() { return userId; }
    public String razorpayOrderId() { return razorpayOrderId; }
    public String razorpayPaymentId() { return razorpayPaymentId; }
    public String razorpaySignature() { return razorpaySignature; }

    private static void require(String value, String message) throws PaymentException {
        if (value == null || value.isBlank()) {
            throw new PaymentException(message);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
