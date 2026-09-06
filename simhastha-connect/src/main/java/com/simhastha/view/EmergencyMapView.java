package com.simhastha.view;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import com.simhastha.model.EmergencyAlert;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/** A single reusable WebView instance for the in-page Nashik Emergency map. */
public final class EmergencyMapView {
    private final WebView webView;
    private final StackPane root = new StackPane();
    private final StackPane loading = new StackPane(new Label("Loading Nashik Emergency Map..."));
    private final StackPane failed = new StackPane();
    private final WebEngine engine;
    private final Consumer<String> markerSelectionHandler;
    private boolean mapReady;
    private String pendingMarkers = "[]";
    private String pendingSelectedId = "";
    private String focusId = "";
    private boolean routeActive;
    private double routeDestinationLatitude;
    private double routeDestinationLongitude;
    private AlertRequest pendingAlert;
    private boolean resizeInvalidationQueued;
    private double lastReportedWidth = -1;
    private double lastReportedHeight = -1;

    EmergencyMapView(Consumer<String> markerSelectionHandler) {
        this.markerSelectionHandler = Objects.requireNonNull(markerSelectionHandler);
        root.getStyleClass().add("emergency-live-map");
        root.setMinHeight(360);
        loading.getStyleClass().add("emergency-map-loading");
        StackPane.setAlignment(loading, Pos.CENTER);
        root.getChildren().add(loading);
        buildFailureState();
        WebView candidateWebView;
        WebEngine candidateEngine;
        try {
            candidateWebView = new WebView();
            candidateEngine = candidateWebView.getEngine();
            root.getChildren().add(0, candidateWebView);
        } catch (Throwable error) {
            System.err.println("EMERGENCY_MAP_DIAGNOSTIC stage=CONSTRUCT error=" + error);
            candidateWebView = null;
            candidateEngine = null;
        }
        webView = candidateWebView;
        engine = candidateEngine;
        if (webView == null || engine == null) {
            showFailure("CONSTRUCT", null);
            return;
        }
        webView.widthProperty().addListener((observable, oldValue, newValue) -> scheduleResizeInvalidation());
        webView.heightProperty().addListener((observable, oldValue, newValue) -> scheduleResizeInvalidation());
        root.widthProperty().addListener((observable, oldValue, newValue) -> scheduleResizeInvalidation());
        root.heightProperty().addListener((observable, oldValue, newValue) -> scheduleResizeInvalidation());
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) initializeBridge();
            if (state == Worker.State.FAILED || state == Worker.State.CANCELLED) {
                showFailure("LOAD", engine.getLoadWorker().getException());
            }
        });
        loadMap();
    }

    Node node() { return root; }

    void detach() {
        if (root.getParent() instanceof Pane pane) pane.getChildren().remove(root);
    }

    /** Receives the complete already-filtered active service collection from the page. */
    void updateMarkers(String markersJson, String selectedId) {
        pendingMarkers = markersJson == null ? "[]" : markersJson;
        pendingSelectedId = selectedId == null ? "" : selectedId;
        applyPendingState();
    }

    void focusFacility(String serviceId) {
        focusId = serviceId == null ? "" : serviceId;
        applyPendingState();
    }

    void drawApproximateRoute(double destinationLatitude, double destinationLongitude) {
        routeActive = true;
        routeDestinationLatitude = destinationLatitude;
        routeDestinationLongitude = destinationLongitude;
        applyPendingState();
    }

    void clearRoute() {
        routeActive = false;
        applyPendingState();
    }

    void showEmergencyAlert(EmergencyAlert alert) {
        setEmergencyAlert(alert, false);
    }

    void focusEmergencyAlert(EmergencyAlert alert) {
        setEmergencyAlert(alert, true);
    }

    private void setEmergencyAlert(EmergencyAlert alert, boolean focus) {
        if (alert == null || !alert.hasLocation()) {
            clearEmergencyAlert();
            return;
        }
        pendingAlert = new AlertRequest(
                safe(alert.id()), safe(alert.title()), safe(alert.message()),
                alert.severity() == null ? "INFO" : alert.severity().name(),
                safe(alert.locationLabel()), alert.latitude(), alert.longitude(), focus);
        applyPendingState();
    }

    void clearEmergencyAlert() {
        pendingAlert = null;
        applyPendingState();
    }

    void centerOnDemoLocation() { execute("window.emergencyMap && window.emergencyMap.centerOnUser();"); }

    void requestSizeInvalidationAfterLayout() {
        scheduleResizeInvalidation();
        Platform.runLater(this::scheduleResizeInvalidation);
    }

    private void buildFailureState() {
        Label title = new Label("Nashik Emergency Map unavailable");
        title.getStyleClass().add("emergency-map-failure-title");
        Label detail = new Label("Check internet connection or retry.");
        detail.getStyleClass().add("emergency-map-failure-detail");
        Button retry = new Button("RETRY MAP");
        retry.getStyleClass().add("emergency-secondary-button");
        retry.setOnAction(event -> loadMap());
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(7, title, detail, retry);
        content.setAlignment(Pos.CENTER);
        failed.getChildren().setAll(content);
        failed.getStyleClass().add("emergency-map-failure");
        failed.setVisible(false);
        failed.setManaged(false);
        root.getChildren().add(failed);
    }

    private void loadMap() {
        try {
            mapReady = false;
            loading.setVisible(true);
            loading.setManaged(true);
            failed.setVisible(false);
            failed.setManaged(false);
            URL resource = EmergencyMapView.class.getResource("/maps/emergency-nashik-map.html");
            // IDE/debug launches may run stale target/classes without copying resources.
            // Resolve the checked-in resource as a safe local fallback before failing.
            if (resource == null) {
                Path[] candidates = {
                        Path.of("src", "main", "resources", "maps", "emergency-nashik-map.html"),
                        Path.of("simhastha-connect", "src", "main", "resources", "maps", "emergency-nashik-map.html")
                };
                for (Path candidate : candidates) {
                    if (Files.isRegularFile(candidate)) {
                        try {
                            resource = candidate.toAbsolutePath().toUri().toURL();
                            break;
                        } catch (java.net.MalformedURLException ignored) {
                            // Try the next known workspace location.
                        }
                    }
                }
            }
            System.out.println("EMERGENCY_MAP_DIAGNOSTIC resourceFound=" + (resource != null) + " resourceUrl=" + resource);
            if (resource == null) { showFailure("RESOURCE", null); return; }
            engine.load(resource.toExternalForm());
        } catch (RuntimeException error) {
            showFailure("LOAD", error);
        }
    }

    private void initializeBridge() {
        try {
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("emergencyBridge", new JavaBridge());
            mapReady = true;
            loading.setVisible(false);
            loading.setManaged(false);
            applyPendingState();
            scheduleResizeInvalidation();
        } catch (RuntimeException error) {
            showFailure("INIT", error);
        }
    }

    private void applyPendingState() {
        if (!mapReady) return;
        execute("window.emergencyMap && window.emergencyMap.setMarkers(" + jsQuoted(pendingMarkers) + ","
                + jsQuoted(pendingSelectedId) + ");");
        if (!focusId.isBlank()) execute("window.emergencyMap && window.emergencyMap.focusFacility(" + jsQuoted(focusId) + ");");
        if (routeActive) execute("window.emergencyMap && window.emergencyMap.drawRoute(" + routeDestinationLatitude + "," + routeDestinationLongitude + ");");
        else execute("window.emergencyMap && window.emergencyMap.clearRoute();");
        if (pendingAlert == null) {
            executeAlert("window.emergencyMap && window.emergencyMap.clearAlert();", "CLEAR");
        } else {
            String alertJson = "{\"id\":\"" + jsonEscape(pendingAlert.id())
                    + "\",\"title\":\"" + jsonEscape(pendingAlert.title())
                    + "\",\"message\":\"" + jsonEscape(pendingAlert.message())
                    + "\",\"severity\":\"" + jsonEscape(pendingAlert.severity())
                    + "\",\"locationLabel\":\"" + jsonEscape(pendingAlert.locationLabel())
                    + "\",\"latitude\":" + pendingAlert.latitude()
                    + ",\"longitude\":" + pendingAlert.longitude() + "}";
            executeAlert("window.emergencyMap && window.emergencyMap.showAlert(" + jsQuoted(alertJson) + ","
                    + pendingAlert.focus() + ");", pendingAlert.focus() ? "VIEW_ON_MAP" : "SHOW");
        }
    }

    private void execute(String script) {
        if (!mapReady) return;
        try { engine.executeScript(script); } catch (RuntimeException error) { showFailure("SCRIPT", error); }
    }

    private void executeAlert(String script, String action) {
        if (!mapReady) return;
        try {
            engine.executeScript(script);
            System.out.println("EMERGENCY_ALERT_MAP_DIAGNOSTIC action=" + action + " result=success");
        } catch (RuntimeException error) {
            // An alert marker is optional; it must not take down the existing map.
            System.err.println("EMERGENCY_ALERT_MAP_DIAGNOSTIC action=" + action
                    + " result=failed error=" + error.getClass().getSimpleName());
        }
    }

    private void scheduleResizeInvalidation() {
        if (resizeInvalidationQueued) return;
        resizeInvalidationQueued = true;
        Platform.runLater(() -> {
            resizeInvalidationQueued = false;
            if (!mapReady || webView == null || webView.getWidth() <= 1 || webView.getHeight() <= 1) return;
            webView.applyCss();
            webView.layout();
            execute("window.invalidateEmergencyMapSize && window.invalidateEmergencyMapSize();");
            if (Math.abs(webView.getWidth() - lastReportedWidth) >= 1 || Math.abs(webView.getHeight() - lastReportedHeight) >= 1) {
                lastReportedWidth = webView.getWidth();
                lastReportedHeight = webView.getHeight();
                System.out.println("EMERGENCY_MAP size=" + Math.round(lastReportedWidth) + "x" + Math.round(lastReportedHeight));
            }
        });
    }

    private void showFailure(String stage, Throwable error) {
        System.err.println("EMERGENCY_MAP_DIAGNOSTIC stage=" + stage + " error=" + (error == null ? "none" : error));
        mapReady = false;
        loading.setVisible(false);
        loading.setManaged(false);
        failed.setVisible(true);
        failed.setManaged(true);
    }

    private String jsQuoted(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "") + "'";
    }

    private String jsonEscape(String value) {
        return safe(value).replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private String safe(String value) { return value == null ? "" : value; }

    private record AlertRequest(String id, String title, String message, String severity,
            String locationLabel, double latitude, double longitude, boolean focus) { }

    public final class JavaBridge {
        public void markerSelected(String id) { markerSelectionHandler.accept(id); }
        public void mapDiagnostic(String message) { System.out.println(message); }
    }
}
