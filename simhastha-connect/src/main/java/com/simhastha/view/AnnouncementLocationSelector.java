package com.simhastha.view;

import java.util.function.Consumer;

import netscape.javascript.JSObject;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Modal, local-only map selector. Map clicks update pending state until Confirm is pressed. */
public final class AnnouncementLocationSelector {
    public record Selection(double latitude, double longitude) { }
    private AnnouncementLocationSelector() { }

    public static void show(Window owner, double initialLat, double initialLon, Consumer<Selection> confirm) {
        double startLat = valid(initialLat, initialLon) ? initialLat : 20.0064;
        double startLon = valid(initialLat, initialLon) ? initialLon : 73.7904;
        double[] pending = { startLat, startLon };
        Stage dialog = new Stage();
        if (owner != null) dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL); dialog.setTitle("Select Announcement Location");
        Label selected = new Label(coords(pending[0], pending[1])); selected.getStyleClass().add("pilgrim-card-detail");
        WebView map = new WebView(); map.setMinHeight(440); VBox.setVgrow(map, Priority.ALWAYS);
        WebEngine engine = map.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new Bridge((lat, lon) -> {
                    pending[0] = lat; pending[1] = lon; selected.setText(coords(lat, lon));
                }));
            }
        });
        engine.loadContent(html(startLat, startLon));
        Button cancel = new Button("Cancel"); cancel.getStyleClass().add("pilgrim-small-action"); cancel.setOnAction(e -> dialog.close());
        Button save = new Button("Confirm Location"); save.getStyleClass().add("primary-button"); save.setOnAction(e -> { confirm.accept(new Selection(pending[0], pending[1])); dialog.close(); });
        VBox root = new VBox(10, map, new VBox(3, new Label("Selected Location"), selected), new HBox(10, cancel, spacer(), save));
        root.getStyleClass().add("pilgrim-dashboard-main"); root.setPadding(new Insets(16));
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 760, 620);
        java.net.URL css = AnnouncementLocationSelector.class.getResource("/css/simhastha-theme.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        ThemeManager.applyTo(root);
        dialog.setScene(scene); dialog.showAndWait();
    }

    public static final class Bridge {
        private final java.util.function.BiConsumer<Double, Double> onSelect;
        public Bridge(java.util.function.BiConsumer<Double, Double> onSelect) { this.onSelect = onSelect; }
        public void select(double latitude, double longitude) { onSelect.accept(latitude, longitude); }
    }
    private static String html(double lat, double lon) {
        return """
                <!doctype html><html><head><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>
                <style>html,body,#map{width:100%;height:100%;margin:0}</style></head><body><div id='map'></div>
                <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>
                var map=L.map('map').setView([__LAT__,__LON__],16);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
                var marker=L.circleMarker([__LAT__,__LON__],{radius:10,color:'#9b1c20',fillColor:'#c93836',fillOpacity:1,weight:3}).addTo(map);
                map.on('click',function(e){marker.setLatLng(e.latlng);if(window.javaBridge)window.javaBridge.select(e.latlng.lat,e.latlng.lng);});</script></body></html>
                """.replace("__LAT__", Double.toString(lat)).replace("__LON__", Double.toString(lon));
    }
    private static boolean valid(double lat, double lon) { return Double.isFinite(lat) && Double.isFinite(lon) && lat != 0 && lon != 0; }
    private static String coords(double lat, double lon) { return "Latitude: " + String.format(java.util.Locale.ROOT, "%.6f", lat) + "    Longitude: " + String.format(java.util.Locale.ROOT, "%.6f", lon); }
    private static javafx.scene.layout.Region spacer() { javafx.scene.layout.Region r = new javafx.scene.layout.Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }
}
