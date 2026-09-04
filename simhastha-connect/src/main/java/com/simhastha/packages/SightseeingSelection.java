package com.simhastha.packages;

/** Stable Nashik sightseeing data retained independently from its rendered card. */
public record SightseeingSelection(String id, String name, String location, String description,
        String duration, int packageCharge) { }
