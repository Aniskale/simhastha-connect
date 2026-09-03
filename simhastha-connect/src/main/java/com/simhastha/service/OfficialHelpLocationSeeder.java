package com.simhastha.service;

import com.simhastha.dao.LostFoundReportDao;

/** Development/admin-only entry point for verified Help Near Me location records. */
public final class OfficialHelpLocationSeeder {
    private final LostFoundReportDao dao;
    public OfficialHelpLocationSeeder(LostFoundReportDao dao) { this.dao = dao; }
    public int seed(String adminIdToken) throws Exception { return dao.seedOfficialHelpLocations(adminIdToken); }
}
