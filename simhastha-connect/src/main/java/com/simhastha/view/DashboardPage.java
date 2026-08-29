package com.simhastha.view;

import com.simhastha.model.Ghat;
import com.simhastha.service.GhatService;
import com.simhastha.service.GhatSnanTimeCalculator;
import com.simhastha.service.GhatOperationalStateService;
import com.simhastha.service.GhatRecommendationService;
import com.simhastha.service.GhatSuitabilityService;
import com.simhastha.service.GhatDataFreshnessService;
import com.simhastha.service.GhatNavigationService;
import com.simhastha.service.GhatCatalogueService;
import com.simhastha.service.GhatImageService;
import com.simhastha.service.GoogleMapsConfig;
import com.simhastha.service.GoogleMapsService;
import java.awt.Desktop;
import java.net.URL;
import java.net.URI;
import java.util.List;
import java.util.OptionalInt;

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
    private final java.util.Map<String, Button> navButtons = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, Place> TRANSPORT_PLACES = createTransportPlaces();
    private final GhatService ghatService = new GhatService(new FirestoreGateway(FirebaseConfig.load()));
    private final GhatOperationalStateService ghatOperationalStateService = new GhatOperationalStateService(ghatService);
    private final GhatRecommendationService ghatRecommendationService = new GhatRecommendationService();
    private final GhatSuitabilityService ghatSuitabilityService = new GhatSuitabilityService();
    private AutoCloseable ghatRefreshSubscription;
    private final java.util.Map<String, Ghat> displayedGhats = new java.util.HashMap<>();
    private final GhatNavigationService ghatNavigationService = new GhatNavigationService();
    private final GhatCatalogueService ghatCatalogueService = new GhatCatalogueService();
    private final GhatImageService ghatImageService = new GhatImageService();
    private final java.util.Map<String, Image> localImageCache = new java.util.HashMap<>();
    private final java.util.Map<String, GhatCardCacheEntry> ghatCardCache = new java.util.HashMap<>();
    private final GoogleMapsService googleMapsService = new GoogleMapsService(GoogleMapsConfig.load());
    private List<Ghat> currentGhats = List.of();
    private long ghatRefreshGeneration;
    private VBox ghatOperationalAlerts;
    private Ghat selectedGhat;
    private VBox selectedGhatCard;
    private String ghatRegionFilter = "All Regions";
    private String ghatCrowdFilter = "All";
    private String ghatSnanFilter = "All";
    private String ghatSort = "Live Crowd";
    private String ghatSearch = "";

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
        stopGhatRefresh();
        setActiveModule("home");
        root.setCenter(scroll(createHomePage()));
    }

    private void showModulePage(String module) {
        if (!"ghat".equals(module)) {
            stopGhatRefresh();
        }
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
        currentGhats = List.of();
        displayedGhats.clear();
        FlowPane ghatFlowPane = new FlowPane(16, 16);
        ghatFlowPane.setPrefWrapLength(1140);
        ghatFlowPane.getStyleClass().add("ghat-card-grid");
        List<Ghat> catalogue = ghatCatalogueService.catalogue();
        System.out.println("CATALOGUE COUNT = " + catalogue.size());
        VBox operationalAlerts = new VBox(8);
        ghatOperationalAlerts = operationalAlerts;
        HBox summary = new HBox(12,
                ghatSummaryCard("\uE8B5", "Open Ghats", "—", "Safe for Snan", "ghat-summary-open"),
                ghatSummaryCard("\uE8B5", "Low Crowd", "—", "Comfortable movement", "ghat-summary-low"),
                ghatSummaryCard("\uE8B5", "High Crowd", "—", "Plan extra time", "ghat-summary-high"),
                ghatSummaryCard("\uE8B5", "Critical Crowd", "—", "Choose carefully", "ghat-summary-critical"),
                ghatSummaryCard("\uE8B5", "Restricted / Closed", "—", "Check safety updates", "ghat-summary-restricted"));
        summary.getStyleClass().add("ghat-summary-row");

        TextField search = AppUi.textField("Search Ghat name...");
        search.getStyleClass().add("ghat-search-field");
        search.setText(ghatSearch);
        search.textProperty().addListener((observable, previous, value) -> {
            ghatSearch = value == null ? "" : value;
            renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats);
        });
        ComboBox<String> region = new ComboBox<>();
        region.getItems().addAll("All Regions", "Nashik / Panchavati", "Trimbakeshwar"); region.setValue(ghatRegionFilter); region.getStyleClass().add("input-combo");
        region.setOnAction(event -> { ghatRegionFilter = region.getValue(); renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats); });
        ComboBox<String> crowd = ghatFilterCombo("All", "Low", "Moderate", "High", "Critical", "Unknown");
        crowd.setValue(ghatCrowdFilter);
        crowd.setOnAction(event -> { ghatCrowdFilter = crowd.getValue(); renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats); });
        ComboBox<String> snan = ghatFilterCombo("All", "Available", "Caution", "Suspended", "Unavailable");
        snan.setValue(ghatSnanFilter);
        snan.setOnAction(event -> { ghatSnanFilter = snan.getValue(); renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats); });
        ComboBox<String> sort = ghatFilterCombo("Live Crowd", "Name", "Walking Difficulty", "Estimated Wait");
        sort.setValue(ghatSort);
        sort.setOnAction(event -> { ghatSort = sort.getValue(); renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats); });
        Button mapView = new Button("Map View"); mapView.getStyleClass().add("ghat-map-view-button"); mapView.setOnAction(event -> openGhatMapView());
        FlowPane filter = new FlowPane(8, 8, search, label("Region", "ghat-filter-label"), region,
                label("Crowd", "ghat-filter-label"), crowd, label("Snan", "ghat-filter-label"), snan,
                label("Sort", "ghat-filter-label"), sort, mapView);
        filter.getStyleClass().add("ghat-filter-row");

        HBox featureCards = new HBox(14,
                ghatFeatureCard("\uE787", "Important Snan Guide", "Official Simhastha Snan schedule will appear here when published.", "View Guide", () -> showInfo("Important Snan Guide", "Official Simhastha Snan schedule will appear here when published.")),
                ghatFeatureCard("\uE707", "Interactive Ghat Map", "Explore Ghats and keep selected Ghat guidance in view.", "Open Map", this::openGhatMapView),
                ghatFeatureCard("\uE8D4", "Nashik–Trimbakeshwar Simhastha", "Nashik and Trimbakeshwar have distinct Akhada bathing arrangements.", "Learn More", () -> showInfo("Nashik–Trimbakeshwar Simhastha", "The festival takes place on the Godavari in Nashik and Trimbakeshwar. The two locations historically have distinct Akhada bathing arrangements.")));
        featureCards.getStyleClass().add("ghat-feature-row");
        featureCards.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        VBox content = new VBox(12, ghatHero(), summary, filter,
                new VBox(2, label("Ghats by Live Crowd", "ghat-section-title"),
                        label("Live conditions appear first when available. Catalogue entries remain available for planning.", "ghat-section-subtitle")), operationalAlerts, ghatFlowPane, featureCards);
        content.getStyleClass().addAll("pilgrim-dashboard-main", "ghat-page");
        content.setPadding(new Insets(12, 22, 28, 22));
        ghatFlowPane.getChildren().add(ghatState("Loading Ghat information…", false, ghatFlowPane, summary));
        loadGhats(ghatFlowPane, summary, operationalAlerts);
        return content;
    }

    private StackPane ghatHero() {
        ImageView image = createImage("/images/godavari_kumbh.jpg", 1400, 196, 0.54, 0.47);
        VBox copy = new VBox(5, label("Ghats & Snan", "ghat-hero-title"),
                label("Sacred Bathing • Live Crowd Guide • Safe Snan", "ghat-hero-subtitle"),
                label("Godavari guidance for a calm, informed pilgrimage", "ghat-hero-detail"));
        copy.setPadding(new Insets(20, 28, 22, 28)); copy.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(copy, Pos.CENTER_LEFT);
        HBox controls = ghatTopControls();
        // StackPane resizes unconstrained Regions to fill its content area.  The
        // controls themselves have a light background, so keep that surface to
        // the compact date/time/weather controls rather than the whole banner.
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(controls, Pos.TOP_RIGHT); StackPane.setMargin(controls, new Insets(14, 18, 0, 0));
        StackPane overlay = new StackPane(); overlay.getStyleClass().add("ghat-hero-overlay");
        StackPane hero = new StackPane(image, overlay, copy, controls);
        hero.getStyleClass().add("ghat-hero"); hero.setMinHeight(196);
        return hero;
    }

    private HBox ghatTopControls() {
        Label date = label(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM")), "ghat-top-meta");
        Label time = label(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")), "ghat-top-meta");
        Label weather = label("Weather unavailable", "ghat-top-meta");
        HBox controls = new HBox(9, date, time, weather,
                AppUi.createThemeToggle(), roundButton("\uE7F4", "Notifications"), roundButton("\uE77B", "Profile"));
        controls.getStyleClass().add("ghat-top-controls"); controls.setAlignment(Pos.CENTER_RIGHT); return controls;
    }

    private VBox ghatSummaryCard(String icon, String title, String value, String helper, String modifier) {
        Label iconLabel = label(icon, "ghat-summary-icon");
        VBox copy = new VBox(1, label(value, "ghat-summary-value"), label(title, "ghat-summary-label"), label(helper, "ghat-summary-helper"));
        VBox card = new VBox(4, new HBox(9, iconLabel, copy));
        card.getStyleClass().addAll("ghat-summary-card", modifier); HBox.setHgrow(card, Priority.ALWAYS); return card;
    }

    private ComboBox<String> ghatFilterCombo(String... values) {
        ComboBox<String> combo = new ComboBox<>(); combo.getItems().addAll(values); combo.setValue(values[0]); combo.getStyleClass().add("input-combo"); return combo;
    }

    private VBox ghatFeatureCard(String icon, String title, String detail, String action, Runnable callback) {
        Button button = new Button(action); button.getStyleClass().add("ghat-feature-action"); button.setOnAction(event -> callback.run());
        VBox card = new VBox(8, label(icon, "ghat-feature-icon"), label(title, "ghat-feature-title"), label(detail, "ghat-feature-detail"), button);
        card.getStyleClass().add("ghat-feature-card"); return card;
    }

    private void loadGhats(Pane cards, HBox summary, VBox operationalAlerts) {
        stopGhatRefresh();
        long refreshGeneration = ++ghatRefreshGeneration;
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        ghatRefreshSubscription = ghatOperationalStateService.subscribe(token, java.time.Duration.ofSeconds(30),
                ghats -> Platform.runLater(() -> {
                    if (refreshGeneration != ghatRefreshGeneration) return;
                    try {
                        System.out.println("SERVICE COUNT = " + ghats.size());
                        System.out.println("SERVICE GHATS = " + ghats.stream().map(Ghat::name).collect(java.util.stream.Collectors.joining(", ")));
                        if (!ghats.equals(currentGhats)) renderGhats(cards, summary, operationalAlerts, ghats);
                    } catch (Throwable error) {
                        error.printStackTrace();
                        cards.getChildren().setAll(ghatState("Unable to load Ghat information.", true, cards, summary));
                    }
                }),
                error -> Platform.runLater(() -> {
                    if (refreshGeneration != ghatRefreshGeneration) return;
                    error.printStackTrace();
                    cards.getChildren().clear();
                    cards.getChildren().add(ghatState("Unable to load Ghat information.", true, cards, summary));
                }));
    }

    private void loadGhats(Pane cards, HBox summary) { loadGhats(cards, summary, ghatOperationalAlerts == null ? new VBox() : ghatOperationalAlerts); }

    private void renderGhats(Pane cards, HBox summary, VBox operationalAlerts, List<Ghat> ghats) {
            currentGhats = ghats == null ? List.of() : List.copyOf(ghats);
            updateGhatSummary(summary, currentGhats);
            updateOperationalAlerts(operationalAlerts, currentGhats);
            List<Ghat> visibleGhats = filteredGhats();
            System.out.println("FILTERED COUNT = " + visibleGhats.size());
            if (visibleGhats.isEmpty()) {
                cards.getChildren().setAll(ghatState("No Ghat information is currently available.", false, cards, summary));
                System.out.println("FLOWPANE CHILDREN = " + cards.getChildren().size());
                return;
            }
            List<Node> ghatCards = visibleGhats.stream().filter(java.util.Objects::nonNull).map(ghat -> (Node) cachedGhatCard(ghat)).toList();
            cards.getChildren().setAll(ghatCards);
            System.out.println("FLOWPANE CHILDREN = " + cards.getChildren().size());
    }

    private List<Ghat> filteredGhats() {
        java.util.stream.Stream<Ghat> stream = currentGhats.stream().filter(ghat -> "All Regions".equals(ghatRegionFilter)
                || ("Trimbakeshwar".equals(ghatRegionFilter)
                ? ghatCatalogueService.regionOf(ghat) == GhatCatalogueService.Region.TRIMBAKESHWAR
                : ghatCatalogueService.regionOf(ghat) == GhatCatalogueService.Region.NASHIK_GODAVARI));
        String query = ghatSearch.trim().toLowerCase();
        if (!query.isEmpty()) stream = stream.filter(ghat -> ghat.name().toLowerCase().contains(query));
        if (!"All".equals(ghatCrowdFilter)) stream = stream.filter(ghat -> titleCase(ghat.crowdLevel().name()).equals(ghatCrowdFilter));
        if (!"All".equals(ghatSnanFilter)) stream = stream.filter(ghat -> titleCase(ghat.operationalState().bathingStatus().name()).equals(ghatSnanFilter));
        java.util.Comparator<Ghat> comparator = switch (ghatSort) {
            case "Name" -> java.util.Comparator.comparing(Ghat::name, String.CASE_INSENSITIVE_ORDER);
            case "Walking Difficulty" -> java.util.Comparator.comparingInt(ghat -> ghat.walking().difficulty().ordinal());
            case "Estimated Wait" -> java.util.Comparator.comparing(Ghat::estimatedWaitMinutes, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            default -> com.simhastha.service.GhatCrowdComparator.LIVE_CROWD_ORDER;
        };
        return stream.sorted(comparator).toList();
    }

    private VBox ghatState(String message, boolean retry, Pane cards, HBox summary) {
        Label text = label(message, "ghat-state-message");
        VBox state = new VBox(9, text); state.getStyleClass().add("ghat-state");
        if (retry) { Button button = new Button("Retry"); button.getStyleClass().add("ghat-primary-action");
            button.setOnAction(event -> { cards.getChildren().setAll(ghatState("Loading Ghat information…", false, cards, summary)); loadGhats(cards, summary); }); state.getChildren().add(button); }
        return state;
    }

    private void stopGhatRefresh() {
        ghatRefreshGeneration++;
        if (ghatRefreshSubscription != null) {
            try { ghatRefreshSubscription.close(); }
            catch (Exception exception) { exception.printStackTrace(); }
            ghatRefreshSubscription = null;
        }
    }

    private void updateOperationalAlerts(VBox box, List<Ghat> ghats) {
        box.getChildren().clear();
        for (Ghat ghat : ghats) {
            Ghat previous = displayedGhats.put(ghat.id(), ghat);
            if (previous != null && ghat.crowdLevel().ordinal() > previous.crowdLevel().ordinal()) box.getChildren().add(operationalAlert("WARNING", "Crowd increased at " + ghat.name() + " — " + ghat.crowdLevel()));
            if (ghat.operationalState().priorityAlert().present()) box.getChildren().add(operationalAlert(ghat.operationalState().priorityAlert().priority().name(), ghat.operationalState().priorityAlert().message()));
            if (selectedGhat != null && selectedGhat.id().equals(ghat.id())) selectedGhat = ghat;
        }
        if (selectedGhat != null) {
            List<Ghat> alternatives = ghatRecommendationService.alternativesFor(selectedGhat, ghats);
            if (!ghatRecommendationService.recommended(selectedGhat) && !alternatives.isEmpty()) box.getChildren().add(operationalAlert("ADVISORY", "Safer alternatives: " + alternatives.stream().limit(3).map(Ghat::name).collect(java.util.stream.Collectors.joining(" • "))));
        }
    }

    private VBox operationalAlert(String priority, String message) { VBox alert = new VBox(3, label(priority, "ghat-alert-priority"), label(message, "ghat-alert-message")); alert.getStyleClass().addAll("ghat-operational-alert", "ghat-alert-" + priority.toLowerCase()); return alert; }

    private void updateGhatSummary(HBox summary, List<Ghat> ghats) {
        long open = ghats.stream().filter(ghat -> ghat.operationalStatus() == Ghat.OperationalStatus.OPEN).count();
        long low = ghats.stream().filter(ghat -> ghat.crowdLevel() == Ghat.CrowdLevel.LOW).count();
        long high = ghats.stream().filter(ghat -> ghat.crowdLevel() == Ghat.CrowdLevel.HIGH).count();
        long critical = ghats.stream().filter(ghat -> ghat.crowdLevel() == Ghat.CrowdLevel.CRITICAL).count();
        long restricted = ghats.stream().filter(ghat -> switch (ghat.operationalStatus()) {
            case PARTIALLY_RESTRICTED, RESTRICTED, TEMPORARILY_CLOSED, EMERGENCY_CLOSED -> true;
            default -> false;
        }).count();
        String[] values = { String.valueOf(open), String.valueOf(low), String.valueOf(high), String.valueOf(critical), String.valueOf(restricted) };
        for (int index = 0; index < summary.getChildren().size(); index++) {
            VBox card = (VBox) summary.getChildren().get(index);
            HBox cardHeader = (HBox) card.getChildren().get(0);
            VBox copy = (VBox) cardHeader.getChildren().get(1);
            copy.getChildren().set(0, label(values[index], "ghat-summary-value"));
        }
    }

    private VBox ghatCard(Ghat ghat) {
        VBox card = new VBox(10); card.getStyleClass().add("ghat-card"); card.setPrefWidth(214); card.setMinWidth(205);
        card.setOnMouseClicked(event -> selectGhatCard(ghat, card));
        StackPane imageShell = new StackPane(ghatImageService.createView(ghat, 214, 124));
        imageShell.getStyleClass().add("ghat-card-image-shell");
        Label crowdBadge = ghatBadge(ghat.crowdLevel() == Ghat.CrowdLevel.UNKNOWN ? "INFORMATION ONLY" : ghat.crowdLevel().name(), "ghat-crowd-" + ghat.crowdLevel().name().toLowerCase());
        StackPane.setAlignment(crowdBadge, Pos.TOP_LEFT); StackPane.setMargin(crowdBadge, new Insets(9)); imageShell.getChildren().add(crowdBadge);
        VBox heading = new VBox(2, label(ghat.name(), "ghat-card-title"),
                label("\uE707  " + (ghat.area().isBlank() ? "Location details pending" : ghat.area()), "ghat-card-area"));
        FlowPane statuses = new FlowPane(5, 5); statuses.setPadding(new Insets(0, 11, 0, 11));
        statuses.getStyleClass().add("ghat-status-row");
        statuses.getChildren().add(ghatBadge(displayStatus(ghat.operationalStatus()), "ghat-status"));
        if (ghat.crowdLevel() == Ghat.CrowdLevel.UNKNOWN) statuses.getChildren().add(ghatBadge("Live status unavailable", "ghat-status"));
        else statuses.getChildren().add(ghatBadge("Snan " + titleCase(ghat.operationalState().bathingStatus().name()), ghat.operationalState().bathingRecommended() ? "ghat-bathing-available" : "ghat-bathing-unavailable"));
        if (ghat.crowdLevel() != Ghat.CrowdLevel.UNKNOWN || ghat.operationalStatus() != Ghat.OperationalStatus.INFORMATION_ONLY) {
            statuses.getChildren().add(ghatBadge("Water " + titleCase(ghat.operationalState().waterSafety().name()), "ghat-status"));
        }
        HBox metrics = new HBox(5,
                ghatMetric("WAIT", ghat.crowdLevel() == Ghat.CrowdLevel.UNKNOWN ? "N/A" : ghat.waitLabel()),
                ghatMetric("WALK", titleCase(ghat.walking().difficulty().name())),
                ghatMetric("STEPS", ghat.walking().approximateSteps() == null ? "N/A" : String.valueOf(ghat.walking().approximateSteps())),
                ghatMetric("WEATHER", compactWeather(ghat.weather())));
        Node safety = ghatOperationalNote(ghat);
        HBox actions = new HBox(5, ghatAction("History", event -> showGhatHistory(ghat), "ghat-card-action"),
                ghatAction("Locate", event -> locateGhat(ghat), "ghat-card-action ghat-card-action-primary"),
                ghatAction("Navigate", event -> navigateToGhat(ghat), "ghat-card-action"));
        HBox help = new HBox(4, ghatAction("Medical", event -> requestGhatHelp(ghat, "Medical"), "ghat-help-action"),
                ghatAction("Police", event -> requestGhatHelp(ghat, "Police"), "ghat-help-action"),
                ghatAction("Lost & Found", event -> requestGhatHelp(ghat, "Lost & Found"), "ghat-help-action"));
        card.getChildren().addAll(imageShell, heading, statuses, metrics);
        if (!(safety instanceof Pane)) card.getChildren().add(safety);
        card.getChildren().addAll(actions, help);
        return card;
    }

    private VBox cachedGhatCard(Ghat ghat) {
        GhatCardCacheEntry cached = ghatCardCache.get(ghat.id());
        if (cached != null && cached.ghat().equals(ghat)) return cached.card();
        VBox card = ghatCard(ghat);
        ghatCardCache.put(ghat.id(), new GhatCardCacheEntry(ghat, card));
        return card;
    }

    private void selectGhatCard(Ghat ghat, VBox card) {
        if (selectedGhatCard != null) selectedGhatCard.getStyleClass().remove("ghat-card-selected");
        selectedGhat = ghat; selectedGhatCard = card; card.getStyleClass().add("ghat-card-selected");
    }

    private VBox ghatMetric(String key, String value) {
        VBox metric = new VBox(1, label(key, "ghat-metric-key"), label(value, "ghat-metric-value"));
        metric.getStyleClass().add("ghat-metric"); HBox.setHgrow(metric, Priority.ALWAYS); return metric;
    }

    private String compactWeather(Ghat.Weather weather) { return weather.available() ? weather.temperatureCelsius() + "°C" : "N/A"; }

    private Node ghatOperationalNote(Ghat ghat) {
        String message = ghat.operationalState().priorityAlert().present() ? ghat.operationalState().priorityAlert().message()
                : !ghat.operationalState().hazards().isEmpty() ? ghat.operationalState().hazards().get(0).message()
                : ghat.operationalState().restrictionReason();
        return message.isBlank() ? new Pane() : label(message, "ghat-safety-note");
    }
    private String weatherText(Ghat.Weather weather) { if (!weather.available()) return "Weather unavailable"; String extra = weather.rainProbability() == null ? "" : " • Rain chance " + weather.rainProbability() + "%"; return weather.temperatureCelsius() + "°C • " + weather.condition() + extra; }
    private String gateSummary(Ghat ghat) { return ghat.operationalState().gates().isEmpty() ? "Gate information unavailable" : ghat.operationalState().gates().stream().map(gate -> gate.name() + " " + titleCase(gate.status().name())).collect(java.util.stream.Collectors.joining(" • ")); }
    private String zoneSummary(Ghat ghat) { return ghat.operationalState().zones().isEmpty() ? "Zone information unavailable" : ghat.operationalState().zones().stream().map(zone -> zone.name() + " " + titleCase(zone.status().name())).collect(java.util.stream.Collectors.joining(" • ")); }

    private Label ghatBadge(String text, String style) { Label badge = label(text, "ghat-badge"); badge.getStyleClass().add(style); return badge; }
    private ImageView ghatImage(String url) { ImageView image = new ImageView(new Image(url, 62, 48, true, true, true)); image.setFitWidth(62); image.setFitHeight(48); image.getStyleClass().add("ghat-thumbnail"); return image; }
    private HBox ghatLine(String key, String value) { Label detail = label(value, "ghat-line-value"); detail.setWrapText(true); HBox row = new HBox(7, label(key, "ghat-line-key"), detail); HBox.setHgrow(detail, Priority.ALWAYS); return row; }
    private Button ghatAction(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action, String style) {
        Button button = new Button(text); button.getStyleClass().addAll(style.split(" "));
        button.setOnAction(action); return button;
    }
    private String walkingDescription(Ghat ghat) { String distance = ghat.walking().distanceMeters() == null ? "Distance unavailable" : ghat.walking().distanceMeters() + " m from entry"; return ghat.walking().approximateSteps() == null ? distance : distance + " • Approx. " + ghat.walking().approximateSteps() + " steps"; }
    private String displayStatus(Ghat.OperationalStatus status) { return titleCase(status.name()); }
    private String titleCase(String text) { String[] words = text.toLowerCase().replace('_', ' ').split(" "); StringBuilder result = new StringBuilder(); for (String word : words) { if (!word.isBlank()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' '); } return result.toString().trim(); }
    private String lastUpdatedLabel(String updated) { if (updated == null || updated.isBlank() || GhatDataFreshnessService.isStaleEpochMillis(updated)) return "Live status may be outdated"; try { return "Updated " + Math.max(0, (System.currentTimeMillis() - Long.parseLong(updated)) / 60000) + " min ago"; } catch (NumberFormatException exception) { return "Updated " + updated; } }
    private void showGhatHistory(Ghat ghat) {
        selectedGhat = ghat;
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("History & Significance");
        dialog.setHeaderText(ghat.name());
        dialog.setContentText(ghat.history().available() ? historyText(ghat.history()) : "Verified history information will be available soon.");
        dialog.setGraphic(ghatImageService.createView(ghat, 210, 125));
        dialog.showAndWait();
    }
    private String historyText(Ghat.History history) { return String.join("\n\n", java.util.stream.Stream.of(history.historicalBackground(), history.religiousSignificance(), history.simhasthaConnection(), history.associatedSacredPlaces(), history.rituals(), history.didYouKnow()).filter(value -> !value.isBlank()).toList()); }
    private void locateGhat(Ghat ghat) {
        selectedGhat = ghat;
        googleMapsService.openGhatLocation(ghat)
                .ifPresent(message -> showInfo("Google Maps", message));
    }
    private void openGhatMapView() {
        googleMapsService.showGhats(root.getScene() == null ? null : root.getScene().getWindow(), filteredGhats(), selectedGhat)
                .ifPresent(message -> showInfo("Google Maps", message));
    }
    private void navigateToGhat(Ghat ghat) {
        selectedGhat = ghat; GhatNavigationService.Decision decision = ghatNavigationService.decision(ghat);
        if (decision == GhatNavigationService.Decision.UNSAFE) { showInfo("Safe Snan unavailable", "This Ghat is currently unavailable for safe Snan. Choose one of the less crowded safe alternatives."); return; }
        if (decision == GhatNavigationService.Decision.NO_ENTRY) { showInfo("No active entry route", "No active entry route is currently available for this Ghat."); return; }
        if (decision == GhatNavigationService.Decision.HIGH_CROWD_CONFIRMATION) { Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "This Ghat currently has high crowd. Continue to this Ghat?", ButtonType.YES, ButtonType.NO); if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return; }
        java.util.Optional<GhatNavigationService.Destination> destination = ghatNavigationService.destinationFor(ghat);
        String destinationQuery = destination.isPresent() ? "" : ghatNavigationService.destinationQueryFor(ghat);
        Dialog<ButtonType> originDialog = new Dialog<>(); originDialog.setTitle("Choose Starting Location");
        TextField latitude = AppUi.textField("Latitude"); TextField longitude = AppUi.textField("Longitude");
        String targetName = destination.map(GhatNavigationService.Destination::entryName).orElse(ghat.name());
        ButtonType withoutOrigin = new ButtonType("Continue without origin");
        originDialog.getDialogPane().setContent(new VBox(8, label("Enter your current starting coordinates to open directions to " + targetName + ", or let Google Maps request your location.", "ghat-line-value"), latitude, longitude)); originDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, withoutOrigin, ButtonType.CANCEL);
        ButtonType choice = originDialog.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == ButtonType.CANCEL) return;
        try {
            GoogleMapsService.Point origin = choice == withoutOrigin ? null : new GoogleMapsService.Point("Starting location", Double.parseDouble(latitude.getText().trim()), Double.parseDouble(longitude.getText().trim()));
            GoogleMapsService.Point point = destination.map(value -> new GoogleMapsService.Point(value.entryName(), value.point().latitude(), value.point().longitude())).orElse(null);
            googleMapsService.openDirections(origin, point, destinationQuery)
                    .ifPresent(message -> showInfo("Google Maps", message));
        }
        catch (NumberFormatException exception) { showInfo("Choose a starting location", "Choose a starting location to navigate using valid latitude and longitude."); }
    }
    private void requestGhatHelp(Ghat ghat, String type) {
        selectedGhat = ghat;
        showModulePage("Lost & Found".equals(type) ? "lost" : "emergency");
    }
    private String coordinateText(Ghat ghat) { return ghat.latitude() == null || ghat.longitude() == null ? "unavailable" : ghat.latitude() + ", " + ghat.longitude(); }

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
        HBox categories = new HBox(8,
                badge("Food & Prasadam"), badge("Accommodation"), badge("Puja Items"), badge("Medical Stores"),
                badge("Local Guides"), badge("Lockers"), badge("Charging Points"));
        if (!AppDataStore.items("business").isEmpty()) {
            return pageShell("Approved Businesses & Local Services", "Verified local services for pilgrims.",
                    categories, adminControlledGrid("business", "View Details"));
        }
        return pageShell("Approved Businesses & Local Services", "Verified local services for pilgrims.",
                categories,
                twoColumnGrid(
                        richCard("business", "Verified Food & Prasadam Zone", "Food & Prasadam\nInformation-only listing\nNo forced online payment", "Contact"),
                        paidCard("business", "Paid Parking Reservation", "Approved private parking near Ramkund approach road", "Pay Securely", "business-paid-parking", 1, 1),
                        paidCard("business", "Tent Booking Advance", "Approved paid tent booking advance for festival camp stay", "Pay Advance", "business-tent-advance", 1, 1),
                        paidCard("business", "Local Guide Inquiry", "Pay at location after service confirmation", "Request", "business-guide-pay-location", 1, 1),
                        businessCard("Medical Store Cluster", "Medical Stores", "CBS and Ramkund route"),
                        businessCard("Puja Items Market", "Puja Items", "Temple approach zone")));
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
            Image image = localImageCache.computeIfAbsent(path, key -> new Image(imageUrl.toExternalForm()));
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

    private record GhatCardCacheEntry(Ghat ghat, VBox card) { }

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
