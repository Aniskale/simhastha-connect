package com.simhastha.payment.backend;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.PaymentRequest;

/**
 * Server-side boundary for the transaction that reserves, confirms, or releases
 * inventory. Implementations must use a durable transactional store; this is
 * deliberately not backed by the JavaFX process or the payment memory cache.
 */
public interface BusinessInventoryHoldService {
    void createHold(PaymentRequest request) throws PaymentException;

    void confirmHold(PaymentRecord payment) throws PaymentException;

    void releaseHold(PaymentRecord payment, String reason) throws PaymentException;

    void releaseHold(PaymentRequest request, String reason) throws PaymentException;

    default void releaseExpiredHolds() throws PaymentException { }
}
