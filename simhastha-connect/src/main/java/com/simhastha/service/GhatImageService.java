package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.logging.Logger;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

/** One resolver for local resources and future Cloudinary URLs; loading stays off the JavaFX UI thread. */
public final class GhatImageService {
    public static final String FALLBACK = "/images/godavari_kumbh.jpg";
    private static final Logger LOGGER = Logger.getLogger(GhatImageService.class.getName());
    private static final Map<String, String> CATALOGUE_IMAGES = Map.of(
            "ramkund", "/images/ramkund_sunrise.jpg",
            "tapovan", "/images/trimbakeshwar.jpg",
            "kushavart", "/images/trimbakeshwar.jpg");
    private record ImageKey(String ghatId, String source, double width, double height) { }
    /** Immutable identity carried by one card's ImageView for async safety checks. */
    private record CardImageIdentity(String ghatId, String source) { }
    private final ConcurrentHashMap<ImageKey, Image> cache = new ConcurrentHashMap<>();
    private final Set<String> failedSources = ConcurrentHashMap.newKeySet();

    public ImageView createView(Ghat ghat, double width, double height) {
        String source = sourceFor(ghat);
        String ghatId = ghat == null ? "unknown-ghat" : ghat.id();
        LOGGER.fine(() -> "Ghat image resolver: ghatId=" + ghatId + ", source=" + source
                + ", cacheKey=" + ghatId + "|" + source);
        Image image = imageFor(ghatId, source, width, height);
        ImageView view = new ImageView(image == null ? fallback(width, height) : image);
        CardImageIdentity identity = new CardImageIdentity(ghatId, source);
        view.setUserData(identity);
        view.setFitWidth(width); view.setFitHeight(height); view.setPreserveRatio(true); view.setSmooth(true);
        view.getStyleClass().add("ghat-card-image");
        LOGGER.info(() -> "GHAT IMAGE APPLY: ghatId=" + ghatId + ", ghatName="
                + (ghat == null ? "Unknown" : ghat.name()) + ", imageSource=" + source
                + ", stage=INITIAL, timestamp=" + java.time.Instant.now());
        image.errorProperty().addListener((observable, oldValue, failed) -> {
            if (!failed) return;
            // An asynchronous completion is allowed to touch only the ImageView
            // created for this exact Ghat and source.  A rebuilt card has a new
            // view/image; its old callback is ignored rather than replacing a
            // newer card's image with a fallback.
            if (!identity.equals(view.getUserData()) || view.getImage() != image) {
                LOGGER.info(() -> "GHAT IMAGE APPLY: ghatId=" + ghatId + ", ghatName="
                        + (ghat == null ? "Unknown" : ghat.name()) + ", imageSource=" + source
                        + ", stage=STALE_CALLBACK_IGNORED, timestamp=" + java.time.Instant.now());
                return;
            }
            Image fallback = fallback(width, height);
            view.setImage(fallback);
            LOGGER.info(() -> "GHAT IMAGE APPLY: ghatId=" + ghatId + ", ghatName="
                    + (ghat == null ? "Unknown" : ghat.name()) + ", imageSource=" + FALLBACK
                    + ", stage=OWN_SOURCE_FAILED_FALLBACK, timestamp=" + java.time.Instant.now());
        });
        return view;
    }

    public Image imageFor(String source) {
        return imageFor(source, 0, 0);
    }

    /**
     * Resolves the single approved image reference for a Ghat.  This is public so
     * the card cache can include the resolved reference in its identity; it does
     * not load an image or expose any mutable cache state.
     */
    public String sourceFor(Ghat ghat) {
        String source = ghat == null ? "" : ghat.imageUrl().trim();
        String resolved = !source.isBlank() && permittedGhatImage(source)
                ? source : catalogueFallbackFor(ghat == null ? "" : ghat.id());
        if (!source.isBlank() && !source.equals(resolved)) {
            LOGGER.warning("Rejected unrelated image source for Ghat " + ghat.id() + ": " + source);
        }
        LOGGER.fine(() -> "GHAT ID=" + (ghat == null ? "unknown-ghat" : ghat.id())
                + ", GHAT NAME=" + (ghat == null ? "Unknown" : ghat.name())
                + ", GHAT IMAGE REFERENCE=" + source
                + ", FINAL IMAGE SOURCE=" + resolved
                + ", CACHE KEY=" + (ghat == null ? "unknown-ghat" : ghat.id()) + "|" + resolved);
        return resolved;
    }

    public Image imageFor(String source, double width, double height) {
        return imageFor("generic-ghat", source, width, height);
    }

    private Image imageFor(String ghatId, String source, double width, double height) {
        String resolved = source == null || source.isBlank() ? FALLBACK : source.trim();
        if (failedSources.contains(resolved)) return fallback(width, height);
        ImageKey key = new ImageKey(ghatId, resolved, width, height);
        return cache.computeIfAbsent(key, this::loadImage);
    }

    /** Clears cached entries for an image source after Admin replaces it. */
    public void invalidate(String source) {
        if (source == null || source.isBlank()) return;
        String normalized = source.trim();
        cache.keySet().removeIf(key -> key.source().equals(normalized));
        failedSources.remove(normalized);
    }

    private Image loadImage(ImageKey key) {
        try {
            Image image = new Image(resolve(key.source()), key.width(), key.height(), true, true, true);
            image.errorProperty().addListener((observable, oldValue, failed) -> {
                if (failed && failedSources.add(key.source())) LOGGER.warning("Using fallback image after failed Ghat image load: " + key.source());
            });
            return image;
        } catch (RuntimeException exception) {
            if (failedSources.add(key.source())) LOGGER.warning("Using fallback image after invalid Ghat image source: " + key.source());
            return FALLBACK.equals(key.source()) ? new WritableImage(1, 1) : fallback(key.width(), key.height());
        }
    }

    private Image fallback(double width, double height) {
        return cache.computeIfAbsent(new ImageKey("generic-ghat", FALLBACK, width, height), this::loadImage);
    }

    private static String catalogueFallbackFor(String ghatId) {
        return CATALOGUE_IMAGES.getOrDefault(ghatId == null ? "" : ghatId.trim().toLowerCase(Locale.ROOT), FALLBACK);
    }

    private static boolean permittedGhatImage(String source) {
        String normalized = source.toLowerCase(Locale.ROOT);
        // A classpath image must be one of this module's explicit Ghat resources.
        // Do not let a generic /images lookup accidentally resolve an asset from
        // Transport, Business, Packages, or another dashboard module.
        if (normalized.startsWith("/")) {
            return FALLBACK.equals(source)
                    || CATALOGUE_IMAGES.containsValue(source)
                    || normalized.startsWith("/images/ghats/");
        }
        // Managed local files and durable remote Admin media remain valid.  The
        // names below reject known cross-module references before any ImageView
        // or cache entry can be created.
        return !normalized.contains("redbus") && !normalized.contains("transport") && !normalized.contains("bus-")
                && !normalized.contains("business") && !normalized.contains("package") && !normalized.contains("puja");
    }

    private String resolve(String source) {
        if (!source.startsWith("/")) return source;
        URL resource = GhatImageService.class.getResource(source);
        return resource == null ? source : resource.toExternalForm();
    }
}
