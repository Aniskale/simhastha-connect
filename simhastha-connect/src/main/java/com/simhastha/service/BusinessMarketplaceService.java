package com.simhastha.service;

import com.simhastha.dao.BusinessDao;
import com.simhastha.dao.implementation.FirestoreBusinessDao;
import com.simhastha.gateway.firebase.FirebaseConfig;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.model.BusinessMedia;
import com.simhastha.model.PublicBusinessItem;
import com.simhastha.model.PublicBusinessListing;
import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BusinessMarketplaceService {
    private static final Logger LOGGER = Logger.getLogger(BusinessMarketplaceService.class.getName());
    private final FirestoreGateway gateway;
    private final BusinessDao businessDao;

    public BusinessMarketplaceService() {
        this(new FirestoreGateway(FirebaseConfig.load()));
    }

    BusinessMarketplaceService(FirestoreGateway gateway) {
        this.gateway = gateway;
        this.businessDao = new FirestoreBusinessDao(gateway);
    }

    public boolean isFirebaseEnabled() {
        return gateway.isEnabled();
    }

    public List<PublicBusinessListing> loadApprovedBusinesses(String idToken) throws IOException, InterruptedException {
        List<AppDataStore.BusinessRecord> records;
        try {
            records = gateway.isEnabled() ? businessDao.findPublic(idToken) : AppDataStore.businesses();
        } catch (IOException | InterruptedException exception) {
            LOGGER.log(Level.WARNING, "Approved business records could not be loaded.", exception);
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            records = AppDataStore.businesses();
            if (records.isEmpty()) {
                throw exception;
            }
        }
        List<PublicBusinessListing> listings = new ArrayList<>();
        for (AppDataStore.BusinessRecord business : records) {
            try {
                if (!isPubliclyVisible(business)) continue;
                listings.add(new PublicBusinessListing(
                        business.businessId, business.ownerId, valueOr(business.businessName, "Verified Business"),
                        business.category, userCategory(business.category), business.description, business.location,
                        business.address, business.area, business.city, business.latitude, business.longitude,
                        business.locationUpdatedAt, business.mobile, business.email, business.operatingHours,
                        business.priceRange, mediaFor(business),
                        loadPublicItems(business, idToken)));
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Skipping malformed business record: " + safeBusinessId(business), exception);
            }
        }
        return listings;
    }

    private List<PublicBusinessItem> loadPublicItems(AppDataStore.BusinessRecord business, String idToken) {
        if (business == null || business.businessId == null || business.businessId.isBlank()) return List.of();
        Map<String, PublicBusinessItem> merged = new LinkedHashMap<>();
        try {
            if (gateway.isEnabled()) businessDao.findPublicInventory(business.businessId, idToken).stream()
                    .filter(FirestoreGateway.BusinessInventoryItem::active)
                    .map(item -> new PublicBusinessItem(item.itemId(), item.businessId(), item.name(),
                            item.itemType(), item.description(), item.price(), item.totalUnits(),
                            item.availableUnits(), item.stock(), item.facilities(), item.availability(),
                            AppDataStore.businessItemPhotoFor(item.itemId())))
                    .forEach(item -> merged.put(item.itemId(), item));
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Business inventory could not be loaded for " + business.businessId, exception);
        }
        addLocalItems(merged, AppDataStore.businessItemsFor(business.businessId));
        if (business.ownerId != null && !business.ownerId.equals(business.businessId)) {
            addLocalItems(merged, AppDataStore.businessItemsFor(business.ownerId));
        }
        return new ArrayList<>(merged.values());
    }

    private void addLocalItems(Map<String, PublicBusinessItem> merged, List<PublicBusinessItem> localItems) {
        for (PublicBusinessItem item : localItems) {
            PublicBusinessItem existing = merged.get(item.itemId());
            if (existing == null || (!item.hasPhoto() && !existing.hasPhoto())) merged.put(item.itemId(), item);
            else if (!existing.hasPhoto() && item.hasPhoto()) merged.put(item.itemId(), item);
        }
    }

    private List<BusinessMedia> mediaFor(AppDataStore.BusinessRecord business) {
        List<BusinessMedia> media = AppDataStore.businessMediaFor(business.businessId);
        if (media.isEmpty() && business.ownerId != null && !business.ownerId.equals(business.businessId)) {
            media = AppDataStore.businessMediaFor(business.ownerId);
        }
        return media;
    }

    private boolean isPubliclyVisible(AppDataStore.BusinessRecord business) {
        String status = clean(business.status).toLowerCase(Locale.ROOT);
        boolean approvedOrActive = business.approved || "approved".equals(status) || "active".equals(status);
        return approvedOrActive && !"suspended".equals(status) && !"disabled".equals(status);
    }

    private static String userCategory(String value) {
        String category = clean(value).toLowerCase(Locale.ROOT);
        if (category.contains("tent") || category.contains("camp")) return "Tents";
        if (category.contains("hotel")) return "Hotels";
        if (category.contains("stay") || category.contains("lodge") || category.contains("dharamshala")
                || category.contains("accommodation")) return "Stay";
        if (category.contains("parking")) return "Parking";
        if (category.contains("food") || category.contains("prasadam") || category.contains("restaurant")
                || category.contains("snack")) return category.contains("restaurant") ? "Restaurants" : "Food & Prasadam";
        if (category.contains("puja") || category.contains("pandit")) return "Puja Services";
        if (category.contains("clothes") || category.contains("garment")) return "Clothes";
        if (category.contains("medical") || category.contains("pharmacy")) return "Medical Stores";
        if (category.contains("toilet") || category.contains("sanitation")) return "Toilets";
        if (category.contains("locker")) return "Lockers";
        if (category.contains("charging")) return "Charging Points";
        if (category.contains("guide")) return "Local Guides";
        if (category.contains("shop") || category.contains("retail") || category.contains("store")) return "Shops";
        if (category.contains("essential")) return "Essentials";
        return clean(value).isBlank() ? "Essentials" : value;
    }

    private static String valueOr(String value, String fallback) {
        return clean(value).isBlank() ? fallback : value;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeBusinessId(AppDataStore.BusinessRecord business) {
        return business == null || business.businessId == null || business.businessId.isBlank()
                ? "unknown"
                : business.businessId;
    }
}
