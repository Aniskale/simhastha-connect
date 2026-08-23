package com.simhastha.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentModuleType;
import com.simhastha.payment.PaymentOrder;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.PaymentRequest;
import com.simhastha.payment.PaymentResult;
import com.simhastha.payment.PaymentStatus;
import com.simhastha.payment.PaymentVerificationRequest;
import com.simhastha.payment.RefundStatus;
import com.simhastha.payment.VerificationStatus;

public final class PaymentJson {

    private PaymentJson() {
    }

    public static String orderRequest(PaymentRequest request) {
        StringBuilder json = new StringBuilder();
        json.append('{')
                .append(field("userId", request.userId())).append(',')
                .append(field("bookingId", request.bookingId())).append(',')
                .append(field("moduleType", request.moduleType().name())).append(',')
                .append(field("itemId", request.itemId())).append(',')
                .append(field("businessId", request.businessId())).append(',')
                .append(field("title", request.title())).append(',')
                .append(field("description", request.description())).append(',')
                .append("\"amount\":\"").append(request.amount().toPlainString()).append("\",")
                .append(field("currency", request.currency())).append(',')
                .append(field("customerName", request.customerName())).append(',')
                .append(field("customerEmail", request.customerEmail())).append(',')
                .append(field("customerPhone", request.customerPhone())).append(',')
                .append("\"metadata\":").append(stringMap(request.metadata()))
                .append('}');
        return json.toString();
    }

    public static PaymentOrder parseOrder(String json) throws PaymentException {
        try {
            return new PaymentOrder(
                    value(json, "internalPaymentId"),
                    value(json, "razorpayOrderId"),
                    value(json, "keyId"),
                    longValue(json, "amount"),
                    valueOr(value(json, "currency"), "INR"),
                    status(value(json, "status")),
                    value(json, "receipt"),
                    instant(value(json, "createdAt")));
        } catch (RuntimeException exception) {
            throw new PaymentException("Payment order response was malformed.", exception);
        }
    }

    public static PaymentRequest parseRequest(String json) throws PaymentException {
        try {
            return PaymentRequest.builder()
                    .userId(value(json, "userId"))
                    .bookingId(value(json, "bookingId"))
                    .moduleType(PaymentModuleType.valueOf(value(json, "moduleType")))
                    .itemId(value(json, "itemId"))
                    .businessId(value(json, "businessId"))
                    .title(value(json, "title"))
                    .description(value(json, "description"))
                    .amount(new BigDecimal(value(json, "amount")))
                    .currency(valueOr(value(json, "currency"), "INR"))
                    .customerName(value(json, "customerName"))
                    .customerEmail(value(json, "customerEmail"))
                    .customerPhone(value(json, "customerPhone"))
                    .metadata(metadata(json))
                    .build();
        } catch (RuntimeException exception) {
            throw new PaymentException("Payment order request was malformed.", exception);
        }
    }

    public static String orderResponse(PaymentOrder order) {
        return "{"
                + field("internalPaymentId", order.internalPaymentId()) + ","
                + field("razorpayOrderId", order.razorpayOrderId()) + ","
                + field("keyId", order.keyId()) + ","
                + "\"amount\":" + order.amount() + ","
                + field("currency", order.currency()) + ","
                + field("status", order.status().name()) + ","
                + field("receipt", order.receipt()) + ","
                + field("createdAt", order.createdAt().toString())
                + "}";
    }

    public static String verificationRequest(PaymentVerificationRequest request) {
        return "{"
                + field("internalPaymentId", request.internalPaymentId()) + ","
                + field("bookingId", request.bookingId()) + ","
                + field("userId", request.userId()) + ","
                + field("razorpayOrderId", request.razorpayOrderId()) + ","
                + field("razorpayPaymentId", request.razorpayPaymentId()) + ","
                + field("razorpaySignature", request.razorpaySignature())
                + "}";
    }

    public static PaymentVerificationRequest parseVerificationRequest(String json) {
        return new PaymentVerificationRequest(
                value(json, "internalPaymentId"),
                value(json, "bookingId"),
                value(json, "userId"),
                value(json, "razorpayOrderId"),
                value(json, "razorpayPaymentId"),
                value(json, "razorpaySignature"));
    }

    public static String cancellationRequest(String internalPaymentId, String bookingId, String userId) {
        return "{"
                + field("internalPaymentId", internalPaymentId) + ","
                + field("bookingId", bookingId) + ","
                + field("userId", userId)
                + "}";
    }

    public static String paymentResult(PaymentResult result) {
        return "{"
                + field("internalPaymentId", result.internalPaymentId()) + ","
                + field("razorpayOrderId", result.razorpayOrderId()) + ","
                + field("razorpayPaymentId", result.razorpayPaymentId()) + ","
                + field("razorpaySignature", result.razorpaySignature()) + ","
                + field("paymentStatus", result.paymentStatus().name()) + ","
                + field("message", result.message()) + ","
                + field("errorCode", result.errorCode())
                + "}";
    }

    public static PaymentResult parsePaymentResult(String json) {
        return new PaymentResult(
                value(json, "internalPaymentId"),
                value(json, "razorpayOrderId"),
                value(json, "razorpayPaymentId"),
                value(json, "razorpaySignature"),
                status(value(json, "paymentStatus")),
                value(json, "message"),
                value(json, "errorCode"));
    }

    public static String paymentRecord(PaymentRecord record) {
        return "{"
                + field("internalPaymentId", record.internalPaymentId()) + ","
                + field("bookingId", record.bookingId()) + ","
                + field("userId", record.userId()) + ","
                + field("moduleType", record.moduleType() == null ? "" : record.moduleType().name()) + ","
                + field("itemId", record.itemId()) + ","
                + field("businessId", record.businessId()) + ","
                + field("title", record.title()) + ","
                + "\"amount\":" + record.amount() + ","
                + field("currency", record.currency()) + ","
                + field("razorpayOrderId", record.razorpayOrderId()) + ","
                + field("razorpayPaymentId", record.razorpayPaymentId()) + ","
                + field("paymentStatus", record.paymentStatus().name()) + ","
                + field("verificationStatus", record.verificationStatus().name()) + ","
                + field("receipt", record.receipt()) + ","
                + field("idempotencyKey", record.idempotencyKey()) + ","
                + field("createdAt", instantString(record.createdAt())) + ","
                + field("updatedAt", instantString(record.updatedAt())) + ","
                + field("paidAt", instantString(record.paidAt())) + ","
                + field("failureReason", record.failureReason()) + ","
                + field("refundStatus", record.refundStatus().name()) + ","
                + field("refundId", record.refundId()) + ","
                + "\"refundedAmount\":" + record.refundedAmount() + ","
                + field("refundRequestedAt", instantString(record.refundRequestedAt())) + ","
                + field("refundedAt", instantString(record.refundedAt())) + ","
                + field("source", record.source()) + ","
                + field("paymentProvider", record.paymentProvider()) + ","
                + "\"bookingConfirmationTriggered\":" + record.bookingConfirmationTriggered()
                + "}";
    }

    public static PaymentRecord parsePaymentRecord(String json) {
        String moduleType = value(json, "moduleType");
        return new PaymentRecord(
                value(json, "internalPaymentId"),
                value(json, "bookingId"),
                value(json, "userId"),
                moduleType.isBlank() ? null : PaymentModuleType.valueOf(moduleType),
                value(json, "itemId"),
                value(json, "businessId"),
                value(json, "title"),
                longValue(json, "amount"),
                valueOr(value(json, "currency"), "INR"),
                value(json, "razorpayOrderId"),
                value(json, "razorpayPaymentId"),
                status(value(json, "paymentStatus")),
                verificationStatus(value(json, "verificationStatus")),
                value(json, "receipt"),
                value(json, "idempotencyKey"),
                instant(value(json, "createdAt")),
                instant(value(json, "updatedAt")),
                optionalInstant(value(json, "paidAt")),
                value(json, "failureReason"),
                refundStatus(value(json, "refundStatus")),
                value(json, "refundId"),
                longValue(json, "refundedAmount"),
                optionalInstant(value(json, "refundRequestedAt")),
                optionalInstant(value(json, "refundedAt")),
                value(json, "source"),
                value(json, "paymentProvider"),
                boolValue(json, "bookingConfirmationTriggered"));
    }

    public static String error(String message) {
        return "{" + field("message", message) + "}";
    }

    public static String value(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*(?:\"((?:\\\\.|[^\"])*)\"|([^,}\\s]+))",
                Pattern.DOTALL).matcher(json == null ? "" : json);
        if (!matcher.find()) {
            return "";
        }
        String quoted = matcher.group(1);
        return unescape(quoted != null ? quoted : matcher.group(2));
    }

    public static String field(String name, String value) {
        return "\"" + escape(name) + "\":\"" + escape(value == null ? "" : value) + "\"";
    }

    public static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String stringMap(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append(field(entry.getKey(), entry.getValue()));
            first = false;
        }
        json.append('}');
        return json.toString();
    }

    private static Map<String, String> metadata(String json) {
        Map<String, String> metadata = new LinkedHashMap<>();
        Matcher objectMatcher = Pattern.compile("\"metadata\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL)
                .matcher(json == null ? "" : json);
        if (!objectMatcher.find()) {
            return metadata;
        }
        Matcher fieldMatcher = Pattern.compile("\"((?:\\\\.|[^\"])*)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
                .matcher(objectMatcher.group(1));
        while (fieldMatcher.find()) {
            metadata.put(unescape(fieldMatcher.group(1)), unescape(fieldMatcher.group(2)));
        }
        return metadata;
    }

    private static long longValue(String json, String name) {
        String value = value(json, name);
        return value.isBlank() ? 0 : Long.parseLong(value);
    }

    private static PaymentStatus status(String value) {
        return value == null || value.isBlank() ? PaymentStatus.CREATED : PaymentStatus.valueOf(value);
    }

    private static VerificationStatus verificationStatus(String value) {
        return value == null || value.isBlank() ? VerificationStatus.NOT_STARTED : VerificationStatus.valueOf(value);
    }

    private static RefundStatus refundStatus(String value) {
        return value == null || value.isBlank() ? RefundStatus.NONE : RefundStatus.valueOf(value);
    }

    private static Instant instant(String value) {
        return value == null || value.isBlank() ? Instant.now() : Instant.parse(value);
    }

    private static Instant optionalInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    private static String instantString(Instant instant) {
        return instant == null ? "" : instant.toString();
    }

    private static boolean boolValue(String json, String name) {
        return "true".equalsIgnoreCase(value(json, name));
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String unescape(String value) {
        return value == null ? "" : value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
