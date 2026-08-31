package com.simhastha.view;

import com.simhastha.controller.BusinessMarketplaceController;
import com.simhastha.model.PublicBusinessItem;
import com.simhastha.model.PublicBusinessListing;
import com.simhastha.util.AppSession;
import com.simhastha.util.NavigationUtil;

import java.awt.Desktop;
import java.net.URL;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardPage {

    private BorderPane root;
    private final AppPaymentCoordinator paymentCoordinator = new AppPaymentCoordinator();
    private final BusinessMarketplaceController businessMarketplaceController = new BusinessMarketplaceController();
    private final java.util.Map<String, Button> navButtons = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, Place> TRANSPORT_PLACES = createTransportPlaces();
    private List<PublicBusinessListing> marketplaceBusinesses = List.of();
    private FlowPane marketplaceGrid;
    private Label marketplaceStatus;
    private TextField marketplaceSearch;
    private HBox marketplaceStatsStrip;
    private String selectedMarketplaceCategory = "All";

    public Scene createScene(Stage stage) {
        root = new BorderPane();
        root.getStyleClass().add("pilgrim-dashboard-root");
        root.setLeft(createSidebar(stage));
        showHomePage();

        Scene scene = new Scene(root, 1200, 680);
        addTheme(scene);
        return scene;
    }

    private VBox createSidebar(Stage stage) {
        ImageView logo = createImage("/images/sclogo.png", 54, 54, 0.5, 0.5);
        logo.getStyleClass().add("pilgrim-sidebar-logo-image");

        Label name = new Label("SIMHASTHA\nCONNECT");
        name.getStyleClass().add("pilgrim-sidebar-brand-strong");
        Label event = new Label("Nashik Simhastha 2027");
        event.getStyleClass().add("pilgrim-sidebar-tagline");

        HBox brand = new HBox(10, logo, new VBox(1, name, event));
        brand.getStyleClass().add("pilgrim-sidebar-brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        VBox menu = new VBox(4,
                nav("home", "Dashboard", true),
                nav("packages", "Kumbh Packages", false),
                nav("transport", "Transport", false),
                nav("puja", "Puja Services", false),
                nav("ghat", "Ghats & Snan", false),
                nav("emergency", "Emergency", false),
                nav("stay", "Stay", false),
                nav("lost", "Lost & Found", false),
                nav("schedule", "All Day Schedule", false),
                nav("business", "Business", false),
                nav("bookings", "My Bookings", false),
                nav("announcement", "Announcement", false),
                nav("about", "About Us", false));

        VBox support = new VBox(4, smallGold("24/7 Support"), muted("Emergency help and official information"));
        support.getStyleClass().add("pilgrim-support-box");

        Button logout = sidebarAction("logout", "Logout");
        logout.setOnAction(eventAction -> {
            AppSession.clear();
            NavigationUtil.navigate(stage, new UserAuthPage().createScene(stage));
        });

        VBox sidebar = new VBox(12, brand, menu, createSpacer(), support, logout);
        sidebar.getStyleClass().add("pilgrim-sidebar");
        sidebar.setPadding(new Insets(15, 13, 14, 13));
        sidebar.setPrefWidth(238);
        return sidebar;
    }

    private Button nav(String module, String text, boolean active) {
        Button button = new Button(text);
        button.setGraphic(moduleIcon(module, "pilgrim-nav-icon"));
        button.getStyleClass().add("pilgrim-nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        navButtons.put(module, button);
        setNavSelected(module, active);
        button.setOnAction(event -> {
            if ("home".equals(module)) {
                showHomePage();
            } else {
                showModulePage(module);
            }
        });
        return button;
    }

    private Button sidebarAction(String module, String text) {
        Button button = new Button(text);
        button.setGraphic(moduleIcon(module, "pilgrim-nav-icon-danger"));
        button.getStyleClass().add("pilgrim-sidebar-action");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private Label moduleIcon(String module, String styleClass) {
        String glyph = switch (module) {
            case "home" -> "\uE80F";
            case "packages" -> "\uE8EC";
            case "transport" -> "\uE806";
            case "puja" -> "\uEC29";
            case "ghat" -> "\uE707";
            case "emergency" -> "\uE95E";
            case "stay" -> "\uE809";
            case "lost" -> "\uE721";
            case "schedule" -> "\uE787";
            case "business" -> "\uE719";
            case "bookings" -> "\uE8A7";
            case "announcement" -> "\uE789";
            case "about" -> "\uE946";
            case "logout" -> "\uE7E8";
            default -> "\uE8A5";
        };
        return AppUi.symbolIcon(glyph, styleClass);
    }

    private void showHomePage() {
        setActiveModule("home");
        root.setCenter(scroll(createHomePage()));
    }

    private void showModulePage(String module) {
        setActiveModule(module);
        Node page = switch (module) {
            case "packages" -> packagesPage();
            case "transport" -> transportPage();
            case "puja" -> pujaPage();
            case "ghat" -> ghatsPage();
            case "emergency" -> emergencyPage();
            case "stay" -> stayPage();
            case "lost" -> lostFoundPage();
            case "schedule" -> schedulePage();
            case "business" -> businessPage();
            case "bookings" -> myBookingsPage();
            case "announcement" -> announcementPage();
            case "about" -> aboutPage();
            default -> genericModulePage(module);
        };
        root.setCenter(scroll(page));
    }

    private void setActiveModule(String module) {
        navButtons.keySet().forEach(key -> setNavSelected(key, key.equals(module)));
    }

    private void setNavSelected(String module, boolean selected) {
        Button button = navButtons.get(module);
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("pilgrim-nav-button", "pilgrim-nav-button-active");
        button.getStyleClass().add(selected ? "pilgrim-nav-button-active" : "pilgrim-nav-button");
        button.setGraphic(moduleIcon(module, selected ? "pilgrim-nav-icon-active" : "pilgrim-nav-icon"));
    }

    private ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("pilgrim-dashboard-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox createHomePage() {
        HBox services = new HBox(12,
                serviceCard("packages", "Kumbh Packages", "Guided paid plans"),
                serviceCard("transport", "Transport", "Routes & Timings"),
                serviceCard("puja", "Puja Services", "Book Puja & Pandit"),
                serviceCard("ghat", "Ghats & Snan", "Bathing Places"),
                serviceCard("emergency", "Emergency", "Quick Assistance"),
                serviceCard("stay", "Stay", "Hotels & Dharamshalas"),
                serviceCard("business", "Business", "Approved Local Services"));
        services.setAlignment(Pos.CENTER);

        HBox lower = new HBox(14, liveUpdatesPanel(), divineNashikCard());
        HBox.setHgrow(lower.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lower.getChildren().get(1), Priority.ALWAYS);

        HBox metrics = new HBox(12,
                metric("1.2L+", "Pilgrims Served"),
                metric("86", "Services Available"),
                metric("42", "Help Points"),
                metric("24/7", "Support"));

        return pageShell("Dashboard", "Official Nashik Simhastha 2027 control and information platform",
                searchBar(), services, announcementTicker(), lower, metrics);
    }

    private HBox topControls() {
        Label title = new Label("Pilgrim Dashboard");
        title.getStyleClass().add("pilgrim-page-title");

        HBox actions = new HBox(10, title, createSpacer(), AppUi.createThemeToggle(),
                roundButton("\uE7F4", "Notifications"), roundButton("\uE77B", "Profile"));
        actions.getStyleClass().add("pilgrim-top-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private StackPane photoHeader(String titleText, String subtitleText) {
        ImageView image = createImage("/images/welcome-light.png", 980, 148, 0.54, 0.48);
        image.getStyleClass().add("pilgrim-hero-image");

        VBox copy = new VBox(3,
                label("SIMHASTHA CONNECT", "pilgrim-hero-title"),
                label("ONE PLATFORM FOR A BETTER SIMHASTHA EXPERIENCE", "pilgrim-hero-subtitle"),
                label("॥ ॐ नमः शिवाय ॥", "pilgrim-hero-mantra"),
                label(titleText + " • " + subtitleText, "pilgrim-hero-detail"));
        copy.setAlignment(Pos.CENTER);
        copy.setPadding(new Insets(16));

        StackPane hero = new StackPane(image, copy);
        hero.getStyleClass().add("pilgrim-hero");
        hero.setMinHeight(148);
        return hero;
    }

    private HBox searchBar() {
        TextField search = AppUi.textField("Search services, places, events...");
        search.getStyleClass().add("pilgrim-search-field");
        Button button = new Button("Search");
        button.getStyleClass().add("pilgrim-search-button");
        HBox row = new HBox(0, search, button);
        row.getStyleClass().add("pilgrim-search-bar");
        row.setMaxWidth(560);
        HBox.setHgrow(search, Priority.ALWAYS);
        return row;
    }

    private VBox serviceCard(String module, String title, String detail) {
        VBox card = new VBox(8, moduleIcon(module, "pilgrim-card-icon"), strong(title), muted(detail), arrowAction("View"));
        card.getStyleClass().addAll("pilgrim-module-card", "module-" + module);
        card.setMinSize(132, 104);
        card.setOnMouseClicked(event -> showModulePage(module));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private StackPane announcementTicker() {
        Label text = new Label("Important announcements, emergency alerts and official notices will appear here.");
        text.getStyleClass().add("pilgrim-ticker-text");
        StackPane ticker = new StackPane(text);
        ticker.getStyleClass().add("pilgrim-alert-ticker");
        ticker.setMinHeight(30);
        TranslateTransition transition = new TranslateTransition(Duration.seconds(18), text);
        transition.setFromX(700);
        transition.setToX(-700);
        transition.setCycleCount(Animation.INDEFINITE);
        transition.setInterpolator(Interpolator.LINEAR);
        transition.play();
        return ticker;
    }

    private VBox liveUpdatesPanel() {
        VBox rows = new VBox(8);
        List<AppDataStore.ServiceItem> updates = AppDataStore.hasRemoteItems("announcement")
                ? AppDataStore.items("announcement")
                : List.of();
        if (updates.isEmpty()) {
            rows.getChildren().add(dataRow("announcement", "No live updates right now",
                    "Official updates from Admin will appear here."));
        } else {
            updates.stream().limit(4).forEach(item -> rows.getChildren().add(dataRow(item.module, item.title, item.detail)));
        }
        VBox panel = new VBox(12, sectionTitle("Live Updates"), rows);
        panel.getStyleClass().add("pilgrim-panel");
        panel.setMinHeight(190);
        return panel;
    }

    private StackPane divineNashikCard() {
        ImageView image = createImage("/images/godavari_kumbh.jpg", 440, 190, 0.62, 0.58);
        image.getStyleClass().add("pilgrim-experience-image");
        VBox text = new VBox(7,
                smallGold("Experience"),
                label("Divine Nashik", "pilgrim-experience-title"),
                muted("Plan your pilgrimage with transport, stay, puja, ghats and emergency services in one dashboard."),
                arrowAction("Explore Services"));
        text.setPadding(new Insets(18));
        text.setMaxWidth(315);
        StackPane.setAlignment(text, Pos.CENTER_LEFT);
        StackPane panel = new StackPane(image, text);
        panel.getStyleClass().add("pilgrim-experience-panel");
        panel.setMinHeight(190);
        return panel;
    }

    private VBox transportPage() {
        Node officialRoutes = AppDataStore.items("transport").isEmpty()
                ? infoPanel("Government Transport", "Official route information, bus schedules and public crowd movement guidance remain free.")
                : adminControlledGrid("transport", "Open Route");
        return pageShell("Transport Services", "Bus, train, flight and last-mile Kumbh movement planner.",
                transportJourneyPlanner(),
                transportQuickStatus(),
                officialRoutes,
                twoColumnGrid(
                        richCard("transport", "Ozar Airport Arrival Plan",
                                "Ozar Airport to Nashik Road / CBS connector guidance. Public information only.", "Free Info"),
                        paidCard("transport", "Private Cab Reservation",
                                "Optional paid private cab booking for approved service providers.", "Book Cab",
                                "transport-private-cab", 1, 1)),
                transportSolutionsGrid());
    }

    private VBox packagesPage() {
        if (!AppDataStore.items("packages").isEmpty()) {
            return pageShell("Kumbh Packages", "Approved paid packages connected to the centralized payment system.",
                    adminControlledGrid("packages", "Pay Securely"));
        }
        return pageShell("Kumbh Packages", "Approved paid packages connected to the centralized payment system.",
                twoColumnGrid(
                        paidCard("packages", "Divine Nashik Package",
                                "Ramkund, Trimbakeshwar and guided darshan support\nTraveller pass generated after verified payment.",
                                "Pay Securely", "package-divine-nashik", 1, 1),
                        paidCard("packages", "Family Seva Package",
                                "Family assistance, route help and puja desk coordination\nIncludes a reusable booking pass.",
                                "Pay Securely", "package-family-seva", 1, 1)));
    }

    private VBox transportJourneyPlanner() {
        ComboBox<String> from = locationCombo("From");
        from.setValue("Nashik Road Railway Station");
        ComboBox<String> to = locationCombo("To");
        to.setValue("Ramkund Ghat");

        ComboBox<String> mode = new ComboBox<>();
        mode.getItems().addAll("All Modes", "Bus", "Train", "Flight", "Citylink + Walk", "Private Cab");
        mode.setValue("All Modes");
        mode.getStyleClass().add("journey-combo");

        ComboBox<String> travellers = new ComboBox<>();
        travellers.getItems().addAll("1 Adult", "2 Adults", "Family", "Senior Citizen", "Group");
        travellers.setValue("1 Adult");
        travellers.getStyleClass().add("journey-combo");

        DatePicker date = new DatePicker(java.time.LocalDate.now());
        date.getStyleClass().add("journey-field");
        date.setPromptText("Journey Date");

        Button swap = new Button("\uE8AB");
        swap.getStyleClass().add("journey-swap-button");
        swap.setOnAction(event -> {
            String oldFrom = from.getValue();
            from.setValue(to.getValue());
            to.setValue(oldFrom);
        });

        Button search = new Button("Search");
        search.setGraphic(moduleIcon("lost", "journey-search-icon"));
        search.getStyleClass().add("journey-search-button");

        HBox tripType = new HBox(10, badge("One Way"), badge("Round Trip"), badge("Snan Day"), badge("Senior Friendly"));
        tripType.getStyleClass().add("journey-trip-row");

        HBox inputs = new HBox(12,
                journeyInput("FROM", from, "\uE707"),
                swap,
                journeyInput("TO", to, "\uE707"),
                journeyInput("JOURNEY DATE", date, "\uE787"),
                journeyInput("PASSENGERS", travellers, "\uE716"),
                journeyInput("MODE", mode, "\uE806"),
                search);
        inputs.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputs.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(inputs.getChildren().get(2), Priority.ALWAYS);

        VBox results = new VBox(10);
        StackPane map = transportMapPreview("Nashik Road Railway Station", "Ramkund Ghat", "All Modes");
        updateTransportResults(results, map, "Nashik Road Railway Station", "Ramkund Ghat", "All Modes");

        search.setOnAction(event -> {
            String source = from.getValue() == null || from.getValue().isBlank()
                    ? "Nashik Road Railway Station"
                    : from.getValue().trim();
            String destination = to.getValue() == null || to.getValue().isBlank()
                    ? "Ramkund Ghat"
                    : to.getValue().trim();
            String selectedMode = mode.getValue() == null ? "All Modes" : mode.getValue();
            updateTransportResults(results, map, source, destination, selectedMode);
        });

        HBox output = new HBox(14, results, map);
        HBox.setHgrow(results, Priority.ALWAYS);
        HBox.setHgrow(map, Priority.ALWAYS);

        VBox planner = new VBox(14, tripType, inputs, output);
        planner.getStyleClass().add("journey-planner");
        return planner;
    }

    private ComboBox<String> locationCombo(String prompt) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText(prompt);
        combo.getItems().addAll(TRANSPORT_PLACES.keySet());
        combo.getStyleClass().add("journey-combo");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private VBox journeyInput(String title, Node field, String icon) {
        Label iconLabel = AppUi.symbolIcon(icon, "journey-field-icon");
        VBox text = new VBox(3, smallGold(title), field);
        HBox row = new HBox(9, iconLabel, text);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(row);
        box.getStyleClass().add("journey-input-box");
        HBox.setHgrow(text, Priority.ALWAYS);
        if (field instanceof TextField textField) {
            textField.setMaxWidth(Double.MAX_VALUE);
        } else if (field instanceof ComboBox<?> comboBox) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
        } else if (field instanceof DatePicker datePicker) {
            datePicker.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private void updateTransportResults(VBox results, StackPane map, String source, String destination, String mode) {
        Place fromPlace = resolvePlace(source);
        Place toPlace = resolvePlace(destination);
        results.getChildren().setAll(
                sectionTitle("Best Route"),
                transportRouteCard("Recommended", source + " → Nashik Citylink Hub → " + destination,
                        buildTransitGuidance(mode, fromPlace, toPlace),
                        "Open map", "Official route", fromPlace, toPlace),
                transportRouteCard("City Bus", nearestHub(fromPlace) + " → CBS / Citylink → " + destination,
                        "Use Citilinc live tracking or the official city bus app for current stop, route and arrival status.",
                        "Citilinc", "Live source", fromPlace, toPlace),
                transportRouteCard("Cab / Auto", source + " → approved drop zone near " + destination,
                        "Use cab/auto only till permitted drop zones. Final movement may be walking-only during high crowd control.",
                        "Cab apps", "Drop zone", fromPlace, toPlace));
        map.getChildren().setAll(transportMapLayer(source, destination, mode));
    }

    private VBox transportRouteCard(String tag, String route, String detail, String metric, String status, Place from, Place to) {
        HBox top = new HBox(8, moduleIcon("transport", "pilgrim-card-icon"), badge(tag), createSpacer(), badge(status));
        Button open = new Button("Open Route  >");
        open.getStyleClass().add("pilgrim-small-action");
        open.setOnAction(event -> openExternalMap(from, to));
        VBox card = new VBox(8, top, strong(route), paragraph(detail), new HBox(8, badge(metric), open));
        card.getStyleClass().add("transport-route-card");
        card.setOnMouseClicked(event -> openExternalMap(from, to));
        return card;
    }

    private StackPane transportMapPreview(String source, String destination, String mode) {
        StackPane map = new StackPane();
        map.getStyleClass().add("transport-map-shell");
        map.getChildren().setAll(transportMapLayer(source, destination, mode));
        return map;
    }

    private StackPane transportMapLayer(String source, String destination, String mode) {
        Place fromPlace = resolvePlace(source);
        Place toPlace = resolvePlace(destination);
        ImageView mapImage = staticMapImage(fromPlace, toPlace);

        Line route = new Line(70, 238, 300, 88);
        route.getStyleClass().add("transport-route-line");
        Circle startDot = mapDot(70, 238, "transport-dot-start");
        Circle endDot = mapDot(300, 88, "transport-dot-end");

        VBox start = mapPin("Start", fromPlace.name);
        VBox finish = mapPin("Kumbh", toPlace.name);
        StackPane.setAlignment(start, Pos.BOTTOM_LEFT);
        StackPane.setMargin(start, new Insets(0, 0, 22, 18));
        StackPane.setAlignment(finish, Pos.TOP_RIGHT);
        StackPane.setMargin(finish, new Insets(24, 20, 0, 0));

        Button openRoute = new Button("Open Live Map");
        openRoute.getStyleClass().add("map-open-button");
        openRoute.setOnAction(event -> openExternalMap(fromPlace, toPlace));

        Button citilinc = new Button("Citilinc Live");
        citilinc.getStyleClass().add("map-secondary-button");
        citilinc.setOnAction(event -> openUrl("https://citilinc.nmc.gov.in/"));

        HBox tools = new HBox(8, badge(mode), createSpacer(), citilinc, openRoute);
        tools.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(tools, Pos.TOP_LEFT);
        StackPane.setMargin(tools, new Insets(12));

        VBox note = new VBox(3,
                strong("Real Map View"),
                muted("OpenStreetMap map. Use live route button for current directions."));
        note.getStyleClass().add("transport-map-note");
        StackPane.setAlignment(note, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(note, new Insets(0, 14, 14, 0));

        StackPane map = new StackPane(transportMapFallback(fromPlace, toPlace), mapImage, route, startDot, endDot, start,
                finish, tools, note);
        map.getStyleClass().add("transport-map-content");
        return map;
    }

    private Pane transportMapFallback(Place from, Place to) {
        Pane pane = new Pane();
        pane.getStyleClass().add("transport-map-fallback");
        pane.setPrefSize(480, 330);

        for (int i = 0; i < 8; i++) {
            Line horizontal = new Line(0, 38 + i * 38, 480, 22 + i * 38);
            horizontal.getStyleClass().add("transport-map-road");
            pane.getChildren().add(horizontal);
        }
        for (int i = 0; i < 7; i++) {
            Line vertical = new Line(38 + i * 70, 0, 16 + i * 70, 330);
            vertical.getStyleClass().add("transport-map-road-soft");
            pane.getChildren().add(vertical);
        }

        Line river = new Line(0, 220, 480, 150);
        river.getStyleClass().add("transport-map-river");
        pane.getChildren().add(river);

        Label start = label(from.name, "transport-map-place-label");
        start.setLayoutX(22);
        start.setLayoutY(252);
        Label end = label(to.name, "transport-map-place-label");
        end.setLayoutX(300);
        end.setLayoutY(62);
        Label city = label("Nashik Kumbh Route", "transport-map-city-label");
        city.setLayoutX(154);
        city.setLayoutY(145);
        pane.getChildren().addAll(start, end, city);
        return pane;
    }

    private VBox mapPin(String title, String place) {
        VBox pin = new VBox(4, AppUi.symbolIcon("\uE707", "transport-map-pin"), strong(title), muted(place));
        pin.getStyleClass().add("transport-map-pin-box");
        pin.setAlignment(Pos.CENTER);
        return pin;
    }

    private ImageView staticMapImage(Place from, Place to) {
        Image map = new Image(staticMapUrl(from, to), true);
        ImageView imageView = new ImageView(map);
        imageView.getStyleClass().add("transport-map-image");
        imageView.setFitWidth(480);
        imageView.setFitHeight(330);
        imageView.setPreserveRatio(false);
        map.errorProperty().addListener((observable, wasError, isError) -> imageView.setVisible(!isError));
        return imageView;
    }

    private String staticMapUrl(Place from, Place to) {
        double centerLat = (from.lat + to.lat) / 2.0;
        double centerLon = (from.lon + to.lon) / 2.0;
        return "https://staticmap.openstreetmap.de/staticmap.php?center=" + centerLat + "," + centerLon
                + "&zoom=12&size=520x330&markers="
                + from.lat + "," + from.lon + ",red-pushpin%7C"
                + to.lat + "," + to.lon + ",blue-pushpin";
    }

    private HBox transportQuickStatus() {
        HBox row = new HBox(12,
                sourceTile("Citilinc", "Official city bus website", "https://citilinc.nmc.gov.in/"),
                sourceTile("Live Tracking", "Use official bus live source", "https://citilinc.nmc.gov.in/"),
                sourceTile("Railway", "Open station route on map", "https://www.openstreetmap.org/search?query=Nashik%20Road%20Railway%20Station"),
                sourceTile("Airport", "Open Ozar airport route", "https://www.openstreetmap.org/search?query=Ozar%20Airport%20Nashik"));
        return row;
    }

    private VBox sourceTile(String title, String detail, String url) {
        Button open = new Button("Open");
        open.getStyleClass().add("pilgrim-small-action");
        open.setOnAction(event -> openUrl(url));
        VBox tile = new VBox(4, label(title, "pilgrim-stat-value"), muted(detail), open);
        tile.getStyleClass().add("pilgrim-stat-card");
        tile.setOnMouseClicked(event -> openUrl(url));
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    private GridPane osmTileGrid(double lat, double lon, int zoom) {
        int centerX = lonToTileX(lon, zoom);
        int centerY = latToTileY(lat, zoom);
        GridPane grid = new GridPane();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int tileX = centerX + col - 1;
                int tileY = centerY + row - 1;
                ImageView tile = new ImageView("https://tile.openstreetmap.org/" + zoom + "/" + tileX + "/" + tileY + ".png");
                tile.setFitWidth(160);
                tile.setFitHeight(160);
                tile.setPreserveRatio(false);
                grid.add(tile, col, row);
            }
        }
        return grid;
    }

    private int lonToTileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }

    private int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0
                * (1 << zoom));
    }

    private Circle mapDot(double x, double y, String styleClass) {
        Circle circle = new Circle(7);
        circle.setTranslateX(x - 240);
        circle.setTranslateY(y - 160);
        circle.getStyleClass().add(styleClass);
        return circle;
    }

    private String buildTransitGuidance(String mode, Place source, Place destination) {
        if ("Flight".equals(mode) || source.name.toLowerCase().contains("airport")) {
            return "Airport arrival: use approved cab/airport connector till Nashik city hub, then Citilinc or official shuttle towards "
                    + destination.name + ". Open map for current road route.";
        }
        if ("Train".equals(mode) || source.name.toLowerCase().contains("railway")) {
            return "Railway arrival: exit towards official help booth, check Citilinc/city bus live source, then use shuttle or marked walking corridor near "
                    + destination.name + ".";
        }
        if ("Bus".equals(mode) || source.name.toLowerCase().contains("cbs")) {
            return "Bus arrival: use CBS / Citylink hub for local transfer. Follow police diversion and shuttle boarding signs for "
                    + destination.name + ".";
        }
        return "Use official map route first, then prefer Citilinc/city bus or marked Kumbh shuttle. Walking-only gates and drop zones may change on crowd days.";
    }

    private String nearestHub(Place place) {
        if (place.name.toLowerCase().contains("airport")) {
            return "Ozar Airport";
        }
        if (place.name.toLowerCase().contains("railway")) {
            return "Nashik Road Station";
        }
        if (place.name.toLowerCase().contains("trimbak")) {
            return "Trimbakeshwar Bus Stand";
        }
        return "CBS Nashik";
    }

    private Place resolvePlace(String input) {
        if (input == null || input.isBlank()) {
            return TRANSPORT_PLACES.get("Ramkund Ghat");
        }
        String normalized = input.toLowerCase().trim();
        for (java.util.Map.Entry<String, Place> entry : TRANSPORT_PLACES.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.equals(normalized) || key.contains(normalized) || normalized.contains(key)) {
                return entry.getValue();
            }
        }
        if (normalized.contains("kumbh") || normalized.contains("khumbh")) {
            return TRANSPORT_PLACES.get("Ramkund Ghat");
        }
        if (normalized.contains("rail") || normalized.contains("station")) {
            return TRANSPORT_PLACES.get("Nashik Road Railway Station");
        }
        if (normalized.contains("airport") || normalized.contains("ozar")) {
            return TRANSPORT_PLACES.get("Ozar Airport");
        }
        if (normalized.contains("bus") || normalized.contains("cbs")) {
            return TRANSPORT_PLACES.get("CBS Nashik");
        }
        if (normalized.contains("trimb")) {
            return TRANSPORT_PLACES.get("Trimbakeshwar Temple");
        }
        return new Place(input, 19.9975, 73.7898);
    }

    private void openExternalMap(Place from, Place to) {
        String url = "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route="
                + from.lat + "%2C" + from.lon + "%3B" + to.lat + "%2C" + to.lon;
        openUrl(url);
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                showInfo("Open Link", url);
            }
        } catch (Exception exception) {
            showInfo("Open Link", url);
        }
    }

    private static java.util.Map<String, Place> createTransportPlaces() {
        java.util.Map<String, Place> places = new java.util.LinkedHashMap<>();
        places.put("Ramkund Ghat", new Place("Ramkund Ghat", 20.0059, 73.7890));
        places.put("Panchavati", new Place("Panchavati", 20.0067, 73.7914));
        places.put("Kalaram Mandir", new Place("Kalaram Mandir", 20.0076, 73.7907));
        places.put("Tapovan", new Place("Tapovan", 20.0132, 73.8080));
        places.put("Tapovan Parking", new Place("Tapovan Parking", 20.0180, 73.8095));
        places.put("Trimbakeshwar Temple", new Place("Trimbakeshwar Temple", 19.9322, 73.5303));
        places.put("Kushavarta Kund", new Place("Kushavarta Kund", 19.9325, 73.5309));
        places.put("Nashik Road Railway Station", new Place("Nashik Road Railway Station", 19.9476, 73.8421));
        places.put("CBS Nashik", new Place("CBS Nashik", 19.9975, 73.7898));
        places.put("Mahamarg Bus Stand", new Place("Mahamarg Bus Stand", 19.9944, 73.7847));
        places.put("Ozar Airport", new Place("Ozar Airport", 20.1191, 73.9129));
        places.put("Mumbai", new Place("Mumbai", 19.0760, 72.8777));
        places.put("Pune", new Place("Pune", 18.5204, 73.8567));
        places.put("Shirdi", new Place("Shirdi", 19.7669, 74.4774));
        places.put("Saptashrungi Gad", new Place("Saptashrungi Gad", 20.3900, 73.9070));
        return places;
    }

    private record Place(String name, double lat, double lon) {
    }

    private GridPane transportSolutionsGrid() {
        return twoColumnGrid(
                richCard("transport", "Ozar Airport Arrival Plan",
                        "Ozar Airport → Nashik Road / CBS connector\nApproved cab and bus options\nRecommended: pre-book city transfer, then shuttle to Ramkund",
                        "Flight"),
                richCard("transport", "Nashik Road Railway Station Plan",
                        "Station exit → official help booth\nCitylink / shuttle pickup guidance\nLuggage counter and senior assistance markers",
                        "Train"),
                richCard("transport", "Intercity Bus Arrival Plan",
                        "CBS Nashik → Ramkund / Panchavati shuttle\nTrimbakeshwar buses separated by crowd route\nQueue and platform guidance",
                        "Bus"),
                richCard("transport", "Peak-Day Movement Problems Solved",
                        "Road closures\nWalking-only zones\nFamily regroup points\nEmergency lane awareness\nAlternate ghat route suggestions",
                        "Smart"));
    }

    private VBox pujaPage() {
        if (!AppDataStore.items("puja").isEmpty()) {
            return pageShell("Puja Services", "Traditional puja support, verified counters and darshan guidance.",
                    adminControlledGrid("puja", "View Details"));
        }
        return pageShell("Puja Services", "Traditional puja support, verified counters and darshan guidance.",
                twoColumnGrid(
                        paidCard("puja", "Ramkund Rudrabhishek Help Desk", "Pandit Booking\nReceipt Guidance\nPuja Slot Assistance", "Pay Securely", "puja-rudrabhishek", 1, 1),
                        paidCard("puja", "Trimbakeshwar Darshan Support", "Temple Direction\nPuja Counter Guidance\nElderly Assistance", "Pay Securely", "puja-trimbakeshwar-darshan", 1, 1),
                        richCard("puja", "Pind Daan Information Counter", "Ritual Requirements\nTiming Windows\nVerified Contact Support", "Timings")));
    }

    private VBox ghatsPage() {
        if (!AppDataStore.items("ghat").isEmpty()) {
            return pageShell("Ghats & Snan", "Ramkund, Godavari and Trimbakeshwar snan information.",
                    crowdStatus(), adminControlledGrid("ghat", "Official Update"));
        }
        return pageShell("Ghats & Snan", "Ramkund, Godavari and Trimbakeshwar snan information.",
                crowdStatus(),
                twoColumnGrid(
                        richCard("ghat", "Ramkund Ghat", "Primary Nashik Snan Ghat\nCrowd Level: High\nSafety Point available", "High"),
                        richCard("ghat", "Kushavarta Kund, Trimbakeshwar", "Important Snan Location\nAkhara focus\nMorning hours preferred", "Moderate"),
                        richCard("ghat", "Godavari Ghat Safety Line", "Marked bathing points\nPolice barricades\nMedical support nearby", "Guide"),
                        infoPanel("Snan Guidelines", "Use marked entry points only.\nKeep children and elders together.\nFollow police and volunteer instructions.")));
    }

    private VBox emergencyPage() {
        if (!AppDataStore.items("emergency").isEmpty()) {
            return pageShell("Emergency & Medical", "Important emergency numbers and safety support.",
                    infoPanel("Emergency Safety Tips", "Stay calm\nFollow police instructions\nUse official help booths\nKeep ID with you\nAvoid overcrowded routes"),
                    adminControlledGrid("emergency", "Verified"));
        }
        return pageShell("Emergency & Medical", "Important emergency numbers and safety support.",
                infoPanel("Emergency Safety Tips", "Stay calm\nFollow police instructions\nUse official help booths\nKeep ID with you\nAvoid overcrowded routes"),
                twoColumnGrid(
                        emergencyCard("Unified Emergency", "112", "Police, fire and medical help"),
                        emergencyCard("Ambulance", "108", "Medical emergency support"),
                        emergencyCard("Police", "100 / 112", "Nashik city emergency control"),
                        emergencyCard("Fire", "101", "Fire and rescue support"),
                        emergencyCard("Women Helpline", "1091", "Women safety support"),
                        emergencyCard("Child Helpline", "1098", "Child safety support"),
                        emergencyCard("Cyber Crime", "1930", "Cyber fraud helpline"),
                        emergencyCard("District Disaster Management Nashik", "1077", "Disaster control support")));
    }

    private VBox stayPage() {
        if (!AppDataStore.items("stay").isEmpty()) {
            return pageShell("Stay & Accommodation", "Hotels, dharamshalas and camp information.",
                    filterRow("Area", "Type", "Budget"),
                    adminControlledGrid("stay", "View Details"));
        }
        return pageShell("Stay & Accommodation", "Hotels, dharamshalas and camp information.",
                filterRow("Area", "Type", "Budget"),
                twoColumnGrid(
                        paidCard("stay", "Dharamshala Availability Desk", "Panchavati / Trimbakeshwar\nBudget\nPilgrim-friendly", "Reserve", "stay-dharamshala-bed", 1, 1),
                        paidCard("stay", "Family Hotel Zone", "Nashik Road\nCBS\nGangapur Road", "Reserve", "stay-family-room", 1, 1),
                        paidCard("stay", "Festival Camp Stay", "Temporary camps\nVerified group stay support", "Reserve", "stay-festival-tent", 1, 1),
                        infoPanel("Area Guide", "Panchavati: close to Ramkund\nNashik Road: rail access\nTrimbakeshwar: temple-focused stay")));
    }

    private VBox lostFoundPage() {
        HBox actions = new HBox(12,
                richCard("lost", "Report Lost Person", "Name, age, clothing and last seen area.", "Report"),
                richCard("lost", "Report Found Item", "Item type, location found and contact counter.", "Submit"));
        HBox.setHgrow(actions.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(actions.getChildren().get(1), Priority.ALWAYS);

        return pageShell("Lost & Found", "Official support for lost persons and found items.",
                actions,
                filterRow("Item/Person Type", "Last Seen Area", "Date", "Search"),
                twoColumnGrid(
                        richCard("lost", "Lost Person Help Desk", "Report with clear identity details and contact number.", "Help"),
                        richCard("lost", "Found Item Counter", "Submit items only at verified counters.", "Counter"),
                        richCard("announcement", "Public Announcement Support", "Official announcement support for urgent cases.", "Notice")));
    }

    private VBox schedulePage() {
        if (!AppDataStore.items("schedule").isEmpty()) {
            return pageShell("KUMBH ALL DAY SCHEDULE", "Daily movement, snan, seva and security schedule.",
                    filterRow("Select Date", "Select Location", "Filter by Category", "Download Schedule"),
                    adminControlledGrid("schedule", "Official Update"));
        }
        HBox body = new HBox(14, scheduleTable(),
                new VBox(10,
                        infoPanel("Today Highlights", "Morning snan guidance\nEvening aarti crowd movement\nNight patrol after 10 PM"),
                        infoPanel("Important Information", "Carry ID\nUse official route diversions\nKeep water with you"),
                        infoPanel("Emergency Contacts", "112 Unified Emergency\n108 Ambulance\n100 Police"),
                        infoPanel("Weather Update", "Demo weather panel for official advisory display")));
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return pageShell("KUMBH ALL DAY SCHEDULE", "Daily movement, snan, seva and security schedule.",
                filterRow("Select Date", "Select Location", "Filter by Category", "Download Schedule"), body);
    }

    private VBox businessPage() {
        selectedMarketplaceCategory = "All";
        marketplaceBusinesses = List.of();

        marketplaceSearch = AppUi.textField("Search hotels, tents, parking, food, puja services...");
        marketplaceSearch.getStyleClass().add("marketplace-search-field");
        marketplaceSearch.textProperty().addListener((observable, oldValue, newValue) -> renderMarketplaceResults());
        marketplaceSearch.setOnAction(event -> renderMarketplaceResults());

        Label searchIcon = AppUi.symbolIcon("\uE721", "marketplace-hero-search-icon");
        HBox searchBox = new HBox(10, marketplaceSearch, searchIcon);
        searchBox.getStyleClass().add("marketplace-hero-search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(marketplaceSearch, Priority.ALWAYS);

        Button nearMe = new Button("Near Me", AppUi.symbolIcon("\uE707", "marketplace-button-icon"));
        nearMe.getStyleClass().add("marketplace-secondary-button");
        nearMe.setOnAction(event -> openNearbyBusinessMap());

        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("marketplace-search-button");
        searchButton.setOnAction(event -> renderMarketplaceResults());

        HBox search = new HBox(14, searchBox, nearMe, searchButton);
        search.getStyleClass().add("marketplace-search-row");
        search.setAlignment(Pos.CENTER_LEFT);
        search.setMaxWidth(960);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        FlowPane categories = marketplaceCategories();
        marketplaceStatus = label("Loading approved businesses...", "marketplace-state-title");
        marketplaceGrid = new FlowPane(16, 14);
        marketplaceGrid.getStyleClass().add("marketplace-grid");
        marketplaceStatsStrip = marketplaceStatsStrip();

        HBox sectionHeader = new HBox(18, sectionTitle("Popular & Nearby Businesses"), createSpacer(),
                marketplaceTabs(), marketplaceArrows());
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        sectionHeader.getStyleClass().add("marketplace-section-header");

        VBox body = new VBox(14, marketplaceTopBar(), marketplaceHero(search), trustStrip(), categories,
                sectionHeader, marketplaceStatus, marketplaceGrid, marketplaceStatsStrip);
        body.getStyleClass().addAll("pilgrim-dashboard-main", "marketplace-page");
        body.setPadding(new Insets(0, 20, 24, 20));
        loadMarketplaceBusinesses();
        return body;
    }

    private HBox marketplaceTopBar() {
        Button location = new Button("Nashik", AppUi.symbolIcon("\uE707", "marketplace-button-icon"));
        location.getStyleClass().add("marketplace-location-chip");
        location.setOnAction(event -> showInfo("Location", "Nashik is selected for marketplace browsing."));

        Button notifications = marketplaceIconButton("\uE7F4", "Notifications", this::showUserNotificationsPage);
        Button profile = marketplaceIconButton("\uE77B", "Profile", this::showUserProfilePage);

        HBox row = new HBox(14, location, createSpacer(), AppUi.createThemeToggle(), notifications, profile);
        row.getStyleClass().add("marketplace-top-bar");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button marketplaceIconButton(String icon, String tooltip, Runnable action) {
        Button button = new Button();
        button.setGraphic(AppUi.symbolIcon(icon, "marketplace-top-icon"));
        button.getStyleClass().add("marketplace-top-icon-button");
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(event -> action.run());
        return button;
    }

    private void showUserNotificationsPage() {
        VBox notices = new VBox(10,
                dataRow("announcement", "Business marketplace", "Approved business updates and booking alerts will appear here."),
                dataRow("announcement", "Saved searches", "Search and service alerts are ready for your pilgrim account."));
        root.setCenter(scroll(pageShell("Notifications", "Your Simhastha Connect alerts.", notices)));
    }

    private void openNearbyBusinessMap() {
        SimhasthaMapPage mapPage = new SimhasthaMapPage("", false,
                () -> showModulePage("business"), this::openBusinessFromMap);
        root.setCenter(scroll(mapPage.createContent()));
    }

    private void showUserProfilePage() {
        AppSession.User user = AppSession.currentUser();
        String name = user == null ? "Simhastha Pilgrim" : displayName(user);
        String email = user == null ? "Not signed in" : valueOr("Not available", user.email());
        String status = user == null ? "Guest" : valueOr("active", user.status());
        VBox card = new VBox(14,
                new HBox(14, AppUi.symbolIcon("\uE77B", "profile-avatar-icon"),
                        new VBox(4, label(name, "business-detail-title"),
                                label("Pilgrim Account", "business-detail-category"))),
                new ImagePickerPane("Profile Photo (optional)", 1, null),
                profileRow("Email", email),
                profileRow("Role", "User"),
                profileRow("Account Status", status),
                profileRow("Selected City", "Nashik"));
        card.getStyleClass().addAll("pilgrim-panel", "user-profile-card");

        VBox quick = new VBox(10,
                sectionTitle("Quick Access"),
                dataRow("business", "Business Marketplace", "Browse approved local services."),
                dataRow("bookings", "My Bookings", "Paid bookings and tickets stay linked to this account."),
                dataRow("announcement", "Notifications", "Important updates appear from official modules."));
        quick.getStyleClass().add("pilgrim-panel");

        root.setCenter(scroll(pageShell("Profile", "Your pilgrim account overview.", card, quick)));
    }

    private HBox profileRow(String key, String value) {
        HBox row = new HBox(12, label(key, "business-overview-key"), createSpacer(),
                label(value, "business-overview-value"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private StackPane marketplaceHero(HBox search) {
        ImageView image = createImage(ThemeManager.isDark() ? "/images/welcome-dark.png" : "/images/welcome-light.png",
                1080, 300, 0.64, 0.50);
        image.getStyleClass().add("marketplace-hero-image");

        VBox copy = new VBox(8,
                label("Trusted Services for\nYour Simhastha Journey", "marketplace-hero-title"),
                label("Discover approved stays, food, puja services, parking,\nessentials and more - all in one place.",
                        "marketplace-hero-subtitle"));
        copy.setPrefWidth(760);
        copy.setMaxWidth(760);
        copy.setPadding(new Insets(18, 34, 0, 34));

        VBox overlay = new VBox(22, copy, createSpacer(), search);
        overlay.setPadding(new Insets(0, 34, 26, 34));
        overlay.setAlignment(Pos.TOP_LEFT);

        StackPane hero = new StackPane(image, overlay);
        hero.getStyleClass().add("marketplace-hero");
        hero.setMinHeight(300);
        hero.setPrefHeight(300);
        hero.setMaxWidth(Double.MAX_VALUE);
        return hero;
    }

    private HBox trustStrip() {
        HBox row = new HBox(0,
                trustBadge("Administration Approved", "\uE73E"),
                trustBadge("Secure Payments", "\uE72E"),
                trustBadge("Verified Businesses", "\uE8FB"),
                trustBadge("24/7 Support", "\uE95E"));
        row.getStyleClass().add("marketplace-trust-strip");
        row.setMaxWidth(960);
        return row;
    }

    private HBox trustBadge(String text, String icon) {
        HBox badge = new HBox(8, AppUi.symbolIcon(icon, "marketplace-trust-icon"), label(text, "marketplace-trust-text"));
        badge.setAlignment(Pos.CENTER);
        HBox.setHgrow(badge, Priority.ALWAYS);
        return badge;
    }

    private FlowPane marketplaceCategories() {
        FlowPane pane = new FlowPane(10, 10);
        pane.getStyleClass().add("marketplace-category-row");
        for (String category : List.of("All", "Stay", "Tents", "Hotels", "Parking", "Food & Prasadam",
                "Restaurants", "Puja Services", "Puja Items", "Clothes", "Medical Stores", "Toilets",
                "Lockers", "Charging Points", "Local Guides", "Shops", "Essentials", "More")) {
            Button chip = new Button(category, AppUi.symbolIcon(categoryGlyph(category), "marketplace-chip-icon"));
            chip.getStyleClass().add(category.equals(selectedMarketplaceCategory)
                    ? "marketplace-chip-active"
                    : "marketplace-chip");
            chip.setOnAction(event -> {
                selectedMarketplaceCategory = category;
                pane.getChildren().forEach(node -> node.getStyleClass().setAll("marketplace-chip"));
                chip.getStyleClass().setAll("marketplace-chip-active");
                renderMarketplaceResults();
            });
            pane.getChildren().add(chip);
        }
        return pane;
    }

    private void loadMarketplaceBusinesses() {
        if (marketplaceStatus != null) {
            marketplaceStatus.setText("Loading approved businesses...");
        }
        if (marketplaceGrid != null) {
            marketplaceGrid.getChildren().clear();
        }
        AppSession.User user = AppSession.currentUser();
        String token = user == null ? "" : user.idToken();
        businessMarketplaceController.loadApprovedBusinesses(token).whenComplete((businesses, throwable) ->
                Platform.runLater(() -> {
                    if (throwable != null) {
                        marketplaceBusinesses = List.of();
                        updateMarketplaceStats();
                        marketplaceStatus.setText("Businesses could not be loaded. Please try again.");
                        Button retry = new Button("Retry");
                        retry.getStyleClass().add("marketplace-secondary-button");
                        retry.setOnAction(event -> loadMarketplaceBusinesses());
                        marketplaceGrid.getChildren().setAll(retry);
                        return;
                    }
                    marketplaceBusinesses = businesses == null ? List.of() : businesses;
                    marketplaceBusinesses.forEach(AppDataStore::rememberPublicBusiness);
                    updateMarketplaceStats();
                    renderMarketplaceResults();
                }));
    }

    private void renderMarketplaceResults() {
        if (marketplaceGrid == null || marketplaceStatus == null || marketplaceSearch == null) {
            return;
        }
        String query = marketplaceSearch.getText();
        List<PublicBusinessListing> filtered = businessMarketplaceController.filter(
                marketplaceBusinesses, query, selectedMarketplaceCategory);
        marketplaceGrid.getChildren().clear();
        if (filtered.isEmpty()) {
            String activeQuery = query == null ? "" : query.trim();
            String message = marketplaceBusinesses.isEmpty()
                    ? "No verified businesses available yet."
                    : !activeQuery.isBlank()
                            ? "No businesses match your search."
                            : "No verified businesses found in this category.";
            marketplaceStatus.setText(message);
            Button clear = new Button("Clear Filters");
            clear.getStyleClass().add("marketplace-secondary-button");
            clear.setOnAction(event -> {
                marketplaceSearch.clear();
                selectedMarketplaceCategory = "All";
                showModulePage("business");
            });
            marketplaceGrid.getChildren().add(clear);
            return;
        }
        marketplaceStatus.setText(filtered.size() + " approved business" + (filtered.size() == 1 ? "" : "es") + " found.");
        filtered.forEach(business -> marketplaceGrid.getChildren().add(marketplaceCard(business)));
    }

    private VBox marketplaceCard(PublicBusinessListing business) {
        StackPane image = remoteBusinessImage(business);
        Label verified = label("Verified", "marketplace-verified-badge");
        StackPane.setAlignment(verified, Pos.BOTTOM_LEFT);
        StackPane.setMargin(verified, new Insets(0, 0, 12, 12));
        image.getChildren().add(verified);

        VBox text = new VBox(7, label(business.name(), "marketplace-card-title"));
        addIfPresent(text, business.displayCategory(), "marketplace-card-detail", "");
        addIfPresent(text, business.displayLocation(), "marketplace-card-detail", "\uE707 ");
        addIfPresent(text, availabilityText(business), "marketplace-card-detail", "");
        addIfPresent(text, priceText(business), "marketplace-price", "");

        Button details = new Button("View Details");
        details.getStyleClass().add("marketplace-primary-action");
        details.setOnAction(event -> openBusinessDetails(business));

        Button directions = new Button("Directions", AppUi.symbolIcon("\uE707", "marketplace-action-icon"));
        directions.getStyleClass().add("marketplace-secondary-action");
        directions.setDisable(!business.hasCoordinates());
        directions.setTooltip(new Tooltip(business.hasCoordinates()
                ? "Open exact business location"
                : "Location not available yet"));
        directions.setOnAction(event -> openBusinessLocate(business, true));

        HBox actions = new HBox(12, details, directions);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(10, image, text, createSpacer(), actions);
        card.getStyleClass().add("marketplace-card");
        card.setPrefWidth(232);
        card.setMinHeight(300);
        return card;
    }

    private StackPane remoteBusinessImage(PublicBusinessListing business) {
        String cover = business.coverPhotoUrl();
        if (cover != null && !cover.isBlank()) {
            ImageView image = new ImageView(new Image(cover, true));
            image.setFitWidth(232);
            image.setFitHeight(122);
            image.setPreserveRatio(false);
            StackPane pane = new StackPane(image);
            pane.getStyleClass().add("marketplace-card-photo");
            return pane;
        }
        StackPane placeholder = new StackPane(AppUi.symbolIcon(categoryGlyph(business.displayCategory()), "marketplace-image-icon"));
        placeholder.getStyleClass().addAll("marketplace-image-placeholder", "marketplace-card-photo");
        return placeholder;
    }

    private void addIfPresent(VBox box, String value, String styleClass, String prefix) {
        if (value != null && !value.isBlank()) {
            box.getChildren().add(label(prefix + value, styleClass));
        }
    }

    private String priceText(PublicBusinessListing business) {
        if (business.priceRange() != null && !business.priceRange().isBlank()) {
            return business.priceRange();
        }
        return business.items().stream()
                .map(PublicBusinessItem::price)
                .filter(price -> price != null && !price.isBlank() && !"0".equals(price.trim()))
                .findFirst()
                .map(price -> "Starts at \u20B9" + price.trim())
                .orElse("");
    }

    private String availabilityText(PublicBusinessListing business) {
        return business.items().stream()
                .filter(item -> item.availability() != null && !item.availability().isBlank())
                .findFirst()
                .map(item -> {
                    String units = availabilityUnits(item);
                    return units.isBlank() ? item.availability() : units + " available";
                })
                .orElse("");
    }

    private String availabilityUnits(PublicBusinessItem item) {
        if (item.availableUnits() != null && !item.availableUnits().isBlank()
                && item.totalUnits() != null && !item.totalUnits().isBlank()
                && !"0".equals(item.totalUnits().trim())) {
            return item.availableUnits().trim() + " of " + item.totalUnits().trim();
        }
        if (item.stock() != null && !item.stock().isBlank() && !"0".equals(item.stock().trim())) {
            return item.stock().trim();
        }
        return "";
    }

    private void openBusinessDetails(PublicBusinessListing business) {
        PublicBusinessListing selected = latestBusiness(business);
        BusinessDetailsPage detailsPage = new BusinessDetailsPage(selected.businessId(), selected,
                () -> showModulePage("business"),
                () -> openBusinessLocate(selected, false),
                () -> openBusinessLocate(selected, true),
                item -> openBusinessBooking(selected, item));
        root.setCenter(scroll(detailsPage.createContent()));
    }

    private void openBusinessBooking(PublicBusinessListing business, PublicBusinessItem item) {
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            showInfo("Login required", "Please login before creating a booking.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Book " + (item.name() == null ? "Service" : item.name()));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField quantity = new TextField("1");
        quantity.setPromptText("Quantity");
        DatePicker date = new DatePicker(LocalDate.now().plusDays(1));
        VBox form = new VBox(10,
                label("Service", "business-overview-key"), label(item.name(), "pilgrim-card-title"),
                label("Quantity", "business-overview-key"), quantity,
                label("Booking date", "business-overview-key"), date,
                label("Amount: " + amountText(item, quantity.getText())
                        + " | Payment at location", "marketplace-card-detail"));
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);
        dialog.showAndWait().ifPresent(ignored -> saveBusinessBooking(business, item, user, quantity.getText(), date.getValue()));
    }

    private void saveBusinessBooking(PublicBusinessListing business, PublicBusinessItem item, AppSession.User user,
            String quantityText, LocalDate date) {
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText == null ? "" : quantityText.trim());
        } catch (NumberFormatException exception) {
            showInfo("Invalid quantity", "Enter a valid booking quantity.");
            return;
        }
        if (quantity <= 0 || date == null || date.isBefore(LocalDate.now())) {
            showInfo("Invalid booking", "Choose a valid quantity and booking date.");
            return;
        }
        int available = positiveInt(item.availableUnits());
        if (available > 0 && quantity > available) {
            showInfo("Not available", "Only " + available + " unit(s) are currently available.");
            return;
        }
        long amountPaise = pricePaise(item.price()) * quantity;
        String bookingId = "BKG-BUSINESS-" + UUID.randomUUID().toString().substring(0, 8);
        AppDataStore.addBooking(new AppDataStore.BookingRecord(bookingId, user.uid(), "BUSINESS", item.itemId(),
                business.businessId(), item.name(), displayName(user), date.toString(), business.displayLocation(),
                quantity, 1, amountPaise, "INR", "PENDING", "NOT_REQUIRED", "", ""));
        showInfo("Booking request saved", "Your request was sent to the business owner. Payment is at location.");
    }

    private int positiveInt(String value) {
        try { return value == null ? 0 : Math.max(0, Integer.parseInt(value.trim())); }
        catch (NumberFormatException exception) { return 0; }
    }

    private long pricePaise(String value) {
        try { return value == null || value.isBlank() ? 0 : valueAsPaise(new BigDecimal(value.trim())); }
        catch (NumberFormatException exception) { return 0; }
    }

    private long valueAsPaise(BigDecimal amount) { return amount.max(BigDecimal.ZERO).movePointRight(2).longValue(); }

    private String amountText(PublicBusinessItem item, String quantity) {
        long total = pricePaise(item.price()) * Math.max(1, positiveInt(quantity));
        return "₹" + (total / 100);
    }

    private PublicBusinessListing latestBusiness(PublicBusinessListing business) {
        AppSession.User user = AppSession.currentUser();
        String token = user == null ? "" : user.idToken();
        try {
            return businessMarketplaceController.loadApprovedBusiness(business.businessId(), token)
                    .join()
                    .orElse(business);
        } catch (RuntimeException exception) {
            return business;
        }
    }

    private void openBusinessLocate(PublicBusinessListing business, boolean routeMode) {
        PublicBusinessListing selected = latestBusiness(business);
        if (!selected.hasCoordinates()) {
            showInfo("Directions", "Location details are not available for this business yet.");
            return;
        }
        SimhasthaMapPage mapPage = new SimhasthaMapPage(selected.businessId(), routeMode,
                () -> showModulePage("business"), this::openBusinessFromMap);
        root.setCenter(scroll(mapPage.createContent()));
    }

    private void openBusinessFromMap(String businessId) {
        marketplaceBusinesses.stream()
                .filter(business -> business.businessId().equals(businessId))
                .findFirst()
                .ifPresentOrElse(this::openBusinessDetails,
                        () -> {
                            AppSession.User user = AppSession.currentUser();
                            String token = user == null ? "" : user.idToken();
                            businessMarketplaceController.loadApprovedBusiness(businessId, token)
                                    .thenAccept(result -> Platform.runLater(() ->
                                            result.ifPresent(this::openBusinessDetails)));
                        });
    }

    private String categoryGlyph(String category) {
        String value = category == null ? "" : category.toLowerCase(Locale.ROOT);
        if (value.contains("hotel") || value.contains("stay")) return "\uE809";
        if (value.contains("tent")) return "\uEA41";
        if (value.contains("food") || value.contains("restaurant") || value.contains("prasadam")) return "\uEC27";
        if (value.contains("parking")) return "\uE804";
        if (value.contains("puja")) return "\uEC29";
        if (value.contains("cloth")) return "\uE8BB";
        if (value.contains("toilet")) return "\uE80F";
        if (value.contains("shop") || value.contains("essential")) return "\uE719";
        return "\uE8A9";
    }

    private HBox marketplaceTabs() {
        HBox tabs = new HBox(20);
        tabs.getStyleClass().add("marketplace-tabs");
        for (String tab : List.of("Near You", "Popular", "Available Now", "Top Rated")) {
            tabs.getChildren().add(label(tab, tab.equals("Near You") ? "marketplace-tab-active" : "marketplace-tab"));
        }
        return tabs;
    }

    private HBox marketplaceArrows() {
        Button previous = new Button("\uE76B");
        Button next = new Button("\uE76C");
        previous.getStyleClass().add("marketplace-arrow-button");
        next.getStyleClass().add("marketplace-arrow-button");
        previous.setOnAction(event -> showInfo("Businesses", "Scroll the business list to view more."));
        next.setOnAction(event -> showInfo("Businesses", "Scroll the business list to view more."));
        HBox arrows = new HBox(8, previous, next);
        arrows.setAlignment(Pos.CENTER);
        return arrows;
    }

    private HBox marketplaceStatsStrip() {
        HBox stats = new HBox(0,
                marketplaceStat("0", "Verified Businesses", "\uE8FB"),
                marketplaceStat("0", "Happy Pilgrims", "\uE716"),
                marketplaceStat("0", "Service Categories", "\uE8A9"),
                marketplaceStat("24/7", "Support Available", "\uE95E"),
                marketplaceStat("100%", "Secure Booking", "\uE72E"));
        stats.getStyleClass().add("marketplace-stats-strip");
        return stats;
    }

    private VBox marketplaceStat(String value, String label, String icon) {
        VBox text = new VBox(2, label(value, "marketplace-stat-value"), label(label, "marketplace-stat-label"));
        HBox row = new HBox(10, AppUi.symbolIcon(icon, "marketplace-stat-icon"), text);
        row.setAlignment(Pos.CENTER);
        VBox wrapper = new VBox(row);
        wrapper.getStyleClass().add("marketplace-stat");
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private void updateMarketplaceStats() {
        if (marketplaceStatsStrip == null) return;
        int verified = marketplaceBusinesses == null ? 0 : marketplaceBusinesses.size();
        int categories = marketplaceBusinesses == null ? 0 : (int) marketplaceBusinesses.stream()
                .map(PublicBusinessListing::displayCategory)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .count();
        marketplaceStatsStrip.getChildren().setAll(
                marketplaceStat(verified + "+", "Verified Businesses", "\uE8FB"),
                marketplaceStat("15,000+", "Happy Pilgrims", "\uE716"),
                marketplaceStat(Math.max(categories, 1) + "+", "Service Categories", "\uE8A9"),
                marketplaceStat("24/7", "Support Available", "\uE95E"),
                marketplaceStat("100%", "Secure Booking", "\uE72E"));
    }

    private VBox myBookingsPage() {
        AppSession.User user = AppSession.currentUser();
        VBox rows = new VBox(10);
        if (user == null || AppDataStore.bookingsForUser(user.uid()).isEmpty()) {
            rows.getChildren().add(infoPanel("No bookings yet",
                    "Paid bookings and tickets will appear here after you reserve a package, stay, puja or approved business service."));
        } else {
            for (AppDataStore.BookingRecord booking : AppDataStore.bookingsForUser(user.uid())) {
                rows.getChildren().add(bookingRow(booking));
            }
        }
        return pageShell("My Bookings & Payments", "Your real booking records, payment status and tickets.", rows);
    }

    private VBox announcementPage() {
        if (!AppDataStore.items("announcement").isEmpty()) {
            return pageShell("Announcements & Official Notices", "Pinned alerts, route changes and schedule updates.",
                    adminControlledGrid("announcement", "Official"));
        }
        return pageShell("Announcements & Official Notices", "Pinned alerts, route changes and schedule updates.",
                twoColumnGrid(
                        notice("Important", "09:00 AM", "Ramkund Crowd Level Updated", "Use alternate entry if volunteer teams advise."),
                        notice("Transport", "10:30 AM", "Additional Shuttle Buses Added", "Extra buses active from Nashik Road and CBS."),
                        notice("Events", "04:15 PM", "Evening Aarti Timing Changed", "Check updated timing before moving to ghat."),
                        notice("Safety", "06:40 PM", "Medical Help Booth Added", "New help booth near main pedestrian route."),
                        notice("Important", "Pinned", "Official notices only", "Follow verified announcements from this panel.")));
    }

    private VBox aboutPage() {
        return pageShell("About Us", "Simhastha Connect platform vision and service purpose.",
                new VBox(12,
                        infoPanel("About Simhastha Connect",
                                "Simhastha Connect is a unified digital platform designed to help pilgrims access transport, ghats, puja services, accommodation, emergency support and official information during Nashik Simhastha."),
                        infoPanel("Our Mission", "Safe pilgrimage. Reliable information. Better coordination."),
                        infoPanel("Platform Features", "Pilgrim Services\nTransport Integration\nEmergency Assistance\nBusiness Services\nOfficial Announcements\nAdmin-controlled Information"),
                        infoPanel("Nashik Simhastha 2027", "A traditional and technology-ready information layer for Nashik, Ramkund, Godavari and Trimbakeshwar pilgrimage movement."),
                        label("सेवा • सुरक्षा • श्रद्धा • समन्वय", "pilgrim-about-slogan")));
    }

    private VBox genericModulePage(String module) {
        VBox rows = new VBox(10);
        for (AppDataStore.ServiceItem item : AppDataStore.items(module)) {
            rows.getChildren().add(dataRow(module, item.title, item.detail));
        }
        return pageShell(AppDataStore.displayName(module), "Official information and pilgrim support.", rows);
    }

    private GridPane adminControlledGrid(String module, String action) {
        List<Node> cards = new java.util.ArrayList<>();
        for (AppDataStore.ServiceItem item : AppDataStore.items(module)) {
            cards.add("business".equals(module) ? publicBusinessCard(item) : richCard(module, item.title, item.detail, action));
        }
        if (cards.isEmpty()) {
            cards.add(infoPanel("No official records", "Admin-published information will appear here."));
        }
        return twoColumnGrid(cards.toArray(new Node[0]));
    }

    private VBox publicBusinessCard(AppDataStore.ServiceItem item) {
        Button details = new Button("View Details  >");
        details.getStyleClass().add("pilgrim-small-action");
        details.setOnAction(event -> showInfo(item.title, item.detail));
        Button book = new Button("Book Now  >");
        book.getStyleClass().add("pilgrim-small-action");
        book.setOnAction(event -> createBusinessBooking(book, item));
        VBox card = new VBox(8, new HBox(10, moduleIcon("business", "pilgrim-card-icon"), badge("Approved")),
                strong(item.title), paragraph(item.detail), new HBox(8, details, book));
        card.getStyleClass().add("pilgrim-rich-card");
        return card;
    }

    private void createBusinessBooking(Button button, AppDataStore.ServiceItem item) {
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            showInfo("Login required", "Please login before creating a booking.");
            return;
        }
        button.setDisable(true);
        try {
            String bookingId = "BKG-BUSINESS-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            AppDataStore.addBooking(new AppDataStore.BookingRecord(bookingId, user.uid(), "BUSINESS", item.id,
                    item.id, item.title, displayName(user), java.time.LocalDate.now().plusDays(1).toString(),
                    "", 1, 1, 0, "INR", "PENDING", "NOT_REQUIRED", "", ""));
            showInfo("Booking request saved", "Your request was sent to the business owner.");
        } finally {
            button.setDisable(false);
        }
    }

    private String displayName(AppSession.User user) {
        if (user.displayName() != null && !user.displayName().isBlank()) {
            return user.displayName();
        }
        return user.email() == null || user.email().isBlank() ? "Simhastha pilgrim" : user.email();
    }

    private VBox pageShell(String title, String subtitle, Node... sections) {
        VBox content = new VBox(10, topControls(), photoHeader(title, subtitle));
        content.getChildren().addAll(sections);
        content.getStyleClass().add("pilgrim-dashboard-main");
        content.setPadding(new Insets(12, 22, 28, 22));
        return content;
    }

    private GridPane twoColumnGrid(Node... nodes) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int i = 0; i < nodes.length; i++) {
            grid.add(nodes[i], i % 2, i / 2);
            GridPane.setHgrow(nodes[i], Priority.ALWAYS);
        }
        return grid;
    }

    private GridPane scheduleTable() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("pilgrim-schedule-table");
        String[][] rows = {
                { "Time", "Event / Activity", "Location", "Category", "Details / Notes" },
                { "04:00 AM", "Brahma Muhurta Snan", "Ramkund", "Snan", "Early morning sacred bathing guidance" },
                { "06:00 AM", "Mangal Aarti", "Kalaram Mandir", "Spiritual", "Temple route support active" },
                { "07:30 AM", "Sadhus' Peshwai", "Trimbakeshwar Road", "Procession", "Follow procession barricades" },
                { "09:00 AM", "Dharmik Pravachan", "Yagyashala", "Spiritual", "Seating and water point available" },
                { "11:00 AM", "Anna Prasad Seva", "Seva Camp", "Seva", "Queue assistance active" },
                { "03:00 PM", "Sant Sabha", "Satsang Mandap", "Spiritual", "Volunteer support nearby" },
                { "05:00 PM", "Sandhya Aarti", "Ramkund", "Spiritual", "High crowd movement expected" },
                { "06:30 PM", "Cultural Program", "Main Stage", "Cultural", "Family viewing zone" },
                { "08:00 PM", "Security & Crowd Check", "All Zones", "Security", "Route clearance updates" },
                { "09:30 PM", "Day Summary & Updates", "Control Center", "Information", "Official next-day notices" },
                { "10:00 PM", "Night Patrolling", "All Zones", "Security", "Night safety patrol" } };

        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length; c++) {
                Label cell = new Label(rows[r][c]);
                cell.setWrapText(true);
                cell.getStyleClass().add(r == 0 ? "pilgrim-table-head" : "pilgrim-table-cell");
                grid.add(cell, c, r);
            }
        }
        return grid;
    }

    private HBox filterRow(String... prompts) {
        HBox row = new HBox(10);
        row.getStyleClass().add("pilgrim-filter-row");
        for (String prompt : prompts) {
            if (prompt.startsWith("Select") || prompt.startsWith("Filter") || prompt.equals("Transport Type")
                    || prompt.equals("Area") || prompt.equals("Type") || prompt.equals("Budget")) {
                ComboBox<String> combo = new ComboBox<>();
                combo.setPromptText(prompt);
                combo.getItems().addAll("All", "Ramkund", "Trimbakeshwar", "Panchavati", "Nashik Road");
                combo.getStyleClass().add("input-combo");
                combo.setMaxWidth(Double.MAX_VALUE);
                row.getChildren().add(combo);
                HBox.setHgrow(combo, Priority.ALWAYS);
            } else {
                TextField field = AppUi.textField(prompt);
                row.getChildren().add(field);
                HBox.setHgrow(field, Priority.ALWAYS);
            }
        }
        return row;
    }

    private VBox richCard(String module, String title, String detail, String action) {
        VBox card = new VBox(8, new HBox(10, moduleIcon(module, "pilgrim-card-icon"), badge(action)), strong(title),
                paragraph(detail), arrowAction(action));
        card.getStyleClass().add("pilgrim-rich-card");
        return card;
    }

    private VBox paidCard(String module, String title, String detail, String action, String catalogItemId,
            int quantity, int nights) {
        Button button = new Button(action + "  >");
        button.getStyleClass().add("pilgrim-small-action");
        button.setOnAction(event -> {
            button.setDisable(true);
            paymentCoordinator.startPaidBooking(root.getScene() == null ? null : root.getScene().getWindow(),
                    catalogItemId, quantity, nights);
            button.setDisable(false);
        });
        VBox card = new VBox(8, new HBox(10, moduleIcon(module, "pilgrim-card-icon"), badge("Paid")),
                strong(title), paragraph(detail), button);
        card.getStyleClass().add("pilgrim-rich-card");
        return card;
    }

    private HBox bookingRow(AppDataStore.BookingRecord booking) {
        Button ticket = new Button("View Ticket");
        ticket.getStyleClass().add("pilgrim-small-action");
        ticket.setDisable(AppDataStore.ticketForBooking(booking.bookingId) == null);
        ticket.setOnAction(event -> {
            AppDataStore.TicketRecord record = AppDataStore.ticketForBooking(booking.bookingId);
            if (record != null) {
                TicketViewDialog.show(record);
            }
        });

        Button refresh = new Button("Payment Status");
        refresh.getStyleClass().add("text-button");
        refresh.setOnAction(event -> showInfo("Payment Status",
                booking.title + "\nBooking: " + booking.bookingId + "\nPayment: " + booking.paymentStatus
                        + "\nBooking Status: " + booking.bookingStatus));

        VBox text = new VBox(2, strong(booking.title),
                muted(booking.bookingId + " | " + booking.moduleType + " | " + booking.dateText),
                muted("Payment: " + booking.paymentStatus + " | Booking: " + booking.bookingStatus));
        HBox row = new HBox(10, moduleIcon(booking.moduleType.toLowerCase(), "pilgrim-row-icon"), text,
                createSpacer(), refresh, ticket);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox emergencyCard(String title, String number, String detail) {
        VBox card = new VBox(7, new HBox(10, moduleIcon("emergency", "pilgrim-card-icon"), badge("Call Info")),
                strong(title), label(number, "pilgrim-emergency-number"), muted(detail), arrowAction("Call Info"));
        card.getStyleClass().addAll("pilgrim-rich-card", "pilgrim-emergency-card");
        return card;
    }

    private VBox businessCard(String title, String category, String location) {
        VBox card = new VBox(7, new HBox(10, moduleIcon("business", "pilgrim-card-icon"), badge("Verified")),
                strong(title), muted(category), muted(location), arrowAction("Contact"));
        card.getStyleClass().add("pilgrim-rich-card");
        return card;
    }

    private VBox notice(String category, String time, String title, String detail) {
        VBox card = new VBox(7, new HBox(10, badge(category), muted(time)), strong(title), muted(detail));
        card.getStyleClass().add("pilgrim-rich-card");
        return card;
    }

    private VBox infoPanel(String title, String body) {
        VBox panel = new VBox(7, sectionTitle(title), paragraph(body));
        panel.getStyleClass().add("pilgrim-panel");
        return panel;
    }

    private HBox dataRow(String module, String title, String detail) {
        HBox row = new HBox(10, moduleIcon(module, "pilgrim-row-icon"), new VBox(2, strong(title), muted(detail)));
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox crowdStatus() {
        HBox row = new HBox(10, badge("Low"), badge("Moderate"), badge("High"));
        row.getStyleClass().add("pilgrim-filter-row");
        return row;
    }

    private VBox metric(String value, String label) {
        VBox metric = new VBox(2, label(value, "pilgrim-stat-value"), muted(label));
        metric.getStyleClass().add("pilgrim-stat-card");
        HBox.setHgrow(metric, Priority.ALWAYS);
        return metric;
    }

    private Button roundButton(String icon, String text) {
        Button button = new Button(icon);
        button.getStyleClass().add("pilgrim-round-icon-button");
        button.setOnAction(event -> showInfo(text, text + " panel will open here."));
        return button;
    }

    private Button arrowAction(String text) {
        Button button = new Button(text + "  >");
        button.getStyleClass().add("pilgrim-small-action");
        button.setOnAction(event -> showInfo(text, "Selected service details will open here."));
        return button;
    }

    private Label sectionTitle(String text) {
        return label(text, "pilgrim-section-title");
    }

    private Label strong(String text) {
        return label(text, "pilgrim-card-title");
    }

    private Label muted(String text) {
        return label(text, "pilgrim-card-detail");
    }

    private Label smallGold(String text) {
        return label(text, "pilgrim-small-gold");
    }

    private Label paragraph(String text) {
        Label label = label(text, "pilgrim-card-detail");
        label.setWrapText(true);
        label.setMaxWidth(460);
        return label;
    }

    private Label badge(String text) {
        return label(text, "pilgrim-badge");
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private String valueOr(String fallback, String value) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private ImageView createImage(String path, double width, double height, double xBias, double yBias) {
        URL imageUrl = getClass().getResource(path);
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        if (imageUrl != null) {
            Image image = new Image(imageUrl.toExternalForm());
            imageView.setImage(image);
            double scale = Math.max(width / image.getWidth(), height / image.getHeight());
            double cropWidth = Math.min(image.getWidth(), width / scale);
            double cropHeight = Math.min(image.getHeight(), height / scale);
            double x = Math.max(0, (image.getWidth() - cropWidth) * xBias);
            double y = Math.max(0, (image.getHeight() - cropHeight) * yBias);
            imageView.setViewport(new Rectangle2D(x, y, cropWidth, cropHeight));
        }
        return imageView;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void addTheme(Scene scene) {
        ThemeManager.addTheme(scene, this);
        ThemeManager.addListener(() -> ThemeManager.applyTo(scene.getRoot()));
    }
}
