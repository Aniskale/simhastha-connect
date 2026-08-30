package com.simhastha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simhastha.model.Ghat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GhatServiceTest {

    @Test
    void emptyLiveBackendStillReturnsTheRenderableCatalogue() {
        List<Ghat> ghats = new GhatService(null).resolvedGhats(List.of());

        assertFalse(ghats.isEmpty());
        assertTrue(ghats.stream().anyMatch(ghat -> ghat.id().equals("ramkund")));
        assertTrue(ghats.stream().anyMatch(ghat -> ghat.id().equals("kushavart")));
        assertTrue(ghats.stream().allMatch(ghat -> ghat.crowdLevel() == Ghat.CrowdLevel.UNKNOWN));
    }

    @Test
    void liveOperationalStateEnrichesRatherThanRemovesCatalogueGhat() {
        Ghat liveRamkund = new Ghat("ramkund", "Ramkund", "", "", null, null, null, null, "",
                Ghat.OperationalStatus.OPEN, Ghat.CrowdLevel.LOW, 10, true, Ghat.Walking.unknown(), List.of(),
                Ghat.Weather.unavailable(), Ghat.History.unavailable(), "", null);

        Ghat ramkund = new GhatService(null).resolvedGhats(List.of(liveRamkund)).stream()
                .filter(ghat -> ghat.id().equals("ramkund")).findFirst().orElseThrow();

        assertEquals(Ghat.OperationalStatus.OPEN, ramkund.operationalStatus());
        assertEquals(Ghat.CrowdLevel.LOW, ramkund.crowdLevel());
        assertTrue(ramkund.history().available());
    }

    @Test
    void adminImageAndOperationalFieldsOverrideCatalogueFields() {
        Ghat adminRamkund = live("ramkund", "Ramkund", true, true, Ghat.CrowdLevel.HIGH, "https://media.example/ramkund.jpg");
        Ghat resolved = new GhatService(null).resolvedGhats(List.of(adminRamkund)).stream()
                .filter(ghat -> ghat.id().equals("ramkund")).findFirst().orElseThrow();

        assertEquals(Ghat.CrowdLevel.HIGH, resolved.crowdLevel());
        assertEquals("https://media.example/ramkund.jpg", resolved.imageUrl());
    }

    @Test
    void publishedBackendOnlyGhatIsUserVisibleButDraftIsNot() {
        Ghat published = live("new-ghat", "New Ghat", true, true, Ghat.CrowdLevel.LOW, "");
        Ghat draft = live("draft-ghat", "Draft Ghat", false, true, Ghat.CrowdLevel.LOW, "");
        List<Ghat> userGhats = new GhatService(null).userVisibleGhats(List.of(published, draft));

        assertTrue(userGhats.stream().anyMatch(ghat -> ghat.id().equals("new-ghat")));
        assertFalse(userGhats.stream().anyMatch(ghat -> ghat.id().equals("draft-ghat")));
    }

    @Test
    void crowdUpdatesReuseTheSharedLiveCrowdSortOrder() {
        Ghat high = live("high", "High", true, true, Ghat.CrowdLevel.HIGH, "");
        Ghat low = live("low", "Low", true, true, Ghat.CrowdLevel.LOW, "");
        List<Ghat> sorted = new GhatService(null).userVisibleGhats(List.of(high, low));
        assertEquals("low", sorted.get(0).id());
    }

    @Test
    void persistedAdminImageSurvivesNewServiceInstancesAndRepeatedLoads() throws Exception {
        SharedGhatStore store = new SharedGhatStore();
        Ghat changedRamkund = live("ramkund", "Ramkund", true, true, Ghat.CrowdLevel.HIGH, "https://media.example/ramkund-image-b.jpg");

        new GhatService(new InMemoryGhatRepository(store)).saveGhat(changedRamkund, "").get();
        GhatService reloadedService = new GhatService(new InMemoryGhatRepository(store));
        Ghat firstLoad = ramkund(reloadedService.loadGhats("").get());
        Ghat secondLoad = ramkund(reloadedService.loadGhats("").get());

        assertEquals("https://media.example/ramkund-image-b.jpg", firstLoad.imageUrl());
        assertEquals("https://media.example/ramkund-image-b.jpg", secondLoad.imageUrl());
    }

    @Test
    void localFileUriIsNotAcceptedAsPersistentSharedImage() {
        Ghat localFileImage = live("ramkund", "Ramkund", true, true, Ghat.CrowdLevel.LOW, "file:///temporary/ramkund.jpg");
        var failure = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> new GhatService(new InMemoryGhatRepository(new SharedGhatStore())).saveGhat(localFileImage, "").get());
        assertTrue(failure.getCause() instanceof GhatService.GhatLoadException);
    }

    @Test
    void managedImageReplacementSurvivesRepositoryAndServiceRecreation(@TempDir Path temporaryDirectory) throws Exception {
        GhatImageStorageService storage = new GhatImageStorageService(temporaryDirectory.resolve("managed-ghat-images"));
        Path imageB = Files.writeString(temporaryDirectory.resolve("ramkund-b.jpg"), "image-b");
        Path imageC = Files.writeString(temporaryDirectory.resolve("ramkund-c.jpg"), "image-c");
        String referenceB = storage.store(imageB, "ramkund");
        String referenceC = storage.store(imageC, "ramkund");
        assertTrue(storage.isManagedReference(referenceB));
        assertTrue(storage.isManagedReference(referenceC));

        SharedGhatStore store = new SharedGhatStore();
        new GhatService(new InMemoryGhatRepository(store), storage)
                .saveGhat(live("ramkund", "Ramkund", true, true, Ghat.CrowdLevel.HIGH, referenceB), "").get();
        Ghat userRamkundB = ramkund(new GhatService(new InMemoryGhatRepository(store), storage).loadGhats("").get());
        assertEquals(referenceB, userRamkundB.imageUrl());
        assertEquals(referenceB, new GhatImageService().sourceFor(userRamkundB));

        new GhatService(new InMemoryGhatRepository(store), storage)
                .updateGhatImage(userRamkundB, referenceC, "").get();
        Ghat userRamkundC = ramkund(new GhatService(new InMemoryGhatRepository(store), storage).loadGhats("").get());
        assertEquals(referenceC, userRamkundC.imageUrl());
        assertEquals(referenceC, new GhatImageService().sourceFor(userRamkundC));
    }

    @Test
    void permissionDeniedImageSaveUsesLocalOverrideAcrossFreshServices(@TempDir Path temporaryDirectory) throws Exception {
        GhatImageStorageService storage = new GhatImageStorageService(temporaryDirectory.resolve("data/ghat-images"));
        LocalGhatOverrideStore overrides = new LocalGhatOverrideStore(temporaryDirectory.resolve("data/ghat-overrides.json"));
        Path imageB = Files.writeString(temporaryDirectory.resolve("ramkund-b.jpg"), "image-b");
        Path imageC = Files.writeString(temporaryDirectory.resolve("ramkund-c.jpg"), "image-c");
        String referenceB = storage.store(imageB, "ramkund");
        String referenceC = storage.store(imageC, "ramkund");
        Ghat initial = live("ramkund", "Ramkund", true, true, Ghat.CrowdLevel.UNKNOWN, "");

        GhatService firstService = new GhatService(new PermissionDeniedGhatRepository(), storage, overrides);
        GhatService.GhatUpdateResult first = firstService.updateGhatImage(initial, referenceB, "admin-token").get();
        assertTrue(first.locallySaved());
        assertEquals(referenceB, first.ghat().imageUrl());

        GhatService restartedService = new GhatService(new PermissionDeniedGhatRepository(), storage,
                new LocalGhatOverrideStore(temporaryDirectory.resolve("data/ghat-overrides.json")));
        assertEquals(referenceB, ramkund(restartedService.loadGhats("user-token").get()).imageUrl());

        GhatService.GhatUpdateResult second = restartedService.updateGhatImage(initial, referenceC, "admin-token").get();
        assertTrue(second.locallySaved());
        GhatService afterSecondRestart = new GhatService(new PermissionDeniedGhatRepository(), storage,
                new LocalGhatOverrideStore(temporaryDirectory.resolve("data/ghat-overrides.json")));
        assertEquals(referenceC, ramkund(afterSecondRestart.loadGhats("user-token").get()).imageUrl());
    }

    private Ghat ramkund(List<Ghat> ghats) {
        return ghats.stream().filter(ghat -> ghat.id().equals("ramkund")).findFirst().orElseThrow();
    }

    private Ghat live(String id, String name, boolean published, boolean active, Ghat.CrowdLevel crowd, String image) {
        return new Ghat(id, name, "Admin area", "", null, null, null, null, image, Ghat.OperationalStatus.OPEN, crowd, 12, true,
                Ghat.Walking.unknown(), List.of(), Ghat.Weather.unavailable(), Ghat.History.unavailable(), "", null, published, active);
    }

    private static final class InMemoryGhatRepository implements GhatRepository {
        private final SharedGhatStore store;
        private InMemoryGhatRepository(SharedGhatStore store) { this.store = store; }
        @Override public List<Ghat> loadGhats(String idToken) { return List.copyOf(store.ghats); }
        @Override public List<Ghat> loadAdminGhats(String idToken) { return List.copyOf(store.ghats); }
        @Override public Ghat loadGhat(String ghatId, String idToken) throws IOException {
            return store.ghats.stream().filter(ghat -> ghat.id().equals(ghatId)).findFirst()
                    .orElseThrow(() -> new IOException("Missing Ghat: " + ghatId));
        }
        @Override public void saveGhat(Ghat ghat, String idToken) throws IOException {
            store.ghats.removeIf(existing -> existing.id().equals(ghat.id()));
            store.ghats.add(ghat);
        }
        @Override public void updateGhatImage(String ghatId, String imageUrl, String idToken) throws IOException {
            Ghat existing = loadGhat(ghatId, idToken);
            saveGhat(new Ghat(existing.id(), existing.name(), existing.area(), existing.description(), existing.latitude(), existing.longitude(),
                    existing.entryLatitude(), existing.entryLongitude(), imageUrl, existing.operationalStatus(), existing.crowdLevel(),
                    existing.estimatedWaitMinutes(), existing.bathingAvailable(), existing.walking(), existing.facilities(), existing.weather(),
                    existing.history(), existing.lastUpdated(), existing.operationalState(), existing.published(), existing.active()), idToken);
        }
    }

    private static final class SharedGhatStore { private final List<Ghat> ghats = new ArrayList<>(); }

    private static final class PermissionDeniedGhatRepository implements GhatRepository {
        private IOException denied() { return new IOException("Firestore GET failed: HTTP 403 PERMISSION_DENIED Missing or insufficient permissions."); }
        @Override public List<Ghat> loadGhats(String idToken) throws IOException { throw denied(); }
        @Override public List<Ghat> loadAdminGhats(String idToken) throws IOException { throw denied(); }
        @Override public Ghat loadGhat(String ghatId, String idToken) throws IOException { throw denied(); }
        @Override public void saveGhat(Ghat ghat, String idToken) throws IOException { throw denied(); }
        @Override public void updateGhatImage(String ghatId, String imageUrl, String idToken) throws IOException { throw denied(); }
    }
}
