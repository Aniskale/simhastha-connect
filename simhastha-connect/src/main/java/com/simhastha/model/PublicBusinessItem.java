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
        String facilities,
        String availability,
        String photoUrl) {

    public PublicBusinessItem(String itemId, String businessId, String name, String itemType, String description,
            String price, String totalUnits, String availableUnits, String stock, String facilities,
            String availability) {
        this(itemId, businessId, name, itemType, description, price, totalUnits, availableUnits, stock, facilities,
                availability, "");
    }

    public boolean bookingEnabled() {
        return name != null && !name.isBlank()
                && "available".equalsIgnoreCase(availability == null ? "" : availability.trim());
    }

    public boolean hasPhoto() {
        return photoUrl != null && !photoUrl.isBlank();
    }
}
