package com.simhastha.packages;

import java.util.List;

/** Catalogue-only package data. Provider availability and final pricing are intentionally not represented here. */
public record KumbhPackage(String id, String title, String origin, PackageCategory category, int days, int nights,
        int price, int discount, String travel, String stay, String meals, List<String> themes, List<String> facilities,
        List<String> sightseeing, List<String> inclusions, List<String> exclusions, List<Day> itinerary,
        PackageMedia coverImage, PackageMedia heroImage, List<PackageMedia> gallery, List<String> travelChoices,
        List<String> stayChoices, List<String> mealChoices) {
    public String duration() { return days == 1 ? "1 Day" : days + "D / " + nights + "N"; }
    public record Day(String title, List<Item> items) { }
    public record Item(ItineraryItemType type, String text, String location) { }
}
