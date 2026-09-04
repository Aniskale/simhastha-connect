package com.simhastha.packages;

import java.time.LocalDate;
import java.util.Set;

public record PackageSearchCriteria(String origin, LocalDate departureDate, String duration, PackageCategory category,
        Set<String> types, Set<String> durations, Set<String> budgets, Set<String> travel, Set<String> stay,
        Set<String> meals, Set<String> themes, Set<String> extras) {
    public static PackageSearchCriteria empty() { return new PackageSearchCriteria(null, null, null, null, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()); }
}
