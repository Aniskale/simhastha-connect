package com.simhastha.view;

import java.awt.Desktop;
import java.net.URL;
import java.net.URI;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class DashboardPage {

    private BorderPane root;
    private final AppPaymentCoordinator paymentCoordinator = new AppPaymentCoordinator();
    private final java.util.Map<String, Button> navButtons = new java.util.LinkedHashMap<>();
    private String activeModule = "home";
    private boolean pujaRefreshInProgress;
    private Timeline pujaLiveRefresh;
    private LocationOption selectedFromLocation;
    private LocationOption selectedToLocation;
    private static final java.util.Map<String, Image> IMAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Place> TRANSPORT_PLACES = createTransportPlaces();
    private static final java.util.List<LocationOption> INDIA_LOCATION_OPTIONS = loadIndiaLocationOptions();
    private static final int LOCATION_SEARCH_LIMIT = 30;
    private static final java.util.concurrent.ExecutorService LOCATION_SEARCH_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "simhastha-location-search");
        thread.setDaemon(true);
        return thread;
    });

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
        if ("puja".equals(module) || "bookings".equals(module)) {
            refreshPujaDataAsync(module);
        }
    }

    private void setActiveModule(String module) {
        activeModule = module;
        navButtons.keySet().forEach(key -> setNavSelected(key, key.equals(module)));
        if ("puja".equals(module) || "bookings".equals(module)) {
            startPujaLiveRefresh();
        } else {
            stopPujaLiveRefresh();
        }
    }

    private void refreshPujaDataAsync(String module) {
        if (pujaRefreshInProgress || !AppDataStore.isFirebaseEnabled()) {
            return;
        }
        pujaRefreshInProgress = true;
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        java.util.concurrent.CompletableFuture.runAsync(() -> AppDataStore.refreshPujaFirebaseData(token))
                .whenComplete((ignored, error) -> javafx.application.Platform.runLater(() -> {
                    pujaRefreshInProgress = false;
                    if (module.equals(activeModule)) {
                        Node focusOwner = root == null || root.getScene() == null ? null : root.getScene().getFocusOwner();
                        if (focusOwner instanceof javafx.scene.control.TextInputControl) {
                            return;
                        }
                        Node refreshed = "bookings".equals(module) ? myBookingsPage() : pujaPage();
                        root.setCenter(scroll(refreshed));
                    }
                }));
    }

    private void startPujaLiveRefresh() {
        stopPujaLiveRefresh();
    }

    private void stopPujaLiveRefresh() {
        if (pujaLiveRefresh != null) {
            pujaLiveRefresh.stop();
        }
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

    private record PujaService(String serviceId, String name, String type, String description, String duration,
            String availableSlots, int price, String location, String mode, String languages, boolean availableToday, String image,
            String providerId, String providerName, String bookingStatus) {
        private PujaService(String name, String type, String description, String duration, int price, String location,
                String mode, String languages, boolean availableToday, String image) {
            this("mock-" + name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-"),
                    name, type, description, duration, "Varies", price, location, mode, languages, availableToday, image,
                    "", "Simhastha Verified Desk", "OPEN");
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
        ComboBox<String> from = transportLocationCombo("From");
        from.setValue("Nashik Road Railway Station");
        ComboBox<String> to = transportLocationCombo("To");
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

    private ComboBox<String> transportLocationCombo(String prompt) {
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
        java.util.List<PujaService> services = visiblePujaServices();
        TextField search = AppUi.textField("Search Puja, Temple, Priest or Location...");
        search.getStyleClass().add("puja-search-field");

        ComboBox<String> type = pujaFilter("Puja Type", pujaFilterOptions("All Types",
                services.stream().map(PujaService::type).toList()));
        ComboBox<String> location = pujaFilter("Location", pujaFilterOptions("All Locations",
                services.stream().map(PujaService::location).toList()));
        ComboBox<String> language = pujaFilter("Language", pujaLanguageFilterOptions(services));
        ComboBox<String> mode = pujaFilter("Mode", pujaFilterOptions("Any Mode",
                services.stream().map(PujaService::mode).toList()));
        ComboBox<String> price = pujaFilter("Price", pujaPriceFilterOptions(services));
        CheckBox today = new CheckBox("Available Today");
        today.getStyleClass().add("puja-today-filter");

        javafx.scene.layout.FlowPane serviceCards = new javafx.scene.layout.FlowPane(14, 14);
        serviceCards.getStyleClass().add("puja-service-grid");

        Runnable render = () -> renderPujaCards(serviceCards, services, search.getText(), type.getValue(),
                location.getValue(), language.getValue(), mode.getValue(), price.getValue(), today.isSelected());
        Runnable resetFilters = () -> {
            search.clear();
            type.setValue("All Types");
            location.setValue("All Locations");
            language.setValue("All Languages");
            mode.setValue("Any Mode");
            price.setValue("Any Price");
            today.setSelected(false);
            render.run();
        };
        search.textProperty().addListener((observable, oldValue, newValue) -> render.run());
        search.setOnAction(event -> render.run());
        type.setOnAction(event -> render.run());
        location.setOnAction(event -> render.run());
        language.setOnAction(event -> render.run());
        mode.setOnAction(event -> render.run());
        price.setOnAction(event -> render.run());
        today.setOnAction(event -> render.run());
        render.run();

        return pageShell("Puja Services", "Safe & Trusted Spiritual Booking",
                pujaTrustBanner(),
                pujaFilterPanel(search, type, location, language, mode, price, today, render, resetFilters),
                pujaProviderRegistrationBanner(),
                pujaSection("Available / Popular Puja Services", serviceCards),
                pujaUserSupportSections(services));
    }

    private HBox pujaUserSupportSections(java.util.List<PujaService> services) {
        VBox priests = pujaMiniInfoSection("Verified Priests", "\uE77B",
                AppDataStore.approvedPujaProviders().isEmpty()
                        ? "Approved priest/provider profiles will appear here after admin verification."
                        : AppDataStore.approvedPujaProviders().stream()
                                .limit(3)
                                .map(provider -> valueOr(provider.providerId, provider.fullName))
                                .reduce((left, right) -> left + "\n" + right)
                                .orElse(""));
        VBox darshan = pujaMiniInfoSection("Official Darshan Services", "\uE8D7",
                services.stream().anyMatch(service -> service.type().toLowerCase(java.util.Locale.ROOT).contains("darshan"))
                        ? "Admin-approved darshan services are listed in the Puja cards above."
                        : "Only admin-created or admin-approved special darshan services will be shown.");
        VBox locations = pujaMiniInfoSection("Authorized Puja Locations", "\uE707",
                services.stream().map(PujaService::location).distinct().limit(3)
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("Ramkund\nTrimbakeshwar\nPanchavati"));
        VBox safety = pujaMiniInfoSection("Puja Safety Center", "\uE72E",
                "Book only verified services. Do not pay unknown agents. Report suspicious activity from the official support flow.");
        HBox row = new HBox(12, priests, darshan, locations, safety);
        row.getStyleClass().add("puja-user-support-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        return row;
    }

    private VBox pujaMiniInfoSection(String title, String icon, String text) {
        VBox card = new VBox(8,
                AppUi.symbolIcon(icon, "puja-trust-icon"),
                strong(title),
                muted(text));
        card.getStyleClass().add("puja-mini-info-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private HBox pujaProviderRegistrationBanner() {
        VBox copy = new VBox(5,
                strong("Are you a priest or verified spiritual service provider?"),
                muted("Submit your documents for admin verification. Only approved providers appear to pilgrims."));
        Button register = new Button("Register Provider");
        register.getStyleClass().add("transport-primary-button");
        register.setOnAction(event -> showPujaProviderRegistrationPage());
        HBox banner = new HBox(14, AppUi.symbolIcon("\uE73E", "puja-trust-icon"), copy, createSpacer(), register);
        banner.getStyleClass().add("puja-provider-register-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        return banner;
    }

    private void showPujaProviderRegistrationPage() {
        TextField fullName = AppUi.textField("Name");
        TextField profilePhoto = AppUi.textField("Profile photo path / URL");
        TextField phone = AppUi.textField("Phone");
        TextField email = AppUi.textField("Email");
        TextField address = AppUi.textField("Address");
        TextField experience = AppUi.textField("Experience");
        TextField specialization = AppUi.textField("Specialization");
        TextField languages = AppUi.textField("Languages");
        TextField temple = AppUi.textField("Temple / Organization");
        TextField identity = AppUi.textField("Identity document path / reference");
        TextField certificates = AppUi.textField("Supporting certificates path / reference");
        TextField services = AppUi.textField("Services offered");
        TextField locations = AppUi.textField("Service locations");

        Button back = pujaBackButton("← Back");
        back.setOnAction(event -> showModulePage("puja"));

        Button submit = new Button("Submit for Admin Verification");
        submit.getStyleClass().add("transport-primary-button");
        submit.setOnAction(event -> submitPujaProviderRegistration(fullName, profilePhoto, phone, email, address,
                experience, specialization, languages, temple, identity, certificates, services, locations));

        VBox basic = pujaRegistrationGroup("Basic Details", fullName, phone, email, address);
        VBox expertise = pujaRegistrationGroup("Experience & Specialization", experience, specialization, languages, temple);
        VBox documents = pujaRegistrationGroup("Documents & Services", profilePhoto, identity, certificates, services, locations);
        VBox note = infoPanel("Verification Flow",
                "Provider Registration → Document Submission → Admin Review → Admin Approval → Provider Visible to Users\n\n"
                        + "Important: Providers cannot create unauthorized VIP Darshan. Official Special Darshan is shown only when admin-created or admin-approved.");
        HBox top = new HBox(10, back, createSpacer(), badge("Status: PENDING after submit"));
        top.setAlignment(Pos.CENTER_LEFT);
        VBox content = pageShell("Provider Registration", "Submit documents for Simhastha verified priest/provider approval.",
                top,
                pujaTrustBanner(),
                infoPanel("Provider Details", basic, expertise, documents, new HBox(10, createSpacer(), submit)),
                note);
        root.setCenter(scroll(content));
    }

    private VBox pujaRegistrationGroup(String title, TextField... fields) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int i = 0; i < fields.length; i++) {
            fields[i].setMaxWidth(Double.MAX_VALUE);
            grid.add(fields[i], i % 2, i / 2);
            GridPane.setHgrow(fields[i], Priority.ALWAYS);
        }
        VBox group = new VBox(10, strong(title), grid);
        group.getStyleClass().add("admin-puja-editor-group");
        return group;
    }

    private void submitPujaProviderRegistration(TextField fullName, TextField profilePhoto, TextField phone,
            TextField email, TextField address, TextField experience, TextField specialization, TextField languages,
            TextField temple, TextField identity, TextField certificates, TextField services, TextField locations) {
        if (fullName.getText().isBlank() || phone.getText().isBlank() || email.getText().isBlank()
                || identity.getText().isBlank() || services.getText().isBlank() || locations.getText().isBlank()) {
            showInfo("Missing Details", "Please enter full name, phone, email, identity document, services offered and service locations.");
            return;
        }
        String offered = services.getText().trim();
        if (offered.toLowerCase(java.util.Locale.ROOT).contains("vip darshan")) {
            showInfo("Unauthorized Service Blocked",
                    "Providers cannot create or publish unauthorized VIP Darshan. Official Special Darshan must be admin-created or admin-approved.");
            return;
        }
        try {
            AppDataStore.registerPujaProvider(new AppDataStore.PujaProviderRecord(
                    "", fullName.getText(), profilePhoto.getText(), phone.getText(), email.getText(), address.getText(),
                    experience.getText(), specialization.getText(), languages.getText(), temple.getText(),
                    identity.getText(), certificates.getText(), offered, locations.getText(),
                    "pending", false, "", ""));
            showInfo("Registration Submitted",
                    "Your provider registration is pending admin review. It will appear to pilgrims only after approval.");
            showModulePage("puja");
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Registration Failed", exception.getMessage());
        }
    }

    private HBox pujaTrustBanner() {
        HBox banner = new HBox(12,
                pujaTrustItem("\uE73E", "Verified Providers", "Priests and service counters are checked before listing."),
                pujaTrustItem("\uE8C7", "Transparent Pricing", "Starting prices are shown clearly before booking."),
                pujaTrustItem("\uE72E", "Secure Booking", "Booking details stay inside the official flow."),
                pujaTrustItem("\uE8D7", "No Agent Payments", "Do not pay unofficial middlemen or unknown agents."));
        banner.getStyleClass().add("puja-trust-banner");
        return banner;
    }

    private VBox pujaTrustItem(String icon, String title, String detail) {
        VBox item = new VBox(7, AppUi.symbolIcon(icon, "puja-trust-icon"), strong(title), muted(detail));
        item.getStyleClass().add("puja-trust-item");
        HBox.setHgrow(item, Priority.ALWAYS);
        return item;
    }

    private java.util.List<String> pujaFilterOptions(String defaultValue, java.util.List<String> rawValues) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        values.add(defaultValue);
        rawValues.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(values::add);
        return new java.util.ArrayList<>(values);
    }

    private java.util.List<String> pujaLanguageFilterOptions(java.util.List<PujaService> services) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        values.add("All Languages");
        services.stream()
                .map(PujaService::languages)
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(values::add);
        return new java.util.ArrayList<>(values);
    }

    private java.util.List<String> pujaPriceFilterOptions(java.util.List<PujaService> services) {
        java.util.List<String> values = new java.util.ArrayList<>();
        values.add("Any Price");
        boolean hasUnder500 = services.stream().anyMatch(service -> service.price() > 0 && service.price() < 500);
        boolean hasMid = services.stream().anyMatch(service -> service.price() >= 500 && service.price() <= 1500);
        boolean hasHigh = services.stream().anyMatch(service -> service.price() > 1500);
        if (hasUnder500) {
            values.add("Under Rs. 500");
        }
        if (hasMid) {
            values.add("Rs. 500 - Rs. 1500");
        }
        if (hasHigh) {
            values.add("Above Rs. 1500");
        }
        return values;
    }

    private ComboBox<String> pujaFilter(String prompt, java.util.List<String> values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(values);
        combo.setValue(values.isEmpty() ? prompt : values.get(0));
        combo.setPromptText(prompt);
        combo.getStyleClass().add("puja-filter-combo");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private VBox pujaFilterPanel(TextField search, ComboBox<String> type, ComboBox<String> location,
            ComboBox<String> language, ComboBox<String> mode, ComboBox<String> price, CheckBox today,
            Runnable render, Runnable resetFilters) {
        Button clearSearch = new Button("×");
        clearSearch.getStyleClass().add("puja-search-clear-button");
        clearSearch.setOnAction(event -> {
            search.clear();
            render.run();
        });

        Label searchIcon = AppUi.symbolIcon("\uE721", "puja-search-box-icon");
        StackPane searchIconBlock = new StackPane(searchIcon);
        searchIconBlock.getStyleClass().add("puja-search-icon-block");

        HBox searchBox = new HBox(0,
                searchIconBlock,
                search,
                label("ॐ  मंदिर  ⌁", "puja-search-decoration"),
                clearSearch);
        searchBox.getStyleClass().add("puja-search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        Button searchButton = new Button("Search");
        searchButton.setGraphic(AppUi.symbolIcon("\uE721", "puja-search-button-icon"));
        searchButton.getStyleClass().add("puja-main-search-button");
        searchButton.setOnAction(event -> render.run());

        HBox todayBox = new HBox(8, AppUi.symbolIcon("\uE787", "puja-filter-icon"), today);
        todayBox.getStyleClass().add("puja-today-toggle-box");
        todayBox.setAlignment(Pos.CENTER);

        HBox topRow = new HBox(12, searchBox, todayBox, searchButton);
        topRow.getStyleClass().add("puja-filter-top-row");
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        decoratePujaFilter(type, "\uEC29", "Puja Type", "All Types");
        decoratePujaFilter(location, "\uE707", "Location", "All Locations");
        decoratePujaFilter(language, "\uE8D4", "Language", "All Languages");
        decoratePujaFilter(mode, "\uE8A5", "Mode", "Any Mode");
        decoratePujaFilter(price, "\uE8C7", "Price", "Any Price");

        Button reset = new Button("Reset All");
        reset.setGraphic(AppUi.symbolIcon("\uE72C", "puja-filter-icon"));
        reset.getStyleClass().add("puja-reset-filter-button");
        reset.setOnAction(event -> resetFilters.run());

        javafx.scene.layout.FlowPane filters = new javafx.scene.layout.FlowPane(10, 10,
                type, location, language, mode, price, reset);
        filters.getStyleClass().add("puja-filter-second-row");
        filters.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(13, topRow, filters);
        panel.getStyleClass().add("puja-filter-panel");
        return panel;
    }

    private void decoratePujaFilter(ComboBox<String> combo, String icon, String label, String defaultValue) {
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(label);
                    setGraphic(AppUi.symbolIcon(icon, "puja-filter-icon"));
                } else {
                    setText(item.equals(defaultValue) ? label : item);
                    setGraphic(AppUi.symbolIcon(icon, "puja-filter-icon"));
                }
            }
        });
    }

    private VBox pujaSection(String title, Node body) {
        VBox section = new VBox(12, sectionTitle(title), body);
        section.getStyleClass().add("puja-section");
        return section;
    }

    private void renderPujaCards(javafx.scene.layout.FlowPane container, java.util.List<PujaService> services, String query,
            String type, String location, String language, String mode, String price, boolean availableToday) {
        java.util.List<Node> cards = services.stream()
                .filter(service -> pujaMatches(service, query, type, location, language, mode, price, availableToday))
                .map(service -> (Node) pujaServiceCard(service))
                .toList();
        if (cards.isEmpty()) {
            Button clear = new Button("Clear Filters");
            clear.getStyleClass().add("transport-primary-button");
            VBox empty = new VBox(10,
                    sectionTitle("No Puja Services Found"),
                    muted("Try changing your search or filters."),
                    clear);
            empty.getStyleClass().add("puja-empty-state");
            clear.setOnAction(event -> {
                Node parent = container.getParent();
                while (parent != null && !(parent instanceof VBox)) {
                    parent = parent.getParent();
                }
                showModulePage("puja");
            });
            container.getChildren().setAll(empty);
        } else {
            container.getChildren().setAll(cards);
        }
    }

    private boolean pujaMatches(PujaService service, String query, String type, String location, String language,
            String mode, String price, boolean availableToday) {
        String search = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        String searchable = String.join(" ",
                valueOr("", service.name()),
                valueOr("", service.description()),
                valueOr("", service.type()),
                valueOr("", service.location()),
                valueOr("", service.providerName()),
                valueOr("", service.languages()),
                valueOr("", service.mode())).toLowerCase(java.util.Locale.ROOT);
        boolean queryOk = search.isBlank() || searchable.contains(search);
        String serviceType = valueOr("", service.type());
        String serviceLocation = valueOr("", service.location());
        String serviceLanguages = valueOr("", service.languages());
        String serviceMode = valueOr("", service.mode());
        boolean typeOk = type == null || "All Types".equals(type) || serviceType.equalsIgnoreCase(type);
        boolean locationOk = location == null || "All Locations".equals(location)
                || serviceLocation.toLowerCase(java.util.Locale.ROOT).contains(location.toLowerCase(java.util.Locale.ROOT));
        boolean languageOk = language == null || "All Languages".equals(language)
                || serviceLanguages.toLowerCase(java.util.Locale.ROOT).contains(language.toLowerCase(java.util.Locale.ROOT));
        boolean modeOk = mode == null || "Any Mode".equals(mode)
                || serviceMode.equalsIgnoreCase(mode)
                || serviceMode.toLowerCase(java.util.Locale.ROOT).contains(mode.toLowerCase(java.util.Locale.ROOT))
                || "Both".equalsIgnoreCase(serviceMode);
        boolean todayOk = !availableToday || service.availableToday();
        boolean priceOk = switch (price == null ? "Any Price" : price) {
            case "Under Rs. 500" -> service.price() < 500;
            case "Rs. 500 - Rs. 1500" -> service.price() >= 500 && service.price() <= 1500;
            case "Above Rs. 1500" -> service.price() > 1500;
            default -> true;
        };
        return queryOk && typeOk && locationOk && languageOk && modeOk && todayOk && priceOk;
    }

    private VBox pujaServiceCard(PujaService service) {
        ImageView image = createImage(service.image(), 260, 118, 0.5, 0.5);
        image.getStyleClass().add("puja-card-image");
        HBox meta = new HBox(7, badge(service.mode()), badge(service.availableToday() ? "Today" : "Scheduled"), badge("Verified"));
        meta.setAlignment(Pos.CENTER_LEFT);

        VBox details = new VBox(5,
                muted("Temple / Ghat: " + service.location()),
                muted("Puja Type: " + service.type()),
                muted("Location: " + service.location()),
                muted("Duration: " + service.duration()),
                muted("Provider: " + valueOr("Simhastha Verified Desk", service.providerName())),
                muted("Languages: " + service.languages()),
                muted("Mode: " + service.mode()),
                label("Starting Rs. " + service.price(), "puja-price"));

        Button view = new Button("View Details");
        view.getStyleClass().add("puja-secondary-button");
        view.setOnAction(event -> openPujaServiceDetailsPage(service.serviceId()));

        Button book = new Button("Book Now");
        book.getStyleClass().add("transport-primary-button");
        book.setDisable("CLOSED".equalsIgnoreCase(service.bookingStatus()) || "DISABLED".equalsIgnoreCase(service.bookingStatus()));
        book.setOnAction(event -> openPujaServiceDetailsPage(service.serviceId()));

        HBox actions = new HBox(8, view, book);
        VBox card = new VBox(10, image, meta, strong(service.name()), paragraph(service.description()), details, actions);
        card.getStyleClass().add("puja-service-card");
        return card;
    }

    private PujaService findPujaService(String serviceId) {
        return visiblePujaServices().stream()
                .filter(service -> service.serviceId().equals(serviceId))
                .findFirst()
                .orElse(null);
    }

    private void openPujaServiceDetailsPage(String serviceId) {
        PujaService service = findPujaService(serviceId);
        if (service == null) {
            showInfo("Puja Service Not Found", "This Puja service is not available now.");
            showModulePage("puja");
            return;
        }
        setActiveModule("puja");
        Button back = pujaBackButton("← Back");
        back.setOnAction(event -> showModulePage("puja"));

        ImageView image = createImage(service.image(), 300, 175, 0.5, 0.5);
        image.getStyleClass().add("puja-details-image");

        HBox imageBadges = new HBox(7,
                badge("Verified"),
                badge(service.availableToday() ? "Available" : "Scheduled"),
                badge(service.mode()));
        imageBadges.setAlignment(Pos.CENTER_LEFT);
        VBox imagePane = new VBox(8, image, imageBadges);
        imagePane.getStyleClass().add("puja-details-image-card");
        imagePane.setPrefWidth(320);

        VBox details = new VBox(8,
                sectionTitle(service.name()),
                badge("✓ Simhastha Verified"),
                compactPujaInfoGrid(
                        pujaDetailCard("Temple / Ghat", service.location()),
                        pujaDetailCard("Location", service.location()),
                        pujaDetailCard("Puja Type", service.type()),
                        pujaDetailCard("Starting Price", "Rs. " + service.price()),
                        pujaDetailCard("Duration", service.duration()),
                        pujaDetailCard("Available Slots", service.availableSlots()),
                        pujaDetailCard("Provider / Pandit", valueOr("Best available verified priest", service.providerName())),
                        pujaDetailCard("Languages", service.languages()),
                        pujaDetailCard("Mode", service.mode())));
        details.getStyleClass().add("puja-details-center-column");

        Button bookFromSummary = new Button("Book This Puja");
        bookFromSummary.getStyleClass().add("transport-primary-button");
        bookFromSummary.setMaxWidth(Double.MAX_VALUE);
        bookFromSummary.setOnAction(event -> openPujaBookingPage(service.serviceId()));

        VBox booking = new VBox(8,
                sectionTitle("Booking Summary"),
                pujaSummaryRow("Starting Price", "Rs. " + service.price()),
                pujaSummaryRow("Availability", service.availableToday() ? "Available" : "Scheduled"),
                pujaSummaryRow("Provider Status", "Verified"),
                bookFromSummary);
        booking.getStyleClass().add("puja-booking-summary-card");
        booking.setPrefWidth(285);
        booking.setMaxWidth(310);

        HBox hero = new HBox(14, imagePane, details, booking);
        hero.getStyleClass().addAll("puja-details-panel", "puja-details-hero-compact");
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox lower = new HBox(12,
                infoPanel("About This Puja", paragraph(service.description())),
                infoPanel("What's Included", pujaIncludedList(service)));
        lower.getStyleClass().add("puja-details-lower-compact");
        HBox.setHgrow(lower.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lower.getChildren().get(1), Priority.ALWAYS);

        HBox trust = new HBox(10,
                badge("✓ Verified Provider"),
                badge("✓ Transparent Pricing"),
                badge("✓ Secure Booking"),
                badge("✓ No Agent Payments"));
        trust.getStyleClass().add("puja-details-trust-card");

        Button slots = new Button("View Available Slots");
        slots.getStyleClass().add("puja-secondary-button");
        slots.setOnAction(event -> showInfo("Available Slots",
                valueOr("Slots are configured by the verified provider.", service.availableSlots())));
        Button book = new Button("Proceed to Book Puja");
        book.getStyleClass().add("transport-primary-button");
        book.setOnAction(event -> openPujaBookingPage(service.serviceId()));
        HBox bottomActions = new HBox(10, trust, createSpacer(), slots, book);
        bottomActions.getStyleClass().add("puja-details-bottom-actions");
        bottomActions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(trust, Priority.ALWAYS);

        VBox shell = pageShell("Puja Service Details", "Review verified Puja details before booking.",
                back, hero, lower, bottomActions);
        shell.getStyleClass().add("puja-details-compact-page");
        root.setCenter(scroll(shell));
    }

    private VBox pujaDetailCard(String title, String value) {
        VBox card = new VBox(5, muted(title), strong(valueOr("Not available", value)));
        card.getStyleClass().add("puja-mini-panel");
        return card;
    }

    private GridPane compactPujaInfoGrid(Node... nodes) {
        GridPane grid = twoColumnGrid(nodes);
        grid.getStyleClass().add("puja-compact-info-grid");
        return grid;
    }

    private GridPane compactPujaInfoGridColumns(int columns, Node... nodes) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        for (int index = 0; index < nodes.length; index++) {
            grid.add(nodes[index], index % columns, index / columns);
            GridPane.setHgrow(nodes[index], Priority.ALWAYS);
            if (nodes[index] instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
        for (int index = 0; index < columns; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / columns);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        grid.getStyleClass().add("puja-compact-info-grid");
        return grid;
    }

    private VBox pujaIncludedList(PujaService service) {
        VBox list = new VBox(8,
                muted("✓ Verified Pandit / Provider"),
                muted("✓ Temple / Ghat Service: " + service.location()),
                muted("✓ Mode: " + service.mode()));
        if (!service.languages().isBlank()) {
            list.getChildren().add(muted("✓ Languages: " + service.languages()));
        }
        return list;
    }

    private Button pujaBackButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("puja-secondary-button", "puja-back-button");
        return button;
    }

    private void showPujaServiceDetails(PujaService service) {
        openPujaServiceDetailsPage(service.serviceId());
    }

    private void createPujaBooking(Button button, PujaService service) {
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            showInfo("Login required", "Please login before creating a puja booking.");
            return;
        }
        if ("CLOSED".equalsIgnoreCase(service.bookingStatus()) || "DISABLED".equalsIgnoreCase(service.bookingStatus())) {
            showInfo("Booking unavailable", "This Puja service is currently not available for booking.");
            return;
        }
        openPujaBookingPage(service.serviceId());
    }

    private void openPujaBookingPage(String serviceId) {
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            showInfo("Login required", "Please login before creating a puja booking.");
            return;
        }
        PujaService service = findPujaService(serviceId);
        if (service == null || "CLOSED".equalsIgnoreCase(service.bookingStatus())
                || "DISABLED".equalsIgnoreCase(service.bookingStatus())) {
            showInfo("Booking unavailable", "This Puja service is currently not available for booking.");
            showModulePage("puja");
            return;
        }
        setActiveModule("puja");

        DatePicker date = new DatePicker(java.time.LocalDate.now().plusDays(1));
        date.getStyleClass().add("puja-booking-input");
        date.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(java.time.LocalDate.now()));
            }
        });

        ComboBox<String> time = new ComboBox<>();
        time.getItems().addAll(slotOptions(service));
        time.getSelectionModel().selectFirst();
        time.getStyleClass().add("puja-booking-input");

        Spinner<Integer> devotees = new Spinner<>(1, 20, 1);
        devotees.setEditable(true);
        devotees.getStyleClass().add("puja-booking-spinner");

        ComboBox<String> language = new ComboBox<>();
        language.getItems().addAll(languageOptions(service));
        language.getSelectionModel().selectFirst();
        language.getStyleClass().add("puja-booking-input");

        ComboBox<String> mode = new ComboBox<>();
        mode.getItems().addAll("Offline", "Online / Remote");
        mode.getSelectionModel().select(service.mode().toLowerCase(java.util.Locale.ROOT).contains("online")
                ? "Online / Remote" : "Offline");
        mode.getStyleClass().add("puja-booking-input");

        ComboBox<String> location = new ComboBox<>();
        location.getItems().addAll(valueOr("Approved Puja Zone", service.location()), "Temple", "Ghat",
                "Authorized Puja Zone", "Camp");
        location.getSelectionModel().selectFirst();
        location.getStyleClass().add("puja-booking-input");

        ComboBox<AppDataStore.PujaProviderRecord> provider = pujaProviderCombo();
        TextField name = new TextField(displayName(user));
        name.getStyleClass().add("puja-booking-input");
        TextField email = new TextField(valueOr("", user.email()));
        email.getStyleClass().add("puja-booking-input");
        TextField phone = new TextField(currentUserPhone(user.uid()));
        phone.getStyleClass().add("puja-booking-input");

        CheckBox samagri = new CheckBox("Include Puja Samagri - Not configured");
        samagri.getStyleClass().add("puja-booking-check");
        samagri.setDisable(true);
        CheckBox prasad = new CheckBox("Include Prasad - Not configured");
        prasad.getStyleClass().add("puja-booking-check");
        prasad.setDisable(true);

        TextArea special = new TextArea();
        special.setPromptText("Write any special requirements...");
        special.setPrefRowCount(3);
        special.getStyleClass().add("puja-booking-notes");

        VBox summaryRows = new VBox(8);
        Runnable updateSummary = () -> renderPujaBookingSummary(summaryRows, service, date.getValue(), time.getValue(),
                location.getValue(), provider.getValue(), devotees.getValue(), language.getValue(), mode.getValue());
        date.valueProperty().addListener((obs, old, value) -> updateSummary.run());
        time.valueProperty().addListener((obs, old, value) -> updateSummary.run());
        devotees.valueProperty().addListener((obs, old, value) -> updateSummary.run());
        language.valueProperty().addListener((obs, old, value) -> updateSummary.run());
        mode.valueProperty().addListener((obs, old, value) -> updateSummary.run());
        location.valueProperty().addListener((obs, old, value) -> updateSummary.run());
        provider.valueProperty().addListener((obs, old, value) -> updateSummary.run());
        updateSummary.run();

        GridPane bookingGrid = new GridPane();
        bookingGrid.setHgap(12);
        bookingGrid.setVgap(12);
        addBookingField(bookingGrid, 0, "Selected Puja *", strong(service.name()));
        addBookingField(bookingGrid, 1, "Booking Date *", date);
        addBookingField(bookingGrid, 2, "Preferred Time *", time);
        addBookingField(bookingGrid, 3, "No. of Devotees *", devotees);
        addBookingField(bookingGrid, 4, "Language *", language);
        addBookingField(bookingGrid, 5, "Puja Mode *", mode);
        addBookingField(bookingGrid, 6, "Location *", location);
        addBookingField(bookingGrid, 7, "Priest / Provider *", provider);
        bookingGrid.getStyleClass().add("puja-booking-form-grid");

        GridPane userGrid = new GridPane();
        userGrid.setHgap(12);
        userGrid.setVgap(12);
        addBookingField(userGrid, 0, "Full Name", name);
        addBookingField(userGrid, 1, "Email", email);
        addBookingField(userGrid, 2, "Mobile Number", phone);

        VBox left = new VBox(14, sectionTitle("Booking Details"), bookingGrid,
                sectionTitle("User Details"), userGrid,
                new VBox(8, sectionTitle("Additional Options"), samagri, prasad),
                new VBox(8, sectionTitle("Special Requirements"), special));
        left.getStyleClass().add("puja-booking-left-panel");

        VBox secure = new VBox(5, strong("🔒 Secure Payment"), muted("Temporary demo payment flow"),
                muted("Real Razorpay integration will be connected later."));
        secure.getStyleClass().add("puja-secure-payment-card");
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("puja-secondary-button");
        cancel.setOnAction(event -> openPujaServiceDetailsPage(service.serviceId()));
        Button pay = new Button("Proceed to Payment →");
        pay.getStyleClass().add("transport-primary-button");
        pay.setOnAction(event -> confirmPujaBookingFromPage(pay, user, service, date, time, devotees, language,
                mode, location, provider, samagri, prasad, special, name, email, phone));
        HBox actions = new HBox(10, cancel, createSpacer(), pay);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(14, sectionTitle("Booking Summary"), summaryRows, secure, actions);
        right.getStyleClass().add("puja-booking-summary-card");
        right.setPrefWidth(390);
        HBox layout = new HBox(16, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);

        Button back = pujaBackButton("← Back");
        back.setOnAction(event -> openPujaServiceDetailsPage(service.serviceId()));
        HBox top = new HBox(12,
                new VBox(4, sectionTitle("Book Puja"), muted("Complete your verified Simhastha Puja booking request.")),
                createSpacer(), back);
        top.setAlignment(Pos.CENTER_LEFT);

        HBox footer = new HBox(12,
                pujaMiniInfoSection("Verified Priests", "\uE73E", "All priests are verified by Simhastha Connect"),
                pujaMiniInfoSection("Trusted & Secure", "\uE72E", "Booking and payment flow stays safe"),
                pujaMiniInfoSection("Official Counters", "\uE80F", "Puja at authorized counters only"),
                pujaMiniInfoSection("24/7 Support", "\uE717", "We are here to help anytime"));
        footer.getStyleClass().add("puja-user-support-row");
        footer.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        VBox shell = pageShell("Book Puja", "Proceed with a safe and verified spiritual booking.", top, layout, footer);
        shell.getStyleClass().add("puja-booking-compact-page");
        root.setCenter(scroll(shell));
    }

    private ComboBox<AppDataStore.PujaProviderRecord> pujaProviderCombo() {
        ComboBox<AppDataStore.PujaProviderRecord> provider = new ComboBox<>();
        provider.getItems().add(null);
        provider.getItems().addAll(AppDataStore.approvedPujaProviders());
        provider.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(AppDataStore.PujaProviderRecord item) {
                return item == null ? "Assign Best Available Verified Priest" : item.fullName + " • " + item.specialization;
            }

            @Override
            public AppDataStore.PujaProviderRecord fromString(String value) {
                return null;
            }
        });
        provider.getSelectionModel().selectFirst();
        provider.getStyleClass().add("puja-booking-input");
        return provider;
    }

    private String currentUserPhone(String uid) {
        return AppDataStore.users().stream()
                .filter(record -> record.uid.equals(uid))
                .map(record -> record.mobile)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private void renderPujaBookingSummary(VBox rows, PujaService service, java.time.LocalDate date, String time,
            String location, AppDataStore.PujaProviderRecord provider, int devotees, String language, String mode) {
        long base = Math.max(0, service.price());
        long total = base;
        rows.getChildren().setAll(
                pujaSummaryRow("Puja", service.name()),
                pujaSummaryRow("Date", date == null ? "Select date" : date.toString()),
                pujaSummaryRow("Time", valueOr("Select time", time)),
                pujaSummaryRow("Location", valueOr("Select location", location)),
                pujaSummaryRow("Priest / Provider", provider == null ? "Best available verified priest" : provider.fullName),
                pujaSummaryRow("Devotees", String.valueOf(devotees)),
                pujaSummaryRow("Language", valueOr("Select language", language)),
                pujaSummaryRow("Mode", valueOr("Offline", mode)),
                pujaSummaryDivider(),
                pujaSummaryRow("Base Puja Price", "Rs. " + base),
                pujaSummaryRow("Puja Samagri", "Not configured"),
                pujaSummaryRow("Prasad", "Not configured"),
                pujaSummaryRow("Service Fee", "Rs. 0"),
                label("TOTAL AMOUNT: Rs. " + total, "puja-total-amount"));
    }

    private HBox pujaSummaryRow(String labelText, String value) {
        HBox row = new HBox(8, muted(labelText), createSpacer(), strong(valueOr("Not available", value)));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Region pujaSummaryDivider() {
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.getStyleClass().add("puja-summary-divider");
        return divider;
    }

    private void confirmPujaBookingFromPage(Button pay, AppSession.User user, PujaService service,
            DatePicker date, ComboBox<String> time, Spinner<Integer> devotees, ComboBox<String> language,
            ComboBox<String> mode, ComboBox<String> location, ComboBox<AppDataStore.PujaProviderRecord> provider,
            CheckBox samagri, CheckBox prasad, TextArea special, TextField name, TextField email, TextField phone) {
        PujaService freshService = findPujaService(service.serviceId());
        if (freshService == null) {
            showInfo("Service Unavailable", "This Puja service is no longer available.");
            return;
        }
        if (date.getValue() == null || date.getValue().isBefore(java.time.LocalDate.now())) {
            showInfo("Invalid Date", "Select today or a future booking date.");
            return;
        }
        if (time.getValue() == null || time.getValue().isBlank() || location.getValue() == null
                || location.getValue().isBlank() || devotees.getValue() == null || devotees.getValue() < 1) {
            showInfo("Missing Details", "Select time, location and devotees before proceeding.");
            return;
        }
        if (name.getText() == null || name.getText().isBlank()) {
            showInfo("Missing User Name", "Enter devotee name before proceeding.");
            return;
        }
        int slotCapacity = parseCapacity(freshService.availableSlots());
        if (slotCapacity > 0 && devotees.getValue() > slotCapacity) {
            showInfo("Slot Full", "This service has only " + slotCapacity + " configured slots.");
            return;
        }
        long base = Math.max(0, freshService.price());
        long total = base;
        if (total <= 0) {
            showInfo("Payment Amount Required",
                    "This Puja service has Rs. 0 as total amount. Please ask admin to set a valid price before booking.");
            return;
        }
        pay.setDisable(true);
        try {
            AppDataStore.PujaProviderRecord selectedProvider = provider.getValue();
            String bookingId = "PUJA-" + java.time.LocalDate.now().getYear() + "-"
                    + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
            AppDataStore.PujaBookingRecord booking = new AppDataStore.PujaBookingRecord(bookingId, user.uid(),
                    name.getText(), phone.getText(), email.getText(), freshService.serviceId(), freshService.name(),
                    freshService.type(), selectedProvider == null ? freshService.providerId() : selectedProvider.providerId,
                    selectedProvider == null ? valueOr("Best Available Verified Priest", freshService.providerName()) : selectedProvider.fullName,
                    freshService.location(), date.getValue().toString(), time.getValue(), location.getValue(),
                    devotees.getValue(), language.getValue(), mode.getValue(), total, "PAYMENT_PENDING", "PENDING",
                    "", "", "", location.getValue(), samagri.isSelected(), 0, prasad.isSelected(),
                    0, base, 0, total, special.getText(), "", "", "", "", "", "", "", "", "");
            AppDataStore.savePujaBooking(booking);
            startPujaRazorpayPayment(booking, user);
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Booking Failed", exception.getMessage());
        } finally {
            pay.setDisable(false);
        }
    }

    private void addBookingField(GridPane form, int row, String labelText, Node control) {
        Label label = label(labelText, "puja-booking-field-label");
        VBox cell = new VBox(6, label, control);
        cell.getStyleClass().add("puja-booking-field");
        form.add(cell, row % 2, row / 2);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    private java.util.List<String> slotOptions(PujaService service) {
        String slots = service.duration();
        java.util.List<String> defaults = java.util.List.of("Morning Slot", "Afternoon Slot", "Evening Slot");
        return defaults;
    }

    private java.util.List<String> languageOptions(PujaService service) {
        java.util.List<String> options = java.util.Arrays.stream(service.languages().split(",|/"))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .distinct()
                .toList();
        return options.isEmpty() ? java.util.List.of("Marathi", "Hindi", "Sanskrit") : options;
    }

    private void startPujaRazorpayPayment(AppDataStore.PujaBookingRecord booking, AppSession.User user) {
        if ("PAID".equalsIgnoreCase(booking.paymentStatus)) {
            showInfo("Already Paid", "This Puja booking is already paid.");
            return;
        }
        if (booking.totalAmount <= 0) {
            showInfo("Payment Amount Required", "This Puja booking cannot be paid because the total amount is Rs. 0.");
            return;
        }
        showPujaPaymentProcessingPage(booking, user);
    }

    private void showPujaPaymentProcessingPage(AppDataStore.PujaBookingRecord booking, AppSession.User user) {
        setActiveModule("puja");
        final boolean[] cancelled = { false };
        final Timeline[] successRef = new Timeline[1];
        Button back = pujaBackButton("← Back");
        back.setOnAction(event -> {
            cancelled[0] = true;
            if (successRef[0] != null) {
                successRef[0].stop();
            }
            openPujaBookingPage(booking.serviceId);
        });

        VBox summary = new VBox(9,
                sectionTitle("Booking Summary"),
                pujaSummaryRow("Puja", booking.serviceName),
                pujaSummaryRow("Booking ID", booking.bookingId),
                pujaSummaryRow("Date", booking.date),
                pujaSummaryRow("Time", booking.time),
                pujaSummaryRow("Location", valueOr(booking.location, booking.locationName)),
                pujaSummaryRow("Provider", valueOr("Best available verified priest", booking.providerName)),
                pujaSummaryDivider(),
                pujaSummaryRow("Amount", "Rs. " + booking.totalAmount));
        summary.getStyleClass().add("puja-booking-summary-card");

        Label status = label("Preparing secure temporary payment confirmation...", "puja-success-title");
        VBox process = new VBox(12,
                AppUi.symbolIcon("\uE8C7", "puja-trust-icon"),
                sectionTitle("Secure Payment Processing"),
                paragraph("This is a temporary development/demo payment step. No card, CVV, UPI PIN or banking details are collected in Simhastha Connect."),
                status,
                muted("Integration point ready: real Razorpay can replace this processing step later without changing booking records."));
        process.getStyleClass().add("puja-details-panel");

        HBox layout = new HBox(16, process, summary);
        HBox.setHgrow(process, Priority.ALWAYS);
        summary.setPrefWidth(380);
        root.setCenter(scroll(pageShell("Secure Payment Processing",
                "Confirming your Puja booking payment safely inside the dashboard.",
                back, layout)));

        Timeline success = new Timeline(new KeyFrame(Duration.seconds(1.4), event -> {
            if (!cancelled[0]) {
                completeTemporaryPujaPayment(booking);
            }
        }));
        successRef[0] = success;
        success.setCycleCount(1);
        success.play();
    }

    private void completeTemporaryPujaPayment(AppDataStore.PujaBookingRecord booking) {
        String now = String.valueOf(System.currentTimeMillis());
        String internalPaymentId = "TEMP-PUJA-" + java.util.UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        String qrTicketId = "PUJA-TICKET-" + booking.bookingId;
        String verificationToken = java.util.UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(java.util.Locale.ROOT);
        try {
            AppDataStore.updatePujaBookingPayment(booking.bookingId, "CONFIRMED", "PAID",
                    internalPaymentId, "", "", "TEMPORARY_DEMO",
                    booking.paymentCreatedAt.isBlank() ? now : booking.paymentCreatedAt,
                    now, "");
            AppDataStore.PujaBookingRecord ticketBooking =
                    AppDataStore.updatePujaBookingQrTicket(booking.bookingId, qrTicketId, verificationToken);
            showPujaPaymentSuccessPage(ticketBooking);
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Payment Update Failed", exception.getMessage());
            showModulePage("bookings");
        }
    }

    private void showPujaPaymentSuccessPage(AppDataStore.PujaBookingRecord booking) {
        setActiveModule("bookings");
        Button back = pujaBackButton("← Back");
        back.setOnAction(event -> showModulePage("bookings"));

        PujaService service = findPujaService(booking.serviceId);
        ImageView image = createImage(service == null ? "/images/trimbakeshwar.jpg" : service.image(), 190, 112, 0.5, 0.5);
        image.getStyleClass().add("puja-details-image");

        GridPane details = compactPujaInfoGridColumns(3,
                pujaDetailCard("Booking ID", booking.bookingId),
                pujaDetailCard("Puja", booking.serviceName),
                pujaDetailCard("Date", booking.date),
                pujaDetailCard("Time", booking.time),
                pujaDetailCard("Location", valueOr(booking.location, booking.locationName)),
                pujaDetailCard("Devotees", String.valueOf(booking.devoteesCount)),
                pujaDetailCard("Total Amount", "Rs. " + booking.totalAmount),
                pujaDetailCard("Payment Status", "Payment Success"),
                pujaDetailCard("Booking Status", "Booking Confirmed"));

        VBox ticket = pujaTicketCard(booking, "SIMHASTHA-PUJA|" + booking.bookingId + "|" + booking.qrVerificationToken);
        ticket.getStyleClass().add("puja-ticket-compact");
        Button bookings = new Button("View My Bookings");
        bookings.getStyleClass().add("puja-secondary-button");
        bookings.setOnAction(event -> showModulePage("bookings"));
        Button viewTicket = new Button("View Ticket");
        viewTicket.getStyleClass().add("transport-primary-button");
        viewTicket.setOnAction(event -> showPujaTicketVerificationPage(booking.bookingId, booking.qrVerificationToken));
        HBox actions = new HBox(10, createSpacer(), bookings, viewTicket);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox headline = new VBox(6,
                label("✓ Booking Confirmed", "puja-success-title"),
                strong(booking.serviceName),
                muted("Your Puja booking is confirmed. Keep the QR ticket ready at the service counter."),
                new HBox(7, badge("Payment Success"), badge("QR Ready")));
        headline.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headline, Priority.ALWAYS);

        HBox hero = new HBox(16, image, headline, ticket);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("puja-success-hero");

        VBox success = new VBox(10,
                hero,
                details,
                actions);
        success.getStyleClass().add("puja-success-card");

        VBox shell = pageShell("Payment Success",
                "Your Puja booking is confirmed and QR ticket is ready.",
                back, success);
        shell.getStyleClass().add("puja-single-screen-page");
        root.setCenter(scroll(shell));
    }

    private int parseCapacity(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception exception) {
            return 0;
        }
    }

    private java.util.List<PujaService> visiblePujaServices() {
        if (!AppDataStore.pujaServices().isEmpty()) {
            return AppDataStore.publicPujaServices().stream()
                    .map(this::pujaServiceFromRecord)
                    .toList();
        }
        java.util.List<PujaService> services = new java.util.ArrayList<>();
        for (AppDataStore.ServiceItem item : AppDataStore.items("puja")) {
            boolean exists = services.stream().anyMatch(service -> service.name().equalsIgnoreCase(item.title));
            if (!exists) {
                services.add(pujaServiceFromAdminItem(item));
            }
        }
        return services;
    }

    private PujaService pujaServiceFromRecord(AppDataStore.PujaServiceRecord service) {
        return new PujaService(service.serviceId, service.name, service.pujaType,
                valueOr("Admin-approved puja service.", service.description), service.duration,
                service.availableSlots, parsePujaPrice(service.price), service.templeOrGhat, service.mode, service.languages,
                true, service.imageUrl, service.providerId, service.providerName, service.bookingStatus);
    }

    private PujaService pujaServiceFromAdminItem(AppDataStore.ServiceItem item) {
        String detail = item.detail == null ? "" : item.detail;
        return new PujaService(item.id, item.title,
                pujaDetailValue(detail, "Puja Type", "Darshan"),
                valueOr("Admin-approved puja service published by Simhastha admin.", detail),
                pujaDetailValue(detail, "Duration", "Varies"),
                pujaDetailValue(detail, "Available Slots", "Varies"),
                parsePujaPrice(pujaDetailValue(detail, "Price", "0")),
                pujaDetailValue(detail, "Temple / Ghat", "Approved Location"),
                pujaDetailValue(detail, "Mode", "Offline"),
                pujaDetailValue(detail, "Languages", "Marathi, Hindi, Sanskrit"),
                true,
                "/images/trimbakeshwar.jpg", "", pujaDetailValue(detail, "Pandit / Provider", "Admin"),
                pujaDetailValue(detail, "Booking Status", "OPEN"));
    }

    private String pujaDetailValue(String detail, String label, String fallback) {
        String prefix = label + ":";
        for (String part : (detail == null ? "" : detail).split("\\|")) {
            String clean = part.trim();
            if (clean.toLowerCase(java.util.Locale.ROOT).startsWith(prefix.toLowerCase(java.util.Locale.ROOT))) {
                String value = clean.substring(prefix.length()).trim();
                return value.isBlank() ? fallback : value;
            }
        }
        return fallback;
    }

    private int parsePujaPrice(String priceText) {
        try {
            String digits = priceText == null ? "" : priceText.replaceAll("[^0-9]", "");
            return digits.isBlank() ? 0 : Integer.parseInt(digits);
        } catch (Exception exception) {
            return 0;
        }
    }

    private java.util.List<PujaService> pujaServices() {
        return java.util.List.of(
                new PujaService("Rudrabhishek", "Abhishek", "Traditional Shiva abhishek with verified priest support.", "45 min", 751, "Trimbakeshwar", "Offline", "Marathi, Hindi, Sanskrit", true, "/images/trimbakeshwar.jpg"),
                new PujaService("Mahamrityunjaya Jaap", "Jaap", "Jaap sankalp for health, protection and family well-being.", "60 min", 1100, "Panchavati", "Both", "Marathi, Hindi, Sanskrit", true, "/images/ramkund_sunrise.jpg"),
                new PujaService("Hawan", "Hawan", "Sacred fire ritual with guided samagri checklist.", "75 min", 1500, "Ramkund", "Offline", "Hindi, Sanskrit", false, "/images/godavari_kumbh.jpg"),
                new PujaService("Sankalp Puja", "Abhishek", "Quick sankalp puja assistance for Simhastha pilgrims.", "30 min", 501, "Ramkund", "Offline", "Marathi, Hindi", true, "/images/ramkund_sunrise.jpg"),
                new PujaService("Satyanarayan Puja", "Custom", "Family puja service with verified priest coordination.", "90 min", 2100, "Nashik City", "Offline", "Marathi, Hindi", false, "/images/godavari_kumbh.jpg"),
                new PujaService("Graha Shanti", "Jaap", "Ritual guidance for graha shanti and family sankalp.", "80 min", 1800, "Panchavati", "Offline", "Marathi, Hindi, Sanskrit", true, "/images/trimbakeshwar.jpg"),
                new PujaService("Abhishek", "Abhishek", "Temple abhishek guidance with verified counter information.", "40 min", 651, "Trimbakeshwar", "Offline", "Marathi, Hindi", true, "/images/trimbakeshwar.jpg"),
                new PujaService("Aarti Participation", "Darshan", "Participate in scheduled aarti with official timing guidance.", "25 min", 301, "Ramkund", "Offline", "Marathi, Hindi", true, "/images/ramkund_sunrise.jpg"),
                new PujaService("Pind Daan", "Custom", "Pind daan information and verified priest support.", "60 min", 1200, "Godavari Ghat", "Offline", "Hindi, Sanskrit", false, "/images/godavari_kumbh.jpg"),
                new PujaService("Online / Remote Puja", "Remote", "Remote sankalp puja for pilgrims unable to attend physically.", "45 min", 901, "Online", "Online", "Marathi, Hindi, English", true, "/images/welcome-light.png"),
                new PujaService("Prasad Service", "Prasad", "Prasad request and pickup guidance through approved counters.", "20 min", 251, "Panchavati", "Both", "Marathi, Hindi", true, "/images/ramkund_sunrise.jpg"),
                new PujaService("Temple Darshan", "Darshan", "Temple darshan support with timing and queue guidance.", "30 min", 0, "Kalaram Mandir", "Offline", "Marathi, Hindi, English", true, "/images/trimbakeshwar.jpg"),
                new PujaService("Custom Puja", "Custom", "Custom ritual request placeholder for later Firebase-backed booking.", "Varies", 0, "Multiple Locations", "Both", "Marathi, Hindi, English, Sanskrit", true, "/images/godavari_kumbh.jpg"));
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
        Button back = pujaBackButton("← Back");
        back.setOnAction(event -> showModulePage("puja"));
        VBox rows = new VBox(12);
        if (user == null) {
            rows.getChildren().add(pujaBookingsEmptyState("Login Required",
                    "Login to view your Puja bookings and QR tickets."));
        } else {
            java.util.List<AppDataStore.PujaBookingRecord> pujaBookings = AppDataStore.pujaBookingsForUser(user.uid());
            if (pujaBookings.isEmpty()) {
                rows.getChildren().add(pujaBookingsEmptyState("No Puja Bookings Yet",
                        "Browse Puja Services and book a verified spiritual service."));
            } else {
                rows.getChildren().add(pujaBookingSummaryCards(pujaBookings));
                pujaBookings.forEach(booking -> rows.getChildren().add(pujaBookingRow(booking)));
            }
            java.util.List<AppDataStore.BookingRecord> otherBookings = AppDataStore.bookingsForUser(user.uid());
            if (!otherBookings.isEmpty()) {
                VBox otherRows = new VBox(10);
                otherBookings.forEach(booking -> otherRows.getChildren().add(bookingRow(booking)));
                rows.getChildren().add(infoPanel("Other Bookings", otherRows));
            }
        }
        return pageShell("My Bookings", "View and manage your Puja bookings.", back, rows);
    }

    private VBox pujaBookingsEmptyState(String title, String detail) {
        Button browse = new Button("Browse Puja Services");
        browse.getStyleClass().add("transport-primary-button");
        browse.setOnAction(event -> showModulePage("puja"));
        VBox empty = new VBox(12,
                AppUi.symbolIcon("\uE8F9", "puja-trust-icon"),
                sectionTitle(title),
                muted(detail),
                browse);
        empty.getStyleClass().add("puja-empty-state");
        empty.setAlignment(Pos.CENTER_LEFT);
        return empty;
    }

    private HBox pujaBookingSummaryCards(java.util.List<AppDataStore.PujaBookingRecord> bookings) {
        long confirmed = bookings.stream().filter(item -> "CONFIRMED".equalsIgnoreCase(item.bookingStatus)
                || "PRIEST_ASSIGNED".equalsIgnoreCase(item.bookingStatus)
                || "READY".equalsIgnoreCase(item.bookingStatus)).count();
        long pending = bookings.stream().filter(item -> "PENDING".equalsIgnoreCase(item.bookingStatus)
                || "PAYMENT_PENDING".equalsIgnoreCase(item.bookingStatus)).count();
        long completed = bookings.stream().filter(item -> "COMPLETED".equalsIgnoreCase(item.bookingStatus)).count();
        HBox row = new HBox(12,
                pujaBookingMetric("Total Bookings", String.valueOf(bookings.size())),
                pujaBookingMetric("Confirmed", String.valueOf(confirmed)),
                pujaBookingMetric("Pending", String.valueOf(pending)),
                pujaBookingMetric("Completed", String.valueOf(completed)));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox pujaBookingMetric(String title, String value) {
        VBox card = new VBox(4, label(value, "puja-total-amount"), muted(title));
        card.getStyleClass().add("puja-booking-summary-metric");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private String pujaBookingImage(String serviceId) {
        return AppDataStore.pujaServices().stream()
                .filter(service -> service.serviceId.equals(serviceId))
                .map(service -> service.imageUrl)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseGet(() -> visiblePujaServices().stream()
                        .filter(service -> service.serviceId().equals(serviceId))
                        .map(PujaService::image)
                        .findFirst()
                        .orElse("/images/trimbakeshwar.jpg"));
    }

    private String pujaBookingTemple(String serviceId, String fallback) {
        return AppDataStore.pujaServices().stream()
                .filter(service -> service.serviceId.equals(serviceId))
                .map(service -> valueOr(fallback, service.templeOrGhat))
                .findFirst()
                .orElse(fallback);
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

    private HBox pujaBookingRow(AppDataStore.PujaBookingRecord booking) {
        ImageView image = createImage(pujaBookingImage(booking.serviceId), 142, 92, 0.5, 0.5);
        image.getStyleClass().add("puja-card-image");

        Button details = new Button("View Details");
        details.getStyleClass().add("puja-secondary-button");
        details.setOnAction(event -> showPujaBookingDetailsPage(booking));

        Button ticket = new Button("View QR / Ticket");
        ticket.getStyleClass().add("transport-primary-button");
        ticket.setDisable(booking.qrVerificationToken == null || booking.qrVerificationToken.isBlank());
        ticket.setOnAction(event -> showPujaTicketVerificationPage(booking.bookingId, booking.qrVerificationToken));

        HBox badges = new HBox(7, badge(booking.bookingStatus), badge(booking.paymentStatus));
        VBox text = new VBox(5,
                strong(booking.serviceName),
                muted("Booking ID: " + booking.bookingId),
                muted("Date: " + booking.date + "  •  Time: " + booking.time),
                muted("Temple/Ghat: " + pujaBookingTemple(booking.serviceId, valueOr(booking.location, booking.templeOrGhat))),
                muted("Location: " + valueOr(booking.location, booking.locationName)),
                muted("Provider: " + valueOr("Best available verified priest", booking.providerName)),
                label("Amount: Rs. " + booking.totalAmount, "puja-price"),
                badges);
        HBox actions = new HBox(8, details, ticket);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(14, image, text, createSpacer(), actions);
        row.getStyleClass().add("puja-booking-card");
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        return row;
    }

    private void showPujaBookingDetailsPage(AppDataStore.PujaBookingRecord booking) {
        setActiveModule("bookings");
        Button back = pujaBackButton("← Back");
        back.setOnAction(event -> showModulePage("bookings"));

        ImageView image = createImage(pujaBookingImage(booking.serviceId), 190, 112, 0.5, 0.5);
        image.getStyleClass().add("puja-details-image");

        VBox qr = booking.qrVerificationToken == null || booking.qrVerificationToken.isBlank()
                ? new VBox(8, sectionTitle("QR Ticket"), muted("QR ticket will be available after payment confirmation."))
                : pujaTicketCard(booking, "SIMHASTHA-PUJA|" + booking.bookingId + "|" + booking.qrVerificationToken);
        qr.getStyleClass().add("puja-ticket-compact");

        GridPane bookingInfo = compactPujaInfoGridColumns(4,
                pujaDetailCard("Date", booking.date),
                pujaDetailCard("Time", booking.time),
                pujaDetailCard("Location", valueOr(booking.location, booking.locationName)),
                pujaDetailCard("Devotees", String.valueOf(booking.devoteesCount)),
                pujaDetailCard("Language", booking.language),
                pujaDetailCard("Mode", booking.mode),
                pujaDetailCard("Amount", "Rs. " + booking.totalAmount),
                pujaDetailCard("Payment Status", booking.paymentStatus),
                pujaDetailCard("Booking Status", booking.bookingStatus));

        Button ticket = new Button("View QR / Ticket");
        ticket.getStyleClass().add("transport-primary-button");
        ticket.setDisable(booking.qrVerificationToken == null || booking.qrVerificationToken.isBlank());
        ticket.setOnAction(event -> showPujaTicketVerificationPage(booking.bookingId, booking.qrVerificationToken));
        Button retry = new Button("Retry Payment");
        retry.getStyleClass().add("puja-secondary-button");
        retry.setDisable("PAID".equalsIgnoreCase(booking.paymentStatus) || booking.totalAmount <= 0);
        retry.setOnAction(event -> {
            AppSession.User user = AppSession.currentUser();
            if (user != null) {
                startPujaRazorpayPayment(booking, user);
            }
        });
        HBox actions = new HBox(10, createSpacer(), retry, ticket);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(8,
                sectionTitle(booking.serviceName),
                muted("Booking ID: " + booking.bookingId),
                new HBox(7, badge(booking.bookingStatus), badge(booking.paymentStatus)),
                muted("Temple/Ghat: " + pujaBookingTemple(booking.serviceId, booking.templeOrGhat)),
                muted("Provider: " + valueOr("Best available verified priest", booking.providerName)),
                qr);
        right.getStyleClass().add("puja-details-panel");
        HBox hero = new HBox(16, image, right);
        hero.getStyleClass().add("puja-success-hero");
        HBox.setHgrow(right, Priority.ALWAYS);

        VBox panel = new VBox(10,
                hero,
                bookingInfo,
                actions);
        panel.getStyleClass().add("puja-details-panel");
        VBox shell = pageShell("Puja Booking Details", "Review your booking and QR ticket.",
                back, panel);
        shell.getStyleClass().add("puja-single-screen-page");
        root.setCenter(scroll(shell));
    }

    private void showPujaTicketVerificationPage(String bookingId, String token) {
        AppDataStore.PujaBookingRecord booking = AppDataStore.pujaBookings().stream()
                .filter(item -> item.bookingId.equals(bookingId))
                .findFirst()
                .orElse(null);
        if (booking == null || token == null || token.isBlank()
                || booking.qrVerificationToken == null
                || !booking.qrVerificationToken.equals(token)) {
            showInfo("Ticket Not Available", "This Puja ticket is not available or the verification token is invalid.");
            return;
        }

        setActiveModule("bookings");
        Button back = pujaBackButton("← Back");
        back.setOnAction(event -> showPujaBookingDetailsPage(booking));

        String payload = "SIMHASTHA-PUJA|" + booking.bookingId + "|" + token;
        VBox ticketCard = pujaTicketCard(booking, payload);
        ticketCard.getStyleClass().add("puja-ticket-compact");
        GridPane details = compactPujaInfoGridColumns(4,
                pujaDetailCard("Booking ID", booking.bookingId),
                pujaDetailCard("Puja", booking.serviceName),
                pujaDetailCard("Date", booking.date),
                pujaDetailCard("Time", booking.time),
                pujaDetailCard("Location", valueOr(booking.location, booking.locationName)),
                pujaDetailCard("Devotees", String.valueOf(booking.devoteesCount)),
                pujaDetailCard("Payment", booking.paymentStatus),
                pujaDetailCard("Status", booking.bookingStatus));

        VBox trust = new VBox(6,
                sectionTitle("Verification Instructions"),
                muted("Show this ticket at the Puja service counter. QR token is generated from your confirmed booking."));
        trust.getStyleClass().add("puja-details-trust-card");

        HBox hero = new HBox(16, ticketCard, trust);
        hero.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(trust, Priority.ALWAYS);
        VBox panel = new VBox(10, hero, details);
        panel.getStyleClass().add("puja-details-panel");
        VBox shell = pageShell("Puja QR Ticket", "Use this safe ticket for verification at the service counter.",
                back, panel);
        shell.getStyleClass().add("puja-single-screen-page");
        root.setCenter(scroll(shell));
    }

    private VBox pujaTicketCard(AppDataStore.PujaBookingRecord booking, String payload) {
        ImageView qr = new ImageView(createQrImage(payload, 178));
        qr.setFitWidth(178);
        qr.setFitHeight(178);
        qr.setPreserveRatio(true);
        qr.getStyleClass().add("ticket-qr-image");

        VBox copy = new VBox(5,
                strong("SIMHASTHA CONNECT PUJA TICKET"),
                muted(booking.serviceName),
                muted("Booking: " + booking.bookingId),
                muted("Token: " + booking.qrVerificationToken));
        copy.setAlignment(Pos.CENTER_LEFT);
        HBox body = new HBox(18, qr, copy);
        body.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, body);
        card.getStyleClass().add("ticket-card");
        card.setPadding(new Insets(18));
        return card;
    }

    private WritableImage createQrImage(String payload, int pixelSize) {
        boolean[][] modules = createQrMatrix(payload == null ? "" : payload);
        int quiet = 4;
        int moduleCount = modules.length + quiet * 2;
        int scale = Math.max(3, pixelSize / moduleCount);
        int imageSize = moduleCount * scale;
        WritableImage image = new WritableImage(imageSize, imageSize);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                int moduleX = x / scale - quiet;
                int moduleY = y / scale - quiet;
                boolean dark = moduleX >= 0 && moduleY >= 0
                        && moduleX < modules.length && moduleY < modules.length
                        && modules[moduleY][moduleX];
                writer.setColor(x, y, dark ? Color.BLACK : Color.WHITE);
            }
        }
        return image;
    }

    private boolean[][] createQrMatrix(String text) {
        final int version = 4;
        final int size = version * 4 + 17;
        boolean[][] modules = new boolean[size][size];
        boolean[][] reserved = new boolean[size][size];
        addQrFinder(modules, reserved, 0, 0);
        addQrFinder(modules, reserved, size - 7, 0);
        addQrFinder(modules, reserved, 0, size - 7);
        addQrTiming(modules, reserved);
        addQrAlignment(modules, reserved, 26, 26);
        setQrModule(modules, reserved, 8, size - 8, true, true);
        reserveQrFormat(reserved, size);

        java.util.List<Integer> data = qrDataCodewords(text);
        data.addAll(qrReedSolomonRemainder(data, 20));
        placeQrData(modules, reserved, data, 0);
        addQrFormatBits(modules, 0);
        return modules;
    }

    private java.util.List<Integer> qrDataCodewords(String text) {
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int length = Math.min(bytes.length, 78);
        java.util.List<Integer> bits = new java.util.ArrayList<>();
        appendQrBits(bits, 0b0100, 4);
        appendQrBits(bits, length, 8);
        for (int index = 0; index < length; index++) {
            appendQrBits(bits, bytes[index] & 0xff, 8);
        }
        int maxBits = 80 * 8;
        appendQrBits(bits, 0, Math.min(4, maxBits - bits.size()));
        while (bits.size() % 8 != 0) {
            bits.add(0);
        }
        java.util.List<Integer> codewords = new java.util.ArrayList<>();
        for (int i = 0; i < bits.size(); i += 8) {
            int value = 0;
            for (int bit = 0; bit < 8; bit++) {
                value = (value << 1) | bits.get(i + bit);
            }
            codewords.add(value);
        }
        for (int pad = 0; codewords.size() < 80; pad++) {
            codewords.add((pad % 2 == 0) ? 0xec : 0x11);
        }
        return codewords;
    }

    private void appendQrBits(java.util.List<Integer> bits, int value, int count) {
        for (int i = count - 1; i >= 0; i--) {
            bits.add((value >>> i) & 1);
        }
    }

    private void addQrFinder(boolean[][] modules, boolean[][] reserved, int x, int y) {
        for (int dy = -1; dy <= 7; dy++) {
            for (int dx = -1; dx <= 7; dx++) {
                int xx = x + dx;
                int yy = y + dy;
                if (xx < 0 || yy < 0 || yy >= modules.length || xx >= modules.length) {
                    continue;
                }
                boolean dark = dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6
                        && (dx == 0 || dx == 6 || dy == 0 || dy == 6
                                || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4));
                setQrModule(modules, reserved, xx, yy, dark, true);
            }
        }
    }

    private void addQrTiming(boolean[][] modules, boolean[][] reserved) {
        for (int i = 8; i < modules.length - 8; i++) {
            setQrModule(modules, reserved, i, 6, i % 2 == 0, true);
            setQrModule(modules, reserved, 6, i, i % 2 == 0, true);
        }
    }

    private void addQrAlignment(boolean[][] modules, boolean[][] reserved, int centerX, int centerY) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                boolean dark = Math.max(Math.abs(dx), Math.abs(dy)) != 1;
                setQrModule(modules, reserved, centerX + dx, centerY + dy, dark, true);
            }
        }
    }

    private void reserveQrFormat(boolean[][] reserved, int size) {
        for (int i = 0; i < 9; i++) {
            if (i != 6) {
                reserved[8][i] = true;
                reserved[i][8] = true;
            }
        }
        for (int i = 0; i < 8; i++) {
            reserved[8][size - 1 - i] = true;
            reserved[size - 1 - i][8] = true;
        }
    }

    private void setQrModule(boolean[][] modules, boolean[][] reserved, int x, int y, boolean dark, boolean reserve) {
        modules[y][x] = dark;
        if (reserve) {
            reserved[y][x] = true;
        }
    }

    private void placeQrData(boolean[][] modules, boolean[][] reserved, java.util.List<Integer> codewords, int mask) {
        java.util.List<Integer> bits = new java.util.ArrayList<>();
        for (int codeword : codewords) {
            appendQrBits(bits, codeword, 8);
        }
        int bitIndex = 0;
        int direction = -1;
        for (int right = modules.length - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right--;
            }
            for (int i = 0; i < modules.length; i++) {
                int y = direction == -1 ? modules.length - 1 - i : i;
                for (int dx = 0; dx < 2; dx++) {
                    int x = right - dx;
                    if (reserved[y][x]) {
                        continue;
                    }
                    boolean bit = bitIndex < bits.size() && bits.get(bitIndex++) == 1;
                    if (qrMask(mask, x, y)) {
                        bit = !bit;
                    }
                    modules[y][x] = bit;
                }
            }
            direction = -direction;
        }
    }

    private boolean qrMask(int mask, int x, int y) {
        return switch (mask) {
            case 0 -> (x + y) % 2 == 0;
            default -> false;
        };
    }

    private void addQrFormatBits(boolean[][] modules, int mask) {
        int size = modules.length;
        int format = qrFormatBits(1, mask);
        for (int i = 0; i <= 5; i++) {
            modules[i][8] = ((format >>> i) & 1) != 0;
        }
        modules[7][8] = ((format >>> 6) & 1) != 0;
        modules[8][8] = ((format >>> 7) & 1) != 0;
        modules[8][7] = ((format >>> 8) & 1) != 0;
        for (int i = 9; i < 15; i++) {
            modules[14 - i][8] = ((format >>> i) & 1) != 0;
        }
        for (int i = 0; i < 8; i++) {
            modules[8][size - 1 - i] = ((format >>> i) & 1) != 0;
        }
        for (int i = 8; i < 15; i++) {
            modules[size - 15 + i][8] = ((format >>> i) & 1) != 0;
        }
    }

    private int qrFormatBits(int errorLevelBits, int mask) {
        int data = (errorLevelBits << 3) | mask;
        int value = data << 10;
        int generator = 0x537;
        for (int i = 14; i >= 10; i--) {
            if (((value >>> i) & 1) != 0) {
                value ^= generator << (i - 10);
            }
        }
        return ((data << 10) | value) ^ 0x5412;
    }

    private java.util.List<Integer> qrReedSolomonRemainder(java.util.List<Integer> data, int degree) {
        int[] generator = qrRsGenerator(degree);
        int[] remainder = new int[degree];
        for (int value : data) {
            int factor = value ^ remainder[0];
            System.arraycopy(remainder, 1, remainder, 0, degree - 1);
            remainder[degree - 1] = 0;
            for (int i = 0; i < degree; i++) {
                remainder[i] ^= qrGfMultiply(generator[i], factor);
            }
        }
        java.util.List<Integer> result = new java.util.ArrayList<>();
        for (int value : remainder) {
            result.add(value);
        }
        return result;
    }

    private int[] qrRsGenerator(int degree) {
        int[] generator = { 1 };
        for (int i = 0; i < degree; i++) {
            int[] next = new int[generator.length + 1];
            for (int j = 0; j < generator.length; j++) {
                next[j] ^= qrGfMultiply(generator[j], 1);
                next[j + 1] ^= qrGfMultiply(generator[j], qrGfPow(2, i));
            }
            generator = next;
        }
        return java.util.Arrays.copyOfRange(generator, 1, generator.length);
    }

    private int qrGfPow(int value, int power) {
        int result = 1;
        for (int i = 0; i < power; i++) {
            result = qrGfMultiply(result, value);
        }
        return result;
    }

    private int qrGfMultiply(int a, int b) {
        int result = 0;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) {
                result ^= a;
            }
            boolean carry = (a & 0x80) != 0;
            a = (a << 1) & 0xff;
            if (carry) {
                a ^= 0x1d;
            }
            b >>>= 1;
        }
        return result;
    }

    private String pujaBookingDetails(AppDataStore.PujaBookingRecord booking) {
        return "Booking ID: " + booking.bookingId
                + "\nPuja: " + booking.serviceName
                + "\nProvider: " + valueOr("Best available verified priest", booking.providerName)
                + "\nDate: " + booking.date
                + "\nTime: " + booking.time
                + "\nLocation: " + valueOr(booking.location, booking.locationName)
                + "\nDevotees: " + booking.devoteesCount
                + "\nLanguage: " + booking.language
                + "\nMode: " + booking.mode
                + "\nSamagri: " + (booking.samagriSelected ? "Included - Rs. " + booking.samagriAmount : "Not included")
                + "\nPrasad: " + (booking.prasadSelected ? "Included - Rs. " + booking.prasadAmount : "Not included")
                + "\nBase Price: Rs. " + booking.basePrice
                + "\nService Fee: Rs. " + booking.serviceFee
                + "\nTotal Amount: Rs. " + booking.totalAmount
                + "\nSpecial Requirements: " + valueOr("None", booking.specialRequirements)
                + "\nBooking Status: " + booking.bookingStatus
                + "\nPayment Status: " + booking.paymentStatus
                + "\nPayment Method: " + valueOr("Not started", booking.paymentMethod)
                + "\nRazorpay Order ID: " + valueOr("Not available", booking.razorpayOrderId)
                + "\nRazorpay Payment ID: " + valueOr("Not available", booking.razorpayPaymentId)
                + "\nPayment Failure Reason: " + valueOr("None", booking.paymentFailureReason)
                + "\n\nQR Code: Coming in a later part.";
    }

    private void updateOwnPujaBooking(AppDataStore.PujaBookingRecord booking, String status) {
        if (("CANCELLED".equals(status))
                && !("PENDING".equalsIgnoreCase(booking.bookingStatus)
                        || "CONFIRMED".equalsIgnoreCase(booking.bookingStatus))) {
            showInfo("Cannot Cancel", "Only Pending or Confirmed Puja bookings can be cancelled from user side.");
            return;
        }
        try {
            AppDataStore.updatePujaBookingStatus(booking.bookingId, status);
            showInfo("Puja Booking Updated", "Booking status updated to " + status + ".");
            showModulePage("bookings");
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Booking Update Failed", exception.getMessage());
        }
    }

    private void submitPujaFraudReport(AppDataStore.PujaBookingRecord booking) {
        AppSession.User user = AppSession.currentUser();
        try {
            AppDataStore.submitFraudReport(new AppDataStore.FraudReportRecord("",
                    user == null ? "" : user.uid(),
                    user == null ? "" : displayName(user),
                    booking.bookingId,
                    booking.serviceId,
                    booking.serviceName,
                    booking.providerId,
                    booking.providerName,
                    "User reported possible fraud from My Puja Bookings.",
                    "OPEN",
                    "",
                    ""));
            showInfo("Fraud Report Submitted", "Your report was saved and is visible to admin for review.");
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Fraud Report Failed", exception.getMessage());
        }
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

    private VBox infoPanel(String title, Node... body) {
        VBox panel = new VBox(7, sectionTitle(title));
        panel.getChildren().addAll(body);
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
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        try {
            String source = path == null || path.isBlank() ? "/images/trimbakeshwar.jpg" : path;
            Image image = null;
            if (source.startsWith("http") || source.startsWith("file:")) {
                image = new Image(source, true);
            } else {
                URL imageUrl = getClass().getResource(source);
                if (imageUrl != null) {
                    String key = imageUrl.toExternalForm();
                    image = IMAGE_CACHE.computeIfAbsent(key, Image::new);
                }
            }
            if (image == null) {
                return imageView;
            }
            imageView.setImage(image);
            if (image.getWidth() > 0 && image.getHeight() > 0) {
                double scale = Math.max(width / image.getWidth(), height / image.getHeight());
                double cropWidth = Math.min(image.getWidth(), width / scale);
                double cropHeight = Math.min(image.getHeight(), height / scale);
                double x = Math.max(0, (image.getWidth() - cropWidth) * xBias);
                double y = Math.max(0, (image.getHeight() - cropHeight) * yBias);
                imageView.setViewport(new Rectangle2D(x, y, cropWidth, cropHeight));
            }
        } catch (Exception ignored) {
            // Decorative images should never break the dashboard.
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
