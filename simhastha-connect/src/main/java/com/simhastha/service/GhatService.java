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
                return userVisibleGhats(backendGhats);
            }
            catch (Exception exception) {
                logLoadFallback("published ghats", exception);
                return userVisibleGhats(List.of());
            }
        });
    }

    /** Admin reads the same ghats collection, including drafts and inactive records. */
    public CompletableFuture<List<Ghat>> loadAdminGhats(String idToken) {
        return CompletableFuture.supplyAsync(() -> {
            try { return resolvedGhats(gateway.loadAdminGhats(idToken)); }
            catch (Exception exception) {
                logLoadFallback("admin ghat records", exception);
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
                LOGGER.info("Ghat save accepted by Firestore: ghatId=" + ghat.id());
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
                Ghat reloaded = gateway.loadGhat(ghat.id(), idToken);
                if (!Objects.equals(imageUrl, reloaded.imageUrl())) {
                    throw new IllegalStateException("Saved Ghat image reference did not persist.");
                }
                return new GhatUpdateResult(reloaded, false);
            } catch (Exception exception) {
                if (permissionDenied(exception)) {
                    try {
                        Ghat local = new Ghat(ghat.id(), ghat.name(), ghat.area(), ghat.description(), ghat.latitude(),
                                ghat.longitude(), ghat.entryLatitude(), ghat.entryLongitude(), imageUrl,
                                ghat.operationalStatus(), ghat.crowdLevel(), ghat.estimatedWaitMinutes(),
                                ghat.bathingAvailable(), ghat.walking(), ghat.facilities(), ghat.weather(),
                                ghat.history(), ghat.lastUpdated(), ghat.operationalState(), ghat.published(), ghat.active());
                        localOverrides.save(local);
                        LOGGER.info("Saved legacy Ghat image locally because Firestore write is unavailable: " + localOverrides.file());
                        return new GhatUpdateResult(local, true);
                    } catch (Exception localFailure) { exception.addSuppressed(localFailure); }
                }
                throw new GhatLoadException(exception);
            }
        });
    }

    public CompletableFuture<GhatUpdateResult> updateGhatImage(Ghat ghat, String imageUrl, String imagePublicId, String idToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (imageUrl == null || imageUrl.isBlank()) throw new IllegalArgumentException("An image reference is required.");
                if (!imageUrl.startsWith("https://")) {
                    throw new IllegalArgumentException("Ghat image updates must use a persisted HTTPS Cloudinary URL.");
                }
                if (imagePublicId == null || imagePublicId.isBlank()) {
                    throw new IllegalArgumentException("A Cloudinary publicId is required for Ghat image updates.");
                }
                LOGGER.info("Ghat image Firestore update started: ghatId=" + ghat.id()
                        + ", secureUrlPresent=true, publicIdPresent=true");
                Ghat accepted = new Ghat(ghat.id(), ghat.name(), ghat.area(), ghat.description(), ghat.latitude(),
                        ghat.longitude(), ghat.entryLatitude(), ghat.entryLongitude(), imageUrl, imagePublicId,
                        ghat.operationalStatus(), ghat.crowdLevel(), ghat.estimatedWaitMinutes(),
                        ghat.bathingAvailable(), ghat.walking(), ghat.facilities(), ghat.weather(),
                        ghat.history(), String.valueOf(System.currentTimeMillis()), ghat.operationalState(),
                        true, true);
                gateway.saveGhat(accepted, idToken);
                LOGGER.info("Ghat image Firestore update accepted: ghatId=" + ghat.id()
                        + ", secureUrlPresent=true, publicIdPresent=true");
                return new GhatUpdateResult(accepted, false);
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Ghat image Firestore update failed: ghatId=" + ghat.id(), exception);
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
        Map<String, Ghat> localByIdOrName = new HashMap<>();
        for (Ghat local : localOverrides.load().values()) {
            localByIdOrName.put(key(local.id()), local);
            localByIdOrName.putIfAbsent(key(local.name()), local);
        }
        Map<String, Ghat> liveByIdOrName = new HashMap<>();
        for (Ghat live : liveRecords) {
            liveByIdOrName.put(key(live.id()), live);
            liveByIdOrName.putIfAbsent(key(live.name()), live);
        }

        List<Ghat> result = new ArrayList<>();
        for (Ghat catalogueGhat : catalogue.catalogue()) {
            Ghat local = localByIdOrName.remove(key(catalogueGhat.id()));
            if (local == null) local = localByIdOrName.remove(key(catalogueGhat.name()));
            Ghat base = local == null ? catalogueGhat : merge(catalogueGhat, local);
            Ghat live = liveByIdOrName.remove(key(catalogueGhat.id()));
            if (live == null) live = liveByIdOrName.remove(key(catalogueGhat.name()));
            result.add(live == null ? base : merge(base, live));
        }
        for (Ghat local : localByIdOrName.values()) {
            if (!result.stream().anyMatch(ghat -> sameGhat(ghat, local))) result.add(local);
        }
        for (Ghat live : liveRecords) {
            if (!result.stream().anyMatch(ghat -> sameGhat(ghat, live))) {
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
        return new Ghat(catalogueGhat.id(), first(live.name(), catalogueGhat.name()), first(live.area(), catalogueGhat.area()),
                first(live.description(), catalogueGhat.description()), value(live.latitude(), catalogueGhat.latitude()),
                value(live.longitude(), catalogueGhat.longitude()), value(live.entryLatitude(), catalogueGhat.entryLatitude()),
                value(live.entryLongitude(), catalogueGhat.entryLongitude()), first(live.imageUrl(), catalogueGhat.imageUrl()),
                first(live.imagePublicId(), catalogueGhat.imagePublicId()),
                operationalStatus(live, catalogueGhat), crowdLevel(live, catalogueGhat),
                value(live.estimatedWaitMinutes(), catalogueGhat.estimatedWaitMinutes()),
                hasLiveBathing(live) ? live.bathingAvailable() : catalogueGhat.bathingAvailable(),
                hasWalkingDetails(live) ? live.walking() : catalogueGhat.walking(),
                live.facilities().isEmpty() ? catalogueGhat.facilities() : live.facilities(),
                live.weather().available() ? live.weather() : catalogueGhat.weather(),
                live.history().available() ? live.history() : catalogueGhat.history(),
                first(live.lastUpdated(), catalogueGhat.lastUpdated()), operationalState(live, catalogueGhat),
                live.published(), live.active());
    }

    private void logLoadFallback(String label, Exception exception) {
        if (permissionDenied(exception)) {
            LOGGER.info("Firestore access denied while loading " + label + "; using local/catalogue fallback.");
        } else if (timeout(exception)) {
            LOGGER.info("Firestore timed out while loading " + label + "; using local/catalogue fallback.");
        } else {
            LOGGER.log(Level.WARNING, "Unable to load " + label + " from Firestore", exception);
        }
    }

    private static String first(String preferred, String fallback) { return preferred == null || preferred.isBlank() ? fallback : preferred; }
    private static <T> T value(T preferred, T fallback) { return preferred == null ? fallback : preferred; }
    private static String key(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static Ghat.OperationalStatus operationalStatus(Ghat live, Ghat fallback) {
        return live.operationalStatus() == Ghat.OperationalStatus.INFORMATION_ONLY ? fallback.operationalStatus() : live.operationalStatus();
    }
    private static Ghat.CrowdLevel crowdLevel(Ghat live, Ghat fallback) {
        return live.crowdLevel() == Ghat.CrowdLevel.UNKNOWN ? fallback.crowdLevel() : live.crowdLevel();
    }
    private static boolean hasLiveBathing(Ghat live) {
        return live.operationalState().bathingStatus() != com.simhastha.model.GhatOperationalState.BathingStatus.UNAVAILABLE;
    }
    private static boolean hasWalkingDetails(Ghat ghat) {
        return ghat.walking().approximateSteps() != null || ghat.walking().distanceMeters() != null
                || ghat.walking().seniorFriendly() || ghat.walking().wheelchairAccessible();
    }
    private static com.simhastha.model.GhatOperationalState operationalState(Ghat live, Ghat fallback) {
        // Facility statuses are independently live/admin-managed; do not discard
        // them simply because bathing status has not changed.
        return hasLiveBathing(live) || !live.operationalState().facilities().isEmpty() || !live.operationalState().lastUpdated().isBlank()
                ? live.operationalState() : fallback.operationalState();
    }
    private static boolean sameGhat(Ghat first, Ghat second) {
        return first != null && second != null
                && ((!key(first.id()).isBlank() && key(first.id()).equals(key(second.id())))
                || (!key(first.name()).isBlank() && key(first.name()).equals(key(second.name()))));
    }
    private static boolean permissionDenied(Exception exception) {
        String message = String.valueOf(exception.getMessage());
        Throwable cause = exception.getCause();
        while (cause != null) { message += " " + cause.getMessage(); cause = cause.getCause(); }
        return message.contains("HTTP 401") || message.contains("HTTP 403")
                || message.contains("PERMISSION_DENIED")
                || message.contains("Missing or insufficient permissions");
    }
    private static boolean timeout(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof java.net.http.HttpTimeoutException) return true;
            String message = String.valueOf(current.getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains("timed out") || message.contains("timeout")) return true;
            current = current.getCause();
        }
        return false;
    }
    public static final class GhatLoadException extends RuntimeException { public GhatLoadException(Throwable cause) { super(cause); } }
    public record GhatUpdateResult(Ghat ghat, boolean locallySaved) { }
}
