package com.simhastha.payment.backend;

import com.simhastha.payment.PaymentException;
import com.simhastha.payment.PaymentModuleType;
import com.simhastha.payment.PaymentRecord;
import com.simhastha.payment.PaymentRequest;

/** Fails closed until the trusted Firestore transaction service is configured. */
public final class UnavailableBusinessInventoryHoldService implements BusinessInventoryHoldService {
    private static final String MESSAGE = "Business booking is temporarily unavailable because the trusted inventory hold backend is not configured.";

    @Override
    public void createHold(PaymentRequest request) throws PaymentException {
        if (request.moduleType() == PaymentModuleType.BUSINESS) {
            throw new PaymentException(MESSAGE);
        }
    }

    @Override
    public void confirmHold(PaymentRecord payment) throws PaymentException {
        if (payment.moduleType() == PaymentModuleType.BUSINESS) {
            throw new PaymentException(MESSAGE);
        }
    }

    @Override
    public void releaseHold(PaymentRecord payment, String reason) {
        // No authoritative hold was ever created, so there is nothing safe to release.
    }

    @Override
    public void releaseHold(PaymentRequest request, String reason) {
        // No authoritative hold was ever created, so there is nothing safe to release.
    }
}
