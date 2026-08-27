package com.simhastha.model;

public record PublicBusinessItem(
        String itemId,
        String businessId,
        String name,
        String itemType,
        String description,
        String price,
        String totalUnits,
        String availableUnits,
        String stock,
        String availability) {
}
