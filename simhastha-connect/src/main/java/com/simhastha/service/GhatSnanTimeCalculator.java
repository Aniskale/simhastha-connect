package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.util.OptionalInt;

/** Route timing is deliberately not guessed until a later navigation integration provides it. */
public final class GhatSnanTimeCalculator {
    private GhatSnanTimeCalculator() { }
    public static String totalTimeLabel(Ghat ghat, OptionalInt travelMinutes) {
        if (travelMinutes.isEmpty() || ghat.estimatedWaitMinutes() == null) return "Select location to calculate total time";
        return "≈ " + (travelMinutes.getAsInt() + ghat.estimatedWaitMinutes()) + " min to Snan";
    }
}
