package com.simhastha.model;

import java.util.List;

/** User-owned Lost & Found record persisted in lostFoundReports. */
public record LostFoundReport(
        String reportId, String trackingId, String userId, String reportType, String category, String title,
        String description, List<String> imageUrls, String locationName, Double latitude, Double longitude,
        String landmark, String incidentDate, String incidentTime, String reporterName, String reporterPhone,
        String relation, String priority, String priorityReason, String status, String verificationStatus,
        String assignedAdminId, String assignedAuthorityId, String foundLocationId, String collectionInstructions,
        String createdAt, String updatedAt, String resolvedAt) {
    public LostFoundReport {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
