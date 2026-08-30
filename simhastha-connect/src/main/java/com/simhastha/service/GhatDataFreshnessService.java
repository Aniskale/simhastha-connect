package com.simhastha.service;

import java.time.Duration;

/** Central freshness policy for operational status; configurable later without touching JavaFX. */
public final class GhatDataFreshnessService {
    private static final Duration STALE_AFTER = Duration.ofMinutes(60);
    private GhatDataFreshnessService() { }
    public static boolean isStaleEpochMillis(String updated) {
        if (updated == null || updated.isBlank()) return true;
        try { return System.currentTimeMillis() - Long.parseLong(updated) > STALE_AFTER.toMillis(); }
        catch (NumberFormatException exception) { return false; }
    }
}
