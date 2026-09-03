package com.simhastha.dao;

import com.simhastha.model.LostFoundReport;
import com.simhastha.view.FirestoreGateway;
import java.util.List;

public final class LostFoundReportDao {
    private final FirestoreGateway gateway;
    public LostFoundReportDao(FirestoreGateway gateway) { this.gateway = gateway; }
    public void save(LostFoundReport report, String token) throws Exception { gateway.saveLostFoundReport(report, token); }
    public List<LostFoundReport> findForUser(String userId, String token) throws Exception { return gateway.loadLostFoundReportsForUser(userId, token); }
    public void updateStatus(String reportId, String status, String token) throws Exception {
        gateway.markLostFoundReportFoundByOwner(reportId, token);
    }
    public int seedDemoReports(String token) throws Exception { return gateway.seedLostFoundDemoReports(token); }
    public int seedOfficialHelpLocations(String token) throws Exception { return gateway.seedOfficialHelpLocations(token); }
    public List<com.simhastha.model.OfficialHelpLocation> officialHelpLocations(String token) throws Exception {
        return gateway.loadOfficialHelpLocations(token);
    }
    public List<com.simhastha.model.OfficialHelpLocation> officialHelpLocationsForAdmin(String token) throws Exception {
        return gateway.loadOfficialHelpLocationsForAdmin(token);
    }
    public void saveOfficialHelpLocation(com.simhastha.model.OfficialHelpLocation location, boolean create, String token) throws Exception {
        gateway.saveOfficialHelpLocation(location, create, token);
    }
    public void deleteOfficialHelpLocation(String locationId, String token) throws Exception {
        gateway.deleteOfficialHelpLocation(locationId, token);
    }
}
