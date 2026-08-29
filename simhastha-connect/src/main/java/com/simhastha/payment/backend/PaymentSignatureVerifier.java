package com.simhastha.payment.backend;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.simhastha.payment.PaymentException;

public final class PaymentSignatureVerifier {

    private PaymentSignatureVerifier() {
    }

    public static boolean verifyCheckoutSignature(String orderId, String paymentId, String signature, String keySecret)
            throws PaymentException {
        String payload = orderId + "|" + paymentId;
        return verifyHmacSha256(payload, signature, keySecret);
    }

    public static boolean verifyWebhookSignature(String rawPayload, String signature, String webhookSecret)
            throws PaymentException {
        return verifyHmacSha256(rawPayload, signature, webhookSecret);
    }

    private static boolean verifyHmacSha256(String payload, String signature, String secret) throws PaymentException {
        if (payload == null || signature == null || signature.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new PaymentException("Payment signature verification failed.", exception);
        }
    }
}
