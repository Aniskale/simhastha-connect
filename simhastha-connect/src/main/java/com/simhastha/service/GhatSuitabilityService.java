package com.simhastha.service;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;

public final class GhatSuitabilityService {
    public boolean seniorRecommended(Ghat ghat) { return ghat.walking().seniorFriendly() && ghat.crowdLevel().ordinal() <= Ghat.CrowdLevel.MODERATE.ordinal() && ghat.operationalState().bathingRecommended() && available(ghat, "medical") && ghat.operationalStatus() == Ghat.OperationalStatus.OPEN; }
    public boolean familyRecommended(Ghat ghat) { return ghat.crowdLevel().ordinal() <= Ghat.CrowdLevel.MODERATE.ordinal() && ghat.operationalState().bathingRecommended() && ghat.operationalStatus() == Ghat.OperationalStatus.OPEN && available(ghat, "toilet") && available(ghat, "medical"); }
    private boolean available(Ghat ghat, String facility) { return ghat.operationalState().facilities().stream().anyMatch(item -> item.name().toLowerCase().contains(facility) && item.status() == GhatOperationalState.FacilityStatus.AVAILABLE); }
}
