package com.simhastha.controller;

import com.simhastha.model.MapLocation;
import com.simhastha.model.MapLocationType;
import com.simhastha.service.MapLocationService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SimhasthaMapController {
    private final MapLocationService service = new MapLocationService();

    public CompletableFuture<List<MapLocation>> loadLocations(String idToken, MapLocationType type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.loadLocations(idToken, type);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }
}
