package com.simhastha.service;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import java.util.Comparator;
import java.util.List;

public final class GhatRecommendationService {
    public List<Ghat> alternativesFor(Ghat selected, List<Ghat> all) {
        return all.stream().filter(ghat -> !ghat.id().equals(selected.id())).filter(this::recommended)
                .sorted(GhatCrowdComparator.LIVE_CROWD_ORDER.thenComparing(Comparator.comparing(Ghat::estimatedWaitMinutes, Comparator.nullsLast(Integer::compareTo)))) .toList();
    }
    public boolean recommended(Ghat ghat) {
        return ghat.isRecommendedForSnan() && ghat.operationalState().bathingRecommended()
                && ghat.operationalState().cleaningStatus() != GhatOperationalState.CleaningStatus.SANITATION_CLOSURE
                && ghat.crowdLevel() != Ghat.CrowdLevel.CRITICAL;
    }
}
