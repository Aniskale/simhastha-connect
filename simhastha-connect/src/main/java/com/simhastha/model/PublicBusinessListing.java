package com.simhastha.model;

import java.util.List;
import java.util.Locale;

public record PublicBusinessListing(
        String businessId,
        String name,
        String category,
        String displayCategory,
        String description,
        String location,
        String operatingHours,
        String priceRange,
        List<PublicBusinessItem> items) {

    public String searchText() {
        StringBuilder text = new StringBuilder();
        text.append(name).append(' ')
                .append(category).append(' ')
                .append(displayCategory).append(' ')
                .append(description).append(' ')
                .append(location).append(' ')
                .append(operatingHours).append(' ')
                .append(priceRange);
        for (PublicBusinessItem item : items) {
            text.append(' ')
                    .append(item.name()).append(' ')
                    .append(item.itemType()).append(' ')
                    .append(item.description()).append(' ')
                    .append(item.availability());
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }
}
