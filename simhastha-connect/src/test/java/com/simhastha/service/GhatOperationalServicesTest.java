package com.simhastha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GhatOperationalServicesTest {
    @Test void crowdComparatorKeepsUsableGhatsInLiveCrowdOrder() {
        List<Ghat> sorted = List.of(ghat("critical", Ghat.CrowdLevel.CRITICAL, state()), ghat("low", Ghat.CrowdLevel.LOW, state()), ghat("high", Ghat.CrowdLevel.HIGH, state()))
                .stream().sorted(GhatCrowdComparator.LIVE_CROWD_ORDER).toList();
        assertEquals(List.of("low", "high", "critical"), sorted.stream().map(Ghat::id).toList());
    }

    @Test void recommendationExcludesUnsafeAndCriticalGhats() {
        Ghat selected = ghat("selected", Ghat.CrowdLevel.CRITICAL, state());
        Ghat safe = ghat("safe", Ghat.CrowdLevel.LOW, state());
        Ghat unsafe = ghat("unsafe", Ghat.CrowdLevel.LOW, new GhatOperationalState(GhatOperationalState.BathingStatus.SUSPENDED, GhatOperationalState.WaterSafety.HIGH_WATER, List.of(), List.of(), List.of(), List.of(), List.of(), GhatOperationalState.CleaningStatus.NORMAL, "", GhatOperationalState.PriorityAlert.none(), ""));
        assertEquals(List.of("safe"), new GhatRecommendationService().alternativesFor(selected, List.of(selected, unsafe, safe)).stream().map(Ghat::id).toList());
    }

    @Test void suitabilityAndScheduledAccessAreComputedFromOperationalData() {
        Ghat seniorSafe = ghat("senior", Ghat.CrowdLevel.LOW, new GhatOperationalState(GhatOperationalState.BathingStatus.AVAILABLE, GhatOperationalState.WaterSafety.NORMAL, List.of(), List.of(), List.of(), List.of(new GhatOperationalState.Facility("Medical", GhatOperationalState.FacilityStatus.AVAILABLE), new GhatOperationalState.Facility("Toilets", GhatOperationalState.FacilityStatus.AVAILABLE)), List.of(new GhatOperationalState.AccessWindow(LocalTime.of(6, 0), LocalTime.of(9, 30), GhatOperationalState.ZoneStatus.OPEN, "")), GhatOperationalState.CleaningStatus.NORMAL, "", GhatOperationalState.PriorityAlert.none(), ""));
        assertTrue(new GhatSuitabilityService().seniorRecommended(seniorSafe));
        assertTrue(new GhatSuitabilityService().familyRecommended(seniorSafe));
        assertEquals(GhatOperationalState.ZoneStatus.OPEN, GhatOperationalStateService.effectiveAccess(seniorSafe, LocalTime.of(7, 0)));
        assertFalse(new GhatSuitabilityService().seniorRecommended(ghat("crowded", Ghat.CrowdLevel.CRITICAL, seniorSafe.operationalState())));
    }

    private Ghat ghat(String id, Ghat.CrowdLevel crowd, GhatOperationalState state) {
        return new Ghat(id, id, "", "", null, null, null, null, "", Ghat.OperationalStatus.OPEN, crowd, 10, true,
                new Ghat.Walking(Ghat.WalkingDifficulty.EASY, 0, 0, true, true), List.of(), Ghat.Weather.unavailable(), Ghat.History.unavailable(), "", state);
    }
    private GhatOperationalState state() { return new GhatOperationalState(GhatOperationalState.BathingStatus.AVAILABLE, GhatOperationalState.WaterSafety.NORMAL, List.of(), List.of(), List.of(), List.of(), List.of(), GhatOperationalState.CleaningStatus.NORMAL, "", GhatOperationalState.PriorityAlert.none(), ""); }
}
