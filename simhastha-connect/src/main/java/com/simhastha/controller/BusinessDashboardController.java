package com.simhastha.controller;

import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.model.BusinessLocation;
import com.simhastha.model.BusinessProfileUpdate;
import com.simhastha.model.CloudImage;
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

    public void updateBusinessProfile(BusinessProfileUpdate update, String idToken)
            throws IOException, InterruptedException {
        service.updateBusinessProfile(update, idToken);
    }

    public void updateBusinessLocation(String businessId, BusinessLocation location, String idToken)
            throws IOException, InterruptedException {
        service.updateBusinessLocation(businessId, location, idToken);
    }

    public void updateBusinessMedia(String businessId, String logoUrl, String logoPublicId, String coverPhotoUrl,
            String coverPhotoPublicId, List<CloudImage> galleryImages, String idToken)
            throws IOException, InterruptedException {
        service.updateBusinessMedia(businessId, logoUrl, logoPublicId, coverPhotoUrl, coverPhotoPublicId,
                galleryImages, idToken);
    }

    public List<AppDataStore.BookingRecord> findBookingsForBusiness(String businessId, String idToken)
            throws IOException, InterruptedException {
        return service.findBookingsForBusiness(businessId, idToken);
    }
}
