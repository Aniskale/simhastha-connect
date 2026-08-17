package com.simhastha.view;

import java.net.URL;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class ThemedBackgroundPane extends StackPane {

    private final Image lightImage;
    private final Image darkImage;
    private final ImageView backgroundImage;

    public ThemedBackgroundPane(Node content) {
        this(content, "/images/welcome-light.png", "/images/welcome-dark.png");
    }

    public ThemedBackgroundPane(Node content, String lightPath, String darkPath) {
        lightImage = loadImage(lightPath);
        darkImage = loadImage(darkPath);
        backgroundImage = new ImageView();
        backgroundImage.setPreserveRatio(true);
        backgroundImage.setSmooth(true);
        backgroundImage.fitWidthProperty().bind(widthProperty());
        backgroundImage.fitHeightProperty().bind(heightProperty());
        backgroundImage.getStyleClass().add("themed-page-background-image");

        Rectangle overlay = new Rectangle();
        overlay.widthProperty().bind(widthProperty());
        overlay.heightProperty().bind(heightProperty());
        overlay.getStyleClass().add("themed-page-overlay");

        getStyleClass().add("themed-page-root");
        getChildren().addAll(backgroundImage, overlay, content);

        ChangeListener<Number> viewportListener = (observable, oldValue, newValue) -> updateViewport();
        widthProperty().addListener(viewportListener);
        heightProperty().addListener(viewportListener);
        backgroundImage.imageProperty().addListener((observable, oldValue, newValue) -> updateViewport());
        ThemeManager.addListener(this::syncTheme);
        syncTheme();
    }

    private void syncTheme() {
        backgroundImage.setImage(ThemeManager.isDark() ? darkImage : lightImage);
        updateViewport();
    }

    private void updateViewport() {
        Image image = backgroundImage.getImage();
        double viewWidth = getWidth();
        double viewHeight = getHeight();

        if (image == null || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        double scale = Math.max(viewWidth / image.getWidth(), viewHeight / image.getHeight());
        double cropWidth = viewWidth / scale;
        double cropHeight = viewHeight / scale;
        double xBias = ThemeManager.isDark() ? 0.48 : 0.50;
        double yBias = ThemeManager.isDark() ? 0.42 : 0.40;
        double x = Math.max(0, (image.getWidth() - cropWidth) * xBias);
        double y = Math.max(0, (image.getHeight() - cropHeight) * yBias);
        backgroundImage.setViewport(new Rectangle2D(x, y, cropWidth, cropHeight));
    }

    private Image loadImage(String path) {
        URL imageUrl = getClass().getResource(path);
        return imageUrl == null ? null : new Image(imageUrl.toExternalForm());
    }
}
