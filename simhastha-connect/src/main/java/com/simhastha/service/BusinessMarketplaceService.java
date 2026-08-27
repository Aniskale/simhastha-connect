package com.simhastha.service;

import com.simhastha.dao.BusinessDao;
import com.simhastha.dao.implementation.FirestoreBusinessDao;
import com.simhastha.gateway.firebase.FirebaseConfig;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.model.PublicBusinessItem;
import com.simhastha.model.PublicBusinessListing;
import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BusinessMarketplaceService {

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
        List<AppDataStore.BusinessRecord> records = gateway.isEnabled()
                ? businessDao.findAll(idToken)
                : AppDataStore.businesses();
        List<PublicBusinessListing> listings = new ArrayList<>();
        for (AppDataStore.BusinessRecord business : records) {
            if (!isPubliclyVisible(business)) {
                continue;
            }
            listings.add(new PublicBusinessListing(
                    business.businessId,
                    valueOr(business.businessName, "Verified Business"),
                    business.category,
                    userCategory(business.category),
                    business.description,
                    business.location,
                    business.operatingHours,
                    business.priceRange,
                    loadPublicItems(business, idToken)));
        }
        return listings;
    }

    private List<PublicBusinessItem> loadPublicItems(AppDataStore.BusinessRecord business, String idToken) {
        if (!gateway.isEnabled() || business.businessId.isBlank() || business.ownerId.isBlank()) {
            return List.of();
        }
        try {
            return businessDao.findInventory(business.businessId, business.ownerId, idToken).stream()
                    .filter(FirestoreGateway.BusinessInventoryItem::active)
                    .map(item -> new PublicBusinessItem(item.itemId(), item.businessId(), item.name(),
                            item.itemType(), item.description(), item.price(), item.totalUnits(),
                            item.availableUnits(), item.stock(), item.availability()))
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private boolean isPubliclyVisible(AppDataStore.BusinessRecord business) {
        String status = clean(business.status).toLowerCase(Locale.ROOT);
        return business.approved
                && ("approved".equals(status) || "active".equals(status))
                && !"suspended".equals(status)
                && !"disabled".equals(status);
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
}
