package com.simhastha.model;

import java.util.List;
import java.util.Objects;

/** Admin-managed emergency facility stored in emergency_services. */
public record EmergencyService(String serviceId, String name, Category category, String subType, String description,
        double latitude, double longitude, String address, String sector, String area, String landmark,
        String contactNumber, String alternateContact, OperationalStatus operationalStatus, List<String> facilities,
        boolean active, String createdAt, String updatedAt, String createdBy, String updatedBy) {
    public enum Category { HOSPITAL, MEDICAL_CAMP, FIRST_AID, AMBULANCE, POLICE, FIRE_SAFETY, HELP_DESK, EMERGENCY_EXIT }
    public enum OperationalStatus { OPEN, LIMITED, TEMPORARILY_CLOSED, INACTIVE }
    public EmergencyService {
        serviceId = safe(serviceId); name = safe(name); subType = safe(subType); description = safe(description);
        address = safe(address); sector = safe(sector); area = safe(area); landmark = safe(landmark);
        contactNumber = safe(contactNumber); alternateContact = safe(alternateContact); createdAt = safe(createdAt);
        updatedAt = safe(updatedAt); createdBy = safe(createdBy); updatedBy = safe(updatedBy);
        category = Objects.requireNonNullElse(category, Category.HELP_DESK);
        operationalStatus = Objects.requireNonNullElse(operationalStatus, OperationalStatus.INACTIVE);
        facilities = facilities == null ? List.of() : List.copyOf(facilities);
    }
    public boolean visibleToUsers() { return active && operationalStatus != OperationalStatus.INACTIVE; }
    public static String displayStatus(OperationalStatus value) { return switch (Objects.requireNonNullElse(value, OperationalStatus.INACTIVE)) {
        case OPEN -> "Open"; case LIMITED -> "Limited Service"; case TEMPORARILY_CLOSED -> "Temporarily Closed"; case INACTIVE -> "Inactive"; }; }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
