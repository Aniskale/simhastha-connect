package com.simhastha.controller;

import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.service.BusinessDashboardService;
import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class BusinessDashboardController {

    private final BusinessDashboardService service = new BusinessDashboardService();

    public boolean isFirebaseEnabled() {
        return service.isFirebaseEnabled();
    }

    public Optional<FirestoreGateway.BusinessProfile> findBusinessForOwner(String ownerId, String idToken)
            throws IOException, InterruptedException {
        return service.findBusinessForOwner(ownerId, idToken);
    }

    public List<FirestoreGateway.BusinessInventoryItem> findInventory(String businessId, String ownerId, String idToken)
            throws IOException, InterruptedException {
        return service.findInventory(businessId, ownerId, idToken);
    }

    public void saveInventoryItem(FirestoreGateway.BusinessInventoryItem item, String idToken)
            throws IOException, InterruptedException {
        service.saveInventoryItem(item, idToken);
    }

    public List<AppDataStore.BookingRecord> findBookingsForBusiness(String businessId, String idToken)
            throws IOException, InterruptedException {
        return service.findBookingsForBusiness(businessId, idToken);
    }
}
