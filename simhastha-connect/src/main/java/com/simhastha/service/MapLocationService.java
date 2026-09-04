package com.simhastha.service;

import com.simhastha.model.MapLocation;
import com.simhastha.model.MapLocationType;
import com.simhastha.model.PublicBusinessListing;

import java.util.List;

public final class MapLocationService {
    private final BusinessMarketplaceService businessService = new BusinessMarketplaceService();

    public List<MapLocation> loadLocations(String idToken, MapLocationType type) throws Exception {
        if (type != null && type != MapLocationType.BUSINESS) {
            return List.of();
        }
        return businessService.loadApprovedBusinesses(idToken).stream()
                .filter(PublicBusinessListing::hasCoordinates)
                .map(this::fromBusiness)
                .toList();
    }

    private MapLocation fromBusiness(PublicBusinessListing business) {
        return new MapLocation("business-" + business.businessId(), business.name(), MapLocationType.BUSINESS,
                business.address(), business.area(), business.city(), business.latitudeValue(), business.longitudeValue(),
                "", "", true, false, "", business.mobile(), business.description(), business.businessId(),
                business.displayCategory());
    }
}
