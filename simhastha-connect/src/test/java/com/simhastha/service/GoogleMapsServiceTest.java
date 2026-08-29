package com.simhastha.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simhastha.model.Ghat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GoogleMapsServiceTest {
    @Test
    void missingApiKeyKeepsBrowserMapFallbackAvailable() {
        AtomicReference<String> openedUrl = new AtomicReference<>();
        GoogleMapsService service = new GoogleMapsService(GoogleMapsConfig.forApiKey(""), url -> {
            openedUrl.set(url);
            return java.util.Optional.empty();
        });
        assertFalse(service.configured());
        assertTrue(service.showGhats(null, List.of(), null).isEmpty());
        assertTrue(openedUrl.get().contains("Nashik+Ghats+Panchavati"));
    }

    @Test
    void configuredServiceAcceptsEnvironmentProvidedKeyShape() {
        GoogleMapsService service = new GoogleMapsService(GoogleMapsConfig.forApiKey("test-key"));
        assertTrue(service.configured());
    }

    @Test
    void directionsUseExactCoordinatesAndCanOmitOrigin() {
        String route = GoogleMapsService.directionsUrl(null, new GoogleMapsService.Point("Ramkund", 20.0059, 73.7890), "");
        assertTrue(route.contains("destination=20.0059%2C73.789"));
        assertFalse(route.contains("origin="));
    }

    @Test
    void directionsCanFallBackToTheExactGhatNameAndArea() {
        String route = GoogleMapsService.directionsUrl(null, null, "Kapila Ghat Panchavati Nashik");
        assertTrue(route.contains("Kapila+Ghat+Panchavati+Nashik"));
    }

    @Test
    void locateAlwaysUsesTheClickedGhatRatherThanAStaleSelection() {
        GhatCatalogueService catalogue = new GhatCatalogueService();
        AtomicReference<String> openedUrl = new AtomicReference<>();
        GoogleMapsService service = new GoogleMapsService(GoogleMapsConfig.forApiKey(""), url -> {
            openedUrl.set(url);
            return java.util.Optional.empty();
        });

        service.openGhatLocation(find(catalogue, "ramkund"));
        assertTrue(openedUrl.get().contains("Ramkund%2C+Panchavati%2C+Nashik%2C+Maharashtra"));

        service.openGhatLocation(find(catalogue, "kapila"));
        assertTrue(openedUrl.get().contains("Kapila+Ghat%2C+Panchavati%2C+Nashik%2C+Maharashtra"));
        assertFalse(openedUrl.get().contains("Ramkund"));

        service.openGhatLocation(find(catalogue, "naroshankar"));
        assertTrue(openedUrl.get().contains("Naroshankar+Ghat%2C+Panchavati%2C+Nashik%2C+Maharashtra"));

        service.openGhatLocation(find(catalogue, "kushavart"));
        assertTrue(openedUrl.get().contains("Kushavart+Tirtha%2C+Trimbakeshwar%2C+Maharashtra"));
    }

    private Ghat find(GhatCatalogueService catalogue, String id) {
        return catalogue.catalogue().stream().filter(ghat -> ghat.id().equals(id)).findFirst().orElseThrow();
    }
}
