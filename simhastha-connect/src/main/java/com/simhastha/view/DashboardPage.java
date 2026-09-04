package com.simhastha.view;

import com.simhastha.controller.BusinessMarketplaceController;
import com.simhastha.model.PublicBusinessItem;
import com.simhastha.model.PublicBusinessListing;
import com.simhastha.util.AppSession;
import com.simhastha.util.NavigationUtil;

import com.simhastha.config.CloudinaryFolders;
import com.simhastha.gateway.firebase.FirebaseConfig;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.service.CloudinaryService;
import com.simhastha.service.AuthService;
import com.simhastha.model.CloudinaryUploadResult;
import com.simhastha.model.EmergencyAlert;
import com.simhastha.model.EmergencyDevelopmentServices;
import com.simhastha.model.EmergencyReport;
import com.simhastha.model.EmergencyService;
import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
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
import com.simhastha.schedule.NashikLocationRegistry;
import com.simhastha.schedule.ScheduleAlert;
import com.simhastha.schedule.ScheduleCategory;
import com.simhastha.schedule.SchedulePeriod;
import com.simhastha.schedule.ScheduleService;
import com.simhastha.schedule.ScheduleStatus;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;

import javafx.application.Platform;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class DashboardPage {

    private static final Logger LOGGER = Logger.getLogger(DashboardPage.class.getName());
    private static final double GHAT_CARD_WIDTH = 200;
    private static final double GHAT_CARD_IMAGE_HEIGHT = 122;
    private BorderPane root;
    private final AppPaymentCoordinator paymentCoordinator = new AppPaymentCoordinator();
    private final BusinessMarketplaceController businessMarketplaceController = new BusinessMarketplaceController();
    private final java.util.Map<String, Button> navButtons = new java.util.LinkedHashMap<>();
    private LocationOption selectedFromLocation;
    private LocationOption selectedToLocation;
    private LocationOption selectedTrainFrom;
    private LocationOption selectedTrainTo;
    private AirportOption selectedFlightFrom;
    private AirportOption selectedFlightTo;
    private LocationOption selectedLocalFrom;
    private LocationOption selectedLocalTo;
    private java.time.LocalDate selectedLocalDate;
    private static final java.util.Map<String, Place> TRANSPORT_PLACES = createTransportPlaces();
    private List<PublicBusinessListing> marketplaceBusinesses = List.of();
    private FlowPane marketplaceGrid;
    private Label marketplaceStatus;
    private TextField marketplaceSearch;
    private HBox marketplaceStatsStrip;
    private String selectedMarketplaceCategory = "All";
    private static final java.util.List<LocationOption> INDIA_LOCATION_OPTIONS = loadIndiaLocationOptions();
    private static final java.util.List<AirportOption> INDIA_AIRPORT_OPTIONS = loadIndiaAirportOptions();
    private static final java.util.List<LocationOption> NASHIK_LOCAL_OPTIONS = loadNashikLocalOptions();
    private static final int LOCATION_SEARCH_LIMIT = 30;
    private static final java.util.concurrent.ExecutorService LOCATION_SEARCH_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "simhastha-location-search");
        thread.setDaemon(true);
        return thread;
    });
    private static final java.util.concurrent.ExecutorService SCHEDULE_REFRESH_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "simhastha-schedule-refresh");
        thread.setDaemon(true);
        return thread;
    });

    final FirestoreGateway firestoreGateway = new FirestoreGateway(FirebaseConfig.load());
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
    private final ScheduleService scheduleService = new ScheduleService();
    private final Map<String, com.simhastha.schedule.ScheduleEvent> savedScheduleEvents = new LinkedHashMap<>();
    private List<Ghat> currentGhats = List.of();
    private long ghatRefreshGeneration;
    private java.time.LocalDate selectedScheduleDate = java.time.LocalDate.now();
    private ScheduleCategory selectedScheduleCategory;
    private List<com.simhastha.schedule.ScheduleEvent> loadedScheduleEvents = List.of();
    private List<ScheduleAlert> loadedScheduleAlerts = List.of();
    private final java.util.Set<String> readNotifications = new java.util.HashSet<>();
    private VBox scheduleDateArea;
    private VBox scheduleFeatureArea;
    private VBox scheduleFilterArea;
    private VBox scheduleTimelineArea;
    private Timeline scheduleRefreshTimeline;
    private boolean schedulePageActive;
    private long scheduleRefreshGeneration;
    private boolean scheduleRefreshInFlight;
    private final Runnable scheduleLocalChangeListener = () -> Platform.runLater(() -> {
        if (schedulePageActive) {
            refreshSchedule();
        }
    });
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
    private Image profilePhoto;
    private String profilePhotoUrl;
    private String profilePhotoPublicId;
    private Rectangle2D profilePhotoViewport;
    private String editedProfileName;
    private String editedProfileEmail;
    private String editedProfileMobile = "+91 98765 43210";
    private String editedProfileAddress = "Nashik, Maharashtra, India";
    private String editedProfileBirthDate = "15 Aug 1995";
    private String editedProfileGender = "Male";
    private String profileSuccessMessage;
    private final FirestoreGateway emergencyFirestore = new FirestoreGateway(FirebaseConfig.load());
    private List<EmergencyServiceDemo> emergencyServices = List.of();
    private boolean emergencyServicesLoading;
    private String emergencyServicesError = "";
    private boolean usingEmergencyDevelopmentFallback;
    private EmergencyAlert activeEmergencyAlert;
    private boolean emergencyAlertsLoading;
    private String emergencyAlertsError = "";
    private boolean usingEmergencyAlertPreview;
    private String selectedEmergencyCategory = "All";
    private String selectedEmergencyFilter = "All";
    private EmergencyServiceDemo selectedEmergencyFacility;
    private EmergencyReport currentEmergency;
    private EmergencyDemoRequest currentDemoEmergency;
    private boolean emergencySubmissionInProgress;
    private boolean emergencyStateLoading;
    private boolean emergencyPageActive;
    private Timeline emergencyStatusRefresh;
    private boolean emergencyReportFormVisible;
    private boolean emergencyDetailsExpanded;
    private boolean sosConfirmationVisible;
    private String emergencyFeedback = "";
    private String emergencyMapMessage = "";
    private static final double EMERGENCY_DEMO_USER_LATITUDE = 20.0105;
    private static final double EMERGENCY_DEMO_USER_LONGITUDE = 73.7925;
    private EmergencyMapView emergencyMapView;
    private String emergencyMapFocusId = "";
    private boolean emergencyRouteActive;

    public Scene createScene(Stage stage) {
        hydrateProfilePhotoFromSession();
        root = new BorderPane();
        root.getStyleClass().add("pilgrim-dashboard-root");
        root.setLeft(createSidebar(stage));
        showHomePage();

        Scene scene = new Scene(root, 1200, 680);
        addTheme(scene);
        return scene;
    }

    private void hydrateProfilePhotoFromSession() {
        AppSession.User user = AppSession.currentUser();
        if (user != null && user.profilePhotoUrl() != null && !user.profilePhotoUrl().isBlank()) {
            profilePhotoUrl = user.profilePhotoUrl();
            profilePhotoPublicId = user.profilePhotoPublicId();
            profilePhoto = ImageMediaHelper.loadImage(profilePhotoUrl, 260, 260);
            profilePhotoViewport = null;
        }
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
                nav("faq", "FAQs / Help Center", false),
                nav("about", "About Us", false));

        Button logout = sidebarAction("logout", "Logout");
        logout.setOnAction(eventAction -> {
            AppSession.clear();
            NavigationUtil.navigate(stage, new UserAuthPage().createScene(stage));
        });

        VBox sidebar = new VBox(12, brand, menu, createSpacer(), supportPanel(), logout);
        sidebar.getStyleClass().add("pilgrim-sidebar");
        sidebar.setPadding(new Insets(15, 13, 14, 13));
        sidebar.setPrefWidth(238);
        sidebar.setMinWidth(238);
        sidebar.setMaxWidth(238);
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

    private VBox supportPanel() {
        Label title = label("24/7 Support", "pilgrim-support-title");
        title.setGraphic(moduleIcon("emergency", "pilgrim-support-icon"));
        Label detail = label("Emergency help and official information", "pilgrim-support-detail");
        VBox panel = new VBox(7, title, detail);
        panel.getStyleClass().add("pilgrim-support-box");
        return panel;
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
            case "faq" -> "\uE9CE";
            case "about" -> "\uE946";
            case "logout" -> "\uE7E8";
            default -> "\uE8A5";
        };
        return AppUi.symbolIcon(glyph, styleClass);
    }

    private void showHomePage() {
        stopGhatRefresh();
        stopScheduleRefresh();
        emergencyPageActive = false;
        stopEmergencyRefresh();
        setActiveModule("home");
        root.setCenter(scroll(createHomePage()));
    }

    private void showModulePage(String module) {
    if (!"ghat".equals(module)) {
        stopGhatRefresh();
    }

    if (!"schedule".equals(module)) {
        stopScheduleRefresh();
    }
        emergencyPageActive = "emergency".equals(module);
        if (emergencyPageActive) {
            startEmergencyRefresh();
        } else {
            stopEmergencyRefresh();
        }
        setActiveModule(module);
        if ("stay".equals(module)) {
            root.setCenter(scroll(stayPage()));
            return;
        }
        Node page = switch (module) {
            case "packages" -> packagesPage();
            case "transport" -> transportPage();
            case "puja" -> pujaPage();
            case "ghat" -> ghatsPage();
            case "emergency" -> {
                loadCurrentEmergencyAsync();
                loadEmergencyServicesAsync();
                loadEmergencyAlertsAsync();
                yield emergencyPage();
            }
            case "lost" -> lostFoundPage();
            case "schedule" -> schedulePage();
            case "business" -> businessPage();
            case "bookings" -> myBookingsPage();
            case "announcement" -> announcementPage();
            case "faq" -> faqPage();
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

    private Node scroll(Node content) {
        if (content instanceof VBox box && box.getStyleClass().contains("pilgrim-dashboard-main")) {
            return dashboardFrame(box);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("pilgrim-dashboard-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox dashboardFrame(VBox content) {
        VBox pinned = new VBox(10);
        while (!content.getChildren().isEmpty() && isPinnedDashboardNode(content.getChildren().get(0))) {
            pinned.getChildren().add(content.getChildren().remove(0));
        }
        if (pinned.getChildren().isEmpty()) {
            ScrollPane bodyOnly = plainDashboardScroll(content);
            VBox frame = new VBox(bodyOnly);
            VBox.setVgrow(bodyOnly, Priority.ALWAYS);
            return frame;
        }

        pinned.getStyleClass().addAll("pilgrim-dashboard-main", "pilgrim-sticky-header");
        pinned.setPadding(new Insets(12, 22, 8, 12));
        content.setPadding(new Insets(8, 22, 28, 12));

        ScrollPane body = plainDashboardScroll(content);
        VBox frame = new VBox(pinned, body);
        frame.getStyleClass().add("pilgrim-dashboard-frame");
        VBox.setVgrow(body, Priority.ALWAYS);
        return frame;
    }

    private ScrollPane plainDashboardScroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("pilgrim-dashboard-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private boolean isPinnedDashboardNode(Node node) {
        return node.getStyleClass().contains("pilgrim-top-actions")
                || node.getStyleClass().contains("pilgrim-hero")
                || node.getStyleClass().contains("ghat-hero")
                || node.getStyleClass().contains("bus-transport-hero")
                || node.getStyleClass().contains("outside-journey-hero");
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
        services.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        services.setAlignment(Pos.CENTER_LEFT);
        services.getStyleClass().add("pilgrim-service-grid");

        HBox lower = new HBox(14, facilityOptionsPanel(), quickAccessPanel());
        HBox.setHgrow(lower.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lower.getChildren().get(1), Priority.SOMETIMES);

        HBox metrics = new HBox(12,
                metric(remoteItemCountLabel("packages", "transport", "puja", "ghat", "emergency", "stay", "business"), "Active Services"),
                metric(remoteItemCountLabel("emergency"), "Open Help Points"),
                metric(remoteItemCountLabel("stay"), "Available Stays"),
                metric("24/7", "Support"));
        metrics.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));

        // TEMPORARY RAZORPAY API TEST
        // Remove after module payment integration is verified.
        Button paymentTest = new Button("Test Razorpay Payment");
        paymentTest.getStyleClass().add("developer-payment-test-button");
        paymentTest.setOnAction(event -> paymentCoordinator.startDeveloperRazorpayTest(
                root.getScene() == null ? null : root.getScene().getWindow()));
        HBox developerTest = new HBox(paymentTest);
        developerTest.setAlignment(Pos.CENTER_RIGHT);
        developerTest.getStyleClass().add("developer-payment-test-row");

        return pageShell("Dashboard", "Official Nashik Simhastha 2027 control and information platform",
                searchBar(), announcementTicker(), services, simhasthaMapCard(), lower, metrics, developerTest);
    }

    private HBox topControls() {
        Label title = new Label("Pilgrim Dashboard");
        title.getStyleClass().add("pilgrim-page-title");
        Label welcome = new Label("Welcome, " + currentUserDisplayName());
        welcome.getStyleClass().add("pilgrim-user-welcome");
        VBox titleBox = new VBox(2, title, welcome);

        HBox rightActions = new HBox(8, AppUi.createThemeToggle(), notificationBell(), profileButton());
        rightActions.getStyleClass().add("pilgrim-top-action-group");
        rightActions.setAlignment(Pos.CENTER_RIGHT);

        HBox actions = new HBox(12, titleBox, createSpacer(), rightActions);
        actions.getStyleClass().add("pilgrim-top-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private String currentUserDisplayName() {
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            return "Pilgrim";
        }
        return displayName(user);
    }

    private Button profileButton() {
        return AppUi.createProfileChip(currentUserDisplayName(), this::showProfilePage);
    }

    private void showProfilePage() {
        stopGhatRefresh();
        stopScheduleRefresh();
        setActiveModule("");
        root.setCenter(scroll(profilePage()));
    }

    private VBox profilePage() {
        AppSession.User user = AppSession.currentUser();
        java.util.List<AppDataStore.BookingRecord> bookings = user == null ? java.util.List.of()
                : AppDataStore.bookingsForUser(user.uid());
        long upcoming = bookings.stream().filter(booking -> bookingBucket(booking).equals("Upcoming")).count();
        long completed = bookings.stream().filter(booking -> bookingBucket(booking).equals("Completed")).count();

        HBox profileHero = new HBox(26, profileIdentityCard(), profileInfoCard());
        profileHero.getStyleClass().add("profile-hero-card");
        HBox.setHgrow(profileHero.getChildren().get(1), Priority.ALWAYS);

        HBox metrics = new HBox(12,
                profileMetric("My Bookings", String.valueOf(bookings.size()), "Total Bookings"),
                profileMetric("Upcoming Bookings", String.valueOf(upcoming), "Next 30 Days"),
                profileMetric("Completed", String.valueOf(completed), "Total Completed"));
        metrics.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));

        HBox accountRows = new HBox(14, accountSecurityCard(), supportHelpCard());
        accountRows.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));

        java.util.List<Node> sections = new java.util.ArrayList<>();
        if (profileSuccessMessage != null && !profileSuccessMessage.isBlank()) {
            sections.add(successBanner(profileSuccessMessage));
            profileSuccessMessage = null;
        }
        sections.add(profileHero);
        sections.add(metrics);
        sections.add(accountRows);

        return pageShell("Profile", "Dashboard  >  Profile", sections.toArray(new Node[0]));
    }

    private VBox profileIdentityCard() {
        VBox identity = new VBox(10, profilePhotoControl(), label(profileName(), "profile-user-name"),
                iconDetail("\uE717", profileMobile()), iconDetail("\uE715", profileEmail()),
                iconDetail("\uE707", profileAddress()), label("Verified User", "profile-verified-badge"));
        identity.getStyleClass().add("profile-identity-panel");
        identity.setAlignment(Pos.CENTER);
        return identity;
    }

    private StackPane profilePhotoControl() {
        StackPane avatar = profileAvatar(142, "profile-avatar-large");
        Button camera = new Button();
        camera.setGraphic(AppUi.symbolIcon("\uE722", "profile-camera-icon"));
        camera.getStyleClass().add("profile-camera-button");
        camera.setOnAction(event -> chooseProfilePhoto());
        StackPane wrapper = new StackPane(avatar, camera);
        StackPane.setAlignment(camera, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(camera, new Insets(0, 8, 8, 0));
        return wrapper;
    }

    private StackPane profileAvatar(double size, String styleClass) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add(styleClass);
        avatar.setMinSize(size, size);
        avatar.setPrefSize(size, size);
        avatar.setMaxSize(size, size);
        Circle clip = new Circle(size / 2, size / 2, size / 2);
        if (profilePhoto == null) {
            avatar.getChildren().add(AppUi.symbolIcon("\uE77B", "profile-avatar-placeholder"));
        } else {
            ImageView view = new ImageView(profilePhoto);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(false);
            if (profilePhotoViewport != null) {
                view.setViewport(profilePhotoViewport);
            }
            view.setClip(clip);
            avatar.getChildren().add(view);
        }
        return avatar;
    }

    private VBox profileInfoCard() {
        Button edit = new Button("Edit Profile");
        edit.setGraphic(AppUi.symbolIcon("\uE70F", "button-icon"));
        edit.getStyleClass().add("profile-edit-button");
        edit.setOnAction(event -> showEditProfileDialog());
        HBox header = new HBox(sectionTitle("Personal Information"), createSpacer(), edit);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(8, header,
                profileInfoRow("Full Name", profileName()),
                profileInfoRow("Date of Birth", profileBirthDate()),
                profileInfoRow("Gender", profileGender()),
                profileInfoRow("Mobile Number", profileMobile()),
                profileInfoRow("Email Address", profileEmail()),
                profileInfoRow("Address", profileAddress()),
                profileInfoRow("Registration Date", "21 Aug 2025, 10:30 AM"));
        card.getStyleClass().add("profile-info-panel");
        return card;
    }

    private HBox profileInfoRow(String labelText, String value) {
        Label label = muted(labelText);
        Label valueLabel = label(value, "profile-info-value");
        HBox row = new HBox(16, label, createSpacer(), valueLabel);
        row.getStyleClass().add("profile-info-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox profileMetric(String title, String value, String detail) {
        Button view = new Button("View All");
        view.getStyleClass().add("profile-text-action");
        view.setOnAction(event -> showModulePage("bookings"));
        VBox card = new VBox(5, label(title, "profile-metric-title"), label(value, "profile-metric-value"),
                muted(detail), view);
        card.getStyleClass().add("profile-metric-card");
        return card;
    }

    private VBox accountSecurityCard() {
        return profileActionPanel("Account & Security",
                profileActionRow("\uE72E", "Change Password", "Update your account password", "Change",
                        this::showChangePasswordPage),
                profileActionRow("\uE72E", "Privacy Settings", "Manage your privacy and data", "Manage",
                        this::showPrivacySettingsPage),
                profileActionRow("\uE7E8", "Logout", "Sign out from your account", "Logout", () -> {
                    AppSession.clear();
                    NavigationUtil.navigate((Stage) root.getScene().getWindow(), new UserAuthPage().createScene((Stage) root.getScene().getWindow()));
                }));
    }

    private VBox supportHelpCard() {
        return profileActionPanel("Support & Help",
                profileActionRow("\uE897", "Help Center", "Get help and support", ">", () -> showModulePage("about")),
                profileActionRow("\uE8F2", "Contact Support", "Talk to our support team", ">", () -> showModulePage("emergency")),
                profileActionRow("\uE9CE", "FAQs", "Frequently asked questions", ">", () -> showModulePage("faq")));
    }

    private VBox profileActionPanel(String title, Node... rows) {
        VBox panel = new VBox(6, sectionTitle(title));
        panel.getChildren().addAll(rows);
        panel.getStyleClass().add("profile-action-panel");
        return panel;
    }

    private HBox profileActionRow(String icon, String title, String detail, String action, Runnable handler) {
        Button button = new Button(action);
        button.getStyleClass().add("profile-row-action");
        button.setOnAction(event -> handler.run());
        VBox copy = new VBox(2, strong(title), muted(detail));
        HBox row = new HBox(10, AppUi.symbolIcon(icon, "profile-row-icon"), copy, createSpacer(), button);
        row.getStyleClass().add("profile-action-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox iconDetail(String icon, String detail) {
        HBox row = new HBox(6, AppUi.symbolIcon(icon, "profile-detail-icon"), muted(detail));
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private HBox successBanner(String message) {
        HBox banner = new HBox(8, AppUi.symbolIcon("\uE73E", "profile-success-icon"),
                label(message, "profile-success-text"));
        banner.getStyleClass().add("profile-success-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        return banner;
    }

    private void chooseProfilePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = chooser.showOpenDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Image selected = new Image(file.toURI().toString(), false);
        if (selected.isError() || selected.getWidth() <= 0 || selected.getHeight() <= 0) {
            showInfo("Profile Photo", "The selected image could not be opened.");
            return;
        }
        showCropPhotoDialog(selected, file);
    }

    private void showCropPhotoDialog(Image selected, File file) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Crop Photo");
        ButtonType confirm = new ButtonType("Confirm");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, confirm);

        ImageView preview = new ImageView(selected);
        preview.setFitWidth(260);
        preview.setFitHeight(260);
        preview.setPreserveRatio(false);
        Rectangle previewClip = new Rectangle(260, 260);
        previewClip.setArcWidth(10);
        previewClip.setArcHeight(10);
        preview.setClip(previewClip);

        Slider zoom = new Slider(1, 2.4, 1.15);
        zoom.setShowTickMarks(true);
        zoom.setBlockIncrement(0.1);
        Runnable updateViewport = () -> preview.setViewport(centerCropViewport(selected, zoom.getValue()));
        zoom.valueProperty().addListener((observable, oldValue, newValue) -> updateViewport.run());
        updateViewport.run();

        VBox content = new VBox(12, label("Crop & Confirm", "profile-dialog-title"), new StackPane(preview),
                muted("Adjust zoom, then confirm your profile photo."), zoom);
        content.getStyleClass().add("profile-crop-dialog");
        dialog.getDialogPane().setContent(content);
        styleProfileDialog(dialog, confirm);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == confirm) {
            AppSession.User user = AppSession.currentUser();
            if (user == null) {
                showInfo("Profile Photo", "Please login again before updating your profile photo.");
                return;
            }
            String oldPublicId = profilePhotoPublicId;
            double selectedZoom = zoom.getValue();
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    CloudinaryService cloudinaryService = new CloudinaryService();
                    CloudinaryUploadResult uploadResult = cloudinaryService.uploadImage(file, CloudinaryFolders.USER_PROFILE);
                    firestoreGateway.updateUserProfilePhoto(user.uid(), uploadResult.getSecureUrl(),
                            uploadResult.getPublicId(), user.idToken());
                    if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(uploadResult.getPublicId())) {
                        try {
                            cloudinaryService.deleteImage(oldPublicId);
                        } catch (IOException cleanupFailure) {
                            LOGGER.fine("Old profile image cleanup failed after successful save.");
                        }
                    }
                    return uploadResult;
                } catch (IOException | InterruptedException exception) {
                    throw new java.util.concurrent.CompletionException(exception);
                }
            }).whenComplete((uploadResult, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showInfo("Profile Photo", "The selected profile photo could not be uploaded. Your current photo was kept.");
                    return;
                }
                profilePhotoUrl = uploadResult.getSecureUrl();
                profilePhotoPublicId = uploadResult.getPublicId();
                profilePhoto = selected;
                profilePhotoViewport = centerCropViewport(selected, selectedZoom);
                AppSession.set(new AppSession.User(user.uid(), user.email(), user.role(), user.idToken(),
                        user.displayName(), user.status(), profilePhotoUrl, profilePhotoPublicId));
                profileSuccessMessage = "Profile photo updated successfully.";
                showProfilePage();
            }));
        }
    }

    private Rectangle2D centerCropViewport(Image image, double zoom) {
        double size = Math.min(image.getWidth(), image.getHeight()) / Math.max(1, zoom);
        double x = Math.max(0, (image.getWidth() - size) / 2.0);
        double y = Math.max(0, (image.getHeight() - size) / 2.0);
        return new Rectangle2D(x, y, size, size);
    }

    private void showEditProfileDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        ButtonType save = new ButtonType("Save Changes");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        TextField name = AppUi.textField("Full name");
        name.setText(profileName());
        TextField birthDate = AppUi.textField("Date of birth");
        birthDate.setText(profileBirthDate());
        ComboBox<String> gender = profileCombo("Gender", "Male", "Female", "Other", "Prefer not to say");
        gender.setValue(profileGender());
        TextField mobile = AppUi.textField("Mobile number");
        mobile.setText(profileMobile());
        TextField email = AppUi.textField("Email address");
        email.setText(profileEmail());
        TextField address = AppUi.textField("Address");
        address.setText(profileAddress());
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.add(profileField("Full Name", name), 0, 0);
        formGrid.add(profileField("Date of Birth", birthDate), 1, 0);
        formGrid.add(profileField("Gender", gender), 0, 1);
        formGrid.add(profileField("Mobile Number", mobile), 1, 1);
        formGrid.add(profileField("Email Address", email), 0, 2);
        formGrid.add(profileField("Address", address), 1, 2);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        formGrid.getColumnConstraints().addAll(left, right);
        VBox form = new VBox(12, label("Edit Profile", "profile-dialog-title"),
                muted("Update the details visible across your dashboard and admin records."), formGrid);
        form.getStyleClass().add("profile-edit-dialog");
        dialog.getDialogPane().setContent(form);
        styleProfileDialog(dialog, save);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == save) {
            if (name.getText().trim().isBlank() || mobile.getText().trim().isBlank()
                    || email.getText().trim().isBlank()) {
                showInfo("Profile", "Please enter your name, mobile number and email address.");
                return;
            }
            saveProfileDetails(name.getText().trim(), birthDate.getText().trim(), gender.getValue(),
                    mobile.getText().trim(), email.getText().trim(), address.getText().trim());
        }
    }

    private VBox profileField(String title, Node field) {
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        VBox box = new VBox(5, muted(title), field);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private ComboBox<String> profileCombo(String prompt, String... values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setPromptText(prompt);
        combo.getItems().addAll(values);
        combo.getStyleClass().add("input-combo");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private void saveProfileDetails(String name, String birthDate, String gender, String mobile, String email,
            String address) {
        editedProfileName = name;
        editedProfileBirthDate = birthDate.isBlank() ? "15 Aug 1995" : birthDate;
        editedProfileGender = gender == null || gender.isBlank() ? "Prefer not to say" : gender;
        editedProfileMobile = mobile;
        editedProfileEmail = email;
        editedProfileAddress = address.isBlank() ? "Nashik, Maharashtra, India" : address;

        AppSession.User user = AppSession.currentUser();
        if (user != null) {
            AppSession.User updated = new AppSession.User(user.uid(), editedProfileEmail, user.role(), user.idToken(),
                    editedProfileName, user.status(), profilePhotoUrl, profilePhotoPublicId);
            AppSession.set(updated);
            AppDataStore.updateLocalUserProfile(user.uid(), editedProfileName, editedProfileEmail, editedProfileMobile);
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    firestoreGateway.updateUserProfileDetails(user.uid(), editedProfileName, editedProfileEmail,
                            editedProfileMobile, editedProfileBirthDate, editedProfileGender, editedProfileAddress,
                            user.idToken());
                } catch (IOException | InterruptedException exception) {
                    throw new java.util.concurrent.CompletionException(exception);
                }
            }).whenComplete((ignored, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showInfo("Profile", "Details updated on this device. Firestore sync failed, please try again.");
                }
            }));
        }
        profileSuccessMessage = "Profile details updated successfully.";
        showProfilePage();
    }

    private void showChangePasswordPage() {
        setActiveModule("");
        PasswordField current = AppUi.passwordField("Current password");
        PasswordField next = AppUi.passwordField("New password");
        PasswordField confirm = AppUi.passwordField("Confirm new password");
        Label strength = muted("Use at least 8 characters with uppercase, lowercase, number and symbol.");
        next.textProperty().addListener((observable, oldValue, value) ->
                strength.setText(passwordStrengthText(value)));
        Button save = new Button("Update Password");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            if (!next.getText().equals(confirm.getText())) {
                showInfo("Change Password", "New password and confirmation do not match.");
                return;
            }
            if (!isStrongPassword(next.getText())) {
                showInfo("Change Password", "Please choose a stronger password before saving.");
                return;
            }
            save.setDisable(true);
            AuthService.changePassword(current.getText(), next.getText()).whenComplete((outcome, error) ->
                    Platform.runLater(() -> {
                        save.setDisable(false);
                        if (error != null || outcome == null || !outcome.success()) {
                            showInfo("Change Password", outcome == null ? "Password could not be changed."
                                    : outcome.message());
                            return;
                        }
                        profileSuccessMessage = "Password changed successfully.";
                        showProfilePage();
                    }));
        });
        VBox requirements = profileSecurityChecklist();
        VBox form = new VBox(12, sectionTitle("Secure Password Change"),
                muted("Verify your current password before setting a new one."),
                AppUi.passwordFieldWithToggle(current), AppUi.passwordFieldWithToggle(next),
                AppUi.passwordFieldWithToggle(confirm), strength, requirements, save);
        form.getStyleClass().add("profile-security-page");
        root.setCenter(scroll(pageShell("Change Password", "Profile  >  Account Security", form)));
    }

    private VBox profileSecurityChecklist() {
        return new VBox(7,
                securityItem("8+ characters"),
                securityItem("Uppercase and lowercase letters"),
                securityItem("At least one number"),
                securityItem("At least one symbol"));
    }

    private HBox securityItem(String text) {
        HBox row = new HBox(8, AppUi.symbolIcon("\uE73E", "profile-success-icon"), muted(text));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String passwordStrengthText(String value) {
        int score = passwordScore(value == null ? "" : value);
        if (score >= 4) {
            return "Password strength: Strong";
        }
        if (score >= 3) {
            return "Password strength: Good";
        }
        return "Password strength: Needs more protection";
    }

    private boolean isStrongPassword(String value) {
        return passwordScore(value == null ? "" : value) >= 4;
    }

    private int passwordScore(String value) {
        int score = value.length() >= 8 ? 1 : 0;
        if (value.matches(".*[A-Z].*")) score++;
        if (value.matches(".*[a-z].*")) score++;
        if (value.matches(".*[0-9].*")) score++;
        if (value.matches(".*[^A-Za-z0-9].*")) score++;
        return score;
    }

    private void showPrivacySettingsPage() {
        setActiveModule("");
        CheckBox showPhoto = new CheckBox("Show my profile photo on dashboard headers");
        showPhoto.setSelected(true);
        CheckBox bookingUpdates = new CheckBox("Allow booking and payment status notifications");
        bookingUpdates.setSelected(true);
        CheckBox safetyAlerts = new CheckBox("Allow emergency, crowd and route safety alerts");
        safetyAlerts.setSelected(true);
        CheckBox supportAccess = new CheckBox("Allow support team to view my support request history");
        Button save = new Button("Save Privacy Preferences");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            profileSuccessMessage = "Privacy preferences updated successfully.";
            showProfilePage();
        });
        VBox controls = new VBox(11, showPhoto, bookingUpdates, safetyAlerts, supportAccess);
        controls.getStyleClass().add("privacy-settings-list");
        VBox privacy = new VBox(14, sectionTitle("Privacy Settings"),
                muted("Control how Simhastha Connect uses your profile, alerts and support data."),
                profilePrivacyCard("\uE72E", "Account Protection",
                        "Your password and login session are handled through secure Firebase authentication."),
                profilePrivacyCard("\uE8BD", "Data Visibility",
                        "Only approved admins can review user records needed for safety and service support."),
                profilePrivacyCard(AppUi.notificationBellGlyph(), "Notifications",
                        "Important safety and booking updates remain enabled for pilgrim protection."),
                controls, save);
        privacy.getStyleClass().add("profile-security-page");
        root.setCenter(scroll(pageShell("Privacy Settings", "Profile  >  Privacy", privacy)));
    }

    private HBox profilePrivacyCard(String icon, String title, String detail) {
        VBox copy = new VBox(3, strong(title), muted(detail));
        HBox card = new HBox(10, AppUi.symbolIcon(icon, "profile-row-icon"), copy);
        card.getStyleClass().add("profile-privacy-card");
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    private String profileName() {
        return editedProfileName == null || editedProfileName.isBlank() ? currentUserDisplayName() : editedProfileName;
    }

    private String profileEmail() {
        AppSession.User user = AppSession.currentUser();
        if (editedProfileEmail != null && !editedProfileEmail.isBlank()) {
            return editedProfileEmail;
        }
        return user == null || user.email() == null || user.email().isBlank() ? "aniket.patil@example.com" : user.email();
    }

    private String profileMobile() {
        return editedProfileMobile;
    }

    private String profileAddress() {
        return editedProfileAddress;
    }

    private String profileBirthDate() {
        return editedProfileBirthDate;
    }

    private String profileGender() {
        return editedProfileGender;
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
        TextField search = AppUi.textField("Search services, places, events, businesses...");
        search.getStyleClass().add("pilgrim-search-field");
        Button button = new Button("Search");
        button.getStyleClass().add("pilgrim-search-button");
        HBox row = new HBox(0, search, button);
        row.getStyleClass().add("pilgrim-search-bar");
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(search, Priority.ALWAYS);
        return row;
    }

    private VBox serviceCard(String module, String title, String detail) {
        Label icon = moduleIcon(module, "pilgrim-card-icon");
        StackPane iconCircle = new StackPane(icon);
        iconCircle.getStyleClass().add("pilgrim-card-icon-circle");
        iconCircle.setMinSize(46, 46);
        iconCircle.setPrefSize(46, 46);
        iconCircle.setMaxSize(46, 46);
        Button action = new Button("emergency".equals(module) ? "SOS" : "View  >");
        action.getStyleClass().add("emergency".equals(module) ? "pilgrim-sos-action" : "pilgrim-small-action");
        action.setOnAction(event -> {
            event.consume();
            showModulePage(module);
        });
        VBox card = new VBox(8, iconCircle, strong(title), muted(detail), action);
        card.getStyleClass().addAll("pilgrim-module-card", "module-" + module);
        card.setMinSize(106, 128);
        card.setPrefWidth(142);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setAlignment(Pos.CENTER);
        card.setOnMouseClicked(event -> showModulePage(module));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private StackPane announcementTicker() {
        String updates = AnnouncementDemoStore.all().stream()
                .filter(item -> "LIVE".equals(item.status()) || "NEW".equals(item.status()) || "UPDATED".equals(item.status()))
                .map(AnnouncementDemoStore.Announcement::title)
                .collect(java.util.stream.Collectors.joining("  •  "));
        Label text = new Label(updates.isBlank() ? "No active official updates." : updates);
        text.getStyleClass().add("pilgrim-ticker-text");
        Label live = label("LIVE UPDATES", "announcement-ticker-label");
        live.setGraphic(moduleIcon("announcement", "announcement-ticker-icon"));
        Button viewAll = new Button("View All  >");
        viewAll.getStyleClass().add("pilgrim-ticker-action");
        viewAll.setOnAction(event -> {
            event.consume();
            showModulePage("announcement");
        });
        live.getStyleClass().add("announcement-ticker-label");
        StackPane viewport = new StackPane(text);
        viewport.getStyleClass().add("announcement-ticker-viewport");
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
        HBox content = new HBox(12, live, viewport, viewAll);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(viewport, Priority.ALWAYS);
        StackPane ticker = new StackPane(content);
        ticker.getStyleClass().add("pilgrim-alert-ticker");
        ticker.setMinHeight(30);
        ticker.setOnMouseClicked(event -> showModulePage("announcement"));
        javafx.application.Platform.runLater(() -> {
            double travel = Math.max(500, viewport.getWidth() + text.getWidth() + 42);
            TranslateTransition transition = new TranslateTransition(Duration.seconds(Math.max(14, travel / 70)), text);
            transition.setFromX(12);
            transition.setToX(-text.getWidth() - 30);
            transition.setCycleCount(Animation.INDEFINITE);
            transition.setInterpolator(Interpolator.LINEAR);
            transition.play();
        });
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
        Button viewAll = new Button("View All");
        viewAll.getStyleClass().add("pilgrim-text-link");
        viewAll.setOnAction(event -> showModulePage("announcement"));
        HBox heading = new HBox(10, sectionTitle("Live Updates"), createSpacer(), viewAll);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(12, heading, rows);
        panel.getStyleClass().add("pilgrim-panel");
        panel.setMinHeight(190);
        return panel;
    }

    private VBox facilityOptionsPanel() {
        HBox heading = new HBox(10,
                sectionTitle("Simhastha Facilities"),
                createSpacer(),
                facilityMapButton("View Full Map", null));
        heading.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(10,
                facilityOption("help", "Help Centre", "Official support desks"),
                facilityOption("hirkani", "Hirkani Room", "Mother and baby care"),
                facilityOption("toilet", "Toilets", "Public sanitation points"),
                facilityOption("parking", "Parking", "Vehicle parking areas"),
                facilityOption("hospital", "Hospital", "Medical help points"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));

        VBox panel = new VBox(12, heading, row);
        panel.getStyleClass().addAll("pilgrim-panel", "pilgrim-facility-panel");
        panel.setMinHeight(190);
        return panel;
    }

    private Button facilityMapButton(String text, String category) {
        Button button = new Button(text);
        button.getStyleClass().add("pilgrim-text-link");
        button.setOnAction(event -> openSimhasthaAreaMap(category));
        return button;
    }

    private VBox facilityOption(String category, String title, String detail) {
        StackPane iconShell = new StackPane(facilityVectorIcon(category, 30));
        iconShell.getStyleClass().addAll("pilgrim-facility-icon-shell", "facility-" + category);
        iconShell.setMinSize(58, 58);
        iconShell.setPrefSize(58, 58);
        iconShell.setMaxSize(58, 58);

        VBox card = new VBox(7, iconShell, strong(title), muted(detail), facilityMapButton("Show on Map  >", category));
        card.getStyleClass().add("pilgrim-facility-card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(112);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(event -> openSimhasthaAreaMap(category));
        return card;
    }

    private Pane facilityVectorIcon(String category, double size) {
        Pane icon = new Pane();
        icon.setMinSize(size, size);
        icon.setPrefSize(size, size);
        icon.setMaxSize(size, size);
        String stroke = "-fx-stroke: white; -fx-stroke-width: 2.4px; -fx-stroke-line-cap: round; -fx-fill: transparent;";
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

    private HBox simhasthaMapCard() {
        StackPane mapIcon = new StackPane(moduleIcon("ghat", "pilgrim-map-icon"));
        mapIcon.getStyleClass().add("pilgrim-map-icon-circle");
        mapIcon.setMinSize(52, 52);
        mapIcon.setPrefSize(52, 52);
        mapIcon.setMaxSize(52, 52);

        Button open = new Button("Open Map  >");
        open.getStyleClass().add("pilgrim-map-action");
        open.setOnAction(event -> {
            event.consume();
            openSimhasthaAreaMap(null);
        });

        VBox text = new VBox(8,
                mapIcon,
                label("Simhastha Map", "pilgrim-map-title"),
                muted("Explore Kumbh Area — Camps, Parking, Ghats, Temples & more"),
                open);
        text.setAlignment(Pos.CENTER_LEFT);
        text.setMinWidth(330);
        text.setMaxWidth(430);

        StackPane visual = simhasthaMapPreview();
        HBox.setHgrow(visual, Priority.ALWAYS);

        HBox card = new HBox(18, text, visual);
        card.getStyleClass().add("pilgrim-map-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setOnMouseClicked(event -> openSimhasthaAreaMap(null));
        return card;
    }

    private StackPane simhasthaMapPreview() {
        Pane sketch = new Pane();
        sketch.getStyleClass().add("pilgrim-map-sketch");
        sketch.setMinSize(560, 132);
        sketch.setPrefSize(560, 132);

        Line river = new Line(12, 96, 546, 36);
        river.getStyleClass().add("pilgrim-map-river");
        Line routeA = new Line(36, 34, 526, 108);
        routeA.getStyleClass().add("pilgrim-map-route");
        Line routeB = new Line(88, 118, 470, 18);
        routeB.getStyleClass().add("pilgrim-map-route-muted");
        sketch.getChildren().addAll(river, routeA, routeB);
        sketch.getChildren().addAll(
                miniMapPin(70, 62, "help", "Help"),
                miniMapPin(160, 28, "hospital", "Hospital"),
                miniMapPin(254, 76, "toilet", "Toilet"),
                miniMapPin(346, 40, "parking", "Parking"),
                miniMapPin(452, 66, "hirkani", "Hirkani"));

        Label caption = label("Nashik Kumbh service area map", "pilgrim-map-caption");
        StackPane.setAlignment(caption, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(caption, new Insets(0, 14, 12, 0));
        StackPane preview = new StackPane(sketch, caption);
        preview.getStyleClass().add("pilgrim-map-preview");
        preview.setMinHeight(132);
        preview.setMaxWidth(Double.MAX_VALUE);
        return preview;
    }

    private HBox miniMapPin(double x, double y, String category, String title) {
        StackPane marker = new StackPane(facilityVectorIcon(category, 18));
        marker.getStyleClass().addAll("pilgrim-map-mini-marker", "facility-" + category);
        marker.setMinSize(34, 34);
        marker.setPrefSize(34, 34);
        marker.setMaxSize(34, 34);
        Label label = new Label(title);
        label.getStyleClass().add("pilgrim-map-mini-label");
        HBox pin = new HBox(4, marker, label);
        pin.setAlignment(Pos.CENTER_LEFT);
        pin.setLayoutX(x);
        pin.setLayoutY(y);
        return pin;
    }

    private VBox quickAccessPanel() {
        VBox actions = new VBox(8,
                quickAccessButton("emergency", "Emergency Help"),
                quickAccessButton("bookings", "My Bookings"),
                quickAccessButton("lost", "Lost & Found"),
                quickAccessButton("announcement", "Announcements"));
        VBox panel = new VBox(12, sectionTitle("Quick Access"), actions);
        panel.getStyleClass().addAll("pilgrim-panel", "pilgrim-quick-access-panel");
        panel.setMinHeight(190);
        panel.setMinWidth(310);
        panel.setPrefWidth(360);
        return panel;
    }

    private Button quickAccessButton(String module, String text) {
        Button button = new Button(text);
        button.setGraphic(moduleIcon(module, "pilgrim-row-icon"));
        button.getStyleClass().add("pilgrim-quick-action");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> showModulePage(module));
        return button;
    }

    private String remoteItemCountLabel(String... modules) {
        int count = 0;
        boolean hasRemoteData = false;
        for (String module : modules) {
            if (AppDataStore.hasRemoteItems(module)) {
                hasRemoteData = true;
                count += AppDataStore.items(module).size();
            }
        }
        return hasRemoteData ? String.valueOf(count) : "—";
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
        content.setPadding(new Insets(12, 22, 28, 12));
        root.setCenter(scroll(content));
    }

    private HBox transportPageHeader(String titleText, String subtitleText) {
        VBox title = new VBox(2, label(titleText, "bus-page-title"), muted(subtitleText));
        HBox header = new HBox(10, title, createSpacer(), AppUi.createThemeToggle(),
                notificationBell(), roundButton("\uE77B", "Profile"));
        header.getStyleClass().add("pilgrim-top-actions");
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
        showTrainSearch(null);
    }

    private void showTrainSearch(JourneySearchState savedSearch) {
        if (savedSearch != null) {
            selectedTrainFrom = savedSearch.from();
            selectedTrainTo = savedSearch.to();
        }
        ObjectProperty<LocationOption> selectedTrainFromProperty = new SimpleObjectProperty<>(selectedTrainFrom);
        ObjectProperty<LocationOption> selectedTrainToProperty = new SimpleObjectProperty<>(selectedTrainTo);
        selectedTrainFromProperty.addListener((observable, oldLocation, newLocation) -> selectedTrainFrom = newLocation);
        selectedTrainToProperty.addListener((observable, oldLocation, newLocation) -> selectedTrainTo = newLocation);

        ComboBox<LocationOption> from = locationCombo("Search station or city", selectedTrainFromProperty);
        ComboBox<LocationOption> to = locationCombo("Search station or city", selectedTrainToProperty);
        DatePicker date = transportDatePicker();
        ComboBox<Integer> passengers = passengerCombo();
        if (selectedTrainFrom != null) {
            setSelectedLocation(from, selectedTrainFrom, selectedTrainFromProperty);
        }
        if (selectedTrainTo != null) {
            setSelectedLocation(to, selectedTrainTo, selectedTrainToProperty);
        }
        if (savedSearch != null) {
            date.setValue(savedSearch.date());
            passengers.setValue(savedSearch.passengers());
        }

        Button swap = new Button("\uE8AB");
        swap.getStyleClass().add("outside-swap-button");
        swap.setAccessibleText("Swap train From and To");
        swap.setOnAction(event -> {
            LocationOption oldFrom = selectedTrainFrom;
            LocationOption oldTo = selectedTrainTo;
            setSelectedLocation(from, oldTo, selectedTrainFromProperty);
            setSelectedLocation(to, oldFrom, selectedTrainToProperty);
        });

        Button search = new Button("SEARCH TRAINS");
        search.getStyleClass().add("transport-primary-button");
        search.setOnAction(event -> {
            LocationOption fromValue = selectedTrainFrom;
            LocationOption toValue = selectedTrainTo;
            if (!validSelectedJourney(fromValue, toValue, date.getValue(), passengers.getValue())) {
                return;
            }
            showAvailableTrainBookingOptions(new JourneySearchState(fromValue, toValue, date.getValue(), passengers.getValue()));
        });

        VBox form = trainJourneySearchForm(from, swap, to, date, passengers, search);
        form.getStyleClass().add("transport-train-form");
        root.setCenter(scroll(pageShell("Plan Your Train Journey",
                "Search your route and continue to trusted official train booking platforms.",
                transportBackButton("Back to Transport", this::showTransportLanding),
                trainJourneyHero(), form, trainTravelNotice())));
    }

    private VBox trainJourneySearchForm(ComboBox<LocationOption> from, Button swap, ComboBox<LocationOption> to,
            DatePicker date, ComboBox<Integer> passengers, Button search) {
        VBox fromBox = journeyInput("From Station / City", from, "\uE7C0");
        VBox toBox = journeyInput("To Station / City", to, "\uE7C0");
        VBox dateBox = journeyInput("Journey Date", date, "\uE787");
        VBox passengerBox = journeyInput("Passengers", passengers, "\uE716");

        HBox fields = new HBox(10, fromBox, swap, toBox, dateBox, passengerBox, search);
        fields.setAlignment(Pos.BOTTOM_LEFT);
        fields.getStyleClass().add("outside-journey-search-row");

        VBox form = new VBox(12, sectionTitle("Train Outside Traveller"), fields);
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

    private StackPane trainJourneyHero() {
        ImageView image = createImage("/images/transport-train-reference.png", 1100, 130, 0.5, 0.5);
        image.getStyleClass().add("outside-journey-hero-image");
        VBox text = new VBox(4, smallGold("OFFICIAL TRAIN BOOKING"), label("Plan Your Train Journey", "outside-journey-hero-title"),
                muted("Search your route and open trusted train booking platforms in the external browser."));
        text.setPadding(new Insets(18, 28, 18, 28));
        StackPane.setAlignment(text, Pos.CENTER_LEFT);
        StackPane hero = new StackPane(image, text);
        hero.getStyleClass().addAll("outside-journey-hero", "train-journey-hero");
        return hero;
    }

    private HBox trainTravelNotice() {
        HBox notice = new HBox(12,
                journeyBenefit("\uE8FD", "Official platforms", "Continue booking on IRCTC and trusted partners"),
                journeyBenefit("\uE787", "Date preserved", "Use the selected journey date during external search"),
                journeyBenefit("\uE72E", "No fake live data", "Availability is confirmed only on provider websites"));
        notice.getStyleClass().add("outside-journey-benefits");
        return notice;
    }

    private void showAvailableTrainBookingOptions(JourneySearchState search) {
        VBox optionCards = new VBox(12,
                trainBookingOptionCard("🚆", "IRCTC Official Train Booking",
                        "Official Indian Railways online ticketing portal.",
                        "https://www.irctc.co.in/nget/train-search", search),
                trainBookingOptionCard("🚆", "MakeMyTrip Rail",
                        "Trusted train search and booking partner.",
                        "https://www.makemytrip.com/railways/", search),
                trainBookingOptionCard("🚆", "ConfirmTkt",
                        "Train search, availability guidance and booking partner.",
                        "https://www.confirmtkt.com/", search),
                trainBookingOptionCard("🚆", "ixigo Trains",
                        "Train journey search and booking partner.",
                        "https://www.ixigo.com/trains", search));

        root.setCenter(scroll(pageShell("Available Train Booking Options",
                "Open official/trusted platforms and complete train booking externally.",
                transportBackButton("Back to Train Search", () -> showTrainSearch(search)),
                trainJourneySummaryCard(search), optionCards,
                muted("Simhastha Connect does not show fake live train availability and does not collect train payment details."))));
    }

    private VBox trainJourneySummaryCard(JourneySearchState search) {
        Label title = label("AVAILABLE TRAIN BOOKING OPTIONS", "available-route-title");
        Label route = label(search.from().displayName() + "  \u2192  " + search.to().displayName(), "available-bus-name");
        Label details = muted("Journey Date: " + formatJourneyDate(search.date()) + "  |  "
                + search.passengers() + (search.passengers() == 1 ? " Passenger" : " Passengers"));
        VBox summary = new VBox(7, smallGold("SELECTED TRAIN JOURNEY"), title, route, details);
        summary.getStyleClass().add("available-journey-summary");
        return summary;
    }

    private VBox trainBookingOptionCard(String icon, String title, String description, String url, JourneySearchState search) {
        Label optionIcon = new Label(icon);
        optionIcon.getStyleClass().add("transport-provider-icon");
        Label route = muted(search.from().name() + " → " + search.to().name());
        Label date = muted("Date: " + formatJourneyDate(search.date()) + "  |  Passengers: " + search.passengers());
        Button open = new Button("OPEN OFFICIAL WEBSITE");
        open.getStyleClass().add("transport-primary-button");
        open.setOnAction(event -> {
            openTravelWebsite(url);
            showInfo("External Train Booking",
                    "Please enter/search the selected journey details on the opened website:\n"
                            + search.from().displayName() + " → " + search.to().displayName()
                            + "\nDate: " + formatJourneyDate(search.date())
                            + "\nPassengers: " + search.passengers());
        });
        VBox copy = new VBox(5, strong(title), paragraph(description), route, date);
        HBox row = new HBox(12, optionIcon, copy, createSpacer(), open);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        VBox card = new VBox(row);
        card.getStyleClass().add("available-bus-card");
        return card;
    }

    private void showFlightSearch() {
        showFlightSearch(null);
    }

    private void showFlightSearch(FlightSearchState savedSearch) {
        if (savedSearch != null) {
            selectedFlightFrom = savedSearch.from();
            selectedFlightTo = savedSearch.to();
        }
        ObjectProperty<AirportOption> selectedFlightFromProperty = new SimpleObjectProperty<>(selectedFlightFrom);
        ObjectProperty<AirportOption> selectedFlightToProperty = new SimpleObjectProperty<>(selectedFlightTo);
        selectedFlightFromProperty.addListener((observable, oldAirport, newAirport) -> selectedFlightFrom = newAirport);
        selectedFlightToProperty.addListener((observable, oldAirport, newAirport) -> selectedFlightTo = newAirport);

        ComboBox<AirportOption> from = airportCombo("Search airport or city", selectedFlightFromProperty);
        ComboBox<AirportOption> to = airportCombo("Search airport or city", selectedFlightToProperty);
        DatePicker date = transportDatePicker();
        ComboBox<Integer> passengers = passengerCombo();
        if (selectedFlightFrom != null) {
            setSelectedAirport(from, selectedFlightFrom, selectedFlightFromProperty);
        }
        if (selectedFlightTo != null) {
            setSelectedAirport(to, selectedFlightTo, selectedFlightToProperty);
        }
        if (savedSearch != null) {
            date.setValue(savedSearch.date());
            passengers.setValue(savedSearch.passengers());
        }

        Button swap = new Button("\uE8AB");
        swap.getStyleClass().add("outside-swap-button");
        swap.setAccessibleText("Swap flight From and To airports");
        swap.setOnAction(event -> {
            AirportOption oldFrom = selectedFlightFrom;
            AirportOption oldTo = selectedFlightTo;
            setSelectedAirport(from, oldTo, selectedFlightFromProperty);
            setSelectedAirport(to, oldFrom, selectedFlightToProperty);
        });

        Button search = new Button("SEARCH FLIGHTS");
        search.getStyleClass().add("transport-primary-button");
        search.setOnAction(event -> {
            AirportOption fromValue = selectedFlightFrom;
            AirportOption toValue = selectedFlightTo;
            if (!validFlightJourney(fromValue, toValue, date.getValue(), passengers.getValue())) {
                return;
            }
            showAvailableFlightBookingOptions(new FlightSearchState(fromValue, toValue, date.getValue(), passengers.getValue()));
        });

        VBox form = flightJourneySearchForm(from, swap, to, date, passengers, search);
        form.getStyleClass().add("transport-flight-form");
        root.setCenter(scroll(pageShell("Plan Your Flight Journey",
                "Search your journey and continue to trusted flight booking platforms.",
                transportBackButton("Back to Transport", this::showTransportLanding),
                flightJourneyHero(), form, flightTravelNotice())));
    }

    private VBox flightJourneySearchForm(ComboBox<AirportOption> from, Button swap, ComboBox<AirportOption> to,
            DatePicker date, ComboBox<Integer> passengers, Button search) {
        VBox fromBox = journeyInput("From Airport / City", from, "\uE709");
        VBox toBox = journeyInput("To Airport / City", to, "\uE709");
        VBox dateBox = journeyInput("Journey Date", date, "\uE787");
        VBox passengerBox = journeyInput("Passengers", passengers, "\uE716");

        HBox fields = new HBox(10, fromBox, swap, toBox, dateBox, passengerBox, search);
        fields.setAlignment(Pos.BOTTOM_LEFT);
        fields.getStyleClass().add("outside-journey-search-row");

        VBox form = new VBox(12, sectionTitle("Flight Search"), fields);
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

    private StackPane flightJourneyHero() {
        ImageView image = createImage("/images/transport-flight-reference.png", 1100, 130, 0.5, 0.5);
        image.getStyleClass().add("outside-journey-hero-image");
        VBox text = new VBox(4, smallGold("TRUSTED FLIGHT BOOKING"), label("Plan Your Flight Journey", "outside-journey-hero-title"),
                muted("Select airports, choose a date and continue to trusted flight booking platforms."));
        text.setPadding(new Insets(18, 28, 18, 28));
        StackPane.setAlignment(text, Pos.CENTER_LEFT);
        StackPane hero = new StackPane(image, text);
        hero.getStyleClass().addAll("outside-journey-hero", "flight-journey-hero");
        return hero;
    }

    private HBox flightTravelNotice() {
        HBox notice = new HBox(12,
                journeyBenefit("\uE709", "Airport search", "Search by city, airport name or airport code"),
                journeyBenefit("\uE787", "Plan ahead", "Use your selected journey date on the opened website"),
                journeyBenefit("\uE8C7", "External payment", "Complete booking and payment only on trusted provider sites"));
        notice.getStyleClass().add("outside-journey-benefits");
        return notice;
    }

    private void showAvailableFlightBookingOptions(FlightSearchState search) {
        VBox optionCards = new VBox(12,
                flightBookingOptionCard("✈", "MakeMyTrip Flights",
                        "Trusted domestic and international flight booking platform.",
                        "https://www.makemytrip.com/flights/", search),
                flightBookingOptionCard("✈", "Yatra Flights",
                        "Search and book flight journeys on Yatra.",
                        "https://www.yatra.com/flights", search),
                flightBookingOptionCard("✈", "EaseMyTrip Flights",
                        "Flight booking platform for domestic and international routes.",
                        "https://www.easemytrip.com/flights.html", search),
                flightBookingOptionCard("✈", "Air India Official",
                        "Official Air India booking website.",
                        "https://www.airindia.com/", search),
                flightBookingOptionCard("✈", "IndiGo Official",
                        "Official IndiGo flight booking website.",
                        "https://www.goindigo.in/", search));

        root.setCenter(scroll(pageShell("Available Flight Booking Options",
                "Open official/trusted platforms and complete flight booking externally.",
                transportBackButton("Back to Flight Search", () -> showFlightSearch(search)),
                flightJourneySummaryCard(search), optionCards,
                muted("Simhastha Connect does not show fake live flight availability and does not collect flight payment details."))));
    }

    private VBox flightJourneySummaryCard(FlightSearchState search) {
        Label title = label("AVAILABLE FLIGHT BOOKING OPTIONS", "available-route-title");
        Label route = label(search.from().shortDisplay() + "  \u2192  " + search.to().shortDisplay(), "available-bus-name");
        Label details = muted("Journey Date: " + formatJourneyDate(search.date()) + "  |  "
                + search.passengers() + (search.passengers() == 1 ? " Passenger" : " Passengers"));
        VBox summary = new VBox(7, smallGold("SELECTED FLIGHT JOURNEY"), title, route, details);
        summary.getStyleClass().add("available-journey-summary");
        return summary;
    }

    private VBox flightBookingOptionCard(String icon, String title, String description, String url, FlightSearchState search) {
        Label optionIcon = new Label(icon);
        optionIcon.getStyleClass().add("transport-provider-icon");
        Label route = muted(search.from().city() + " (" + search.from().code() + ") → "
                + search.to().city() + " (" + search.to().code() + ")");
        Label date = muted("Date: " + formatJourneyDate(search.date()) + "  |  Passengers: " + search.passengers());
        Button open = new Button("OPEN WEBSITE");
        open.getStyleClass().add("transport-primary-button");
        open.setOnAction(event -> {
            openTravelWebsite(url);
            showInfo("External Flight Booking",
                    "Please enter/search the selected journey details on the opened website:\n"
                            + search.from().displayName() + " → " + search.to().displayName()
                            + "\nDate: " + formatJourneyDate(search.date())
                            + "\nPassengers: " + search.passengers());
        });
        VBox copy = new VBox(5, strong(title), paragraph(description), route, date);
        HBox row = new HBox(12, optionIcon, copy, createSpacer(), open);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        VBox card = new VBox(row);
        card.getStyleClass().add("available-bus-card");
        return card;
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

    private ComboBox<AirportOption> airportCombo(String prompt, ObjectProperty<AirportOption> selectedAirportProperty) {
        ComboBox<AirportOption> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText(prompt);
        combo.getEditor().setPromptText("Type airport, city or code");
        combo.setVisibleRowCount(8);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().addAll("journey-combo", "location-autocomplete", "airport-autocomplete");
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AirportOption airport) {
                return airport == null ? "" : airport.displayName();
            }

            @Override
            public AirportOption fromString(String text) {
                return null;
            }
        });
        Label noResults = new Label("No matching airport found");
        noResults.getStyleClass().add("city-search-empty");
        combo.setPlaceholder(noResults);
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AirportOption airport, boolean empty) {
                super.updateItem(airport, empty);
                if (empty || airport == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label icon = new Label("\uE709");
                icon.getStyleClass().add("location-result-pin");
                Label name = new Label(airport.airportName());
                name.getStyleClass().add("location-result-name");
                Label detail = new Label(airport.code() + " • " + airport.city() + ", " + airport.region());
                detail.getStyleClass().add("location-result-detail");
                HBox row = new HBox(8, icon, new VBox(1, name, detail));
                row.getStyleClass().add("location-search-result");
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AirportOption airport, boolean empty) {
                super.updateItem(airport, empty);
                setText(empty || airport == null ? "" : airport.displayName());
            }
        });
        combo.valueProperty().addListener((observable, oldAirport, newAirport) -> {
            if (Boolean.TRUE.equals(combo.getProperties().get("airportSelectionInProgress"))) {
                return;
            }
            if (newAirport != null) {
                selectedAirportProperty.set(newAirport);
                combo.getProperties().put("airportSelectionInProgress", true);
                try {
                    combo.getEditor().setText(newAirport.displayName());
                    combo.getEditor().positionCaret(combo.getEditor().getText().length());
                } finally {
                    combo.getProperties().remove("airportSelectionInProgress");
                    combo.getProperties().remove("airportUserTyping");
                }
                System.out.println("AIRPORT SELECTED: " + newAirport.displayName());
                javafx.application.Platform.runLater(combo::hide);
            }
        });

        java.util.function.Consumer<String> filter = text -> {
            String query = text == null ? "" : text.trim().toLowerCase(java.util.Locale.ROOT);
            int requestId = nextAirportRequestId(combo);
            LOCATION_SEARCH_EXECUTOR.execute(() -> {
                java.util.stream.Stream<AirportOption> airports = query.isBlank()
                        ? INDIA_AIRPORT_OPTIONS.stream().limit(12)
                        : INDIA_AIRPORT_OPTIONS.stream().filter(airport -> airport.matches(query));
                java.util.List<AirportOption> matches = airports.limit(LOCATION_SEARCH_LIMIT).toList();
                javafx.application.Platform.runLater(() -> {
                    if (currentAirportRequestId(combo) == requestId) {
                        setAirportItemsPreservingSelection(combo, matches, selectedAirportProperty,
                                combo.getEditor().getText());
                    }
                });
            });
        };

        combo.getEditor().addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {
            if (!Boolean.TRUE.equals(combo.getProperties().get("airportSelectionInProgress"))) {
                combo.getProperties().put("airportUserTyping", true);
            }
        });
        combo.getEditor().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case BACK_SPACE, DELETE -> {
                    if (!Boolean.TRUE.equals(combo.getProperties().get("airportSelectionInProgress"))) {
                        combo.getProperties().put("airportUserTyping", true);
                    }
                }
                default -> { }
            }
        });
        combo.getEditor().textProperty().addListener((observable, previous, text) -> {
            if (Boolean.TRUE.equals(combo.getProperties().get("airportSelectionInProgress"))) {
                return;
            }
            AirportOption selected = selectedAirportProperty.get();
            boolean userTyping = Boolean.TRUE.equals(combo.getProperties().get("airportUserTyping"));
            if (userTyping && selected != null && !selected.displayName().equals(text)) {
                selectedAirportProperty.set(null);
            }
            filter.accept(text);
            if (combo.isFocused() && !combo.isShowing()
                    && !Boolean.TRUE.equals(combo.getProperties().get("airportRefreshingForShow"))) {
                combo.show();
            }
        });
        combo.setOnShowing(event -> {
            combo.getProperties().put("airportRefreshingForShow", true);
            try {
                filter.accept(combo.getEditor().getText());
            } finally {
                combo.getProperties().remove("airportRefreshingForShow");
            }
        });
        combo.getEditor().setOnMouseClicked(event -> {
            filter.accept(combo.getEditor().getText());
            if (!combo.isShowing()) {
                combo.show();
            }
        });
        combo.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                AirportOption selected = selectedAirportProperty.get();
                if (selected != null) {
                    setSelectedAirport(combo, selected, selectedAirportProperty);
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

    private void setAirportItemsPreservingSelection(ComboBox<AirportOption> combo,
            java.util.List<AirportOption> matches, ObjectProperty<AirportOption> selectedAirportProperty,
            String editorText) {
        AirportOption selected = selectedAirportProperty.get();
        boolean editorStillShowsSelection = selected != null && selected.displayName().equals(editorText);
        boolean shouldRestoreSelection = selected != null && (editorStillShowsSelection || !combo.isFocused());
        java.util.List<AirportOption> items = new java.util.ArrayList<>(matches);
        if (shouldRestoreSelection && !items.contains(selected)) {
            items.add(0, selected);
            if (items.size() > LOCATION_SEARCH_LIMIT) {
                items = new java.util.ArrayList<>(items.subList(0, LOCATION_SEARCH_LIMIT));
            }
        }
        combo.getProperties().put("airportSelectionInProgress", true);
        try {
            combo.getItems().setAll(items);
            if (shouldRestoreSelection) {
                combo.setValue(selected);
                combo.getSelectionModel().select(selected);
                combo.getEditor().setText(selected.displayName());
                combo.getEditor().positionCaret(combo.getEditor().getText().length());
            }
        } finally {
            combo.getProperties().remove("airportSelectionInProgress");
            combo.getProperties().remove("airportUserTyping");
        }
    }

    private int nextAirportRequestId(ComboBox<AirportOption> combo) {
        Object requestId = combo.getProperties().getOrDefault("airportRequestId", 0);
        int next = ((Integer) requestId) + 1;
        combo.getProperties().put("airportRequestId", next);
        return next;
    }

    private int currentAirportRequestId(ComboBox<AirportOption> combo) {
        Object requestId = combo.getProperties().getOrDefault("airportRequestId", 0);
        return (Integer) requestId;
    }

    private void setSelectedAirport(ComboBox<AirportOption> combo, AirportOption airport,
            ObjectProperty<AirportOption> selectedAirportProperty) {
        combo.getProperties().put("airportSelectionInProgress", true);
        try {
            selectedAirportProperty.set(airport);
            if (airport == null) {
                combo.setValue(null);
                combo.getEditor().clear();
                return;
            }
            if (!combo.getItems().contains(airport)) {
                combo.getItems().setAll(INDIA_AIRPORT_OPTIONS.stream()
                        .filter(option -> option.matches(airport.code()) || option.equals(airport))
                        .limit(LOCATION_SEARCH_LIMIT)
                        .toList());
            }
            combo.getSelectionModel().select(airport);
            combo.setValue(airport);
            combo.getEditor().setText(airport.displayName());
            combo.getEditor().positionCaret(combo.getEditor().getText().length());
        } finally {
            combo.getProperties().remove("airportSelectionInProgress");
            combo.getProperties().remove("airportUserTyping");
        }
    }

    private boolean validFlightJourney(AirportOption from, AirportOption to, java.time.LocalDate date, Integer passengers) {
        if (from == null) {
            showInfo("Search details required", "Please select From Airport / City from the search results.");
            return false;
        }
        if (to == null) {
            showInfo("Search details required", "Please select To Airport / City from the search results.");
            return false;
        }
        if (from.displayName().equalsIgnoreCase(to.displayName())) {
            showInfo("Search details required", "From Airport / City and To Airport / City cannot be the same.");
            return false;
        }
        if (date == null || date.isBefore(java.time.LocalDate.now())) {
            showInfo("Search details required", "Please select a valid journey date.");
            return false;
        }
        if (passengers == null || passengers < 1) {
            showInfo("Search details required", "Please select a valid passenger count.");
            return false;
        }
        return true;
    }

    private String formatJourneyDate(java.time.LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM uuuu"));
    }

    private void showLocalTransport() {
        showLocalTransport(null);
    }

    private void showLocalTransport(LocalJourneyState savedSearch) {
        if (savedSearch != null) {
            selectedLocalFrom = savedSearch.from();
            selectedLocalTo = savedSearch.to();
            selectedLocalDate = savedSearch.date();
        }
        ObjectProperty<LocationOption> selectedLocalFromProperty = new SimpleObjectProperty<>(selectedLocalFrom);
        ObjectProperty<LocationOption> selectedLocalToProperty = new SimpleObjectProperty<>(selectedLocalTo);
        selectedLocalFromProperty.addListener((observable, oldLocation, newLocation) -> selectedLocalFrom = newLocation);
        selectedLocalToProperty.addListener((observable, oldLocation, newLocation) -> selectedLocalTo = newLocation);

        ComboBox<LocationOption> from = localLocationCombo("Search local location", selectedLocalFromProperty);
        ComboBox<LocationOption> to = localLocationCombo("Search local destination", selectedLocalToProperty);
        if (selectedLocalFrom != null) {
            setSelectedLocation(from, selectedLocalFrom, selectedLocalFromProperty);
        }
        if (selectedLocalTo != null) {
            setSelectedLocation(to, selectedLocalTo, selectedLocalToProperty);
        }
        DatePicker date = transportDatePicker();
        if (selectedLocalDate != null) {
            date.setValue(selectedLocalDate);
        }

        Button swap = new Button("\uE8AB");
        swap.getStyleClass().add("outside-swap-button");
        swap.setAccessibleText("Swap local From and To locations");
        swap.setOnAction(event -> {
            LocationOption oldFrom = selectedLocalFrom;
            LocationOption oldTo = selectedLocalTo;
            setSelectedLocation(from, oldTo, selectedLocalFromProperty);
            setSelectedLocation(to, oldFrom, selectedLocalToProperty);
        });

        Button search = new Button("SEARCH LOCAL TRANSPORT");
        search.getStyleClass().add("transport-primary-button");
        search.setOnAction(event -> {
            LocationOption fromValue = selectedLocalFrom;
            LocationOption toValue = selectedLocalTo;
            java.time.LocalDate dateValue = date.getValue();
            if (!validLocalJourney(fromValue, toValue, dateValue)) {
                return;
            }
            selectedLocalDate = dateValue;
            showAvailableLocalTransport(new LocalJourneyState(fromValue, toValue, dateValue));
        });

        VBox form = localJourneySearchForm(from, swap, to, date, search);
        root.setCenter(scroll(pageShell("Nashik Local Travel",
                "Find convenient transport options to travel within Nashik.",
                transportBackButton("Back to Bus Options", this::showBusOptions),
                localJourneyHero(), form, localTravelNotice())));
    }

    private VBox localJourneySearchForm(ComboBox<LocationOption> from, Button swap,
            ComboBox<LocationOption> to, DatePicker date, Button search) {
        VBox fromBox = journeyInput("From Location", from, "\uE707");
        VBox toBox = journeyInput("To Location", to, "\uE774");
        VBox dateBox = journeyInput("Travel Date", date, "\uE787");
        HBox fields = new HBox(10, fromBox, swap, toBox, dateBox, search);
        fields.setAlignment(Pos.BOTTOM_LEFT);
        fields.getStyleClass().add("outside-journey-search-row");

        VBox form = new VBox(12, sectionTitle("Inside Traveller / Nashik Local Transport"), fields);
        form.getStyleClass().addAll("journey-planner", "outside-journey-form", "local-journey-form");
        fromBox.setMinWidth(0);
        toBox.setMinWidth(0);
        dateBox.setMinWidth(0);
        fromBox.prefWidthProperty().bind(form.widthProperty().multiply(0.28));
        toBox.prefWidthProperty().bind(form.widthProperty().multiply(0.28));
        dateBox.prefWidthProperty().bind(form.widthProperty().multiply(0.18));
        search.prefWidthProperty().bind(form.widthProperty().multiply(0.16));
        search.setMaxWidth(Double.MAX_VALUE);
        return form;
    }

    private StackPane localJourneyHero() {
        ImageView image = createImage("/images/bus-local-reference.png", 1100, 130, 0.5, 0.5);
        image.getStyleClass().add("outside-journey-hero-image");
        VBox text = new VBox(4, smallGold("NASHIK LOCAL TRAVEL"), label("Find local transport around Nashik Kumbh", "outside-journey-hero-title"),
                muted("Search local landmarks, stations, ghats, temples and important pilgrim zones."));
        text.setPadding(new Insets(18, 28, 18, 28));
        StackPane.setAlignment(text, Pos.CENTER_LEFT);
        StackPane hero = new StackPane(image, text);
        hero.getStyleClass().addAll("outside-journey-hero", "local-journey-hero");
        return hero;
    }

    private HBox localTravelNotice() {
        HBox notice = new HBox(12,
                journeyBenefit("\uE707", "Nashik locations", "Stations, bus stands, ghats, temples and city areas"),
                journeyBenefit("\uE806", "Local options", "Bus, shuttle, cab, auto and walking route guidance"),
                journeyBenefit("\uE8A5", "Official sources", "Open maps or trusted information websites externally"));
        notice.getStyleClass().add("outside-journey-benefits");
        return notice;
    }

    private ComboBox<LocationOption> localLocationCombo(String prompt, ObjectProperty<LocationOption> selectedLocationProperty) {
        return locationCombo(prompt, selectedLocationProperty, NASHIK_LOCAL_OPTIONS,
                NASHIK_LOCAL_OPTIONS.stream().limit(12).toList(),
                "Type Nashik location", "No matching Nashik location found");
    }

    private boolean validLocalJourney(LocationOption from, LocationOption to, java.time.LocalDate date) {
        if (from == null) {
            showInfo("Search details required", "Please select From Location from the search results.");
            return false;
        }
        if (to == null) {
            showInfo("Search details required", "Please select To Location from the search results.");
            return false;
        }
        if (from.displayName().equalsIgnoreCase(to.displayName())) {
            showInfo("Search details required", "From Location and To Location cannot be the same.");
            return false;
        }
        if (date == null || date.isBefore(java.time.LocalDate.now())) {
            showInfo("Search details required", "Please select a valid travel date.");
            return false;
        }
        return true;
    }

    private void showAvailableLocalTransport(LocalJourneyState search) {
        VBox options = new VBox(12,
                localTransportOptionCard("🚌", "Local Bus",
                        "Travel between selected locations using Nashik local bus services.",
                        "VIEW ROUTE", () -> openLocalMap(search)),
                localTransportOptionCard("🚐", "Official Shuttle Service",
                        "Official shuttle transport guidance for authorized Simhastha locations.",
                        "VIEW DETAILS", () -> openTravelWebsite("https://citilinc.nmc.gov.in/")),
                localTransportOptionCard("🚖", "Cab / Taxi",
                        "Find a suitable cab option. Complete any booking only on trusted external platforms.",
                        "OPEN BOOKING WEBSITE", () -> openTravelWebsite("https://www.google.com/search?q=Nashik+cab+taxi+booking")),
                localTransportOptionCard("🛺", "Auto Rickshaw",
                        "Use local auto rickshaw stands and approved pickup/drop zones around crowd areas.",
                        "VIEW ON MAP", () -> openLocalMap(search)),
                localTransportOptionCard("🚶", "Walking Route",
                        "View walking/navigation guidance for nearby locations.",
                        "VIEW ON MAP", () -> openLocalMap(search)));

        root.setCenter(scroll(pageShell("Available Local Transport",
                "Choose a local transport option and open route details externally.",
                transportBackButton("Back to Local Travel Search", () -> showLocalTransport(search)),
                localJourneySummaryCard(search), options,
                localTravelInformation(search),
                muted("Live GPS/ETA is not shown unless an official live source is connected."))));
    }

    private VBox localJourneySummaryCard(LocalJourneyState search) {
        VBox summary = new VBox(7,
                smallGold("YOUR LOCAL JOURNEY"),
                label("📍 From: " + search.from().displayName(), "available-bus-name"),
                label("↓", "available-route-title"),
                label("🎯 To: " + search.to().displayName(), "available-bus-name"),
                muted("📅 Date: " + formatJourneyDate(search.date())));
        summary.getStyleClass().add("available-journey-summary");
        return summary;
    }

    private VBox localTransportOptionCard(String icon, String title, String description, String actionText, Runnable action) {
        Label optionIcon = new Label(icon);
        optionIcon.getStyleClass().add("transport-provider-icon");
        Button actionButton = new Button(actionText);
        actionButton.getStyleClass().add("transport-primary-button");
        actionButton.setOnAction(event -> action.run());
        VBox copy = new VBox(5, strong(title), paragraph(description), badge("Live tracking unavailable"));
        HBox row = new HBox(12, optionIcon, copy, createSpacer(), actionButton);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        VBox card = new VBox(row);
        card.getStyleClass().add("available-bus-card");
        return card;
    }

    private HBox localTravelInformation(LocalJourneyState search) {
        HBox info = new HBox(12,
                journeyBenefit("\uE707", "Journey Route", search.from().name() + " → " + search.to().name()),
                journeyBenefit("\uE8A5", "Navigation", "Use Google Maps for route and navigation guidance."),
                journeyBenefit("\uE7BA", "Important Notice",
                        "Routes may change due to crowd management, traffic restrictions or official instructions."));
        info.getStyleClass().add("outside-journey-benefits");
        return info;
    }

    private void openLocalMap(LocalJourneyState search) {
        String route = search.from().displayName() + " Nashik to " + search.to().displayName() + " Nashik";
        String encoded = java.net.URLEncoder.encode(route, java.nio.charset.StandardCharsets.UTF_8);
        openTravelWebsite("https://www.google.com/maps/search/?api=1&query=" + encoded);
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
        return locationCombo(prompt, selectedLocationProperty, INDIA_LOCATION_OPTIONS, suggestedJourneyLocations(),
                "Type city name", "No matching city found");
    }

    private ComboBox<LocationOption> locationCombo(String prompt, ObjectProperty<LocationOption> selectedLocationProperty,
            java.util.List<LocationOption> sourceOptions, java.util.List<LocationOption> suggestedOptions,
            String editorPrompt, String emptyMessage) {
        ComboBox<LocationOption> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText(prompt);
        combo.getEditor().setPromptText(editorPrompt);
        combo.setVisibleRowCount(8);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getProperties().put("locationOptions", sourceOptions);
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
        Label noResults = new Label(emptyMessage);
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
                    combo.getProperties().remove("locationUserTyping");
                }
                javafx.application.Platform.runLater(combo::hide);
            }
        });

        java.util.function.Consumer<String> filter = text -> {
            String query = text == null ? "" : text.trim().toLowerCase();
            int requestId = nextLocationRequestId(combo);
            LOCATION_SEARCH_EXECUTOR.execute(() -> {
                java.util.stream.Stream<LocationOption> locations = query.isBlank()
                        ? suggestedOptions.stream()
                        : sourceOptions.stream().filter(location -> location.matches(query));
                java.util.List<LocationOption> matches = locations.limit(LOCATION_SEARCH_LIMIT).toList();
                javafx.application.Platform.runLater(() -> {
                    if (currentLocationRequestId(combo) == requestId) {
                        setLocationItemsPreservingSelection(combo, matches, selectedLocationProperty,
                                combo.getEditor().getText());
                    }
                });
            });
        };
        combo.getEditor().addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event ->
                combo.getProperties().put("locationUserTyping", true));
        combo.getEditor().textProperty().addListener((observable, previous, text) -> {
            if (Boolean.TRUE.equals(combo.getProperties().get("locationSelectionInProgress"))) {
                return;
            }
            LocationOption selected = selectedLocationProperty.get();
            boolean userTyping = Boolean.TRUE.equals(combo.getProperties().get("locationUserTyping"));
            if (userTyping && selected != null && !selected.displayName().equals(text)) {
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
                        combo.getProperties().remove("locationUserTyping");
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

    private void setLocationItemsPreservingSelection(ComboBox<LocationOption> combo,
            java.util.List<LocationOption> matches, ObjectProperty<LocationOption> selectedLocationProperty,
            String editorText) {
        LocationOption selected = selectedLocationProperty.get();
        boolean editorStillShowsSelection = selected != null && selected.displayName().equals(editorText);
        boolean shouldRestoreSelection = selected != null && (editorStillShowsSelection || !combo.isFocused());
        java.util.List<LocationOption> items = new java.util.ArrayList<>(matches);
        if (shouldRestoreSelection && !items.contains(selected)) {
            items.add(0, selected);
            if (items.size() > LOCATION_SEARCH_LIMIT) {
                items = new java.util.ArrayList<>(items.subList(0, LOCATION_SEARCH_LIMIT));
            }
        }
        combo.getProperties().put("locationSelectionInProgress", true);
        try {
            combo.getItems().setAll(items);
            if (shouldRestoreSelection) {
                combo.setValue(selected);
                combo.getEditor().setText(selected.displayName());
                combo.getEditor().positionCaret(combo.getEditor().getText().length());
            }
        } finally {
            combo.getProperties().remove("locationSelectionInProgress");
            combo.getProperties().remove("locationUserTyping");
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
                java.util.List<LocationOption> sourceOptions = locationSourceOptions(combo);
                combo.getItems().setAll(sourceOptions.stream()
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

    @SuppressWarnings("unchecked")
    private java.util.List<LocationOption> locationSourceOptions(ComboBox<LocationOption> combo) {
        Object source = combo.getProperties().get("locationOptions");
        if (source instanceof java.util.List<?>) {
            return (java.util.List<LocationOption>) source;
        }
        return INDIA_LOCATION_OPTIONS;
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
        java.util.List<String> allValues = java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        combo.getItems().setAll(allValues.stream().limit(40).toList());

        java.util.function.Consumer<String> filterValues = text -> {
            String query = text == null ? "" : text.trim().toLowerCase();
            String selected = combo.getValue();
            java.util.List<String> matches = allValues.stream()
                    .filter(value -> value.toLowerCase().contains(query))
                    .limit(40)
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            if (selected != null && !matches.contains(selected)) {
                matches.add(0, selected);
            }
            combo.getProperties().put("citySelectionInProgress", true);
            try {
                combo.getItems().setAll(matches);
                if (selected != null) {
                    combo.setValue(selected);
                    combo.getSelectionModel().select(selected);
                }
            } finally {
                combo.getProperties().remove("citySelectionInProgress");
            }
            if (combo.isFocused() && !combo.isShowing()
                    && !Boolean.TRUE.equals(combo.getProperties().get("cityRefreshingForShow"))) {
                combo.show();
            }
        };

        combo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(combo.getProperties().get("citySelectionInProgress"))) {
                return;
            }
            if (newValue != null) {
                combo.getProperties().put("citySelectionInProgress", true);
                try {
                    combo.getProperties().put("cityUserTyping", false);
                    combo.getEditor().setText(newValue);
                    combo.getEditor().positionCaret(combo.getEditor().getText().length());
                } finally {
                    combo.getProperties().remove("citySelectionInProgress");
                }
                javafx.application.Platform.runLater(combo::hide);
            }
        });

        combo.getEditor().addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event ->
                combo.getProperties().put("cityUserTyping", true));
        combo.getEditor().textProperty().addListener((observable, oldText, text) -> {
            if (Boolean.TRUE.equals(combo.getProperties().get("citySelectionInProgress"))) {
                return;
            }
            String selected = combo.getValue();
            boolean userTyping = Boolean.TRUE.equals(combo.getProperties().get("cityUserTyping"));
            if (userTyping && selected != null && !selected.equals(text)) {
                combo.getSelectionModel().clearSelection();
                combo.setValue(null);
            }
            filterValues.accept(text);
        });
        combo.setOnShowing(event -> {
            combo.getProperties().put("cityRefreshingForShow", true);
            try {
                filterValues.accept(combo.getEditor().getText());
            } finally {
                combo.getProperties().remove("cityRefreshingForShow");
            }
        });
        combo.getEditor().setOnMouseClicked(event -> {
            filterValues.accept(combo.getEditor().getText());
            if (!combo.isShowing()) {
                combo.show();
            }
        });
        combo.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            String selected = combo.getValue();
            if (!isFocused && selected != null) {
                combo.getProperties().put("citySelectionInProgress", true);
                try {
                    combo.getProperties().put("cityUserTyping", false);
                    combo.getEditor().setText(selected);
                    combo.getEditor().positionCaret(combo.getEditor().getText().length());
                } finally {
                    combo.getProperties().remove("citySelectionInProgress");
                }
            }
        });
        combo.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE || event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                combo.getProperties().put("cityUserTyping", true);
            }
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
                    if (!combo.isShowing()) {
                        combo.show();
                    }
                    if (!combo.getItems().isEmpty()) {
                        int index = combo.getSelectionModel().getSelectedIndex();
                        combo.getSelectionModel().select(Math.min(index + 1, combo.getItems().size() - 1));
                    }
                    event.consume();
                }
                case UP -> {
                    if (!combo.isShowing()) {
                        combo.show();
                    }
                    if (!combo.getItems().isEmpty()) {
                        int index = combo.getSelectionModel().getSelectedIndex();
                        combo.getSelectionModel().select(index <= 0 ? 0 : index - 1);
                    }
                    event.consume();
                }
                default -> { }
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

    private record LocalJourneyState(LocationOption from, LocationOption to, java.time.LocalDate date) { }

    private record FlightSearchState(AirportOption from, AirportOption to, java.time.LocalDate date, int passengers) { }

    private record AirportOption(String airportName, String code, String city, String region) {
        private String displayName() {
            return city + " (" + code + ") - " + airportName;
        }

        private String shortDisplay() {
            return city + " (" + code + ")";
        }

        private boolean matches(String query) {
            if (query == null || query.isBlank()) {
                return true;
            }
            String searchable = (airportName + " " + code + " " + city + " " + region).toLowerCase(java.util.Locale.ROOT);
            return searchable.contains(query.toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static java.util.List<AirportOption> loadIndiaAirportOptions() {
        java.util.LinkedHashMap<String, AirportOption> options = new java.util.LinkedHashMap<>();
        java.util.List<AirportOption> airports = java.util.List.of(
                new AirportOption("Ozar / Nashik Airport", "ISK", "Nashik", "Maharashtra"),
                new AirportOption("Chhatrapati Shivaji Maharaj International Airport", "BOM", "Mumbai", "Maharashtra"),
                new AirportOption("Pune Airport", "PNQ", "Pune", "Maharashtra"),
                new AirportOption("Dr. Babasaheb Ambedkar International Airport", "NAG", "Nagpur", "Maharashtra"),
                new AirportOption("Chhatrapati Sambhajinagar Airport", "IXU", "Aurangabad", "Maharashtra"),
                new AirportOption("Shirdi Airport", "SAG", "Shirdi", "Maharashtra"),
                new AirportOption("Indira Gandhi International Airport", "DEL", "New Delhi", "Delhi"),
                new AirportOption("Kempegowda International Airport", "BLR", "Bengaluru", "Karnataka"),
                new AirportOption("Rajiv Gandhi International Airport", "HYD", "Hyderabad", "Telangana"),
                new AirportOption("Chennai International Airport", "MAA", "Chennai", "Tamil Nadu"),
                new AirportOption("Netaji Subhas Chandra Bose International Airport", "CCU", "Kolkata", "West Bengal"),
                new AirportOption("Sardar Vallabhbhai Patel International Airport", "AMD", "Ahmedabad", "Gujarat"),
                new AirportOption("Surat Airport", "STV", "Surat", "Gujarat"),
                new AirportOption("Vadodara Airport", "BDQ", "Vadodara", "Gujarat"),
                new AirportOption("Devi Ahilya Bai Holkar Airport", "IDR", "Indore", "Madhya Pradesh"),
                new AirportOption("Raja Bhoj Airport", "BHO", "Bhopal", "Madhya Pradesh"),
                new AirportOption("Jaipur International Airport", "JAI", "Jaipur", "Rajasthan"),
                new AirportOption("Maharana Pratap Airport", "UDR", "Udaipur", "Rajasthan"),
                new AirportOption("Chaudhary Charan Singh International Airport", "LKO", "Lucknow", "Uttar Pradesh"),
                new AirportOption("Lal Bahadur Shastri International Airport", "VNS", "Varanasi", "Uttar Pradesh"),
                new AirportOption("Prayagraj Airport", "IXD", "Prayagraj", "Uttar Pradesh"),
                new AirportOption("Cochin International Airport", "COK", "Kochi", "Kerala"),
                new AirportOption("Goa Manohar International Airport", "GOX", "Goa", "Goa"),
                new AirportOption("Dabolim Airport", "GOI", "Goa", "Goa"),
                new AirportOption("Biju Patnaik International Airport", "BBI", "Bhubaneswar", "Odisha"),
                new AirportOption("Jay Prakash Narayan Airport", "PAT", "Patna", "Bihar"),
                new AirportOption("Shaheed Bhagat Singh International Airport", "IXC", "Chandigarh", "Chandigarh"),
                new AirportOption("Sri Guru Ram Dass Jee International Airport", "ATQ", "Amritsar", "Punjab"),
                new AirportOption("Jolly Grant Airport", "DED", "Dehradun", "Uttarakhand"),
                new AirportOption("Swami Vivekananda Airport", "RPR", "Raipur", "Chhattisgarh"),
                new AirportOption("Birsa Munda Airport", "IXR", "Ranchi", "Jharkhand"),
                new AirportOption("Lokpriya Gopinath Bordoloi International Airport", "GAU", "Guwahati", "Assam"),
                new AirportOption("Sheikh ul-Alam International Airport", "SXR", "Srinagar", "Jammu and Kashmir"),
                new AirportOption("Veer Savarkar International Airport", "IXZ", "Port Blair", "Andaman and Nicobar Islands"));
        for (AirportOption airport : airports) {
            options.putIfAbsent(airport.city().toLowerCase(java.util.Locale.ROOT), airport);
        }
        for (String city : baseIndianCities()) {
            String normalizedCity = city.replaceAll("\\s*\\([^)]*\\)", "").replace(" / ", " ").trim();
            String key = normalizedCity.toLowerCase(java.util.Locale.ROOT);
            options.putIfAbsent(key, new AirportOption("City search - choose nearest available airport", citySearchCode(normalizedCity), normalizedCity, "India"));
        }
        return options.values().stream()
                .sorted(java.util.Comparator.comparing(AirportOption::city, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String citySearchCode(String city) {
        String letters = city == null ? "CTY" : city.replaceAll("[^A-Za-z]", "").toUpperCase(java.util.Locale.ROOT);
        if (letters.length() >= 3) {
            return letters.substring(0, Math.min(5, letters.length()));
        }
        return (letters + "CTY").substring(0, 3);
    }

    private static java.util.List<LocationOption> loadNashikLocalOptions() {
        java.util.LinkedHashMap<String, LocationOption> options = new java.util.LinkedHashMap<>();
        String[][] locations = {
                {"Panchavati", "Kumbh Area"}, {"Ramkund", "Ghat"}, {"Kalaram Mandir", "Temple"}, {"Sita Gufa", "Temple"},
                {"Kapaleshwar Mandir", "Temple"}, {"Tapovan", "Kumbh Area"}, {"Sadhugram", "Simhastha Zone"}, {"Godavari Ghat", "Ghat"},
                {"Naroshankar Temple", "Temple"}, {"Sundarnarayan Temple", "Temple"}, {"Muktidham", "Temple"}, {"Nashik Road", "City Area"},
                {"Nashik Road Railway Station", "Railway Station"}, {"CBS Bus Stand", "Bus Stand"}, {"Thakkar Bazaar", "Bus Stand"},
                {"Mahatma Nagar", "City Area"}, {"College Road", "City Area"}, {"Gangapur Road", "City Area"}, {"Canada Corner", "City Junction"},
                {"Sharanpur Road", "City Area"}, {"Indira Nagar", "City Area"}, {"CIDCO", "City Area"}, {"Ambad", "City Area"},
                {"Satpur", "City Area"}, {"Govind Nagar", "City Area"}, {"Pathardi Phata", "City Junction"}, {"Dwarka", "City Junction"},
                {"Mumbai Naka", "City Junction"}, {"Trimbak Naka", "City Junction"}, {"Old Nashik", "City Area"}, {"Panchavati Karanja", "City Junction"},
                {"Adgaon", "City Area"}, {"Makhmalabad", "City Area"}, {"Meru", "Local Area"}, {"Hirawadi", "City Area"},
                {"Amrutdham", "City Area"}, {"Konark Nagar", "City Area"}, {"Nashik Airport", "Airport"}, {"Ozar Airport", "Airport"},
                {"Deolali", "City Area"}, {"Deolali Camp", "City Area"}, {"Bhagur", "Town"}, {"Sinnar", "Taluka"},
                {"Igatpuri", "Taluka"}, {"Trimbakeshwar", "Temple Town"}, {"Trimbakeshwar Jyotirling Mandir", "Temple"}, {"Anjaneri", "Village"},
                {"Anjaneri Hills", "Tourist Place"}, {"Saptashrungi", "Temple"}, {"Vani", "Town"}, {"Dindori", "Taluka"},
                {"Niphad", "Taluka"}, {"Yeola", "Taluka"}, {"Malegaon", "City"}, {"Kalwan", "Taluka"}, {"Peth", "Taluka"},
                {"Peint", "Taluka"}, {"Nandgaon", "Taluka"}, {"Chandwad", "Taluka"}, {"Manmad", "Town"}, {"Lasalgaon", "Town"},
                {"Ozar", "Town"}, {"Chandori", "Village"}, {"Gangapur Dam", "Tourist Place"}, {"Someshwar Waterfall", "Tourist Place"},
                {"Pandavleni Caves", "Tourist Place"}, {"Sula Vineyards", "Tourist Place"}, {"Coin Museum", "Tourist Place"},
                {"Shivaji Nagar", "City Area"}, {"Jail Road", "City Area"}, {"Upnagar", "City Area"}, {"Datta Mandir Road", "City Area"},
                {"Khutwad Nagar", "City Area"}, {"Wadala Gaon", "Village"}, {"Kamatwade", "City Area"}, {"Untwadi", "City Area"},
                {"Trimurti Chowk", "City Junction"}, {"Parijat Nagar", "City Area"}, {"Lekha Nagar", "City Area"}, {"Chetana Nagar", "City Area"},
                {"Rane Nagar", "City Area"}, {"Shinde Gaon", "Village"}
        };
        for (String[] location : locations) {
            String id = "local-" + location[0].toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
            options.putIfAbsent(id, new LocationOption(id, location[0], location[1], "Nashik", "Maharashtra"));
        }
        return options.values().stream()
                .sorted(java.util.Comparator.comparing(LocationOption::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

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
        FlowPane ghatFlowPane = new FlowPane(12, 18);
        ghatFlowPane.setPrefWrapLength(1048);
        ghatFlowPane.getStyleClass().add("ghat-card-grid");
        List<Ghat> catalogue = ghatCatalogueService.catalogue();
        VBox operationalAlerts = new VBox(8);
        ghatOperationalAlerts = operationalAlerts;
        HBox summary = new HBox(12,
                ghatSummaryCard("\uE8B5", "Open Ghats", "—", "Safe for Snan", "ghat-summary-open"),
                ghatSummaryCard("\uE8B5", "Low Crowd", "—", "Comfortable movement", "ghat-summary-low"),
                ghatSummaryCard("\uE8B5", "High Crowd", "—", "Plan extra time", "ghat-summary-high"),
                ghatSummaryCard("\uE8B5", "Critical Crowd", "—", "Choose carefully", "ghat-summary-critical"),
                ghatSummaryMapCard());
        summary.getStyleClass().add("ghat-summary-row");

        TextField search = AppUi.textField("Search Ghat name...");
        search.getStyleClass().add("ghat-search-field");
        search.setText(ghatSearch);
        search.textProperty().addListener((observable, previous, value) -> {
            ghatSearch = value == null ? "" : value;
            renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats);
        });
        ComboBox<String> region = new ComboBox<>();
        region.getItems().addAll("All Regions", "Nashik / Panchavati", "Trimbakeshwar"); region.setValue(ghatRegionFilter); region.getStyleClass().add("input-combo"); region.setPrefWidth(146);
        region.setOnAction(event -> { ghatRegionFilter = region.getValue(); renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats); });
        ComboBox<String> crowd = ghatFilterCombo("All", "Low", "Moderate", "High", "Critical", "Unknown");
        crowd.setValue(ghatCrowdFilter);
        crowd.setOnAction(event -> { ghatCrowdFilter = crowd.getValue(); renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats); });
        ComboBox<String> snan = ghatFilterCombo("All", "Available", "Caution", "Suspended", "Unavailable");
        snan.setValue(ghatSnanFilter);
        snan.setOnAction(event -> { ghatSnanFilter = snan.getValue(); renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats); });
        ComboBox<String> sort = ghatFilterCombo("Live Crowd", "Name", "Walking Difficulty", "Estimated Wait");
        sort.setValue(ghatSort);
        sort.setPrefWidth(146);
        sort.setOnAction(event -> { ghatSort = sort.getValue(); renderGhats(ghatFlowPane, summary, operationalAlerts, currentGhats); });
        Button mapView = new Button("Filters"); mapView.getStyleClass().add("ghat-user-filter-button");
        mapView.setOnAction(event -> showInfo("Ghat Filters", "Use search, region, crowd, snan and sort controls to refine the Ghat list."));
        HBox filter = new HBox(10, search, region, crowd, snan, sort, mapView);
        filter.getStyleClass().add("ghat-filter-row");

        HBox featureCards = new HBox(14,
                ghatFeatureCard("\uE787", "Important Snan Guide", "Official Simhastha Snan schedule will appear here when published.", "View Guide", () -> showInfo("Important Snan Guide", "Official Simhastha Snan schedule will appear here when published.")),
                ghatFeatureCard("\uE707", "Interactive Ghat Map", "Explore Ghats and keep selected Ghat guidance in view.", "Open Map", this::openGhatMapView),
                ghatFeatureCard("\uE8D4", "Nashik–Trimbakeshwar Simhastha", "Nashik and Trimbakeshwar have distinct Akhada bathing arrangements.", "Learn More", () -> showInfo("Nashik–Trimbakeshwar Simhastha", "The festival takes place on the Godavari in Nashik and Trimbakeshwar. The two locations historically have distinct Akhada bathing arrangements.")));
        featureCards.getStyleClass().add("ghat-feature-row");
        featureCards.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        VBox content = new VBox(12, ghatHero(search), summary, filter,
                new VBox(2, label("Ghats by Live Crowd", "ghat-section-title"),
                        label("Live conditions appear first when available. Catalogue entries remain available for planning.", "ghat-section-subtitle")),
                operationalAlerts, ghatFlowPane, ghatGuideStrip(), featureCards);
        content.getStyleClass().addAll("pilgrim-dashboard-main", "ghat-page");
        content.setPadding(new Insets(12, 22, 28, 12));
        ghatFlowPane.getChildren().add(ghatState("Loading Ghat information…", false, ghatFlowPane, summary));
        loadGhats(ghatFlowPane, summary, operationalAlerts);
        return content;
    }

    private StackPane ghatHero(TextField pageSearch) {
        ImageView image = createImage("/images/welcome-light.png", 1080, 208, 0.54, 0.48);
        VBox copy = new VBox(5, label("Ghats & Snan", "ghat-hero-title"),
                label("Sacred Bathing • Live Crowd Guide • Safe Snan", "ghat-hero-subtitle"),
                label("Godavari guidance for a calm, informed pilgrimage", "ghat-hero-detail"));
        copy.setPadding(new Insets(28, 28, 24, 28)); copy.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(copy, Pos.CENTER_LEFT);
        HBox controls = ghatTopControls(pageSearch);
        // StackPane resizes unconstrained Regions to fill its content area.  The
        // controls themselves have a light background, so keep that surface to
        // the compact date/time/weather controls rather than the whole banner.
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(controls, Pos.TOP_RIGHT); StackPane.setMargin(controls, new Insets(14, 18, 0, 0));
        StackPane overlay = new StackPane(); overlay.getStyleClass().add("ghat-hero-overlay");
        StackPane hero = new StackPane(image, overlay, copy, controls);
        hero.getStyleClass().add("ghat-hero"); hero.setMinHeight(208);
        return hero;
    }

    private HBox ghatTopControls(TextField pageSearch) {
        Label weather = label("27°C   Nashik", "ghat-weather-pill");
        Button search = roundButton("\uE721", "Search ghats");
        search.setOnAction(event -> pageSearch.requestFocus());
        HBox controls = new HBox(8, AppUi.createThemeToggle(), weather, search, notificationBell(), profileButton());
        controls.getStyleClass().add("ghat-top-controls"); controls.setAlignment(Pos.CENTER_RIGHT); return controls;
    }

    private VBox ghatSummaryCard(String icon, String title, String value, String helper, String modifier) {
        Label iconLabel = label(icon, "ghat-summary-icon");
        VBox copy = new VBox(1, label(value, "ghat-summary-value"), label(title, "ghat-summary-label"), label(helper, "ghat-summary-helper"));
        VBox card = new VBox(4, new HBox(9, iconLabel, copy));
        card.getStyleClass().addAll("ghat-summary-card", modifier); HBox.setHgrow(card, Priority.ALWAYS); return card;
    }

    private Button ghatSummaryMapCard() {
        Button button = new Button("Interactive Map\nView all ghats on map");
        button.getStyleClass().add("ghat-summary-map");
        button.setOnAction(event -> openGhatMapView());
        HBox.setHgrow(button, Priority.ALWAYS);
        return button;
    }

    private ComboBox<String> ghatFilterCombo(String... values) {
        ComboBox<String> combo = new ComboBox<>(); combo.getItems().addAll(values); combo.setValue(values[0]); combo.getStyleClass().add("input-combo"); combo.setPrefWidth(128); return combo;
    }

    private VBox ghatFilterGroup(String title, Node control) {
        VBox group = new VBox(4, label(title, "ghat-filter-label"), control);
        group.getStyleClass().add("ghat-filter-group");
        return group;
    }

    private VBox ghatFeatureCard(String icon, String title, String detail, String action, Runnable callback) {
        Button button = new Button(action); button.getStyleClass().add("ghat-feature-action"); button.setOnAction(event -> callback.run());
        VBox card = new VBox(8, label(icon, "ghat-feature-icon"), label(title, "ghat-feature-title"), label(detail, "ghat-feature-detail"), button);
        card.getStyleClass().add("ghat-feature-card"); return card;
    }

    private HBox ghatGuideStrip() {
        HBox strip = new HBox(16,
                ghatGuideItem("\uE8EF", "Check Live Crowd", "Real-time crowd status for safe snan"),
                ghatGuideItem("\uE823", "Best Time to Visit", "Early morning 4:00 AM - 8:00 AM"),
                ghatGuideItem("\uE9D9", "Water Quality", "Regularly monitored for your safety"),
                ghatGuideItem("\uE72E", "Stay Safe", "Follow guidelines for a holy experience"));
        strip.getStyleClass().add("ghat-guide-strip");
        strip.getChildren().forEach(item -> HBox.setHgrow(item, Priority.ALWAYS));
        return strip;
    }

    private HBox ghatGuideItem(String icon, String title, String detail) {
        HBox item = new HBox(9, label(icon, "ghat-guide-icon"),
                new VBox(2, label(title, "ghat-guide-title"), label(detail, "ghat-guide-detail")));
        item.getStyleClass().add("ghat-guide-item");
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private void loadGhats(Pane cards, HBox summary, VBox operationalAlerts) {
        stopGhatRefresh();
        long refreshGeneration = ++ghatRefreshGeneration;
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        ghatRefreshSubscription = ghatOperationalStateService.subscribe(token, java.time.Duration.ofSeconds(30),
                ghats -> Platform.runLater(() -> {
                    if (refreshGeneration != ghatRefreshGeneration) return;
                    try {
                        if (!ghats.equals(currentGhats)) renderGhats(cards, summary, operationalAlerts, ghats);
                    } catch (Throwable error) {
                        LOGGER.fine("Unable to render Ghat information: " + error.getMessage());
                        cards.getChildren().setAll(ghatState("Unable to load Ghat information.", true, cards, summary));
                    }
                }),
                error -> Platform.runLater(() -> {
                    if (refreshGeneration != ghatRefreshGeneration) return;
                    LOGGER.fine("Unable to load Ghat information: " + error.getMessage());
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
            if (visibleGhats.isEmpty()) {
                cards.getChildren().setAll(ghatState("No Ghat information is currently available.", false, cards, summary));
                return;
            }
            List<Node> ghatCards = visibleGhats.stream().filter(java.util.Objects::nonNull).map(ghat -> (Node) cachedGhatCard(ghat)).toList();
            cards.getChildren().setAll(ghatCards);
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
            catch (Exception exception) { LOGGER.fine("Unable to close Ghat refresh subscription: " + exception.getMessage()); }
            ghatRefreshSubscription = null;
        }
    }

    private void stopScheduleRefresh() {
        schedulePageActive = false;
        scheduleRefreshGeneration++;
        scheduleRefreshInFlight = false;
        scheduleService.removeLocalChangeListener(scheduleLocalChangeListener);
        if (scheduleRefreshTimeline != null) {
            scheduleRefreshTimeline.stop();
            scheduleRefreshTimeline = null;
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
        int valueIndex = 0;
        for (Node node : summary.getChildren()) {
            if (!(node instanceof VBox) || valueIndex >= values.length) continue;
            VBox card = (VBox) node;
            HBox cardHeader = (HBox) card.getChildren().get(0);
            VBox copy = (VBox) cardHeader.getChildren().get(1);
            copy.getChildren().set(0, label(values[valueIndex], "ghat-summary-value"));
            valueIndex++;
        }
    }

    private VBox ghatCard(Ghat ghat) {
        VBox card = new VBox(10); card.getStyleClass().add("ghat-card"); card.setPrefWidth(GHAT_CARD_WIDTH); card.setMinWidth(GHAT_CARD_WIDTH); card.setMaxWidth(GHAT_CARD_WIDTH); card.setMinHeight(358);
        card.setOnMouseClicked(event -> openGhatDetailsFromCard(ghat, card));
        ImageView cardImage = ghatImageService.createView(ghat, GHAT_CARD_WIDTH, GHAT_CARD_IMAGE_HEIGHT);
        cardImage.setPreserveRatio(false);
        StackPane imageShell = new StackPane(cardImage);
        imageShell.getStyleClass().add("ghat-card-image-shell");
        Rectangle imageClip = new Rectangle(GHAT_CARD_WIDTH, GHAT_CARD_IMAGE_HEIGHT);
        imageClip.setArcWidth(22);
        imageClip.setArcHeight(22);
        imageShell.setClip(imageClip);
        if (ghat.crowdLevel() != Ghat.CrowdLevel.UNKNOWN) {
            Label crowdBadge = ghatBadge(ghat.crowdLevel().name(), "ghat-crowd-" + ghat.crowdLevel().name().toLowerCase());
            StackPane.setAlignment(crowdBadge, Pos.TOP_LEFT); StackPane.setMargin(crowdBadge, new Insets(9)); imageShell.getChildren().add(crowdBadge);
        }
        Label title = label(ghat.name(), "ghat-card-title");
        title.setWrapText(true);
        Label area = label("\uE707  " + (ghat.area().isBlank() ? "Location details pending" : ghat.area()), "ghat-card-area");
        area.setWrapText(true);
        VBox heading = new VBox(2, title, area);
        FlowPane statuses = new FlowPane(5, 5); statuses.setPadding(new Insets(0, 11, 0, 11));
        statuses.getStyleClass().add("ghat-status-row");
        if (ghat.operationalStatus() != Ghat.OperationalStatus.INFORMATION_ONLY) {
            statuses.getChildren().add(ghatBadge(displayStatus(ghat.operationalStatus()), "ghat-status"));
        }
        if (ghat.crowdLevel() != Ghat.CrowdLevel.UNKNOWN
                && ghat.operationalState().bathingStatus() != GhatOperationalState.BathingStatus.UNAVAILABLE) {
            statuses.getChildren().add(ghatBadge("Snan " + titleCase(ghat.operationalState().bathingStatus().name()),
                    ghat.operationalState().bathingRecommended() ? "ghat-bathing-available" : "ghat-bathing-unavailable"));
        }
        if (ghat.crowdLevel() != Ghat.CrowdLevel.UNKNOWN) {
            statuses.getChildren().add(ghatBadge("Water " + titleCase(ghat.operationalState().waterSafety().name()), "ghat-status"));
        }
        HBox metrics = new HBox(4);
        if (ghat.crowdLevel() != Ghat.CrowdLevel.UNKNOWN && ghat.estimatedWaitMinutes() != null) {
            metrics.getChildren().add(ghatMetric("WAIT", ghat.waitLabel()));
        }
        if (hasWalkingDetails(ghat)) {
            metrics.getChildren().add(ghatMetric("WALK", titleCase(ghat.walking().difficulty().name())));
        }
        if (ghat.walking().approximateSteps() != null) {
            metrics.getChildren().add(ghatMetric("STEPS", String.valueOf(ghat.walking().approximateSteps())));
        }
        if (ghat.weather().available()) {
            metrics.getChildren().add(ghatMetric("WEATHER", compactWeather(ghat.weather())));
        }
        Node safety = ghatOperationalNote(ghat);
        HBox actions = new HBox(5, ghatAction("History", event -> showGhatHistory(ghat), "ghat-card-action"),
                ghatAction("Locate", event -> locateGhat(ghat), "ghat-card-action ghat-card-action-primary"),
                ghatAction("Navigate", event -> navigateToGhat(ghat), "ghat-card-action"));
        HBox help = new HBox(4, ghatAction("Medical", event -> requestGhatHelp(ghat, "Medical"), "ghat-help-action"),
                ghatAction("Police", event -> requestGhatHelp(ghat, "Police"), "ghat-help-action"),
                ghatAction("Lost & Found", event -> requestGhatHelp(ghat, "Lost & Found"), "ghat-help-action"));
        card.getChildren().addAll(imageShell, heading);
        if (!statuses.getChildren().isEmpty()) card.getChildren().add(statuses);
        if (!metrics.getChildren().isEmpty()) card.getChildren().add(metrics);
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

    private void openGhatDetailsFromCard(Ghat ghat, VBox card) {
        selectGhatCard(ghat, card);
        root.setCenter(scroll(ghatDetailsPage(ghat, "")));
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
        button.setOnAction(event -> { action.handle(event); event.consume(); }); return button;
    }
    private boolean hasWalkingDetails(Ghat ghat) {
        return ghat != null && ghat.walking() != null
                && (ghat.walking().approximateSteps() != null || ghat.walking().distanceMeters() != null
                || ghat.walking().seniorFriendly() || ghat.walking().wheelchairAccessible());
    }
    private String walkingDescription(Ghat ghat) { String distance = ghat.walking().distanceMeters() == null ? "Entry distance will be updated by the administration." : ghat.walking().distanceMeters() + " m from entry"; return ghat.walking().approximateSteps() == null ? distance : distance + " • Approx. " + ghat.walking().approximateSteps() + " steps"; }
    private String displayStatus(Ghat.OperationalStatus status) { return titleCase(status.name()); }
    private String titleCase(String text) { String[] words = text.toLowerCase().replace('_', ' ').split(" "); StringBuilder result = new StringBuilder(); for (String word : words) { if (!word.isBlank()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' '); } return result.toString().trim(); }
    private String lastUpdatedLabel(String updated) { if (updated == null || updated.isBlank() || GhatDataFreshnessService.isStaleEpochMillis(updated)) return "Live status may be outdated"; try { return "Updated " + Math.max(0, (System.currentTimeMillis() - Long.parseLong(updated)) / 60000) + " min ago"; } catch (NumberFormatException exception) { return "Updated " + updated; } }
    private void showGhatHistory(Ghat ghat) {
        selectedGhat = ghat;
        root.setCenter(scroll(ghatDetailsPage(ghat, "")));
    }
    private String historyText(Ghat.History history) { return String.join("\n\n", java.util.stream.Stream.of(history.historicalBackground(), history.religiousSignificance(), history.simhasthaConnection(), history.associatedSacredPlaces(), history.rituals(), history.didYouKnow()).filter(value -> !value.isBlank()).toList()); }
    private VBox ghatDetailsPage(Ghat ghat, String notice) {
        Button back = new Button("Back to Ghats");
        back.getStyleClass().add("pilgrim-small-action");
        back.setOnAction(event -> showModulePage("ghat"));
        ImageView image = ghatImageService.createView(ghat, 520, 292);
        VBox heroText = new VBox(8, badge("GHATS & SNAN"), label(ghat.name(), "ghat-detail-title"),
                paragraph(ghat.area().isBlank() ? "Nashik Godavari riverfront" : ghat.area()),
                paragraph(ghat.description().isBlank() ? "Sacred bathing and movement guidance for Simhastha pilgrims." : ghat.description()));
        HBox hero = new HBox(18, image, heroText);
        hero.getStyleClass().add("ghat-detail-hero");
        HBox.setHgrow(heroText, Priority.ALWAYS);
        VBox status = new VBox(10, sectionTitle("Current Ghat Status"));
        java.util.List<Node> statusRows = new java.util.ArrayList<>();
        if (ghat.crowdLevel() != Ghat.CrowdLevel.UNKNOWN) statusRows.add(ghatLine("Crowd", titleCase(ghat.crowdLevel().name())));
        if (ghat.estimatedWaitMinutes() != null) statusRows.add(ghatLine("Wait", ghat.waitLabel()));
        if (ghat.operationalStatus() != Ghat.OperationalStatus.INFORMATION_ONLY) statusRows.add(ghatLine("Status", displayStatus(ghat.operationalStatus())));
        if (ghat.operationalState().bathingStatus() != GhatOperationalState.BathingStatus.UNAVAILABLE) statusRows.add(ghatLine("Snan", titleCase(ghat.operationalState().bathingStatus().name())));
        if (ghat.crowdLevel() != Ghat.CrowdLevel.UNKNOWN) statusRows.add(ghatLine("Water", titleCase(ghat.operationalState().waterSafety().name())));
        if (hasWalkingDetails(ghat)) statusRows.add(ghatLine("Walking", walkingDescription(ghat)));
        if (!ghat.lastUpdated().isBlank()) statusRows.add(ghatLine("Updated", lastUpdatedLabel(ghat.lastUpdated())));
        if (statusRows.isEmpty()) statusRows.add(paragraph("Live operational details will appear here after the administration publishes current field data."));
        status.getChildren().addAll(statusRows);
        status.getStyleClass().add("pilgrim-panel");
        VBox essentials = new VBox(10, sectionTitle("Pilgrim Essentials"), ghatEssentialsGrid(ghat));
        essentials.getStyleClass().add("pilgrim-panel");
        VBox support = new VBox(10, sectionTitle("Nearby Support"), ghatSupportGrid(ghat));
        support.getStyleClass().add("pilgrim-panel");
        VBox history = new VBox(10, sectionTitle("History & Significance"), paragraph(cleanHistoryText(ghat)));
        history.getStyleClass().add("pilgrim-panel");
        VBox location = new VBox(10, sectionTitle("Location & Access"), ghatLine("Area", ghat.area().isBlank() ? "Nashik, Maharashtra" : ghat.area()),
                ghatLine("Coordinates", coordinateText(ghat)), ghatLine("Gates", gateSummary(ghat)), ghatLine("Zones", zoneSummary(ghat)),
                ghatLine("Access note", accessNote(ghat)));
        location.getStyleClass().add("pilgrim-panel");
        VBox content = pageShell(ghat.name(), "Ghats & Snan details", back, hero);
        if (notice != null && !notice.isBlank()) content.getChildren().add(infoPanel("Important Update", notice));
        content.getChildren().addAll(status, essentials, support, history, location);
        return content;
    }

    private String cleanHistoryText(Ghat ghat) {
        java.util.List<String> parts = java.util.stream.Stream.of(
                        ghat.history().historicalBackground(),
                        ghat.history().religiousSignificance(),
                        ghat.history().simhasthaConnection(),
                        ghat.history().associatedSacredPlaces(),
                        ghat.history().rituals(),
                        ghat.history().didYouKnow())
                .filter(value -> value != null && !value.isBlank())
                .filter(value -> !value.toLowerCase(java.util.Locale.ROOT).contains("pending"))
                .filter(value -> !value.toLowerCase(java.util.Locale.ROOT).contains("will be added"))
                .toList();
        if (!parts.isEmpty()) return String.join("\n\n", parts);
        String area = ghat.area().isBlank() ? "Nashik" : ghat.area();
        return ghat.name() + " is part of the Godavari Ghat network used by pilgrims for darshan, movement and Snan planning during Simhastha. "
                + "The " + area + " location helps pilgrims identify the correct riverfront zone, nearby access routes and support services.";
    }
    private void locateGhat(Ghat ghat) {
        selectedGhat = ghat;
        googleMapsService.openGhatLocation(ghat)
                .ifPresent(message -> showInfo("Google Maps", message));
    }
    private void openGhatMapView() {
        List<Ghat> mapGhats = allGhatsForMap();
        googleMapsService.showGhats(root.getScene() == null ? null : root.getScene().getWindow(), mapGhats, null)
                .ifPresent(message -> showInfo("Google Maps", message));
    }

    private void openSimhasthaAreaMap(String category) {
        googleMapsService.showSimhasthaArea(root.getScene() == null ? null : root.getScene().getWindow(), category)
                .ifPresent(message -> showInfo("Simhastha Map", message));
    }

    private List<Ghat> allGhatsForMap() {
        LinkedHashMap<String, Ghat> ghats = new LinkedHashMap<>();
        ghatCatalogueService.catalogue().forEach(ghat -> ghats.put(ghat.id(), ghat));
        if (currentGhats != null) currentGhats.forEach(ghat -> ghats.put(ghat.id(), ghat));
        return List.copyOf(ghats.values());
    }
    private void navigateToGhat(Ghat ghat) {
        selectedGhat = ghat; GhatNavigationService.Decision decision = ghatNavigationService.decision(ghat);
        if (decision == GhatNavigationService.Decision.UNSAFE) { root.setCenter(scroll(ghatDetailsPage(ghat, "Safe Snan is currently not recommended for this Ghat. Choose one of the safer, less crowded alternatives."))); return; }
        if (decision == GhatNavigationService.Decision.NO_ENTRY) { root.setCenter(scroll(ghatDetailsPage(ghat, "No active entry route is currently available for this Ghat."))); return; }
        if (decision == GhatNavigationService.Decision.HIGH_CROWD_CONFIRMATION) { Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "This Ghat currently has high crowd. Continue to this Ghat?", ButtonType.YES, ButtonType.NO); confirm.setTitle("High Crowd"); confirm.setHeaderText("High Crowd"); AppUi.styleDialog(confirm, root.getScene() == null ? null : root.getScene().getWindow(), "ghat-navigation-dialog", ButtonType.YES); if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return; }
        java.util.Optional<GhatNavigationService.Destination> destination = ghatNavigationService.destinationFor(ghat);
        String destinationQuery = destination.isPresent() ? "" : ghatNavigationService.destinationQueryFor(ghat);
        Dialog<ButtonType> originDialog = new Dialog<>(); originDialog.setTitle("Choose Starting Location");
        TextField latitude = AppUi.textField("Latitude"); TextField longitude = AppUi.textField("Longitude");
        String targetName = destination.map(GhatNavigationService.Destination::entryName).orElse(ghat.name());
        ButtonType withoutOrigin = new ButtonType("Continue without origin");
        originDialog.getDialogPane().setContent(new VBox(8, label("Enter your current starting coordinates to open directions to " + targetName + ", or let Google Maps request your location.", "ghat-line-value"), latitude, longitude)); originDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, withoutOrigin, ButtonType.CANCEL); AppUi.styleDialog(originDialog, root.getScene() == null ? null : root.getScene().getWindow(), "ghat-navigation-dialog", ButtonType.OK);
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

    private FlowPane ghatEssentialsGrid(Ghat ghat) {
        FlowPane grid = new FlowPane(10, 10);
        grid.getStyleClass().add("ghat-detail-grid");
        grid.getChildren().addAll(
                ghatInfoTile("Snan readiness", titleCase(ghat.operationalState().bathingStatus().name()), snanAdvice(ghat)),
                ghatInfoTile("Water safety", titleCase(ghat.operationalState().waterSafety().name()), waterAdvice(ghat)),
                ghatInfoTile("Expected wait", ghat.estimatedWaitMinutes() == null ? "Admin update expected" : ghat.waitLabel(), waitAdvice(ghat)),
                ghatInfoTile("Walking access", walkingShort(ghat), walkingDescription(ghat)));
        return grid;
    }

    private FlowPane ghatSupportGrid(Ghat ghat) {
        FlowPane grid = new FlowPane(10, 10);
        grid.getStyleClass().add("ghat-detail-grid");
        supportTiles(ghat).forEach(tile -> grid.getChildren().add(tile));
        return grid;
    }

    private java.util.List<Node> supportTiles(Ghat ghat) {
        String area = ghat.area().isBlank() ? "near this Ghat" : "near " + ghat.area();
        java.util.List<Node> tiles = new java.util.ArrayList<>();
        tiles.add(ghatInfoTile("Locker / Cloakroom", "Nearby guidance", "Use official cloakroom or locker counters " + area + " before entering crowded Snan steps."));
        tiles.add(ghatInfoTile("Toilets", "Available nearby", "Follow signboards and official help points for nearest public toilet blocks."));
        tiles.add(ghatInfoTile("Parking", parkingLabel(ghat), "Use designated parking and continue by walking route or local shuttle when crowd control is active."));
        tiles.add(ghatInfoTile("Hotels & Stay", "Search nearby", "Stay options around Panchavati, CBS, Nashik Road and Trimbakeshwar can be checked from the Stay page."));
        tiles.add(ghatInfoTile("Medical Help", "First-aid support", "Use Medical action from the Ghat card for emergency numbers and support information."));
        tiles.add(ghatInfoTile("Police / Lost Help", "Help desk route", "For missing person or crowd assistance, use Police or Lost & Found from the Ghat card."));
        for (String facility : ghat.facilities()) {
            if (!facility.isBlank()) tiles.add(ghatInfoTile(facility, "Listed facility", "Shown from admin/catalogue facility data for this Ghat."));
        }
        return tiles.stream().limit(8).toList();
    }

    private VBox ghatInfoTile(String title, String value, String detail) {
        Label titleLabel = label(title, "ghat-detail-tile-title");
        Label valueLabel = label(value, "ghat-detail-tile-value");
        Label detailLabel = label(detail, "ghat-detail-tile-detail");
        detailLabel.setWrapText(true);
        VBox tile = new VBox(5, titleLabel, valueLabel, detailLabel);
        tile.getStyleClass().add("ghat-detail-tile");
        return tile;
    }

    private String snanAdvice(Ghat ghat) {
        return switch (ghat.operationalState().bathingStatus()) {
            case AVAILABLE -> "Snan entry is open. Follow gate and volunteer instructions.";
            case CAUTION -> "Snan is allowed with caution. Prefer low-crowd windows and avoid rushing.";
            case SUSPENDED -> "Snan is suspended right now. Choose another open Ghat.";
            case UNAVAILABLE -> "Admin has not marked Snan entry open for this Ghat.";
        };
    }

    private String waterAdvice(Ghat ghat) {
        return switch (ghat.operationalState().waterSafety()) {
            case NORMAL -> "Water condition is marked normal for guided Snan.";
            case CAUTION, HIGH_WATER -> "Use caution and stay inside guided bathing areas.";
            case DANGEROUS, BATHING_SUSPENDED -> "Avoid bathing here until administration marks it safe.";
        };
    }

    private String waitAdvice(Ghat ghat) {
        if (ghat.estimatedWaitMinutes() == null) return "Live wait will be shown after admin update.";
        if (ghat.estimatedWaitMinutes() <= 15) return "Good time for short visit and quick movement.";
        if (ghat.estimatedWaitMinutes() <= 35) return "Plan extra time and keep group members together.";
        return "Heavy wait expected. Check alternatives before reaching the entry gate.";
    }

    private String walkingShort(Ghat ghat) {
        String difficulty = titleCase(ghat.walking().difficulty().name());
        return ghat.walking().approximateSteps() == null ? difficulty : difficulty + " • " + ghat.walking().approximateSteps() + " steps";
    }

    private String parkingLabel(Ghat ghat) {
        return ghat.area().toLowerCase(java.util.Locale.ROOT).contains("trimbakeshwar") ? "Trimbakeshwar lots" : "Nashik city lots";
    }

    private String accessNote(Ghat ghat) {
        if (!ghat.operationalState().restrictionReason().isBlank()) return ghat.operationalState().restrictionReason();
        if (ghat.operationalStatus() == Ghat.OperationalStatus.OPEN) return "Entry is open. Use the listed gate and follow one-way crowd movement when active.";
        return "Check live status before visiting. Some access paths may be restricted by administration.";
    }

    private String coordinateText(Ghat ghat) { return ghat.latitude() == null || ghat.longitude() == null ? "Nashik Godavari zone coordinates will be updated by administration." : ghat.latitude() + ", " + ghat.longitude(); }

    private VBox emergencyPage() {
        VBox page = new VBox(12, emergencyTopControls(), emergencyHeader());
        page.getStyleClass().addAll("pilgrim-dashboard-main", "emergency-page");
        page.setPadding(new Insets(12, 22, 28, 22));
        if (!emergencyFeedback.isBlank()) {
            page.getChildren().add(emergencyFeedbackCard());
        }

        VBox left = new VBox(12, immediateHelpSection(), emergencyStatusRow(), emergencyServicesSection(), nearbyServicesSection());
        if (emergencyReportFormVisible) left.getChildren().add(emergencyReportForm());
        if (currentEmergency != null) left.getChildren().add(emergencyStatusSection());
        left.getStyleClass().add("emergency-left-column");
        left.setMinWidth(0);
        left.setMaxWidth(Double.MAX_VALUE);

        VBox right = new VBox(12, emergencyMapSection(), selectedFacilitySection(), kumbhAlertSection());
        right.getStyleClass().add("emergency-right-column");
        right.setMinWidth(0);
        right.setMaxWidth(Double.MAX_VALUE);

        GridPane layout = new GridPane();
        layout.getStyleClass().add("emergency-content-layout");
        layout.setHgap(14);
        layout.setVgap(14);
        javafx.scene.layout.ColumnConstraints leftColumn = new javafx.scene.layout.ColumnConstraints();
        leftColumn.setPercentWidth(35);
        leftColumn.setHgrow(Priority.ALWAYS);
        leftColumn.setFillWidth(true);
        javafx.scene.layout.ColumnConstraints rightColumn = new javafx.scene.layout.ColumnConstraints();
        rightColumn.setPercentWidth(65);
        rightColumn.setHgrow(Priority.ALWAYS);
        rightColumn.setFillWidth(true);
        layout.getColumnConstraints().addAll(leftColumn, rightColumn);
        layout.add(left, 0, 0);
        layout.add(right, 1, 0);
        GridPane.setHgrow(left, Priority.ALWAYS);
        GridPane.setHgrow(right, Priority.ALWAYS);
        GridPane.setFillWidth(left, true);
        GridPane.setFillWidth(right, true);
        layout.setMaxWidth(Double.MAX_VALUE);
        page.getChildren().add(layout);
        return page;
    }

    private HBox emergencyTopControls() {
        Label location = label("Nashik, Maharashtra", "emergency-top-meta");
        Label weather = label("Clear  •  29°C", "emergency-top-meta");
        Label dateTime = label(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM uuuu  •  h:mm a")), "emergency-top-meta");
        HBox actions = new HBox(10, location, weather, dateTime, createSpacer(), AppUi.createThemeToggle(),
                roundButton("\uE7F4", "Notifications"), roundButton("\uE77B", "Profile"));
        actions.getStyleClass().addAll("pilgrim-top-actions", "emergency-top-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private HBox emergencyHeader() {
        Label icon = AppUi.symbolIcon("\uE95E", "emergency-header-icon");
        Label title = label("Emergency & Safety", "emergency-header-title");
        Label subtitle = label("Get immediate help or find nearest emergency services", "emergency-header-subtitle");
        VBox copy = new VBox(3, title, subtitle);
        HBox header = new HBox(13, icon, copy);
        header.getStyleClass().add("emergency-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox immediateHelpSection() {
        VBox sos = emergencyActionCard("\uE95E", "SOS EMERGENCY", "Tap for instant critical help", "SEND SOS", "emergency-sos-card");
        VBox report = emergencyActionCard("\uE7BA", "REPORT EMERGENCY", "Provide details & get help", "REPORT NOW", "emergency-report-card");
        HBox cards = new HBox(10, sos, report);
        HBox.setHgrow(sos, Priority.ALWAYS);
        HBox.setHgrow(report, Priority.ALWAYS);
        cards.getStyleClass().add("emergency-action-row");
        VBox section = new VBox(10, emergencySectionTitle("NEED IMMEDIATE HELP?"), cards);
        if (sosConfirmationVisible) section.getChildren().add(sosConfirmationCard());
        section.getStyleClass().add("emergency-section");
        return section;
    }

    private VBox emergencyActionCard(String iconText, String titleText, String detailText, String actionText, String style) {
        Button action = new Button(actionText);
        action.getStyleClass().add("emergency-action-button");
        action.setMaxWidth(Double.MAX_VALUE);
        action.setOnAction(event -> {
            if ("SEND SOS".equals(actionText)) {
                sosConfirmationVisible = true;
            } else {
                emergencyReportFormVisible = true;
            }
            emergencyFeedback = "";
            refreshEmergencyPage();
        });
        VBox card = new VBox(8, AppUi.symbolIcon(iconText, "emergency-action-icon"),
                label(titleText, "emergency-action-title"), label(detailText, "emergency-action-detail"), action);
        card.getStyleClass().addAll("emergency-action-card", style);
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox emergencyStatusRow() {
        HBox row = new HBox(8,
                emergencyStatusCard("\uE707", "Current Location", "Nashik Kumbh Zone", "location"),
                emergencyStatusCard("\uE774", "GPS Accuracy", "High", "accuracy"),
                emergencyStatusCard("\uE73E", "You Are Safe", "We are here to help", "safe"));
        row.getStyleClass().add("emergency-status-row");
        return row;
    }

    private VBox emergencyStatusCard(String iconText, String titleText, String detailText, String type) {
        VBox card = new VBox(3, AppUi.symbolIcon(iconText, "emergency-status-icon"),
                label(titleText, "emergency-status-title"), label(detailText, "emergency-status-detail"));
        card.getStyleClass().addAll("emergency-status-card", "emergency-status-" + type);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox emergencyServicesSection() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        String[][] services = {
                { "\uE91D", "Hospital", "hospital" }, { "\uE7B8", "Medical Camp", "camp" },
                { "\uE95E", "First Aid Center", "aid" }, { "\uE7F2", "Ambulance", "ambulance" },
                { "\uE7FC", "Police Help Center", "police" }, { "\uE894", "Fire / Safety Unit", "fire" },
                { "\uE897", "Emergency Help Desk", "desk" }, { "\uE7E8", "Emergency Exit", "exit" } };
        for (int index = 0; index < services.length; index++) {
            VBox card = emergencyServiceCard(services[index][0], services[index][1], services[index][2]);
            grid.add(card, index % 4, index / 4);
            GridPane.setHgrow(card, Priority.ALWAYS);
        }
        Button nearestMedical = new Button("FIND NEAREST MEDICAL HELP");
        nearestMedical.getStyleClass().add("emergency-nearest-medical-button");
        nearestMedical.setMaxWidth(Double.MAX_VALUE);
        nearestMedical.setOnAction(event -> selectNearestMedicalHelp());
        VBox section = new VBox(10, emergencySectionTitle("FIND EMERGENCY SERVICES"), grid, nearestMedical);
        section.getStyleClass().add("emergency-section");
        return section;
    }

    private VBox emergencyServiceCard(String iconText, String titleText, String category) {
        VBox card = new VBox(5, AppUi.symbolIcon(iconText, "emergency-service-icon"), label(titleText, "emergency-service-title"));
        card.getStyleClass().addAll("emergency-service-card", "emergency-service-" + category);
        String filter = categoryForServiceCard(category);
        if (filter.equals(selectedEmergencyCategory)) card.getStyleClass().add("emergency-service-card-selected");
        card.setOnMouseClicked(event -> selectEmergencyCategory(filter));
        return card;
    }

    private VBox nearbyServicesSection() {
        VBox rows = new VBox(7);
        List<EmergencyServiceDemo> services = filteredEmergencyServices();
        if (emergencyServicesLoading) {
            rows.getChildren().add(label("Loading emergency services...", "emergency-empty-state"));
        } else if (!emergencyServicesError.isBlank() && !usingEmergencyDevelopmentFallback) {
            Button retry = new Button("RETRY"); retry.getStyleClass().add("emergency-secondary-button"); retry.setOnAction(event -> loadEmergencyServicesAsync());
            rows.getChildren().addAll(label("Emergency service data is currently unavailable.", "emergency-empty-state"), retry);
        } else if (services.isEmpty()) {
            rows.getChildren().add(label("No emergency service locations are currently available.", "emergency-empty-state"));
        } else {
            if (usingEmergencyDevelopmentFallback) {
                rows.getChildren().add(label("Emergency Service Locations", "emergency-empty-state"));
            }
            services.forEach(service -> rows.getChildren().add(nearbyServiceRow(service)));
        }
        Button viewAll = new Button("VIEW ALL SERVICES");
        viewAll.getStyleClass().add("emergency-text-button");
        viewAll.setOnAction(event -> selectEmergencyFilter("All"));
        VBox section = new VBox(10, emergencySectionTitle("NEARBY SERVICES"), rows, viewAll);
        section.getStyleClass().add("emergency-section");
        return section;
    }

    private HBox nearbyServiceRow(EmergencyServiceDemo service) {
        VBox copy = new VBox(2, label(service.name(), "emergency-nearby-name"), label(service.subType(), "emergency-nearby-type"));
        Label open = label(displayEmergencyValue(service.status()), "emergency-open-status");
        Label arrow = label(">", "emergency-nearby-arrow");
        HBox row = new HBox(9, AppUi.symbolIcon(emergencyIcon(service.category()), "emergency-nearby-icon"), copy, createSpacer(),
                label(formatEmergencyDistance(emergencyDistanceMeters(service)), "emergency-distance"), open, arrow);
        row.getStyleClass().add("emergency-nearby-card");
        if (selectedEmergencyFacility != null && service.id().equals(selectedEmergencyFacility.id())) row.getStyleClass().add("emergency-nearby-card-selected");
        row.setOnMouseClicked(event -> selectEmergencyFacility(service));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox emergencyMapSection() {
        FlowPane chips = new FlowPane(7, 7);
        String[] filters = { "All", "Hospital", "Medical Camp", "Ambulance", "Police", "Fire / Safety",
                "First Aid", "Emergency Help Desk", "Exit" };
        for (String filter : filters) {
            Button chip = new Button(filter);
            chip.getStyleClass().add("emergency-filter-chip");
            if (filter.equals(selectedEmergencyFilter)) chip.getStyleClass().add("emergency-filter-chip-selected");
            chip.setOnAction(event -> selectEmergencyFilter(filter));
            chips.getChildren().add(chip);
        }
        StackPane map = buildEmergencyMapShell();
        VBox section = new VBox(11, chips, map);
        section.getStyleClass().addAll("emergency-section", "emergency-map-container");
        return section;
    }

    private StackPane buildEmergencyMapShell() {
        try {
            EmergencyMapView mapView = emergencyMapView();
            if (mapView == null) return emergencyMapFallback();
            mapView.detach();
            List<EmergencyServiceDemo> filteredServices = filteredEmergencyServices();
            mapView.updateMarkers(emergencyMarkersJson(filteredServices),
                    selectedEmergencyFacility == null ? "" : selectedEmergencyFacility.id());
            if (!emergencyMapFocusId.isBlank()) mapView.focusFacility(emergencyMapFocusId);
            if (emergencyRouteActive && selectedEmergencyFacility != null) mapView.drawApproximateRoute(selectedEmergencyFacility.latitude(), selectedEmergencyFacility.longitude());
            else mapView.clearRoute();
            if (activeEmergencyAlert != null && activeEmergencyAlert.hasLocation()) {
                mapView.showEmergencyAlert(activeEmergencyAlert);
            } else {
                mapView.clearEmergencyAlert();
            }

            Button centerOnMe = new Button("CENTER ON ME");
            centerOnMe.getStyleClass().add("emergency-map-control");
            centerOnMe.setOnAction(event -> mapView.centerOnDemoLocation());
            StackPane map = new StackPane(mapView.node(), centerOnMe);
            map.getStyleClass().add("emergency-map-shell");
            map.setMinHeight(360);
            StackPane.setAlignment(centerOnMe, Pos.TOP_RIGHT);
            StackPane.setMargin(centerOnMe, new Insets(10));
            return map;
        } catch (RuntimeException | LinkageError error) {
            System.err.println("EMERGENCY_MAP_DIAGNOSTIC stage=PAGE_BUILD error=" + error);
            return emergencyMapFallback();
        }
    }

    private StackPane emergencyMapFallback() {
        Label title = label("Nashik Emergency Map unavailable", "emergency-map-failure-title");
        Label detail = label("Check internet connection or retry.", "emergency-map-failure-detail");
        Button retry = new Button("RETRY MAP");
        retry.getStyleClass().add("emergency-secondary-button");
        retry.setOnAction(event -> { emergencyMapView = null; refreshEmergencyPage(); });
        VBox fallback = new VBox(7, title, detail, retry);
        fallback.setAlignment(Pos.CENTER);
        StackPane map = new StackPane(fallback);
        map.getStyleClass().addAll("emergency-map-shell", "emergency-map-failure");
        map.setMinHeight(360);
        return map;
    }

    private VBox selectedFacilitySection() {
        EmergencyServiceDemo facility = selectedEmergencyFacility;
        if (facility == null) {
            VBox empty = new VBox(8, emergencySectionTitle("SELECTED FACILITY"), label("Select a nearby service to view facility details.", "emergency-empty-state"));
            empty.getStyleClass().add("emergency-facility-details");
            return empty;
        }
        HBox heading = new HBox(10, AppUi.symbolIcon(emergencyIcon(facility.category()), "emergency-facility-icon"),
                new VBox(2, label(facility.name(), "emergency-facility-name"), label(facility.subType(), "emergency-facility-type")),
                createSpacer(), label(displayEmergencyValue(facility.status()), "emergency-open-status"));
        HBox details = new HBox(13, label("Distance: " + formatEmergencyDistance(emergencyDistanceMeters(facility)), "emergency-facility-detail"), label("Contact: " + displayEmergencyValue(facility.contact()), "emergency-facility-detail"),
                label("Sector: " + displayEmergencyValue(facility.sector()), "emergency-facility-detail"));
        FlowPane facilities = new FlowPane(6, 6);
        for (String item : facility.facilities()) facilities.getChildren().add(label(item, "emergency-facility-tag"));
        Button directions = emergencyPlaceholderButton("GET DIRECTIONS", "emergency-secondary-button");
        directions.setOnAction(event -> {
            emergencyRouteActive = true;
            emergencyMapFocusId = facility.id();
            emergencyFeedback = "Approximate route preview shown for " + facility.name() + ".";
            refreshEmergencyPage();
        });
        Button call = emergencyPlaceholderButton("CALL", "emergency-primary-button");
        call.setOnAction(event -> showEmergencyFeedback(facility.contact() == null || facility.contact().isBlank() ? "Contact information is not available." : "Emergency Contact: " + facility.contact()));
        Button detailsButton = emergencyPlaceholderButton(emergencyDetailsExpanded ? "HIDE DETAILS" : "VIEW DETAILS", "emergency-secondary-button");
        detailsButton.setOnAction(event -> { emergencyDetailsExpanded = !emergencyDetailsExpanded; refreshEmergencyPage(); });
        HBox actions = new HBox(8, directions, call, detailsButton);
        VBox panel = new VBox(10, heading, details, facilities, actions);
        if (emergencyRouteActive) panel.getChildren().add(emergencyRouteSummary(facility));
        if (emergencyDetailsExpanded) panel.getChildren().add(expandedFacilityDetails(facility));
        panel.getStyleClass().add("emergency-facility-details");
        return panel;
    }

    private Button emergencyPlaceholderButton(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        return button;
    }

    private VBox kumbhAlertSection() {
        if (emergencyAlertsLoading) return alertCard("KUMBH ALERT", "Loading emergency alerts...");
        if (activeEmergencyAlert == null) return alertCard("KUMBH ALERT", "No active emergency alerts.");
        Button view = new Button("VIEW ON MAP");
        view.getStyleClass().add("emergency-alert-action");
        boolean hasMapLocation = activeEmergencyAlert.hasLocation();
        view.setDisable(!hasMapLocation);
        view.setOnAction(event -> {
            if (!activeEmergencyAlert.hasLocation()) return;
            EmergencyMapView mapView = emergencyMapView();
            if (mapView != null) {
                mapView.focusEmergencyAlert(activeEmergencyAlert);
                emergencyMapMessage = activeEmergencyAlert.title() + " emergency alert selected.";
                emergencyFeedback = "Emergency alert location selected on the Nashik map.";
            }
            refreshEmergencyPage();
        });
        if (!hasMapLocation) view.setText("MAP LOCATION UNAVAILABLE");
        HBox heading = new HBox(8, label("KUMBH ALERT", "emergency-alert-title"), createSpacer(),
                label(activeEmergencyAlert.severity().name(), "emergency-alert-title"));
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(3,
                label(displayEmergencyValue(activeEmergencyAlert.title()), "emergency-alert-text"),
                label(displayEmergencyValue(activeEmergencyAlert.message()), "emergency-alert-text"));
        if (!activeEmergencyAlert.locationLabel().isBlank()) {
            content.getChildren().add(label("Location: " + activeEmergencyAlert.locationLabel(), "emergency-alert-text"));
        }
        if (!activeEmergencyAlert.affectedArea().isBlank()) {
            content.getChildren().add(label("Affected area: " + activeEmergencyAlert.affectedArea(), "emergency-alert-text"));
        }
        VBox alert = new VBox(5, heading, content, view);
        alert.getStyleClass().add("emergency-alert-card");
        return alert;
    }
    private VBox alertCard(String title,String text){ VBox alert=new VBox(5,label(title,"emergency-alert-title"),label(text,"emergency-alert-text")); alert.getStyleClass().add("emergency-alert-card"); return alert; }

    private void selectEmergencyCategory(String category) {
        applyEmergencyFilter(category);
    }

    private void selectEmergencyFilter(String filter) {
        applyEmergencyFilter(filter);
    }

    /** Shared by sidebar category cards and map chips to keep list and marker state identical. */
    private void applyEmergencyFilter(String filter) {
        selectedEmergencyFilter = filter == null ? "All" : filter;
        selectedEmergencyCategory = selectedEmergencyFilter;
        emergencyMapMessage = "";
        // A category change should display the complete result set, not retain a prior row's map focus.
        emergencyMapFocusId = "";
        reconcileEmergencySelectionForFilter();
        refreshEmergencyPage();
    }

    private void reconcileEmergencySelectionForFilter() {
        if (selectedEmergencyFacility == null) return;

        EmergencyServiceDemo previous = selectedEmergencyFacility;
        EmergencyServiceDemo latest = filteredEmergencyServices().stream()
                .filter(service -> service.id().equals(previous.id()))
                .findFirst()
                .orElse(null);
        if (latest == null) {
            // The document was deleted, deactivated, or filtered out: never leave a stale
            // details card or route on the page.
            selectedEmergencyFacility = null;
            emergencyDetailsExpanded = false;
            emergencyMapFocusId = "";
            clearEmergencyRouteForUnavailableFacility();
            return;
        }

        // A reload may contain an Admin edit for the same service id. Keep the selection
        // attached to that document, not to the old immutable display snapshot.
        selectedEmergencyFacility = latest;
        boolean locationChanged = Double.compare(previous.latitude(), latest.latitude()) != 0
                || Double.compare(previous.longitude(), latest.longitude()) != 0;
        if (locationChanged) clearEmergencyRouteForUnavailableFacility();
    }

    private void clearEmergencyRouteForUnavailableFacility() {
        if (!emergencyRouteActive) return;
        emergencyRouteActive = false;
        if (emergencyMapView != null) emergencyMapView.clearRoute();
    }

    private void selectEmergencyFacility(EmergencyServiceDemo facility) {
        selectedEmergencyFacility = facility;
        emergencyDetailsExpanded = false;
        emergencyMapFocusId = facility.id();
        emergencyFeedback = "Selected facility: " + facility.name() + " — " + formatEmergencyDistance(emergencyDistanceMeters(facility));
        refreshEmergencyPage();
    }

    private void selectNearestMedicalHelp() {
        selectedEmergencyFacility = emergencyServices.stream()
                .filter(service -> List.of("Hospital", "Medical Camp", "First Aid", "Ambulance").contains(service.category()))
                .min(java.util.Comparator.comparingInt(this::emergencyDistanceMeters))
                .orElse(null);
        selectedEmergencyFilter = "All";
        selectedEmergencyCategory = "All";
        emergencyDetailsExpanded = false;
        emergencyMapMessage = "";
        emergencyMapFocusId = selectedEmergencyFacility == null ? "" : selectedEmergencyFacility.id();
        if (selectedEmergencyFacility != null) {
            emergencyFeedback = "Nearest medical help: " + selectedEmergencyFacility.name() + " — " + formatEmergencyDistance(emergencyDistanceMeters(selectedEmergencyFacility));
        }
        refreshEmergencyPage();
    }

    private List<EmergencyServiceDemo> filteredEmergencyServices() {
        return emergencyServices.stream().filter(service -> matchesEmergencyFilter(service, selectedEmergencyFilter)).toList();
    }

    private boolean matchesEmergencyFilter(EmergencyServiceDemo service, String filter) {
        return "All".equals(filter) || service.category().equals(filter);
    }

    private void loadEmergencyServicesAsync() {
        AppSession.User user = AppSession.currentUser();
        if (emergencyServicesLoading || user == null || user.idToken() == null || user.idToken().isBlank() || !emergencyFirestore.isEnabled()) return;
        emergencyServicesLoading = true;
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try { return emergencyFirestore.loadEmergencyServices(user.idToken()); }
            catch (Exception exception) {
                System.err.println("EMERGENCY_SERVICE_READ_DIAGNOSTIC action=LOAD tokenPresent=true sessionRole=" + user.role() + " result=failed error=" + exception.getMessage());
                return null;
            }
        }).thenAccept(records -> Platform.runLater(() -> {
            emergencyServicesLoading = false;
            if (records == null) {
                emergencyServicesError = "permission_or_network";
                usingEmergencyDevelopmentFallback = true;
                emergencyServices = EmergencyDevelopmentServices.create().stream()
                        .map(this::toEmergencyServiceDemo).toList();
            }
            else {
                emergencyServicesError = "";
                // A successful but empty collection gets a local visual dataset; any real
                // document immediately replaces it rather than being mixed with it.
                usingEmergencyDevelopmentFallback = records.isEmpty();
                emergencyServices = (records.isEmpty() ? EmergencyDevelopmentServices.create() : records).stream()
                        .filter(EmergencyService::visibleToUsers)
                        .map(this::toEmergencyServiceDemo)
                        .toList();
                reconcileEmergencySelectionForFilter();
            }
            reconcileEmergencySelectionForFilter();
            if (emergencyPageActive) refreshEmergencyPage();
        }));
    }
    private void loadEmergencyAlertsAsync() {
        AppSession.User user = AppSession.currentUser();
        if (emergencyAlertsLoading || user == null || !emergencyFirestore.isEnabled()) return;
        emergencyAlertsLoading = true;
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return emergencyFirestore.loadEmergencyAlerts(user.idToken());
            } catch (Exception exception) {
                System.err.println("EMERGENCY_ALERT_READ_DIAGNOSTIC action=LOAD result=failed error=" + exception.getMessage());
                return null;
            }
        }).thenAccept(alerts -> Platform.runLater(() -> {
            emergencyAlertsLoading = false;
            if (alerts == null) {
                emergencyAlertsError = "permission_or_network";
                usingEmergencyAlertPreview = true;
                activeEmergencyAlert = emergencyAlertPreview();
            } else {
                emergencyAlertsError = "";
                usingEmergencyAlertPreview = false;
                activeEmergencyAlert = alerts.stream()
                        .sorted(java.util.Comparator
                                .comparingInt((EmergencyAlert alert) -> alertSeverityPriority(alert.severity()))
                                .reversed()
                                .thenComparing(EmergencyAlert::createdAt,
                                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                        .findFirst()
                        .orElse(null);
            }
            if (emergencyPageActive) refreshEmergencyPage();
        }));
    }

    private int alertSeverityPriority(EmergencyAlert.Severity severity) {
        return switch (severity == null ? EmergencyAlert.Severity.INFO : severity) {
            case CRITICAL -> 3;
            case HIGH -> 2;
            case INFO -> 1;
        };
    }

    private EmergencyAlert emergencyAlertPreview() {
        return new EmergencyAlert("local-preview-ramkund-advisory", "Crowd Advisory - Ramkund Area",
                "Heavy pilgrim movement reported near Ramkund. Use alternate access routes and follow on-ground instructions.",
                EmergencyAlert.Severity.HIGH, "Ramkund, Nashik", "Ramkund Area", 20.0056, 73.7939, "", "");
    }

    private EmergencyServiceDemo toEmergencyServiceDemo(EmergencyService service) {
        String category = switch (service.category()) {
            case HOSPITAL -> "Hospital"; case MEDICAL_CAMP -> "Medical Camp"; case FIRST_AID -> "First Aid";
            case AMBULANCE -> "Ambulance"; case POLICE -> "Police"; case FIRE_SAFETY -> "Fire / Safety";
            case HELP_DESK -> "Emergency Help Desk"; case EMERGENCY_EXIT -> "Exit";
        };
        return new EmergencyServiceDemo(service.serviceId(), service.name(), category, service.subType(), 0,
                EmergencyService.displayStatus(service.operationalStatus()), service.contactNumber(), service.sector(), service.area(),
                service.facilities(), service.description(), service.latitude(), service.longitude());
    }

    private String categoryForServiceCard(String category) {
        return switch (category) {
            case "camp" -> "Medical Camp";
            case "aid" -> "First Aid";
            case "police" -> "Police";
            case "fire" -> "Fire / Safety";
            case "desk" -> "Emergency Help Desk";
            case "exit" -> "Exit";
            default -> category.substring(0, 1).toUpperCase() + category.substring(1);
        };
    }

    private String emergencyIcon(String category) {
        return switch (category) {
            case "Hospital" -> "\uE91D";
            case "Medical Camp" -> "\uE7B8";
            case "First Aid" -> "\uE95E";
            case "Ambulance" -> "\uE7F2";
            case "Police" -> "\uE7FC";
            case "Fire / Safety" -> "\uE894";
            case "Emergency Help Desk" -> "\uE897";
            default -> "\uE7E8";
        };
    }

    private String formatEmergencyDistance(int meters) {
        return meters >= 1000 ? String.format(java.util.Locale.ROOT, "%.1f km", meters / 1000.0) : meters + " m";
    }

    private int emergencyDistanceMeters(EmergencyServiceDemo service) {
        double latitudeDelta = Math.toRadians(service.latitude() - EMERGENCY_DEMO_USER_LATITUDE);
        double longitudeDelta = Math.toRadians(service.longitude() - EMERGENCY_DEMO_USER_LONGITUDE);
        double originLatitude = Math.toRadians(EMERGENCY_DEMO_USER_LATITUDE);
        double destinationLatitude = Math.toRadians(service.latitude());
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(originLatitude) * Math.cos(destinationLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return (int) Math.round(6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private String displayEmergencyValue(String value) {
        return value == null || value.isBlank() ? "Not available" : value;
    }

    private void refreshEmergencyPage() {
        root.setCenter(scroll(emergencyPage()));
        if (emergencyMapView != null) {
            Platform.runLater(emergencyMapView::requestSizeInvalidationAfterLayout);
        }
    }

    private void showEmergencyFeedback(String message) {
        emergencyFeedback = message;
        refreshEmergencyPage();
    }

    private EmergencyMapView emergencyMapView() {
        try {
            if (emergencyMapView == null) {
                emergencyMapView = new EmergencyMapView(this::selectEmergencyFacilityById);
            }
            return emergencyMapView;
        } catch (RuntimeException | LinkageError error) {
            System.err.println("EMERGENCY_MAP_DIAGNOSTIC stage=CONSTRUCT error=" + error);
            emergencyMapView = null;
            return null;
        }
    }

    private void selectEmergencyFacilityById(String id) {
        emergencyServices.stream().filter(service -> service.id().equals(id)).findFirst()
                .ifPresent(this::selectEmergencyFacility);
    }

    private String emergencyMarkersJson(List<EmergencyServiceDemo> services) {
        return services.stream().map(service -> "{"
                + "\"id\":\"" + json(service.id()) + "\","
                + "\"name\":\"" + json(service.name()) + "\","
                + "\"category\":\"" + json(service.category()) + "\","
                + "\"subType\":\"" + json(service.subType()) + "\","
                + "\"distance\":\"" + json(formatEmergencyDistance(emergencyDistanceMeters(service))) + "\","
                + "\"status\":\"" + json(service.status()) + "\","
                + "\"contact\":\"" + json(service.contact()) + "\","
                + "\"sector\":\"" + json(service.sector()) + "\","
                + "\"area\":\"" + json(service.area()) + "\","
                + "\"latitude\":" + service.latitude() + ","
                + "\"longitude\":" + service.longitude() + "}").collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String json(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private VBox emergencyFeedbackCard() {
        VBox feedback = new VBox(3, label("EMERGENCY UPDATE", "emergency-feedback-title"), label(emergencyFeedback, "emergency-feedback-text"));
        feedback.getStyleClass().add("emergency-feedback-card");
        return feedback;
    }

    private VBox sosConfirmationCard() {
        Button confirm = new Button(emergencySubmissionInProgress ? "SUBMITTING..." : "CONFIRM SOS");
        confirm.getStyleClass().add("emergency-danger-button");
        confirm.setDisable(emergencySubmissionInProgress);
        confirm.setOnAction(event -> submitEmergency(createEmergencyReport(EmergencyReport.Source.SOS,
                EmergencyReport.EmergencyType.OTHER, "Emergency SOS request", 1, "", EmergencyReport.Priority.CRITICAL), true));
        Button cancel = new Button("CANCEL");
        cancel.getStyleClass().add("emergency-secondary-button");
        cancel.setDisable(emergencySubmissionInProgress);
        cancel.setOnAction(event -> { sosConfirmationVisible = false; refreshEmergencyPage(); });
        VBox card = new VBox(7, label("Send Emergency SOS?", "emergency-confirm-title"),
                label("This is a real emergency request to Simhastha Emergency Management. The stored location is Demo Nashik Location until GPS is enabled.", "emergency-confirm-text"),
                new HBox(8, confirm, cancel));
        card.getStyleClass().add("emergency-confirm-card");
        return card;
    }

    private VBox emergencyReportForm() {
        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll("Medical Emergency", "Police / Security", "Fire Emergency", "Crowd / Stampede Risk", "Accident", "Child / Elderly Assistance", "River / Ghat Emergency", "Other");
        category.setPromptText("Emergency Type *");
        category.getStyleClass().add("input-combo");
        TextField description = AppUi.textField("Short Description *");
        TextField peopleAffected = AppUi.textField("People Affected *");
        TextField landmark = AppUi.textField("Optional Landmark");
        Label error = label("", "emergency-validation-error");
        Label location = label("Location: Demo Nashik Location (GPS integration is not enabled).", "emergency-form-note");
        Button submit = new Button(emergencySubmissionInProgress ? "SUBMITTING..." : "SUBMIT REPORT");
        submit.getStyleClass().add("emergency-primary-button");
        submit.setDisable(emergencySubmissionInProgress);
        submit.setOnAction(event -> {
            int people;
            try { people = Integer.parseInt(peopleAffected.getText().trim()); } catch (Exception ignored) { people = 0; }
            if (category.getValue() == null || category.getValue().isBlank()) { error.setText("Select an emergency type."); return; }
            if (description.getText().trim().isBlank()) { error.setText("Enter a short description."); return; }
            if (people < 1) { error.setText("People affected must be at least 1."); return; }
            EmergencyReport.EmergencyType type = EmergencyReport.typeForLabel(category.getValue());
            submitEmergency(createEmergencyReport(EmergencyReport.Source.REPORT, type, description.getText().trim(), people,
                    landmark.getText().trim(), EmergencyReport.defaultPriority(type)), false);
        });
        Button cancel = new Button("CANCEL");
        cancel.getStyleClass().add("emergency-secondary-button");
        cancel.setDisable(emergencySubmissionInProgress);
        cancel.setOnAction(event -> { emergencyReportFormVisible = false; refreshEmergencyPage(); });
        VBox form = new VBox(8, emergencySectionTitle("REPORT EMERGENCY"), category, description, peopleAffected, location, landmark, error, new HBox(8, submit, cancel));
        form.getStyleClass().add("emergency-report-form");
        return form;
    }

    private EmergencyReport createEmergencyReport(EmergencyReport.Source source, EmergencyReport.EmergencyType type,
            String description, int peopleAffected, String landmark, EmergencyReport.Priority priority) {
        AppSession.User user = AppSession.currentUser();
        String now = String.valueOf(System.currentTimeMillis());
        String id = "emg-" + now + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        String tracking = "EMG-" + now.substring(Math.max(0, now.length() - 8));
        return new EmergencyReport(id, tracking, user == null ? "" : user.uid(), user == null ? "" : user.displayName(),
                "Not available", type, description, peopleAffected, landmark, EMERGENCY_DEMO_USER_LATITUDE,
                EMERGENCY_DEMO_USER_LONGITUDE, "Demo Nashik Location", priority, EmergencyReport.Status.REPORTED,
                "", "", "", "", EmergencyReport.userStatusText(EmergencyReport.Status.REPORTED), now, now, "", "", "", "", source);
    }

    private void submitEmergency(EmergencyReport report, boolean sos) {
        if (emergencySubmissionInProgress) return;
        AppSession.User user = AppSession.currentUser();
        if (user == null || user.idToken() == null || user.idToken().isBlank() || !emergencyFirestore.isEnabled()) {
            emergencyFeedback = "Emergency request could not be submitted. Sign in with Firebase and check the connection, then retry.";
            refreshEmergencyPage();
            return;
        }
        emergencySubmissionInProgress = true;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emergencyFirestore.createEmergencyReport(report, user.idToken());
                Platform.runLater(() -> {
                    currentEmergency = report;
                    sosConfirmationVisible = false;
                    emergencyReportFormVisible = false;
                    emergencySubmissionInProgress = false;
                    emergencyFeedback = (sos ? "Emergency SOS submitted successfully. " : "Emergency report submitted successfully. ")
                            + "Tracking ID: " + report.trackingId() + ". Your emergency request has been submitted to Simhastha Emergency Management.";
                    if (emergencyPageActive) refreshEmergencyPage();
                });
            } catch (Exception exception) {
                System.err.println("EMERGENCY_CREATE_DIAGNOSTIC action=CREATE emergencyId=" + report.emergencyId() + " result=failed error=" + exception.getMessage());
                Platform.runLater(() -> {
                    emergencySubmissionInProgress = false;
                    emergencyFeedback = "Emergency request could not be submitted. Please use RETRY after checking your connection.";
                    if (emergencyPageActive) refreshEmergencyPage();
                });
            }
        });
    }

    private void loadCurrentEmergencyAsync() {
        AppSession.User user = AppSession.currentUser();
        if (emergencyStateLoading || user == null || user.idToken() == null || user.idToken().isBlank() || !emergencyFirestore.isEnabled()) return;
        emergencyStateLoading = true;
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try { return emergencyFirestore.loadEmergencyReportsForUser(user.uid(), user.idToken()); }
            catch (Exception exception) {
                System.err.println("EMERGENCY_READ_DIAGNOSTIC action=USER_LOAD sessionUid=" + user.uid() + " result=failed error=" + exception.getMessage());
                return null;
            }
        }).thenAccept(records -> Platform.runLater(() -> {
            emergencyStateLoading = false;
            if (records == null) {
                emergencyFeedback = "Emergency status cannot be loaded because Firestore access was denied. Please contact the administrator.";
                stopEmergencyRefresh();
                if (emergencyPageActive) refreshEmergencyPage();
                return;
            }
            currentEmergency = records.stream().filter(EmergencyReport::unresolved)
                    .max(java.util.Comparator.comparing(EmergencyReport::createdAt)).orElseGet(() -> records.stream()
                            .max(java.util.Comparator.comparing(EmergencyReport::createdAt)).orElse(null));
            if (emergencyPageActive) refreshEmergencyPage();
        }));
    }

    private void startEmergencyRefresh() {
        if (emergencyStatusRefresh == null) {
            emergencyStatusRefresh = new Timeline(new KeyFrame(Duration.seconds(15), event -> loadCurrentEmergencyAsync()));
            emergencyStatusRefresh.setCycleCount(Animation.INDEFINITE);
        }
        emergencyStatusRefresh.play();
    }

    private void stopEmergencyRefresh() {
        if (emergencyStatusRefresh != null) emergencyStatusRefresh.stop();
    }

    private VBox emergencyStatusSection() {
        FlowPane steps = new FlowPane(6, 6);
        for (EmergencyReport.Status status : EmergencyReport.Status.values()) {
            Label item = label(status.name().replace('_', ' '), "emergency-status-step");
            item.getStyleClass().add(status.ordinal() <= currentEmergency.status().ordinal()
                    ? "emergency-status-step-active" : "emergency-status-step-inactive");
            steps.getChildren().add(item);
        }
        VBox section = new VBox(8, emergencySectionTitle("YOUR EMERGENCY STATUS"),
                label("Tracking ID: " + currentEmergency.trackingId() + " | " + currentEmergency.emergencyType().name().replace('_', ' '), "emergency-status-request"),
                label("Priority: " + currentEmergency.priority() + " | Status: " + currentEmergency.status().name().replace('_', ' '), "emergency-status-request"),
                label(currentEmergency.userStatusMessage().isBlank() ? EmergencyReport.userStatusText(currentEmergency.status()) : currentEmergency.userStatusMessage(), "emergency-form-note"),
                currentEmergency.assignedTeamName().isBlank() ? new Label() : label("Assigned team: " + currentEmergency.assignedTeamName(), "emergency-form-note"), steps);
        section.getStyleClass().add("emergency-section");
        return section;
    }

    private VBox demoSosConfirmationCard() {
        Button confirm = new Button("CONFIRM SOS");
        confirm.getStyleClass().add("emergency-danger-button");
        confirm.setOnAction(event -> {
            currentDemoEmergency = new EmergencyDemoRequest("DEMO-EMG-" + System.currentTimeMillis(), "SOS Emergency", "Emergency SOS created locally", 0, "", "CRITICAL", "REPORTED", java.time.LocalDateTime.now());
            sosConfirmationVisible = false;
            emergencyFeedback = "SOS REQUEST CREATED — DEMO MODE. Not sent to emergency services.";
            refreshEmergencyPage();
        });
        Button cancel = new Button("CANCEL");
        cancel.getStyleClass().add("emergency-secondary-button");
        cancel.setOnAction(event -> { sosConfirmationVisible = false; refreshEmergencyPage(); });
        VBox card = new VBox(7, label("Send Emergency SOS?", "emergency-confirm-title"),
                label("This will send your current location and emergency request when backend integration is enabled.", "emergency-confirm-text"),
                new HBox(8, confirm, cancel));
        card.getStyleClass().add("emergency-confirm-card");
        return card;
    }

    private VBox demoEmergencyReportForm() {
        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll("Medical Emergency", "Police / Security", "Fire Emergency", "Crowd / Stampede Risk", "Accident", "Child / Elderly Assistance", "River / Ghat Emergency", "Other");
        category.setPromptText("Emergency Type *");
        category.getStyleClass().add("input-combo");
        TextField description = AppUi.textField("Short Description *");
        TextField peopleAffected = AppUi.textField("People Affected *");
        TextField landmark = AppUi.textField("Optional Landmark");
        Label error = label("", "emergency-validation-error");
        Label location = label("Location: Demo Nashik Location (GPS integration is not enabled).", "emergency-form-note");
        Button submit = new Button("SUBMIT REPORT");
        submit.getStyleClass().add("emergency-primary-button");
        submit.setOnAction(event -> {
            int people;
            try { people = Integer.parseInt(peopleAffected.getText().trim()); } catch (Exception exception) { people = 0; }
            if (category.getValue() == null || category.getValue().isBlank()) { error.setText("Select an emergency type."); return; }
            if (description.getText().trim().isBlank()) { error.setText("Enter a short description."); return; }
            if (people < 1) { error.setText("People affected must be at least 1."); return; }
            String priority = (category.getValue().contains("Fire") || category.getValue().contains("Crowd")) ? "CRITICAL" : "HIGH";
            currentDemoEmergency = new EmergencyDemoRequest("DEMO-RPT-" + System.currentTimeMillis(), category.getValue(), description.getText().trim(), people, landmark.getText().trim(), priority, "REPORTED", java.time.LocalDateTime.now());
            emergencyReportFormVisible = false;
            emergencyFeedback = "Emergency report created in demo mode. Not sent to emergency services.";
            refreshEmergencyPage();
        });
        Button cancel = new Button("CANCEL");
        cancel.getStyleClass().add("emergency-secondary-button");
        cancel.setOnAction(event -> { emergencyReportFormVisible = false; refreshEmergencyPage(); });
        VBox form = new VBox(8, emergencySectionTitle("REPORT EMERGENCY — DEMO MODE"), category, description, peopleAffected, location, landmark, error, new HBox(8, submit, cancel));
        form.getStyleClass().add("emergency-report-form");
        return form;
    }

    private VBox demoEmergencyStatusSection() {
        FlowPane steps = new FlowPane(6, 6);
        for (String step : new String[] { "Reported", "Acknowledged", "Team Dispatched", "Help Arriving", "Resolved" }) {
            Label item = label(step, "emergency-status-step");
            if ("Reported".equals(step)) item.getStyleClass().add("emergency-status-step-active");
            else item.getStyleClass().add("emergency-status-step-inactive");
            steps.getChildren().add(item);
        }
        VBox section = new VBox(8, emergencySectionTitle("YOUR EMERGENCY STATUS"),
                label("Demo mode — not sent to emergency services.", "emergency-form-note"),
                label("Request ID: " + currentDemoEmergency.requestId() + "  •  Priority: " + currentDemoEmergency.priority(), "emergency-status-request"), steps);
        section.getStyleClass().add("emergency-section");
        return section;
    }

    private VBox expandedFacilityDetails(EmergencyServiceDemo facility) {
        VBox details = new VBox(4,
                label("Facility: " + facility.name(), "emergency-expanded-detail"),
                label("Category: " + facility.category(), "emergency-expanded-detail"),
                label("Description: " + displayEmergencyValue(facility.description()), "emergency-expanded-detail"),
                label("Sector: " + displayEmergencyValue(facility.sector()), "emergency-expanded-detail"),
                label("Location: " + displayEmergencyValue(facility.area()), "emergency-expanded-detail"),
                label("Contact: " + displayEmergencyValue(facility.contact()), "emergency-expanded-detail"),
                label("Distance: " + formatEmergencyDistance(emergencyDistanceMeters(facility)), "emergency-expanded-detail"),
                label("Status: " + displayEmergencyValue(facility.status()), "emergency-expanded-detail"));
        details.getStyleClass().add("emergency-expanded-details");
        return details;
    }

    private VBox emergencyRouteSummary(EmergencyServiceDemo facility) {
        Button clear = new Button("CLEAR ROUTE");
        clear.getStyleClass().add("emergency-secondary-button");
        clear.setOnAction(event -> {
            emergencyRouteActive = false;
            if (emergencyMapView != null) emergencyMapView.clearRoute();
            emergencyFeedback = "Route preview cleared.";
            refreshEmergencyPage();
        });
        VBox summary = new VBox(4, label("APPROXIMATE ROUTE PREVIEW", "emergency-route-title"),
                label("To: " + facility.name(), "emergency-expanded-detail"),
                label("Distance: " + formatEmergencyDistance(emergencyDistanceMeters(facility)), "emergency-expanded-detail"),
                label("Route: Straight-line map route (not road navigation)", "emergency-expanded-detail"), clear);
        summary.getStyleClass().add("emergency-route-summary");
        return summary;
    }

    private record EmergencyServiceDemo(String id, String name, String category, String subType, int distanceMeters,
            String status, String contact, String sector, String area, List<String> facilities, String description,
            double latitude, double longitude) { }

    private record EmergencyDemoRequest(String requestId, String category, String description, int peopleAffected,
            String landmark, String priority, String status, java.time.LocalDateTime createdAt) { }

    private Label emergencySectionTitle(String text) {
        return label(text, "emergency-section-title");
    }

    private VBox stayPage() {
        VBox dynamic = new VBox(16);
        Button approved = new Button("View Approved Stays");
        approved.getStyleClass().add("primary-button");
        Button other = new Button("Search Other Stays");
        other.getStyleClass().add("pilgrim-small-action");
        VBox approvedCard = stayChoiceCard("SIMHASTHA CONNECT", "Approved Stays", "Discover trusted accommodation connected with SIMHASTHA CONNECT.", approved, "stay-approved-card");
        VBox otherCard = stayChoiceCard("EXPLORE NASHIK", "Other Stays", "Search hotels, OYO, lodges, dharamshalas, guest houses and homestays across Nashik.", other, "stay-other-card");
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
        return pageShell("Stay", "Find the right accommodation for your Simhastha journey.", stayBackRow(), stayIntroPanel(),
                sectionTitle("How would you like to find your stay?"), cards, dynamic,
                sectionTitle("Why Book with Simhastha Connect?"), stayWhyBookStrip(), stayStatsBand());
    }

    private HBox stayBackRow() {
        Button back = new Button("← Back");
        back.getStyleClass().add("pilgrim-small-action");
        back.setOnAction(event -> showHomePage());
        HBox row = new HBox(back);
        row.getStyleClass().add("stay-back-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox stayIntroPanel() {
        HBox facts = new HBox(10,
                stayIntroFact("\uE8E1", "Trusted Stays", "Verified SIMHASTHA CONNECT listings"),
                stayIntroFact("\uE55F", "Real Location", "Open map before planning travel"),
                stayIntroFact("\uE8B6", "Flexible Search", "Explore hotels, lodges and dharamshalas"));
        facts.getChildren().forEach(item -> HBox.setHgrow(item, Priority.ALWAYS));
        VBox panel = new VBox(facts);
        panel.getStyleClass().add("stay-intro-panel");
        return panel;
    }

    private HBox stayIntroFact(String icon, String title, String detail) {
        VBox copy = new VBox(4, strong(title), muted(detail));
        HBox fact = new HBox(14, label(icon, "stay-feature-icon"), copy);
        fact.setAlignment(Pos.CENTER_LEFT);
        fact.getStyleClass().add("stay-intro-fact");
        return fact;
    }

    private VBox stayChoiceCard(String eyebrow, String title, String detail, Button action, String style) {
        HBox header = new HBox(8, badge(eyebrow), "stay-approved-card".equals(style) ? label("\uE86C", "stay-verified-mark") : new Pane());
        header.setAlignment(Pos.CENTER_LEFT);
        HBox features = "stay-approved-card".equals(style)
                ? stayChoiceFeatures(stayChoiceFeature("\uE8E1", "Trusted Stay Listings"), stayChoiceFeature("\uE0AF", "Property Details"),
                stayChoiceFeature("\uE55F", "Real Location"), stayChoiceFeature("\uE870", "Internal Booking Flow"))
                : stayChoiceFeatures(stayChoiceFeature("\uE7F1", "Hotels"), stayChoiceFeature("OYO", "OYO"),
                stayChoiceFeature("\uE0AF", "Lodges"), stayChoiceFeature("\uE88A", "Dharamshalas"), stayChoiceFeature("\uE88A", "Guest Houses"));
        StackPane artwork = new StackPane();
        artwork.getStyleClass().add("stay-choice-artwork");
        VBox card = new VBox(12, header, strong(title), paragraph(detail), features, createSpacer(), action, artwork);
        card.getStyleClass().addAll("pilgrim-rich-card", "stay-choice-card", style); card.setMinHeight(305); HBox.setHgrow(card, Priority.ALWAYS); return card;
    }

    private HBox stayChoiceFeatures(Node... nodes) {
        HBox row = new HBox(12, nodes);
        row.setAlignment(Pos.CENTER);
        row.getStyleClass().add("stay-choice-features");
        for (Node node : nodes) HBox.setHgrow(node, Priority.ALWAYS);
        return row;
    }

    private VBox stayChoiceFeature(String icon, String title) {
        VBox item = new VBox(6, label(icon, "stay-choice-feature-icon"), label(title, "stay-choice-feature-title"));
        item.setAlignment(Pos.CENTER);
        item.getStyleClass().add("stay-choice-feature");
        return item;
    }

    private HBox stayWhyBookStrip() {
        HBox strip = new HBox(12,
                stayBenefit("\uE8E1", "Trusted & Verified", "All listings are verified by our team"),
                stayBenefit("\uE55F", "Exact Location", "View exact location on map"),
                stayBenefit("\uE227", "Best Prices", "Competitive prices & great deals"),
                stayBenefit("\uE0CD", "24x7 Support", "We are here to help you anytime"),
                stayBenefit("\uE86C", "Secure Booking", "Safe, easy & secure booking"));
        strip.getStyleClass().add("stay-benefit-strip");
        strip.getChildren().forEach(item -> HBox.setHgrow(item, Priority.ALWAYS));
        return strip;
    }

    private HBox stayBenefit(String icon, String title, String detail) {
        HBox item = new HBox(10, label(icon, "stay-benefit-icon"), new VBox(2, strong(title), muted(detail)));
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("stay-benefit-card");
        return item;
    }

    private HBox stayStatsBand() {
        HBox band = new HBox(18,
                stayStat("\uE7F1", "750+", "Verified Stays"),
                stayStat("\uE7EF", "50K+", "Happy Pilgrims"),
                stayStat("\uE838", "4.8", "Average Rating"),
                stayStat("\uE55F", "100+", "Locations"),
                stayStat("\uE0CD", "24x7", "Support"));
        band.getStyleClass().add("stay-stats-band");
        band.getChildren().forEach(item -> HBox.setHgrow(item, Priority.ALWAYS));
        return band;
    }

    private HBox stayStat(String icon, String value, String title) {
        HBox item = new HBox(10, label(icon, "stay-stat-icon"), new VBox(1, label(value, "stay-stat-value"), label(title, "stay-stat-title")));
        item.setAlignment(Pos.CENTER);
        item.getStyleClass().add("stay-stat-item");
        return item;
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
                ImageView image = stayImage(stay, 116, 72);
                VBox detailsCopy = new VBox(3, strong(stay.name()), muted(stay.category() + " • " + stay.area()),
                        muted(stay.priceInfo().isBlank() ? "Price available during connected booking" : stay.priceInfo()));
                HBox row = new HBox(10, image, detailsCopy, createSpacer(), details, map, book);
                if (removable) { Button remove = new Button("Remove"); remove.getStyleClass().add("pilgrim-small-action"); remove.setOnAction(event -> { collection.remove(stay.stableId()); dialog.close(); }); row.getChildren().add(remove); }
                row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("stay-collection-row"); content.getChildren().add(row);
            }
        }
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(460);
        dialog.getDialogPane().setContent(scroll); dialog.getDialogPane().setPrefSize(940, 560); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); AppUi.styleDialog(dialog, root.getScene() == null ? null : root.getScene().getWindow(), "stay-dialog-pane", ButtonType.CLOSE); dialog.showAndWait();
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
        content.getStyleClass().add("stay-booking-summary"); dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); AppUi.styleDialog(dialog, root.getScene() == null ? null : root.getScene().getWindow(), "stay-dialog-pane", ButtonType.CLOSE); dialog.showAndWait();
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
        ImageView image = stayImage(stay, 390, 190);
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
                        blankTo(business.location, "Location details available during connected booking."),
                        firstNonBlank(business.coverPhotoUrl, business.logoUrl), "/images/welcome-light.png",
                        business.businessName + " " + blankTo(business.location, "Nashik"), stayBudgetFrom(business.priceRange), List.of(),
                        blankTo(business.description, "Approved accommodation listing."), false, business.priceRange, business.businessId,
                        business.galleryImages);
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

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
        ImageView image = stayImage(stay, 650, 265);
        FlowPane facilities = new FlowPane(6, 6); stay.facilities().forEach(item -> facilities.getChildren().add(badge(item)));
        FlowPane gallery = new FlowPane(8, 8); stay.galleryImages().forEach(photo -> gallery.getChildren().add(ImageMediaHelper.imageView(photo.url(), 140, 92))); if (gallery.getChildren().isEmpty()) gallery.getChildren().add(muted("No additional stay photos available yet."));
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
                sectionTitle("Photos"), gallery, sectionTitle("Location"), new HBox(8, save, map), option);
        content.setPadding(new Insets(4));
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(560); scroll.getStyleClass().add("stay-dialog-scroll");
        dialog.getDialogPane().setContent(scroll); dialog.getDialogPane().setPrefSize(900, 650); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE, continueBooking); AppUi.styleDialog(dialog, root.getScene() == null ? null : root.getScene().getWindow(), "stay-dialog-pane", continueBooking);
        dialog.showAndWait().filter(choice -> choice == continueBooking).ifPresent(choice -> showStayBookingDetails(stay, criteria));
    }

    private void showStayBookingDetails(StayItem stay, StayCriteria criteria) {
        if (!criteria.hasDates()) { showInfo("Select dates", "Choose a valid check-in and check-out date before continuing to booking."); return; }
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Complete Your Stay Booking"); dialog.setHeaderText(null);
        TextField name = AppUi.textField("Full Name");
        TextField mobile = AppUi.textField("Mobile Number");
        TextField email = AppUi.textField("Email Address");
        TextField request = AppUi.textField("Special Request (optional)");
        ImageView image = stayImage(stay, 170, 105);
        VBox property = new VBox(4, strong(stay.name()), muted(stay.category() + " • " + stay.area()), muted(stay.address()));
        HBox propertySummary = new HBox(14, image, property); propertySummary.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(14, propertySummary, sectionTitle("Trip Details"), muted(staySearchSummary(criteria)),
                sectionTitle("Guest Details"), name, mobile, email, request,
                muted("Personal details are used only for this booking request and are not logged by the Stay page."));
        content.getStyleClass().add("stay-booking-summary"); dialog.getDialogPane().setContent(content); dialog.getDialogPane().setPrefSize(760, 620);
        ButtonType back = new ButtonType("Back", javafx.scene.control.ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType review = new ButtonType("Review Booking"); dialog.getDialogPane().getButtonTypes().addAll(back, review); AppUi.styleDialog(dialog, root.getScene() == null ? null : root.getScene().getWindow(), "stay-dialog-pane", review);
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
        ImageView image = stayImage(stay, 180, 108);
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
        ButtonType proceed = new ButtonType("Proceed to Payment"); dialog.getDialogPane().getButtonTypes().addAll(back, proceed); AppUi.styleDialog(dialog, root.getScene() == null ? null : root.getScene().getWindow(), "stay-dialog-pane", proceed);
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
        private static StayCriteria empty() { return new StayCriteria("All Areas", null, null, "1 Room • 2 Guests", "Any Budget"); }
        private boolean hasDates() { return checkIn != null && checkOut != null && checkOut.isAfter(checkIn); }
        private long nights() { return hasDates() ? java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut) : 0; }
        private StayCriteria withBudget(String secondaryBudget) { return new StayCriteria(location, checkIn, checkOut, guests, secondaryBudget == null ? "Any Budget" : secondaryBudget); }
    }

    private record StayGuestDetails(String name, String mobile, String email, String specialRequest) { }

    private record StayItem(String name, String category, String area, String address, String imageUrl, String localImagePath, String mapQuery,
            String budgetCategory, java.util.List<String> facilities, String shortDescription, boolean locallyApprovedSeed, String priceInfo,
            String stableId, java.util.List<com.simhastha.model.CloudImage> galleryImages) {
        private StayItem(String name, String category, String area, String address, String imageUrl, String localImagePath, String mapQuery,
                String budgetCategory, java.util.List<String> facilities, String shortDescription, boolean locallyApprovedSeed) {
            this(name, category, area, address, imageUrl, localImagePath, mapQuery, budgetCategory, facilities, shortDescription, locallyApprovedSeed, "", "", List.of());
        }

        private StayItem(String name, String category, String area, String address, String imageUrl, String localImagePath, String mapQuery,
                String budgetCategory, java.util.List<String> facilities, String shortDescription, boolean locallyApprovedSeed, String priceInfo) {
            this(name, category, area, address, imageUrl, localImagePath, mapQuery, budgetCategory, facilities, shortDescription, locallyApprovedSeed, priceInfo, "", List.of());
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
        schedulePageActive = true;
        scheduleDateArea = new VBox();
        scheduleFeatureArea = new VBox();
        scheduleFilterArea = new VBox();
        scheduleTimelineArea = new VBox();
        VBox content = new VBox(16, topControls(), scheduleHeader(), scheduleDateArea, scheduleFeatureArea,
                scheduleFilterArea, scheduleTimelineArea, scheduleInfoStrip());
        content.getStyleClass().addAll("pilgrim-dashboard-main", "schedule-page");
        content.setPadding(new Insets(12, 22, 28, 12));
        startScheduleRefresh();
        scheduleService.addLocalChangeListener(scheduleLocalChangeListener);
        refreshSchedule();
        return content;
    }

    private VBox scheduleHeader() {
        Label title = label("All Day Schedule", "schedule-page-title");
        Label subtitle = label("Plan your day at Simhastha", "schedule-page-subtitle");
        Region divider = new Region();
        divider.getStyleClass().add("schedule-divider");
        return new VBox(3, title, subtitle, divider);
    }

    private HBox scheduleDateControls() {
        Label calendar = AppUi.symbolIcon("\uE787", "schedule-date-icon");
        boolean today = selectedScheduleDate.equals(java.time.LocalDate.now());
        String dateText = today ? "Today, " + formatScheduleDate(selectedScheduleDate)
                : selectedScheduleDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"));
        VBox dateCopy = new VBox(1, label(today ? "TODAY" : selectedScheduleDate.getDayOfWeek().toString(),
                "schedule-date-caption"), label(dateText, "schedule-date-value"));
        DatePicker picker = new DatePicker(selectedScheduleDate);
        picker.getStyleClass().add("schedule-date-picker");
        picker.setOnAction(event -> {
            if (picker.getValue() != null) {
                changeScheduleDate(picker.getValue());
            }
        });
        HBox dateCard = new HBox(9, calendar, dateCopy, picker);
        dateCard.getStyleClass().add("schedule-date-card");
        dateCard.setAlignment(Pos.CENTER_LEFT);
        Button previous = scheduleIconButton("\uE72B", "Previous day");
        Button next = scheduleIconButton("\uE72A", "Next day");
        previous.setOnAction(event -> changeScheduleDate(selectedScheduleDate.minusDays(1)));
        next.setOnAction(event -> changeScheduleDate(selectedScheduleDate.plusDays(1)));
        Button mySchedule = new Button("*  My Schedule");
        mySchedule.getStyleClass().add("schedule-my-button");
        mySchedule.setOnAction(event -> showMySchedule());
        HBox row = new HBox(8, dateCard, previous, next, createSpacer(), mySchedule);
        row.getStyleClass().add("schedule-date-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox scheduleFilterBar() {
        HBox filters = new HBox(7);
        String[] categories = { "All Events", "Snan", "Aarti", "Akhada", "Samaj", "Cultural", "Government", "Important" };
        for (String category : categories) {
            boolean selected = selectedScheduleCategory == null && "All Events".equals(category)
                    || selectedScheduleCategory != null && selectedScheduleCategory.label().equals(category);
            filters.getChildren().add(createFilterButton(category, selected));
        }
        Button filter = new Button("\uE71C  Filter");
        filter.getStyleClass().add("schedule-filter-button");
        filter.setOnAction(event -> showScheduleDialog("Schedule filter",
                "Use the category filters to narrow the displayed events."));
        HBox row = new HBox(9, filters, createSpacer(), filter);
        row.getStyleClass().add("schedule-filter-bar");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button createFilterButton(String text, boolean selected) {
        Button button = new Button(text);
        button.getStyleClass().addAll("schedule-filter-button",
                selected ? "schedule-filter-selected" : "schedule-filter-idle");
        button.setOnAction(event -> {
            selectedScheduleCategory = "All Events".equals(text) ? null : ScheduleCategory.valueOf(text.toUpperCase());
            refreshTimeline();
            if (scheduleFilterArea != null) {
                scheduleFilterArea.getChildren().setAll(scheduleFilterBar());
            }
        });
        return button;
    }

    private void refreshSchedule() {
        if (!schedulePageActive || scheduleDateArea == null || scheduleRefreshInFlight) {
            return;
        }
        final long generation = scheduleRefreshGeneration;
        final java.time.LocalDate requestedDate = selectedScheduleDate;
        scheduleRefreshInFlight = true;
        scheduleDateArea.getChildren().setAll(scheduleDateControls());
        if (loadedScheduleEvents.isEmpty()) {
            scheduleFeatureArea.getChildren().setAll(infoPanel("Loading schedule", "Loading official schedule..."));
        }
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return new ScheduleLoad(scheduleService.eventsForDate(requestedDate),
                        scheduleService.alertsForDate(requestedDate));
            } catch (Exception exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }, SCHEDULE_REFRESH_EXECUTOR).whenComplete((loaded, error) -> Platform.runLater(() -> {
            if (!schedulePageActive) {
                return;
            }
            if (generation != scheduleRefreshGeneration) {
                scheduleRefreshInFlight = false;
                refreshSchedule();
                return;
            }
            scheduleRefreshInFlight = false;
            if (error == null) {
                loadedScheduleEvents = loaded.events();
                loadedScheduleAlerts = loaded.alerts();
                savedScheduleEvents.keySet().removeIf(id -> loadedScheduleEvents.stream()
                        .noneMatch(event -> event.id().equals(id)));
                loadedScheduleEvents.forEach(event -> {
                    if (savedScheduleEvents.containsKey(event.id())) {
                        savedScheduleEvents.put(event.id(), event);
                    }
                });
            } else {
                LOGGER.fine("Schedule could not be refreshed for " + requestedDate + ": " + error.getCause());
            }
            scheduleFeatureArea.getChildren().setAll(functionalFeatureCards());
            scheduleFilterArea.getChildren().setAll(scheduleFilterBar());
            refreshTimeline();
        }));
    }

    private void refreshTimeline() {
        if (scheduleTimelineArea != null) {
            scheduleTimelineArea.getChildren().setAll(functionalScheduleTimeline());
        }
    }

    private void changeScheduleDate(java.time.LocalDate date) {
        selectedScheduleDate = date;
        scheduleRefreshGeneration++;
        scheduleRefreshInFlight = false;
        refreshSchedule();
    }

    private HBox functionalFeatureCards() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Optional<com.simhastha.schedule.ScheduleEvent> live = loadedScheduleEvents.stream()
                .filter(item -> scheduleService.statusFor(item, now) == ScheduleStatus.LIVE_NOW)
                .sorted(Comparator.comparing(com.simhastha.schedule.ScheduleEvent::important).reversed()
                        .thenComparing(com.simhastha.schedule.ScheduleEvent::startTime))
                .findFirst();
        Optional<com.simhastha.schedule.ScheduleEvent> next = findNextEvent(now);
        ScheduleAlert alert = loadedScheduleAlerts.stream().filter(ScheduleAlert::active)
                .filter(item -> !selectedScheduleDate.equals(java.time.LocalDate.now())
                        || item.startTime() == null || !java.time.LocalTime.now().isBefore(item.startTime()))
                .filter(item -> item.endTime() == null || !selectedScheduleDate.equals(java.time.LocalDate.now())
                        || !java.time.LocalTime.now().isAfter(item.endTime()))
                .sorted(Comparator.comparingInt((ScheduleAlert item) -> switch (item.severity().toUpperCase()) {
                    case "CRITICAL" -> 3;
                    case "WARNING" -> 2;
                    default -> 1;
                }).reversed())
                .findFirst().orElse(null);
        VBox liveCard = live.map(item -> dynamicFeatureCard("LIVE NOW", item.title(), item.location(),
                formatEventTime(item), "View Route", "schedule-feature-live", () -> handleRoute(item)))
                .orElseGet(() -> dynamicFeatureCard("LIVE NOW", "No event live right now",
                        "Check upcoming schedule below", "", "View Schedule", "schedule-feature-live", () -> { }));
        VBox nextCard = next.map(item -> dynamicFeatureCard("UP NEXT", item.title(), item.location(),
                formatEventTime(item),
                selectedScheduleDate.equals(now.toLocalDate())
                        ? formatCountdown(java.time.Duration.between(now,
                                java.time.LocalDateTime.of(item.date(), item.startTime())))
                        : "Upcoming",
                "schedule-feature-next", () -> showEventDetails(item)))
                .orElseGet(() -> dynamicFeatureCard("UP NEXT", "No more events today",
                        "Check another date for events", "", "Schedule", "schedule-feature-next", () -> { }));
        VBox alertCard = alert == null ? dynamicFeatureCard("IMPORTANT UPDATE",
                "No important updates for this date.", "", "", "Details", "schedule-feature-alert", () -> { })
                : dynamicFeatureCard("IMPORTANT UPDATE", alert.title(), alert.location(), "",
                        "View Details", "schedule-feature-alert", () -> showAlertDetails(alert));
        HBox row = new HBox(12, liveCard, nextCard, alertCard);
        row.getStyleClass().add("schedule-feature-row");
        return row;
    }

    private VBox dynamicFeatureCard(String heading, String title, String place, String detail, String action,
            String style, Runnable handler) {
        Button button = new Button(action);
        button.getStyleClass().add("schedule-feature-action");
        button.setOnAction(event -> handler.run());
        VBox card = new VBox(7, label(heading, "schedule-feature-eyebrow"), label(title, "schedule-feature-title"),
                label(place, "schedule-feature-detail"), label(detail, "schedule-feature-time"), button);
        card.getStyleClass().addAll("schedule-feature-card", style);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private GridPane functionalScheduleTimeline() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("schedule-timeline-grid");
        ColumnConstraints first = new ColumnConstraints();
        first.setPercentWidth(50);
        first.setHgrow(Priority.ALWAYS);
        ColumnConstraints second = new ColumnConstraints();
        second.setPercentWidth(50);
        second.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(first, second);
        SchedulePeriod[] periods = SchedulePeriod.values();
        for (int index = 0; index < periods.length; index++) {
            grid.add(functionalScheduleSection(periods[index]), index % 2, index / 2);
        }
        return grid;
    }

    private VBox functionalScheduleSection(SchedulePeriod period) {
        List<com.simhastha.schedule.ScheduleEvent> items = filteredScheduleEvents().stream()
                .filter(item -> SchedulePeriod.from(item.startTime()) == period).toList();
        VBox rows = new VBox(8);
        if (items.isEmpty()) {
            rows.getChildren().add(label("No events scheduled", "schedule-empty-text"));
        } else {
            items.forEach(item -> rows.getChildren().add(functionalEventRow(item)));
        }
        VBox card = new VBox(12, new VBox(2, label(period.title(), "schedule-section-title"),
                label(period.duration(), "schedule-section-duration")), rows);
        card.getStyleClass().add("schedule-section-card");
        return card;
    }

    private HBox functionalEventRow(com.simhastha.schedule.ScheduleEvent item) {
        ScheduleStatus status = scheduleService.statusFor(item, java.time.LocalDateTime.now());
        VBox time = new VBox(1, label(formatTime(item.startTime()), "schedule-event-time"),
                label("- " + formatTime(item.endTime()), "schedule-event-end"));
        StackPane node = new StackPane(label(item.category().label().substring(0, 1), "schedule-event-icon"));
        node.getStyleClass().add("schedule-event-node");
        VBox details = new VBox(2, label(item.title(), "schedule-event-name"),
                label(item.location(), "schedule-event-location"));
        Button detailsButton = new Button("Details");
        detailsButton.getStyleClass().add("schedule-event-action");
        detailsButton.setOnAction(event -> showEventDetails(item));
        Button routeButton = new Button("Route");
        routeButton.getStyleClass().add("schedule-event-action");
        routeButton.setOnAction(event -> handleRoute(item));
        boolean saved = savedScheduleEvents.containsKey(item.id());
        Button bell = new Button(saved ? "Saved" : AppUi.notificationBellGlyph());
        bell.getStyleClass().addAll("schedule-icon-button", "schedule-event-bell");
        if (saved) {
            bell.getStyleClass().add("schedule-event-bell-saved");
        }
        bell.setOnAction(event -> toggleSavedEvent(item));
        HBox row = new HBox(8, time, node, details, createStatusBadge(status), detailsButton, routeButton, bell);
        row.getStyleClass().add("schedule-event-row");
        if (status == ScheduleStatus.LIVE_NOW) {
            row.getStyleClass().add("schedule-event-live");
        }
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(details, Priority.ALWAYS);
        return row;
    }

    private List<com.simhastha.schedule.ScheduleEvent> filteredScheduleEvents() {
        return loadedScheduleEvents.stream()
                .filter(item -> selectedScheduleCategory == null || item.category() == selectedScheduleCategory
                        || selectedScheduleCategory == ScheduleCategory.IMPORTANT && item.important())
                .sorted(Comparator.comparing(com.simhastha.schedule.ScheduleEvent::startTime))
                .toList();
    }

    private Optional<com.simhastha.schedule.ScheduleEvent> findNextEvent(java.time.LocalDateTime now) {
        return loadedScheduleEvents.stream()
                .filter(item -> !item.cancelled()
                        && (item.date().isAfter(now.toLocalDate())
                                || item.date().equals(now.toLocalDate()) && item.startTime().isAfter(now.toLocalTime())))
                .sorted(Comparator.comparing(com.simhastha.schedule.ScheduleEvent::date)
                        .thenComparing(com.simhastha.schedule.ScheduleEvent::startTime))
                .findFirst();
    }

    private void toggleSavedEvent(com.simhastha.schedule.ScheduleEvent item) {
        if (savedScheduleEvents.containsKey(item.id())) {
            savedScheduleEvents.remove(item.id());
        } else {
            savedScheduleEvents.put(item.id(), item);
        }
        refreshTimeline();
    }

    private void handleRoute(com.simhastha.schedule.ScheduleEvent item) {
        if (item.latitude() != null || item.longitude() != null) {
            if (item.latitude() == null || item.longitude() == null
                    || !NashikLocationRegistry.validNashikPoint(item.latitude(), item.longitude())) {
                showScheduleDialog("Location unavailable",
                        "Location is outside the supported Nashik Simhastha area.");
                return;
            }
        }
        NashikLocationRegistry.resolve(item.locationId(), item.location(), item.latitude(), item.longitude())
                .ifPresentOrElse(point -> showScheduleLocationMap(item, point),
                        () -> showScheduleDialog("Location unavailable",
                                "Accurate location is not available for this event yet."));
    }

    private void showScheduleLocationMap(com.simhastha.schedule.ScheduleEvent item,
            NashikLocationRegistry.LocationPoint point) {
        stopScheduleRefresh();
        Button back = new Button("< Back to All Day Schedule");
        back.getStyleClass().add("map-secondary-button");
        back.setOnAction(event -> showModulePage("schedule"));
        Button centerMarker = new Button("Center Marker");
        centerMarker.getStyleClass().add("map-secondary-button");
        Button copyCoordinates = new Button("Copy Coordinates");
        copyCoordinates.getStyleClass().add("map-secondary-button");
        copyCoordinates.setOnAction(event -> copyRouteCoordinates(item, point));
        Button getRoute = new Button("Get Route");
        getRoute.getStyleClass().add("map-open-button");
        getRoute.setOnAction(event -> openUrl(googleMapsDirectionsUrl(point)));
        Button openMap = new Button("Open in OpenStreetMap");
        openMap.getStyleClass().add("map-open-button");
        openMap.setOnAction(event -> openUrl("https://www.openstreetmap.org/?mlat=" + point.latitude()
                + "&mlon=" + point.longitude() + "#map=16/" + point.latitude() + "/" + point.longitude()));
        HBox tools = new HBox(8, back, createSpacer(), centerMarker, copyCoordinates, getRoute, openMap);
        tools.getStyleClass().add("schedule-route-toolbar");
        tools.setAlignment(Pos.CENTER_LEFT);

        WebView mapView = new WebView();
        mapView.getStyleClass().add("schedule-map-webview");
        mapView.setContextMenuEnabled(false);
        mapView.setMinHeight(430);
        mapView.setPrefHeight(470);
        mapView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        WebEngine engine = mapView.getEngine();
        Label loading = label("Loading OpenStreetMap...", "schedule-map-loading");
        Label fallback = label("Map could not be loaded. Use Get Route to open navigation in your browser.",
                "schedule-map-loading");
        fallback.setVisible(false);
        fallback.setManaged(false);
        StackPane map = new StackPane(mapView, loading, fallback);
        map.getStyleClass().add("schedule-map-shell");
        map.setMinHeight(430);
        map.setPrefHeight(470);
        map.setMaxWidth(Double.MAX_VALUE);
        mapView.prefWidthProperty().bind(map.widthProperty());
        mapView.prefHeightProperty().bind(map.heightProperty());
        VBox.setVgrow(map, Priority.ALWAYS);
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                loading.setVisible(false);
                loading.setManaged(false);
                refreshLeafletMapSize(engine);
            } else if (newState == javafx.concurrent.Worker.State.FAILED
                    || newState == javafx.concurrent.Worker.State.CANCELLED) {
                loading.setVisible(false);
                loading.setManaged(false);
                fallback.setVisible(true);
                fallback.setManaged(true);
            }
        });
        engine.setOnError(event -> {
            loading.setVisible(false);
            loading.setManaged(false);
            fallback.setVisible(true);
            fallback.setManaged(true);
        });
        engine.loadContent(scheduleMapHtml(item, point));
        centerMarker.setOnAction(event -> {
            try {
                engine.executeScript("if (window.simhasthaMap) { window.simhasthaMap.setView(["
                        + point.latitude() + "," + point.longitude() + "], 16); window.simhasthaMarker.openPopup(); }");
            } catch (RuntimeException ignored) {
                refreshLeafletMapSize(engine);
            }
        });

        VBox content = pageShell("Event Location", "Nashik Simhastha schedule destination", tools, map,
                scheduleRouteQuickPanel(item, point), selectedScheduleEventCard(item));
        root.setCenter(scroll(content));
        Platform.runLater(() -> refreshLeafletMapSize(engine));
    }

    private void refreshLeafletMapSize(WebEngine engine) {
        Runnable refresh = () -> {
            try {
                engine.executeScript("if (window.simhasthaMap) { window.simhasthaMap.invalidateSize(true); }");
            } catch (RuntimeException ignored) {
            }
        };
        Platform.runLater(refresh);
        new Timeline(new KeyFrame(Duration.millis(250), event -> refresh.run()),
                new KeyFrame(Duration.millis(800), event -> refresh.run())).play();
    }

    private HBox scheduleRouteQuickPanel(com.simhastha.schedule.ScheduleEvent item,
            NashikLocationRegistry.LocationPoint point) {
        HBox panel = new HBox(10,
                createInfoCard("\uE81D", "Destination", item.location(), item.title()),
                createInfoCard("\uE121", "Event Time", formatTime(item.startTime()), formatTime(item.endTime())),
                createInfoCard("\uE8F4", "Coordinates", String.format(java.util.Locale.US, "%.5f, %.5f",
                        point.latitude(), point.longitude()), "Nashik Simhastha area"),
                createInfoCard("\uE8A7", "Browser Route", "Google Maps", "Uses your current location"));
        panel.getStyleClass().add("schedule-route-summary");
        panel.setAlignment(Pos.CENTER_LEFT);
        return panel;
    }

    private VBox selectedScheduleEventCard(com.simhastha.schedule.ScheduleEvent item) {
        StringBuilder detail = new StringBuilder();
        detail.append(item.location())
                .append("\nDate: ").append(formatScheduleDate(item.date()))
                .append("\nTime: ").append(formatEventTime(item))
                .append("\nCategory: ").append(item.category().label());
        if (item.organizer() != null && !item.organizer().isBlank()) {
            detail.append("\nOrganizer: ").append(item.organizer());
        }
        if (item.note() != null && !item.note().isBlank()) {
            detail.append("\nImportant note: ").append(item.note());
        }
        VBox card = new VBox(7, sectionTitle("Selected Event"), strong(item.title()), paragraph(detail.toString()));
        card.getStyleClass().addAll("pilgrim-panel", "schedule-selected-event-card");
        return card;
    }

    private void copyRouteCoordinates(com.simhastha.schedule.ScheduleEvent item,
            NashikLocationRegistry.LocationPoint point) {
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(String.format(java.util.Locale.US, "%s - %s: %.6f, %.6f",
                item.title(), item.location(), point.latitude(), point.longitude()));
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        showScheduleDialog("Coordinates copied", item.title() + "\n" + item.location() + "\n"
                + String.format(java.util.Locale.US, "%.6f, %.6f", point.latitude(), point.longitude()));
    }

    private String googleMapsDirectionsUrl(NashikLocationRegistry.LocationPoint point) {
        return "https://www.google.com/maps/dir/?api=1&destination=" + point.latitude() + "," + point.longitude();
    }

    private String scheduleMapHtml(com.simhastha.schedule.ScheduleEvent item,
            NashikLocationRegistry.LocationPoint point) {
        String popup = "<strong>" + htmlEscape(item.title()) + "</strong><br>"
                + htmlEscape(item.location()) + "<br>"
                + htmlEscape(formatEventTime(item)) + "<br>"
                + htmlEscape(item.category().label());
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
                  <style>
                    html, body, #map { width: 100%%; height: 100%%; margin: 0; padding: 0; overflow: hidden; }
                    body { background: #fff7e8; }
                    .leaflet-container { background: #fff7e8; outline: 0; overflow: hidden; font: 12px/1.5 "Segoe UI", Arial, sans-serif; }
                    .leaflet-pane, .leaflet-tile, .leaflet-marker-icon, .leaflet-marker-shadow,
                    .leaflet-tile-container, .leaflet-pane > svg, .leaflet-pane > canvas,
                    .leaflet-zoom-box, .leaflet-image-layer, .leaflet-layer { position: absolute; left: 0; top: 0; }
                    .leaflet-container img.leaflet-tile { max-width: none !important; max-height: none !important; width: 256px; height: 256px; }
                    .leaflet-tile-pane { z-index: 200; }
                    .leaflet-overlay-pane { z-index: 400; }
                    .leaflet-shadow-pane { z-index: 500; }
                    .leaflet-marker-pane { z-index: 600; }
                    .leaflet-tooltip-pane { z-index: 650; }
                    .leaflet-popup-pane { z-index: 700; }
                    .leaflet-control { position: relative; z-index: 800; pointer-events: auto; }
                    .leaflet-top, .leaflet-bottom { position: absolute; z-index: 1000; pointer-events: none; }
                    .leaflet-top { top: 0; }
                    .leaflet-right { right: 0; }
                    .leaflet-bottom { bottom: 0; }
                    .leaflet-left { left: 0; }
                    .leaflet-control { float: left; clear: both; }
                    .leaflet-right .leaflet-control { float: right; }
                    .leaflet-top .leaflet-control { margin-top: 10px; }
                    .leaflet-left .leaflet-control { margin-left: 10px; }
                    .leaflet-right .leaflet-control { margin-right: 10px; }
                    .leaflet-bottom .leaflet-control { margin-bottom: 10px; }
                    .leaflet-control-zoom a { background: #fff8ec; color: #5a0a0a; display: block; width: 30px; height: 30px; line-height: 30px; text-align: center; text-decoration: none; border-bottom: 1px solid #e4bd7d; font-size: 18px; font-weight: bold; }
                    .leaflet-control-zoom { border: 1px solid #d98c27; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(94, 47, 11, .18); }
                    .leaflet-control-attribution { background: rgba(255, 248, 236, .86); color: #5a0a0a; padding: 2px 7px; }
                    .leaflet-popup { position: absolute; text-align: center; margin-bottom: 20px; }
                    .leaflet-popup-content-wrapper { background: #fff8ec; color: #4a160d; border-radius: 8px; box-shadow: 0 3px 14px rgba(54, 20, 3, .22); border: 1px solid #e0ad60; }
                    .leaflet-popup-content { color: #4a160d; font-family: "Segoe UI", Arial, sans-serif; line-height: 1.45; margin: 10px 12px; min-width: 150px; }
                    .leaflet-popup-tip-container { width: 40px; height: 20px; position: absolute; left: 50%%; margin-left: -20px; overflow: hidden; pointer-events: none; }
                    .leaflet-popup-tip { background: #fff8ec; width: 14px; height: 14px; padding: 1px; margin: -8px auto 0; transform: rotate(45deg); border-right: 1px solid #e0ad60; border-bottom: 1px solid #e0ad60; }
                    .leaflet-popup-close-button { position: absolute; top: 0; right: 0; padding: 4px 7px 0 0; color: #8b3d0d; text-decoration: none; font-weight: bold; }
                    .simhastha-marker { width: 34px !important; height: 34px !important; margin-left: -17px !important; margin-top: -34px !important; }
                    .simhastha-marker-pin { width: 26px; height: 26px; border-radius: 50%% 50%% 50%% 0; background: #e87505; transform: rotate(-45deg); border: 3px solid #fff7e8; box-shadow: 0 2px 8px rgba(80, 18, 5, .35); }
                    .simhastha-marker-pin:after { content: ""; position: absolute; width: 8px; height: 8px; border-radius: 50%%; background: #5a0a0a; top: 6px; left: 6px; }
                  </style>
                </head>
                <body>
                  <div id="map"></div>
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <script>
                    window.onerror = function () {
                      document.body.innerHTML = '<div style="font-family: Segoe UI, Arial, sans-serif; color: #5a0a0a; padding: 18px;">Map could not be loaded. Use Get Route to open navigation in your browser.</div>';
                    };
                    const map = L.map('map', { zoomControl: true }).setView([%s, %s], 16);
                    window.simhasthaMap = map;
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      maxZoom: 19,
                      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    }).addTo(map);
                    const markerIcon = L.divIcon({
                      className: 'simhastha-marker',
                      html: '<div class="simhastha-marker-pin"></div>',
                      iconSize: [34, 34],
                      iconAnchor: [17, 34],
                      popupAnchor: [0, -30]
                    });
                    const marker = L.marker([%s, %s], { icon: markerIcon }).addTo(map).bindPopup(%s);
                    window.simhasthaMarker = marker;
                    marker.openPopup();
                    window.addEventListener('resize', function () { map.invalidateSize(true); });
                    setTimeout(function () { map.invalidateSize(true); }, 150);
                    setTimeout(function () { map.invalidateSize(true); }, 700);
                  </script>
                </body>
                </html>
                """.formatted(point.latitude(), point.longitude(), point.latitude(), point.longitude(),
                jsString(popup));
    }

    private String htmlEscape(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String jsString(String value) {
        String escaped = (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("</", "<\\/");
        return "'" + escaped + "'";
    }

    private String formatScheduleDate(java.time.LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"));
    }

    private String formatTime(java.time.LocalTime time) {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
    }

    private String formatEventTime(com.simhastha.schedule.ScheduleEvent item) {
        return formatTime(item.startTime()) + " - " + formatTime(item.endTime());
    }

    private String formatCountdown(java.time.Duration duration) {
        long minutes = Math.max(0, duration.toMinutes());
        return minutes < 60 ? "Starting in " + minutes + "m"
                : "Starting in " + minutes / 60 + "h " + minutes % 60 + "m";
    }

    private Label createStatusBadge(ScheduleStatus status) {
        String text = status == ScheduleStatus.LIVE_NOW ? "Live Now"
                : status.name().substring(0, 1) + status.name().substring(1).toLowerCase().replace('_', ' ');
        Label badge = label(text, "schedule-status-badge");
        badge.getStyleClass().add(switch (status) {
            case LIVE_NOW -> "schedule-status-live";
            case COMPLETED -> "schedule-status-completed";
            case CANCELLED -> "schedule-status-cancelled";
            default -> "schedule-status-upcoming";
        });
        return badge;
    }

    private void startScheduleRefresh() {
        stopScheduleRefresh();
        schedulePageActive = true;
        scheduleRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> refreshSchedule()));
        scheduleRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        scheduleRefreshTimeline.play();
    }

    private HBox scheduleInfoStrip() {
        HBox strip = new HBox(10,
                createInfoCard("\uE706", "Crowd Status", "Moderate", "Ramkund Area"),
                createInfoCard("\uE708", "Weather", "26 C", "Partly Cloudy"),
                createInfoCard("\uE7D4", "Drinking Water", "Available", "At all ghats"),
                createInfoCard("\uE95E", "Medical Help", "24x7", "Near Ramkund"),
                createInfoCard("\uE717", "Emergency Contact", "112 / 108", "Police / Ambulance"));
        strip.getStyleClass().add("schedule-info-strip");
        return strip;
    }

    private VBox createInfoCard(String icon, String title, String value, String detail) {
        VBox card = new VBox(3, AppUi.symbolIcon(icon, "schedule-info-icon"), label(title, "schedule-info-title"),
                label(value, "schedule-info-value"), label(detail, "schedule-info-detail"));
        card.getStyleClass().add("schedule-info-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Button scheduleIconButton(String icon, String tooltip) {
        Button button = new Button(icon);
        button.getStyleClass().add("schedule-icon-button");
        button.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return button;
    }

    private void showEventDetails(com.simhastha.schedule.ScheduleEvent item) {
        showScheduleDialog(item.title(),
                "Category: " + item.category().label()
                        + "\nDate: " + formatScheduleDate(item.date())
                        + "\nTime: " + formatEventTime(item)
                        + "\nLocation: " + item.location()
                        + "\nOrganizer: " + item.organizer()
                        + "\nStatus: " + createStatusBadge(scheduleService.statusFor(item,
                                java.time.LocalDateTime.now())).getText()
                        + "\n\n" + item.description()
                        + "\n\nNote: " + item.note());
    }

    private void showAlertDetails(ScheduleAlert alert) {
        showScheduleDialog(alert.severity() + " Update",
                alert.title() + "\n\n" + alert.message() + "\n\nLocation: " + alert.location());
    }

    private void showScheduleDialog(String title, String detail) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        AppUi.styleDialog(dialog, root == null || root.getScene() == null ? null : root.getScene().getWindow(), "schedule-dialog", ButtonType.OK);
        Label heading = label(title, "schedule-dialog-title");
        Label body = label(detail, "schedule-dialog-detail");
        body.setWrapText(true);
        body.setMaxWidth(420);
        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setMaxHeight(260);
        bodyScroll.getStyleClass().add("schedule-dialog-scroll");
        VBox content = new VBox(12, heading, bodyScroll);
        content.getStyleClass().add("schedule-dialog-content");
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showMySchedule() {
        String detail = savedScheduleEvents.isEmpty() ? "No events added to My Schedule yet."
                : savedScheduleEvents.values().stream()
                        .sorted(Comparator.comparing(com.simhastha.schedule.ScheduleEvent::date)
                                .thenComparing(com.simhastha.schedule.ScheduleEvent::startTime))
                        .map(item -> formatTime(item.startTime()) + "  " + item.title() + " - " + item.location())
                        .collect(java.util.stream.Collectors.joining("\n"));
        showScheduleDialog("My Schedule", detail);
    }

    private record ScheduleLoad(List<com.simhastha.schedule.ScheduleEvent> events, List<ScheduleAlert> alerts) {
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
        java.util.List<AppDataStore.BookingRecord> bookings = user == null ? java.util.List.of()
                : AppDataStore.bookingsForUser(user.uid()).stream()
                        .sorted(java.util.Comparator.comparing((AppDataStore.BookingRecord booking) -> booking.createdAt).reversed())
                        .toList();
        long upcoming = bookings.stream().filter(booking -> bookingBucket(booking).equals("Upcoming")).count();
        long completed = bookings.stream().filter(booking -> bookingBucket(booking).equals("Completed")).count();
        long cancelled = bookings.stream().filter(booking -> bookingBucket(booking).equals("Cancelled")).count();

        HBox summary = new HBox(12,
                bookingMetric("All Bookings", bookings.size(), "All recorded services"),
                bookingMetric("Upcoming", upcoming, "Confirmed or in progress"),
                bookingMetric("Completed", completed, "Completed services"),
                bookingMetric("Cancelled", cancelled, "Cancelled bookings"));
        summary.getStyleClass().add("booking-summary-grid");
        summary.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));

        TextField search = AppUi.textField("Search booking ID or service");
        String[] categories = {"All", "Kumbh Packages", "Stay", "Puja Services", "Business Services"};
        VBox results = new VBox(10);
        java.util.Map<String, Button> filters = new java.util.LinkedHashMap<>();
        final String[] selectedCategory = {"All"};
        Runnable refresh = () -> renderBookingResults(results, bookings, selectedCategory[0], search.getText());
        HBox filterBar = new HBox(8);
        for (String category : categories) {
            Button filter = new Button(category);
            filter.getStyleClass().add("booking-filter-button");
            filter.setOnAction(event -> {
                selectedCategory[0] = category;
                filters.values().forEach(button -> button.getStyleClass().remove("booking-filter-active"));
                filter.getStyleClass().add("booking-filter-active");
                refresh.run();
            });
            filters.put(category, filter);
            filterBar.getChildren().add(filter);
        }
        filters.get("All").getStyleClass().add("booking-filter-active");
        search.textProperty().addListener((observable, oldValue, newValue) -> refresh.run());
        HBox controls = new HBox(12, filterBar, createSpacer(), search);
        controls.setAlignment(Pos.CENTER_LEFT); controls.getStyleClass().add("booking-filter-bar");
        HBox.setHgrow(search, Priority.ALWAYS); search.setMaxWidth(300);
        refresh.run();
        return pageShell("My Bookings", "View and manage all your Simhastha bookings in one place.", summary, controls, results);
    }

    private VBox bookingMetric(String title, long count, String detail) {
        VBox card = new VBox(4, muted(title), label(String.valueOf(count), "booking-metric-value"), muted(detail));
        card.getStyleClass().add("booking-metric-card");
        card.setMinWidth(150);
        return card;
    }

    private void renderBookingResults(VBox results, java.util.List<AppDataStore.BookingRecord> bookings, String category, String query) {
        java.util.List<AppDataStore.BookingRecord> matches = bookings.stream()
                .filter(booking -> bookingMatchesCategory(booking, category))
                .filter(booking -> bookingMatchesSearch(booking, query)).toList();
        if (matches.isEmpty()) {
            VBox empty = new VBox(9, label("⌂", "booking-empty-icon"), strong(bookings.isEmpty() ? "No bookings yet" : "No matching bookings"),
                    muted(bookings.isEmpty() ? "Your package, stay, puja and service bookings will appear here."
                            : "Try a different category or search term."));
            empty.setAlignment(Pos.CENTER); empty.getStyleClass().add("booking-empty-state");
            results.getChildren().setAll(empty);
            return;
        }
        VBox cards = new VBox(12);
        matches.forEach(booking -> cards.getChildren().add(bookingCard(booking)));
        results.getChildren().setAll(cards);
    }

    private boolean bookingMatchesCategory(AppDataStore.BookingRecord booking, String category) {
        return "All".equals(category) || categoryForBooking(booking).equals(category);
    }

    private boolean bookingMatchesSearch(AppDataStore.BookingRecord booking, String query) {
        if (query == null || query.isBlank()) return true;
        String text = (safeBookingText(booking.bookingId) + " " + safeBookingText(booking.title) + " "
                + safeBookingText(booking.location) + " " + safeBookingText(booking.businessId)).toLowerCase(java.util.Locale.ROOT);
        return text.contains(query.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private String categoryForBooking(AppDataStore.BookingRecord booking) {
        String module = safeBookingText(booking.moduleType).toLowerCase(java.util.Locale.ROOT);
        if (module.contains("stay")) return "Stay";
        if (module.contains("puja")) return "Puja Services";
        if (module.contains("package")) return "Kumbh Packages";
        return "Business Services";
    }

    private String bookingBucket(AppDataStore.BookingRecord booking) {
        String status = safeBookingText(booking.bookingStatus).toLowerCase(java.util.Locale.ROOT);
        if (status.contains("cancel") || status.contains("reject")) return "Cancelled";
        if (status.contains("complete") || status.contains("past")) return "Completed";
        return "Upcoming";
    }

    private HBox bookingCard(AppDataStore.BookingRecord booking) {
        StackPane visual = new StackPane(moduleIcon(safeBookingText(booking.moduleType).toLowerCase(java.util.Locale.ROOT), "booking-card-icon"));
        visual.getStyleClass().add("booking-card-visual"); visual.setMinSize(100, 86); visual.setPrefSize(100, 86);
        VBox copy = new VBox(5, strong(safeBookingText(booking.title)), badge(categoryForBooking(booking)),
                muted("Booking ID: " + safeBookingText(booking.bookingId)),
                muted(bookingPrimaryDetail(booking)),
                muted("Booked " + formatBookingTimestamp(booking.createdAt)));
        HBox.setHgrow(copy, Priority.ALWAYS);
        VBox status = new VBox(7, label(bookingAmount(booking), "booking-amount"), bookingStatusPill(booking.bookingStatus), paymentStatusPill(booking.paymentStatus));
        status.setAlignment(Pos.CENTER_RIGHT);
        Button view = new Button("View Booking"); view.getStyleClass().add("primary-button");
        view.setOnAction(event -> root.setCenter(scroll(bookingDetailsPage(booking))));
        HBox card = new HBox(15, visual, copy, status, view);
        card.setAlignment(Pos.CENTER_LEFT); card.getStyleClass().add("booking-premium-card");
        return card;
    }

    private String bookingPrimaryDetail(AppDataStore.BookingRecord booking) {
        java.util.List<String> details = new java.util.ArrayList<>();
        if (!safeBookingText(booking.dateText).isBlank()) details.add(booking.dateText);
        if (!safeBookingText(booking.location).isBlank()) details.add(booking.location);
        if (booking.nights > 0) details.add(booking.nights + (booking.nights == 1 ? " night" : " nights"));
        if (booking.quantity > 0) details.add(booking.quantity + (booking.quantity == 1 ? " unit" : " units"));
        return details.isEmpty() ? "Booking details available in your confirmation." : String.join("  •  ", details);
    }

    private Label bookingStatusPill(String value) { return bookingPill("Booking: " + safeBookingText(value), "booking-status-pill"); }
    private Label paymentStatusPill(String value) { return bookingPill("Payment: " + safeBookingText(value), "booking-payment-pill"); }
    private Label bookingPill(String value, String style) { Label pill = badge(value); pill.getStyleClass().add(style); return pill; }
    private String bookingAmount(AppDataStore.BookingRecord booking) {
        return booking.amountPaise > 0 ? "₹" + String.format(java.util.Locale.ROOT, "%,.2f", booking.amountPaise / 100.0) : "Amount not recorded";
    }
    private String safeBookingText(String value) { return value == null ? "" : value.trim(); }
    private String formatBookingTimestamp(String value) {
        try { return java.time.Instant.ofEpochMilli(Long.parseLong(value)).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(); }
        catch (RuntimeException ignored) { return value == null || value.isBlank() ? "date unavailable" : value; }
    }

    private VBox bookingDetailsPage(AppDataStore.BookingRecord booking) {
        Button back = new Button("← Back to My Bookings"); back.getStyleClass().add("pilgrim-small-action"); back.setOnAction(event -> showModulePage("bookings"));
        StackPane visual = new StackPane(moduleIcon(safeBookingText(booking.moduleType).toLowerCase(java.util.Locale.ROOT), "booking-card-icon"));
        visual.getStyleClass().add("booking-detail-visual"); visual.setMinSize(170, 135);
        VBox intro = new VBox(6, strong(safeBookingText(booking.title)), badge(categoryForBooking(booking)), muted("Booking ID: " + safeBookingText(booking.bookingId)),
                bookingStatusPill(booking.bookingStatus), paymentStatusPill(booking.paymentStatus));
        HBox overview = new HBox(18, visual, intro); overview.setAlignment(Pos.CENTER_LEFT); overview.getStyleClass().add("booking-detail-hero");
        VBox service = bookingDetailSection("Service Details", detailLine("Service", booking.title), detailLine("Category", categoryForBooking(booking)),
                detailLine("Reference", booking.catalogItemId), detailLine("Quantity", booking.quantity > 0 ? String.valueOf(booking.quantity) : ""));
        VBox customer = bookingDetailSection("Customer Details", detailLine("Customer", booking.customerName));
        VBox schedule = bookingDetailSection("Date & Location", detailLine("Service / visit date", booking.dateText),
                detailLine("Location", booking.location), detailLine("Nights", booking.nights > 0 ? String.valueOf(booking.nights) : ""));
        VBox payment = bookingDetailSection("Payment Summary", detailLine("Total amount", bookingAmount(booking)),
                detailLine("Payment status", booking.paymentStatus), detailLine("Booking status", booking.bookingStatus),
                detailLine("Payment ID", booking.razorpayPaymentId), detailLine("Transaction reference", booking.internalPaymentId));
        VBox timeline = bookingTimeline(booking);
        HBox actions = new HBox(10);
        AppDataStore.TicketRecord ticket = AppDataStore.ticketForBooking(booking.bookingId);
        if (ticket != null && ("CONFIRMED".equalsIgnoreCase(booking.bookingStatus) || "UPCOMING".equalsIgnoreCase(booking.bookingStatus))) {
            Button pass = new Button("View Ticket / Booking Pass"); pass.getStyleClass().add("primary-button"); pass.setOnAction(event -> TicketViewDialog.show(ticket)); actions.getChildren().add(pass);
        }
        if (!safeBookingText(booking.location).isBlank()) {
            Button map = new Button("View Location"); map.getStyleClass().add("pilgrim-small-action");
            map.setOnAction(event -> openUrl("https://www.google.com/maps/search/?api=1&query=" + java.net.URLEncoder.encode(booking.location, java.nio.charset.StandardCharsets.UTF_8)));
            actions.getChildren().add(map);
        }
        return pageShell("Booking Details", "Your persisted booking and payment information.", back, overview, service, customer, schedule, payment, timeline, actions);
    }

    private VBox bookingDetailSection(String title, Node... lines) {
        VBox section = new VBox(7, sectionTitle(title));
        for (Node line : lines) if (line != null) section.getChildren().add(line);
        section.getStyleClass().add("booking-detail-section"); return section;
    }

    private Node detailLine(String label, String value) {
        if (value == null || value.isBlank()) return null;
        return new HBox(8, muted(label), createSpacer(), strong(value));
    }

    private VBox bookingTimeline(AppDataStore.BookingRecord booking) {
        FlowPane steps = new FlowPane(8, 8, badge("Booking Created"));
        String status = safeBookingText(booking.bookingStatus).toLowerCase(java.util.Locale.ROOT);
        String payment = safeBookingText(booking.paymentStatus).toLowerCase(java.util.Locale.ROOT);
        if (payment.contains("paid") || payment.contains("verified")) steps.getChildren().add(badge("Payment Confirmed"));
        if (status.contains("cancel") || status.contains("reject")) steps.getChildren().add(badge("Cancelled"));
        else {
            if (status.contains("confirm") || status.contains("upcoming") || status.contains("complete")) steps.getChildren().add(badge("Booking Confirmed"));
            if (status.contains("complete") || status.contains("past")) steps.getChildren().add(badge("Completed"));
            else if (status.contains("confirm") || status.contains("upcoming")) steps.getChildren().add(badge("Service Upcoming"));
        }
        VBox section = new VBox(7, sectionTitle("Booking Timeline"), steps); section.getStyleClass().add("booking-detail-section"); return section;
    }

    private Node announcementPage() {
        VBox page = new VBox(12, topControls(), new UserAnnouncementView(this::showAnnouncementContent).page());
        page.getStyleClass().add("pilgrim-dashboard-main");
        page.setPadding(new Insets(12, 22, 28, 12));
        return page;
    }

    private void showAnnouncementContent(Node page) {
        if (page instanceof UserAnnouncementView.AnnouncementDetailsPage) {
            root.setCenter(page);
        } else {
            root.setCenter(scroll(page));
        }
    }

    private VBox faqPage() {
        TextField search = AppUi.textField("Search questions...");
        Button searchButton = new Button("Search");
        searchButton.setGraphic(AppUi.symbolIcon("\uE721", "button-icon"));
        searchButton.getStyleClass().add("faq-search-button");
        HBox searchBar = new HBox(10, search, searchButton);
        searchBar.getStyleClass().add("faq-search-bar");
        searchBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        ObjectProperty<String> selectedCategory = new SimpleObjectProperty<>("All");
        ObjectProperty<String> expandedFaqId = new SimpleObjectProperty<>("");
        ObjectProperty<Boolean> showAll = new SimpleObjectProperty<>(false);
        VBox categoryList = new VBox(6);
        categoryList.getStyleClass().add("faq-category-list");
        VBox questionList = new VBox(8);
        questionList.getStyleClass().add("faq-question-list");

        Runnable render = () -> {
            renderFaqCategories(categoryList, selectedCategory);
            renderFaqQuestions(questionList, selectedCategory.get(), search.getText(), expandedFaqId, showAll.get());
        };
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            expandedFaqId.set("");
            showAll.set(false);
            render.run();
        });
        search.setOnAction(event -> render.run());
        searchButton.setOnAction(event -> render.run());
        selectedCategory.addListener((observable, oldValue, newValue) -> {
            expandedFaqId.set("");
            showAll.set(false);
            render.run();
        });
        expandedFaqId.addListener((observable, oldValue, newValue) -> render.run());
        showAll.addListener((observable, oldValue, newValue) -> render.run());

        FlowPane categoryCards = new FlowPane(10, 10,
                faqCategoryCard("\uE946", "General", "General info & support", selectedCategory),
                faqCategoryCard("\uE946", "Simhastha 2027", "Dates, plans, updates", selectedCategory),
                faqCategoryCard("\uE707", "Ghats & Snan", "Bathing & rituals", selectedCategory),
                faqCategoryCard("\uE806", "Travel & Transport", "Routes, bus, parking", selectedCategory),
                faqCategoryCard("\uE809", "Stay & Accommodation", "Hotels and camps", selectedCategory),
                faqCategoryCard("\uE707", "Puja & Rituals", "Puja slots and seva", selectedCategory),
                faqCategoryCard("\uE95E", "Emergency & Safety", "Medical & safety", selectedCategory),
                faqCategoryCard("\uE8A7", "Bookings & Payments", "Payments & tickets", selectedCategory));
        categoryCards.getStyleClass().add("faq-category-cards");

        Button contactSupport = new Button("Contact Emergency Desk");
        contactSupport.setGraphic(AppUi.symbolIcon("\uE72A", "button-icon"));
        contactSupport.getStyleClass().add("faq-support-button");
        contactSupport.setMaxWidth(Double.MAX_VALUE);
        contactSupport.setOnAction(event -> showModulePage("emergency"));
        VBox helpCard = new VBox(8, AppUi.symbolIcon("\uE897", "faq-help-icon"),
                strong("Still need help?"),
                muted("24/7 support for emergency help, lost people/items, route changes and medical assistance."),
                label("Emergency Desk  |  Response guidance  |  Official updates", "faq-support-meta"),
                contactSupport);
        helpCard.getStyleClass().add("faq-help-card");

        Label allCategories = label("All Categories", "faq-side-title");
        allCategories.setWrapText(false);
        HBox sideHeader = new HBox(8, AppUi.symbolIcon("\uE9CE", "faq-panel-icon"),
                allCategories, createSpacer(), badge(String.valueOf(faqEntries().size())));
        sideHeader.setAlignment(Pos.CENTER_LEFT);
        VBox left = new VBox(10, sideHeader, categoryList, helpCard);
        left.getStyleClass().add("faq-side-panel");
        left.setMinWidth(300);
        left.setPrefWidth(330);

        ComboBox<String> sort = profileCombo("Sort by", "Most Popular", "Recently Updated", "Category");
        sort.setValue("Most Popular");
        HBox listHeader = new HBox(10, createSpacer(), muted("Sort by:"), sort);
        listHeader.setAlignment(Pos.CENTER_LEFT);
        VBox right = new VBox(12, listHeader, questionList, loadMoreFaqButton(showAll));
        right.getStyleClass().add("faq-main-panel");
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox body = new HBox(14, left, right);
        body.getStyleClass().add("faq-body");
        render.run();

        return pageShell("Frequently Asked Questions", "Find quick answers to common questions about Simhastha Connect.",
                searchBar, categoryCards, body);
    }

    private Button faqCategoryCard(String icon, String title, String detail, ObjectProperty<String> selectedCategory) {
        Button button = new Button();
        button.setGraphic(AppUi.symbolIcon(icon, "faq-category-icon"));
        button.setText(title + "\n" + detail);
        button.getStyleClass().add("faq-category-card");
        button.setWrapText(true);
        button.setMinWidth(170);
        button.setPrefWidth(176);
        button.setOnAction(event -> selectedCategory.set(title));
        return button;
    }

    private void renderFaqCategories(VBox list, ObjectProperty<String> selectedCategory) {
        list.getChildren().clear();
        List<String> categories = List.of("All", "General", "Simhastha 2027", "Ghats & Snan", "Travel & Transport",
                "Stay & Accommodation", "Puja & Rituals", "Emergency & Safety", "Bookings & Payments");
        for (String category : categories) {
            long count = faqEntries().stream()
                    .filter(entry -> "All".equals(category) || entry.category.equals(category))
                    .filter(entry -> entry.active)
                    .count();
            Button button = new Button(category + "  " + count);
            button.setGraphic(AppUi.symbolIcon(categoryIcon(category), "faq-list-icon"));
            button.getStyleClass().add(category.equals(selectedCategory.get())
                    ? "faq-category-list-active" : "faq-category-list-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setWrapText(true);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setOnAction(event -> selectedCategory.set(category));
            list.getChildren().add(button);
        }
    }

    private void renderFaqQuestions(VBox list, String category, String query, ObjectProperty<String> expandedFaqId,
            boolean showAll) {
        list.getChildren().clear();
        List<AppDataStore.FaqRecord> entries = faqEntries().stream()
                .filter(entry -> entry.active)
                .filter(entry -> "All".equals(category) || entry.category.equals(category))
                .filter(entry -> query == null || query.isBlank()
                        || (entry.question + " " + entry.answer).toLowerCase(java.util.Locale.ROOT)
                                .contains(query.toLowerCase(java.util.Locale.ROOT)))
                .sorted(Comparator.comparingInt(this::faqSortIndex))
                .limit(showAll ? 200 : 8)
                .toList();
        for (int i = 0; i < entries.size(); i++) {
            AppDataStore.FaqRecord entry = entries.get(i);
            list.getChildren().add(faqQuestionRow(entry, entry.id.equals(expandedFaqId.get()), expandedFaqId));
        }
        if (entries.isEmpty()) {
            list.getChildren().add(infoPanel("No questions found", "Try another keyword or category."));
        }
    }

    private int faqSortIndex(AppDataStore.FaqRecord entry) {
        try {
            return Integer.parseInt(entry.sortOrder);
        } catch (NumberFormatException ignored) {
            return 99;
        }
    }

    private VBox faqQuestionRow(AppDataStore.FaqRecord entry, boolean expanded, ObjectProperty<String> expandedFaqId) {
        Label question = strong(entry.question);
        question.setWrapText(true);
        question.setMaxWidth(Double.MAX_VALUE);
        Label control = AppUi.symbolIcon(expanded ? "\uE738" : "\uE710", "faq-expand-icon");
        HBox title = new HBox(10, AppUi.symbolIcon(categoryIcon(entry.category), "faq-question-icon"), question,
                createSpacer(), control);
        title.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(question, Priority.ALWAYS);
        VBox row = new VBox(8, title);
        if (expanded) {
            Label answer = muted(entry.answer);
            answer.setWrapText(true);
            answer.setMaxWidth(Double.MAX_VALUE);
            answer.getStyleClass().add("faq-question-answer");
            row.getChildren().add(answer);
            row.getStyleClass().add("faq-question-row-expanded");
        } else {
            row.getStyleClass().add("faq-question-row");
        }
        row.setOnMouseClicked(event -> expandedFaqId.set(expanded ? "" : entry.id));
        return row;
    }

    private void showFaqDetails(AppDataStore.FaqRecord entry) {
        Button back = new Button("\u2190 Back to FAQs");
        back.getStyleClass().add("pilgrim-small-action");
        back.setOnAction(event -> showModulePage("faq"));
        VBox detailPage = new VBox(14,
                back,
                new HBox(10, badge(entry.category), badge(entry.active ? "Published" : "Draft")),
                sectionTitle(entry.question),
                paragraph(entry.answer),
                infoPanel("Need More Help",
                        "Use Emergency, Announcement, Transport or Contact Support modules for live Simhastha guidance."));
        detailPage.getStyleClass().add("faq-detail-page");
        root.setCenter(scroll(pageShell("FAQ Details", "Help Center  >  " + entry.category, detailPage)));
    }

    private Button loadMoreFaqButton(ObjectProperty<Boolean> showAll) {
        Button button = new Button("Load More Questions");
        button.setGraphic(AppUi.symbolIcon("\uE74B", "button-icon"));
        button.getStyleClass().add("faq-load-more-button");
        button.setOnAction(event -> {
            showAll.set(true);
            button.setText("Showing All Questions");
        });
        return button;
    }

    private String categoryIcon(String category) {
        return switch (category) {
            case "Ghats & Snan", "Puja & Rituals" -> "\uE707";
            case "Travel & Transport" -> "\uE806";
            case "Stay & Accommodation" -> "\uE809";
            case "Emergency & Safety" -> "\uE95E";
            case "Bookings & Payments" -> "\uE8A7";
            case "Simhastha 2027" -> "\uE946";
            default -> "\uE9CE";
        };
    }

    private List<AppDataStore.FaqRecord> faqEntries() {
        return AppDataStore.faqs();
    }

    private VBox aboutPage() {
        VBox content = new VBox(8,
                topControls(),
                label("About Us", "pilgrim-page-title"),
                aboutHeroSection(),
                aboutMissionVisionSection(),
                aboutPlatformFeaturesSection(),
                aboutSafetyAndFlowSection(),
                aboutSimhasthaAndHelpSection(),
                aboutFooter());
        content.getStyleClass().addAll("pilgrim-dashboard-main", "about-dashboard-main");
        content.setPadding(new Insets(10, 18, 10, 18));
        return content;
    }

    private StackPane aboutHeroSection() {
        ImageView image = createImage("/images/welcome-light.png", 620, 150, 0.54, 0.48);
        image.getStyleClass().add("about-hero-image");
        StackPane imagePane = new StackPane(image);
        imagePane.getStyleClass().add("about-hero-image-pane");

        HBox highlights = new HBox(8,
                aboutHighlight("\uE716", "Millions of", "Pilgrims"),
                aboutHighlight("\uE73E", "Trusted &", "Verified"),
                aboutHighlight("\uE717", "24/7", "Support"),
                aboutHighlight("\uE946", "Official", "Information"));
        highlights.getStyleClass().add("about-highlight-row");

        VBox text = new VBox(8,
                label("About Simhastha Connect", "about-hero-title"),
                paragraph("Simhastha Connect is a unified digital platform designed to help pilgrims access transport, accommodation, puja services, emergency support and official information during Nashik Simhastha 2027."),
                highlights);
        text.getStyleClass().add("about-hero-copy");
        text.setMaxWidth(520);

        HBox content = new HBox(14, text, createSpacer(), imagePane);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(imagePane, Priority.ALWAYS);

        StackPane hero = new StackPane(content);
        hero.getStyleClass().add("about-hero-card");
        return hero;
    }

    private HBox aboutHighlight(String iconCode, String lineOne, String lineTwo) {
        Label icon = AppUi.symbolIcon(iconCode, "about-highlight-icon");
        VBox copy = new VBox(0,
                label(lineOne, "about-highlight-text"),
                label(lineTwo, "about-highlight-text"));
        HBox item = new HBox(9, icon, copy);
        item.getStyleClass().add("about-highlight-item");
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private HBox aboutMissionVisionSection() {
        HBox row = new HBox(10,
                aboutInfoCard("\uF1D8", "Our Mission",
                        "Our mission is to make the Simhastha pilgrimage safer, easier and better organized by connecting pilgrims with verified services and official information."),
                aboutInfoCard("\uE890", "Our Vision",
                        "To build a reliable digital platform that improves accessibility, coordination, safety and convenience for every pilgrim."));
        row.getStyleClass().add("about-two-card-row");
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private HBox aboutInfoCard(String iconCode, String title, String text) {
        Label icon = AppUi.symbolIcon(iconCode, "about-info-icon");
        VBox copy = new VBox(4, sectionTitle(title), paragraph(text));
        HBox card = new HBox(12, icon, copy);
        card.getStyleClass().add("about-info-card");
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        return card;
    }

    private VBox aboutPlatformFeaturesSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Node[] cards = {
                aboutFeatureCard("\uE806", "Transport Services", "Find travel options, routes and transport guidance.", "blue"),
                aboutFeatureCard("\uE774", "Puja Services", "Browse verified puja and spiritual services.", "orange"),
                aboutFeatureCard("\uE80F", "Ghats & Snan", "Get information about ghats, snan dates and important locations.", "green"),
                aboutFeatureCard("\uE783", "Emergency Support", "Quick access to emergency assistance and helpline services.", "red"),
                aboutFeatureCard("\uE809", "Stay & Accommodation", "Find suitable accommodation and stay options.", "purple"),
                aboutFeatureCard("\uE721", "Lost & Found", "Report or search for lost items and persons.", "green"),
                aboutFeatureCard("\uE787", "All Day Schedule", "View daily schedules and important Simhastha events.", "orange"),
                aboutFeatureCard("\uE789", "Official Announcements", "Receive important official updates and notices.", "purple")
        };
        for (int i = 0; i < cards.length; i++) {
            grid.add(cards[i], i, 0);
            GridPane.setHgrow(cards[i], Priority.ALWAYS);
        }
        VBox section = new VBox(8, sectionTitle("Platform Features"), grid);
        section.getStyleClass().add("about-section-card");
        return section;
    }

    private VBox aboutFeatureCard(String iconCode, String title, String detail, String accent) {
        Label icon = AppUi.symbolIcon(iconCode, "about-feature-icon");
        StackPane iconBubble = new StackPane(icon);
        iconBubble.getStyleClass().addAll("about-feature-icon-bubble", "about-accent-" + accent);
        VBox card = new VBox(5, iconBubble, strong(title), muted(detail));
        card.getStyleClass().addAll("about-feature-card", "about-feature-" + accent);
        card.setAlignment(Pos.TOP_CENTER);
        return card;
    }

    private HBox aboutSafetyAndFlowSection() {
        HBox row = new HBox(10, aboutSafetySection(), aboutWorksSection());
        row.getStyleClass().add("about-two-card-row");
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private HBox aboutSafetySection() {
        Label shield = AppUi.symbolIcon("\uE73E", "about-safety-icon");
        VBox points = new VBox(4,
                aboutCheckPoint("Trusted and official information"),
                aboutCheckPoint("Verified service providers"),
                aboutCheckPoint("Safe and transparent service flow"),
                aboutCheckPoint("Fraud and suspicious activity reporting"),
                aboutCheckPoint("Admin-controlled services and information"));
        VBox copy = new VBox(7, sectionTitle("Safety & Verified Services"), points);
        HBox card = new HBox(12, shield, copy);
        card.getStyleClass().addAll("about-info-card", "about-safety-card");
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        return card;
    }

    private HBox aboutCheckPoint(String text) {
        HBox point = new HBox(6, AppUi.symbolIcon("\uE930", "about-check-icon"), muted(text));
        point.setAlignment(Pos.CENTER_LEFT);
        return point;
    }

    private VBox aboutWorksSection() {
        HBox flow = new HBox(6,
                aboutFlowStep("\uE707", "Plan Your Visit", "Select destination, dates and preferences."),
                aboutArrow(),
                aboutFlowStep("\uE721", "Find Verified Services", "Explore transport, stay, puja and more."),
                aboutArrow(),
                aboutFlowStep("\uE8A5", "Book / Navigate Safely", "Book services or get routes and guidance."),
                aboutArrow(),
                aboutFlowStep("\uE7F4", "Receive Updates", "Get real-time updates, alerts and notices."),
                aboutArrow(),
                aboutFlowStep("\uE717", "Get Support", "24/7 help and emergency support."));
        flow.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(8, sectionTitle("How Simhastha Connect Works"), flow);
        card.getStyleClass().add("about-section-card");
        return card;
    }

    private VBox aboutFlowStep(String iconCode, String title, String detail) {
        Label icon = AppUi.symbolIcon(iconCode, "about-flow-icon");
        VBox step = new VBox(4, icon, strong(title), muted(detail));
        step.getStyleClass().add("about-flow-step");
        step.setAlignment(Pos.TOP_CENTER);
        return step;
    }

    private Label aboutArrow() {
        Label arrow = label("\u2192", "about-flow-arrow");
        arrow.setMinWidth(18);
        return arrow;
    }

    private HBox aboutSimhasthaAndHelpSection() {
        HBox row = new HBox(10, aboutSimhastha2027Card(), aboutNeedHelpCard());
        row.getStyleClass().add("about-two-card-row");
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private HBox aboutSimhastha2027Card() {
        Label icon = AppUi.symbolIcon("\uE80F", "about-info-icon");
        VBox copy = new VBox(5,
                sectionTitle("Nashik Simhastha 2027"),
                paragraph("Nashik Simhastha 2027 is a major spiritual gathering that brings together millions of pilgrims. Simhastha Connect aims to provide a technology-enabled platform for better access to information, services, coordination and support."));
        HBox card = new HBox(12, icon, copy);
        card.getStyleClass().add("about-info-card");
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        return card;
    }

    private VBox aboutNeedHelpCard() {
        HBox actions = new HBox(7,
                aboutSupportAction("\uE783", "Emergency\nSupport", () -> showModulePage("emergency")),
                aboutSupportAction("\uE789", "Official\nAnnouncements", () -> showModulePage("announcement")),
                aboutSupportAction("\uE717", "Help &\nSupport", () -> showInfo("Help & Support", "Use the 24/7 Support panel or Emergency section for quick assistance.")),
                aboutSupportAction("\uE946", "Report Fraud /\nSuspicious Activity", () -> showInfo("Report Fraud", "Use the Puja booking safety flow or contact official support to report suspicious activity.")));
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(8, sectionTitle("Need Help?"),
                muted("We are here for you. Reach out anytime you need assistance."), actions);
        card.getStyleClass().add("about-section-card");
        return card;
    }

    private Button aboutSupportAction(String iconCode, String text, Runnable action) {
        Button button = new Button(text);
        button.setGraphic(AppUi.symbolIcon(iconCode, "about-support-icon"));
        button.getStyleClass().add("about-support-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private HBox aboutFooter() {
        Label slogan = label("सेवा • सुरक्षा • श्रद्धा • समन्वय", "pilgrim-about-slogan");
        Label copyright = muted("Simhastha Connect 2027");
        HBox footer = new HBox(14, slogan, createSpacer(), copyright, smallGold("Nashik Simhastha 2027"));
        footer.getStyleClass().add("about-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
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
            cards.add("business".equals(module) ? publicBusinessCard(item)
                    : mediaRichCard(module, item, action));
        }
        if (cards.isEmpty()) {
            cards.add(infoPanel("No official records", "Admin-published information will appear here."));
        }
        return twoColumnGrid(cards.toArray(new Node[0]));
    }

    private VBox publicBusinessCard(AppDataStore.ServiceItem item) {
        Button details = new Button("View Details  >");
        details.getStyleClass().add("pilgrim-small-action");
        details.setOnAction(event -> showBusinessDetails(item));
        Button book = new Button("Book Now  >");
        book.getStyleClass().add("pilgrim-small-action");
        book.setOnAction(event -> createBusinessBooking(book, item));
        ImageView image = ImageMediaHelper.imageView(item.imageUrl, 390, 150);
        VBox card = new VBox(8, image, new HBox(10, moduleIcon("business", "pilgrim-card-icon"), badge("Approved")),
                strong(item.title), paragraph(item.detail), new HBox(8, details, book));
        card.getStyleClass().add("pilgrim-rich-card");
        return card;
    }

    private ImageView stayImage(StayItem stay, double width, double height) {
        return ImageMediaHelper.imageView(stay.imageUrl().isBlank() ? stay.localImagePath() : stay.imageUrl(), width, height);
    }

    private VBox mediaRichCard(String module, AppDataStore.ServiceItem item, String action) {
        VBox card = richCard(module, item.title, item.detail, action);
        if (item.imageUrl != null && !item.imageUrl.isBlank()) {
            card.getChildren().add(1, ImageMediaHelper.imageView(item.imageUrl, 390, 150));
        }
        return card;
    }

    private void showBusinessDetails(AppDataStore.ServiceItem item) {
        FlowPane gallery = new FlowPane(8, 8);
        if (item.galleryImages.isEmpty()) {
            gallery.getChildren().add(muted("No gallery photos available yet."));
        } else {
            item.galleryImages.forEach(image -> gallery.getChildren().add(ImageMediaHelper.imageView(image.url(), 150, 96)));
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(item.title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox content = new VBox(12,
                ImageMediaHelper.imageView(item.imageUrl, 560, 220),
                label(item.title, "profile-dialog-title"),
                paragraph(item.detail),
                new HBox(8, badge("Verified"), badge("Location / Map"), badge("Booking")),
                sectionTitle("Business Photos"),
                gallery);
        dialog.getDialogPane().setContent(content);
        AppUi.styleDialog(dialog, root == null || root.getScene() == null ? null : root.getScene().getWindow(), "business-detail-dialog", ButtonType.CLOSE);
        dialog.showAndWait();
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
        content.setPadding(new Insets(12, 22, 28, 12));
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
        boolean notification = "Notifications".equalsIgnoreCase(text);
        Button button = new Button(notification ? AppUi.notificationBellGlyph() : icon);
        button.getStyleClass().add("pilgrim-round-icon-button");
        if (notification) {
            button.getStyleClass().add("pilgrim-notification-button");
            button.setOnAction(event -> showPilgrimNotificationCenter());
        } else {
            button.setOnAction(event -> showInfo(text, text + " panel will open here."));
        }
        return button;
    }

    private StackPane notificationBell() {
        List<NotificationCenter.NotificationItem> items = pilgrimNotifications();
        int unread = (int) items.stream().filter(item -> !readNotifications.contains(item.id())).count();
        return NotificationCenter.bell(unread, this::showPilgrimNotificationCenter);
    }

    private void showPilgrimNotificationCenter() {
        NotificationCenter.show(root.getScene() == null ? null : root.getScene().getWindow(),
                "Notifications", "Booking, official and safety updates", pilgrimNotifications(),
                readNotifications, this::openNotificationTarget);
    }

    private List<NotificationCenter.NotificationItem> pilgrimNotifications() {
        List<NotificationCenter.NotificationItem> items = new java.util.ArrayList<>();
        AppSession.User user = AppSession.currentUser();
        String uid = user == null ? "" : user.uid();
        AppDataStore.bookingsForUser(uid).stream().limit(8).forEach(booking -> items.add(
                new NotificationCenter.NotificationItem("booking-" + booking.bookingId,
                        bookingTitle(booking), booking.title + " | " + booking.dateText + " | " + booking.location,
                        "Booking", bookingSeverity(booking), "bookings")));
        AppDataStore.lostFoundCases().stream()
                .filter(item -> !"closed".equalsIgnoreCase(item.status))
                .limit(4)
                .forEach(item -> items.add(new NotificationCenter.NotificationItem("lost-" + item.caseId,
                        "Lost & Found " + item.status, item.name + " | " + item.lastSeenLocation,
                        "Safety", item.priority, "lost")));
        AppDataStore.items("announcement").stream().limit(6).forEach(item -> items.add(
                new NotificationCenter.NotificationItem("announcement-" + item.id, item.title, item.detail,
                        "Official", "info", "announcement")));
        if (items.isEmpty()) {
            items.add(new NotificationCenter.NotificationItem("welcome-notification",
                    "Welcome to Simhastha Connect",
                    "Your booking, safety and official updates will appear here.", "System", "info", "home"));
        }
        return items;
    }

    private String bookingTitle(AppDataStore.BookingRecord booking) {
        if ("PAID".equalsIgnoreCase(booking.paymentStatus) || "VERIFIED".equalsIgnoreCase(booking.paymentStatus)) {
            return "Booking confirmed";
        }
        if ("FAILED".equalsIgnoreCase(booking.paymentStatus) || "PAYMENT_FAILED".equalsIgnoreCase(booking.bookingStatus)) {
            return "Payment needs attention";
        }
        return "Booking update";
    }

    private String bookingSeverity(AppDataStore.BookingRecord booking) {
        if ("FAILED".equalsIgnoreCase(booking.paymentStatus) || "PAYMENT_FAILED".equalsIgnoreCase(booking.bookingStatus)) {
            return "failed";
        }
        if ("PENDING".equalsIgnoreCase(booking.paymentStatus) || booking.bookingStatus.toUpperCase().contains("PENDING")) {
            return "pending";
        }
        return "confirmed";
    }

    private void openNotificationTarget(String target) {
        if ("profile".equals(target)) {
            showProfilePage();
        } else if ("home".equals(target)) {
            showHomePage();
        } else {
            showModulePage(target);
        }
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
        AppUi.showInfo(title, message, root == null || root.getScene() == null ? null : root.getScene().getWindow());
    }

    private void styleProfileDialog(Dialog<?> dialog, ButtonType primaryAction) {
        AppUi.styleDialog(dialog, root == null || root.getScene() == null ? null : root.getScene().getWindow(), "profile-dialog-pane", primaryAction);
    }

    private void addTheme(Scene scene) {
        ThemeManager.addTheme(scene, this);
        ThemeManager.addListener(() -> ThemeManager.applyTo(scene.getRoot()));
    }
}
