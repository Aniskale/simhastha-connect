package com.simhastha.schedule;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Exact schedule destinations are kept in one registry. Coordinates reuse the
 * established project transport-location data where an equivalent landmark exists.
 */
public final class NashikLocationRegistry {
    private static final Map<String, LocationPoint> LOCATIONS = new LinkedHashMap<>();
    static {
        add("ramkund", "Ramkund, Panchavati", 20.0059, 73.7890, "Ramkund Ghat");
        add("godavari-ghat", "Godavari Ghat", 20.0059, 73.7890, "Ramkund Ghat");
        add("panchavati-main-road", "Panchavati Main Road", 20.0067, 73.7914, "Panchavati");
        add("kalaram-temple", "Kalaram Temple", 20.0076, 73.7907, "Kalaram Mandir");
        add("tapovan", "Tapovan", 20.0132, 73.8080, "Tapovan");
        add("trimbakeshwar", "Trimbakeshwar", 19.9322, 73.5303, "Trimbakeshwar Temple");
        add("ozar", "Ozar", 20.1191, 73.9129, "Ozar Airport");
        add("nashik-road", "Nashik Road", 19.9476, 73.8421, "Nashik Road Railway Station");
        add("cbs", "CBS Nashik", 19.9975, 73.7898, "CBS Nashik");
    }
    private NashikLocationRegistry() { }
    public static Optional<LocationPoint> resolve(String locationId, String location, Double latitude, Double longitude) {
        if (validNashikPoint(latitude, longitude)) return Optional.of(new LocationPoint(locationId == null ? "event-location" : locationId,
                location == null ? "Event location" : location, latitude, longitude, "Event coordinates"));
        if (locationId != null && LOCATIONS.containsKey(normalize(locationId))) return Optional.of(LOCATIONS.get(normalize(locationId)));
        String query = normalize(location);
        return LOCATIONS.values().stream().filter(point -> query.contains(normalize(point.displayName())) || normalize(point.displayName()).contains(query)
                || query.contains(normalize(point.sourceName()))).findFirst();
    }
    public static boolean validNashikPoint(Double latitude, Double longitude) {
        return latitude != null && longitude != null && latitude >= 19.85 && latitude <= 20.18 && longitude >= 73.45 && longitude <= 74.00;
    }
    private static void add(String id, String displayName, double latitude, double longitude, String sourceName) { LOCATIONS.put(normalize(id), new LocationPoint(id, displayName, latitude, longitude, sourceName)); }
    private static String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim(); }
    public record LocationPoint(String id, String displayName, double latitude, double longitude, String sourceName) { }
}
