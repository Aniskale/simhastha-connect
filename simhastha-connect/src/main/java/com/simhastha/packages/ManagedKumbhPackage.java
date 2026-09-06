package com.simhastha.packages;

import java.util.List;
import java.util.Map;

/** Admin-owned package definition. String lists are deliberately data-driven for Connection A forms. */
public record ManagedKumbhPackage(String packageId, String packageCode, String name, PackageCategory category,
        String theme, String badge, String origin, String destination, int days, int nights, String shortDescription,
        String description, List<String> travelOptions, List<String> stayOptions, List<String> mealOptions,
        List<String> facilities, List<String> touristPlaces, List<KumbhPackage.Day> itinerary, int basePrice,
        int startingPrice, int originalPrice, int discount, List<String> inclusions, List<String> exclusions,
        Map<String, String> policies, String availableFrom, String availableUntil, String departureDates,
        int maximumCapacity, int minimumTravellers, PackageStatus status, String createdBy, String createdAt,
        String updatedAt, String publishedAt, PackageMedia coverImage, PackageMedia heroImage, List<PackageMedia> gallery) {
    /** Converts the original in-app catalogue record into an admin-owned record. */
    public static ManagedKumbhPackage fromCatalogue(KumbhPackage source, String createdBy) {
        String now = String.valueOf(System.currentTimeMillis());
        String owner = createdBy == null || createdBy.isBlank() ? "legacy-migration" : createdBy;
        return new ManagedKumbhPackage(source.id(), "LEGACY-" + source.id(), source.title(), source.category(),
                source.themes().isEmpty() ? "" : source.themes().get(0), "Legacy catalogue", source.origin(),
                "Nashik – Simhastha 2027", source.days(), source.nights(), source.title(), source.title(),
                source.travelChoices(), source.stayChoices(), source.mealChoices(), source.facilities(),
                source.sightseeing(), source.itinerary(), source.price(), source.price(), source.price(),
                source.discount(), source.inclusions(), source.exclusions(), Map.of(), "", "", "", 0, 1,
                PackageStatus.PUBLISHED, owner, now, now, now, source.coverImage(), source.heroImage(), source.gallery());
    }

    public KumbhPackage cataloguePackage() {
        String travel = travelOptions.isEmpty() ? "Travel details configured by admin" : travelOptions.get(0);
        String stay = stayOptions.isEmpty() ? "Stay details configured by admin" : stayOptions.get(0);
        String meals = mealOptions.isEmpty() ? "Meal plan configured by admin" : mealOptions.get(0);
        return new KumbhPackage(packageId, name, origin, category, days, nights, startingPrice, discount, travel, stay,
                meals, theme.isBlank() ? List.of() : List.of(theme), facilities, touristPlaces, inclusions, exclusions, itinerary,
                coverImage, heroImage, gallery, travelOptions, stayOptions, mealOptions);
    }
}
