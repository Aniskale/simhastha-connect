package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Reusable Google Maps WebView integration for the Ghat user flow. */
public final class GoogleMapsService {
    public record Point(String name, double latitude, double longitude) { }
    @FunctionalInterface
    interface BrowserLauncher {
        Optional<String> open(String url);
    }

    private final GoogleMapsConfig config;
    private final BrowserLauncher browserLauncher;

    public GoogleMapsService(GoogleMapsConfig config) { this(config, GoogleMapsService::browse); }
    GoogleMapsService(GoogleMapsConfig config, BrowserLauncher browserLauncher) {
        this.config = config;
        this.browserLauncher = browserLauncher;
    }
    public boolean configured() { return config.configured(); }

    /**
     * Opens the exact location requested from an individual Ghat card. This is
     * deliberately separate from the all-Ghats map, so Locate cannot use a
     * previous selection or fall back to the generic Nashik Ghats search.
     */
    public Optional<String> openGhatLocation(Ghat ghat) {
        if (ghat == null || ghat.name() == null || ghat.name().isBlank()) {
            return Optional.of("Verified location is currently unavailable for this Ghat.");
        }
        return openBrowser(searchUrl(locationQuery(ghat)));
    }

    public Optional<String> showGhats(Window owner, List<Ghat> ghats, Ghat selected) {
        // A selected Ghat without verified coordinates must search for that exact Ghat;
        // an embedded map cannot safely focus it.
        if (selected != null && !hasCoordinates(selected)) return openBrowser(searchUrl(locationQuery(selected)));
        if (!configured()) return openBrowser(searchUrl(selected == null ? "Nashik Ghats Panchavati" : locationQuery(selected)));
        List<Ghat> located = ghats.stream().filter(this::hasCoordinates).toList();
        if (located.isEmpty()) return openBrowser(searchUrl(selected == null ? "Nashik Ghats Panchavati" : locationQuery(selected)));
        Stage dialog = new Stage(); dialog.setTitle("Ghats & Snan Map");
        if (owner != null) { dialog.initOwner(owner); dialog.initModality(Modality.NONE); }
        WebView view = new WebView(); view.setPrefSize(950, 660);
        Label status = new Label("Opening Google Maps..."); status.setPadding(new Insets(8));
        BorderPane root = new BorderPane(view); root.setTop(status);
        WebEngine engine = view.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observable, previous, state) -> {
            if (state == Worker.State.SUCCEEDED) status.setText("Google Maps • " + located.size() + " verified Ghat location(s)");
            else if (state == Worker.State.FAILED) status.setText("Google Maps could not be loaded. Check your internet connection and API key restrictions.");
        });
        dialog.setScene(new Scene(root));
        engine.loadContent(mapHtml(located, selected));
        dialog.show();
        return Optional.empty();
    }

    /** Browser directions work without a Maps JavaScript key and let Google Maps request origin when omitted. */
    public Optional<String> openDirections(Point origin, Point destination, String destinationQuery) {
        if (destination == null && (destinationQuery == null || destinationQuery.isBlank())) return Optional.of("No active entry route is currently available for this Ghat.");
        return openBrowser(directionsUrl(origin, destination, destinationQuery));
    }

    static String searchUrl(String query) {
        return "https://www.google.com/maps/search/?api=1&query=" + encodeStatic(query == null ? "Nashik Ghats Panchavati" : query);
    }

    static String directionsUrl(Point origin, Point destination, String destinationQuery) {
        String destinationValue = destination == null ? destinationQuery : destination.latitude() + "," + destination.longitude();
        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1&destination=").append(encodeStatic(destinationValue));
        if (origin != null) url.append("&origin=").append(encodeStatic(origin.latitude() + "," + origin.longitude()));
        return url.append("&travelmode=walking").toString();
    }

    private Optional<String> openBrowser(String url) {
        return browserLauncher.open(url);
    }

    private static Optional<String> browse(String url) {
        try {
            if (!Desktop.isDesktopSupported()) return Optional.of("Google Maps could not be opened on this device.");
            Desktop.getDesktop().browse(URI.create(url));
            return Optional.empty();
        } catch (Exception error) { return Optional.of("Google Maps could not be opened in your browser."); }
    }

    private String mapHtml(List<Ghat> ghats, Ghat selected) {
        Ghat focus = selected != null && hasCoordinates(selected) ? selected : ghats.get(0);
        String markers = ghats.stream().map(ghat -> """
                const marker = new google.maps.Marker({position:{lat:%s,lng:%s},map:map,title:'%s'});
                const info = new google.maps.InfoWindow({content:'<strong>%s</strong><br>%s<br>Crowd: %s<br>Bathing: %s<br>Water safety: %s'});
                marker.addListener('click',()=>info.open({map:map,anchor:marker}));
                %s
                """.formatted(ghat.latitude(), ghat.longitude(), js(ghat.name()), html(ghat.name()), html(ghat.area()), html(ghat.crowdLevel().name()),
                html(ghat.operationalState().bathingStatus().name()), html(ghat.operationalState().waterSafety().name()),
                selected != null && selected.id().equals(ghat.id()) ? "info.open({map:map,anchor:marker});" : "")).collect(Collectors.joining("\n"));
        return """
                <!doctype html><html><head><meta charset='utf-8'><style>html,body,#map{height:100%%;margin:0}body{font-family:Arial,sans-serif}</style></head>
                <body><div id='map'></div><script>
                function initMap(){const map=new google.maps.Map(document.getElementById('map'),{center:{lat:%s,lng:%s},zoom:15,mapTypeControl:true,streetViewControl:false});%s}
                </script><script async src='https://maps.googleapis.com/maps/api/js?key=%s&callback=initMap'></script></body></html>
                """.formatted(focus.latitude(), focus.longitude(), markers, encode(config.apiKey()));
    }

    private boolean hasCoordinates(Ghat ghat) { return ghat != null && ghat.latitude() != null && ghat.longitude() != null; }

    /**
     * Catalogue coordinates currently have no verification provenance.  Locate
     * therefore resolves an exact named place instead of treating an approximate
     * point as an authoritative Ghat pin.  Navigation retains its existing
     * gate/coordinate logic and is intentionally not changed here.
     */
    private String locationQuery(Ghat ghat) {
        String area = ghat.area() == null ? "" : ghat.area().trim();
        String exactName = "kushavart".equals(ghat.id()) ? "Kushavart Tirtha" : ghat.name();
        return area.isBlank() ? exactName + ", Maharashtra" : exactName + ", " + area + ", Maharashtra";
    }
    private String encode(String value) { return encodeStatic(value); }
    private static String encodeStatic(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String js(String value) { return (value == null ? "" : value).replace("\\", "\\\\").replace("'", "\\'"); }
    private String html(String value) { return (value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;"); }
}
