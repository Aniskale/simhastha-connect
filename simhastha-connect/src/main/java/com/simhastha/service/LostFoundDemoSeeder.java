package com.simhastha.service;

import com.simhastha.dao.LostFoundReportDao;

/** Development-only entry point; it can only run through an authenticated admin session. */
public final class LostFoundDemoSeeder {
    private final LostFoundReportDao dao;

    public LostFoundDemoSeeder(LostFoundReportDao dao) { this.dao = dao; }

    public int seed(String adminIdToken) throws Exception { return dao.seedDemoReports(adminIdToken); }
}
