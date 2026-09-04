package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.net.URL;
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
    private record ImageKey(String source, double width, double height) { }
    private final ConcurrentHashMap<ImageKey, Image> cache = new ConcurrentHashMap<>();
    private final Set<String> failedSources = ConcurrentHashMap.newKeySet();

    public ImageView createView(Ghat ghat, double width, double height) {
        String source = sourceFor(ghat);
        LOGGER.fine("Ghat image resolver source: " + source);
        Image image = imageFor(source, width, height);
        ImageView view = new ImageView(image == null ? fallback(width, height) : image);
        view.setFitWidth(width); view.setFitHeight(height); view.setPreserveRatio(true); view.setSmooth(true);
        view.getStyleClass().add("ghat-card-image");
        image.errorProperty().addListener((observable, oldValue, failed) -> { if (failed) view.setImage(fallback(width, height)); });
        return view;
    }

    public Image imageFor(String source) {
        return imageFor(source, 0, 0);
    }

    String sourceFor(Ghat ghat) {
        String source = ghat == null ? null : ghat.imageUrl();
        return source == null || source.isBlank() ? FALLBACK : source.trim();
    }

    public Image imageFor(String source, double width, double height) {
        String resolved = source == null || source.isBlank() ? FALLBACK : source.trim();
        if (failedSources.contains(resolved)) return fallback(width, height);
        ImageKey key = new ImageKey(resolved, width, height);
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

    private Image fallback(double width, double height) { return cache.computeIfAbsent(new ImageKey(FALLBACK, width, height), this::loadImage); }

    private String resolve(String source) {
        if (!source.startsWith("/")) return source;
        URL resource = GhatImageService.class.getResource(source);
        return resource == null ? source : resource.toExternalForm();
    }
}
