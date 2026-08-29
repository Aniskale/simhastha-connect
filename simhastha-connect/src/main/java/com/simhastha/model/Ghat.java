package com.simhastha.model;

import java.util.List;

/** Admin-controlled ghat information shown to pilgrims. Optional live values stay nullable. */
public record Ghat(
        String id, String name, String area, String description, Double latitude, Double longitude,
        Double entryLatitude, Double entryLongitude, String imageUrl, OperationalStatus operationalStatus,
        CrowdLevel crowdLevel, Integer estimatedWaitMinutes, boolean bathingAvailable, Walking walking,
        List<String> facilities, Weather weather, History history, String lastUpdated, GhatOperationalState operationalState,
        boolean published, boolean active) {

    public Ghat(String id, String name, String area, String description, Double latitude, Double longitude,
            Double entryLatitude, Double entryLongitude, String imageUrl, OperationalStatus operationalStatus,
            CrowdLevel crowdLevel, Integer estimatedWaitMinutes, boolean bathingAvailable, Walking walking,
            List<String> facilities, Weather weather, History history, String lastUpdated) {
        this(id, name, area, description, latitude, longitude, entryLatitude, entryLongitude, imageUrl,
                operationalStatus, crowdLevel, estimatedWaitMinutes, bathingAvailable, walking, facilities,
                weather, history, lastUpdated, GhatOperationalState.unavailable());
    }

    /** Backward-compatible shared Ghat constructor; catalogue records are visible and active by default. */
    public Ghat(String id, String name, String area, String description, Double latitude, Double longitude,
            Double entryLatitude, Double entryLongitude, String imageUrl, OperationalStatus operationalStatus,
            CrowdLevel crowdLevel, Integer estimatedWaitMinutes, boolean bathingAvailable, Walking walking,
            List<String> facilities, Weather weather, History history, String lastUpdated, GhatOperationalState operationalState) {
        this(id, name, area, description, latitude, longitude, entryLatitude, entryLongitude, imageUrl,
                operationalStatus, crowdLevel, estimatedWaitMinutes, bathingAvailable, walking, facilities,
                weather, history, lastUpdated, operationalState, true, true);
    }

    public Ghat {
        id = safe(id);
        name = safe(name);
        area = safe(area);
        description = safe(description);
        imageUrl = safe(imageUrl);
        operationalStatus = operationalStatus == null ? OperationalStatus.INFORMATION_ONLY : operationalStatus;
        crowdLevel = crowdLevel == null ? CrowdLevel.UNKNOWN : crowdLevel;
        walking = walking == null ? Walking.unknown() : walking;
        facilities = facilities == null ? List.of() : List.copyOf(facilities);
        weather = weather == null ? Weather.unavailable() : weather;
        history = history == null ? History.unavailable() : history;
        lastUpdated = safe(lastUpdated);
        operationalState = operationalState == null ? GhatOperationalState.unavailable() : operationalState;
    }

    public boolean isRecommendedForSnan() {
        return operationalStatus == OperationalStatus.OPEN && bathingAvailable && operationalState.bathingRecommended();
    }

    public String waitLabel() {
        if (estimatedWaitMinutes == null || estimatedWaitMinutes < 0) return "Wait time unavailable";
        return estimatedWaitMinutes >= 45 ? "45+ min" : estimatedWaitMinutes + " min";
    }

    public enum CrowdLevel { LOW, MODERATE, HIGH, CRITICAL, UNKNOWN }
    public enum OperationalStatus { OPEN, PARTIALLY_RESTRICTED, RESTRICTED, TEMPORARILY_CLOSED, EMERGENCY_CLOSED, INFORMATION_ONLY }
    public enum WalkingDifficulty { EASY, MODERATE, DIFFICULT }

    public record Walking(WalkingDifficulty difficulty, Integer approximateSteps, Integer distanceMeters,
                          boolean seniorFriendly, boolean wheelchairAccessible) {
        public Walking { difficulty = difficulty == null ? WalkingDifficulty.MODERATE : difficulty; }
        public static Walking unknown() { return new Walking(WalkingDifficulty.MODERATE, null, null, false, false); }
    }

    public record Weather(Integer temperatureCelsius, String condition, Integer rainProbability, Integer humidity, String updatedAt) {
        public Weather(Integer temperatureCelsius, String condition) { this(temperatureCelsius, condition, null, null, ""); }
        public Weather { condition = safe(condition); updatedAt = safe(updatedAt); }
        public static Weather unavailable() { return new Weather(null, "", null, null, ""); }
        public boolean available() { return temperatureCelsius != null && !condition.isBlank(); }
    }

    /** Content is intentionally supplied by verified/admin-controlled Firestore documents. */
    public record History(String historicalBackground, String religiousSignificance, String simhasthaConnection,
                          String associatedSacredPlaces, String rituals, String didYouKnow, String imageUrl) {
        public History {
            historicalBackground = safe(historicalBackground); religiousSignificance = safe(religiousSignificance);
            simhasthaConnection = safe(simhasthaConnection); associatedSacredPlaces = safe(associatedSacredPlaces);
            rituals = safe(rituals); didYouKnow = safe(didYouKnow); imageUrl = safe(imageUrl);
        }
        public static History unavailable() { return new History("", "", "", "", "", "", ""); }
        public boolean available() { return !(historicalBackground + religiousSignificance + simhasthaConnection
                + associatedSacredPlaces + rituals + didYouKnow).isBlank(); }
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
