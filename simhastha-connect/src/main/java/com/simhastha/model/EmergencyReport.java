package com.simhastha.model;

import java.util.Locale;
import java.util.Objects;

/** Immutable Firestore-backed emergency request. Administrative fields are only changed by admins. */
public record EmergencyReport(
        String emergencyId, String trackingId, String userId, String reporterName, String reporterPhone,
        EmergencyType emergencyType, String description, int peopleAffected, String landmark,
        double latitude, double longitude, String locationLabel, Priority priority, Status status,
        String assignedTeamId, String assignedTeamName, String assignedTeamType, String adminNotes,
        String userStatusMessage, String createdAt, String updatedAt, String acknowledgedAt,
        String dispatchedAt, String helpArrivingAt, String resolvedAt, Source source) {

    public enum EmergencyType { MEDICAL, POLICE_SECURITY, FIRE, CROWD_RISK, ACCIDENT, CHILD_ELDERLY_ASSISTANCE, RIVER_GHAT, OTHER }
    public enum Priority { CRITICAL, HIGH, MEDIUM }
    public enum Status { REPORTED, ACKNOWLEDGED, TEAM_DISPATCHED, HELP_ARRIVING, RESOLVED }
    public enum Source { SOS, REPORT }

    public EmergencyReport {
        emergencyId = safe(emergencyId);
        trackingId = safe(trackingId);
        userId = safe(userId);
        reporterName = safe(reporterName);
        reporterPhone = safe(reporterPhone);
        description = safe(description);
        landmark = safe(landmark);
        locationLabel = safe(locationLabel);
        assignedTeamId = safe(assignedTeamId);
        assignedTeamName = safe(assignedTeamName);
        assignedTeamType = safe(assignedTeamType);
        adminNotes = safe(adminNotes);
        userStatusMessage = safe(userStatusMessage);
        createdAt = safe(createdAt);
        updatedAt = safe(updatedAt);
        acknowledgedAt = safe(acknowledgedAt);
        dispatchedAt = safe(dispatchedAt);
        helpArrivingAt = safe(helpArrivingAt);
        resolvedAt = safe(resolvedAt);
        emergencyType = Objects.requireNonNullElse(emergencyType, EmergencyType.OTHER);
        priority = Objects.requireNonNullElse(priority, Priority.MEDIUM);
        status = Objects.requireNonNullElse(status, Status.REPORTED);
        source = Objects.requireNonNullElse(source, Source.REPORT);
        peopleAffected = Math.max(0, peopleAffected);
    }

    public boolean unresolved() { return status != Status.RESOLVED; }

    public static EmergencyType typeForLabel(String label) {
        String value = safe(label).toUpperCase(Locale.ROOT);
        if (value.contains("MEDICAL")) return EmergencyType.MEDICAL;
        if (value.contains("POLICE")) return EmergencyType.POLICE_SECURITY;
        if (value.contains("FIRE")) return EmergencyType.FIRE;
        if (value.contains("CROWD")) return EmergencyType.CROWD_RISK;
        if (value.contains("ACCIDENT")) return EmergencyType.ACCIDENT;
        if (value.contains("CHILD") || value.contains("ELDERLY")) return EmergencyType.CHILD_ELDERLY_ASSISTANCE;
        if (value.contains("RIVER") || value.contains("GHAT")) return EmergencyType.RIVER_GHAT;
        return EmergencyType.OTHER;
    }

    public static Priority defaultPriority(EmergencyType type) {
        return type == EmergencyType.FIRE || type == EmergencyType.CROWD_RISK ? Priority.CRITICAL
                : type == EmergencyType.OTHER ? Priority.MEDIUM : Priority.HIGH;
    }

    public static String userStatusText(Status value) {
        return switch (Objects.requireNonNullElse(value, Status.REPORTED)) {
            case REPORTED -> "Emergency request submitted.";
            case ACKNOWLEDGED -> "Emergency Management has acknowledged your request.";
            case TEAM_DISPATCHED -> "Response team has been dispatched.";
            case HELP_ARRIVING -> "Assigned help team is approaching.";
            case RESOLVED -> "Emergency request has been resolved.";
        };
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
