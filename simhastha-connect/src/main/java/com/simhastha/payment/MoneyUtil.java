package com.simhastha.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public final class MoneyUtil {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("INR");

    private MoneyUtil() {
    }

    public static void validateAmount(BigDecimal amount, String currency) throws PaymentException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Payment amount must be greater than zero.");
        }
        if (currency == null || !SUPPORTED_CURRENCIES.contains(currency.trim().toUpperCase())) {
            throw new PaymentException("Unsupported payment currency.");
        }
        if (amount.scale() > 2) {
            throw new PaymentException("Payment amount must not contain more than two decimal places.");
        }
    }

    public static long toSmallestUnit(BigDecimal amount, String currency) throws PaymentException {
        validateAmount(amount, currency);
        return amount.setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact();
    }

    public static BigDecimal fromSmallestUnit(long amount, String currency) throws PaymentException {
        if (amount <= 0) {
            throw new PaymentException("Payment amount must be greater than zero.");
        }
        if (currency == null || !SUPPORTED_CURRENCIES.contains(currency.trim().toUpperCase())) {
            throw new PaymentException("Unsupported payment currency.");
        }
        return BigDecimal.valueOf(amount, 2);
    }
}
