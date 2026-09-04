package com.simhastha.view;

import com.simhastha.model.OfficialHelpLocation;
import com.simhastha.service.GoogleMapsConfig;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import netscape.javascript.JSObject;
import java.util.function.BiConsumer;

/** Leaflet/OSM selection view. No location is committed until Confirm is clicked. */
public final class LostFoundMapView {
    private LostFoundMapView() { }
    public static final class SelectionBridge {
        private final java.util.function.BiConsumer<Double, Double> receiver;
        public SelectionBridge(java.util.function.BiConsumer<Double, Double> receiver) { this.receiver = receiver; }
        public void select(double latitude, double longitude) { javafx.application.Platform.runLater(() -> receiver.accept(latitude, longitude)); }
    }
    public static void showSelector(Window owner, String title, BiConsumer<Double, Double> confirmed) {
        showSelector(owner, title, Double.NaN, Double.NaN, confirmed);
    }

    /** Reusable in-app selector; an existing point remains provisional until Use This Location is clicked. */
    public static void showSelector(Window owner, String title, double initialLatitude, double initialLongitude,
            BiConsumer<Double, Double> confirmed) {
        Stage stage = new Stage(); stage.setTitle(title);
        if (owner != null) { stage.initOwner(owner); stage.initModality(Modality.APPLICATION_MODAL); }
        WebView view = new WebView(); view.setMinSize(760, 480); view.setPrefSize(960, 620); WebEngine engine = view.getEngine();
        boolean hasInitialPoint = validCoordinate(initialLatitude, initialLongitude);
        Label selected = new Label(hasInitialPoint ? coordinateLabel(initialLatitude, initialLongitude) : "Select a point on the map. Coordinates are not saved until you confirm."); selected.setWrapText(true);
        final double[] point = {hasInitialPoint ? initialLatitude : Double.NaN, hasInitialPoint ? initialLongitude : Double.NaN};
        Button cancel = new Button("Cancel"); cancel.setOnAction(e -> stage.close());
        Button confirm = new Button("Use This Location"); confirm.setDisable(!hasInitialPoint);
        confirm.setOnAction(e -> { confirmed.accept(point[0], point[1]); stage.close(); });
        GoogleMapsConfig googleMaps = GoogleMapsConfig.load();
        Button streetView = new Button("Street View");
        streetView.setDisable(!googleMaps.configured() || !hasInitialPoint);
        streetView.setVisible(googleMaps.configured());
        streetView.setManaged(googleMaps.configured());
        streetView.setOnAction(e -> {
            if (validCoordinate(point[0], point[1])) showStreetView(stage, googleMaps, point[0], point[1]);
        });
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox actions = new HBox(10, selected, spacer, streetView, cancel, confirm); actions.setPadding(new Insets(12)); actions.setMinHeight(58); actions.setStyle("-fx-background-color: #fffaf0; -fx-border-color: #d9b66f transparent transparent transparent;");
        VBox heading = new VBox(5, new Label("Select exact location on the street map")); heading.setPadding(new Insets(10));
        BorderPane root = new BorderPane(); root.setCenter(view); root.setTop(heading); root.setBottom(actions); root.setMinSize(760, 560);
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaSelection", new SelectionBridge((lat, lon) -> {
                    point[0] = lat; point[1] = lon;
                    selected.setText(coordinateLabel(lat, lon));
                    confirm.setDisable(false);
                    streetView.setDisable(!googleMaps.configured());
                }));
            } else if (state == Worker.State.FAILED) selected.setText("Map could not be loaded. Check your internet connection and try again.");
        });
        engine.loadContent(selectorHtml(initialLatitude, initialLongitude)); stage.setScene(new Scene(root, 980, 720)); stage.setMinWidth(760); stage.setMinHeight(600); stage.showAndWait();
    }

    /** Read-only official-help map; markers are supplied only from verified Firestore records. */
    public static void showHelpCenters(Window owner, List<OfficialHelpLocation> centers) {
        if (centers == null || centers.isEmpty()) return;
        Stage stage = new Stage(); stage.setTitle("Help Near Me");
        if (owner != null) { stage.initOwner(owner); stage.initModality(Modality.NONE); }
        WebView view = new WebView(); view.setMinSize(760, 480); view.setPrefSize(960, 620);
        Label selected = new Label("Select a verified help center marker to view its details."); selected.setWrapText(true);
        final OfficialHelpLocation[] active = {null};
        Button directions = new Button("Directions"); directions.setDisable(true);
        directions.setOnAction(event -> { if (active[0] != null) openDirections(active[0]); });
        Button close = new Button("Close"); close.setOnAction(event -> stage.close());
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox actions = new HBox(10, selected, spacer, directions, close); actions.setPadding(new Insets(12)); actions.setMinHeight(58); actions.setStyle("-fx-background-color: #fffaf0; -fx-border-color: #d9b66f transparent transparent transparent;");
        VBox heading = new VBox(5, new Label("Verified Pilgrim Help Centers"), new Label("Official active locations from Simhastha Connect.")); heading.setPadding(new Insets(10));
        BorderPane root = new BorderPane(); root.setCenter(view); root.setTop(heading); root.setBottom(actions); root.setMinSize(760, 560);
        view.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) view.getEngine().executeScript("window");
                window.setMember("javaHelpCenter", new HelpCenterBridge(centers, location -> {
                    active[0] = location;
                    selected.setText(location.name() + "\n" + location.address() + "\nServices: " + String.join(", ", location.services()));
                    directions.setDisable(false);
                }));
            } else if (state == Worker.State.FAILED) selected.setText("Map could not be loaded. Check your internet connection and try again.");
        });
        view.getEngine().loadContent(helpCenterHtml(centers));
        stage.setScene(new Scene(root, 980, 720)); stage.setMinWidth(760); stage.setMinHeight(600); stage.show();
    }

    public static final class HelpCenterBridge {
        private final List<OfficialHelpLocation> centers;
        private final java.util.function.Consumer<OfficialHelpLocation> selected;
        public HelpCenterBridge(List<OfficialHelpLocation> centers, java.util.function.Consumer<OfficialHelpLocation> selected) { this.centers = centers; this.selected = selected; }
        public void select(String id) { centers.stream().filter(center -> center.id().equals(id)).findFirst().ifPresent(center -> javafx.application.Platform.runLater(() -> selected.accept(center))); }
    }

    public static void openDirections(OfficialHelpLocation center) {
        try {
            if (!Desktop.isDesktopSupported()) return;
            String destination = URLEncoder.encode(center.latitude() + "," + center.longitude(), StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(URI.create("https://www.google.com/maps/dir/?api=1&destination=" + destination + "&travelmode=walking"));
        } catch (Exception ignored) { }
    }
    private static String selectorHtml(double initialLatitude, double initialLongitude) {
        boolean validInitial = validCoordinate(initialLatitude, initialLongitude);
        String marker = validInitial ? "let marker=L.marker([__LAT__,__LON__]).addTo(map);" : "let marker;";
        return """
        <!doctype html><html><head><meta charset='utf-8'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'><style>html,body,#map{height:100%;margin:0}</style></head>
        <body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>
        const map=L.map('map').setView([__LAT__,__LON__],__ZOOM__);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);__MARKER__
        map.on('click',e=>{if(marker)marker.setLatLng(e.latlng);else marker=L.marker(e.latlng).addTo(map);if(window.javaSelection)window.javaSelection.select(e.latlng.lat,e.latlng.lng);});
        </script></body></html>"""
                .replace("__LAT__", Double.toString(validInitial ? initialLatitude : 20.0059))
                .replace("__LON__", Double.toString(validInitial ? initialLongitude : 73.7903))
                .replace("__ZOOM__", validInitial ? "16" : "13")
                .replace("__MARKER__", marker.replace("__LAT__", Double.toString(initialLatitude)).replace("__LON__", Double.toString(initialLongitude)));
    }

    private static boolean validCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    private static String coordinateLabel(double latitude, double longitude) {
        return String.format("Selected location\nLatitude: %.6f\nLongitude: %.6f", latitude, longitude);
    }

    private static void showStreetView(Window owner, GoogleMapsConfig config, double latitude, double longitude) {
        if (!config.configured() || !validCoordinate(latitude, longitude)) return;
        Stage stage = new Stage(); stage.setTitle("Street View");
        if (owner != null) { stage.initOwner(owner); stage.initModality(Modality.NONE); }
        WebView view = new WebView();
        String key = URLEncoder.encode(config.apiKey(), StandardCharsets.UTF_8);
        String html = """
                <!doctype html><html><head><meta charset='utf-8'><style>html,body,#pano{height:100%;margin:0}</style></head>
                <body><div id='pano'></div><script>
                function init(){new google.maps.StreetViewPanorama(document.getElementById('pano'),{position:{lat:__LAT__,lng:__LON__},pov:{heading:0,pitch:0}});}
                </script><script async src='https://maps.googleapis.com/maps/api/js?key=__KEY__&callback=init'></script></body></html>
                """.replace("__LAT__", Double.toString(latitude)).replace("__LON__", Double.toString(longitude)).replace("__KEY__", key);
        view.getEngine().loadContent(html);
        stage.setScene(new Scene(new BorderPane(view), 900, 620));
        stage.show();
    }

    private static String helpCenterHtml(List<OfficialHelpLocation> centers) {
        OfficialHelpLocation first = centers.get(0);
        StringBuilder markers = new StringBuilder();
        for (OfficialHelpLocation center : centers) {
            markers.append("const marker=L.marker([").append(center.latitude()).append(',').append(center.longitude()).append("]).addTo(map);")
                    .append("marker.bindPopup('<strong>").append(html(center.name())).append("</strong><br>")
                    .append(html(center.address())).append("<br>Services: ").append(html(String.join(", ", center.services())))
                    .append("<br>Use the Directions button below the map.');")
                    .append("marker.on('click',()=>{if(window.javaHelpCenter)window.javaHelpCenter.select('").append(js(center.id())).append("');});\n");
        }
        return """
                <!doctype html><html><head><meta charset='utf-8'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'><style>html,body,#map{height:100%;margin:0}</style></head>
                <body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>
                const map=L.map('map').setView([__LAT__,__LON__],12);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
                __MARKERS__
                </script></body></html>"""
                .replace("__LAT__", Double.toString(first.latitude()))
                .replace("__LON__", Double.toString(first.longitude()))
                .replace("__MARKERS__", markers.toString());
    }
    private static String js(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'"); }
    private static String html(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;"); }
}
