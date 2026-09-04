package com.simhastha.model;

import java.util.List;

/** Verified, active locations are the only locations exposed to pilgrims. */
public record OfficialHelpLocation(
        String id, String name, String type, String area, double latitude, double longitude, String address, String landmark,
        String phone, String openingHours, List<String> services, String verificationStatus, boolean active,
        String createdAt, String updatedAt) {
    public OfficialHelpLocation { services = services == null ? List.of() : List.copyOf(services); }
    public boolean publiclyVisible() { return active && "VERIFIED".equalsIgnoreCase(verificationStatus); }
}
