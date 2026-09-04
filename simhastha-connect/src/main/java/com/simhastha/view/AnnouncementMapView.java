package com.simhastha.view;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/** Internal OpenStreetMap view for one announcement destination at a time. */
public final class AnnouncementMapView {
    private record NearbyPlace(String name, String type, double latitude, double longitude) { }
    private static final List<NearbyPlace> LOCAL_PLACES = List.of(
            new NearbyPlace("Ramkund Ghat", "Ghat", 20.0059, 73.7890),
            new NearbyPlace("Panchavati", "Important landmark", 20.0067, 73.7914),
            new NearbyPlace("Kalaram Mandir", "Temple", 20.0076, 73.7907),
            new NearbyPlace("CBS Nashik", "Bus stop", 19.9975, 73.7898),
            new NearbyPlace("Tapovan Parking", "Parking", 20.0180, 73.8095));

    private final Consumer<Node> navigator;
    private final AnnouncementDemoStore.Announcement announcement;

    public AnnouncementMapView(Consumer<Node> navigator, AnnouncementDemoStore.Announcement announcement) {
        this.navigator = navigator;
        this.announcement = announcement;
    }

    public static void navigateTo(AnnouncementDemoStore.Announcement announcement) {
        String destination;
        if (valid(announcement.latitude(), announcement.longitude())) {
            destination = announcement.latitude() + "," + announcement.longitude();
        } else {
            String location = destinationName(announcement);
            if (location.isBlank()) location = announcement.address();
            if (location == null || location.isBlank()) return;
            destination = URLEncoder.encode(location + " Nashik", StandardCharsets.UTF_8);
        }
        String url = "https://www.google.com/maps/dir/?api=1&destination=" + destination;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception ignored) {
                // The in-app map remains available if the system browser cannot be opened.
            }
        });
    }

    public Node page() {
        String destination = destinationName(announcement);
        Button back = new Button("← Back to Announcements");
        back.getStyleClass().add("pilgrim-small-action");
        back.setOnAction(event -> UserAnnouncementView.openMainAnnouncements(navigator));

        VBox mapHolder = new VBox();
        mapHolder.getStyleClass().add("announcement-live-map");
        mapHolder.setMinHeight(470);
        mapHolder.setPrefHeight(540);
        VBox.setVgrow(mapHolder, Priority.ALWAYS);
        if (valid(announcement.latitude(), announcement.longitude())) {
            addLiveMap(mapHolder, announcement.latitude(), announcement.longitude(), destination);
        } else {
            mapHolder.getChildren().add(fallback("Location unavailable", "This announcement does not have valid map coordinates."));
        }

        String address = announcement.address() == null || announcement.address().isBlank()
                ? "Address / Landmark: Not available" : "Address / Landmark: " + announcement.address();
        VBox locationCopy = new VBox(4, label("📍 " + destination), detail(address),
                detail("Coordinates: " + announcement.latitude() + ", " + announcement.longitude()));
        Button navigate = new Button("Go to Navigate");
        navigate.getStyleClass().add("primary-button");
        boolean navigationAvailable = valid(announcement.latitude(), announcement.longitude())
                || !destination.isBlank() || (announcement.address() != null && !announcement.address().isBlank());
        navigate.setDisable(!navigationAvailable);
        navigate.setOnAction(event -> navigateTo(announcement));
        HBox destinationRow = new HBox(12, locationCopy, spacer(), navigate);
        destinationRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(locationCopy, Priority.ALWAYS);
        VBox destinationInfo = new VBox(5, label("Destination"), destinationRow);
        if (announcement.locationChange()) {
            destinationInfo.getChildren().addAll(detail("Previous: " + announcement.previousLocation()),
                    detail("New: " + announcement.newLocation()));
        }
        destinationInfo.getStyleClass().add("pilgrim-panel");
        VBox root = new VBox(10, back, label("Announcement Map"), detail(destination), mapHolder,
                destinationInfo);
        root.getStyleClass().add("pilgrim-dashboard-main");
        root.setPadding(new Insets(12, 22, 28, 22));
        return root;
    }

    private void addLiveMap(VBox holder, double latitude, double longitude, String destination) {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setMinHeight(470);
        VBox.setVgrow(webView, Priority.ALWAYS);
        WebEngine engine = webView.getEngine();
        StackPane wrapper = new StackPane(webView);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        Label loading = detail("Loading OpenStreetMap…");
        loading.getStyleClass().add("announcement-map-loading");
        StackPane.setAlignment(loading, Pos.CENTER);
        wrapper.getChildren().add(loading);
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) wrapper.getChildren().remove(loading);
            if (state == Worker.State.FAILED) {
                wrapper.getChildren().setAll(fallback("Map unavailable", "OpenStreetMap could not be loaded. Please try again later."));
            }
        });
        holder.getChildren().add(wrapper);
        engine.loadContent(mapHtml(latitude, longitude, destination));
    }

    private String mapHtml(double latitude, double longitude, String destination) {
        StringBuilder nearby = new StringBuilder();
        for (NearbyPlace place : LOCAL_PLACES) {
            if (distanceKm(latitude, longitude, place.latitude(), place.longitude()) <= 2.5) {
                nearby.append("L.circleMarker([")
                        .append(place.latitude()).append(',').append(place.longitude())
                        .append("],{radius:6,color:'#8a641b',fillColor:'#e0b968',fillOpacity:.85,weight:1})")
                        .append(".addTo(map).bindPopup('<b>").append(escapeForMapPopup(place.name()))
                        .append("</b><br>").append(escapeForMapPopup(place.type())).append("');");
            }
        }
        String html = """
                <!doctype html><html><head><meta charset='utf-8'>
                <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>
                <style>html,body,#map{height:100%;width:100%;margin:0} .leaflet-popup-content{font-family:Arial,sans-serif;color:#302015}</style>
                </head><body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
                <script>var map=L.map('map',{zoomControl:true}).setView([__LAT__,__LON__],16);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
                var destination=L.circleMarker([__LAT__,__LON__],{radius:10,color:'#9b1c20',fillColor:'#c93836',fillOpacity:1,weight:3}).addTo(map);
                destination.bindPopup('<b>__TITLE__</b><br>__LOCATION__<br>__CATEGORY__').openPopup();__NEARBY__</script></body></html>
                """;
        return html.replace("__LAT__", Double.toString(latitude))
                .replace("__LON__", Double.toString(longitude))
                .replace("__TITLE__", escapeForMapPopup(announcement.title()))
                .replace("__LOCATION__", escapeForMapPopup(destination))
                .replace("__CATEGORY__", escapeForMapPopup(announcement.category()))
                .replace("__NEARBY__", nearby.toString());
    }

    private static Node fallback(String title, String message) {
        VBox card = new VBox(7, label(title), detail(message));
        card.getStyleClass().add("pilgrim-panel");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private static String destinationName(AnnouncementDemoStore.Announcement item) {
        String value = item.locationChange() ? item.newLocation() : item.locationName();
        return value == null ? "" : value;
    }
    private static boolean valid(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude) && latitude != 0 && longitude != 0
                && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180;
    }
    private static double distanceKm(double aLat, double aLon, double bLat, double bLon) {
        double dLat = Math.toRadians(bLat - aLat), dLon = Math.toRadians(bLon - aLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(aLat))
                * Math.cos(Math.toRadians(bLat)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    private static String escapeForMapPopup(String text) {
        return (text == null ? "" : text).replace("\\", "\\\\").replace("\r", "")
                .replace("\n", "\\n").replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("'", "&#39;").replace("\"", "&quot;");
    }
    private static Label label(String text) { Label label = new Label(text); label.getStyleClass().add("pilgrim-card-title"); label.setWrapText(true); return label; }
    private static Label detail(String text) { Label label = new Label(text); label.getStyleClass().add("pilgrim-card-detail"); label.setWrapText(true); return label; }
    private static Region spacer() { Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS); return spacer; }
}
