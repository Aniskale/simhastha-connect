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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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
import javafx.util.StringConverter;

public class DashboardPage {

    private BorderPane root;
    private final AppPaymentCoordinator paymentCoordinator = new AppPaymentCoordinator();
    private final java.util.Map<String, Button> navButtons = new java.util.LinkedHashMap<>();
    private LocationOption selectedFromLocation;
    private LocationOption selectedToLocation;
    private static final java.util.Map<String, Place> TRANSPORT_PLACES = createTransportPlaces();
    private static final java.util.List<LocationOption> INDIA_LOCATION_OPTIONS = loadIndiaLocationOptions();
    private static final int LOCATION_SEARCH_LIMIT = 30;
    private static final java.util.concurrent.ExecutorService LOCATION_SEARCH_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "simhastha-location-search");
        thread.setDaemon(true);
        return thread;
    });
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
    private final java.util.LinkedHashMap<String, StayItem> stayWishlist = new java.util.LinkedHashMap<>();
    private final java.util.LinkedHashMap<String, StayItem> recentlyViewedStays = new java.util.LinkedHashMap<>();

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
        if ("stay".equals(module)) {
            root.setCenter(stayPageShell());
            return;
        }
        Node page = switch (module) {
            case "packages" -> packagesPage();
            case "transport" -> transportPage();
            case "puja" -> pujaPage();
            case "ghat" -> ghatsPage();
            case "emergency" -> emergencyPage();
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

    private BorderPane stayPageShell() {
        Button back = new Button("← Back");
        back.getStyleClass().add("pilgrim-small-action");
        back.setOnAction(event -> showHomePage());
        HBox header = new HBox(back);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 22, 8, 22));
        header.getStyleClass().add("stay-top-bar");
        BorderPane shell = new BorderPane();
        shell.setTop(header);
        shell.setCenter(scroll(stayPage()));
        return shell;
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
        HBox modes = new HBox(14,
                transportModeCard("🚌", "BUS", "Bus travel to Nashik", "Explore Bus", this::showBusOptions),
                transportModeCard("🚆", "TRAIN", "Train travel to Nashik", "Explore Train", this::showTrainSearch),
                transportModeCard("✈", "AIRPLANE", "Flight travel to Nashik", "Explore Flights", this::showFlightSearch));
        modes.setAlignment(Pos.CENTER);
        return pageShell("Transport", "Choose the best travel mode for your journey to Nashik Kumbh.",
                transportOfficialSources(), modes, transportTrustStrip());
    }

    private VBox transportModeCard(String icon, String title, String detail, String action, Runnable onAction) {
        ImageView image = createImage(transportCardImage(title), 390, 154, 0.5, 0.5);
        javafx.scene.shape.Rectangle imageClip = new javafx.scene.shape.Rectangle(390, 154);
        imageClip.setArcWidth(20);
        imageClip.setArcHeight(20);
        image.setClip(imageClip);
        StackPane visual = new StackPane(image);
        Button button = new Button(action);
        button.getStyleClass().add("transport-primary-button");
        button.setOnAction(event -> onAction.run());
        VBox card = new VBox(12, visual, strong(title), paragraph(detail), transportCardFeatures(title), button);
        card.getStyleClass().add("transport-mode-card");
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private String transportCardImage(String title) {
        return switch (title) {
            case "BUS" -> "/images/transport-bus-reference.png";
            case "TRAIN" -> "/images/transport-train-reference.png";
            default -> "/images/transport-airplane-reference.png";
        };
    }

    private HBox transportCardFeatures(String title) {
        return switch (title) {
            case "BUS" -> new HBox(7, transportFeature("All India Cities"), transportFeature("Official Sources"), transportFeature("Safe & Reliable"));
            case "TRAIN" -> new HBox(7, transportFeature("All India Stations"), transportFeature("IRCTC & Partners"), transportFeature("Secure Booking"));
            default -> new HBox(7, transportFeature("All India Airports"), transportFeature("Official Websites"), transportFeature("Best Options"));
        };
    }

    private Label transportFeature(String text) {
        return label(text, "transport-feature-chip");
    }

    private VBox transportOfficialSources() {
        HBox row = new HBox(16, AppUi.symbolIcon("\uE8A5", "transport-source-icon"),
                new VBox(4, strong("Plan with official sources"),
                        muted("Use official travel websites for availability, fares and booking.\nLocal Kumbh transport guidance is also available.")));
        row.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(row);
        panel.getStyleClass().add("transport-sources-panel");
        return panel;
    }

    private HBox transportTrustStrip() {
        HBox strip = new HBox(18,
                transportTrustItem("Trusted & Secure", "All bookings redirect to official and trusted platforms."),
                transportTrustItem("Provider Updates", "Check current availability and travel information directly."),
                transportTrustItem("Local Guidance", "Find Kumbh local transport, routes and facilities."),
                transportTrustItem("24/7 Support", "We are here to help you plan your journey."));
        strip.getStyleClass().add("transport-trust-strip");
        return strip;
    }

    private HBox transportTrustItem(String title, String detail) {
        VBox copy = new VBox(3, strong(title), muted(detail));
        HBox item = new HBox(10, AppUi.symbolIcon("\uE8A5", "transport-trust-icon"), copy);
        item.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(item, Priority.ALWAYS);
        return item;
    }

    private void showBusOptions() {
        HBox options = new HBox(16,
                busJourneyCard("Outside Nashik Journey", "Travel to Nashik Kumbh from any city in India.",
                        "/images/bus-intercity-reference.png", false, "Plan Journey", this::showOutsideNashikJourney,
                        "Search buses from your city to Nashik", "Compare multiple travel providers",
                        "Book on official and trusted websites", "Comfortable and safe bus travel"),
                busJourneyCard("Nashik Local /\nKumbh Transport", "Explore local buses and transport options around Nashik Kumbh.",
                        "/images/bus-local-reference.png", true, "Find Local Transport", this::showLocalTransport,
                        "Routes connecting major Kumbh locations", "Railway stations, bus stands, ghats & more",
                        "Local transport guidance & route info", "Nashik CitiLink - City Bus Service"));
        options.setAlignment(Pos.CENTER);
        VBox content = new VBox(12, transportPageHeader("Bus Transport",
                "Choose the best bus travel option for your journey to Nashik Kumbh"), busTransportHero(), busBackButton(), options,
                busTravelTip());
        content.getStyleClass().add("pilgrim-dashboard-main");
        content.setPadding(new Insets(12, 22, 28, 22));
        root.setCenter(scroll(content));
    }

    private HBox transportPageHeader(String titleText, String subtitleText) {
        VBox title = new VBox(2, label(titleText, "bus-page-title"), muted(subtitleText));
        HBox header = new HBox(10, title, createSpacer(), AppUi.createThemeToggle(),
                roundButton("\uE7F4", "Notifications"), roundButton("\uE77B", "Profile"));
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private StackPane busTransportHero() {
        ImageView image = createImage("/images/welcome-light.png", 1100, 150, 0.54, 0.48);
        image.getStyleClass().add("bus-transport-hero-image");
        Label bus = new Label("\uD83D\uDE8C");
        bus.getStyleClass().add("bus-hero-icon");
        VBox text = new VBox(7, label("BUS TRANSPORT", "bus-hero-title"), label("Plan your journey to Nashik Kumbh", "bus-hero-subtitle"),
                label("|| \u0964 \u0913\u0902 \u0928\u092E\u0903 \u0936\u093F\u0935\u093E\u092F \u0964 ||", "bus-hero-mantra"));
        HBox copy = new HBox(22, bus, text);
        copy.setAlignment(Pos.CENTER_LEFT);
        copy.setPadding(new Insets(18, 38, 18, 38));
        StackPane.setAlignment(copy, Pos.CENTER_LEFT);
        StackPane hero = new StackPane(image, copy);
        hero.getStyleClass().add("bus-transport-hero");
        return hero;
    }

    private Button busBackButton() {
        Button back = new Button("\u2190  Back");
        back.getStyleClass().add("bus-back-button");
        back.setOnAction(event -> showModulePage("transport"));
        return back;
    }

    private VBox busJourneyCard(String title, String subtitle, String imagePath, boolean local, String action, Runnable onAction,
            String... points) {
        ImageView image = createImage(imagePath, 185, 205, 0.5, 0.5);
        image.getStyleClass().add("bus-journey-image");
        VBox details = new VBox(12, strong(title), paragraph(subtitle), busFeatureList(local, points));
        if (local) {
            details.getChildren().add(citiLinkPanel());
        }
        HBox main = new HBox(16, image, details);
        main.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(details, Priority.ALWAYS);
        Button button = new Button(action + "  \u2192");
        button.getStyleClass().add(local ? "bus-local-action" : "bus-intercity-action");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> onAction.run());
        VBox card = new VBox(14, main, button);
        card.getStyleClass().addAll("bus-journey-card", local ? "bus-local-card" : "bus-intercity-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox busFeatureList(boolean local, String... points) {
        VBox list = new VBox(8);
        for (String point : points) {
            Label feature = new Label((local ? "●  " : "●  ") + point);
            feature.getStyleClass().add(local ? "bus-local-feature" : "bus-intercity-feature");
            list.getChildren().add(feature);
        }
        return list;
    }

    private HBox citiLinkPanel() {
        Button routes = new Button("View Routes  \u2197");
        routes.getStyleClass().add("citilink-routes-button");
        routes.setOnAction(event -> openUrl("https://citilinc.nmc.gov.in/"));
        HBox panel = new HBox(10, new VBox(2, smallGold("NASHIK CITILINK"), muted("Nashik CitiLink - City Bus Service")), createSpacer(), routes);
        panel.getStyleClass().add("citilink-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        return panel;
    }

    private HBox busTravelTip() {
        VBox copy = new VBox(5, new HBox(10, AppUi.symbolIcon("\uE946", "bus-tip-icon"), strong("Travel Tip")),
                muted("Visitors arriving from outside Nashik can first search intercity buses to Nashik.\nAfter reaching Nashik, use Local / Kumbh Transport to find nearby routes and important destinations."));
        ImageView art = createImage("/images/bus-tip-reference.png", 180, 78, 0.5, 0.5);
        HBox tip = new HBox(18, copy, createSpacer(), art);
        tip.getStyleClass().add("bus-travel-tip");
        tip.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        return tip;
    }

    private VBox transportChoiceCard(String title, String detail, String action, Runnable onAction) {
        Button button = new Button(action);
        button.getStyleClass().add("transport-primary-button");
        button.setOnAction(event -> onAction.run());
        VBox card = new VBox(8, new HBox(10, moduleIcon("transport", "pilgrim-card-icon"), badge("BUS")),
                strong(title), paragraph(detail), button);
        card.getStyleClass().add("transport-choice-card");
        return card;
    }

    private void showTransportLanding() {
        showModulePage("transport");
    }

    private Button transportBackButton(String text, Runnable action) {
        Button back = new Button("\u2190  " + text);
        back.getStyleClass().add("transport-detail-back-button");
        back.setOnAction(event -> action.run());
        return back;
    }

    private VBox outsideJourneyWelcome() {
        VBox copy = new VBox(5, smallGold("INTERCITY BUS"), strong("Travel smart to Nashik Kumbh"),
                muted("Choose your starting city, journey date and travel preference. We will guide you to trusted official booking websites."));
        Label icon = new Label("\uD83D\uDE8C");
        icon.getStyleClass().add("outside-journey-icon");
        HBox banner = new HBox(16, icon, copy, createSpacer(),
                badge("Official providers only"));
        banner.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(banner);
        panel.getStyleClass().add("outside-journey-welcome");
        return panel;
    }

    private StackPane outsideJourneyHero() {
        ImageView image = createImage("/images/transport-bus-reference.png", 1100, 130, 0.5, 0.5);
        image.getStyleClass().add("outside-journey-hero-image");
        VBox text = new VBox(4, smallGold("INTERCITY BUS PLANNER"), label("Travel to Nashik with confidence", "outside-journey-hero-title"),
                muted("Search your city, select a date and continue through trusted official providers."));
        text.setPadding(new Insets(18, 28, 18, 28));
        StackPane.setAlignment(text, Pos.CENTER_LEFT);
        StackPane hero = new StackPane(image, text);
        hero.getStyleClass().add("outside-journey-hero");
        return hero;
    }

    private HBox outsideJourneyBenefits() {
        HBox benefits = new HBox(12,
                journeyBenefit("\uE707", "India-wide routes", "Search from hundreds of cities"),
                journeyBenefit("\uE787", "Plan ahead", "Pick the date that suits you"),
                journeyBenefit("\uE8A5", "Trusted websites", "Book with official providers"));
        benefits.getStyleClass().add("outside-journey-benefits");
        return benefits;
    }

    private HBox journeyBenefit(String icon, String title, String detail) {
        HBox item = new HBox(10, AppUi.symbolIcon(icon, "outside-benefit-icon"), new VBox(2, strong(title), muted(detail)));
        item.getStyleClass().add("outside-benefit-item");
        item.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(item, Priority.ALWAYS);
        return item;
    }

    private void showOutsideNashikJourney() {
        showOutsideNashikJourney(null);
    }

    private void showOutsideNashikJourney(JourneySearchState savedSearch) {
        if (savedSearch != null) {
            selectedFromLocation = savedSearch.from();
            selectedToLocation = savedSearch.to();
        }
        ObjectProperty<LocationOption> selectedFromLocationProperty = new SimpleObjectProperty<>(selectedFromLocation);
        ObjectProperty<LocationOption> selectedToLocationProperty = new SimpleObjectProperty<>(selectedToLocation);
        selectedFromLocationProperty.addListener((observable, oldLocation, newLocation) -> selectedFromLocation = newLocation);
        selectedToLocationProperty.addListener((observable, oldLocation, newLocation) -> selectedToLocation = newLocation);
        ComboBox<LocationOption> from = locationCombo("Search city, village, taluka or district", selectedFromLocationProperty);
        ComboBox<LocationOption> to = locationCombo("Search city, village, taluka or district", selectedToLocationProperty);
        DatePicker date = transportDatePicker();
        ComboBox<Integer> passengers = passengerCombo();
        ComboBox<String> preference = cityCombo("Choose preference", "Cheapest", "Fastest", "Less Walking", "Less Changes", "Family Friendly");
        preference.setValue("Cheapest");
        if (selectedFromLocation != null) {
            setSelectedLocation(from, selectedFromLocation, selectedFromLocationProperty);
        }
        if (selectedToLocation != null) {
            setSelectedLocation(to, selectedToLocation, selectedToLocationProperty);
        }
        if (savedSearch != null) {
            date.setValue(savedSearch.date());
            passengers.setValue(savedSearch.passengers());
        }

        Button plan = new Button("Search");
        plan.getStyleClass().add("transport-primary-button");
        plan.setOnAction(event -> {
            LocationOption selectedFrom = selectedFromLocation;
            LocationOption selectedTo = selectedToLocation;
            if (!validSelectedJourney(selectedFrom, selectedTo, date.getValue(), passengers.getValue())) {
                return;
            }
            showAvailableBuses(new JourneySearchState(selectedFrom, selectedTo, date.getValue(), passengers.getValue()));
        });

        Button swapCities = new Button("\uE8AB");
        swapCities.getStyleClass().add("outside-swap-button");
        swapCities.setAccessibleText("Swap From and To cities");
        swapCities.setOnAction(event -> {
            LocationOption oldFrom = selectedFromLocation;
            LocationOption oldTo = selectedToLocation;
            setSelectedLocation(from, oldTo, selectedFromLocationProperty);
            setSelectedLocation(to, oldFrom, selectedToLocationProperty);
        });
        VBox form = outsideJourneySearchForm(from, swapCities, to, date, passengers, plan);
        root.setCenter(scroll(pageShell("Outside Nashik Journey", "Compare travel modes for your journey to Nashik Kumbh.",
                transportBackButton("Back to Bus Transport", this::showBusOptions), outsideJourneyHero(), outsideJourneyWelcome(), form,
                outsideJourneyBenefits())));
    }

    private void showTravelModeResults(String from, String to, java.time.LocalDate date, Integer passengers, String preference) {
        Button bus = new Button("SEARCH BUS");
        bus.getStyleClass().add("transport-primary-button");
        bus.setOnAction(event -> showBusSearch(from, to, date, passengers));
        VBox busCard = new VBox(9, AppUi.symbolIcon("\uE806", "transport-mode-icon"), strong("BUS"),
                paragraph("Search official bus travel websites for " + from + " to " + to + "."), bus);
        busCard.getStyleClass().add("transport-mode-card");
        HBox.setHgrow(busCard, Priority.ALWAYS);
        HBox choices = new HBox(14, busCard,
                unavailableTravelCard("TRAIN", "Train journey planning will be added in a later part."),
                unavailableTravelCard("AIRPLANE", "Flight journey planning will be added in a later part."));
        root.setCenter(scroll(pageShell("Journey Options", "Preference: " + preference + "  •  " + passengers + " passenger(s)  •  " + date,
                transportBackButton("Back to Journey Planner", this::showOutsideNashikJourney), choices)));
    }

    private VBox unavailableTravelCard(String title, String detail) {
        VBox card = new VBox(9, strong(title), paragraph(detail), badge("Coming soon"));
        card.getStyleClass().add("transport-mode-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void showAvailableBuses(JourneySearchState search) {
        java.util.List<BusOption> buses = createBusOptions(search);
        VBox busList = new VBox(12);
        String[] activeFilter = { "All" };

        ComboBox<String> sort = new ComboBox<>();
        sort.getItems().addAll("Departure Time", "Arrival Time", "Price Low to High", "Price High to Low");
        sort.setValue("Departure Time");
        sort.getStyleClass().add("journey-combo");
        sort.setMaxWidth(230);

        HBox filters = new HBox(8);
        for (String filter : new String[] { "All", "AC", "Non-AC", "Sleeper", "Seater" }) {
            Button button = new Button(filter);
            button.getStyleClass().addAll("bus-filter-button", "All".equals(filter) ? "bus-filter-active" : "bus-filter-inactive");
            button.setOnAction(event -> {
                activeFilter[0] = filter;
                filters.getChildren().forEach(node -> node.getStyleClass().remove("bus-filter-active"));
                filters.getChildren().forEach(node -> {
                    if (!node.getStyleClass().contains("bus-filter-inactive")) {
                        node.getStyleClass().add("bus-filter-inactive");
                    }
                });
                button.getStyleClass().remove("bus-filter-inactive");
                button.getStyleClass().add("bus-filter-active");
                renderBusCards(busList, buses, activeFilter[0], sort.getValue(), search);
            });
            filters.getChildren().add(button);
        }

        sort.setOnAction(event -> renderBusCards(busList, buses, activeFilter[0], sort.getValue(), search));
        renderBusCards(busList, buses, activeFilter[0], sort.getValue(), search);

        HBox controls = new HBox(12, filters, createSpacer(), new VBox(5, muted("Sort by"), sort));
        controls.getStyleClass().add("available-bus-controls");
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox page = pageShell("Available Buses", "Choose a trusted provider and complete booking on the official website.",
                transportBackButton("Back to Search", () -> showOutsideNashikJourney(search)),
                journeySummaryCard(search), controls, busList);
        root.setCenter(scroll(page));
    }

    private VBox journeySummaryCard(JourneySearchState search) {
        Label route = label(search.from().displayName() + "  \u2192  " + search.to().displayName(), "available-route-title");
        Label detail = muted(formatJourneyDate(search.date()) + "  |  " + search.passengers() + (search.passengers() == 1 ? " Passenger" : " Passengers"));
        VBox summary = new VBox(7, smallGold("SELECTED JOURNEY"), route, detail);
        summary.getStyleClass().add("available-journey-summary");
        return summary;
    }

    private void renderBusCards(VBox container, java.util.List<BusOption> buses, String filter, String sort, JourneySearchState search) {
        java.util.stream.Stream<BusOption> stream = buses.stream().filter(bus -> matchesBusFilter(bus, filter));
        java.util.Comparator<BusOption> comparator = switch (sort == null ? "Departure Time" : sort) {
            case "Arrival Time" -> java.util.Comparator.comparing(BusOption::arrivalTime);
            case "Price Low to High" -> java.util.Comparator.comparingInt(BusOption::price);
            case "Price High to Low" -> java.util.Comparator.comparingInt(BusOption::price).reversed();
            default -> java.util.Comparator.comparing(BusOption::departureTime);
        };
        List<VBox> cards = stream.sorted(comparator).map(bus -> availableBusCard(bus, search)).toList();
        if (cards.isEmpty()) {
            container.getChildren().setAll(infoPanel("No buses found", "Try another filter or sorting option for this route."));
        } else {
            container.getChildren().setAll(cards);
        }
    }

    private boolean matchesBusFilter(BusOption bus, String filter) {
        if (filter == null || "All".equals(filter)) {
            return true;
        }
        return bus.busType().toLowerCase().contains(filter.toLowerCase());
    }

    private VBox availableBusCard(BusOption bus, JourneySearchState search) {
        Label operator = smallGold(bus.provider());
        Label name = label(bus.busName(), "available-bus-name");
        Label timing = label(bus.displayDepartureTime() + "  \u2192  " + bus.displayArrivalTime(), "available-bus-time");
        Label duration = muted("Duration: " + bus.duration());
        Label type = badge(bus.busType());
        Label seats = muted("Available Seats: " + bus.availableSeats());
        Label price = label("Rs. " + bus.price(), "available-bus-price");

        Button viewSeats = new Button("VIEW SEATS");
        viewSeats.getStyleClass().add("bus-secondary-button");
        viewSeats.setOnAction(event -> showInfo("Seat selection", "Seat selection is completed on the official booking website."));

        Button book = new Button("BOOK NOW");
        book.getStyleClass().add("transport-primary-button");
        book.setOnAction(event -> {
            openTravelWebsite(bus.bookingUrl());
            showInfo("External booking", "Complete seat selection, passenger details and payment on the trusted provider website. Simhastha Connect does not collect payment details.");
        });

        HBox actions = new HBox(10, viewSeats, book);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox left = new VBox(7, operator, name, timing, duration, type);
        HBox right = new HBox(18, new VBox(5, seats, price), actions);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox cardRow = new HBox(18, left, createSpacer(), right);
        cardRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, cardRow, muted(search.from().name() + " to " + search.to().name() + " | Live fares and seats are confirmed on provider website."));
        card.getStyleClass().add("available-bus-card");
        return card;
    }

    private java.util.List<BusOption> createBusOptions(JourneySearchState search) {
        String routeKey = search.from().displayName() + "|" + search.to().displayName() + "|" + search.date();
        int seed = Math.abs(routeKey.hashCode());
        String[] providers = { "redBus Partner", "MSRTC / Official Operator", "AbhiBus Partner", "MakeMyTrip Partner", "Orange Travels", "Shivshahi Connect" };
        String[] names = { "Kumbh Express", "Nashik Darshan", "Simhastha Rider", "Godavari Link", "Pilgrim Comfort", "Maharashtra Highway" };
        String[] types = { "AC Seater", "Non-AC Seater", "AC Sleeper", "Non-AC Sleeper", "AC Seater/Sleeper" };
        String[] urls = { "https://www.redbus.in/", "https://msrtc.maharashtra.gov.in/", "https://www.abhibus.com/",
                "https://www.makemytrip.com/bus-tickets/", "https://www.orangetravels.in/", "https://www.redbus.in/" };
        java.util.List<BusOption> buses = new java.util.ArrayList<>();
        for (int index = 0; index < 6; index++) {
            int minutes = 360 + ((seed / (index + 3)) % 620) + (index * 35);
            java.time.LocalTime departure = java.time.LocalTime.of((minutes / 60) % 24, minutes % 60);
            int durationMinutes = 210 + ((seed / (index + 5)) % 190);
            java.time.LocalTime arrival = departure.plusMinutes(durationMinutes);
            int price = 320 + ((seed / (index + 7)) % 680);
            int seats = 6 + ((seed / (index + 9)) % 31);
            buses.add(new BusOption(providers[index], names[index], departure, arrival, formatDuration(durationMinutes),
                    types[(seed + index) % types.length], seats, price, urls[index]));
        }
        return buses;
    }

    private String formatDuration(int totalMinutes) {
        return (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m";
    }

    private void showBusSearch(String initialFrom, String initialTo, java.time.LocalDate initialDate, Integer initialPassengers) {
        ComboBox<String> from = cityCombo("Search city or taluka", allIndianCities());
        from.setValue(initialFrom);
        ComboBox<String> to = cityCombo("Search city or taluka", allIndianCities());
        to.setValue(initialTo == null || initialTo.isBlank() ? "Nashik" : initialTo);
        DatePicker date = transportDatePicker();
        date.setValue(initialDate);
        ComboBox<Integer> passengers = passengerCombo();
        passengers.setValue(initialPassengers == null ? 1 : initialPassengers);
        VBox results = new VBox(12);
        Button search = new Button("SEARCH");
        search.getStyleClass().add("transport-primary-button");
        search.setOnAction(event -> {
            String source = selectedValue(from);
            String destination = selectedValue(to);
            if (validJourney(source, destination, date.getValue(), passengers.getValue())) {
                results.getChildren().setAll(officialBusWebsites(source, destination, date.getValue()));
            }
        });
        VBox form = transportForm("Bus Search", journeyInput("FROM", from, "\uE707"), journeyInput("TO", to, "\uE707"),
                journeyInput("DATE", date, "\uE787"), journeyInput("PASSENGERS", passengers, "\uE716"), search);
        root.setCenter(scroll(pageShell("Bus Journey", "Search official travel providers. Booking and availability are handled on each provider's website.",
                transportBackButton("Back to Journey Options", () -> showTravelModeResults(initialFrom, initialTo, initialDate, initialPassengers, "Cheapest")),
                form, results)));
    }

    private VBox officialBusWebsites(String from, String to, java.time.LocalDate date) {
        VBox cards = new VBox(10, sectionTitle("Official Travel Websites"));
        HBox sites = new HBox(12,
                travelWebsiteCard("🚌", "redBus", "Search bus routes and availability on redBus.", "https://www.redbus.in/", date),
                travelWebsiteCard("🚌", "AbhiBus", "Check schedules and booking options on AbhiBus.", "https://www.abhibus.com/", date),
                travelWebsiteCard("🚌", "MSRTC", "Maharashtra State Road Transport official website.", "https://msrtc.maharashtra.gov.in/", date),
                travelWebsiteCard("🚌", "MakeMyTrip", "Search bus travel options on MakeMyTrip.", "https://www.makemytrip.com/bus-tickets/", date));
        cards.getChildren().addAll(sites, dateEntryNote(date), muted("Live availability, timings, fares and seat information are provided by the respective travel provider."));
        return cards;
    }

    private VBox officialWebsiteCard(String name, String url) {
        Button open = new Button("OPEN WEBSITE");
        open.getStyleClass().add("transport-primary-button");
        open.setOnAction(event -> openTravelWebsite(url));
        VBox card = new VBox(9, AppUi.symbolIcon("\uE806", "transport-mode-icon"), strong(name), muted("Official website"), open);
        card.getStyleClass().add("transport-website-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void showTrainSearch() {
        ComboBox<String> from = cityCombo("Select Station", majorIndianStations());
        ComboBox<String> to = cityCombo("Select Station", majorIndianStations());
        DatePicker date = transportDatePicker();
        ComboBox<Integer> passengers = passengerCombo();
        Button search = new Button("SEARCH TRAINS");
        search.getStyleClass().add("transport-primary-button");
        VBox results = new VBox(12);
        search.setOnAction(event -> {
            if (validJourney(selectedValue(from), selectedValue(to), date.getValue(), passengers.getValue())) {
                results.getChildren().setAll(officialTrainWebsites(date.getValue()));
            }
        });
        VBox form = transportForm("TRAIN SEARCH", journeyInput("FROM", from, "\uE707"),
                journeyInput("TO", to, "\uE707"), journeyInput("DATE", date, "\uE787"),
                journeyInput("PASSENGERS", passengers, "\uE716"), search);
        form.getStyleClass().add("transport-train-form");
        root.setCenter(scroll(pageShell("Train Travel", "Search train travel websites for your journey to Nashik.",
                transportBackButton("Back to Transport", this::showTransportLanding), form, results)));
    }

    private VBox officialTrainWebsites(java.time.LocalDate date) {
        VBox cards = new VBox(10, sectionTitle("Official Train Travel Websites"));
        HBox sites = new HBox(12,
                travelWebsiteCard("🚆", "IRCTC", "Official Indian Railways online ticketing portal.", "https://www.irctc.co.in/nget/train-search", date),
                travelWebsiteCard("🚆", "MakeMyTrip Rail", "Search rail travel options on MakeMyTrip.", "https://www.makemytrip.com/railways/", date),
                travelWebsiteCard("🚆", "ConfirmTkt", "Check rail travel information on ConfirmTkt.", "https://www.confirmtkt.com/", date),
                travelWebsiteCard("🚆", "ixigo Trains", "Search train journeys on ixigo.", "https://www.ixigo.com/trains", date));
        cards.getChildren().addAll(sites, dateEntryNote(date), muted("Live availability, timings, fares and seat information are provided by the respective travel provider."));
        return cards;
    }

    private void showFlightSearch() {
        ComboBox<String> from = cityCombo("Select City/Airport", majorIndianAirports());
        ComboBox<String> to = cityCombo("Select City/Airport", majorIndianAirports());
        to.setValue("Nashik / Ozar Airport (ISK)");
        DatePicker date = transportDatePicker();
        ComboBox<Integer> passengers = passengerCombo();
        Button search = new Button("SEARCH FLIGHTS");
        search.getStyleClass().add("transport-primary-button");
        VBox results = new VBox(12);
        search.setOnAction(event -> {
            if (validJourney(selectedValue(from), selectedValue(to), date.getValue(), passengers.getValue())) {
                results.getChildren().setAll(officialFlightWebsites(date.getValue()));
            }
        });
        VBox form = transportForm("FLIGHT SEARCH", journeyInput("FROM", from, "\uE707"),
                journeyInput("TO", to, "\uE707"), journeyInput("DATE", date, "\uE787"),
                journeyInput("PASSENGERS", passengers, "\uE716"), search);
        form.getStyleClass().add("transport-flight-form");
        root.setCenter(scroll(pageShell("Flight Travel", "Search flight travel websites for your journey to Nashik.",
                transportBackButton("Back to Transport", this::showTransportLanding), form, results)));
    }

    private VBox officialFlightWebsites(java.time.LocalDate date) {
        VBox cards = new VBox(10, sectionTitle("Official Flight Travel Websites"));
        HBox sites = new HBox(12,
                travelWebsiteCard("✈", "MakeMyTrip", "Search domestic flight options on MakeMyTrip.", "https://www.makemytrip.com/flights/", date),
                travelWebsiteCard("✈", "Yatra", "Search flights on Yatra.", "https://www.yatra.com/flights", date),
                travelWebsiteCard("✈", "EaseMyTrip", "Search flights on EaseMyTrip.", "https://www.easemytrip.com/flights.html", date),
                travelWebsiteCard("✈", "Air India", "Official Air India website.", "https://www.airindia.com/", date));
        cards.getChildren().addAll(sites, dateEntryNote(date), muted("Live availability, timings, fares and seat information are provided by the respective travel provider."));
        return cards;
    }

    private VBox datedTravelWebsiteCard(String name, String url, java.time.LocalDate date, String icon) {
        Button open = new Button("OPEN WEBSITE");
        open.getStyleClass().add("transport-primary-button");
        open.setOnAction(event -> {
            openTravelWebsite(url);
            showInfo("Enter journey date", "Please enter the selected journey date: " + formatJourneyDate(date));
        });
        VBox card = new VBox(9, AppUi.symbolIcon(icon, "transport-mode-icon"), strong(name), muted("Official website"), open);
        card.getStyleClass().add("transport-website-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox travelWebsiteCard(String icon, String name, String description, String url, java.time.LocalDate date) {
        Button open = new Button("OPEN WEBSITE");
        open.getStyleClass().add("transport-primary-button");
        open.setOnAction(event -> {
            openTravelWebsite(url);
            showInfo("Enter journey date", "Please enter the selected journey date: " + formatJourneyDate(date));
        });
        Label websiteIcon = new Label(icon);
        websiteIcon.getStyleClass().add("transport-provider-icon");
        VBox card = new VBox(9, websiteIcon, strong(name), paragraph(description), open);
        card.getStyleClass().add("transport-website-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Label dateEntryNote(java.time.LocalDate date) {
        return muted("Please enter the selected journey date: " + formatJourneyDate(date)
                + ". These websites are opened for manual search because date transfer is not confirmed.");
    }

    private String formatJourneyDate(java.time.LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM uuuu"));
    }

    private void showLocalTransport() {
        ComboBox<String> from = cityCombo("Select starting point", "Nashik CBS", "Nashik Road Railway Station", "Panchavati", "Tapovan", "Ramkund", "Mahamarg Bus Stand");
        ComboBox<String> to = cityCombo("Select destination", "Ramkund", "Panchavati", "Tapovan", "Trimbakeshwar", "Kalaram Mandir");
        DatePicker date = transportDatePicker();
        VBox results = new VBox(12);
        Button search = new Button("SEARCH LOCAL TRANSPORT");
        search.getStyleClass().add("transport-primary-button");
        search.setOnAction(event -> {
            if (from.getValue() == null || from.getValue().isBlank() || to.getValue() == null || to.getValue().isBlank() || date.getValue() == null) {
                showInfo("Missing details", "Select From, To and a valid date before searching local transport.");
                return;
            }
            results.getChildren().setAll(localRouteCards(from.getValue(), to.getValue()));
        });
        VBox form = transportForm("Nashik Local & Kumbh Transport", journeyInput("FROM", from, "\uE707"),
                journeyInput("TO", to, "\uE707"), journeyInput("DATE", date, "\uE787"), search);
        root.setCenter(scroll(pageShell("Nashik Local & Kumbh Transport", "Route information only. Live GPS and ETA are not shown when no verified live source is connected.",
                transportBackButton("Back to Bus Transport", this::showBusOptions), form, results)));
    }

    private VBox localRouteCards(String from, String to) {
        VBox routes = new VBox(10, sectionTitle("Local Transport Routes"));
        routes.getChildren().addAll(localRouteCard("Kumbh Shuttle", from + " → " + to, "Check the official boarding point on arrival."),
                localRouteCard("Nashik City Bus", from + " → CBS / Citylink → " + to, "Route diversions may apply on crowd-control days."));
        return routes;
    }

    private VBox localRouteCard(String bus, String route, String nextStop) {
        VBox card = new VBox(6, new HBox(10, moduleIcon("transport", "pilgrim-card-icon"), strong(bus)),
                muted("Route: " + route), muted("Next stop: " + nextStop), badge("Live tracking unavailable"));
        card.getStyleClass().add("transport-route-card");
        return card;
    }

    private VBox transportForm(String title, Node... nodes) {
        VBox form = new VBox(14, sectionTitle(title));
        javafx.scene.layout.FlowPane fields = new javafx.scene.layout.FlowPane(12, 12);
        fields.setAlignment(Pos.CENTER_LEFT);
        fields.setRowValignment(javafx.geometry.VPos.BOTTOM);
        for (Node node : nodes) {
            fields.getChildren().add(node);
        }
        form.getChildren().add(fields);
        form.getStyleClass().add("journey-planner");
        return form;
    }

    private VBox outsideJourneySearchForm(ComboBox<LocationOption> from, Button swapCities, ComboBox<LocationOption> to,
            DatePicker date, ComboBox<Integer> passengers, Button search) {
        VBox fromBox = journeyInput("From City", from, "\uE707");
        VBox toBox = journeyInput("To City", to, "\uE707");
        VBox dateBox = journeyInput("Date of Journey", date, "\uE787");
        VBox passengerBox = journeyInput("Passengers", passengers, "\uE716");

        HBox fields = new HBox(10, fromBox, swapCities, toBox, dateBox, passengerBox, search);
        fields.setAlignment(Pos.BOTTOM_LEFT);
        fields.getStyleClass().add("outside-journey-search-row");

        VBox form = new VBox(12, sectionTitle("Plan Your Journey to Nashik Kumbh"), fields);
        form.getStyleClass().addAll("journey-planner", "outside-journey-form");

        fromBox.setMinWidth(0);
        toBox.setMinWidth(0);
        dateBox.setMinWidth(0);
        passengerBox.setMinWidth(0);
        fromBox.prefWidthProperty().bind(form.widthProperty().multiply(0.23));
        toBox.prefWidthProperty().bind(form.widthProperty().multiply(0.23));
        dateBox.prefWidthProperty().bind(form.widthProperty().multiply(0.18));
        passengerBox.prefWidthProperty().bind(form.widthProperty().multiply(0.12));
        search.prefWidthProperty().bind(form.widthProperty().multiply(0.12));
        search.setMaxWidth(Double.MAX_VALUE);
        return form;
    }

    private ComboBox<LocationOption> locationCombo(String prompt, ObjectProperty<LocationOption> selectedLocationProperty) {
        ComboBox<LocationOption> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText(prompt);
        combo.getEditor().setPromptText("Type village / gaon name");
        combo.setVisibleRowCount(8);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().addAll("journey-combo", "location-autocomplete");
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocationOption location) {
                return location == null ? "" : location.displayName();
            }

            @Override
            public LocationOption fromString(String text) {
                return null;
            }
        });
        Label noResults = new Label("No village / location found");
        noResults.getStyleClass().add("city-search-empty");
        combo.setPlaceholder(noResults);
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LocationOption location, boolean empty) {
                super.updateItem(location, empty);
                if (empty || location == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label pin = new Label("\uE707");
                pin.getStyleClass().add("location-result-pin");
                Label name = new Label(location.name());
                name.getStyleClass().add("location-result-name");
                Label detail = new Label(location.details());
                detail.getStyleClass().add("location-result-detail");
                VBox copy = new VBox(1, name, detail);
                HBox row = new HBox(8, pin, copy);
                row.getStyleClass().add("location-search-result");
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LocationOption location, boolean empty) {
                super.updateItem(location, empty);
                setText(empty || location == null ? "" : location.displayName());
            }
        });

        combo.valueProperty().addListener((observable, oldLocation, newLocation) -> {
            if (Boolean.TRUE.equals(combo.getProperties().get("locationSelectionInProgress"))) {
                return;
            }
            if (newLocation != null) {
                selectedLocationProperty.set(newLocation);
                combo.getProperties().put("locationSelectionInProgress", true);
                try {
                    combo.getEditor().setText(newLocation.displayName());
                    combo.getEditor().positionCaret(combo.getEditor().getText().length());
                } finally {
                    combo.getProperties().remove("locationSelectionInProgress");
                }
                System.out.println("LOCATION SELECTED: " + newLocation.displayName());
                javafx.application.Platform.runLater(combo::hide);
            }
        });

        java.util.function.Consumer<String> filter = text -> {
            String query = text == null ? "" : text.trim().toLowerCase();
            int requestId = nextLocationRequestId(combo);
            LOCATION_SEARCH_EXECUTOR.execute(() -> {
                java.util.stream.Stream<LocationOption> locations = query.isBlank()
                        ? suggestedJourneyLocations().stream()
                        : INDIA_LOCATION_OPTIONS.stream().filter(location -> location.matches(query));
                java.util.List<LocationOption> matches = locations.limit(LOCATION_SEARCH_LIMIT).toList();
                javafx.application.Platform.runLater(() -> {
                    if (currentLocationRequestId(combo) == requestId) {
                        setLocationItemsPreservingSelection(combo, matches, selectedLocationProperty);
                    }
                });
            });
        };
        combo.getEditor().textProperty().addListener((observable, previous, text) -> {
            if (Boolean.TRUE.equals(combo.getProperties().get("locationSelectionInProgress"))) {
                return;
            }
            LocationOption selected = selectedLocationProperty.get();
            if (selected != null && !selected.displayName().equals(text)) {
                clearSelectedLocation(combo, selectedLocationProperty);
            }
            filter.accept(text);
            if (combo.isFocused() && !combo.isShowing()) {
                combo.show();
            }
        });
        combo.setOnShowing(event -> filter.accept(combo.getEditor().getText()));
        combo.getEditor().setOnMouseClicked(event -> combo.show());
        combo.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                LocationOption selected = selectedLocationProperty.get();
                if (selected != null) {
                    combo.getProperties().put("locationSelectionInProgress", true);
                    try {
                        combo.setValue(selected);
                        combo.getEditor().setText(selected.displayName());
                        combo.getEditor().positionCaret(combo.getEditor().getText().length());
                    } finally {
                        combo.getProperties().remove("locationSelectionInProgress");
                    }
                }
            }
        });
        combo.getEditor().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> {
                    combo.hide();
                    event.consume();
                }
                case ENTER -> {
                    if (combo.isShowing() && !combo.getItems().isEmpty()) {
                        int index = combo.getSelectionModel().getSelectedIndex();
                        combo.setValue(combo.getItems().get(index < 0 ? 0 : index));
                        event.consume();
                    }
                }
                case DOWN -> {
                    combo.show();
                    if (!combo.getItems().isEmpty()) {
                        int index = combo.getSelectionModel().getSelectedIndex();
                        combo.getSelectionModel().select(Math.min(index + 1, combo.getItems().size() - 1));
                    }
                    event.consume();
                }
                case UP -> {
                    combo.show();
                    if (!combo.getItems().isEmpty()) {
                        int index = combo.getSelectionModel().getSelectedIndex();
                        combo.getSelectionModel().select(index <= 0 ? 0 : index - 1);
                    }
                    event.consume();
                }
                default -> { }
            }
        });
        filter.accept("");
        return combo;
    }

    private void setLocationItemsPreservingSelection(ComboBox<LocationOption> combo, java.util.List<LocationOption> matches,
            ObjectProperty<LocationOption> selectedLocationProperty) {
        LocationOption selected = selectedLocationProperty.get();
        java.util.List<LocationOption> items = new java.util.ArrayList<>(matches);
        if (selected != null && !items.contains(selected)) {
            items.add(0, selected);
            if (items.size() > LOCATION_SEARCH_LIMIT) {
                items = new java.util.ArrayList<>(items.subList(0, LOCATION_SEARCH_LIMIT));
            }
        }
        combo.getProperties().put("locationSelectionInProgress", true);
        try {
            combo.getItems().setAll(items);
            if (selected != null) {
                combo.setValue(selected);
                combo.getEditor().setText(selected.displayName());
                combo.getEditor().positionCaret(combo.getEditor().getText().length());
            }
        } finally {
            combo.getProperties().remove("locationSelectionInProgress");
        }
    }

    private int nextLocationRequestId(ComboBox<LocationOption> combo) {
        Object requestId = combo.getProperties().getOrDefault("locationRequestId", 0);
        int next = ((Integer) requestId) + 1;
        combo.getProperties().put("locationRequestId", next);
        return next;
    }

    private int currentLocationRequestId(ComboBox<LocationOption> combo) {
        Object requestId = combo.getProperties().getOrDefault("locationRequestId", 0);
        return (Integer) requestId;
    }

    private void setSelectedLocation(ComboBox<LocationOption> combo, LocationOption location, ObjectProperty<LocationOption> selectedLocationProperty) {
        combo.getProperties().put("locationSelectionInProgress", true);
        try {
            if (location == null) {
                selectedLocationProperty.set(null);
                return;
            }
            selectedLocationProperty.set(location);
            if (!combo.getItems().contains(location)) {
                combo.getItems().setAll(INDIA_LOCATION_OPTIONS.stream()
                        .filter(option -> option.matches(location.name()) || option.equals(location))
                        .limit(LOCATION_SEARCH_LIMIT)
                        .toList());
            }
            combo.getSelectionModel().select(location);
            combo.setValue(location);
            combo.getEditor().setText(location.displayName());
            combo.getEditor().positionCaret(combo.getEditor().getText().length());
        } finally {
            combo.getProperties().remove("locationSelectionInProgress");
        }
    }

    private void clearSelectedLocation(ComboBox<LocationOption> combo, ObjectProperty<LocationOption> selectedLocationProperty) {
        selectedLocationProperty.set(null);
    }

    private boolean validSelectedJourney(LocationOption from, LocationOption to, java.time.LocalDate date, Integer passengers) {
        if (from == null || to == null) {
            showInfo("Search details required", "Please select a valid location from the search results.");
            return false;
        }
        return validJourney(from.displayName(), to.displayName(), date, passengers);
    }

    private ComboBox<String> cityCombo(String prompt, String... values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setPromptText(prompt);
        combo.getItems().addAll(values);
        combo.setEditable(true);
        combo.setVisibleRowCount(12);
        combo.getEditor().setPromptText("Type to search city / taluka");
        Label noResults = new Label("No city or taluka found. You may type a location manually.");
        noResults.getStyleClass().add("city-search-empty");
        combo.setPlaceholder(noResults);
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().remove("city-search-result");
                if (!empty) {
                    getStyleClass().add("city-search-result");
                }
            }
        });
        java.util.List<String> allValues = java.util.List.of(values);
        combo.getEditor().textProperty().addListener((observable, oldText, text) -> {
            String query = text == null ? "" : text.trim().toLowerCase();
            combo.getItems().setAll(allValues.stream()
                    .filter(value -> value.toLowerCase().contains(query))
                    .toList());
            if (!query.isEmpty() && !combo.isShowing()) {
                combo.show();
            }
        });
        combo.setOnShowing(event -> {
            String query = combo.getEditor().getText() == null ? "" : combo.getEditor().getText().trim().toLowerCase();
            if ("nashik".equals(query) && "nashik".equalsIgnoreCase(combo.getValue())) {
                query = "";
            }
            final String searchQuery = query;
            combo.getItems().setAll(allValues.stream()
                    .filter(value -> value.toLowerCase().contains(searchQuery))
                    .toList());
        });
        combo.getEditor().setOnMouseClicked(event -> combo.show());
        combo.getEditor().setOnKeyReleased(event -> {
            String query = combo.getEditor().getText() == null ? "" : combo.getEditor().getText().trim().toLowerCase();
            combo.getItems().setAll(allValues.stream()
                    .filter(value -> value.toLowerCase().contains(query))
                    .toList());
            combo.show();
        });
        combo.setOnAction(event -> {
            String selected = combo.getValue();
            if (selected != null) {
                combo.getEditor().setText(selected);
                combo.hide();
            }
        });
        combo.getStyleClass().add("journey-combo");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private HBox journeyFormHint() {
        HBox hint = new HBox(9, AppUi.symbolIcon("\uE946", "journey-form-hint-icon"),
                muted("Select or type any Indian city in both From and To. You can search the dropdown by city name."));
        hint.getStyleClass().add("journey-form-hint");
        hint.setAlignment(Pos.CENTER_LEFT);
        return hint;
    }

    private String selectedValue(ComboBox<String> combo) {
        String editorText = combo.isEditable() ? combo.getEditor().getText() : null;
        if (editorText != null && !editorText.isBlank()) {
            return editorText.trim();
        }
        return combo.getValue();
    }

    private String[] allIndianCities() {
        return INDIA_LOCATION_OPTIONS.stream().map(LocationOption::displayName).toArray(String[]::new);
    }

    private static java.util.List<LocationOption> suggestedJourneyLocations() {
        return INDIA_LOCATION_OPTIONS.stream()
                .filter(location -> java.util.Set.of("Nashik", "Sinnar", "Shirdi", "Rahuri", "Rahata", "Trimbak",
                        "Pune", "Mumbai", "Nagpur", "Ahmednagar", "Aurangabad", "Delhi").contains(location.name()))
                .limit(LOCATION_SEARCH_LIMIT)
                .toList();
    }

    private static java.util.List<LocationOption> loadIndiaLocationOptions() {
        java.util.LinkedHashMap<String, LocationOption> options = new java.util.LinkedHashMap<>();
        for (String city : baseIndianCities()) {
            addLocation(options, new LocationOption("city-" + city.toLowerCase().replaceAll("[^a-z0-9]+", "-"), city,
                    "City", "", "India"));
        }
        for (LocationOption location : supplementalVillageLocations()) {
            addLocation(options, location);
        }
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                DashboardPage.class.getResourceAsStream("/data/india-subdistricts-lgd.csv"), java.nio.charset.StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1);
                if (columns.length > 7 && !columns[7].isBlank()) {
                    String state = toLocationName(columns[2]);
                    String district = toLocationName(columns[4]);
                    String name = columns[7].trim();
                    addLocation(options, new LocationOption("subdistrict-" + columns[5], name, "Taluka", district, state));
                    addLocation(options, new LocationOption("district-" + columns[3], district, "District", district, state));
                }
            }
        } catch (Exception ignored) {
            // The manually supplied city list remains usable if the local resource is unavailable.
        }
        return options.values().stream().sorted(java.util.Comparator.comparingInt(DashboardPage::locationPriority)
                .thenComparing(LocationOption::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(LocationOption::type)).toList();
    }

    private static void addLocation(java.util.Map<String, LocationOption> options, LocationOption location) {
        options.putIfAbsent(location.id(), location);
    }

    private static int locationPriority(LocationOption location) {
        return switch (location.type().toLowerCase()) {
            case "village" -> 0;
            case "town", "locality" -> 1;
            case "taluka" -> 2;
            case "district" -> 3;
            default -> 4;
        };
    }

    private static String toLocationName(String value) {
        String lowerCase = value == null ? "" : value.trim().toLowerCase();
        StringBuilder display = new StringBuilder(lowerCase.length());
        boolean capitalize = true;
        for (char character : lowerCase.toCharArray()) {
            display.append(capitalize ? Character.toUpperCase(character) : character);
            capitalize = character == ' ' || character == '-' || character == '/';
        }
        return display.toString();
    }

    private record LocationOption(String id, String name, String type, String district, String state) {
        private String displayName() {
            return district == null || district.isBlank() ? name : name + ", " + district + ", " + state;
        }

        private String details() {
            if ("District".equalsIgnoreCase(type)) {
                return name + " District, " + state;
            }
            if ("Taluka".equalsIgnoreCase(type)) {
                return name + " Taluka, " + district + ", " + state;
            }
            return district == null || district.isBlank() ? state : district + ", " + state;
        }

        private boolean matches(String query) {
            if (query == null || query.isBlank()) {
                return true;
            }
            String searchable = (name + " " + type + " " + district + " " + state).toLowerCase();
            return searchable.contains(query.toLowerCase());
        }
    }

    private record JourneySearchState(LocationOption from, LocationOption to, java.time.LocalDate date, int passengers) { }

    private record BusOption(String provider, String busName, java.time.LocalTime departureTime, java.time.LocalTime arrivalTime,
            String duration, String busType, int availableSeats, int price, String bookingUrl) {
        private static final java.time.format.DateTimeFormatter TIME_FORMAT =
                java.time.format.DateTimeFormatter.ofPattern("hh:mm a");

        private String displayDepartureTime() {
            return departureTime.format(TIME_FORMAT);
        }

        private String displayArrivalTime() {
            return arrivalTime.format(TIME_FORMAT);
        }
    }

    private static String[] baseIndianCities() {
        return new String[] {
                "Agartala", "Agra", "Ahmedabad", "Ahmednagar", "Aizawl", "Ajmer", "Akola", "Alappuzha", "Aligarh", "Alwar", "Amaravati", "Ambala", "Amravati", "Amritsar", "Anand", "Anantapur", "Asansol", "Aurangabad", "Ayodhya",
                "Badlapur", "Bagalkot", "Balasore", "Ballari", "Banda", "Bengaluru", "Berhampur", "Bhagalpur", "Bharatpur", "Bharuch", "Bhavnagar", "Bhilai", "Bhilwara", "Bhiwandi", "Bhiwani", "Bhopal", "Bhubaneswar", "Bidar", "Bikaner", "Bilaspur", "Bokaro", "Bongaigaon", "Brahmapur", "Bulandshahr",
                "Chandigarh", "Chandrapur", "Chennai", "Chhatrapati Sambhajinagar (Aurangabad)", "Chhindwara", "Chittorgarh", "Coimbatore", "Cuttack",
                "Darbhanga", "Darjeeling", "Davangere", "Dehradun", "Deoghar", "Dewas", "Dhanbad", "Dharamshala", "Dharwad", "Dibrugarh", "Dimapur", "Durg", "Durgapur",
                "Eluru", "Erode", "Etawah",
                "Faridabad", "Firozabad", "Gandhidham", "Gandhinagar", "Gangtok", "Gaya", "Ghaziabad", "Goa / Panaji", "Gorakhpur", "Greater Noida", "Guntur", "Gurugram", "Guwahati", "Gwalior",
                "Haldia", "Haridwar", "Hisar", "Hosur", "Hubballi", "Hyderabad",
                "Imphal", "Indore", "Itanagar",
                "Jabalpur", "Jaipur", "Jalandhar", "Jalgaon", "Jalna", "Jammu", "Jamnagar", "Jamshedpur", "Jaunpur", "Jhansi", "Jodhpur", "Jorhat", "Junagadh",
                "Kakinada", "Kalaburagi", "Kannur", "Kanpur", "Karimnagar", "Karnal", "Katni", "Kavaratti", "Kochi", "Kolhapur", "Kolkata", "Kollam", "Kota", "Kottayam", "Kozhikode", "Kurnool",
                "Latur", "Leh", "Lucknow", "Ludhiana",
                "Madurai", "Mangaluru", "Mathura", "Meerut", "Mira-Bhayandar", "Modinagar", "Moradabad", "Morbi", "Mumbai", "Munger", "Muzaffarnagar", "Muzaffarpur", "Mysuru",
                "Nagpur", "Nanded", "Nashik", "Navi Mumbai", "Nellore", "New Delhi", "Nizamabad", "Noida",
                "Ongole",
                "Palakkad", "Panipat", "Parbhani", "Patiala", "Patna", "Phagwara", "Pimpri-Chinchwad", "Port Blair", "Prayagraj", "Puducherry", "Pune", "Puri",
                "Raipur", "Rajahmundry", "Rajkot", "Ranchi", "Ratlam", "Rewa", "Rewari", "Rohtak", "Roorkee", "Rourkela",
                "Sagar", "Salem", "Sambalpur", "Sangli", "Sasaram", "Satara", "Shillong", "Shimla", "Shirdi", "Shivamogga", "Sikar", "Siliguri", "Srinagar", "Solapur", "Sonipat", "Sri Ganganagar", "Srikakulam", "Srinagar", "Surat", "Surendranagar",
                "Thane", "Thanjavur", "Thiruvananthapuram", "Thrissur", "Tiruchirappalli", "Tirunelveli", "Tirupati", "Tumakuru",
                "Udaipur", "Udupi", "Ujjain", "Ulhasnagar", "Una", "Vadodara", "Varanasi", "Vellore", "Vijayawada", "Visakhapatnam", "Vizianagaram",
                "Warangal", "Yamunanagar",

                "Nashik Taluka", "Niphad Taluka", "Dindori Taluka", "Igatpuri Taluka", "Kalwan Taluka", "Malegaon Taluka", "Baglan / Satana Taluka", "Sinnar Taluka", "Yeola Taluka", "Nandgaon Taluka", "Chandwad Taluka", "Deola Taluka", "Trimbakeshwar Taluka", "Peth Taluka", "Surgana Taluka",
                "Ambegaon Taluka", "Baramati Taluka", "Bhor Taluka", "Daund Taluka", "Haveli Taluka", "Indapur Taluka", "Junnar Taluka", "Khed Taluka", "Maval Taluka", "Mulshi Taluka", "Purandar Taluka", "Shirur Taluka", "Velhe Taluka",
                "Karjat Taluka", "Khalapur Taluka", "Panvel Taluka", "Uran Taluka", "Alibag Taluka", "Mahad Taluka", "Mangaon Taluka", "Murud Taluka", "Pen Taluka", "Roha Taluka", "Shrivardhan Taluka", "Tala Taluka",
                "Akole Taluka", "Jamkhed Taluka", "Karjat Taluka (Ahmednagar)", "Kopargaon Taluka", "Nevasa Taluka", "Parner Taluka", "Pathardi Taluka", "Rahata Taluka", "Rahuri Taluka", "Sangamner Taluka", "Shevgaon Taluka", "Shrigonda Taluka", "Shrirampur Taluka",
                "Koregaon Taluka", "Khandala Taluka", "Man Taluka", "Patan Taluka", "Phaltan Taluka", "Wai Taluka", "Khatav Taluka", "Jaoli Taluka", "Mahabaleshwar Taluka", "Satara Taluka",
                "Miraj Taluka", "Tasgaon Taluka", "Kavathe Mahankal Taluka", "Khanapur Taluka", "Atpadi Taluka", "Jat Taluka", "Walwa Taluka", "Shirala Taluka", "Palus Taluka", "Kadegaon Taluka",
                "Chopda Taluka", "Erandol Taluka", "Jamner Taluka", "Pachora Taluka", "Raver Taluka", "Yawal Taluka", "Bhusawal Taluka", "Amalner Taluka", "Bodwad Taluka", "Dharangaon Taluka", "Muktainagar Taluka",
                "Bhiwapur Taluka", "Hingna Taluka", "Kalmeshwar Taluka", "Kamptee Taluka", "Katol Taluka", "Kuhi Taluka", "Mauda Taluka", "Narkhed Taluka", "Parseoni Taluka", "Ramtek Taluka", "Savner Taluka", "Umred Taluka",
                "Mokhada Taluka", "Palghar Taluka", "Dahanu Taluka", "Talasari Taluka", "Vasai Taluka", "Wada Taluka", "Vikramgad Taluka", "Jawhar Taluka",
                "Kankavli Taluka", "Kudal Taluka", "Malvan Taluka", "Sawantwadi Taluka", "Vengurla Taluka", "Devgad Taluka", "Vaibhavwadi Taluka", "Dodamarg Taluka"
        };
    }

    private static LocationOption[] supplementalVillageLocations() {
        return new LocationOption[] {
                new LocationOption("village-anjaneri-nashik", "Anjaneri", "Village", "Nashik", "Maharashtra"),
                new LocationOption("village-matori-nashik", "Matori", "Village", "Nashik", "Maharashtra"),
                new LocationOption("village-dugaon-nashik", "Dugaon", "Village", "Nashik", "Maharashtra"),
                new LocationOption("village-saykheda-nashik", "Saykheda", "Village", "Nashik", "Maharashtra"),
                new LocationOption("village-nandur-naka-nashik", "Nandur Naka", "Locality", "Nashik", "Maharashtra"),
                new LocationOption("village-gangapur-nashik", "Gangapur", "Village", "Nashik", "Maharashtra"),
                new LocationOption("village-trimbak-nashik", "Trimbak", "Village", "Nashik", "Maharashtra"),
                new LocationOption("town-sinnar-nashik", "Sinnar", "Town", "Nashik", "Maharashtra"),
                new LocationOption("village-shirdi-ahmednagar", "Shirdi", "Village", "Rahata Taluka, Ahmednagar", "Maharashtra"),
                new LocationOption("village-ghoti-nashik", "Ghoti", "Village", "Nashik", "Maharashtra"),
                new LocationOption("village-vani-nashik", "Vani", "Village", "Nashik", "Maharashtra"),
                new LocationOption("village-nanduri-nashik", "Nanduri", "Village", "Nashik", "Maharashtra"),
                new LocationOption("village-lasur-station-aurangabad", "Lasur Station", "Village", "Chhatrapati Sambhajinagar", "Maharashtra"),
                new LocationOption("village-shani-shingnapur-ahmednagar", "Shani Shingnapur", "Village", "Ahmednagar", "Maharashtra"),
                new LocationOption("village-ranjangaon-pune", "Ranjangaon", "Village", "Pune", "Maharashtra"),
                new LocationOption("village-alandi-pune", "Alandi", "Town", "Pune", "Maharashtra"),
                new LocationOption("village-jejuri-pune", "Jejuri", "Town", "Pune", "Maharashtra"),
                new LocationOption("village-mahabalipuram-chengalpattu", "Mahabalipuram", "Town", "Chengalpattu", "Tamil Nadu"),
                new LocationOption("village-hampi-vijayanagara", "Hampi", "Village", "Vijayanagara", "Karnataka"),
                new LocationOption("village-pushkar-ajmer", "Pushkar", "Town", "Ajmer", "Rajasthan"),
                new LocationOption("village-rishikesh-dehradun", "Rishikesh", "City", "Dehradun", "Uttarakhand"),
                new LocationOption("village-dwarka-devbhumi-dwarka", "Dwarka", "City", "Devbhumi Dwarka", "Gujarat")
        };
    }

    private String[] majorIndianStations() {
        return new String[] { "Nashik Road", "Manmad Junction", "Mumbai CSMT", "Mumbai Central", "Lokmanya Tilak Terminus", "Pune Junction", "Nagpur Junction", "New Delhi", "Delhi Junction", "Ahmedabad Junction", "Surat", "Vadodara Junction", "Indore Junction", "Bhopal Junction", "Jaipur Junction", "Kota Junction", "Varanasi Junction", "Prayagraj Junction", "Lucknow", "Hyderabad Deccan", "Bengaluru City", "Chennai Central", "Howrah (Kolkata)", "Shirdi Sainagar" };
    }

    private String[] majorIndianAirports() {
        return new String[] { "Nashik / Ozar Airport (ISK)", "Mumbai / Chhatrapati Shivaji Maharaj International Airport (BOM)", "Pune Airport (PNQ)", "Delhi / Indira Gandhi International Airport (DEL)", "Bengaluru / Kempegowda International Airport (BLR)", "Hyderabad / Rajiv Gandhi International Airport (HYD)", "Chennai International Airport (MAA)", "Kolkata / Netaji Subhas Chandra Bose International Airport (CCU)", "Ahmedabad / Sardar Vallabhbhai Patel International Airport (AMD)", "Jaipur International Airport (JAI)", "Goa / Manohar International Airport (GOX)", "Indore / Devi Ahilyabai Holkar Airport (IDR)", "Bhopal / Raja Bhoj Airport (BHO)", "Nagpur / Dr. Babasaheb Ambedkar International Airport (NAG)" };
    }

    private ComboBox<Integer> passengerCombo() {
        ComboBox<Integer> combo = new ComboBox<>();
        combo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        combo.setValue(1);
        combo.getStyleClass().add("journey-combo");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private DatePicker transportDatePicker() {
        DatePicker picker = new DatePicker(java.time.LocalDate.now());
        picker.setPromptText("Select Date");
        picker.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                setDisable(empty || value.isBefore(java.time.LocalDate.now()));
            }
        });
        picker.getStyleClass().add("journey-field");
        return picker;
    }

    private boolean validJourney(String from, String to, java.time.LocalDate date, Integer passengers) {
        if (from == null || from.isBlank()) {
            showInfo("Search details required", "Please select From City.");
            return false;
        }
        if (to == null || to.isBlank()) {
            showInfo("Search details required", "Please select To City.");
            return false;
        }
        if (from.trim().equalsIgnoreCase(to.trim())) {
            showInfo("Search details required", "From City and To City cannot be the same.");
            return false;
        }
        if (date == null) {
            showInfo("Search details required", "Please select Journey Date.");
            return false;
        }
        if (date.isBefore(java.time.LocalDate.now())) {
            showInfo("Search details required", "Please select today or a future date.");
            return false;
        }
        if (passengers == null || passengers < 1) {
            showInfo("Invalid passengers", "Select at least one passenger.");
            return false;
        }
        return true;
    }

    private void openTravelWebsite(String url) {
        String[] chromePaths = { System.getenv("ProgramFiles") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("ProgramFiles(x86)") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("LocalAppData") + "\\Google\\Chrome\\Application\\chrome.exe" };
        for (String chromePath : chromePaths) {
            if (chromePath != null && new java.io.File(chromePath).isFile()) {
                try {
                    new ProcessBuilder(chromePath, url).start();
                    return;
                } catch (java.io.IOException ignored) {
                    // Fall through to the operating system's default browser.
                }
            }
        }
        openUrl(url);
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
        VBox dynamic = new VBox(16);
        Button approved = new Button("View Approved Stays");
        approved.getStyleClass().add("primary-button");
        Button other = new Button("Search Other Stays");
        other.getStyleClass().add("pilgrim-small-action");
        VBox approvedCard = stayChoiceCard("SIMHASTHA CONNECT", "Approved Stays", "Discover trusted accommodation connected with SIMHASTHA CONNECT.", "Trusted Stay Listings  •  Property Details  •  Real Location  •  Internal Booking Flow", approved, "stay-approved-card");
        VBox otherCard = stayChoiceCard("EXPLORE NASHIK", "Other Stays", "Search hotels, OYO, lodges, dharamshalas, guest houses and homestays across Nashik.", "Hotels  •  OYO  •  Lodges  •  Dharamshalas  •  Guest Houses", other, "stay-other-card");
        approved.setOnAction(event -> {
            approvedCard.getStyleClass().add("stay-choice-active");
            otherCard.getStyleClass().remove("stay-choice-active");
            dynamic.getChildren().setAll(stayApprovedFlow(() -> dynamic.getChildren().setAll(stayOtherFlow())));
        });
        other.setOnAction(event -> {
            otherCard.getStyleClass().add("stay-choice-active");
            approvedCard.getStyleClass().remove("stay-choice-active");
            dynamic.getChildren().setAll(stayOtherFlow());
        });
        HBox cards = new HBox(14, approvedCard, otherCard);
        cards.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        return pageShell("Stay", "Find the right accommodation for your Simhastha journey.", stayHero(),
                sectionTitle("How would you like to find your stay?"), cards, dynamic);
    }

    private StackPane stayHero() {
        ImageView image = createImage("/images/godavari_kumbh.jpg", 980, 170, 0.54, 0.47);
        VBox copy = new VBox(7, badge("SIMHASTHA CONNECT  •  STAY"), label("Stay Close. Travel Easy. Experience Simhastha.", "pilgrim-hero-title"),
                label("Choose trusted SIMHASTHA CONNECT stays or explore accommodation across Nashik.", "pilgrim-hero-detail"));
        copy.setAlignment(Pos.CENTER_LEFT); copy.setPadding(new Insets(22, 28, 22, 28));
        StackPane hero = new StackPane(image, copy); StackPane.setAlignment(copy, Pos.CENTER_LEFT); hero.getStyleClass().add("pilgrim-hero"); hero.setMinHeight(170); return hero;
    }

    private VBox stayChoiceCard(String eyebrow, String title, String detail, String features, Button action, String style) {
        VBox card = new VBox(10, badge(eyebrow), strong(title), paragraph(detail), muted(features), action);
        card.getStyleClass().addAll("pilgrim-rich-card", "stay-choice-card", style); card.setMinHeight(225); HBox.setHgrow(card, Priority.ALWAYS); return card;
    }

    private VBox stayApprovedFlow(Runnable exploreOther) {
        ComboBox<String> location = stayCombo("Nearby Location", "All Nashik", "Panchavati", "Ramkund", "Tapovan", "Trimbakeshwar", "Nashik Road", "CBS", "Gangapur Road", "Ozar");
        DatePicker checkIn = stayDatePicker("Check-in");
        DatePicker checkOut = stayDatePicker("Check-out");
        ComboBox<String> guests = stayGuestsCombo();
        ComboBox<String> budget = stayCombo("Budget", "Any Budget", "Budget", "Standard", "Premium");
        Label trip = badge("Select dates and guests to plan your stay");
        Runnable updateTrip = () -> trip.setText(stayTripText(checkIn.getValue(), checkOut.getValue(), guests.getValue()));
        checkIn.valueProperty().addListener((obs, oldValue, newValue) -> updateTrip.run());
        checkOut.valueProperty().addListener((obs, oldValue, newValue) -> updateTrip.run());
        guests.valueProperty().addListener((obs, oldValue, newValue) -> updateTrip.run());
        VBox results = new VBox(14);
        VBox searchPanel = new VBox(9);
        Button find = new Button("Find Approved Stays"); find.getStyleClass().add("primary-button");
        find.setOnAction(event -> {
            if (!validStayDates(checkIn, checkOut)) return;
            updateTrip.run();
            results.getChildren().setAll(stayApprovedSection(stayCriteria(location, checkIn, checkOut, guests, budget), searchPanel::requestFocus, exploreOther));
        });
        FlowPane searchRow = new FlowPane(10, 10, location, checkIn, checkOut, guests, budget, find);
        searchRow.getStyleClass().add("stay-search-row");
        searchPanel.getChildren().setAll(strong("Find an Approved Stay"), muted("Choose your trip preferences to browse curated SIMHASTHA STAY listings."), searchRow, trip);
        searchPanel.getStyleClass().add("stay-search-panel");
        return new VBox(14, stayAccountActions(), searchPanel, results);
    }

    private VBox stayOtherFlow() {
        ComboBox<String> location = stayCombo("Nearby Location", "All Nashik", "Panchavati", "Ramkund", "Tapovan", "Trimbakeshwar", "Nashik Road", "CBS", "Gangapur Road", "Ozar");
        DatePicker checkIn = stayDatePicker("Check-in");
        DatePicker checkOut = stayDatePicker("Check-out");
        ComboBox<String> guests = stayGuestsCombo();
        ComboBox<String> budget = stayCombo("Budget", "Any Budget", "Budget", "Standard", "Premium");
        return stayOtherSection(location, checkIn, checkOut, guests, budget);
    }

    private HBox stayAccountActions() {
        Button recent = new Button("Recently Viewed"); recent.getStyleClass().add("pilgrim-small-action"); recent.setOnAction(event -> showStayCollection("Recently Viewed Stays", recentlyViewedStays, false));
        Button wishlist = new Button("♡ Wishlist"); wishlist.getStyleClass().add("pilgrim-small-action"); wishlist.setOnAction(event -> showStayCollection("Your Stay Wishlist", stayWishlist, true));
        Button bookings = new Button("My Bookings"); bookings.getStyleClass().add("pilgrim-small-action"); bookings.setOnAction(event -> showStayBookings());
        HBox actions = new HBox(9, recent, wishlist, bookings); actions.setAlignment(Pos.CENTER_LEFT); actions.getStyleClass().add("stay-account-actions"); return actions;
    }

    private void toggleStayWishlist(StayItem stay, Button button) {
        String key = stay.stableId();
        if (stayWishlist.remove(key) == null) { stayWishlist.put(key, stay); button.setText("♥ Saved"); }
        else button.setText("♡ Wishlist");
    }

    private void rememberStayViewed(StayItem stay) {
        String key = stay.stableId();
        recentlyViewedStays.remove(key); recentlyViewedStays.put(key, stay);
        while (recentlyViewedStays.size() > 8) recentlyViewedStays.remove(recentlyViewedStays.keySet().iterator().next());
    }

    private void showStayCollection(String title, java.util.LinkedHashMap<String, StayItem> collection, boolean removable) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(title); dialog.setHeaderText(null);
        VBox content = new VBox(10);
        if (collection.isEmpty()) {
            content.getChildren().add(muted(removable ? "Your Stay wishlist is empty." : "You haven't viewed any stays yet."));
        } else {
            java.util.List<StayItem> items = new java.util.ArrayList<>(collection.values()); java.util.Collections.reverse(items);
            for (StayItem stay : items) {
                Button details = new Button("View Details"); details.getStyleClass().add("pilgrim-small-action"); details.setOnAction(event -> showStayDetails(stay, StayCriteria.empty()));
                Button map = new Button("View on Map"); map.getStyleClass().add("pilgrim-small-action"); map.setOnAction(event -> openStayMap(stay));
                Button book = new Button("Book Stay"); book.getStyleClass().add("primary-button"); book.setOnAction(event -> showStayDetails(stay, StayCriteria.empty()));
                ImageView image = createImage(stay.localImagePath(), 116, 72, 0.54, 0.50);
                VBox detailsCopy = new VBox(3, strong(stay.name()), muted(stay.category() + " • " + stay.area()),
                        muted(stay.priceInfo().isBlank() ? "Price available during connected booking" : stay.priceInfo()));
                HBox row = new HBox(10, image, detailsCopy, createSpacer(), details, map, book);
                if (removable) { Button remove = new Button("Remove"); remove.getStyleClass().add("pilgrim-small-action"); remove.setOnAction(event -> { collection.remove(stay.stableId()); dialog.close(); }); row.getChildren().add(remove); }
                row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("stay-collection-row"); content.getChildren().add(row);
            }
        }
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(460);
        dialog.getDialogPane().setContent(scroll); dialog.getDialogPane().setPrefSize(940, 560); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); dialog.showAndWait();
    }

    private void showStayBookings() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("My Stay Bookings"); dialog.setHeaderText(null);
        VBox content = new VBox(10);
        java.util.List<AppDataStore.BookingRecord> stays = currentUserStayBookings();
        VBox rows = new VBox(8);
        Runnable render = () -> { rows.getChildren().setAll(stayBookingRows(stays, "all")); };
        Button upcoming = new Button("Upcoming"); upcoming.getStyleClass().add("pilgrim-small-action"); upcoming.setOnAction(event -> rows.getChildren().setAll(stayBookingRows(stays, "upcoming")));
        Button past = new Button("Completed / Past"); past.getStyleClass().add("pilgrim-small-action"); past.setOnAction(event -> rows.getChildren().setAll(stayBookingRows(stays, "past")));
        Button cancelled = new Button("Cancelled"); cancelled.getStyleClass().add("pilgrim-small-action"); cancelled.setOnAction(event -> rows.getChildren().setAll(stayBookingRows(stays, "cancelled")));
        if (stays.isEmpty()) {
            Button explore = new Button("Find a Stay"); explore.getStyleClass().add("primary-button"); explore.setOnAction(event -> dialog.close());
            content.getChildren().addAll(strong("Your Last Stay Booking"), muted("You haven't booked a stay yet."), explore);
        } else {
            AppDataStore.BookingRecord latest = stays.get(0);
            content.getChildren().addAll(strong("Your Last Stay Booking"), stayBookingRow(latest), new HBox(8, upcoming, past, cancelled), rows);
            render.run();
        }
        content.getStyleClass().add("stay-booking-summary"); dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); dialog.showAndWait();
    }

    private java.util.List<AppDataStore.BookingRecord> currentUserStayBookings() {
        AppSession.User currentUser = AppSession.currentUser();
        String userId = currentUser == null ? "" : currentUser.uid();
        return AppDataStore.bookings().stream()
                .filter(booking -> "stay".equalsIgnoreCase(booking.moduleType) && (userId.isBlank() || userId.equals(booking.userId)))
                .sorted(java.util.Comparator.comparing((AppDataStore.BookingRecord booking) -> booking.createdAt).reversed()).toList();
    }

    private java.util.List<Node> stayBookingRows(java.util.List<AppDataStore.BookingRecord> stays, String bucket) {
        java.util.List<Node> rows = stays.stream().filter(booking -> stayBookingBucket(booking).equals(bucket) || "all".equals(bucket)).map(this::stayBookingRow).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (rows.isEmpty()) rows.add(muted("No " + ("past".equals(bucket) ? "completed or past" : bucket) + " stay bookings."));
        return rows;
    }

    private String stayBookingBucket(AppDataStore.BookingRecord booking) {
        String status = booking.bookingStatus == null ? "" : booking.bookingStatus.toLowerCase();
        if (status.contains("cancel")) return "cancelled";
        if (status.contains("complete") || status.contains("past")) return "past";
        return "upcoming";
    }

    private HBox stayBookingRow(AppDataStore.BookingRecord booking) {
        Button map = new Button("View on Map"); map.getStyleClass().add("pilgrim-small-action");
        map.setOnAction(event -> openUrl("https://www.google.com/maps/search/?api=1&query=" + java.net.URLEncoder.encode(booking.title + " " + booking.location, java.nio.charset.StandardCharsets.UTF_8)));
        HBox row = new HBox(10, new VBox(3, strong(booking.title), muted(booking.location), muted(booking.dateText + " • " + booking.nights + " nights"), muted("Booking ID: " + booking.bookingId + " • " + booking.bookingStatus)), createSpacer(), map);
        row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("stay-collection-row"); return row;
    }

    private VBox stayApprovedSection(StayCriteria criteria, Runnable changeSearch, Runnable exploreOther) {
        TextField propertySearch = AppUi.textField("Search property");
        ComboBox<String> type = stayCombo("Property Type", "All Types", "Hotel", "Lodge", "Guest House", "Dharamshala", "Homestay", "Tent / Camp", "Other Accommodation");
        ComboBox<String> secondaryBudget = stayCombo("Budget", "Any Budget", "Budget", "Standard", "Premium");
        ComboBox<String> facility = stayCombo("Facilities", "All Facilities", "Parking", "Family Friendly", "Meals", "Wi-Fi", "Hot Water", "Accessible");
        ComboBox<String> sort = stayCombo("Sort by", "Recommended", "Property Name A-Z", "Budget First");
        VBox results = new VBox(12);
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> results.getChildren().setAll(stayResults(criteria.withBudget(secondaryBudget.getValue()), propertySearch.getText(), type.getValue(), facility.getValue(), sort.getValue(), exploreOther));
        propertySearch.textProperty().addListener((obs, oldValue, newValue) -> refresh[0].run());
        type.valueProperty().addListener((obs, oldValue, newValue) -> refresh[0].run());
        secondaryBudget.valueProperty().addListener((obs, oldValue, newValue) -> refresh[0].run());
        facility.valueProperty().addListener((obs, oldValue, newValue) -> refresh[0].run());
        sort.valueProperty().addListener((obs, oldValue, newValue) -> refresh[0].run());
        Button map = new Button("View Stays on Map");
        map.getStyleClass().add("pilgrim-small-action");
        map.setOnAction(event -> openStayAreaMap(criteria.location()));
        Button clear = new Button("Clear Filters");
        clear.getStyleClass().add("pilgrim-small-action");
        clear.setOnAction(event -> { propertySearch.clear(); type.setValue(null); secondaryBudget.setValue(null); facility.setValue(null); sort.setValue(null); });
        Button change = new Button("Change Search");
        change.getStyleClass().add("pilgrim-small-action");
        change.setOnAction(event -> changeSearch.run());
        FlowPane controls = new FlowPane(9, 9, propertySearch, type, secondaryBudget, facility, sort, map, clear, change);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("stay-results-controls");
        VBox section = new VBox(10, strong("Approved Stays"), muted("Based on your selected location and travel dates."), controls, results);
        section.getStyleClass().addAll("pilgrim-panel", "stay-results-panel");
        refresh[0].run();
        return section;
    }

    private Node stayResults(StayCriteria criteria, String text, String type, String facility, String sort, Runnable exploreOther) {
        java.util.List<StayItem> matches = approvedStayItems().stream()
                .filter(stay -> stayMatches(stay, criteria, text, type, facility))
                .sorted(stayComparator(criteria.location(), sort)).toList();
        String location = criteria.location() == null ? "Nashik" : criteria.location();
        Label heading = strong(criteria.location() == null || "All Nashik".equals(criteria.location())
                ? matches.size() + " approved stay options"
                : matches.size() + " " + (matches.size() == 1 ? "stay" : "stays") + " found near " + location);
        HBox summary = new HBox(8, badge(staySearchSummary(criteria)));
        summary.setAlignment(Pos.CENTER_LEFT);
        if (matches.isEmpty()) {
            Button explore = new Button("Explore Other Stays"); explore.getStyleClass().add("pilgrim-small-action"); explore.setOnAction(event -> exploreOther.run());
            VBox empty = new VBox(7, heading, muted("No stays match your current filters."), muted("Try another location, stay type or budget."), explore);
            empty.getStyleClass().add("stay-empty-state");
            return new VBox(10, summary, empty);
        }
        FlowPane cards = new FlowPane(14, 14);
        cards.getStyleClass().add("stay-results-grid");
        matches.forEach(stay -> cards.getChildren().add(createStayCard(stay, criteria)));
        return new VBox(10, heading, summary, cards);
    }

    private VBox createStayCard(StayItem stay, StayCriteria criteria) {
        ImageView image = createImage(stay.localImagePath(), 390, 190, 0.54, 0.50);
        Label fallback = label("Representative accommodation image", "stay-image-fallback");
        fallback.setVisible(image.getImage() == null);
        StackPane cover = new StackPane(image, fallback, badge(stay.locallyApprovedSeed() ? "SIMHASTHA STAY" : "SIMHASTHA VERIFIED"));
        StackPane.setAlignment(cover.getChildren().get(2), Pos.TOP_LEFT);
        StackPane.setMargin(cover.getChildren().get(2), new Insets(10));
        cover.getStyleClass().add("stay-cover");
        FlowPane facilities = new FlowPane(6, 6);
        stay.facilities().forEach(item -> facilities.getChildren().add(badge(item)));
        Label trip = muted(criteria.hasDates() ? criteria.nights() + (criteria.nights() == 1 ? " night" : " nights") + " • " + criteria.guests()
                : "Select dates and guests to plan this stay");
        Button save = new Button(stayWishlist.containsKey(stay.stableId()) ? "♥ Saved" : "♡ Wishlist");
        save.getStyleClass().add("pilgrim-small-action"); save.setOnAction(event -> toggleStayWishlist(stay, save));
        Button details = new Button("View Details"); details.getStyleClass().add("pilgrim-small-action"); details.setOnAction(event -> showStayDetails(stay, criteria));
        Button map = new Button("View on Map"); map.getStyleClass().add("pilgrim-small-action"); map.setOnAction(event -> openStayMap(stay));
        Button book = new Button("Check Stay / Book"); book.getStyleClass().add("primary-button"); book.setOnAction(event -> showStayDetails(stay, criteria));
        HBox secondary = new HBox(8, save, details, map);
        VBox card = new VBox(9, cover, label(stay.name(), "stay-property-name"), muted(stay.category() + " • " + stay.area()), muted(stay.address()), facilities,
                badge(stay.budgetCategory() + " Stay"), muted(stay.priceInfo().isBlank() ? "Price available during booking" : stay.priceInfo()), trip, secondary, book);
        card.getStyleClass().addAll("pilgrim-rich-card", "stay-property-card");
        card.setPrefWidth(390); card.setMaxWidth(420); card.setMinHeight(490);
        return card;
    }

    private VBox stayOtherSection(ComboBox<String> location, DatePicker checkIn, DatePicker checkOut, ComboBox<String> guests, ComboBox<String> budget) {
        ComboBox<String> type = stayCombo("Stay Type", "All Stays", "Hotels", "OYO", "Lodges", "Dharamshalas", "Guest Houses", "Homestays");
        ComboBox<String> otherLocation = stayCombo("Nearby Location", "All Nashik", "Panchavati", "Ramkund", "Tapovan", "Trimbakeshwar", "Nashik Road", "CBS", "Gangapur Road", "Ozar");
        ComboBox<String> otherBudget = stayCombo("Budget", "Any Budget", "Budget", "Standard", "Premium");
        DatePicker otherCheckIn = new DatePicker(); otherCheckIn.setPromptText("Check-in"); otherCheckIn.getStyleClass().add("input-combo");
        DatePicker otherCheckOut = new DatePicker(); otherCheckOut.setPromptText("Check-out"); otherCheckOut.getStyleClass().add("input-combo");
        otherLocation.valueProperty().bindBidirectional(location.valueProperty()); otherBudget.valueProperty().bindBidirectional(budget.valueProperty());
        otherCheckIn.valueProperty().bindBidirectional(checkIn.valueProperty()); otherCheckOut.valueProperty().bindBidirectional(checkOut.valueProperty());
        Button google = new Button("Search Other Stays"); google.getStyleClass().add("primary-button");
        google.setOnAction(event -> {
            String place = location.getValue() == null ? "Nashik" : location.getValue() + " Nashik";
            String kind = type.getValue() == null || "All Stays".equals(type.getValue()) ? "stays" : type.getValue();
            String query = kind + " near " + place + (budget.getValue() == null || "Any Budget".equals(budget.getValue()) ? "" : " " + budget.getValue());
            try { openUrl("https://www.google.com/search?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)); }
            catch (Exception ignored) { showInfo("Search stays", "Unable to open browser search."); }
        });
        Button maps = new Button("Open Google Maps"); maps.getStyleClass().add("pilgrim-small-action"); maps.setOnAction(event -> openStayAreaMap(location.getValue()));
        VBox panel = new VBox(10, strong("Explore Other Stays in Nashik"), muted("Search accommodation outside the SIMHASTHA CONNECT listings."),
                gridPaneForStay(otherLocation, type, otherBudget, otherCheckIn, otherCheckOut, guests), new HBox(10, google, maps), muted("External results will open in your browser."));
        panel.getStyleClass().add("pilgrim-panel"); return panel;
    }

    private GridPane gridPaneForStay(Node... nodes) { GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); for (int i = 0; i < nodes.length; i++) { grid.add(nodes[i], i % 2, i / 2); GridPane.setHgrow(nodes[i], Priority.ALWAYS); } return grid; }
    private ComboBox<String> stayCombo(String prompt, String... items) { ComboBox<String> combo = new ComboBox<>(); combo.setPromptText(prompt); combo.getItems().addAll(items); combo.getStyleClass().add("input-combo"); combo.setMaxWidth(Double.MAX_VALUE); return combo; }

    private DatePicker stayDatePicker(String prompt) {
        DatePicker picker = new DatePicker(); picker.setPromptText(prompt); picker.getStyleClass().add("input-combo");
        picker.setDayCellFactory(factory -> new DateCell() {
            @Override public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(java.time.LocalDate.now()));
            }
        });
        return picker;
    }

    private ComboBox<String> stayGuestsCombo() {
        ComboBox<String> guests = new ComboBox<>(); guests.setPromptText("Guests & Rooms");
        for (int adults = 1; adults <= 8; adults++) for (int children = 0; children <= 6; children++) for (int rooms = 1; rooms <= 5; rooms++) {
            String value = adults + (adults == 1 ? " Adult" : " Adults");
            if (children > 0) value += " • " + children + (children == 1 ? " Child" : " Children");
            guests.getItems().add(value + " • " + rooms + (rooms == 1 ? " Room" : " Rooms"));
        }
        guests.setValue("2 Adults • 1 Room"); guests.getStyleClass().add("input-combo"); return guests;
    }

    private boolean validStayDates(DatePicker checkIn, DatePicker checkOut) {
        if (checkIn.getValue() != null && checkOut.getValue() != null && !checkOut.getValue().isAfter(checkIn.getValue())) {
            showInfo("Check-out date", "Check-out date must be after check-in date."); return false;
        }
        return true;
    }

    private String stayNightText(java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        if (checkIn == null || checkOut == null) return "Select dates";
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        return nights <= 0 ? "Choose a later check-out" : nights + (nights == 1 ? "-night stay" : "-night stay");
    }

    private String stayTripText(java.time.LocalDate checkIn, java.time.LocalDate checkOut, String guests) {
        String party = guests == null ? "2 Adults • 1 Room" : guests;
        if (checkIn == null || checkOut == null) return party;
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) return "Choose a later check-out";
        return stayDate(checkIn) + " – " + stayDate(checkOut) + " • " + nights + (nights == 1 ? " night" : " nights") + " • " + party;
    }

    private StayCriteria stayCriteria(ComboBox<String> location, DatePicker checkIn, DatePicker checkOut, ComboBox<String> guests, ComboBox<String> budget) {
        return new StayCriteria(location.getValue(), checkIn.getValue(), checkOut.getValue(), guests.getValue(), budget.getValue() == null ? "Any Budget" : budget.getValue());
    }

    private boolean stayMatches(StayItem stay, StayCriteria criteria, String text, String type, String facility) {
        String haystack = (stay.name() + " " + stay.category() + " " + stay.area() + " " + stay.address()).toLowerCase();
        boolean area = criteria.location() == null || "All Nashik".equals(criteria.location()) || haystack.contains(criteria.location().toLowerCase()) || ("Ramkund".equals(criteria.location()) && haystack.contains("panchavati"));
        boolean search = text == null || text.isBlank() || haystack.contains(text.toLowerCase());
        boolean kind = type == null || "All Types".equals(type) || stay.category().equalsIgnoreCase(type);
        boolean budget = "Any Budget".equals(criteria.budget()) || stay.budgetCategory().equalsIgnoreCase(criteria.budget());
        boolean hasFacility = facility == null || "All Facilities".equals(facility) || stay.facilities().stream().anyMatch(item -> item.equalsIgnoreCase(facility));
        return area && search && kind && budget && hasFacility;
    }

    private java.util.List<StayItem> approvedStayItems() {
        java.util.LinkedHashMap<String, StayItem> stays = new java.util.LinkedHashMap<>();
        LOCAL_STAY_SEEDS.forEach(stay -> stays.put(stay.name().trim().toLowerCase(), stay));
        try {
            for (AppDataStore.BusinessRecord business : AppDataStore.businesses()) {
                if (!isApprovedStayBusiness(business)) continue;
                StayItem mapped = new StayItem(business.businessName, business.category, blankTo(business.location, "Nashik"),
                        blankTo(business.location, "Location details available during connected booking."), "", "/images/welcome-light.png",
                        business.businessName + " " + blankTo(business.location, "Nashik"), stayBudgetFrom(business.priceRange), List.of(),
                        blankTo(business.description, "Approved accommodation listing."), false, business.priceRange, business.businessId);
                stays.put(mapped.name().trim().toLowerCase(), mapped);
            }
        } catch (RuntimeException ignored) {
            // Curated local stays remain available if the existing business store cannot be read.
        }
        return java.util.List.copyOf(stays.values());
    }

    private boolean isApprovedStayBusiness(AppDataStore.BusinessRecord business) {
        if (business == null || !business.approved || !("approved".equalsIgnoreCase(business.status) || "active".equalsIgnoreCase(business.status))) return false;
        String category = business.category == null ? "" : business.category.toLowerCase();
        return java.util.List.of("hotel", "stay", "lodge", "dharamshala", "guest house", "homestay", "tent", "camp", "accommodation", "ashram").stream().anyMatch(category::contains);
    }

    private String stayBudgetFrom(String priceRange) {
        String value = priceRange == null ? "" : priceRange.toLowerCase();
        return value.contains("premium") ? "Premium" : value.contains("standard") ? "Standard" : "Budget";
    }

    private String blankTo(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    private java.util.Comparator<StayItem> stayComparator(String location, String sort) {
        if ("Property Name A-Z".equals(sort)) return java.util.Comparator.comparing(StayItem::name, String.CASE_INSENSITIVE_ORDER);
        if ("Budget First".equals(sort)) return java.util.Comparator.comparingInt(stay -> budgetRank(stay.budgetCategory()));
        if ("Near Selected Area".equals(sort) && location != null) return java.util.Comparator.comparingInt(stay -> stay.area().toLowerCase().contains(location.toLowerCase()) ? 0 : 1);
        return java.util.Comparator.comparing((StayItem stay) -> !stay.locallyApprovedSeed()).thenComparing(StayItem::name, String.CASE_INSENSITIVE_ORDER);
    }

    private int budgetRank(String value) { return "Budget".equals(value) ? 0 : "Standard".equals(value) ? 1 : 2; }
    private String staySearchSummary(StayCriteria criteria) { return (criteria.location() == null ? "Nashik" : criteria.location()) + " • " + (criteria.hasDates() ? criteria.checkIn().getDayOfMonth() + " " + criteria.checkIn().getMonth().toString().substring(0, 3) + "–" + criteria.checkOut().getDayOfMonth() + " " + criteria.checkOut().getMonth().toString().substring(0, 3) : "Choose dates") + " • " + criteria.guests(); }
    private void openStayAreaMap(String location) { openUrl("https://www.google.com/maps/search/?api=1&query=" + java.net.URLEncoder.encode("stays near " + (location == null ? "Nashik" : location) + " Nashik", java.nio.charset.StandardCharsets.UTF_8)); }
    private void openStayMap(StayItem stay) { openUrl("https://www.google.com/maps/search/?api=1&query=" + java.net.URLEncoder.encode(stay.mapQuery(), java.nio.charset.StandardCharsets.UTF_8)); }

    private void showStayDetails(StayItem stay, StayCriteria criteria) {
        rememberStayViewed(stay);
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Stay Details"); dialog.setHeaderText(null);
        ButtonType continueBooking = new ButtonType("Continue to Booking");
        ImageView image = createImage(stay.localImagePath(), 650, 265, 0.54, 0.50);
        FlowPane facilities = new FlowPane(6, 6); stay.facilities().forEach(item -> facilities.getChildren().add(badge(item)));
        Button map = new Button("View on Google Maps"); map.getStyleClass().add("pilgrim-small-action"); map.setOnAction(event -> openStayMap(stay));
        Button save = new Button(stayWishlist.containsKey(stay.stableId()) ? "♥ Saved" : "♡ Save to Wishlist");
        save.getStyleClass().add("pilgrim-small-action"); save.setOnAction(event -> toggleStayWishlist(stay, save));
        Button select = new Button("Select"); select.getStyleClass().add("pilgrim-small-action");
        select.setOnAction(event -> { dialog.setResult(continueBooking); dialog.close(); });
        VBox option = new VBox(5, strong("Stay Option"), muted("Standard Accommodation"), muted("Suitable for selected guests"),
                muted(stay.priceInfo().isBlank() ? "Live price and availability are verified before payment." : stay.priceInfo()), select);
        option.getStyleClass().add("stay-option-card");
        VBox content = new VBox(12, image, label(stay.name(), "stay-property-name"), muted(stay.category() + " • " + stay.area()), muted(stay.address()),
                paragraph(stay.shortDescription()), sectionTitle("Stay Overview"), muted(staySearchSummary(criteria)), muted("Budget: " + stay.budgetCategory()), muted(stay.priceInfo().isBlank() ? "Price available during booking" : stay.priceInfo()), sectionTitle("Facilities"), facilities,
                sectionTitle("Location"), new HBox(8, save, map), option);
        content.setPadding(new Insets(4));
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(560); scroll.getStyleClass().add("stay-dialog-scroll");
        dialog.getDialogPane().setContent(scroll); dialog.getDialogPane().setPrefSize(900, 650); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE, continueBooking);
        dialog.showAndWait().filter(choice -> choice == continueBooking).ifPresent(choice -> showStayBookingDetails(stay, criteria));
    }

    private void showStayBookingDetails(StayItem stay, StayCriteria criteria) {
        if (!criteria.hasDates()) { showInfo("Select dates", "Choose a valid check-in and check-out date before continuing to booking."); return; }
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Complete Your Stay Booking"); dialog.setHeaderText(null);
        TextField name = AppUi.textField("Full Name");
        TextField mobile = AppUi.textField("Mobile Number");
        TextField email = AppUi.textField("Email Address");
        TextField request = AppUi.textField("Special Request (optional)");
        ImageView image = createImage(stay.localImagePath(), 170, 105, 0.54, 0.50);
        VBox property = new VBox(4, strong(stay.name()), muted(stay.category() + " • " + stay.area()), muted(stay.address()));
        HBox propertySummary = new HBox(14, image, property); propertySummary.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(14, propertySummary, sectionTitle("Trip Details"), muted(staySearchSummary(criteria)),
                sectionTitle("Guest Details"), name, mobile, email, request,
                muted("Personal details are used only for this booking request and are not logged by the Stay page."));
        content.getStyleClass().add("stay-booking-summary"); dialog.getDialogPane().setContent(content); dialog.getDialogPane().setPrefSize(760, 620);
        ButtonType back = new ButtonType("Back", javafx.scene.control.ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType review = new ButtonType("Review Booking"); dialog.getDialogPane().getButtonTypes().addAll(back, review);
        dialog.showAndWait().filter(choice -> choice == review).ifPresent(choice -> {
            StayGuestDetails guest = new StayGuestDetails(name.getText(), mobile.getText(), email.getText(), request.getText());
            if (!validStayGuest(guest)) { showStayBookingDetails(stay, criteria); return; }
            showStayBookingSummary(stay, criteria, guest);
        });
    }

    private boolean validStayGuest(StayGuestDetails guest) {
        if (guest.name().isBlank()) { showInfo("Guest details", "Enter the guest's full name."); return false; }
        if (!guest.mobile().replaceAll("[\\s-]", "").matches("\\d{10,15}")) { showInfo("Guest details", "Enter a valid mobile number."); return false; }
        if (!guest.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) { showInfo("Guest details", "Enter a valid email address."); return false; }
        return true;
    }

    private void showStayBookingSummary(StayItem stay, StayCriteria criteria, StayGuestDetails guest) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Review Your Booking"); dialog.setHeaderText(null);
        ImageView image = createImage(stay.localImagePath(), 180, 108, 0.54, 0.50);
        VBox property = new VBox(4, strong(stay.name()), muted(stay.area() + ", Nashik"), muted(stay.category()));
        HBox propertySummary = new HBox(14, image, property); propertySummary.setAlignment(Pos.CENTER_LEFT);
        VBox summary = new VBox(11, sectionTitle("Review Your Booking"), propertySummary, sectionTitle("Your Trip"),
                muted("Check-in: " + stayDate(criteria.checkIn()) + "     Check-out: " + stayDate(criteria.checkOut())),
                muted(criteria.nights() + (criteria.nights() == 1 ? " night" : " nights") + " • " + criteria.guests()),
                sectionTitle("Accommodation"), muted("Standard Accommodation"),
                muted(stay.priceInfo().isBlank() ? "Price is not configured for this stay yet." : stay.priceInfo()),
                sectionTitle("Guest"), muted(guest.name() + " • " + guest.mobile()),
                muted("Online payment is available only after the booking server resolves an approved stay option, live price and availability."));
        summary.getStyleClass().add("stay-booking-summary"); dialog.getDialogPane().setContent(summary); dialog.getDialogPane().setPrefSize(760, 650);
        ButtonType back = new ButtonType("Back", javafx.scene.control.ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType proceed = new ButtonType("Proceed to Payment"); dialog.getDialogPane().getButtonTypes().addAll(back, proceed);
        Button paymentButton = (Button) dialog.getDialogPane().lookupButton(proceed);
        boolean trustedPayment = com.simhastha.payment.PaymentCatalog.find(stay.stableId())
                .map(item -> item.moduleType() == com.simhastha.payment.PaymentModuleType.STAY).orElse(false);
        paymentButton.setDisable(!trustedPayment);
        if (!trustedPayment) paymentButton.setTooltip(new javafx.scene.control.Tooltip("Live price and availability must be verified by the booking server."));
        dialog.showAndWait().filter(choice -> choice == proceed).ifPresent(choice -> {
            paymentButton.setDisable(true);
            paymentCoordinator.startPaidBooking(root.getScene() == null ? null : root.getScene().getWindow(), stay.stableId(), stayRooms(criteria), (int) criteria.nights());
        });
    }

    private int stayRooms(StayCriteria criteria) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\s+Rooms?").matcher(criteria.guests() == null ? "" : criteria.guests());
        return matcher.find() ? Math.max(1, Integer.parseInt(matcher.group(1))) : 1;
    }

    private String stayDate(java.time.LocalDate date) { return date == null ? "Not selected" : date.getDayOfMonth() + " " + date.getMonth().toString().substring(0, 3); }

    private record StayCriteria(String location, java.time.LocalDate checkIn, java.time.LocalDate checkOut, String guests, String budget) {
        private boolean hasDates() { return checkIn != null && checkOut != null && checkOut.isAfter(checkIn); }
        private long nights() { return hasDates() ? java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut) : 0; }
        private StayCriteria withBudget(String secondaryBudget) { return new StayCriteria(location, checkIn, checkOut, guests, secondaryBudget == null ? "Any Budget" : secondaryBudget); }
    }

    private record StayGuestDetails(String name, String mobile, String email, String specialRequest) { }

    private record StayItem(String name, String category, String area, String address, String imageUrl, String localImagePath, String mapQuery,
            String budgetCategory, java.util.List<String> facilities, String shortDescription, boolean locallyApprovedSeed, String priceInfo,
            String stableId) {
        private StayItem(String name, String category, String area, String address, String imageUrl, String localImagePath, String mapQuery,
                String budgetCategory, java.util.List<String> facilities, String shortDescription, boolean locallyApprovedSeed) {
            this(name, category, area, address, imageUrl, localImagePath, mapQuery, budgetCategory, facilities, shortDescription, locallyApprovedSeed, "", "");
        }

        private StayItem(String name, String category, String area, String address, String imageUrl, String localImagePath, String mapQuery,
                String budgetCategory, java.util.List<String> facilities, String shortDescription, boolean locallyApprovedSeed, String priceInfo) {
            this(name, category, area, address, imageUrl, localImagePath, mapQuery, budgetCategory, facilities, shortDescription, locallyApprovedSeed, priceInfo, "");
        }

        public String stableId() {
            return stableId == null || stableId.isBlank() ? "seed:" + name.trim().toLowerCase(java.util.Locale.ROOT) : stableId;
        }
    }

    private static final java.util.List<StayItem> LOCAL_STAY_SEEDS = java.util.List.of(
            new StayItem("Hotel Panchavati Yatri", "Hotel", "Panchavati / Raviwar Karanja", "Panchavati, Nashik", "", "/images/ramkund_sunrise.jpg", "Hotel Panchavati Yatri Panchavati Nashik", "Standard", java.util.List.of("Parking", "Family Friendly"), "A local stay option near Panchavati.", true),
            new StayItem("Shree Balaji Guest House", "Guest House", "Panchavati", "Panchavati, Nashik", "", "/images/godavari_kumbh.jpg", "Shree Balaji Guest House Panchavati Nashik", "Budget", java.util.List.of("Family Friendly", "Hot Water"), "A local guest house option in Panchavati.", true),
            new StayItem("FabHotel Vaishnav Bairagi Yatriniwas", "Lodge", "Ramkund / Panchavati", "Ramkund, Panchavati, Nashik", "", "/images/ramkund_sunrise.jpg", "FabHotel Vaishnav Bairagi Yatriniwas Ramkund Panchavati Nashik", "Standard", java.util.List.of("Wi-Fi", "Family Friendly"), "A lodge option near Ramkund.", true),
            new StayItem("Hotel Sanket Lodge", "Lodge", "Panchavati", "Panchavati, Nashik", "", "/images/godavari_kumbh.jpg", "Hotel Sanket Lodge Panchavati Nashik", "Budget", java.util.List.of("Hot Water"), "A local lodge option in Panchavati.", true),
            new StayItem("Shri Sant Gadge Maharaj Dharamshala Trust Nashik", "Dharamshala", "Panchavati", "Panchavati, Nashik", "", "/images/ramkund_sunrise.jpg", "Shri Sant Gadge Maharaj Dharamshala Trust Panchavati Nashik", "Budget", java.util.List.of("Meals", "Accessible"), "A dharamshala option in the Panchavati area.", true),
            new StayItem("Ladka Bhuvan Dharamshala", "Dharamshala", "Dindori Naka / Panchavati", "Dindori Naka, Nashik", "", "/images/godavari_kumbh.jpg", "Ladka Bhuvan Dharamshala Dindori Naka Nashik", "Budget", java.util.List.of("Family Friendly"), "A dharamshala option near Dindori Naka.", true),
            new StayItem("Hotel Three Leaves", "Hotel", "Trimbakeshwar", "Trimbakeshwar, Nashik", "", "/images/trimbakeshwar.jpg", "Hotel Three Leaves Trimbakeshwar Nashik", "Premium", java.util.List.of("Parking", "Wi-Fi", "Hot Water"), "A hotel option in Trimbakeshwar.", true),
            new StayItem("Tulsi Inn", "Homestay", "Trimbakeshwar", "Trimbakeshwar, Nashik", "", "/images/trimbakeshwar.jpg", "Tulsi Inn Trimbakeshwar Nashik", "Standard", java.util.List.of("Family Friendly", "Meals", "Hot Water"), "A homestay option near Trimbakeshwar Temple.", true));

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
