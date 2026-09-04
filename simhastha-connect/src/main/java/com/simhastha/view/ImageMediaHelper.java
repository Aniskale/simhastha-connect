package com.simhastha.view;

import com.simhastha.service.CloudinaryService;
import java.io.File;
import java.net.URL;
import java.util.logging.Logger;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

final class ImageMediaHelper {
    private static final Logger LOGGER = Logger.getLogger(ImageMediaHelper.class.getName());
    static final String FALLBACK_IMAGE = "/images/godavari_kumbh.jpg";

    private ImageMediaHelper() {
    }

    static File chooseImage(Window owner, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image files", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        return chooser.showOpenDialog(owner);
    }

    static void validateImage(File file) {
        CloudinaryService.validateImage(file);
    }

    static ImageView imageView(String source, double width, double height) {
        ImageView view = new ImageView(loadImage(source, width, height));
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        view.getImage().errorProperty().addListener((observable, oldValue, failed) -> {
            if (failed) {
                view.setImage(loadImage(FALLBACK_IMAGE, width, height));
            }
        });
        return view;
    }

    static Image loadImage(String source, double width, double height) {
        String resolved = resolve(source == null || source.isBlank() ? FALLBACK_IMAGE : source.trim());
        try {
            return new Image(resolved, width, height, true, true, true);
        } catch (RuntimeException exception) {
            LOGGER.fine("Image source could not be loaded: " + source);
            return new Image(resolve(FALLBACK_IMAGE), width, height, true, true, true);
        }
    }

    static String resolve(String source) {
        if (source == null || source.isBlank()) {
            source = FALLBACK_IMAGE;
        }
        if (source.startsWith("/")) {
            URL resource = ImageMediaHelper.class.getResource(source);
            return resource == null ? source : resource.toExternalForm();
        }
        return source;
    }
}
