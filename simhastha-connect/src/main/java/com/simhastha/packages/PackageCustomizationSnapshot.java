package com.simhastha.packages;

import java.util.Map;
/** Part 3 handoff only; it is not a booking or payment record. */
public record PackageCustomizationSnapshot(String packageId, String packageCode, Map<String, String> selections,
        Map<String, Integer> componentPrices, int totalAmount, String currency, String createdAt) { }
