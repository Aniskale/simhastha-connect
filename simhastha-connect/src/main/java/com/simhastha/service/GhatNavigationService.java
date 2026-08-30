package com.simhastha.service;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import java.util.Optional;

/** Pure navigation policy. Route providers are deliberately separate from this safety decision. */
public final class GhatNavigationService {
    public record Point(double latitude, double longitude) { }
    public record Destination(Point point, String entryName) { }
    public enum Decision { READY, HIGH_CROWD_CONFIRMATION, UNSAFE, NO_ENTRY }
    public Decision decision(Ghat ghat) {
        if (ghat.operationalStatus() == Ghat.OperationalStatus.EMERGENCY_CLOSED || ghat.operationalStatus() == Ghat.OperationalStatus.TEMPORARILY_CLOSED || !ghat.operationalState().bathingRecommended()) return Decision.UNSAFE;
        if (destinationFor(ghat).isEmpty() && destinationQueryFor(ghat).isBlank()) return Decision.NO_ENTRY;
        return ghat.crowdLevel().ordinal() >= Ghat.CrowdLevel.HIGH.ordinal() ? Decision.HIGH_CROWD_CONFIRMATION : Decision.READY;
    }
    public Optional<Destination> destinationFor(Ghat ghat) {
        for (GhatOperationalState.Gate gate : ghat.operationalState().gates()) {
            if ((gate.status() == GhatOperationalState.GateStatus.ENTRY_ONLY || gate.status() == GhatOperationalState.GateStatus.OPEN) && gate.latitude() != null && gate.longitude() != null) return Optional.of(new Destination(new Point(gate.latitude(), gate.longitude()), gate.name()));
        }
        return ghat.latitude() == null || ghat.longitude() == null ? Optional.empty() : Optional.of(new Destination(new Point(ghat.latitude(), ghat.longitude()), "Ghat main entry"));
    }
    /** Name/location fallback for Google Maps when verified entry coordinates are not yet available. */
    public String destinationQueryFor(Ghat ghat) {
        String query = (ghat.name() + " " + ghat.area()).trim();
        return query.isBlank() ? "" : query;
    }
}
