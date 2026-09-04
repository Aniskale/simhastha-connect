package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Reusable map and browser integration for the Ghat user flow. */
public final class GoogleMapsService {
    private static final double MAP_WIDTH = 650;
    private static final double MAP_HEIGHT = 580;
    private static final int OSM_ZOOM = 11;
    private static final int OSM_TILE_SIZE = 256;
    private static final HttpClient TILE_CLIENT = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(8))
            .build();
    private static final String TILE_USER_AGENT = "SimhasthaConnect/1.0 JavaFX local pilgrim map";
    private static final Path TILE_CACHE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "simhastha-connect", "osm-tiles");
    private static final Set<String> TILE_DOWNLOADS_IN_FLIGHT = ConcurrentHashMap.newKeySet();
    public record Point(String name, double latitude, double longitude) { }
    private record LocatedGhat(Ghat ghat, Point point) { }
    private record FacilityPoint(String id, String category, String glyph, String color, String name, String detail, Point point) { }
    private static final Map<String, Point> GHAT_POINTS = Map.ofEntries(
            Map.entry("ramkund", new Point("Ramkund", 20.0059, 73.7890)),
            Map.entry("ahilyabai-holkar", new Point("Ahilyabai Holkar Ghat", 20.0063, 73.7895)),
            Map.entry("kapila", new Point("Kapila Ghat", 20.0048, 73.7881)),
            Map.entry("naroshankar", new Point("Naroshankar Ghat", 20.0067, 73.7902)),
            Map.entry("laxman-kund", new Point("Laxman Kund Ghat", 20.0052, 73.7904)),
            Map.entry("ram-ghat", new Point("Ram Ghat", 20.0056, 73.7886)),
            Map.entry("tapovan", new Point("Tapovan Ghat", 20.0132, 73.8080)),
            Map.entry("sita", new Point("Sita Ghat", 20.0060, 73.7911)),
            Map.entry("ganga-godavari", new Point("Ganga Godavari Ghat", 20.0061, 73.7891)),
            Map.entry("dasak", new Point("Dasak Ghat", 19.9652, 73.8650)),
            Map.entry("goda-park", new Point("Goda Park Ghat", 20.0079, 73.7786)),
            Map.entry("sangam", new Point("Sangam Ghat", 20.0120, 73.8035)),
            Map.entry("choudhary", new Point("Choudhary Ghat", 20.0042, 73.7872)),
            Map.entry("teerthraj", new Point("Teerthraj Ghat", 20.0069, 73.7884)),
            Map.entry("kushavart", new Point("Kushavart Tirtha", 19.9325, 73.5309)));
    private static final List<FacilityPoint> FACILITY_POINTS = List.of(
            new FacilityPoint("help-ramkund", "help", "?", "#f68b00", "Simhastha Help Centre - Ramkund", "Official pilgrim support desk", new Point("Ramkund Help Centre", 20.0059, 73.7890)),
            new FacilityPoint("help-panchavati", "help", "?", "#f68b00", "Simhastha Help Centre - Panchavati Sector", "Information, route and missing-person support", new Point("Panchavati Help Centre", 20.0094, 73.7924)),
            new FacilityPoint("help-tapovan", "help", "?", "#f68b00", "Simhastha Help Centre - Tapovan Sector", "Guidance for camps, routes and services", new Point("Tapovan Help Centre", 20.0140, 73.8054)),
            new FacilityPoint("hirkani-ramkund", "hirkani", "M+B", "#9b5de5", "Hirkani Room - Ramkund Sector", "Mother and baby care support", new Point("Hirkani Ramkund", 20.0072, 73.7877)),
            new FacilityPoint("hirkani-panchavati", "hirkani", "M+B", "#9b5de5", "Hirkani Room - Panchavati Sector", "Mother and baby rest point", new Point("Hirkani Panchavati", 20.0111, 73.7956)),
            new FacilityPoint("hirkani-tapovan", "hirkani", "M+B", "#9b5de5", "Hirkani Room - Tapovan Sector", "Feeding and rest support", new Point("Hirkani Tapovan", 20.0156, 73.8077)),
            new FacilityPoint("toilet-ramkund", "toilet", "WC", "#21a35b", "Public Toilets - Ramkund Queue Zone", "Sanitation near riverfront route", new Point("Ramkund Toilets", 20.0051, 73.7876)),
            new FacilityPoint("toilet-panchavati", "toilet", "WC", "#21a35b", "Public Toilets - Panchavati Sector", "Sanitation near walking corridor", new Point("Panchavati Toilets", 20.0107, 73.7941)),
            new FacilityPoint("toilet-tapovan", "toilet", "WC", "#21a35b", "Public Toilets - Tapovan Route", "Sanitation on Tapovan pilgrim route", new Point("Tapovan Toilets", 20.0144, 73.8059)),
            new FacilityPoint("parking-ramkund", "parking", "P", "#0f76bd", "Parking - Ramkund Outer Ring", "Vehicle holding and drop-off", new Point("Ramkund Parking", 20.0016, 73.7818)),
            new FacilityPoint("parking-tapovan", "parking", "P", "#0f76bd", "Parking - Tapovan Entry", "Vehicle parking and drop-off", new Point("Tapovan Parking", 20.0205, 73.8120)),
            new FacilityPoint("parking-trimbak", "parking", "P", "#0f76bd", "Parking - Trimbakeshwar Route", "Outer parking towards Trimbak", new Point("Trimbak Parking", 19.9398, 73.5432)),
            new FacilityPoint("hospital-panchavati", "hospital", "+", "#ef4444", "Medical Camp - Panchavati Sector", "First aid and health desk", new Point("Panchavati Medical Camp", 20.0089, 73.7908)),
            new FacilityPoint("hospital-civil", "hospital", "+", "#ef4444", "Hospital - Nashik City Sector", "Emergency medical support", new Point("Nashik Medical Support", 20.0028, 73.7805)),
            new FacilityPoint("hospital-tapovan", "hospital", "+", "#ef4444", "Medical Camp - Tapovan Zone", "First aid and health desk", new Point("Tapovan Medical Camp", 20.0116, 73.8074)),
            new FacilityPoint("ghat-ramkund", "ghat", "\u0950", "#a24606", "Ramkund Ghat", "Main Godavari snan location", new Point("Ramkund Ghat", 20.0059, 73.7890)),
            new FacilityPoint("ghat-kushavart", "ghat", "\u0950", "#a24606", "Kushavart Tirtha - Trimbakeshwar", "Trimbakeshwar snan and darshan zone", new Point("Kushavart Tirtha", 19.9325, 73.5309)));
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
        List<LocatedGhat> located = ghats.stream().map(this::locatedGhat).filter(java.util.Objects::nonNull).toList();
        if (located.isEmpty()) return openBrowser(searchUrl(selected == null ? "Nashik Ghats Panchavati" : locationQuery(selected)));
        Stage dialog = new Stage(); dialog.setTitle("Ghats & Snan Map");
        if (owner != null) { dialog.initOwner(owner); dialog.initModality(Modality.NONE); }
        dialog.setScene(new Scene(nativeMapContent(located, selected), 950, 660));
        dialog.show();
        return Optional.empty();
    }

    public Optional<String> showSimhasthaArea(Window owner, String selectedCategory) {
        String activeCategory = normalizeFacilityCategory(selectedCategory);
        Stage dialog = new Stage();
        dialog.setTitle("Simhastha Kumbh Area Map");
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.NONE);
        }
        dialog.setScene(new Scene(nativeFacilityMapContent(activeCategory), 1010, 680));
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

    private BorderPane nativeMapContent(List<LocatedGhat> ghats, Ghat selected) {
        double minLat = ghats.stream().mapToDouble(value -> value.point().latitude()).min().orElse(19.9);
        double maxLat = ghats.stream().mapToDouble(value -> value.point().latitude()).max().orElse(20.02);
        double minLon = ghats.stream().mapToDouble(value -> value.point().longitude()).min().orElse(73.52);
        double maxLon = ghats.stream().mapToDouble(value -> value.point().longitude()).max().orElse(73.87);
        double latSpan = Math.max(0.001, maxLat - minLat);
        double lonSpan = Math.max(0.001, maxLon - minLon);

        Pane map = new Pane();
        map.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        map.setMinSize(MAP_WIDTH, MAP_HEIGHT);
        map.setMaxSize(MAP_WIDTH, MAP_HEIGHT);
        map.setStyle("-fx-background-color: linear-gradient(to bottom right, #fff8e7, #ecd5ad); -fx-background-radius: 14px; -fx-border-color: rgba(139,67,12,.20); -fx-border-radius: 14px;");
        map.setClip(roundedMapClip());
        addOpenStreetMapTiles(map, minLat, maxLat, minLon, maxLon);
        Line riverHighlight = new Line(-40, 345, 700, 235);
        riverHighlight.setStrokeWidth(18);
        riverHighlight.setStyle("-fx-stroke: rgba(42, 117, 154, .24); -fx-stroke-line-cap: round;");
        map.getChildren().add(riverHighlight);

        for (LocatedGhat located : ghats) {
            Ghat ghat = located.ghat();
            Point point = located.point();
            double[] marker = mapPoint(point.latitude(), point.longitude(), minLat, maxLat, minLon, maxLon);
            double left = marker[0];
            double top = marker[1];
            Button pin = new Button("\u0950 " + ghat.name());
            pin.setLayoutX(left);
            pin.setLayoutY(top);
            pin.setStyle("-fx-background-color: rgba(255,248,230,.98); -fx-background-radius: 999px; -fx-border-color: rgba(122,15,18,.42); -fx-border-radius: 999px; -fx-text-fill: #7a0f12; -fx-font-family: 'Nirmala UI', 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 8px; -fx-effect: dropshadow(gaussian, rgba(58,25,6,.20), 10, .16, 0, 3); -fx-cursor: hand;");
            if (selected != null && selected.id().equals(ghat.id())) pin.setStyle(pin.getStyle() + " -fx-border-width: 2px; -fx-border-color: #d17812;");
            pin.setOnAction(event -> openGhatLocation(ghat));
            map.getChildren().add(pin);
        }

        Label title = new Label("Ghats & Snan Map");
        title.setStyle("-fx-text-fill: #5b1414; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label subtitle = new Label("All Nashik and Trimbakeshwar Ghat locations");
        subtitle.setStyle("-fx-text-fill: #76553a; -fx-font-size: 12px;");
        VBox titleBox = new VBox(2, title, subtitle);
        titleBox.setPadding(new Insets(14, 16, 12, 16));
        titleBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        titleBox.setStyle("-fx-background-color: rgba(255,253,248,.95); -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(74,36,8,.14), 12, .14, 0, 4);");
        Label attribution = new Label("© OpenStreetMap contributors");
        attribution.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        attribution.setStyle("-fx-text-fill: #5f4a36; -fx-font-size: 10px; -fx-padding: 5px 8px; -fx-background-color: rgba(255,253,248,.86); -fx-background-radius: 999px;");
        StackPane mapShell = new StackPane(map, titleBox, attribution);
        StackPane.setAlignment(titleBox, Pos.TOP_LEFT);
        StackPane.setMargin(titleBox, new Insets(18));
        StackPane.setAlignment(attribution, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(attribution, new Insets(0, 14, 14, 0));

        VBox list = new VBox(8);
        list.setPadding(new Insets(2));
        for (LocatedGhat located : ghats) list.getChildren().add(mapListRow(located.ghat()));
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(260);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Label status = new Label("Ghat Map • " + ghats.size() + " Ghat location(s)");
        status.setPadding(new Insets(9, 14, 9, 14));
        status.setStyle("-fx-text-fill: #5b1414; -fx-font-weight: bold; -fx-background-color: #fff8e8; -fx-border-color: transparent transparent rgba(139,67,12,.14) transparent;");
        BorderPane root = new BorderPane();
        root.setTop(status);
        root.setCenter(mapShell);
        root.setRight(scroll);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #fff7e7;");
        return root;
    }

    private BorderPane nativeFacilityMapContent(String activeCategory) {
        double minLat = FACILITY_POINTS.stream().mapToDouble(value -> value.point().latitude()).min().orElse(19.93);
        double maxLat = FACILITY_POINTS.stream().mapToDouble(value -> value.point().latitude()).max().orElse(20.03);
        double minLon = FACILITY_POINTS.stream().mapToDouble(value -> value.point().longitude()).min().orElse(73.53);
        double maxLon = FACILITY_POINTS.stream().mapToDouble(value -> value.point().longitude()).max().orElse(73.87);
        double[] centerLat = { (minLat + maxLat) / 2.0 };
        double[] centerLon = { (minLon + maxLon) / 2.0 };
        int[] zoom = { OSM_ZOOM };
        double[] drag = new double[4];

        Pane map = new Pane();
        map.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        map.setMinSize(MAP_WIDTH, MAP_HEIGHT);
        map.setMaxSize(MAP_WIDTH, MAP_HEIGHT);
        map.setStyle("-fx-background-color: linear-gradient(to bottom right, #fff8e7, #ecd5ad); -fx-background-radius: 14px; -fx-border-color: rgba(139,67,12,.20); -fx-border-radius: 14px;");
        map.setClip(roundedMapClip());

        Label title = new Label("Simhastha Kumbh Area Map");
        title.setStyle("-fx-text-fill: #5b1414; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label subtitle = new Label("Drag to move, scroll to zoom, or use Locate for exact point");
        subtitle.setStyle("-fx-text-fill: #76553a; -fx-font-size: 12px;");
        VBox titleBox = new VBox(2, title, subtitle);
        titleBox.setPadding(new Insets(14, 16, 12, 16));
        titleBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        titleBox.setStyle("-fx-background-color: rgba(255,253,248,.95); -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(74,36,8,.14), 12, .14, 0, 4);");

        Button zoomIn = mapControlButton("+");
        Button zoomOut = mapControlButton("-");
        Label dragHint = new Label("Drag map");
        dragHint.setStyle("-fx-text-fill: #6a3c20; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: rgba(255,253,248,.90); -fx-background-radius: 999px; -fx-padding: 5px 9px;");
        VBox controls = new VBox(6, zoomIn, zoomOut, dragHint);
        controls.setAlignment(Pos.CENTER);
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label attribution = new Label("© OpenStreetMap contributors");
        attribution.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        attribution.setStyle("-fx-text-fill: #5f4a36; -fx-font-size: 10px; -fx-padding: 5px 8px; -fx-background-color: rgba(255,253,248,.86); -fx-background-radius: 999px;");
        StackPane mapShell = new StackPane(map, titleBox, controls, attribution);
        StackPane.setAlignment(titleBox, Pos.TOP_LEFT);
        StackPane.setMargin(titleBox, new Insets(18));
        StackPane.setAlignment(controls, Pos.TOP_RIGHT);
        StackPane.setMargin(controls, new Insets(18));
        StackPane.setAlignment(attribution, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(attribution, new Insets(0, 14, 14, 0));

        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            map.getChildren().clear();
            addOpenStreetMapTilesForViewport(map, centerLat[0], centerLon[0], zoom[0]);
            addFacilityPins(map, activeCategory, centerLat[0], centerLon[0], zoom[0]);
        };
        map.setOnMousePressed(event -> {
            drag[0] = event.getX();
            drag[1] = event.getY();
            drag[2] = lonToPixel(centerLon[0], zoom[0]);
            drag[3] = latToPixel(centerLat[0], zoom[0]);
        });
        map.setOnMouseDragged(event -> {
            double centerX = drag[2] - (event.getX() - drag[0]);
            double centerY = drag[3] - (event.getY() - drag[1]);
            centerLon[0] = pixelToLon(centerX, zoom[0]);
            centerLat[0] = pixelToLat(centerY, zoom[0]);
            render[0].run();
        });
        map.setOnScroll(event -> {
            int nextZoom = Math.max(10, Math.min(15, zoom[0] + (event.getDeltaY() > 0 ? 1 : -1)));
            if (nextZoom != zoom[0]) {
                zoom[0] = nextZoom;
                render[0].run();
            }
            event.consume();
        });
        zoomIn.setOnAction(event -> {
            zoom[0] = Math.min(15, zoom[0] + 1);
            render[0].run();
        });
        zoomOut.setOnAction(event -> {
            zoom[0] = Math.max(10, zoom[0] - 1);
            render[0].run();
        });
        render[0].run();

        VBox list = new VBox(8);
        list.setPadding(new Insets(2));
        FACILITY_POINTS.stream()
                .sorted((left, right) -> Boolean.compare(!left.category().equals(activeCategory), !right.category().equals(activeCategory)))
                .forEach(facility -> list.getChildren().add(facilityListRow(facility, facility.category().equals(activeCategory))));
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(310);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        String statusText = activeCategory == null
                ? "All Facilities"
                : facilityCategoryTitle(activeCategory);
        Label status = new Label("Simhastha Map • " + statusText + " • " + FACILITY_POINTS.size() + " point(s)");
        status.setPadding(new Insets(9, 14, 9, 14));
        status.setStyle("-fx-text-fill: #5b1414; -fx-font-weight: bold; -fx-background-color: #fff8e8; -fx-border-color: transparent transparent rgba(139,67,12,.14) transparent;");
        BorderPane root = new BorderPane();
        root.setTop(status);
        root.setCenter(mapShell);
        root.setRight(scroll);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #fff7e7;");
        return root;
    }

    private Button mapControlButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: rgba(255,253,248,.96); -fx-background-radius: 999px; -fx-border-color: rgba(122,15,18,.20); -fx-border-radius: 999px; -fx-text-fill: #5b1414; -fx-font-size: 15px; -fx-font-weight: bold; -fx-min-width: 34px; -fx-min-height: 34px; -fx-pref-width: 34px; -fx-pref-height: 34px; -fx-padding: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(58,25,6,.16), 8, .15, 0, 2);");
        return button;
    }

    private void addFacilityPins(Pane map, String activeCategory, double centerLat, double centerLon, int zoom) {
        for (FacilityPoint facility : FACILITY_POINTS) {
            double[] marker = mapPointForViewport(facility.point().latitude(), facility.point().longitude(), centerLat, centerLon, zoom);
            StackPane pinIcon = new StackPane(facilityVectorIcon(facility.category(), 18));
            pinIcon.setStyle("-fx-background-color: " + facility.color() + "; -fx-background-radius: 999px; -fx-border-color: white; -fx-border-radius: 999px; -fx-border-width: 1.6px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px;");
            Button pin = new Button(shortFacilityName(facility));
            pin.setGraphic(pinIcon);
            pin.setLayoutX(marker[0]);
            pin.setLayoutY(marker[1]);
            pin.setStyle(facilityPinStyle(facility, facility.category().equals(activeCategory)));
            pin.setOnAction(event -> openFacilityLocation(facility));
            map.getChildren().add(pin);
        }
    }

    private HBox facilityListRow(FacilityPoint facility, boolean active) {
        StackPane icon = new StackPane(facilityVectorIcon(facility.category(), 19));
        icon.setStyle("-fx-background-color: " + facility.color() + "; -fx-background-radius: 999px; -fx-border-color: white; -fx-border-radius: 999px; -fx-border-width: 1px; -fx-min-width: 32px; -fx-min-height: 32px; -fx-pref-width: 32px; -fx-pref-height: 32px;");
        VBox copy = new VBox(2, new Label(facility.name()), new Label(facility.detail()));
        copy.getChildren().get(0).setStyle("-fx-text-fill: #5b1414; -fx-font-size: 12px; -fx-font-weight: bold;");
        copy.getChildren().get(1).setStyle("-fx-text-fill: #76553a; -fx-font-size: 10px;");
        Button open = new Button("Locate");
        open.setStyle("-fx-background-color: rgba(194, 114, 12, .10); -fx-border-color: rgba(175, 89, 11, .28); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #83380a; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand;");
        open.setOnAction(event -> openFacilityLocation(facility));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, icon, copy, spacer, open);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        String border = active ? facility.color() : "rgba(184, 100, 6, .14)";
        row.setStyle("-fx-background-color: rgba(255, 253, 248, .95); -fx-background-radius: 10px; -fx-border-color: " + border + "; -fx-border-radius: 10px; -fx-border-width: " + (active ? "2px" : "1px") + ";");
        return row;
    }

    private String facilityPinStyle(FacilityPoint facility, boolean active) {
        return "-fx-background-color: rgba(255,248,230,.98);"
                + " -fx-background-radius: 999px;"
                + " -fx-border-color: " + facility.color() + ";"
                + " -fx-border-radius: 999px;"
                + " -fx-border-width: " + (active ? "2.5px" : "1.25px") + ";"
                + " -fx-text-fill: #5b1414;"
                + " -fx-font-family: 'Nirmala UI', 'Segoe UI', sans-serif;"
                + " -fx-font-size: 10.5px;"
                + " -fx-font-weight: bold;"
                + " -fx-padding: 5px 8px;"
                + " -fx-effect: dropshadow(gaussian, rgba(58,25,6,.22), 11, .16, 0, 3);"
                + " -fx-cursor: hand;";
    }

    private Optional<String> openFacilityLocation(FacilityPoint facility) {
        return openBrowser(searchUrl(facility.point().latitude() + "," + facility.point().longitude()));
    }

    private Pane facilityVectorIcon(String category, double size) {
        Pane icon = new Pane();
        icon.setMinSize(size, size);
        icon.setPrefSize(size, size);
        icon.setMaxSize(size, size);
        String stroke = "-fx-stroke: white; -fx-stroke-width: 2.2px; -fx-stroke-line-cap: round; -fx-fill: transparent;";
        String fill = "-fx-fill: white;";
        if ("help".equals(category)) {
            Arc headset = new Arc(size / 2, size / 2, size * 0.34, size * 0.34, 22, 136);
            headset.setStyle(stroke);
            Rectangle left = roundedRect(size * 0.17, size * 0.43, size * 0.13, size * 0.24, 4, fill);
            Rectangle right = roundedRect(size * 0.70, size * 0.43, size * 0.13, size * 0.24, 4, fill);
            Line mic = new Line(size * 0.64, size * 0.66, size * 0.76, size * 0.72);
            mic.setStyle(stroke);
            icon.getChildren().addAll(headset, left, right, mic);
        } else if ("hirkani".equals(category)) {
            Circle mother = filledCircle(size * 0.39, size * 0.25, size * 0.12, fill);
            Circle baby = filledCircle(size * 0.64, size * 0.43, size * 0.09, fill);
            Arc body = new Arc(size * 0.42, size * 0.67, size * 0.24, size * 0.27, 18, 144);
            body.setStyle(stroke);
            Line arm = new Line(size * 0.48, size * 0.56, size * 0.68, size * 0.58);
            arm.setStyle(stroke);
            Arc babyWrap = new Arc(size * 0.64, size * 0.63, size * 0.18, size * 0.18, 200, 120);
            babyWrap.setStyle(stroke);
            icon.getChildren().addAll(mother, baby, body, arm, babyWrap);
        } else if ("toilet".equals(category)) {
            Rectangle tank = roundedRect(size * 0.30, size * 0.18, size * 0.38, size * 0.18, 3, fill);
            Rectangle bowl = roundedRect(size * 0.34, size * 0.41, size * 0.31, size * 0.30, 6, stroke);
            Line pipe = new Line(size * 0.48, size * 0.36, size * 0.48, size * 0.44);
            pipe.setStyle(stroke);
            Line base = new Line(size * 0.35, size * 0.78, size * 0.67, size * 0.78);
            base.setStyle(stroke);
            icon.getChildren().addAll(tank, bowl, pipe, base);
        } else if ("parking".equals(category)) {
            Rectangle body = roundedRect(size * 0.18, size * 0.43, size * 0.64, size * 0.22, 5, stroke);
            Line roof = new Line(size * 0.31, size * 0.43, size * 0.42, size * 0.29);
            roof.setStyle(stroke);
            Line roofBack = new Line(size * 0.42, size * 0.29, size * 0.62, size * 0.43);
            roofBack.setStyle(stroke);
            Circle leftWheel = filledCircle(size * 0.32, size * 0.69, size * 0.07, fill);
            Circle rightWheel = filledCircle(size * 0.68, size * 0.69, size * 0.07, fill);
            icon.getChildren().addAll(body, roof, roofBack, leftWheel, rightWheel);
        } else if ("ghat".equals(category)) {
            Line base = new Line(size * 0.22, size * 0.72, size * 0.78, size * 0.72);
            base.setStyle(stroke);
            Line left = new Line(size * 0.28, size * 0.72, size * 0.44, size * 0.32);
            left.setStyle(stroke);
            Line right = new Line(size * 0.72, size * 0.72, size * 0.56, size * 0.32);
            right.setStyle(stroke);
            Circle flame = filledCircle(size * 0.50, size * 0.24, size * 0.08, fill);
            icon.getChildren().addAll(base, left, right, flame);
        } else {
            Rectangle bag = roundedRect(size * 0.24, size * 0.34, size * 0.52, size * 0.42, 5, stroke);
            Line handle = new Line(size * 0.40, size * 0.34, size * 0.40, size * 0.24);
            handle.setStyle(stroke);
            Line handleTop = new Line(size * 0.40, size * 0.24, size * 0.60, size * 0.24);
            handleTop.setStyle(stroke);
            Line handleRight = new Line(size * 0.60, size * 0.24, size * 0.60, size * 0.34);
            handleRight.setStyle(stroke);
            Line plusH = new Line(size * 0.39, size * 0.55, size * 0.61, size * 0.55);
            plusH.setStyle(stroke);
            Line plusV = new Line(size * 0.50, size * 0.44, size * 0.50, size * 0.66);
            plusV.setStyle(stroke);
            icon.getChildren().addAll(bag, handle, handleTop, handleRight, plusH, plusV);
        }
        return icon;
    }

    private Rectangle roundedRect(double x, double y, double width, double height, double arc, String style) {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setArcWidth(arc);
        rectangle.setArcHeight(arc);
        rectangle.setStyle(style);
        return rectangle;
    }

    private Circle filledCircle(double x, double y, double radius, String style) {
        Circle circle = new Circle(x, y, radius);
        circle.setStyle(style);
        return circle;
    }

    private String shortFacilityName(FacilityPoint facility) {
        String name = facility.name();
        int separator = name.indexOf(" - ");
        return separator > 0 ? name.substring(0, separator) : name;
    }

    private String normalizeFacilityCategory(String category) {
        if (category == null || category.isBlank()) return null;
        String normalized = category.trim().toLowerCase(java.util.Locale.ROOT);
        return FACILITY_POINTS.stream().anyMatch(point -> point.category().equals(normalized)) ? normalized : null;
    }

    private String facilityCategoryTitle(String category) {
        if ("help".equals(category)) return "Simhastha Help Centres";
        if ("hirkani".equals(category)) return "Hirkani Mother and Baby Care";
        if ("toilet".equals(category)) return "Public Toilets";
        if ("parking".equals(category)) return "Parking Areas";
        if ("hospital".equals(category)) return "Hospitals and Medical Camps";
        if ("ghat".equals(category)) return "Ghats";
        return "All Facilities";
    }

    private Rectangle roundedMapClip() {
        Rectangle clip = new Rectangle(MAP_WIDTH, MAP_HEIGHT);
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        return clip;
    }

    private void addOpenStreetMapTiles(Pane map, double minLat, double maxLat, double minLon, double maxLon) {
        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;
        addOpenStreetMapTilesForViewport(map, centerLat, centerLon, OSM_ZOOM);
    }

    private void addOpenStreetMapTilesForViewport(Pane map, double centerLat, double centerLon, int zoom) {
        double centerX = lonToPixel(centerLon, zoom);
        double centerY = latToPixel(centerLat, zoom);
        double viewportX = centerX - MAP_WIDTH / 2.0;
        double viewportY = centerY - MAP_HEIGHT / 2.0;
        int minTileX = (int) Math.floor(viewportX / OSM_TILE_SIZE);
        int maxTileX = (int) Math.floor((viewportX + MAP_WIDTH) / OSM_TILE_SIZE);
        int minTileY = (int) Math.floor(viewportY / OSM_TILE_SIZE);
        int maxTileY = (int) Math.floor((viewportY + MAP_HEIGHT) / OSM_TILE_SIZE);
        int maxTileIndex = (1 << zoom) - 1;
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                if (tileY < 0 || tileY > maxTileIndex) continue;
                int wrappedX = Math.floorMod(tileX, maxTileIndex + 1);
                ImageView tile = new ImageView();
                tile.setFitWidth(OSM_TILE_SIZE);
                tile.setFitHeight(OSM_TILE_SIZE);
                tile.setLayoutX(tileX * OSM_TILE_SIZE - viewportX);
                tile.setLayoutY(tileY * OSM_TILE_SIZE - viewportY);
                tile.setOpacity(0);
                map.getChildren().add(tile);
                loadOpenStreetMapTile(tile, zoom, wrappedX, tileY);
            }
        }
    }

    private void loadOpenStreetMapTile(ImageView tile, int zoom, int tileX, int tileY) {
        Path cachedTile = TILE_CACHE_DIR.resolve(zoom + "-" + tileX + "-" + tileY + ".png");
        if (Files.isRegularFile(cachedTile)) {
            tile.setImage(new Image(cachedTile.toUri().toString(), OSM_TILE_SIZE, OSM_TILE_SIZE, false, true, true));
            tile.setOpacity(1);
            return;
        }
        String key = zoom + "/" + tileX + "/" + tileY;
        if (!TILE_DOWNLOADS_IN_FLIGHT.add(key)) return;
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(TILE_CACHE_DIR);
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://tile.openstreetmap.org/" + key + ".png"))
                        .timeout(java.time.Duration.ofSeconds(12))
                        .header("User-Agent", TILE_USER_AGENT)
                        .header("Accept", "image/png,image/*;q=0.8,*/*;q=0.5")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = TILE_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200 || response.body().length == 0) return;
                Files.write(cachedTile, response.body());
                Platform.runLater(() -> {
                    tile.setImage(new Image(cachedTile.toUri().toString(), OSM_TILE_SIZE, OSM_TILE_SIZE, false, true, true));
                    tile.setOpacity(1);
                });
            } catch (IOException | InterruptedException ignored) {
                if (Thread.currentThread().isInterrupted()) Thread.currentThread().interrupt();
            } finally {
                TILE_DOWNLOADS_IN_FLIGHT.remove(key);
            }
        });
    }

    private double[] mapPoint(double latitude, double longitude, double minLat, double maxLat, double minLon, double maxLon) {
        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;
        return mapPointForViewport(latitude, longitude, centerLat, centerLon, OSM_ZOOM);
    }

    private double[] mapPointForViewport(double latitude, double longitude, double centerLat, double centerLon, int zoom) {
        double viewportX = lonToPixel(centerLon, zoom) - MAP_WIDTH / 2.0;
        double viewportY = latToPixel(centerLat, zoom) - MAP_HEIGHT / 2.0;
        return new double[] { lonToPixel(longitude, zoom) - viewportX, latToPixel(latitude, zoom) - viewportY };
    }

    private double lonToPixel(double longitude, int zoom) {
        return ((longitude + 180.0) / 360.0) * OSM_TILE_SIZE * (1 << zoom);
    }

    private double latToPixel(double latitude, int zoom) {
        double clipped = Math.max(-85.05112878, Math.min(85.05112878, latitude));
        double sinLatitude = Math.sin(Math.toRadians(clipped));
        return (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)) * OSM_TILE_SIZE * (1 << zoom);
    }

    private double pixelToLon(double pixelX, int zoom) {
        return pixelX / (OSM_TILE_SIZE * (1 << zoom)) * 360.0 - 180.0;
    }

    private double pixelToLat(double pixelY, int zoom) {
        double value = Math.PI - 2.0 * Math.PI * pixelY / (OSM_TILE_SIZE * (1 << zoom));
        return Math.toDegrees(Math.atan(Math.sinh(value)));
    }

    private HBox mapListRow(Ghat ghat) {
        VBox copy = new VBox(2, new Label(ghat.name()), new Label(ghat.area()));
        copy.getChildren().get(0).setStyle("-fx-text-fill: #5b1414; -fx-font-size: 12px; -fx-font-weight: bold;");
        copy.getChildren().get(1).setStyle("-fx-text-fill: #76553a; -fx-font-size: 10px;");
        Button open = new Button("Open");
        open.setStyle("-fx-background-color: rgba(194, 114, 12, .10); -fx-border-color: rgba(175, 89, 11, .28); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #83380a; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand;");
        open.setOnAction(event -> openGhatLocation(ghat));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, copy, spacer, open);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: rgba(255, 253, 248, .95); -fx-background-radius: 10px; -fx-border-color: rgba(184, 100, 6, .14); -fx-border-radius: 10px;");
        return row;
    }

    private boolean hasCoordinates(Ghat ghat) { return ghat != null && ghat.latitude() != null && ghat.longitude() != null; }
    private LocatedGhat locatedGhat(Ghat ghat) {
        if (ghat == null) return null;
        if (hasCoordinates(ghat)) return new LocatedGhat(ghat, new Point(ghat.name(), ghat.latitude(), ghat.longitude()));
        Point point = GHAT_POINTS.get(ghat.id());
        return point == null ? null : new LocatedGhat(ghat, point);
    }

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
    private static String encodeStatic(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
