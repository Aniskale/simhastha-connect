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
    public KumbhPackage cataloguePackage() {
        String travel = travelOptions.isEmpty() ? "Travel details configured by admin" : travelOptions.get(0);
        String stay = stayOptions.isEmpty() ? "Stay details configured by admin" : stayOptions.get(0);
        String meals = mealOptions.isEmpty() ? "Meal plan configured by admin" : mealOptions.get(0);
        return new KumbhPackage(packageId, name, origin, category, days, nights, startingPrice, discount, travel, stay,
                meals, theme.isBlank() ? List.of() : List.of(theme), facilities, touristPlaces, inclusions, exclusions, itinerary,
                coverImage, heroImage, gallery, travelOptions, stayOptions, mealOptions);
    }
}
