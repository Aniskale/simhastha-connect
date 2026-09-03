package com.simhastha.service;

import com.simhastha.dao.LostFoundReportDao;
import com.simhastha.model.LostFoundReport;
import java.time.Year;
import java.util.List;
import java.util.UUID;

public final class LostFoundReportService {
    private final LostFoundReportDao dao;
    public LostFoundReportService(LostFoundReportDao dao) { this.dao = dao; }
    public LostFoundReport create(LostFoundReport draft, String token) throws Exception {
        if (draft.userId() == null || draft.userId().isBlank()) throw new IllegalArgumentException("Please sign in to submit a report.");
        if (draft.title() == null || draft.title().isBlank() || draft.category() == null || draft.category().isBlank()) throw new IllegalArgumentException("Enter the report category and name or item details.");
        String id = UUID.randomUUID().toString(); String now = String.valueOf(System.currentTimeMillis());
        Priority priority = priorityFor(draft.reportType(), draft.category());
        LostFoundReport report = new LostFoundReport(id, trackingId(), draft.userId(), draft.reportType(), draft.category(), draft.title(), draft.description(), draft.imageUrls(), draft.locationName(), draft.latitude(), draft.longitude(), draft.landmark(), draft.incidentDate(), draft.incidentTime(), draft.reporterName(), draft.reporterPhone(), draft.relation(), priority.value, priority.reason, "SUBMITTED", "PENDING", "", "", "", "", now, now, "");
        dao.save(report, token); return report;
    }
    public List<LostFoundReport> myReports(String userId, String token) throws Exception { return dao.findForUser(userId, token); }
    public void markFoundByOwner(String reportId, String token) throws Exception {
        if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("Report reference is unavailable.");
        dao.updateStatus(reportId, "FOUND", token);
    }
    private String trackingId() { return "SC-LF-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(); }
    private Priority priorityFor(String type, String category) {
        String value = ((type == null ? "" : type) + " " + (category == null ? "" : category)).toLowerCase();
        if (value.contains("child")) return new Priority("CRITICAL", "Missing child requires immediate assistance");
        if (value.contains("senior")) return new Priority("HIGH", "Missing senior citizen requires urgent assistance");
        if (value.contains("person")) return new Priority("MEDIUM", "Missing person report");
        if (value.contains("document") || value.contains("jewellery") || value.contains("valuable")) return new Priority("MEDIUM", "Sensitive or valuable item");
        return new Priority("NORMAL", "General lost and found report");
    }
    private record Priority(String value, String reason) { }
}
