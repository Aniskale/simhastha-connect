package com.simhastha.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record PublicBusinessListing(
        String businessId,
        String ownerId,
        String name,
        String category,
        String displayCategory,
        String description,
        String location,
        String address,
        String area,
        String city,
        String latitude,
        String longitude,
        String locationUpdatedAt,
        String mobile,
        String email,
        String operatingHours,
        String priceRange,
        List<BusinessMedia> media,
        List<PublicBusinessItem> items) {

    public String searchText() {
        StringBuilder text = new StringBuilder();
        text.append(name).append(' ').append(category).append(' ').append(displayCategory).append(' ')
                .append(description).append(' ').append(location).append(' ').append(address).append(' ')
                .append(area).append(' ').append(city).append(' ').append(mobile).append(' ')
                .append(email).append(' ').append(operatingHours).append(' ').append(priceRange);
        for (PublicBusinessItem item : items == null ? List.<PublicBusinessItem>of() : items) {
            text.append(' ').append(item.name()).append(' ').append(item.itemType()).append(' ')
                    .append(item.description()).append(' ').append(item.facilities()).append(' ')
                    .append(item.availability());
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    public boolean hasCoordinates() {
        return parse(latitude) != null && parse(longitude) != null;
    }

    public double latitudeValue() {
        Double value = parse(latitude);
        return value == null ? 0 : value;
    }

    public double longitudeValue() {
        Double value = parse(longitude);
        return value == null ? 0 : value;
    }

    public String displayLocation() {
        List<String> parts = new ArrayList<>();
        addPart(parts, address);
        addPart(parts, area);
        addPart(parts, city);
        if (!parts.isEmpty()) return String.join(", ", parts);
        return notBlank(location) ? location : "";
    }

    public List<BusinessMedia> activeMedia() {
        return media == null ? List.of() : media.stream()
                .filter(BusinessMedia::active)
                .filter(BusinessMedia::hasUrl)
                .toList();
    }

    public String coverPhotoUrl() {
        List<BusinessMedia> photos = activeMedia();
        return photos.stream().filter(BusinessMedia::cover).map(BusinessMedia::url).findFirst()
                .orElseGet(() -> photos.stream().map(BusinessMedia::url).findFirst().orElse(""));
    }

    private static void addPart(List<String> parts, String value) {
        if (!notBlank(value)) return;
        String clean = value.trim();
        if (parts.stream().noneMatch(existing -> existing.equalsIgnoreCase(clean))) {
            parts.add(clean);
        }
    }

    private static Double parse(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
