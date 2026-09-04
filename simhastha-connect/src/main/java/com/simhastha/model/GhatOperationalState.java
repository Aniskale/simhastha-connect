package com.simhastha.model;

import java.time.LocalTime;
import java.util.List;

/** Mutable-in-Firestore operational information, deliberately separate from a ghat's base profile. */
public record GhatOperationalState(BathingStatus bathingStatus, WaterSafety waterSafety,
        List<Hazard> hazards, List<Zone> zones, List<Gate> gates, List<Facility> facilities,
        List<AccessWindow> accessWindows, CleaningStatus cleaningStatus, String restrictionReason,
        PriorityAlert priorityAlert, String lastUpdated) {
    public GhatOperationalState {
        bathingStatus = bathingStatus == null ? BathingStatus.UNAVAILABLE : bathingStatus;
        waterSafety = waterSafety == null ? WaterSafety.CAUTION : waterSafety;
        hazards = hazards == null ? List.of() : List.copyOf(hazards);
        zones = zones == null ? List.of() : List.copyOf(zones);
        gates = gates == null ? List.of() : List.copyOf(gates);
        facilities = facilities == null ? List.of() : List.copyOf(facilities);
        accessWindows = accessWindows == null ? List.of() : List.copyOf(accessWindows);
        cleaningStatus = cleaningStatus == null ? CleaningStatus.NORMAL : cleaningStatus;
        restrictionReason = restrictionReason == null ? "" : restrictionReason.trim();
        priorityAlert = priorityAlert == null ? PriorityAlert.none() : priorityAlert;
        lastUpdated = lastUpdated == null ? "" : lastUpdated.trim();
    }
    public static GhatOperationalState unavailable() { return new GhatOperationalState(BathingStatus.UNAVAILABLE,
            WaterSafety.CAUTION, List.of(), List.of(), List.of(), List.of(), List.of(), CleaningStatus.NORMAL, "", PriorityAlert.none(), ""); }
    public boolean bathingRecommended() { return bathingStatus == BathingStatus.AVAILABLE && waterSafety == WaterSafety.NORMAL; }
    public enum BathingStatus { AVAILABLE, CAUTION, SUSPENDED, UNAVAILABLE }
    public enum WaterSafety { NORMAL, CAUTION, HIGH_WATER, DANGEROUS, BATHING_SUSPENDED }
    public enum HazardType { SLIPPERY_STEPS, DAMAGED_RAILING, DEEP_WATER_ZONE, CLEANING_IN_PROGRESS, RESTRICTED_STAIRCASE, POOR_VISIBILITY, TEMPORARY_BARRICADE, OTHER }
    public enum ZoneStatus { OPEN, HIGH_CROWD, CLEANING, RESTRICTED, CLOSED }
    public enum GateStatus { ENTRY_ONLY, EXIT_ONLY, OPEN, CLOSED, RESTRICTED }
    public enum CleaningStatus { NORMAL, CLEANING_IN_PROGRESS, PARTIAL_CLEANING, SANITATION_CLOSURE }
    public enum FacilityStatus { AVAILABLE, BUSY, FULL, TEMPORARILY_CLOSED, UNAVAILABLE }
    public enum AlertPriority { INFO, ADVISORY, WARNING, CRITICAL }

    public record Hazard(HazardType type, String message, AlertPriority priority) {
        public Hazard { type = type == null ? HazardType.OTHER : type; message = message == null ? "" : message.trim(); priority = priority == null ? AlertPriority.ADVISORY : priority; }
    }
    public record Zone(String id, String name, ZoneStatus status, Ghat.CrowdLevel crowdLevel, boolean bathingAvailable, String hazard, String lastUpdated) {
        public Zone { id = id == null ? "" : id; name = name == null ? "" : name; status = status == null ? ZoneStatus.OPEN : status; crowdLevel = crowdLevel == null ? Ghat.CrowdLevel.MODERATE : crowdLevel; hazard = hazard == null ? "" : hazard; lastUpdated = lastUpdated == null ? "" : lastUpdated; }
    }
    public record Gate(String id, String name, GateStatus status, String note, Double latitude, Double longitude) {
        public Gate(String id, String name, GateStatus status, String note) { this(id, name, status, note, null, null); }
        public Gate { id = id == null ? "" : id; name = name == null ? "" : name; status = status == null ? GateStatus.OPEN : status; note = note == null ? "" : note; }
    }
    public record Facility(String name, FacilityStatus status) { public Facility { name = name == null ? "" : name; status = status == null ? FacilityStatus.UNAVAILABLE : status; } }
    public record AccessWindow(LocalTime start, LocalTime end, ZoneStatus accessStatus, String note) {
        public boolean activeAt(LocalTime time) { return start != null && end != null && !time.isBefore(start) && time.isBefore(end); }
    }
    public record PriorityAlert(AlertPriority priority, String message) {
        public PriorityAlert { priority = priority == null ? AlertPriority.INFO : priority; message = message == null ? "" : message.trim(); }
        public static PriorityAlert none() { return new PriorityAlert(AlertPriority.INFO, ""); }
        public boolean present() { return !message.isBlank(); }
    }
}
