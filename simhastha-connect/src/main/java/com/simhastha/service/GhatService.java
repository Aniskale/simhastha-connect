package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GhatService {
    private static final Logger LOGGER = Logger.getLogger(GhatService.class.getName());
    private final GhatRepository gateway;
    private final GhatCatalogueService catalogue = new GhatCatalogueService();
    private final GhatImageStorageService imageStorage;
    private final LocalGhatOverrideStore localOverrides;
    public GhatService(GhatRepository gateway) { this(gateway, new GhatImageStorageService(), new LocalGhatOverrideStore()); }
    GhatService(GhatRepository gateway, GhatImageStorageService imageStorage) {
        this(gateway, imageStorage, new LocalGhatOverrideStore());
    }
    GhatService(GhatRepository gateway, GhatImageStorageService imageStorage, LocalGhatOverrideStore localOverrides) {
        this.gateway = gateway;
        this.imageStorage = imageStorage;
        this.localOverrides = localOverrides;
    }
    public CompletableFuture<List<Ghat>> loadGhats(String idToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Ghat> backendGhats = gateway.loadGhats(idToken);
                backendGhats.forEach(ghat -> System.out.println("BACKEND RELOADED IMAGE = " + ghat.id() + " -> " + ghat.imageUrl()));
                List<Ghat> userGhats = userVisibleGhats(backendGhats);
                userGhats.forEach(ghat -> System.out.println("USER SERVICE IMAGE = " + ghat.id() + " -> " + ghat.imageUrl()));
                return userGhats;
            }
            catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Unable to load published ghats from Firestore", exception);
                return userVisibleGhats(List.of());
            }
        });
    }

    /** Admin reads the same ghats collection, including drafts and inactive records. */
    public CompletableFuture<List<Ghat>> loadAdminGhats(String idToken) {
        return CompletableFuture.supplyAsync(() -> {
            try { return resolvedGhats(gateway.loadAdminGhats(idToken)); }
            catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Unable to load admin ghat records from Firestore", exception);
                return resolvedGhats(List.of());
            }
        });
    }

    /** Saving is authoritative only when the shared backend accepts the write. */
    public CompletableFuture<Void> saveGhat(Ghat ghat, String idToken) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (ghat.imageUrl().startsWith("file:") && !imageStorage.isManagedReference(ghat.imageUrl())) {
                    throw new IllegalArgumentException("A local image must be copied into managed Ghat media before it can be saved.");
                }
                gateway.saveGhat(ghat, idToken);
                System.out.println("ADMIN SAVED GHAT ID = " + ghat.id());
                System.out.println("ADMIN SAVED IMAGE = " + ghat.imageUrl());
                Ghat reloaded = gateway.loadGhat(ghat.id(), idToken);
                System.out.println("BACKEND RELOADED IMAGE = " + reloaded.id() + " -> " + reloaded.imageUrl());
                if (!Objects.equals(ghat.imageUrl(), reloaded.imageUrl())) {
                    throw new IllegalStateException("Saved Ghat image reference did not persist.");
                }
            }
            catch (Exception exception) {
                if (permissionDenied(exception)) {
                    try { localOverrides.save(ghat); return; }
                    catch (Exception localFailure) { exception.addSuppressed(localFailure); }
                }
                throw new GhatLoadException(exception);
            }
        });
    }

    /** Persists only the image reference and verifies the same document before returning it to the Admin UI. */
    public CompletableFuture<GhatUpdateResult> updateGhatImage(Ghat ghat, String imageUrl, String idToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (imageUrl == null || imageUrl.isBlank()) throw new IllegalArgumentException("An image reference is required.");
                if (imageUrl.startsWith("file:") && !imageStorage.isManagedReference(imageUrl)) {
                    throw new IllegalArgumentException("A local image must be copied into managed Ghat media before it can be saved.");
                }
                gateway.updateGhatImage(ghat.id(), imageUrl, idToken);
                System.out.println("ADMIN SAVED GHAT ID = " + ghat.id());
                System.out.println("ADMIN SAVED IMAGE = " + imageUrl);
                Ghat reloaded = gateway.loadGhat(ghat.id(), idToken);
                System.out.println("BACKEND RELOADED IMAGE = " + reloaded.id() + " -> " + reloaded.imageUrl());
                if (!Objects.equals(imageUrl, reloaded.imageUrl())) {
                    throw new IllegalStateException("Saved Ghat image reference did not persist.");
                }
                return new GhatUpdateResult(reloaded, false);
            } catch (Exception exception) {
                if (permissionDenied(exception)) {
                    try {
                        Ghat local = withImage(ghat, imageUrl);
                        localOverrides.save(local);
                        System.out.println("LOCAL FALLBACK RESULT = saved " + localOverrides.file());
                        return new GhatUpdateResult(local, true);
                    } catch (Exception localFailure) { exception.addSuppressed(localFailure); }
                }
                throw new GhatLoadException(exception);
            }
        });
    }

    /**
     * Keeps the verified development catalogue visible while allowing a future
     * admin record to replace its live, operational fields.  This is intentionally
     * kept out of the JavaFX layer so every consumer receives the same data set.
     */
    List<Ghat> resolvedGhats(List<Ghat> remote) {
        List<Ghat> liveRecords = remote == null ? List.of() : remote;
        Map<String, Ghat> localById = new HashMap<>(localOverrides.load());
        Map<String, Ghat> liveByIdOrName = new HashMap<>();
        for (Ghat live : liveRecords) {
            liveByIdOrName.put(key(live.id()), live);
            liveByIdOrName.putIfAbsent(key(live.name()), live);
        }

        List<Ghat> result = new ArrayList<>();
        for (Ghat catalogueGhat : catalogue.catalogue()) {
            Ghat local = localById.remove(catalogueGhat.id());
            Ghat base = local == null ? catalogueGhat : merge(catalogueGhat, local);
            Ghat live = liveByIdOrName.remove(key(catalogueGhat.id()));
            if (live == null) live = liveByIdOrName.remove(key(catalogueGhat.name()));
            result.add(live == null ? base : merge(base, live));
        }
        for (Ghat local : localById.values()) {
            if (!result.stream().anyMatch(ghat -> ghat.id().equals(local.id()))) result.add(local);
        }
        for (Ghat live : liveRecords) {
            if (!result.stream().anyMatch(ghat -> ghat.id().equals(live.id()) || key(ghat.name()).equals(key(live.name())))) {
                result.add(live);
            }
        }
        return result.stream().sorted(GhatCrowdComparator.LIVE_CROWD_ORDER).toList();
    }

    /** The Admin can see drafts; pilgrims only receive published, active Ghat records. */
    List<Ghat> userVisibleGhats(List<Ghat> remote) {
        return resolvedGhats(remote).stream().filter(ghat -> ghat.published() && ghat.active()).toList();
    }

    private Ghat merge(Ghat catalogueGhat, Ghat live) {
        Ghat merged = new Ghat(catalogueGhat.id(), first(live.name(), catalogueGhat.name()), first(live.area(), catalogueGhat.area()),
                first(live.description(), catalogueGhat.description()), value(live.latitude(), catalogueGhat.latitude()),
                value(live.longitude(), catalogueGhat.longitude()), value(live.entryLatitude(), catalogueGhat.entryLatitude()),
                value(live.entryLongitude(), catalogueGhat.entryLongitude()), first(live.imageUrl(), catalogueGhat.imageUrl()),
                live.operationalStatus(), live.crowdLevel(), live.estimatedWaitMinutes(), live.bathingAvailable(),
                live.walking(), live.facilities().isEmpty() ? catalogueGhat.facilities() : live.facilities(),
                live.weather().available() ? live.weather() : catalogueGhat.weather(),
                live.history().available() ? live.history() : catalogueGhat.history(),
                first(live.lastUpdated(), catalogueGhat.lastUpdated()), live.operationalState(), live.published(), live.active());
        System.out.println("MERGED USER IMAGE = " + merged.id() + " -> " + merged.imageUrl());
        return merged;
    }

    private static String first(String preferred, String fallback) { return preferred == null || preferred.isBlank() ? fallback : preferred; }
    private static <T> T value(T preferred, T fallback) { return preferred == null ? fallback : preferred; }
    private static String key(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static boolean permissionDenied(Exception exception) {
        String message = String.valueOf(exception.getMessage());
        Throwable cause = exception.getCause();
        while (cause != null) { message += " " + cause.getMessage(); cause = cause.getCause(); }
        return message.contains("HTTP 403") || message.contains("PERMISSION_DENIED") || message.contains("Missing or insufficient permissions");
    }
    private static Ghat withImage(Ghat ghat, String imageUrl) {
        return new Ghat(ghat.id(), ghat.name(), ghat.area(), ghat.description(), ghat.latitude(), ghat.longitude(), ghat.entryLatitude(), ghat.entryLongitude(), imageUrl,
                ghat.operationalStatus(), ghat.crowdLevel(), ghat.estimatedWaitMinutes(), ghat.bathingAvailable(), ghat.walking(), ghat.facilities(), ghat.weather(),
                ghat.history(), ghat.lastUpdated(), ghat.operationalState(), ghat.published(), ghat.active());
    }
    public static final class GhatLoadException extends RuntimeException { public GhatLoadException(Throwable cause) { super(cause); } }
    public record GhatUpdateResult(Ghat ghat, boolean locallySaved) { }
}
