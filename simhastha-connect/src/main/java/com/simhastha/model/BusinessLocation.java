package com.simhastha.model;

import java.util.ArrayList;
import java.util.List;

public record BusinessLocation(String address, String area, String city,
        String latitude, String longitude, String locationUpdatedAt) {

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

    public String displayText() {
        List<String> parts = new ArrayList<>();
        add(parts, address);
        add(parts, area);
        add(parts, city);
        return String.join(", ", parts);
    }

    private static void add(List<String> parts, String value) {
        if (value != null && !value.isBlank()
                && parts.stream().noneMatch(existing -> existing.equalsIgnoreCase(value.trim()))) {
            parts.add(value.trim());
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
}
