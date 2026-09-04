package com.simhastha.service;

import com.simhastha.dao.BookingDao;
import com.simhastha.dao.BusinessDao;
import com.simhastha.gateway.firebase.FirebaseConfig;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.dao.implementation.FirestoreBookingDao;
import com.simhastha.dao.implementation.FirestoreBusinessDao;
import com.simhastha.model.BusinessLocation;
import com.simhastha.model.BusinessProfileUpdate;
import com.simhastha.model.CloudImage;
import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class BusinessDashboardService {

    private final FirestoreGateway gateway;
    private final BusinessDao businessDao;
    private final BookingDao bookingDao;

    public BusinessDashboardService() {
        this(new FirestoreGateway(FirebaseConfig.load()));
    }

    BusinessDashboardService(FirestoreGateway gateway) {
        this.gateway = gateway;
        this.businessDao = new FirestoreBusinessDao(gateway);
        this.bookingDao = new FirestoreBookingDao(gateway);
    }

    public boolean isFirebaseEnabled() {
        return gateway.isEnabled();
    }

    public Optional<FirestoreGateway.BusinessProfile> findBusinessForOwner(String ownerId, String idToken)
            throws IOException, InterruptedException {
        return businessDao.findByOwner(ownerId, idToken);
    }

    public List<FirestoreGateway.BusinessInventoryItem> findInventory(String businessId, String ownerId, String idToken)
            throws IOException, InterruptedException {
        return businessDao.findInventory(businessId, ownerId, idToken);
    }

    public void saveInventoryItem(FirestoreGateway.BusinessInventoryItem item, String idToken)
            throws IOException, InterruptedException {
        businessDao.saveInventoryItem(item, idToken);
    }

    public void updateBusinessProfile(BusinessProfileUpdate update, String idToken)
            throws IOException, InterruptedException {
        businessDao.updateProfile(update, idToken);
    }

    public void updateBusinessLocation(String businessId, BusinessLocation location, String idToken)
            throws IOException, InterruptedException {
        businessDao.updateLocation(businessId, location, idToken);
    }

    public void updateBusinessMedia(String businessId, String logoUrl, String logoPublicId, String coverPhotoUrl,
            String coverPhotoPublicId, List<CloudImage> galleryImages, String idToken)
            throws IOException, InterruptedException {
        businessDao.updateMedia(businessId, logoUrl, logoPublicId, coverPhotoUrl, coverPhotoPublicId,
                galleryImages, idToken);
    }

    public List<AppDataStore.BookingRecord> findBookingsForBusiness(String businessId, String idToken)
            throws IOException, InterruptedException {
        return bookingDao.findByField("businessId", businessId, idToken);
    }
}
