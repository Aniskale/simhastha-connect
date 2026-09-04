package com.simhastha.view;

import com.simhastha.controller.SimhasthaMapController;
import com.simhastha.model.MapLocation;
import com.simhastha.model.MapLocationType;
import com.simhastha.util.AppSession;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class SimhasthaMapPage {
    private static final Logger LOGGER = Logger.getLogger(SimhasthaMapPage.class.getName());
    private final SimhasthaMapController controller = new SimhasthaMapController();
    private final String selectedBusinessId;
    private final boolean routeMode;
    private final Runnable onBack;
    private final Consumer<String> onViewBusiness;
    private final StackPane mapSlot = new StackPane();
    private final VBox infoCard = new VBox(8);
    private final Label status = new Label("Loading map locations...");
    private List<MapLocation> currentLocations = List.of();

    public SimhasthaMapPage(String selectedBusinessId, boolean routeMode, Runnable onBack, Consumer<String> onViewBusiness) {
        this.selectedBusinessId = selectedBusinessId == null ? "" : selectedBusinessId;
        this.routeMode = routeMode;
        this.onBack = onBack;
        this.onViewBusiness = onViewBusiness;
    }

    public Node createContent() {
        Button back = new Button("Back");
        back.getStyleClass().add("marketplace-secondary-button");
        back.setOnAction(event -> { if (onBack != null) onBack.run(); });
        VBox title = new VBox(3, label("Simhastha Map", "business-welcome"),
                label("Verified business markers are synced from approved owner records.", "business-header-date"));
        HBox header = new HBox(12, title, spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);

        FlowPane layers = new FlowPane(8, 8, layer("All", null), layer("Verified Businesses", MapLocationType.BUSINESS));
        mapSlot.getStyleClass().add("simhastha-map-canvas");
        mapSlot.setMinHeight(300);
        mapSlot.setPrefHeight(420);
        mapSlot.getChildren().setAll(label("Loading map...", "business-row-detail"));
        infoCard.getStyleClass().add("simhastha-map-info-card");
        infoCard.getChildren().setAll(label("Select a marker", "business-section-title"),
                label("Business details appear here when a verified marker is selected.", "business-row-detail"));
        HBox body = new HBox(14, mapSlot, infoCard);
        HBox.setHgrow(mapSlot, Priority.ALWAYS);
        infoCard.setPrefWidth(260);
        infoCard.setMinWidth(220);
        status.getStyleClass().add("business-row-detail");
        VBox content = new VBox(14, header, layers, body, status);
        content.getStyleClass().add("business-dashboard-content");
        content.setPadding(new Insets(16, 20, 24, 20));
        loadLocations(null);
        return content;
    }

    private Button layer(String text, MapLocationType type) {
        Button button = new Button(text);
        button.getStyleClass().add("marketplace-filter-chip");
        button.setOnAction(event -> loadLocations(type));
        return button;
    }

    private void loadLocations(MapLocationType layer) {
        status.setText("Loading map...");
        AppSession.User user = AppSession.currentUser();
        String token = user == null ? "" : user.idToken();
        controller.loadLocations(token, layer).whenComplete((locations, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                status.setText("Map locations could not be loaded.");
                return;
            }
            renderLocations(locations);
        }));
    }

    private void renderLocations(List<MapLocation> locations) {
        currentLocations = locations == null ? List.of() : locations;
        mapSlot.getChildren().setAll(MapWebViewFactory.businessMarkerMap(currentLocations, selectedBusinessId, routeMode,
                markerActions(),
                () -> status.setText(routeMode ? "Destination loaded. Use Start Route on the map."
                        : currentLocations.size() + " verified business marker(s) loaded."),
                () -> showNativeMapFallback()));
        currentLocations.stream()
                .filter(location -> selectedBusinessId.equals(location.businessId()))
                .findFirst()
                .ifPresent(this::showLocation);
    }

    private void showNativeMapFallback() {
        mapSlot.getChildren().setAll(MapWebViewFactory.nativeBusinessMap(currentLocations, selectedBusinessId));
        status.setText("Map loaded. Route needs a live location request.");
    }

    private MapWebViewFactory.MarkerActionHandler markerActions() {
        return new MapWebViewFactory.MarkerActionHandler() {
            public void markerSelected(String businessId) { findLocation(businessId).ifPresent(SimhasthaMapPage.this::showLocation); }
            public void viewBusiness(String businessId) { if (onViewBusiness != null) onViewBusiness.accept(businessId); }
            public void getRoute(String businessId) { findLocation(businessId).ifPresent(location -> { showLocation(location); openBrowserRoute(location); }); }
        };
    }

    private void openBrowserRoute(MapLocation location) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                status.setText("Route could not be opened on this device.");
                return;
            }
            String url = "https://www.google.com/maps/dir/?api=1&destination="
                    + location.latitude() + "," + location.longitude() + "&travelmode=driving";
            Desktop.getDesktop().browse(new URI(url));
            status.setText("Route opened in browser. Allow location access there for live directions.");
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Route browser could not be opened for business " + location.businessId(),
                    exception);
            status.setText("Route could not be opened. Business marker is still visible.");
        }
    }

    private void showLocation(MapLocation location) {
        Button view = new Button("View Business");
        view.getStyleClass().add("marketplace-primary-action");
        view.setOnAction(event -> { if (onViewBusiness != null) onViewBusiness.accept(location.businessId()); });
        infoCard.getChildren().setAll(label(location.name(), "business-section-title"),
                label("Verified Business", "business-status-badge"),
                label(location.category(), "marketplace-price"),
                label(locationText(location), "business-row-detail"),
                new HBox(8, view));
    }

    private java.util.Optional<MapLocation> findLocation(String businessId) {
        return currentLocations.stream().filter(location -> businessId.equals(location.businessId())).findFirst();
    }

    private String locationText(MapLocation location) {
        String city = location.city() == null || location.city().isBlank() ? "" : ", " + location.city();
        return location.area() == null || location.area().isBlank()
                ? text(location.address(), "Address not provided")
                : location.area() + city + (location.address().isBlank() ? "" : " | " + location.address());
    }

    private Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().addAll(style.split(" "));
        label.setWrapText(true);
        return label;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
