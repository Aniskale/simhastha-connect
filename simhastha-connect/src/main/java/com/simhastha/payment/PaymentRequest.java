package com.simhastha.payment;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PaymentRequest {

    private final String userId;
    private final String bookingId;
    private final PaymentModuleType moduleType;
    private final String itemId;
    private final String businessId;
    private final String title;
    private final String description;
    private final BigDecimal amount;
    private final String currency;
    private final String customerName;
    private final String customerEmail;
    private final String customerPhone;
    private final Map<String, String> metadata;

    private PaymentRequest(Builder builder) {
        this.userId = clean(builder.userId);
        this.bookingId = clean(builder.bookingId);
        this.moduleType = builder.moduleType;
        this.itemId = clean(builder.itemId);
        this.businessId = clean(builder.businessId);
        this.title = clean(builder.title);
        this.description = clean(builder.description);
        this.amount = builder.amount;
        this.currency = clean(builder.currency == null ? "INR" : builder.currency).toUpperCase();
        this.customerName = clean(builder.customerName);
        this.customerEmail = clean(builder.customerEmail);
        this.customerPhone = clean(builder.customerPhone);
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
    }

    public void validate() throws PaymentException {
        require(userId, "User is required for payment.");
        require(bookingId, "Booking reference is required for payment.");
        if (moduleType == null) {
            throw new PaymentException("Payment module type is required.");
        }
        require(title, "Payment title is required.");
        require(customerName, "Customer name is required for payment.");
        MoneyUtil.validateAmount(amount, currency);
    }

    public String idempotencyKey() {
        return moduleType.name() + ":" + bookingId;
    }

    public String userId() {
        return userId;
    }

    public String bookingId() {
        return bookingId;
    }

    public PaymentModuleType moduleType() {
        return moduleType;
    }

    public String itemId() {
        return itemId;
    }

    public String businessId() {
        return businessId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public String customerName() {
        return customerName;
    }

    public String customerEmail() {
        return customerEmail;
    }

    public String customerPhone() {
        return customerPhone;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void require(String value, String message) throws PaymentException {
        if (value == null || value.isBlank()) {
            throw new PaymentException(message);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Builder {
        private String userId;
        private String bookingId;
        private PaymentModuleType moduleType;
        private String itemId;
        private String businessId;
        private String title;
        private String description;
        private BigDecimal amount;
        private String currency = "INR";
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder bookingId(String bookingId) {
            this.bookingId = bookingId;
            return this;
        }

        public Builder moduleType(PaymentModuleType moduleType) {
            this.moduleType = moduleType;
            return this;
        }

        public Builder itemId(String itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder businessId(String businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public Builder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder customerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
            return this;
        }

        public Builder metadata(String key, String value) {
            if (key != null && !key.isBlank() && value != null) {
                metadata.put(key.trim(), value.trim());
            }
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            if (metadata != null) {
                metadata.forEach(this::metadata);
            }
            return this;
        }

        public PaymentRequest build() {
            return new PaymentRequest(this);
        }
    }
}
