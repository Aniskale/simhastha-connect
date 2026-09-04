package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.util.Comparator;

/** Kept independent of the view so a future live update can reapply the same order. */
public final class GhatCrowdComparator implements Comparator<Ghat> {
    public static final GhatCrowdComparator LIVE_CROWD_ORDER = new GhatCrowdComparator();
    private GhatCrowdComparator() { }

    @Override public int compare(Ghat left, Ghat right) {
        int recommendation = Boolean.compare(!left.isRecommendedForSnan(), !right.isRecommendedForSnan());
        return recommendation != 0 ? recommendation : Integer.compare(order(left.crowdLevel()), order(right.crowdLevel()));
    }
    private int order(Ghat.CrowdLevel level) { return level == Ghat.CrowdLevel.UNKNOWN ? 4 : level.ordinal(); }
}
