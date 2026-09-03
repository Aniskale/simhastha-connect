package com.simhastha.view;

import java.net.URL;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/** Embedded Leaflet picker for Admin emergency-service coordinates. */
public final class EmergencyLocationSelector {
    private static final double DEFAULT_LATITUDE = 20.0097;
    private static final double DEFAULT_LONGITUDE = 73.7920;

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private final StackPane root = new StackPane(webView);
    private final Label loading = new Label("Loading Nashik location map...");
    private final BiConsumer<Double, Double> selectionHandler;
    private boolean ready;
    private double latitude = DEFAULT_LATITUDE;
    private double longitude = DEFAULT_LONGITUDE;

    public EmergencyLocationSelector(BiConsumer<Double, Double> selectionHandler) {
        this.selectionHandler = selectionHandler;
        root.getStyleClass().add("admin-emergency-location-map");
        root.setMinHeight(330);
        StackPane.setAlignment(loading, Pos.CENTER);
        root.getChildren().add(loading);
        webView.widthProperty().addListener((obs, oldValue, newValue) -> invalidateSize());
        webView.heightProperty().addListener((obs, oldValue, newValue) -> invalidateSize());
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) initializeBridge();
            else if (state == Worker.State.FAILED) showFailure();
        });
        load();
    }

    public Node node() {
        return root;
    }

    public void setInitialLocation(double latitude, double longitude) {
        if (valid(latitude, longitude)) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        applyLocation();
    }

    private void load() {
        try {
            URL resource = EmergencyLocationSelector.class.getResource("/maps/emergency-location-selector.html");
            System.out.println("EMERGENCY_LOCATION_SELECTOR resourceFound=" + (resource != null) + " resourceUrl=" + resource);
            if (resource == null) {
                showFailure();
                return;
            }
            engine.load(resource.toExternalForm());
        } catch (RuntimeException exception) {
            System.err.println("EMERGENCY_LOCATION_SELECTOR stage=LOAD error=" + exception.getClass().getSimpleName());
            showFailure();
        }
    }

    private void initializeBridge() {
        try {
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("emergencyLocationSelectorBridge", new JavaBridge());
            ready = true;
            loading.setVisible(false);
            loading.setManaged(false);
            applyLocation();
            invalidateSize();
        } catch (RuntimeException exception) {
            System.err.println("EMERGENCY_LOCATION_SELECTOR stage=INIT error=" + exception.getClass().getSimpleName());
            showFailure();
        }
    }

    private void applyLocation() {
        if (!ready) return;
        try {
            engine.executeScript("window.emergencyLocationSelector && window.emergencyLocationSelector.setLocation("
                    + latitude + "," + longitude + ",true);");
        } catch (RuntimeException exception) {
            System.err.println("EMERGENCY_LOCATION_SELECTOR stage=SET_LOCATION error=" + exception.getClass().getSimpleName());
        }
    }

    private void invalidateSize() {
        Platform.runLater(() -> {
            if (!ready || webView.getWidth() <= 1 || webView.getHeight() <= 1) return;
            try {
                engine.executeScript("window.invalidateEmergencyLocationSelectorSize && window.invalidateEmergencyLocationSelectorSize();");
            } catch (RuntimeException ignored) {
                // The selector remains usable after the next layout pass.
            }
        });
    }

    private void showFailure() {
        ready = false;
        loading.setText("Nashik location map unavailable. Enter coordinates manually.");
    }

    private static boolean valid(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && !(latitude == 0D && longitude == 0D)
                && latitude >= -90D && latitude <= 90D
                && longitude >= -180D && longitude <= 180D;
    }

    public final class JavaBridge {
        public void locationSelected(double newLatitude, double newLongitude) {
            if (!valid(newLatitude, newLongitude)) return;
            latitude = newLatitude;
            longitude = newLongitude;
            if (Platform.isFxApplicationThread()) {
                selectionHandler.accept(newLatitude, newLongitude);
            } else {
                Platform.runLater(() -> selectionHandler.accept(newLatitude, newLongitude));
            }
        }
    }
}
