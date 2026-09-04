package com.simhastha.view;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.EnumMap;
import java.util.EnumSet;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class AdminDashboardPage {

    private static final DateTimeFormatter ADMIN_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy | hh:mm a");
    private static final DateTimeFormatter GHAT_ROW_TIME = DateTimeFormatter.ofPattern("d MMM yyyy\nhh:mm a");
    private static final List<String> SECTIONS = List.of(
            "Dashboard", "Live Operations", "Users", "Business Approvals", "Businesses", "Bookings",
            "Transport", "Transport Operators", "Kumbh Packages", "Puja Services", "Ghats & Snan", "Stay", "Lost & Found",
            "Schedule & Events", "Announcements", "FAQs / Help Center", "Emergency", "Reports & Analytics", "System");

    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private BorderPane root;
    private Stage stage;
    private String selectedSection = "Dashboard";
    private final Set<String> readNotifications = new HashSet<>();
    private String editingRouteId = "";
    private String editingOperationalId = "";
    private String editingOperationalModule = "";
    private boolean adminRefreshInProgress;
    private Timeline pujaAdminLiveRefresh;
    private boolean pujaEditorDirty;
    private boolean pujaEditorFocused;
    private boolean pujaEditorUpdating;
    private boolean pujaEditorVisible;
    private String pendingPujaEditorServiceId = "";
    private Path selectedPujaImageFile;
    private String selectedPujaImageUrl = "";
    private String selectedPujaImagePublicId = "";
    private boolean selectedPujaImageRemoved;
    private ImageView pujaImagePreview;
    private Label pujaImageStatus;
    private static final Map<String, Image> IMAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public Scene createScene(Stage stage) {
        this.stage = stage;
        if (!isAdminSession()) {
            return createAccessDeniedScene(stage);
        }

        root = new BorderPane();
        root.getStyleClass().add("pilgrim-dashboard-root");
        root.setLeft(createSidebar());
        root.setCenter(scroll(loadingPanel()));

        Scene scene = new Scene(root, 1200, 680);
        ThemeManager.addTheme(scene, this);
        ThemeManager.addListener(() -> ThemeManager.applyTo(root));
        refreshAdminData();
        startAdminPujaLiveRefreshIfNeeded();
        return scene;
    }

    private boolean isAdminSession() {
        AppSession.User user = AppSession.currentUser();
        return user != null
                && user.isAdmin()
                && "active".equals(user.status())
                && user.idToken() != null
                && !user.idToken().isBlank();
    }

    private Scene createAccessDeniedScene(Stage stage) {
        BorderPane accessPage = new BorderPane();
        accessPage.getStyleClass().add("management-page");
        accessPage.setTop(AppUi.createHeader(stage, "Admin Access Denied",
                "A verified active Firebase admin session is required.", () -> {
                    AppSession.clear();
                    NavigationUtil.navigate(stage, new LoginSelectionPage().createScene(stage));
                }));

        Label title = new Label("Admin session required");
        title.getStyleClass().add("form-title");
        Label detail = new Label("Login with a Firebase account whose users/{uid} profile has role = admin and status = active.");
        detail.getStyleClass().add("description-text");
        detail.setWrapText(true);

        Button login = new Button("Go to Admin Login");
        login.getStyleClass().add("primary-button");
        login.setOnAction(event -> {
            AppSession.clear();
            NavigationUtil.navigate(stage, new AdminAuthPage().createScene(stage));
        });

        VBox card = new VBox(14, title, detail, login);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(520);
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(38));
        accessPage.setCenter(center);
        AppSession.clear();
        return AppUi.createScene(new ThemedBackgroundPane(accessPage), this);
    }

    private VBox createSidebar() {
        navButtons.clear();
        ImageView logo = createImage("/images/sclogo.png", 54, 54, 0.5, 0.5);
        logo.getStyleClass().add("pilgrim-sidebar-logo-image");

        Label name = new Label("SIMHASTHA\nCONNECT");
        name.getStyleClass().add("pilgrim-sidebar-brand-strong");
        Label event = new Label("Nashik Simhastha 2027");
        event.getStyleClass().add("pilgrim-sidebar-tagline");
        Label role = new Label("ADMIN CONTROL CENTER");
        role.getStyleClass().add("admin-sidebar-role");

        HBox brand = new HBox(10, logo, new VBox(1, name, event, role));
        brand.getStyleClass().add("pilgrim-sidebar-brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        VBox menu = new VBox(4);
        for (String section : SECTIONS) {
            menu.getChildren().add(nav(section, section.equals(selectedSection)));
        }
        ScrollPane menuScroll = new ScrollPane(menu);
        menuScroll.getStyleClass().add("admin-sidebar-scroll");
        menuScroll.setFitToWidth(true);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(menuScroll, Priority.ALWAYS);

        Button logout = sidebarAction("Logout");
        logout.setOnAction(actionEvent -> {
            AppSession.clear();
            NavigationUtil.navigate(stage, new AdminAuthPage().createScene(stage));
        });

        VBox sidebar = new VBox(12, brand, menuScroll, logout);
        sidebar.getStyleClass().add("pilgrim-sidebar");
        sidebar.setPadding(new Insets(15, 13, 14, 13));
        sidebar.setPrefWidth(238);
        return sidebar;
    }

    private Button nav(String section, boolean active) {
        Button button = new Button(section);
        button.setGraphic(moduleIcon(section, active ? "pilgrim-nav-icon-active" : "pilgrim-nav-icon"));
        button.getStyleClass().add(active ? "pilgrim-nav-button-active" : "pilgrim-nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        navButtons.put(section, button);
        button.setOnAction(event -> showSection(section));
        return button;
    }

    private Button sidebarAction(String text) {
        Button button = new Button(text);
        button.setGraphic(moduleIcon("Logout", "pilgrim-nav-icon-danger"));
        button.getStyleClass().add("pilgrim-sidebar-action");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private void refreshAdminData() {
        if (adminRefreshInProgress) {
            return;
        }
        adminRefreshInProgress = true;
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            AppDataStore.refreshFirebaseData(token);
            AppDataStore.refreshAdminOverview(token);
        }).whenComplete((ignored, error) -> Platform.runLater(() -> {
            adminRefreshInProgress = false;
            if (!isPujaEditorProtectedFromRefresh()) {
                showSection(selectedSection);
            }
        }));
    }

    private void refreshAdminPujaData() {
        if (adminRefreshInProgress || !"Puja Services".equals(selectedSection)) {
            return;
        }
        adminRefreshInProgress = true;
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        java.util.concurrent.CompletableFuture.runAsync(() -> AppDataStore.refreshPujaFirebaseData(token))
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    adminRefreshInProgress = false;
                    if ("Puja Services".equals(selectedSection) && !isPujaEditorProtectedFromRefresh()) {
                        showSection(selectedSection);
                    }
                }));
    }

    private void showSection(String section) {
        if (!"Puja Services".equals(section)) {
            pujaEditorDirty = false;
            pujaEditorFocused = false;
            pujaEditorUpdating = false;
        }
        selectedSection = section;
        setActiveSection(section);
        Node page = switch (section) {
            case "Dashboard" -> dashboardPage();
            case "Live Operations" -> liveOperationsPage();
            case "Users" -> usersPage();
            case "Business Approvals" -> businessApprovalsPage();
            case "Businesses" -> businessesPage();
            case "Bookings" -> bookingsPage();
            case "Transport" -> transportPage();
            case "Transport Operators" -> transportOperatorsPage();
            case "Kumbh Packages" -> packagesPage();
            case "Puja Services" -> pujaServicesPage();
            case "Ghats & Snan" -> ghatsPage();
            case "Stay" -> stayPage();
            case "Lost & Found" -> lostFoundPage();
            case "Schedule & Events" -> schedulePage();
            case "Announcements" -> announcementsPage();
            case "FAQs / Help Center" -> faqManagementPage();
            case "Emergency" -> {
                loadEmergencyReportsAsync();
                loadEmergencyServicesAsync();
                yield emergencyPage();
            }
            case "Reports & Analytics" -> reportsPage();
            case "System" -> systemPage();
            default -> dashboardPage();
        };
        root.setCenter(scroll(page));
        startAdminPujaLiveRefreshIfNeeded();
    }

    private void startAdminPujaLiveRefreshIfNeeded() {
        stopAdminPujaLiveRefresh();
    }

    private void stopAdminPujaLiveRefresh() {
        if (pujaAdminLiveRefresh != null) {
            pujaAdminLiveRefresh.stop();
        }
    }

    private void setActiveSection(String section) {
        navButtons.forEach((key, button) -> {
            boolean selected = key.equals(section);
            button.getStyleClass().removeAll("pilgrim-nav-button", "pilgrim-nav-button-active");
            button.getStyleClass().add(selected ? "pilgrim-nav-button-active" : "pilgrim-nav-button");
            button.setGraphic(moduleIcon(key, selected ? "pilgrim-nav-icon-active" : "pilgrim-nav-icon"));
        });
    }

    private ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("pilgrim-dashboard-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox loadingPanel() {
        return pageShell("Admin Dashboard", "Operations & Governance Control",
                infoPanel("Loading", "Refreshing approvals, bookings and operational data."));
    }

    private VBox dashboardPage() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        HBox quick = new HBox(12,
                quickCard("Business Approvals", "Pending registrations", "Business Approvals"),
                quickCard("Bookings", "Transactions & reservations", "Bookings"),
                quickCard("Transport", "Routes & operations", "Transport"),
                quickCard("Emergency", "Safety operations", "Emergency"),
                quickCard("Lost & Found", "Priority cases", "Lost & Found"),
                quickCard("Announcements", "Official notices", "Announcements"));
        quick.setAlignment(Pos.CENTER_LEFT);

        HBox lower = new HBox(14, operationalOverviewPanel(overview), operationsStatusPanel(overview));
        HBox.setHgrow(lower.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lower.getChildren().get(1), Priority.ALWAYS);

        return pageShell("Admin Dashboard", "Official Nashik Simhastha 2027 operations control",
                photoHeader(),
                quick,
                overview.firebaseConnected() ? alertStrip("Operational alerts, pending approvals and important system updates will appear here.")
                        : syncIssueStrip(),
                lower);
    }

    private HBox topControls() {
        Label title = new Label("Admin Dashboard");
        title.getStyleClass().add("pilgrim-page-title");

        List<NotificationCenter.NotificationItem> notificationItems = notifications();
        long notificationCount = notificationItems.stream().filter(item -> !readNotifications.contains(item.id())).count();
        HBox actions = new HBox(10, title, createSpacer(), AppUi.createThemeToggle(),
                NotificationCenter.bell((int) notificationCount, () -> showNotificationCenter(notificationItems)),
                AppUi.createProfileChip(adminDisplayName(),
                        () -> showInfo("Admin Profile", "Admin profile panel will open here.")));
        actions.getStyleClass().add("pilgrim-top-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private String adminDisplayName() {
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            return "Admin";
        }
        if (user.displayName() != null && !user.displayName().isBlank()) {
            return user.displayName();
        }
        return user.email() == null || user.email().isBlank() ? "Admin" : user.email();
    }

    private List<NotificationCenter.NotificationItem> notifications() {
        List<NotificationCenter.NotificationItem> items = new java.util.ArrayList<>();
        for (AppDataStore.ApprovalRequest request : AppDataStore.pendingApprovals()) {
            String section = "transport".equalsIgnoreCase(request.targetModule) ? "Transport Operators"
                    : "Business Approvals";
            items.add(new NotificationCenter.NotificationItem("approval-" + request.ownerId,
                    request.type + " Pending", request.title + " is waiting for review.",
                    "Approval", "pending", section));
        }
        for (AppDataStore.PujaProviderRecord provider : AppDataStore.pujaProviders()) {
            if ("pending".equalsIgnoreCase(provider.status)) {
                items.add(new AdminNotification("puja-provider-" + provider.providerId, "Puja Provider Verification",
                        valueOr(provider.providerId, provider.fullName) + " is waiting for admin review.",
                        "Puja Services"));
            }
        }
        for (AppDataStore.TransportOperatorRecord operator : AppDataStore.transportOperators()) {
            if (!"suspended".equalsIgnoreCase(operator.status) && !"disabled".equalsIgnoreCase(operator.status)) {
                items.add(new NotificationCenter.NotificationItem("operator-" + operator.operatorId,
                        "Transport Operator Registration",
                        valueOr(operator.operatorId, operator.organizationName) + " is in the operator registry.",
                        "Transport", operator.status, "Transport Operators"));
            }
        }
        for (AppDataStore.RouteRecord route : AppDataStore.transportRoutes()) {
            if (!route.published) {
                items.add(new NotificationCenter.NotificationItem("route-" + route.routeId,
                        "Transport Route Submission", valueOr(route.routeId, route.routeName) + " is not published yet.",
                        "Route", "pending", "Transport"));
            }
        }
        for (AppDataStore.LostFoundCaseRecord lostCase : AppDataStore.lostFoundCases()) {
            if ("high".equalsIgnoreCase(lostCase.priority) || "urgent".equalsIgnoreCase(lostCase.priority)) {
                items.add(new NotificationCenter.NotificationItem("lost-" + lostCase.caseId,
                        "High Priority Lost & Found", valueOr(lostCase.caseId, lostCase.name) + " needs attention.",
                        "Safety", lostCase.priority, "Lost & Found"));
            }
        }
        AppDataStore.bookings().stream()
                .filter(booking -> "PENDING".equalsIgnoreCase(booking.bookingStatus)
                        || "FAILED".equalsIgnoreCase(booking.paymentStatus))
                .limit(6)
                .forEach(booking -> items.add(new NotificationCenter.NotificationItem("booking-" + booking.bookingId,
                        "Booking Activity", booking.bookingId + " | " + booking.title,
                        "Bookings", booking.paymentStatus, "Bookings")));
        return items.stream().distinct().toList();
    }

    private void showNotificationCenter(List<NotificationCenter.NotificationItem> items) {
        NotificationCenter.show(root.getScene() == null ? null : root.getScene().getWindow(),
                "Admin Notifications", "Approvals, bookings and safety operations",
                items, readNotifications, this::showSection);
    }

    private StackPane photoHeader() {
        ImageView image = createImage("/images/welcome-light.png", 980, 148, 0.54, 0.48);
        image.getStyleClass().add("pilgrim-hero-image");
        VBox copy = new VBox(3,
                label("SIMHASTHA CONNECT", "pilgrim-hero-title"),
                label("ADMINISTRATION & OPERATIONS", "pilgrim-hero-subtitle"),
                label("॥ ॐ नमः शिवाय ॥", "pilgrim-hero-mantra"),
                label("Official Nashik Simhastha 2027 Control Center", "pilgrim-hero-detail"),
                label("Government Operations • Safety • Services • Monitoring", "pilgrim-hero-detail"));
        copy.setAlignment(Pos.CENTER);
        copy.setPadding(new Insets(16));

        StackPane hero = new StackPane(image, copy);
        hero.getStyleClass().add("pilgrim-hero");
        hero.setMinHeight(148);
        return hero;
    }

    private VBox quickCard(String title, String detail, String targetSection) {
        VBox card = new VBox(8, moduleIcon(targetSection, "pilgrim-card-icon"), strong(title), muted(detail), arrowAction("View", targetSection));
        card.getStyleClass().add("pilgrim-module-card");
        card.setMinSize(132, 104);
        card.setOnMouseClicked(event -> showSection(targetSection));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private StackPane alertStrip(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("pilgrim-ticker-text");
        StackPane strip = new StackPane(label);
        strip.getStyleClass().add("pilgrim-alert-ticker");
        strip.setMinHeight(30);
        return strip;
    }

    private HBox syncIssueStrip() {
        Button retry = new Button("Retry");
        retry.getStyleClass().add("pilgrim-small-action");
        retry.setOnAction(event -> refreshAdminData());
        HBox row = new HBox(10, muted("Some live data is temporarily unavailable."), createSpacer(), retry);
        row.getStyleClass().add("pilgrim-alert-ticker");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 12, 0, 12));
        row.setMinHeight(34);
        return row;
    }

    private VBox operationalOverviewPanel(AppDataStore.AdminOverview overview) {
        VBox rows = new VBox(8,
                metricRow("Users", String.valueOf(overview.totalUsers()), "Total registered accounts"),
                metricRow("Approved Businesses", String.valueOf(overview.approvedBusinesses()), "Verified providers"),
                metricRow("Pending Approvals", String.valueOf(AppDataStore.pendingApprovals().size()
                        + AppDataStore.pujaProviders().stream()
                                .filter(provider -> "pending".equalsIgnoreCase(provider.status))
                                .count()), "Business and puja provider registrations"),
                metricRow("Today's Bookings", String.valueOf(overview.todaysBookings()), "Booking records"));
        VBox panel = new VBox(12, sectionTitle("Operational Overview"), rows);
        panel.getStyleClass().add("pilgrim-panel");
        return panel;
    }

    private VBox operationsStatusPanel(AppDataStore.AdminOverview overview) {
        VBox rows = new VBox(8,
                statusDataRow("Transport", overview.activeRoutes() > 0 ? "Normal" : "No active update"),
                statusDataRow("Ghats", AppDataStore.items("ghat").isEmpty() ? "No active update" : "Guidance published"),
                statusDataRow("Emergency", AppDataStore.items("emergency").isEmpty() ? "No active alerts" : "Contacts active"),
                statusDataRow("Schedule", overview.activeEvents() > 0 ? "Published" : "No active update"),
                statusDataRow("Announcements", overview.activeAnnouncements() + " active"));
        VBox panel = new VBox(12, sectionTitle("Operations Status"), rows);
        panel.getStyleClass().add("pilgrim-panel");
        return panel;
    }

    private HBox metricRow(String title, String value, String detail) {
        HBox row = dataRow("Reports & Analytics", title, detail);
        Label count = label(value, "admin-compact-count");
        row.getChildren().addAll(createSpacer(), count);
        return row;
    }

    private HBox statusDataRow(String title, String status) {
        HBox row = dataRow("Live Operations", title, status);
        row.getChildren().add(statusBadge(status));
        return row;
    }

    private VBox liveOperationsPage() {
        GridPane grid = twoColumnGrid(
                richCard("Transport", "Transport", AppDataStore.transportRoutes().size() + " route record(s), "
                        + AppDataStore.transportRoutes().stream().filter(r -> r.published && r.active).count()
                        + " public.", "Open"),
                richCard("Ghats & Snan", "Ghats", AppDataStore.items("ghat").size()
                        + " official ghat update(s).", "Open"),
                richCard("Emergency", "Emergency", AppDataStore.items("emergency").size()
                        + " verified public emergency record(s).", "Open"),
                richCard("Schedule & Events", "Schedule / Events", AppDataStore.items("schedule").size()
                        + " published event/update record(s).", "Open"));
        return pageShell("Live Operations", "Monitor current Simhastha operational signals.", grid,
                infoPanel("Recent Operational Activity", operationalActivityRows()));
    }

    private VBox operationalActivityRows() {
        VBox rows = new VBox(8);
        AppDataStore.transportRoutes().stream().filter(route -> route.published).limit(3)
                .forEach(route -> rows.getChildren().add(dataRow("Transport", route.routeName,
                        "Route published | Updated: " + valueOr("not available", route.updatedAt))));
        AppDataStore.items("ghat").stream().limit(3)
                .forEach(item -> rows.getChildren().add(dataRow("Ghats & Snan", item.title, item.detail)));
        AppDataStore.items("emergency").stream().limit(3)
                .forEach(item -> rows.getChildren().add(dataRow("Emergency", item.title, item.detail)));
        AppDataStore.items("announcement").stream().limit(3)
                .forEach(item -> rows.getChildren().add(dataRow("Announcements", item.title, item.detail)));
        AppDataStore.lostFoundCases().stream()
                .filter(item -> "high".equalsIgnoreCase(item.priority) || "urgent".equalsIgnoreCase(item.priority))
                .limit(3)
                .forEach(item -> rows.getChildren().add(dataRow("Lost & Found", valueOr(item.caseId, item.name),
                        "Priority: " + item.priority + " | Status: " + item.status)));
        if (rows.getChildren().isEmpty()) {
            rows.getChildren().add(dataRow("Live Operations", "No recent operational activity.",
                    "Published route, ghat, emergency, announcement and safety updates will appear here."));
        }
        return rows;
    }

    private VBox usersPage() {
        List<AppDataStore.UserRecord> users = AppDataStore.users();
        long suspended = users.stream().filter(user -> "suspended".equalsIgnoreCase(user.status)
                || "disabled".equalsIgnoreCase(user.status)).count();
        HBox stats = new HBox(12,
                metric(String.valueOf(suspended), "Suspended"),
                metric(String.valueOf(users.size()), "Total Users"),
                metric(isAdminSession() ? "Active" : "Inactive", "Admin Session"));
        VBox rows = new VBox(10);
        HBox filters = userFilters(users, rows);
        renderUsers(users, rows, "", "All", "All");
        return pageShell("Users", "Manage registered Simhastha Connect accounts.",
                stats,
                filters,
                infoPanel("User Registry", rows));
    }

    private HBox userFilters(List<AppDataStore.UserRecord> users, VBox rows) {
        TextField search = AppUi.textField("Search user / email / mobile");
        ComboBox<String> role = combo("All", "user", "business", "transport_operator", "admin");
        ComboBox<String> status = combo("All", "active", "pending", "approved", "rejected", "suspended", "disabled");
        Runnable apply = () -> renderUsers(users, rows, search.getText(), role.getValue(), status.getValue());
        search.textProperty().addListener((obs, old, value) -> apply.run());
        role.setOnAction(event -> apply.run());
        status.setOnAction(event -> apply.run());
        HBox box = new HBox(10, search, role, status);
        box.getStyleClass().add("pilgrim-filter-row");
        HBox.setHgrow(search, Priority.ALWAYS);
        return box;
    }

    private void renderUsers(List<AppDataStore.UserRecord> users, VBox rows, String search, String role, String status) {
        rows.getChildren().clear();
        List<AppDataStore.UserRecord> filtered = users.stream()
                .filter(user -> matches(search, user.uid, user.name, user.email, user.mobile))
                .filter(user -> "All".equals(valueOr("All", role)) || role.equalsIgnoreCase(user.role))
                .filter(user -> "All".equals(valueOr("All", status)) || status.equalsIgnoreCase(user.status))
                .toList();
        if (filtered.isEmpty()) {
            rows.getChildren().add(dataRow("Users", "No matching user records",
                    "Adjust search or filters to see more accounts."));
            return;
        }
        filtered.forEach(user -> rows.getChildren().add(userRow(user)));
    }

    private HBox userRow(AppDataStore.UserRecord user) {
        Button view = smallButton("View Profile");
        view.setOnAction(event -> showInfo(valueOr("User Profile", user.name),
                "UID: " + user.uid + "\nEmail: " + valueOr("Not available", user.email)
                        + "\nMobile: " + valueOr("Not available", user.mobile)
                        + "\nRole: " + valueOr("Not available", user.role)
                        + "\nStatus: " + valueOr("Not available", user.status)
                        + "\nRegistered: " + valueOr("Not available", user.createdAt)
                        + "\nLast Updated: " + valueOr("Not available", user.updatedAt)));
        Button status = smallButton("suspended".equalsIgnoreCase(user.status) || "disabled".equalsIgnoreCase(user.status)
                ? "Reactivate"
                : "Suspend");
        AppSession.User current = AppSession.currentUser();
        status.setDisable(current != null && current.uid().equals(user.uid));
        status.setOnAction(event -> updateUserStatus(user, status.getText()));
        HBox row = new HBox(10, moduleIcon("Users", "pilgrim-row-icon"),
                new VBox(2, strong(valueOr(user.uid, user.name)),
                        muted(valueOr("No email", user.email) + " | " + valueOr("No mobile", user.mobile)
                                + " | Role: " + valueOr("unknown", user.role)
                                + " | Status: " + valueOr("unknown", user.status)
                                + " | Updated: " + valueOr("not available", user.updatedAt))),
                createSpacer(), view, status);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void updateUserStatus(AppDataStore.UserRecord user, String action) {
        if (!confirm(action + " User", action + " " + valueOr(user.uid, user.name) + "?")) {
            return;
        }
        try {
            if ("Reactivate".equals(action)) {
                AppDataStore.reactivateUser(user.uid);
            } else {
                AppDataStore.suspendUser(user.uid);
            }
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("User Update Failed", exception.getMessage());
        }
    }

    private VBox businessApprovalsPage() {
        VBox rows = new VBox(10);
        if (AppDataStore.pendingApprovals().isEmpty()) {
            rows.getChildren().add(dataRow("Business Approvals", "No pending requests", "New business registrations will appear here."));
        } else {
            for (AppDataStore.ApprovalRequest request : List.copyOf(AppDataStore.pendingApprovals())) {
                rows.getChildren().add(approvalRow(request));
            }
        }
        HBox stats = new HBox(12,
                metric(String.valueOf(AppDataStore.pendingApprovals().size()), "Pending"),
                metric("0", "Approved"),
                metric("0", "Rejected"),
                metric(String.valueOf(AppDataStore.adminOverview().approvedBusinesses() + AppDataStore.adminOverview().pendingBusinesses()), "Total"));
        return pageShell("Business Approvals", "Review and verify business registrations.",
                stats,
                infoPanel("Registration Queue", rows));
    }

    private HBox approvalRow(AppDataStore.ApprovalRequest request) {
        Button view = smallButton("View Details");
        view.setOnAction(event -> showApprovalDetails(request));

        Button approve = smallButton("Approve");
        approve.getStyleClass().add("admin-success-action");
        approve.setOnAction(event -> {
            if (!confirm("Approve Business", "Approve " + request.title + "?")) {
                return;
            }
            try {
                AppDataStore.approve(request);
                refreshAdminData();
            } catch (AppDataStore.ApprovalUpdateException exception) {
                showInfo("Approval Failed", exception.getMessage());
            }
        });

        Button reject = smallButton("Reject");
        reject.getStyleClass().add("admin-danger-action");
        reject.setOnAction(event -> {
            if (!confirm("Reject Business", "Reject " + request.title + "?")) {
                return;
            }
            try {
                AppDataStore.reject(request);
                refreshAdminData();
            } catch (AppDataStore.ApprovalUpdateException exception) {
                showInfo("Rejection Failed", exception.getMessage());
            }
        });

        VBox text = new VBox(2,
                strong(request.title),
                muted("Owner: " + valueOr("Unknown", request.ownerId)
                        + " | Category: " + request.type
                        + " | Location/Contact: " + request.detail
                        + " | Status: Pending"));
        HBox row = new HBox(10, moduleIcon("Business Approvals", "pilgrim-row-icon"), text, createSpacer(), view, approve, reject);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showApprovalDetails(AppDataStore.ApprovalRequest request) {
        AppDataStore.BusinessRecord business = AppDataStore.businessForOwner(request.ownerId);
        if (business == null) {
            showInfo(request.title, request.detail);
            return;
        }
        showInfo(valueOr("Business Registration", business.businessName),
                "Business Name: " + valueOr("Not available", business.businessName)
                        + "\nOwner Name: " + valueOr("Not available", business.ownerName)
                        + "\nCategory: " + valueOr("Not available", business.category)
                        + "\nDescription: " + valueOr("Not available", business.description)
                        + "\nLocation: " + valueOr("Not available", business.location)
                        + "\nMobile: " + valueOr("Not available", business.mobile)
                        + "\nEmail: " + valueOr("Not available", business.email)
                        + "\nOperating Hours: " + valueOr("Not available", business.operatingHours)
                        + "\nPrice Range: " + valueOr("Not available", business.priceRange)
                        + "\nSubmitted: " + valueOr("Not available", business.createdAt)
                        + "\nStatus: " + valueOr("pending", business.status));
    }

    private VBox businessesPage() {
        VBox rows = new VBox(10);
        List<AppDataStore.BusinessRecord> businesses = AppDataStore.businesses();
        HBox filters = businessFilters(businesses, rows);
        renderBusinesses(businesses, rows, "", "All", "All");
        HBox stats = new HBox(12,
                metric(String.valueOf(businesses.size()), "Total"),
                metric(String.valueOf(businesses.stream().filter(b -> b.approved).count()), "Approved"),
                metric(String.valueOf(businesses.stream().filter(b -> "pending".equalsIgnoreCase(b.status)).count()), "Pending"),
                metric(String.valueOf(businesses.stream().filter(b -> "suspended".equalsIgnoreCase(b.status)).count()), "Suspended"));
        return pageShell("Businesses", "Manage approved Simhastha service providers.",
                stats,
                filters,
                infoPanel("Provider Registry", rows));
    }

    private HBox businessFilters(List<AppDataStore.BusinessRecord> businesses, VBox rows) {
        TextField search = AppUi.textField("Search business / owner / contact");
        ComboBox<String> category = combo("All", "Stay", "Food", "Parking", "Tent", "Shop", "Toilet", "Other");
        ComboBox<String> status = combo("All", "pending", "approved", "active", "rejected", "suspended");
        Runnable apply = () -> renderBusinesses(businesses, rows, search.getText(), category.getValue(), status.getValue());
        search.textProperty().addListener((obs, old, value) -> apply.run());
        category.setOnAction(event -> apply.run());
        status.setOnAction(event -> apply.run());
        HBox box = new HBox(10, search, category, status);
        box.getStyleClass().add("pilgrim-filter-row");
        HBox.setHgrow(search, Priority.ALWAYS);
        return box;
    }

    private void renderBusinesses(List<AppDataStore.BusinessRecord> businesses, VBox rows, String search,
            String category, String status) {
        rows.getChildren().clear();
        List<AppDataStore.BusinessRecord> filtered = businesses.stream()
                .filter(business -> matches(search, business.businessId, business.businessName, business.ownerName,
                        business.mobile, business.email, business.location))
                .filter(business -> "All".equals(valueOr("All", category))
                        || business.category.toLowerCase().contains(category.toLowerCase()))
                .filter(business -> "All".equals(valueOr("All", status)) || status.equalsIgnoreCase(business.status))
                .toList();
        if (filtered.isEmpty()) {
            rows.getChildren().add(dataRow("Businesses", "No matching business records",
                    "Registered providers from businesses/{uid} will appear here."));
            return;
        }
        filtered.forEach(business -> rows.getChildren().add(providerRow(business)));
    }

    private HBox providerRow(AppDataStore.BusinessRecord item) {
        Button view = smallButton("View");
        view.setOnAction(event -> showInfo(valueOr("Business", item.businessName),
                "Business ID: " + item.businessId
                        + "\nOwner: " + valueOr("Not available", item.ownerName)
                        + "\nCategory: " + valueOr("Not available", item.category)
                        + "\nDescription: " + valueOr("Not available", item.description)
                        + "\nLocation: " + valueOr("Not available", item.location)
                        + "\nMobile: " + valueOr("Not available", item.mobile)
                        + "\nEmail: " + valueOr("Not available", item.email)
                        + "\nHours: " + valueOr("Not available", item.operatingHours)
                        + "\nPrice Range: " + valueOr("Not available", item.priceRange)
                        + "\nStatus: " + item.status
                        + "\nApproved: " + item.approved));
        Button suspend = smallButton("suspended".equalsIgnoreCase(item.status) ? "Reactivate" : "Suspend");
        suspend.setOnAction(event -> updateBusinessStatus(item, suspend.getText()));
        HBox row = new HBox(10, moduleIcon("Businesses", "pilgrim-row-icon"),
                new VBox(2, strong(valueOr(item.businessId, item.businessName)),
                        muted(valueOr("No category", item.category) + " | Owner: " + valueOr("Unknown", item.ownerName)
                                + " | " + valueOr("No location", item.location)
                                + " | Status: " + item.status + " | Approved: " + item.approved)),
                createSpacer(), view, suspend);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void updateBusinessStatus(AppDataStore.BusinessRecord business, String action) {
        if (!confirm(action + " Business", action + " " + valueOr(business.businessId, business.businessName) + "?")) {
            return;
        }
        try {
            if ("Reactivate".equals(action)) {
                AppDataStore.updateBusinessStatus(business.businessId, "approved", true);
            } else {
                AppDataStore.updateBusinessStatus(business.businessId, "suspended", false);
            }
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Business Update Failed", exception.getMessage());
        }
    }

    private VBox bookingsPage() {
        long successful = AppDataStore.bookings().stream().filter(booking -> "PAID".equals(booking.paymentStatus)).count();
        long pending = AppDataStore.bookings().stream().filter(booking -> "PENDING".equals(booking.paymentStatus)).count();
        long failed = AppDataStore.bookings().stream().filter(booking -> "FAILED".equals(booking.paymentStatus)).count();
        long collected = AppDataStore.bookings().stream()
                .filter(booking -> "PAID".equals(booking.paymentStatus))
                .mapToLong(booking -> booking.amountPaise)
                .sum();

        HBox stats = new HBox(12,
                metric(String.valueOf(AppDataStore.bookings().size()), "Total Bookings"),
                metric(String.valueOf(successful), "Successful"),
                metric(String.valueOf(pending), "Pending"),
                metric(String.valueOf(failed), "Failed"),
                metric("Rs " + (collected / 100), "Total Collected"));

        VBox rows = new VBox(10);
        HBox filters = bookingFilters(AppDataStore.bookings(), rows);
        renderBookings(AppDataStore.bookings(), rows, "", "All", "All");
        return pageShell("Bookings", "Manage booking and payment records.",
                stats,
                filters,
                infoPanel("Booking Records", rows));
    }

    private HBox bookingFilters(List<AppDataStore.BookingRecord> bookings, VBox rows) {
        TextField search = AppUi.textField("Search booking / user / service");
        ComboBox<String> payment = combo("All", "PENDING", "PAID", "FAILED", "CANCELLED", "NOT_REQUIRED");
        ComboBox<String> status = combo("All", "PENDING", "CONFIRMED", "REJECTED", "CANCELLED", "COMPLETED", "PAYMENT_PROCESSING");
        Runnable apply = () -> renderBookings(bookings, rows, search.getText(), payment.getValue(), status.getValue());
        search.textProperty().addListener((obs, old, value) -> apply.run());
        payment.setOnAction(event -> apply.run());
        status.setOnAction(event -> apply.run());
        HBox box = new HBox(10, search, payment, status);
        box.getStyleClass().add("pilgrim-filter-row");
        HBox.setHgrow(search, Priority.ALWAYS);
        return box;
    }

    private void renderBookings(List<AppDataStore.BookingRecord> bookings, VBox rows, String search,
            String payment, String status) {
        rows.getChildren().clear();
        List<AppDataStore.BookingRecord> filtered = bookings.stream()
                .filter(booking -> matches(search, booking.bookingId, booking.userId, booking.businessId,
                        booking.title, booking.customerName))
                .filter(booking -> "All".equals(valueOr("All", payment)) || payment.equalsIgnoreCase(booking.paymentStatus))
                .filter(booking -> "All".equals(valueOr("All", status)) || status.equalsIgnoreCase(booking.bookingStatus))
                .toList();
        if (filtered.isEmpty()) {
            rows.getChildren().add(dataRow("Bookings", "No matching bookings",
                    "Booking records will appear here after users create requests or paid reservations."));
            return;
        }
        filtered.forEach(booking -> rows.getChildren().add(bookingRow(booking)));
    }

    private HBox bookingRow(AppDataStore.BookingRecord booking) {
        HBox row = new HBox(10, moduleIcon("Bookings", "pilgrim-row-icon"),
                new VBox(2, strong(booking.bookingId),
                        muted("User: " + booking.userId + " | Service: " + booking.title + " | Business: "
                                + booking.businessId + " | Date: " + booking.dateText + " | Amount: Rs "
                                + (booking.amountPaise / 100) + " | Payment: " + booking.paymentStatus
                                + " | Status: " + booking.bookingStatus)));
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox transportPage() {
        VBox form = structuredForm("Route Name", "From", "To", "Via / Stops", "Mode", "Start Time", "End Time",
                "Fare", "Duration", "Operator", "Official Route", "Map URL", "Live Source URL", "Published");
        VBox rows = new VBox(10);
        if (AppDataStore.transportRoutes().isEmpty()) {
            rows.getChildren().add(dataRow("Transport", "No route submissions loaded",
                    "Operator and admin routes from transportRoutes will appear here."));
        } else {
            for (AppDataStore.RouteRecord route : AppDataStore.transportRoutes()) {
                rows.getChildren().add(routeRow(route, form));
            }
        }
        return pageShell("Transport Operations", "Manage routes, stops, fare guidance and service alerts.",
                tabRow("Routes", "Stops", "Fare Guidance", "Service Alerts"),
                infoPanel("Route Editor", form, routeActionRow(form)),
                infoPanel("Route Registry", rows));
    }

    private HBox routeActionRow(VBox form) {
        Button save = smallButton("Save Route");
        save.setOnAction(event -> {
            save.setDisable(true);
            try {
                AppDataStore.saveRoute(routeFromForm(form));
                editingRouteId = "";
                formFields(form).forEach(TextField::clear);
                showInfo("Route Saved", "Transport route was saved to Firestore.");
                refreshAdminData();
            } catch (AppDataStore.ApprovalUpdateException exception) {
                showInfo("Route Save Failed", exception.getMessage());
            } finally {
                save.setDisable(false);
            }
        });
        return new HBox(10, save);
    }

    private AppDataStore.RouteRecord routeFromForm(VBox form) {
        List<TextField> fields = formFields(form);
        String id = editingRouteId == null || editingRouteId.isBlank()
                ? "route-" + java.util.UUID.randomUUID().toString().substring(0, 8)
                : editingRouteId;
        return new AppDataStore.RouteRecord(id, valueAt(fields, 0), valueAt(fields, 1), valueAt(fields, 2),
                valueAt(fields, 3), valueAt(fields, 4), valueAt(fields, 5), valueAt(fields, 6), valueAt(fields, 7),
                valueAt(fields, 8), valueAt(fields, 9), parseFlag(valueAt(fields, 10)), valueAt(fields, 11),
                valueAt(fields, 12), parseFlag(valueAt(fields, 13)), true, "", "");
    }

    private HBox routeRow(AppDataStore.RouteRecord route, VBox form) {
        Button edit = smallButton("Edit");
        edit.setOnAction(event -> fillRouteForm(form, route));
        Button publish = smallButton(route.published ? "Unpublish" : "Publish");
        publish.setOnAction(event -> updateRoute(route, !route.published, route.active, publish));
        Button active = smallButton(route.active ? "Deactivate" : "Activate");
        active.setOnAction(event -> updateRoute(route, route.published, !route.active, active));
        HBox row = new HBox(10, moduleIcon("Transport", "pilgrim-row-icon"),
                new VBox(2, strong(valueOr(route.routeId, route.routeName)),
                        muted(valueOr("From", route.from) + " -> " + valueOr("To", route.to)
                                + " | Mode: " + valueOr("Any", route.mode)
                                + " | Fare: " + valueOr("Not set", route.fare)
                                + " | Published: " + route.published + " | Active: " + route.active)),
                createSpacer(), edit, publish, active);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void fillRouteForm(VBox form, AppDataStore.RouteRecord route) {
        editingRouteId = route.routeId;
        List<TextField> fields = formFields(form);
        String[] values = { route.routeName, route.from, route.to, route.via, route.mode, route.startTime,
                route.endTime, route.fare, route.duration, route.operatorId, String.valueOf(route.official),
                route.mapUrl, route.liveSourceUrl, String.valueOf(route.published) };
        for (int i = 0; i < fields.size() && i < values.length; i++) {
            fields.get(i).setText(values[i]);
        }
    }

    private void updateRoute(AppDataStore.RouteRecord route, boolean published, boolean active, Button button) {
        button.setDisable(true);
        try {
            AppDataStore.updateRouteFlags(route.routeId, published, active);
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Route Update Failed", exception.getMessage());
        } finally {
            button.setDisable(false);
        }
    }

    private VBox transportOperatorsPage() {
        List<AppDataStore.TransportOperatorRecord> operators = AppDataStore.transportOperators();
        VBox rows = new VBox(10);
        HBox filters = operatorFilters(operators, rows);
        renderOperators(operators, rows, "", "All");
        HBox stats = new HBox(12,
                metric(String.valueOf(operators.size()), "Total Operators"),
                metric(String.valueOf(operators.stream().filter(o -> !"suspended".equalsIgnoreCase(o.status)
                        && !"disabled".equalsIgnoreCase(o.status)).count()), "Active Operators"),
                metric(String.valueOf(AppDataStore.adminOverview().activeRoutes()), "Routes Published"));
        return pageShell("Transport Operators", "Manage operator registry and route readiness.",
                stats,
                filters,
                infoPanel("Operator Registry", rows));
    }

    private HBox operatorFilters(List<AppDataStore.TransportOperatorRecord> operators, VBox rows) {
        TextField search = AppUi.textField("Search operator / contact");
        ComboBox<String> status = combo("All", "active", "pending", "approved", "suspended", "disabled", "rejected");
        Runnable apply = () -> renderOperators(operators, rows, search.getText(), status.getValue());
        search.textProperty().addListener((obs, old, value) -> apply.run());
        status.setOnAction(event -> apply.run());
        HBox box = new HBox(10, search, status);
        box.getStyleClass().add("pilgrim-filter-row");
        HBox.setHgrow(search, Priority.ALWAYS);
        return box;
    }

    private void renderOperators(List<AppDataStore.TransportOperatorRecord> operators, VBox rows, String search,
            String status) {
        rows.getChildren().clear();
        List<AppDataStore.TransportOperatorRecord> filtered = operators.stream()
                .filter(operator -> matches(search, operator.operatorId, operator.organizationName,
                        operator.contactPerson, operator.mobile, operator.email, operator.serviceType))
                .filter(operator -> "All".equals(valueOr("All", status)) || status.equalsIgnoreCase(operator.status))
                .toList();
        if (filtered.isEmpty()) {
            rows.getChildren().add(dataRow("Transport Operators", "No matching operators",
                    "Transport operator registrations from Firestore will appear here."));
            return;
        }
        filtered.forEach(operator -> rows.getChildren().add(operatorRow(operator)));
    }

    private HBox operatorRow(AppDataStore.TransportOperatorRecord operator) {
        Button view = smallButton("View");
        view.setOnAction(event -> showInfo(valueOr("Operator", operator.organizationName),
                "Organization: " + valueOr("Not available", operator.organizationName)
                        + "\nContact Person: " + valueOr("Not available", operator.contactPerson)
                        + "\nMobile: " + valueOr("Not available", operator.mobile)
                        + "\nEmail: " + valueOr("Not available", operator.email)
                        + "\nService Type: " + valueOr("Not available", operator.serviceType)
                        + "\nVehicles: " + valueOr("0", operator.vehicleCount)
                        + "\nRoutes Submitted: " + valueOr("0", operator.routesSubmitted)
                        + "\nActive Routes: " + valueOr("0", operator.activeRoutes)
                        + "\nStatus: " + operator.status
                        + "\nRegistered: " + valueOr("Not available", operator.createdAt)));
        Button status = smallButton("suspended".equalsIgnoreCase(operator.status) ? "Reactivate" : "Suspend");
        status.setOnAction(event -> updateOperatorStatus(operator, status.getText()));
        HBox row = new HBox(10, moduleIcon("Transport Operators", "pilgrim-row-icon"),
                new VBox(2, strong(valueOr(operator.operatorId, operator.organizationName)),
                        muted(valueOr("No contact", operator.contactPerson) + " | " + valueOr("No phone", operator.mobile)
                                + " | " + valueOr("No service type", operator.serviceType)
                                + " | Status: " + operator.status)),
                createSpacer(), view, status);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void updateOperatorStatus(AppDataStore.TransportOperatorRecord operator, String action) {
        if (!confirm(action + " Operator", action + " " + valueOr(operator.operatorId, operator.organizationName) + "?")) {
            return;
        }
        try {
            if ("Reactivate".equals(action)) {
                AppDataStore.updateTransportOperatorStatus(operator.operatorId, "active", true);
            } else {
                AppDataStore.updateTransportOperatorStatus(operator.operatorId, "suspended", false);
            }
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Operator Update Failed", exception.getMessage());
        }
    }

    private VBox packagesPage() {
        List<ManagedKumbhPackage> packages;
        try {
            // Always load afresh from the same source used by Save Draft.
            packages = PackageRepository.adminSource().getAllPackagesForAdmin(adminToken());
        } catch (Exception exception) {
            return pageShell("Kumbh Packages", "Package catalogue could not be reloaded.",
                    infoPanel("Firestore package load failed", "No package count is shown because the source could not be read. "
                            + actionablePackageError(exception)));
        }
        HBox stats = new HBox(12,
                metric(String.valueOf(packages.size()), "Total Packages"),
                metric(String.valueOf(countPackages(packages, PackageStatus.PUBLISHED)), "Published"),
                metric(String.valueOf(countPackages(packages, PackageStatus.DRAFT)), "Draft"),
                metric(String.valueOf(countPackages(packages, PackageStatus.PAUSED)), "Paused"),
                metric(String.valueOf(countPackages(packages, PackageStatus.ARCHIVED)), "Archived"));
        Button create = smallButton("+ Create New Package"); create.getStyleClass().add("admin-success-action"); create.setOnAction(e -> showPackageEditor(null));
        VBox rows = new VBox(10);
        if (packages.isEmpty()) rows.getChildren().add(infoPanel("No admin-created packages", "Create a draft, then publish it when the catalogue information is ready."));
        else packages.forEach(p -> rows.getChildren().add(packageAdminRow(p)));
        return pageShell("Kumbh Packages", "Admin-owned package catalogue. Only published packages are visible to pilgrims.", stats, create, infoPanel("Package Catalogue", rows));
    }

    private long countPackages(List<ManagedKumbhPackage> packages, PackageStatus status) { return packages.stream().filter(p -> p.status() == status).count(); }
    private String adminToken() { return AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken(); }
    private HBox packageAdminRow(ManagedKumbhPackage p) {
        Button view = smallButton("View"); view.setOnAction(e -> showPackagePreview(p)); Button edit = smallButton("Edit"); edit.setOnAction(e -> showPackageEditor(p));
        Button lifecycle = smallButton(p.status() == PackageStatus.PUBLISHED ? "Unpublish" : "Publish"); lifecycle.setOnAction(e -> changePackageStatus(p, p.status() == PackageStatus.PUBLISHED ? PackageStatus.PAUSED : PackageStatus.PUBLISHED));
        Button archive = smallButton("Archive"); archive.setOnAction(e -> changePackageStatus(p, PackageStatus.ARCHIVED));
        VBox info = new VBox(2, strong(p.name()), muted(p.packageCode() + " | " + p.origin() + " → " + p.destination() + " | " + p.days() + "D / " + p.nights() + "N | ₹" + String.format("%,d", p.startingPrice()) + " | " + p.travelOptions().stream().findFirst().orElse("Travel not set")), muted("Status: " + p.status() + " | Updated: " + valueOr("Not yet", p.updatedAt())));
        HBox row = new HBox(10, moduleIcon("Kumbh Packages", "pilgrim-row-icon"), info, createSpacer(), badge(p.category().name()), statusBadge(p.status().name()), view, edit, lifecycle, archive); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("pilgrim-data-row"); return row;
    }
    private void changePackageStatus(ManagedKumbhPackage p, PackageStatus status) {
        try { if (status == PackageStatus.PUBLISHED && !validForPublishing(p)) { showInfo("Cannot publish", "Add a package name, origin, duration, starting price and at least one itinerary activity before publishing."); return; }
            if (status == PackageStatus.PUBLISHED) PackageRepository.adminSource().publishPackage(p, adminToken()); else if (status == PackageStatus.PAUSED) PackageRepository.adminSource().pausePackage(p, adminToken()); else PackageRepository.adminSource().archivePackage(p, adminToken()); showSection("Kumbh Packages");
        } catch (Exception ex) { showInfo("Package update failed", ex.getMessage() == null ? "Could not save package status." : ex.getMessage()); }
    }
    private boolean validForPublishing(ManagedKumbhPackage p) { return !p.name().isBlank() && !p.origin().isBlank() && p.days() > 0 && p.startingPrice() > 0 && !p.itinerary().isEmpty(); }

    private void showPackageEditor(ManagedKumbhPackage existing) { root.setCenter(scroll(packageEditorPage(existing))); }
    private VBox packageEditorPage(ManagedKumbhPackage existing) {
        PackageEditor editor = new PackageEditor(existing); Button back = smallButton("← Back to package list"); back.setOnAction(e -> showSection("Kumbh Packages")); Button preview = smallButton("Preview Package"); preview.setOnAction(e -> showPackagePreview(editor.toPackage(existing == null ? PackageStatus.DRAFT : existing.status()))); Button draft = smallButton("Save Draft"); draft.setOnAction(e -> savePackage(editor, PackageStatus.DRAFT)); Button publish = smallButton("Publish Package"); publish.getStyleClass().add("admin-success-action"); publish.setOnAction(e -> savePackage(editor, PackageStatus.PUBLISHED));
        HBox actions = new HBox(10, back, createSpacer(), preview, draft, publish); actions.setAlignment(Pos.CENTER_LEFT);
        return pageShell(existing == null ? "Create Kumbh Package" : "Edit Kumbh Package", "Connection A package configuration. Photos and booking settings are intentionally excluded.", actions,
                infoPanel("Basic Information", editor.basic()), infoPanel("Travel", editor.travel()), infoPanel("Stay Options", editor.stay()), infoPanel("Meals", editor.meals()), infoPanel("Kumbh Experience", editor.experience()), infoPanel("Nashik Sightseeing", editor.sightseeing()), infoPanel("Itinerary", editor.itinerary()), infoPanel("Pricing", editor.pricing()), infoPanel("Inclusions / Exclusions", editor.inclusions()), infoPanel("Policies", editor.policies()), infoPanel("Availability", editor.availability()), infoPanel("Photos & Gallery", editor.media()), infoPanel("Preview & Publish", muted("Save Draft keeps the package admin-only. Publish validates the core catalogue details and makes it visible to users.")));
    }
    private void savePackage(PackageEditor editor, PackageStatus status) {
        ManagedKumbhPackage item = editor.toPackage(status); if (status == PackageStatus.PUBLISHED && !validForPublishing(item)) { showInfo("Cannot publish", "Package Name, Origin, Days, Starting Price and at least one itinerary activity are required."); return; }
        try { PackageRepository.adminSource().saveAndReload(item, adminToken()); showInfo(status == PackageStatus.PUBLISHED ? "Package published" : "Draft saved", status == PackageStatus.PUBLISHED ? "The package will appear on a pilgrim's next catalogue refresh." : "Draft " + item.packageCode() + " is persisted and appears in Admin Package Management."); showSection("Kumbh Packages"); } catch (Exception ex) { showInfo("Package save failed", actionablePackageError(ex)); }
    }
    private String actionablePackageError(Exception exception) { String message = exception.getMessage(); return (message == null || message.isBlank() ? "Could not read or write the kumbh_packages collection." : message) + " Verify Firebase is enabled, the admin user profile has role = admin, the ID token is current, and Firestore rules are deployed."; }
    private void showPackagePreview(ManagedKumbhPackage p) { Button back = smallButton("← Back to package list"); back.setOnAction(e -> showSection("Kumbh Packages")); VBox gallery = new VBox(8); if (p.gallery().isEmpty()) gallery.getChildren().add(muted("No gallery images selected.")); else { HBox thumbs = new HBox(8); p.gallery().stream().limit(5).forEach(media -> thumbs.getChildren().add(adminMediaPreview(media, 120, 76))); gallery.getChildren().add(thumbs); } root.setCenter(scroll(pageShell("Package Preview", "Preview uses the same package media metadata that published users receive.", back, infoPanel("Hero / Cover", adminMediaPreview(p.heroImage() == null ? p.coverImage() : p.heroImage(), 760, 210)), infoPanel("Gallery", gallery), infoPanel("Package Summary", paragraph(p.name() + "\n" + p.category() + " • " + p.origin() + " → " + p.destination() + " • " + p.days() + "D / " + p.nights() + "N\nStarting ₹" + String.format("%,d", p.startingPrice()) + "\n\nTravel: " + String.join(", ", p.travelOptions()) + "\nStay: " + String.join(", ", p.stayOptions()) + "\nMeals: " + String.join(", ", p.mealOptions()) + "\nFacilities: " + String.join(", ", p.facilities())))))); }
    private StackPane adminMediaPreview(PackageMedia media, double width, double height) { StackPane frame = new StackPane(); frame.setPrefSize(width, height); frame.setMaxSize(width, height); frame.getStyleClass().add("package-gallery-main"); if (media == null || !PackageMediaService.temporary().isUsable(media)) { frame.getChildren().add(muted("No image selected")); return frame; } try { Image image = new Image(PackageMediaService.temporary().resolveReference(media), false); ImageView view = new ImageView(image); view.setFitWidth(width); view.setFitHeight(height); view.setPreserveRatio(true); frame.getChildren().add(view); } catch (Exception ignored) { frame.getChildren().add(muted("Image unavailable")); } return frame; }

    private VBox pujaServicesPage() {
        VBox form = pujaServiceEditorForm();
        protectPujaEditorFromLiveRefresh(form);
        VBox editor = new VBox(12, pujaServiceTopActionBar(form), form, pujaServiceImageSection());
        if (pujaEditorVisible && !pendingPujaEditorServiceId.isBlank()) {
            adminVisiblePujaServices().stream()
                    .filter(service -> service.serviceId.equals(pendingPujaEditorServiceId))
                    .findFirst()
                    .ifPresent(service -> fillPujaServiceForm(service, form));
            pendingPujaEditorServiceId = "";
        }

        Button addNew = smallButton("+ Add New Puja Service");
        addNew.getStyleClass().add("admin-success-action");
        addNew.setOnAction(event -> {
            pujaEditorVisible = true;
            editingOperationalId = "";
            editingOperationalModule = "";
            clearPujaEditorImageState();
            showSection("Puja Services");
        });

        Node editorPanel = pujaEditorVisible
                ? infoPanel(hasSelectedPujaService() ? "Edit Puja Service" : "Add New Puja Service",
                        editor, pujaServiceActionRow(form))
                : compactAdminPujaAddPanel(addNew);

        return pageShell("Puja Services", "Manage verified puja services and booking readiness.",
                adminPujaTrustBanner(),
                editorPanel,
                pujaProviderVerificationPanel(),
                pujaServiceRegistryPanel(form),
                pujaBookingsPanel(),
                pujaFraudReportsPanel());
    }

    private VBox pujaServiceEditorForm() {
        TextField name = AppUi.textField("Puja Name");
        TextField description = AppUi.textField("Description");
        TextField temple = AppUi.textField("Temple / Ghat");
        TextField location = AppUi.textField("Location");
        TextField type = AppUi.textField("Puja Type");
        TextField price = AppUi.textField("Price");
        TextField duration = AppUi.textField("Duration");
        TextField slots = AppUi.textField("Available Slots");
        TextField provider = AppUi.textField("Verified Pandit / Provider");
        TextField languages = AppUi.textField("Languages");
        TextField mode = AppUi.textField("Mode");
        TextField verification = AppUi.textField("Verification Status");
        TextField booking = AppUi.textField("Booking Status");

        VBox form = new VBox(12,
                pujaEditorGroup("Basic Information", name, description, type, temple, location),
                pujaEditorGroup("Booking Information", price, duration, slots, mode, languages),
                pujaEditorGroup("Provider", provider, verification, booking));
        form.getStyleClass().add("admin-structured-form");
        form.getProperties().put("orderedTextFields", List.of(name, description, type, temple, location,
                price, duration, slots, provider, languages, mode, verification, booking));
        return form;
    }

    private VBox pujaEditorGroup(String title, TextField... fields) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        for (int i = 0; i < fields.length; i++) {
            fields[i].setMaxWidth(Double.MAX_VALUE);
            grid.add(fields[i], i % 2, i / 2);
            GridPane.setHgrow(fields[i], Priority.ALWAYS);
        }
        VBox group = new VBox(10, strong(title), grid);
        group.getStyleClass().add("admin-puja-editor-group");
        return group;
    }

    private HBox adminPujaTrustBanner() {
        HBox banner = new HBox(12,
                adminPujaTrustItem("\uE73E", "Verified Providers", "Priests and service counters are checked before listing."),
                adminPujaTrustItem("\uE8C7", "Transparent Pricing", "Starting prices are shown clearly before booking."),
                adminPujaTrustItem("\uE72E", "Secure Booking", "Booking details stay inside the official flow."),
                adminPujaTrustItem("\uE8D7", "No Agent Payments", "Do not pay unofficial middlemen or unknown agents."));
        banner.getStyleClass().add("puja-trust-banner");
        return banner;
    }

    private VBox adminPujaTrustItem(String icon, String title, String detail) {
        VBox item = new VBox(7, AppUi.symbolIcon(icon, "puja-trust-icon"), strong(title), muted(detail));
        item.getStyleClass().add("puja-trust-item");
        HBox.setHgrow(item, Priority.ALWAYS);
        return item;
    }

    private VBox compactAdminPujaAddPanel(Button addNew) {
        HBox row = new HBox(14,
                moduleIcon("Puja Services", "pilgrim-row-icon"),
                new VBox(4, strong("Published Puja Services are managed from Firestore"),
                        muted("Click Add New Puja Service to open the editor. Existing services can be edited from the list below.")),
                createSpacer(),
                addNew);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(row);
        panel.getStyleClass().add("admin-puja-compact-add-panel");
        return panel;
    }

    private HBox pujaServiceTopActionBar(VBox form) {
        Button addFresh = smallButton("+ Add Puja");
        addFresh.getStyleClass().add("admin-success-action");
        addFresh.setOnAction(event -> {
            editingOperationalId = "";
            editingOperationalModule = "";
            clearPujaServiceForm(form);
            clearPujaEditorImageState();
            pujaEditorVisible = true;
            pujaEditorDirty = false;
            pujaEditorFocused = false;
        });

        Button save = smallButton(hasSelectedPujaService() ? "Save Update" : "Save Puja Service");
        save.getStyleClass().add("admin-success-action");
        save.setOnAction(event -> {
            if (hasSelectedPujaService()) {
                savePujaServiceFromForm(form, false, false, "");
            } else {
                savePujaServiceFromForm(form, false, false, "PENDING");
            }
        });

        Button savePublish = smallButton("Save & Publish");
        savePublish.getStyleClass().add("admin-success-action");
        savePublish.setOnAction(event -> savePujaServiceFromForm(form, true, true, "VERIFIED"));

        Button close = smallButton("Close");
        close.setOnAction(event -> {
            editingOperationalId = "";
            editingOperationalModule = "";
            pendingPujaEditorServiceId = "";
            pujaEditorVisible = false;
            pujaEditorDirty = false;
            pujaEditorFocused = false;
            clearPujaEditorImageState();
            showSection("Puja Services");
        });

        HBox bar = new HBox(10,
                moduleIcon("Puja Services", "pilgrim-row-icon"),
                new VBox(3,
                        strong(hasSelectedPujaService() ? "Edit selected Puja Service" : "Create a new Puja Service"),
                        muted("Use these buttons to save without scrolling to the bottom.")),
                createSpacer(),
                addFresh,
                save,
                savePublish,
                close);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("admin-puja-top-action-bar");
        return bar;
    }

    private HBox pujaServiceActionRow(VBox form) {
        Button add = smallButton("Add Service");
        add.setOnAction(event -> savePujaServiceFromForm(form, false, false, "PENDING"));
        Button update = smallButton("Edit / Update");
        update.setOnAction(event -> {
            if (!hasSelectedPujaService()) {
                showInfo("Select Service", "Select a Puja service row first, then click Edit / Update.");
                return;
            }
            savePujaServiceFromForm(form, false, false, "");
        });
        Button verify = smallButton("Verify Service");
        verify.getStyleClass().add("admin-success-action");
        verify.setOnAction(event -> {
            if (!hasSelectedPujaService()) {
                showInfo("Select Service", "Select a Puja service row first, then click Verify Service.");
                return;
            }
            savePujaServiceFromForm(form, true, false, "VERIFIED");
        });
        Button publish = smallButton("Publish Service");
        publish.getStyleClass().add("admin-success-action");
        publish.setOnAction(event -> {
            if (!hasSelectedPujaService()) {
                showInfo("Select Service", "Select a Puja service row first, then click Publish Service.");
                return;
            }
            savePujaServiceFromForm(form, true, true, "VERIFIED");
        });
        Button disable = smallButton("Disable Selected");
        disable.setOnAction(event -> updateSelectedPujaService(form, false, false, "", "Disable"));
        Button enable = smallButton("Enable Selected");
        enable.setOnAction(event -> updateSelectedPujaService(form, true, true, "VERIFIED", "Enable"));
        Button delete = smallButton("Delete Selected");
        delete.getStyleClass().add("admin-danger-action");
        delete.setOnAction(event -> deleteSelectedPujaService(form));
        Button bookings = smallButton("View Bookings");
        bookings.setOnAction(event -> showPujaBookingsDialog());
        Button cancel = smallButton("Close Editor");
        cancel.setOnAction(event -> {
            editingOperationalId = "";
            editingOperationalModule = "";
            pendingPujaEditorServiceId = "";
            pujaEditorVisible = false;
            pujaEditorDirty = false;
            pujaEditorFocused = false;
            clearPujaEditorImageState();
            showSection("Puja Services");
        });
        return new HBox(10, add, update, verify, publish, disable, enable, delete, bookings, cancel);
    }

    private boolean hasSelectedPujaService() {
        return "puja".equals(editingOperationalModule) && editingOperationalId != null && !editingOperationalId.isBlank();
    }

    private VBox pujaServiceImageSection() {
        pujaImagePreview = new ImageView();
        pujaImagePreview.setFitWidth(170);
        pujaImagePreview.setFitHeight(105);
        pujaImagePreview.setPreserveRatio(false);
        pujaImagePreview.getStyleClass().add("admin-puja-image-preview");
        setPujaImagePreview(selectedPujaImageUrl);
        pujaImageStatus = new Label(selectedPujaImageUrl == null || selectedPujaImageUrl.isBlank()
                ? "No Cloudinary image selected. Default image will be used."
                : "Current image ready.");
        pujaImageStatus.getStyleClass().add("admin-muted-text");

        Button upload = smallButton("Upload Image / Change Image");
        upload.setOnAction(event -> choosePujaServiceImage());
        Button remove = smallButton("Remove Image");
        remove.setOnAction(event -> {
            if (!confirm("Remove Service Image", "Remove the selected service image and use the default placeholder?")) {
                return;
            }
            selectedPujaImageFile = null;
            selectedPujaImageUrl = "";
            selectedPujaImagePublicId = "";
            selectedPujaImageRemoved = true;
            setPujaImagePreview("");
            if (pujaImageStatus != null) {
                pujaImageStatus.setText("Image removed. Save/Update to apply this change.");
            }
            pujaEditorDirty = true;
        });
        HBox actions = new HBox(10, upload, remove);
        VBox text = new VBox(6, strong("Service Image"), muted("Upload JPG, JPEG, PNG or WEBP. Saved image URL comes from Cloudinary."), pujaImageStatus, actions);
        HBox section = new HBox(14, pujaImagePreview, text);
        section.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        VBox wrap = new VBox(section);
        wrap.getStyleClass().add("admin-puja-image-section");
        return wrap;
    }

    private void choosePujaServiceImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Puja Service Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            ImageUploadService.get().validateImage(file.toPath());
            selectedPujaImageFile = file.toPath();
            selectedPujaImageRemoved = false;
            setPujaImagePreview(file.toURI().toString());
            if (pujaImageStatus != null) {
                pujaImageStatus.setText("Selected: " + file.getName() + ". Save/Update will upload to Cloudinary.");
            }
            pujaEditorDirty = true;
        } catch (RuntimeException exception) {
            showInfo("Invalid Image", exception.getMessage());
        }
    }

    private void setPujaImagePreview(String uri) {
        if (pujaImagePreview == null) {
            return;
        }
        String source = uri == null || uri.isBlank() ? "/images/trimbakeshwar.jpg" : uri;
        try {
            if (source.startsWith("http") || source.startsWith("file:")) {
                pujaImagePreview.setImage(new Image(source, true));
            } else {
                URL imageUrl = getClass().getResource(source);
                if (imageUrl != null) {
                    String key = imageUrl.toExternalForm();
                    pujaImagePreview.setImage(IMAGE_CACHE.computeIfAbsent(key, value -> new Image(value, true)));
                }
            }
        } catch (Exception ignored) {
            // Preview is best-effort; saving logic still preserves the stored URL.
        }
    }

    private void savePujaServiceFromForm(VBox form, boolean adminApproved, boolean published, String verificationStatus) {
        List<TextField> fields = formFields(form);
        String name = valueAt(fields, 0);
        System.out.println("ADMIN PUJA: save started, selectedServiceId="
                + valueOr("new", editingOperationalId) + ", fieldCount=" + fields.size());
        if (name.isBlank()) {
            showInfo("Missing Puja Name", "Enter Puja Name before saving.");
            return;
        }
        if (!isValidPujaPrice(valueAt(fields, 5))) {
            showInfo("Invalid Price", "Enter a valid numeric Puja price, for example 101 or Rs. 5100.");
            return;
        }
        String id = editingOperationalModule.equals("puja") && !editingOperationalId.isBlank()
                ? editingOperationalId
                : "puja-service-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        AppDataStore.PujaServiceRecord existing = adminVisiblePujaServices().stream()
                .filter(service -> service.serviceId.equals(id))
                .findFirst()
                .orElse(null);
        System.out.println("IMAGE: serviceId=" + id
                + ", old image present=" + (existing != null && !existing.imageUrl.isBlank())
                + ", new image selected=" + (selectedPujaImageFile != null));
        boolean finalPublished = published || (existing != null && existing.published);
        boolean finalApproved = adminApproved || (existing != null && existing.adminApproved);
        String finalVerification = verificationStatus == null || verificationStatus.isBlank()
                ? valueOr(existing == null ? "PENDING" : existing.verificationStatus, valueAt(fields, 11))
                : verificationStatus;
        String imageUrl = selectedPujaImageRemoved ? "/images/trimbakeshwar.jpg"
                : valueOr(existing == null ? "/images/trimbakeshwar.jpg" : existing.imageUrl, selectedPujaImageUrl);
        String imagePublicId = selectedPujaImageRemoved ? ""
                : valueOr(existing == null ? "" : existing.imagePublicId, selectedPujaImagePublicId);
        String description = valueOr(name + " at " + valueAt(fields, 3), valueAt(fields, 1));
        String templeOrLocation = valueOr(valueAt(fields, 3), valueAt(fields, 4));
        AppDataStore.PujaServiceRecord draft = new AppDataStore.PujaServiceRecord(id, name,
                description,
                valueOr("Custom", valueAt(fields, 2)), templeOrLocation, valueAt(fields, 5),
                valueOr("Varies", valueAt(fields, 6)),
                "", valueAt(fields, 8), finalVerification, valueOr("Varies", valueAt(fields, 7)),
                valueOr("Offline", valueAt(fields, 10)), valueOr("Marathi, Hindi, Sanskrit", valueAt(fields, 9)),
                imageUrl, imagePublicId,
                valueOr("OPEN", valueAt(fields, 12)), finalPublished && finalApproved ? "active" : "draft",
                finalPublished, true, finalApproved, existing == null ? "" : existing.createdAt, "",
                existing == null ? "" : existing.createdBy, existing == null ? "" : existing.updatedBy,
                existing == null ? "" : existing.verifiedAt, existing == null ? "" : existing.verifiedBy);
        persistPujaServiceWithOptionalImage(form, draft, existing);
    }

    private boolean isValidPujaPrice(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        return !digits.isBlank();
    }

    private void persistPujaServiceWithOptionalImage(VBox form, AppDataStore.PujaServiceRecord draft,
            AppDataStore.PujaServiceRecord existing) {
        if (pujaImageStatus != null) {
            pujaImageStatus.setText(selectedPujaImageFile == null ? "Saving service..." : "Uploading image to Cloudinary...");
        }
        System.out.println("IMAGE: upload started=" + (selectedPujaImageFile != null) + ", serviceId=" + draft.serviceId);
        java.util.concurrent.CompletableFuture<AppDataStore.PujaServiceRecord> prepared =
                selectedPujaImageFile == null
                        ? java.util.concurrent.CompletableFuture.completedFuture(draft)
                        : ImageUploadService.get().uploadImage(selectedPujaImageFile, "puja-services")
                                .thenApply(result -> draft.withImage(result.secureUrl(), result.publicId()));
        prepared.thenAccept(service -> {
            try {
                AppDataStore.savePujaService(service);
                System.out.println("ADMIN PUJA: persistence success, serviceId=" + service.serviceId
                        + ", imageUrl present=" + !service.imageUrl.isBlank()
                        + ", imagePublicId present=" + !service.imagePublicId.isBlank());
                if (existing != null && selectedPujaImageFile != null && !existing.imagePublicId.isBlank()
                        && !existing.imagePublicId.equals(service.imagePublicId)) {
                    ImageUploadService.get().deleteImage(existing.imagePublicId);
                }
                Platform.runLater(() -> {
                    clearPujaEditorImageState();
                    editingOperationalId = "";
                    editingOperationalModule = "";
                    formFields(form).forEach(TextField::clear);
                    pujaEditorVisible = false;
                    pujaEditorDirty = false;
                    pujaEditorFocused = false;
                    showSection("Puja Services");
                    refreshAdminPujaData();
                });
            } catch (AppDataStore.ApprovalUpdateException exception) {
                Platform.runLater(() -> showInfo("Puja Service Save Failed", exception.getMessage()));
            }
        }).exceptionally(error -> {
            Platform.runLater(() -> {
                System.out.println("IMAGE: upload/persistence failure, serviceId=" + draft.serviceId
                        + ", message=" + (error.getCause() == null ? error.getMessage() : error.getCause().getMessage()));
                if (pujaImageStatus != null) {
                    pujaImageStatus.setText("Image upload failed. Form was not cleared.");
                }
                showInfo("Puja Image Upload Failed", error.getCause() == null ? error.getMessage() : error.getCause().getMessage());
            });
            return null;
        });
    }

    private void clearPujaEditorImageState() {
        selectedPujaImageFile = null;
        selectedPujaImageUrl = "";
        selectedPujaImagePublicId = "";
        selectedPujaImageRemoved = false;
    }

    private void updateSelectedPujaService(VBox form, boolean enabled, boolean adminApproved,
            String verificationStatus, String action) {
        if (editingOperationalId == null || editingOperationalId.isBlank()) {
            showInfo("Select Service", "Select a Puja service row first, then click " + action + ".");
            return;
        }
        try {
            AppDataStore.PujaServiceRecord current = adminVisiblePujaServices().stream()
                    .filter(service -> service.serviceId.equals(editingOperationalId))
                    .findFirst()
                    .orElse(null);
            if (current == null) {
                showInfo("Select Service", "Selected Puja service was not found.");
                return;
            }
            AppDataStore.savePujaService(current.withControl(
                    enabled && current.published,
                    enabled,
                    adminApproved || current.adminApproved,
                    verificationStatus == null || verificationStatus.isBlank()
                            ? current.verificationStatus
                            : verificationStatus));
            editingOperationalId = "";
            editingOperationalModule = "";
            formFields(form).forEach(TextField::clear);
            pujaEditorVisible = false;
            pujaEditorDirty = false;
            pujaEditorFocused = false;
            showSection("Puja Services");
            refreshAdminPujaData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Puja Service Update Failed", exception.getMessage());
        }
    }

    private void deleteSelectedPujaService(VBox form) {
        if (!hasSelectedPujaService()) {
            showInfo("Select Service", "Select a Puja service row first, then click Delete Selected.");
            return;
        }
        String serviceId = editingOperationalId;
        AppDataStore.PujaServiceRecord current = adminVisiblePujaServices().stream()
                .filter(service -> service.serviceId.equals(serviceId))
                .findFirst()
                .orElse(null);
        if (current == null) {
            showInfo("Select Service", "Selected Puja service was not found.");
            return;
        }
        if (!confirm("Delete Puja Service", "Delete " + valueOr(serviceId, current.name)
                + " from Firestore? This will remove it from the user page too.")) {
            return;
        }
        try {
            AppDataStore.deletePujaService(serviceId);
            editingOperationalId = "";
            editingOperationalModule = "";
            formFields(form).forEach(TextField::clear);
            pujaEditorVisible = false;
            pujaEditorDirty = false;
            pujaEditorFocused = false;
            showSection("Puja Services");
            refreshAdminPujaData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Puja Service Delete Failed", exception.getMessage());
        }
    }

    private VBox pujaServiceRegistryPanel(VBox form) {
        List<AppDataStore.PujaServiceRecord> services = adminVisiblePujaServices();
        System.out.println("ADMIN PUJA: raw service count=" + AppDataStore.pujaServices().size()
                + ", mapped/displayable count=" + services.size());
        TextField search = AppUi.textField("Search Puja, Temple, Priest or Location...");
        search.getStyleClass().add("admin-puja-search-field");
        ComboBox<String> type = adminPujaFilter("Puja Type", "All Types", "Abhishek", "Jaap", "Hawan", "Darshan", "Remote", "Prasad", "Custom");
        ComboBox<String> location = adminPujaFilter("Location", "All Locations", "Ramkund", "Trimbakeshwar", "Panchavati", "Kalaram Mandir", "Online");
        ComboBox<String> language = adminPujaFilter("Language", "Any Language", "Marathi", "Hindi", "English", "Sanskrit");
        ComboBox<String> mode = adminPujaFilter("Mode", "Any Mode", "Offline", "Online", "Both");
        ComboBox<String> price = adminPujaFilter("Price", "Any Price", "Under Rs. 500", "Rs. 500 - Rs. 1500", "Above Rs. 1500");
        CheckBox today = new CheckBox("Available Today");
        today.getStyleClass().add("puja-today-filter");

        VBox rows = new VBox(10);
        Runnable render = () -> {
            List<AppDataStore.PujaServiceRecord> filtered = services.stream()
                    .filter(service -> adminPujaServiceMatches(service, search.getText(), type.getValue(),
                            location.getValue(), language.getValue(), mode.getValue(), price.getValue(), today.isSelected()))
                    .toList();
            rows.getChildren().clear();
            if (filtered.isEmpty()) {
                rows.getChildren().add(dataRow("Puja Services", "No Puja service records",
                        "Firestore pujaServices records and existing admin Puja items will appear here."));
            } else {
                filtered.stream().limit(10).forEach(service -> rows.getChildren().add(pujaServiceRow(service, form)));
                if (filtered.size() > 10) {
                    rows.getChildren().add(dataRow("Puja Services", "Showing 1 to 10 of " + filtered.size() + " services",
                            "Use search/filter to narrow the list. Pagination-ready list avoids rendering heavy cards at once."));
                }
            }
        };
        search.textProperty().addListener((obs, oldValue, newValue) -> render.run());
        type.setOnAction(event -> render.run());
        location.setOnAction(event -> render.run());
        language.setOnAction(event -> render.run());
        mode.setOnAction(event -> render.run());
        price.setOnAction(event -> render.run());
        today.setOnAction(event -> render.run());

        Button reset = smallButton("Reset All");
        reset.setOnAction(event -> {
            search.clear();
            type.setValue("All Types");
            location.setValue("All Locations");
            language.setValue("Any Language");
            mode.setValue("Any Mode");
            price.setValue("Any Price");
            today.setSelected(false);
            render.run();
        });
        Button addNew = smallButton("+ Add New Puja Service");
        addNew.getStyleClass().add("admin-success-action");
        addNew.setOnAction(event -> {
            pujaEditorVisible = true;
            editingOperationalId = "";
            editingOperationalModule = "";
            clearPujaEditorImageState();
            showSection("Puja Services");
        });

        HBox topRow = new HBox(10, search, today, smallButton("Search"));
        ((Button) topRow.getChildren().get(2)).setOnAction(event -> render.run());
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        javafx.scene.layout.FlowPane filters = new javafx.scene.layout.FlowPane(10, 10,
                type, location, language, mode, price, reset);
        VBox searchPanel = new VBox(12, topRow, filters);
        searchPanel.getStyleClass().add("puja-filter-panel");

        Label count = muted(services.size() + " Services");
        HBox header = new HBox(10, sectionTitle("Published Puja Services"), count, createSpacer(), addNew);
        header.setAlignment(Pos.CENTER_LEFT);
        render.run();
        VBox panel = new VBox(13, searchPanel, header, rows);
        panel.getStyleClass().add("pilgrim-panel");
        return panel;
    }

    private ComboBox<String> adminPujaFilter(String prompt, String... values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(values);
        combo.setValue(values.length == 0 ? prompt : values[0]);
        combo.setPromptText(prompt);
        combo.getStyleClass().add("puja-filter-combo");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private boolean adminPujaServiceMatches(AppDataStore.PujaServiceRecord service, String query, String type,
            String location, String language, String mode, String price, boolean availableToday) {
        String search = query == null ? "" : query.trim().toLowerCase();
        boolean queryOk = search.isBlank() || (service.name + " " + service.description + " " + service.templeOrGhat
                + " " + service.pujaType + " " + service.providerName + " " + service.languages)
                .toLowerCase().contains(search);
        boolean typeOk = type == null || "All Types".equals(type) || service.pujaType.equalsIgnoreCase(type);
        boolean locationOk = location == null || "All Locations".equals(location)
                || service.templeOrGhat.toLowerCase().contains(location.toLowerCase());
        boolean languageOk = language == null || "Any Language".equals(language)
                || service.languages.toLowerCase().contains(language.toLowerCase());
        boolean modeOk = mode == null || "Any Mode".equals(mode)
                || service.mode.equalsIgnoreCase(mode) || "Both".equalsIgnoreCase(service.mode);
        boolean todayOk = !availableToday || "OPEN".equalsIgnoreCase(service.bookingStatus);
        int numericPrice = parseAdminPujaPrice(service.price);
        boolean priceOk = switch (price == null ? "Any Price" : price) {
            case "Under Rs. 500" -> numericPrice < 500;
            case "Rs. 500 - Rs. 1500" -> numericPrice >= 500 && numericPrice <= 1500;
            case "Above Rs. 1500" -> numericPrice > 1500;
            default -> true;
        };
        return queryOk && typeOk && locationOk && languageOk && modeOk && todayOk && priceOk;
    }

    private int parseAdminPujaPrice(String text) {
        try {
            String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
            return digits.isBlank() ? 0 : Integer.parseInt(digits);
        } catch (Exception exception) {
            return 0;
        }
    }

    private List<AppDataStore.PujaServiceRecord> adminVisiblePujaServices() {
        List<AppDataStore.PujaServiceRecord> services = new java.util.ArrayList<>(AppDataStore.pujaServices());
        for (AppDataStore.ServiceItem item : AppDataStore.items("puja")) {
            boolean exists = services.stream().anyMatch(service -> service.serviceId.equals(item.id)
                    || service.name.equalsIgnoreCase(item.title));
            if (!exists) {
                services.add(pujaServiceFromItem(item));
            }
        }
        return services;
    }

    private AppDataStore.PujaServiceRecord pujaServiceFromItem(AppDataStore.ServiceItem item) {
        String detail = item.detail == null ? "" : item.detail;
        return new AppDataStore.PujaServiceRecord(item.id, item.title,
                valueOr("Admin-published puja service.", detail),
                valueOr("Custom", detailValue(detail, "Puja Type")),
                valueOr("Approved Location", detailValue(detail, "Temple / Ghat")),
                valueOr("0", detailValue(detail, "Price")),
                valueOr("Varies", detailValue(detail, "Duration")),
                "",
                valueOr("Admin", detailValue(detail, "Pandit / Provider")),
                valueOr("VERIFIED", detailValue(detail, "Verification Status")),
                valueOr("Varies", detailValue(detail, "Available Slots")),
                valueOr("Offline", detailValue(detail, "Mode")),
                valueOr("Marathi, Hindi, Sanskrit", detailValue(detail, "Languages")),
                "/images/trimbakeshwar.jpg",
                valueOr("OPEN", detailValue(detail, "Booking Status")),
                "active",
                true,
                true,
                true,
                "",
                "");
    }

    private HBox pujaServiceRow(AppDataStore.PujaServiceRecord service, VBox form) {
        ImageView thumb = createPujaServiceThumbnail(service.imageUrl);
        Button edit = smallButton("Edit");
        edit.setText("Edit Service");
        edit.setOnAction(event -> {
            pujaEditorVisible = true;
            pendingPujaEditorServiceId = service.serviceId;
            showSection("Puja Services");
        });
        Button view = smallButton("View Details");
        view.setOnAction(event -> showAdminPujaServiceDetailsPage(service));
        Button publish = smallButton(service.published ? "Unpublish" : "Publish");
        publish.setOnAction(event -> updatePujaServicePublish(service, !service.published));
        Button enable = smallButton(service.enabled ? "Disable" : "Enable");
        enable.setOnAction(event -> updatePujaServiceEnabled(service, !service.enabled));
        Button delete = smallButton("Delete");
        delete.getStyleClass().add("admin-danger-action");
        delete.setOnAction(event -> {
            editingOperationalId = service.serviceId;
            editingOperationalModule = "puja";
            deleteSelectedPujaService(form);
        });
        Button bookings = smallButton("View Bookings");
        bookings.setOnAction(event -> showPujaBookingsDialog(service.serviceId));
        HBox badges = new HBox(6,
                badge(service.verificationStatus),
                badge(service.published ? "Published" : "Draft"),
                badge(service.enabled ? "Enabled" : "Disabled"));
        VBox text = new VBox(3,
                strong(valueOr(service.serviceId, service.name)),
                muted(valueOr("Admin-published puja service.", service.description)),
                muted("Type: " + service.pujaType
                        + "  •  Temple/Ghat: " + service.templeOrGhat
                        + "  •  Provider: " + valueOr("Admin", service.providerName)),
                muted("Price: Rs. " + parseAdminPujaPrice(service.price)
                        + "  •  Duration: " + service.duration
                        + "  •  Location: " + service.templeOrGhat),
                muted("Languages: " + service.languages
                        + "  •  Mode: " + service.mode
                        + "  •  Booking: " + service.bookingStatus),
                badges);
        HBox row = new HBox(12, thumb, text, createSpacer(), view, edit, publish, enable, delete, bookings);
        row.getStyleClass().add("admin-puja-service-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private ImageView createPujaServiceThumbnail(String imageUrl) {
        ImageView image = new ImageView();
        image.setFitWidth(92);
        image.setFitHeight(62);
        image.setPreserveRatio(false);
        image.getStyleClass().add("admin-puja-thumb");
        String source = valueOr("/images/trimbakeshwar.jpg", imageUrl);
        try {
            if (source.startsWith("http") || source.startsWith("file:")) {
                image.setImage(new Image(source, true));
            } else {
                URL url = getClass().getResource(source);
                if (url != null) {
                    String key = url.toExternalForm();
                    image.setImage(IMAGE_CACHE.computeIfAbsent(key, value -> new Image(value, true)));
                }
            }
        } catch (Exception ignored) {
            // Thumbnail is decorative; row remains usable if the remote image fails.
        }
        return image;
    }

    private void showAdminPujaServiceDetailsPage(AppDataStore.PujaServiceRecord service) {
        selectedSection = "Puja Services";
        setActiveSection("Puja Services");
        Button back = smallButton("← Back");
        back.setOnAction(event -> showSection("Puja Services"));

        ImageView image = createPujaServiceThumbnail(service.imageUrl);
        image.setFitWidth(340);
        image.setFitHeight(220);
        image.getStyleClass().add("puja-details-image");

        GridPane details = twoColumnGrid(
                adminPujaDetailCard("Service ID", service.serviceId),
                adminPujaDetailCard("Puja Type", service.pujaType),
                adminPujaDetailCard("Temple / Ghat", service.templeOrGhat),
                adminPujaDetailCard("Duration", service.duration),
                adminPujaDetailCard("Available Slots", service.availableSlots),
                adminPujaDetailCard("Provider", valueOr("Admin", service.providerName)),
                adminPujaDetailCard("Languages", service.languages),
                adminPujaDetailCard("Mode", service.mode),
                adminPujaDetailCard("Price", "Rs. " + parseAdminPujaPrice(service.price)),
                adminPujaDetailCard("Booking Status", service.bookingStatus));

        HBox status = new HBox(8,
                badge(service.verificationStatus),
                badge(service.published ? "Published" : "Draft"),
                badge(service.enabled ? "Enabled" : "Disabled"));
        VBox text = new VBox(12,
                sectionTitle(service.name),
                paragraph(valueOr("Admin-published puja service.", service.description)),
                status,
                details);
        HBox hero = new HBox(18, image, text);
        hero.getStyleClass().add("puja-details-panel");
        HBox.setHgrow(text, Priority.ALWAYS);

        Button edit = smallButton("Edit Service");
        edit.getStyleClass().add("admin-success-action");
        edit.setOnAction(event -> {
            pujaEditorVisible = true;
            pendingPujaEditorServiceId = service.serviceId;
            showSection("Puja Services");
        });
        Button bookings = smallButton("View Bookings");
        bookings.setOnAction(event -> showPujaBookingsDialog(service.serviceId));
        HBox actions = new HBox(10, createSpacer(), edit, bookings);
        actions.setAlignment(Pos.CENTER_LEFT);

        root.setCenter(scroll(pageShell("Puja Service Details",
                "Review the active Firestore service record used by pilgrims.",
                back,
                hero,
                infoPanel("Stored Image",
                        paragraph("imageUrl: " + service.imageUrl + "\nimagePublicId: "
                                + valueOr("Not uploaded", service.imagePublicId))),
                actions)));
    }

    private VBox adminPujaDetailCard(String title, String value) {
        VBox card = new VBox(5, muted(title), strong(valueOr("Not available", value)));
        card.getStyleClass().add("puja-mini-panel");
        return card;
    }

    private void fillPujaServiceForm(AppDataStore.PujaServiceRecord service, VBox form) {
        editingOperationalId = service.serviceId;
        editingOperationalModule = "puja";
        List<TextField> fields = formFields(form);
        String[] values = { service.name, service.description, service.pujaType, service.templeOrGhat,
                service.templeOrGhat, service.price, service.duration, service.availableSlots, service.providerName,
                service.languages, service.mode, service.verificationStatus, service.bookingStatus };
        pujaEditorUpdating = true;
        for (int i = 0; i < fields.size() && i < values.length; i++) {
            fields.get(i).setText(values[i]);
        }
        pujaEditorUpdating = false;
        pujaEditorDirty = false;
        selectedPujaImageFile = null;
        selectedPujaImageUrl = service.imageUrl;
        selectedPujaImagePublicId = service.imagePublicId;
        selectedPujaImageRemoved = false;
        setPujaImagePreview(service.imageUrl);
        if (pujaImageStatus != null) {
            pujaImageStatus.setText(service.imageUrl.startsWith("http")
                    ? "Cloudinary image loaded. Change Image only if you want to replace it."
                    : "Default image loaded. Upload Image to add a Cloudinary image.");
        }
    }

    private void protectPujaEditorFromLiveRefresh(VBox form) {
        for (TextField field : formFields(form)) {
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!pujaEditorUpdating && "Puja Services".equals(selectedSection)) {
                    pujaEditorDirty = true;
                }
            });
            field.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                if ("Puja Services".equals(selectedSection)) {
                    pujaEditorFocused = formFields(form).stream().anyMatch(TextField::isFocused);
                }
            });
        }
    }

    private boolean isPujaEditorProtectedFromRefresh() {
        return "Puja Services".equals(selectedSection) && (pujaEditorDirty || pujaEditorFocused);
    }

    private void updatePujaServicePublish(AppDataStore.PujaServiceRecord service, boolean published) {
        try {
            AppDataStore.savePujaService(service.withControl(published, service.enabled,
                    published || service.adminApproved, published ? "VERIFIED" : service.verificationStatus));
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Puja Service Update Failed", exception.getMessage());
        }
    }

    private void updatePujaServiceEnabled(AppDataStore.PujaServiceRecord service, boolean enabled) {
        try {
            AppDataStore.savePujaService(service.withControl(service.published && enabled, enabled,
                    service.adminApproved, service.verificationStatus));
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Puja Service Update Failed", exception.getMessage());
        }
    }

    private VBox pujaBookingsPanel() {
        VBox rows = new VBox(10);
        if (AppDataStore.pujaBookings().isEmpty()) {
            rows.getChildren().add(dataRow("Puja Services", "No Puja bookings yet",
                    "User pujaBookings records will appear here after pilgrims click Book Now."));
        } else {
            AppDataStore.pujaBookings().forEach(booking -> rows.getChildren().add(pujaBookingAdminRow(booking)));
        }
        return infoPanel("Puja Bookings", rows);
    }

    private HBox pujaBookingAdminRow(AppDataStore.PujaBookingRecord booking) {
        ImageView image = createPujaServiceThumbnail(adminPujaBookingImage(booking.serviceId));
        Button view = smallButton("View Details");
        view.setOnAction(event -> showAdminPujaBookingDetailsPage(booking));
        Button confirm = smallButton("Confirm");
        confirm.getStyleClass().add("admin-success-action");
        confirm.setOnAction(event -> updatePujaBookingStatus(booking, "CONFIRMED"));
        Button assign = smallButton("Priest Assigned");
        assign.setOnAction(event -> updatePujaBookingStatus(booking, "PRIEST_ASSIGNED"));
        Button complete = smallButton("Complete");
        complete.getStyleClass().add("admin-success-action");
        complete.setOnAction(event -> updatePujaBookingStatus(booking, "COMPLETED"));
        Button cancel = smallButton("Cancel");
        cancel.getStyleClass().add("admin-danger-action");
        cancel.setOnAction(event -> updatePujaBookingStatus(booking, "CANCELLED"));
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("PENDING", "CONFIRMED", "PRIEST_ASSIGNED", "READY", "IN_PROGRESS",
                "COMPLETED", "CANCELLED", "DISPUTED", "REFUNDED");
        status.getSelectionModel().select(booking.bookingStatus);
        status.getStyleClass().add("input-combo");
        Button update = smallButton("Update Status");
        update.setOnAction(event -> updatePujaBookingStatus(booking, status.getValue()));
        VBox text = new VBox(4,
                strong(valueOr(booking.bookingId, booking.serviceName)),
                muted("Booking ID: " + booking.bookingId + "  •  User: " + valueOr("Unknown", booking.userName)),
                muted("Provider: " + valueOr("Admin", booking.providerName)
                        + "  •  Date: " + valueOr("Not set", booking.date)
                        + "  •  Time: " + valueOr("Not set", booking.time)),
                muted("Location: " + valueOr("Not set", booking.location)
                        + "  •  Amount: Rs " + booking.totalAmount),
                new HBox(6, badge(booking.paymentStatus), badge(booking.bookingStatus)));
        HBox actions = new HBox(8, view, confirm, assign, complete, cancel, status, update);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(12, image, text, createSpacer(), actions);
        row.getStyleClass().add("admin-puja-booking-card");
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        return row;
    }

    private void showPujaBookingsDialog() {
        showPujaBookingsDialog("");
    }

    private void showPujaBookingsDialog(String serviceId) {
        selectedSection = "Puja Services";
        setActiveSection("Puja Services");
        List<AppDataStore.PujaBookingRecord> bookings = AppDataStore.pujaBookings().stream()
                .filter(booking -> serviceId == null || serviceId.isBlank() || booking.serviceId.equals(serviceId))
                .toList();
        TextField search = AppUi.textField("Search Booking ID, User or Puja");
        search.getStyleClass().add("admin-puja-search-field");
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("All Status", "PENDING", "PAYMENT_PENDING", "CONFIRMED", "PRIEST_ASSIGNED",
                "READY", "IN_PROGRESS", "COMPLETED", "CANCELLED", "DISPUTED", "REFUNDED");
        status.setValue("All Status");
        status.getStyleClass().add("input-combo");
        ComboBox<String> service = new ComboBox<>();
        service.getItems().add("All Services");
        AppDataStore.pujaBookings().stream()
                .map(booking -> booking.serviceName)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(service.getItems()::add);
        service.setValue("All Services");
        service.getStyleClass().add("input-combo");

        VBox rows = new VBox(10);
        Runnable render = () -> renderAdminPujaBookings(rows, bookings, search.getText(), status.getValue(), service.getValue());
        search.textProperty().addListener((obs, oldValue, newValue) -> render.run());
        search.setOnAction(event -> render.run());
        status.setOnAction(event -> render.run());
        service.setOnAction(event -> render.run());
        render.run();

        HBox filters = new HBox(10, search, status, service);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        Button back = smallButton("← Back");
        back.setOnAction(event -> showSection("Puja Services"));
        root.setCenter(scroll(pageShell("Puja Bookings",
                serviceId == null || serviceId.isBlank()
                        ? "View and manage Puja service bookings."
                        : "View and manage bookings for selected Puja service.",
                back,
                infoPanel("Search & Filters", filters),
                infoPanel("Booking Records", rows))));
    }

    private void renderAdminPujaBookings(VBox rows, List<AppDataStore.PujaBookingRecord> bookings,
            String query, String status, String service) {
        List<AppDataStore.PujaBookingRecord> filtered = bookings.stream()
                .filter(booking -> adminPujaBookingMatches(booking, query, status, service))
                .toList();
        rows.getChildren().clear();
        if (filtered.isEmpty()) {
            rows.getChildren().add(dataRow("Puja Services", "No Puja bookings found",
                    "Try changing search or filters. User pujaBookings records will appear here after pilgrims book services."));
        } else {
            filtered.forEach(booking -> rows.getChildren().add(pujaBookingAdminRow(booking)));
        }
    }

    private boolean adminPujaBookingMatches(AppDataStore.PujaBookingRecord booking, String query, String status,
            String service) {
        String search = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        String source = String.join(" ",
                valueOr("", booking.bookingId),
                valueOr("", booking.userName),
                valueOr("", booking.serviceName),
                valueOr("", booking.providerName),
                valueOr("", booking.location)).toLowerCase(java.util.Locale.ROOT);
        boolean queryOk = search.isBlank() || source.contains(search);
        boolean statusOk = status == null || "All Status".equals(status)
                || booking.bookingStatus.equalsIgnoreCase(status)
                || booking.paymentStatus.equalsIgnoreCase(status);
        boolean serviceOk = service == null || "All Services".equals(service)
                || booking.serviceName.equalsIgnoreCase(service);
        return queryOk && statusOk && serviceOk;
    }

    private String adminPujaBookingImage(String serviceId) {
        return AppDataStore.pujaServices().stream()
                .filter(service -> service.serviceId.equals(serviceId))
                .map(service -> service.imageUrl)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("/images/trimbakeshwar.jpg");
    }

    private void showAdminPujaBookingDetailsPage(AppDataStore.PujaBookingRecord booking) {
        selectedSection = "Puja Services";
        setActiveSection("Puja Services");
        Button back = smallButton("← Back");
        back.setOnAction(event -> showPujaBookingsDialog());
        ImageView image = createPujaServiceThumbnail(adminPujaBookingImage(booking.serviceId));
        image.setFitWidth(300);
        image.setFitHeight(175);
        VBox qr = booking.qrVerificationToken == null || booking.qrVerificationToken.isBlank()
                ? new VBox(8, sectionTitle("QR Verification"), muted("QR ticket has not been generated yet."))
                : new VBox(8, sectionTitle("QR Verification"),
                        muted("Ticket ID: " + valueOr("Not available", booking.qrTicketId)),
                        muted("Token: " + booking.qrVerificationToken));
        qr.getStyleClass().add("puja-details-trust-card");
        GridPane details = twoColumnGrid(
                adminPujaDetailCard("Booking ID", booking.bookingId),
                adminPujaDetailCard("User", valueOr("Not available", booking.userName)),
                adminPujaDetailCard("Puja", valueOr("Not available", booking.serviceName)),
                adminPujaDetailCard("Provider", valueOr("Not available", booking.providerName)),
                adminPujaDetailCard("Date", valueOr("Not available", booking.date)),
                adminPujaDetailCard("Time", valueOr("Not available", booking.time)),
                adminPujaDetailCard("Location", valueOr("Not available", booking.location)),
                adminPujaDetailCard("Amount", "Rs. " + booking.totalAmount),
                adminPujaDetailCard("Payment Status", booking.paymentStatus),
                adminPujaDetailCard("Booking Status", booking.bookingStatus),
                adminPujaDetailCard("Razorpay Payment", valueOr("Not available", booking.razorpayPaymentId)),
                adminPujaDetailCard("QR Status", booking.qrVerificationToken == null || booking.qrVerificationToken.isBlank()
                        ? "Not generated" : "Generated"));
        HBox hero = new HBox(16, image, new VBox(12, sectionTitle(valueOr(booking.bookingId, booking.serviceName)),
                muted("Booking ID: " + booking.bookingId), new HBox(7, badge(booking.paymentStatus), badge(booking.bookingStatus)), qr));
        hero.getStyleClass().add("puja-details-panel");
        VBox panel = new VBox(14,
                hero,
                details);
        panel.getStyleClass().add("puja-details-panel");
        root.setCenter(scroll(pageShell("Puja Booking Details",
                "Review booking, payment and QR information from the same Firestore record.",
                back,
                panel)));
    }

    private String pujaBookingDetails(AppDataStore.PujaBookingRecord booking) {
        return "Booking ID: " + booking.bookingId
                + "\nUser: " + valueOr("Not available", booking.userName)
                + "\nPuja: " + valueOr("Not available", booking.serviceName)
                + "\nProvider: " + valueOr("Not available", booking.providerName)
                + "\nDate: " + valueOr("Not available", booking.date)
                + "\nTime: " + valueOr("Not available", booking.time)
                + "\nLocation: " + valueOr("Not available", booking.location)
                + "\nDevotees: " + booking.devoteesCount
                + "\nLanguage: " + valueOr("Not available", booking.language)
                + "\nMode: " + valueOr("Not available", booking.mode)
                + "\nSamagri: " + (booking.samagriSelected ? "Included - Rs " + booking.samagriAmount : "Not included")
                + "\nPrasad: " + (booking.prasadSelected ? "Included - Rs " + booking.prasadAmount : "Not included")
                + "\nBase Price: Rs " + booking.basePrice
                + "\nService Fee: Rs " + booking.serviceFee
                + "\nTotal Amount: Rs " + booking.totalAmount
                + "\nSpecial Requirements: " + valueOr("None", booking.specialRequirements)
                + "\nPayment Status: " + booking.paymentStatus
                + "\nPayment Method: " + valueOr("Not started", booking.paymentMethod)
                + "\nInternal Payment ID: " + valueOr("Not available", booking.internalPaymentId)
                + "\nRazorpay Order ID: " + valueOr("Not available", booking.razorpayOrderId)
                + "\nRazorpay Payment ID: " + valueOr("Not available", booking.razorpayPaymentId)
                + "\nPayment Failure Reason: " + valueOr("None", booking.paymentFailureReason)
                + "\nBooking Status: " + booking.bookingStatus
                + "\nCreated: " + valueOr("Not available", booking.createdAt);
    }

    private void updatePujaBookingStatus(AppDataStore.PujaBookingRecord booking, String status) {
        if (!confirm("Update Puja Booking", "Set " + booking.bookingId + " to " + status + "?")) {
            return;
        }
        try {
            AppDataStore.updatePujaBookingStatus(booking.bookingId, status);
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Booking Update Failed", exception.getMessage());
        }
    }

    private VBox pujaFraudReportsPanel() {
        VBox rows = new VBox(10);
        List<AppDataStore.FraudReportRecord> reports = AppDataStore.fraudReports();
        List<AppDataStore.PujaBookingRecord> disputes = AppDataStore.pujaBookings().stream()
                .filter(booking -> "DISPUTED".equalsIgnoreCase(booking.bookingStatus))
                .toList();
        if (reports.isEmpty() && disputes.isEmpty()) {
            rows.getChildren().add(dataRow("Puja Services", "No disputes or fraud reports",
                    "User disputes and fraudReports records will appear here."));
        } else {
            for (AppDataStore.PujaBookingRecord booking : disputes) {
                rows.getChildren().add(dataRow("Puja Services", "Disputed Booking: " + booking.bookingId,
                        booking.serviceName + " | User: " + booking.userName + " | Status: " + booking.bookingStatus));
            }
            for (AppDataStore.FraudReportRecord report : reports) {
                rows.getChildren().add(dataRow("Puja Services", "Fraud Report: " + report.reportId,
                        valueOr("Unknown service", report.serviceName)
                                + " | User: " + valueOr("Unknown", report.userName)
                                + " | Provider: " + valueOr("Not available", report.providerName)
                                + " | Status: " + report.status));
            }
        }
        return infoPanel("Puja Disputes & Fraud Reports", rows);
    }

    private VBox pujaProviderVerificationPanel() {
        VBox rows = new VBox(10);
        List<AppDataStore.PujaProviderRecord> providers = AppDataStore.pujaProviders();
        if (providers.isEmpty()) {
            rows.getChildren().add(dataRow("Puja Services", "No provider registrations",
                    "Provider Registration submissions from pujaProviders will appear here."));
        } else {
            providers.forEach(provider -> rows.getChildren().add(pujaProviderRow(provider)));
        }
        HBox stats = new HBox(12,
                metric(String.valueOf(providers.stream().filter(p -> "pending".equalsIgnoreCase(p.status)).count()), "Pending"),
                metric(String.valueOf(providers.stream().filter(p -> p.approved).count()), "Approved"),
                metric(String.valueOf(providers.stream().filter(p -> "suspended".equalsIgnoreCase(p.status)).count()), "Suspended"),
                metric(String.valueOf(providers.stream().filter(p -> "banned".equalsIgnoreCase(p.status)).count()), "Banned"));
        VBox panel = infoPanel("Provider Verification", stats, rows);
        return panel;
    }

    private HBox pujaProviderRow(AppDataStore.PujaProviderRecord provider) {
        Button view = smallButton("View Documents");
        view.setOnAction(event -> showPujaProviderDetails(provider));

        Button approve = smallButton("Approve");
        approve.getStyleClass().add("admin-success-action");
        approve.setOnAction(event -> updatePujaProvider(provider, "approved", true, "Approve"));

        Button reject = smallButton("Reject");
        reject.getStyleClass().add("admin-danger-action");
        reject.setOnAction(event -> updatePujaProvider(provider, "rejected", false, "Reject"));

        Button suspend = smallButton("Suspend");
        suspend.setOnAction(event -> updatePujaProvider(provider, "suspended", false, "Suspend"));

        Button ban = smallButton("Ban");
        ban.getStyleClass().add("admin-danger-action");
        ban.setOnAction(event -> updatePujaProvider(provider, "banned", false, "Ban"));

        Button enable = smallButton("Re-enable");
        enable.getStyleClass().add("admin-success-action");
        enable.setOnAction(event -> updatePujaProvider(provider, "approved", true, "Re-enable"));

        boolean approved = "approved".equalsIgnoreCase(provider.status) || "active".equalsIgnoreCase(provider.status);
        approve.setDisable(approved);
        reject.setDisable("rejected".equalsIgnoreCase(provider.status));
        suspend.setDisable("suspended".equalsIgnoreCase(provider.status) || "banned".equalsIgnoreCase(provider.status));
        ban.setDisable("banned".equalsIgnoreCase(provider.status));
        enable.setDisable(approved);

        VBox text = new VBox(2,
                strong(valueOr(provider.providerId, provider.fullName)),
                muted("Status: " + provider.status + " | Approved: " + provider.approved
                        + " | Phone: " + valueOr("Not available", provider.phone)
                        + " | Services: " + valueOr("Not listed", provider.servicesOffered)
                        + " | Locations: " + valueOr("Not listed", provider.serviceLocations)));
        HBox row = new HBox(10, moduleIcon("Puja Services", "pilgrim-row-icon"),
                text, createSpacer(), view, approve, reject, suspend, ban, enable);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showPujaProviderDetails(AppDataStore.PujaProviderRecord provider) {
        selectedSection = "Puja Services";
        setActiveSection("Puja Services");
        HBox status = new HBox(8, badge(provider.status), badge(provider.approved ? "Simhastha Verified" : "Pending Review"));
        GridPane details = twoColumnGrid(
                adminPujaDetailCard("Provider ID", provider.providerId),
                adminPujaDetailCard("Phone", provider.phone),
                adminPujaDetailCard("Email", provider.email),
                adminPujaDetailCard("Experience", provider.experience),
                adminPujaDetailCard("Specialization", provider.specialization),
                adminPujaDetailCard("Languages", provider.languages),
                adminPujaDetailCard("Temple / Organization", provider.templeOrganization),
                adminPujaDetailCard("Service Locations", provider.serviceLocations),
                adminPujaDetailCard("Identity Document", provider.identityDocument),
                adminPujaDetailCard("Supporting Certificates", provider.supportingCertificates));
        VBox hero = new VBox(12,
                sectionTitle(valueOr("Puja Provider", provider.fullName)),
                paragraph(valueOr("No address submitted.", provider.address)),
                status,
                details,
                infoPanel("Services Offered", paragraph(valueOr("Not listed", provider.servicesOffered))));
        hero.getStyleClass().add("puja-details-panel");
        Button back = smallButton("← Back to Puja Services");
        back.setOnAction(event -> showSection("Puja Services"));
        Button approve = smallButton("Approve");
        approve.getStyleClass().add("admin-success-action");
        approve.setOnAction(event -> updatePujaProvider(provider, "approved", true, "Approve"));
        Button reject = smallButton("Reject");
        reject.getStyleClass().add("admin-danger-action");
        reject.setOnAction(event -> updatePujaProvider(provider, "rejected", false, "Reject"));
        HBox actions = new HBox(10, back, createSpacer(), approve, reject);
        actions.setAlignment(Pos.CENTER_LEFT);
        root.setCenter(scroll(pageShell("Provider Verification Details",
                "Review documents before approving a priest/provider.",
                hero,
                actions)));
    }

    private void updatePujaProvider(AppDataStore.PujaProviderRecord provider, String status, boolean approved,
            String action) {
        if (!confirm(action + " Puja Provider", action + " " + valueOr(provider.providerId, provider.fullName) + "?")) {
            return;
        }
        try {
            AppDataStore.updatePujaProviderStatus(provider.providerId, status, approved);
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Provider Update Failed", exception.getMessage());
        }
    }

    private VBox ghatsPage() {
        VBox rows = new VBox(8);
        managedGhatRows = rows;
        List<Ghat> initial = managedGhats.isEmpty() ? ghatCatalogueService.catalogue() : managedGhats;
        renderManagedGhats(rows, initial);
        Button add = new Button("+  Add New Ghat");
        add.getStyleClass().addAll("primary-button", "ghat-primary-action");
        add.setOnAction(event -> showGhatEditor(null));
        Button refresh = smallButton("Refresh");
        refresh.getStyleClass().add("ghat-refresh-action");
        refresh.setOnAction(event -> refreshManagedGhats(rows));

        VBox actionStack = new VBox(8, add, refresh);
        actionStack.setAlignment(Pos.CENTER_RIGHT);
        actionStack.setMinWidth(166);

        HBox metrics = new HBox(0,
                ghatMetric("\uE707", "Total Ghats", String.valueOf(initial.size()), "Published: " + initial.stream().filter(Ghat::published).count(), "orange"),
                ghatMetric("\uE802", "Active for Snan", String.valueOf(initial.stream().filter(Ghat::isRecommendedForSnan).count()), "Draft: " + initial.stream().filter(ghat -> !ghat.published()).count(), "blue"),
                ghatMetric("\uE73E", "Information Only", String.valueOf(initial.stream().filter(ghat -> ghat.operationalStatus() == Ghat.OperationalStatus.INFORMATION_ONLY).count()), "Unavailable: " + initial.stream().filter(ghat -> !ghat.active()).count(), "green"),
                ghatMetric("\uE890", "Crowd Alerts", String.valueOf(initial.stream().filter(this::ghatNeedsAttention).count()), "Live watch", "purple"));
        HBox.setHgrow(metrics, Priority.ALWAYS);

        HBox overview = new HBox(14, metrics, actionStack);
        overview.getStyleClass().add("ghat-overview-card");
        overview.setAlignment(Pos.CENTER_LEFT);

        TextField search = AppUi.textField("Search ghats by name, location...");
        search.setText(ghatSearchQuery);
        search.getStyleClass().add("ghat-search-field");
        ComboBox<String> location = combo("All Locations", "Panchavati", "Nashik", "Trimbakeshwar");
        location.setValue(ghatLocationFilter);
        ComboBox<String> status = combo("All Status", "Published", "Draft", "Active", "Inactive", "Snan Available", "Wait High");
        status.setValue(ghatStatusFilter);
        ComboBox<String> type = combo("All Types", "Bathing Ghat", "Information Only");
        type.setValue(ghatTypeFilter);
        ComboBox<String> sort = combo("Name A-Z", "Name Z-A", "Wait High", "Crowd High");
        sort.setValue(ghatSort);
        Button filter = smallButton("Filter");
        filter.getStyleClass().add("ghat-filter-button");

        Runnable applyFilters = () -> {
            ghatSearchQuery = search.getText() == null ? "" : search.getText().trim();
            ghatLocationFilter = location.getValue() == null ? "All Locations" : location.getValue();
            ghatStatusFilter = status.getValue() == null ? "All Status" : status.getValue();
            ghatTypeFilter = type.getValue() == null ? "All Types" : type.getValue();
            ghatSort = sort.getValue() == null ? "Name A-Z" : sort.getValue();
            renderManagedGhats(rows, managedGhats.isEmpty() ? ghatCatalogueService.catalogue() : managedGhats);
        };
        search.textProperty().addListener((observable, oldValue, newValue) -> applyFilters.run());
        location.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters.run());
        status.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters.run());
        type.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters.run());
        sort.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters.run());
        filter.setOnAction(event -> applyFilters.run());

        VBox locationControl = ghatFilterControl("Location", location);
        VBox statusControl = ghatFilterControl("Status", status);
        VBox typeControl = ghatFilterControl("Type", type);
        VBox sortControl = ghatFilterControl("Sort By", sort);
        HBox filters = new HBox(10, search, locationControl, statusControl, typeControl, createSpacer(), sortControl, filter);
        filters.getStyleClass().add("ghat-filter-card");
        filters.setAlignment(Pos.CENTER_LEFT);
        search.setMinWidth(230);
        search.setPrefWidth(270);
        search.setMaxWidth(310);
        HBox.setHgrow(search, Priority.ALWAYS);

        VBox table = new VBox(0, ghatTableHeader(), rows, ghatTableFooter(initial.size()));
        table.getStyleClass().add("ghat-table-card");
        refreshManagedGhats(rows);
        return pageShell("Ghats & Snan Management", "Manage ghats, snan points and their operational status for pilgrims.",
                overview, filters, table);
    }

    private void refreshManagedGhats(VBox rows) {
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        ghatService.loadAdminGhats(token).whenComplete((ghats, error) -> Platform.runLater(() -> {
            managedGhats = ghats == null || ghats.isEmpty() ? ghatCatalogueService.catalogue() : ghats;
            renderManagedGhats(rows, managedGhats);
            if (error != null) showInfo("Ghat data unavailable", "Showing catalogue information. Backend changes could not be loaded.");
        }));
    }

    private void renderManagedGhats(VBox rows, List<Ghat> ghats) {
        rows.getChildren().clear();
        List<Ghat> visible = ghats.stream()
                .filter(this::matchesGhatFilters)
                .sorted(this::compareManagedGhats)
                .toList();
        if (visible.isEmpty()) {
            rows.getChildren().add(ghatEmptyRow());
            return;
        }
        for (Ghat ghat : visible) rows.getChildren().add(managedGhatRow(ghat));
    }

    private GridPane managedGhatRow(Ghat ghat) {
        ImageView image = ghatImageService.createView(ghat, 112, 68);
        image.getStyleClass().add("ghat-row-image");
        VBox name = new VBox(5, strong(ghat.name()), muted(valueOr("Nashik", ghat.area())), ghatTypeBadge(ghat));
        name.setAlignment(Pos.CENTER_LEFT);
        name.setMaxWidth(150);
        HBox nameCell = new HBox(11, image, name);
        nameCell.setAlignment(Pos.CENTER_LEFT);

        Button edit = ghatRowAction("Edit"); edit.getStyleClass().add("ghat-action-edit"); edit.setOnAction(event -> showGhatEditor(ghat));
        Button live = ghatRowAction("Update Live"); live.setOnAction(event -> showGhatEditor(ghat, true));
        Button imageAction = ghatRowAction("Change Image"); imageAction.setOnAction(event -> chooseGhatImage(ghat, imageAction));
        Button publish = ghatRowAction(ghat.published() ? "Unpublish" : "Publish"); publish.setOnAction(event -> saveGhat(withFlags(ghat, !ghat.published(), ghat.active())));
        Button active = ghatRowAction(ghat.active() ? "Deactivate" : "Activate"); active.setOnAction(event -> saveGhat(withFlags(ghat, ghat.published(), !ghat.active())));
        VBox actions = new VBox(5, edit, live, imageAction, publish, active);
        actions.setAlignment(Pos.CENTER_LEFT);

        GridPane row = new GridPane();
        row.getStyleClass().add("ghat-table-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setHgap(0);
        addGhatCell(row, nameCell, 0, 304);
        addGhatCell(row, ghatInfoCell("\uE707", locationPrimary(ghat), "Nashik"), 1, 118);
        addGhatCell(row, ghatInfoCell("\uE946", ghatTypeText(ghat), ghat.operationalStatus() == Ghat.OperationalStatus.INFORMATION_ONLY ? "Info" : "Managed"), 2, 120);
        addGhatCell(row, ghatStatusCell(ghat), 3, 108);
        addGhatCell(row, ghatSnanCell(ghat), 4, 144);
        addGhatCell(row, ghatUpdatedCell(ghat), 5, 116);
        addGhatCell(row, actions, 6, 154);
        return row;
    }

    private HBox ghatMetric(String glyph, String title, String value, String detail, String tone) {
        Label icon = AppUi.symbolIcon(glyph, "ghat-metric-icon");
        icon.getStyleClass().add("ghat-metric-icon-" + tone);
        VBox copy = new VBox(1, label(title, "ghat-metric-title"), label(value, "ghat-metric-value"), label(detail, "ghat-metric-detail"));
        HBox metric = new HBox(12, icon, copy);
        metric.getStyleClass().add("ghat-metric-card");
        metric.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(metric, Priority.ALWAYS);
        return metric;
    }

    private VBox ghatFilterControl(String title, Node control) {
        Label label = label(title, "ghat-filter-label");
        VBox box = new VBox(5, label, control);
        box.setMinWidth(126);
        box.setPrefWidth(142);
        box.setMaxWidth(156);
        return box;
    }

    private GridPane ghatTableHeader() {
        GridPane header = new GridPane();
        header.getStyleClass().add("ghat-table-header");
        header.setAlignment(Pos.CENTER_LEFT);
        addGhatHeader(header, "Ghat Name", 0, 304);
        addGhatHeader(header, "Location", 1, 118);
        addGhatHeader(header, "Type", 2, 120);
        addGhatHeader(header, "Status", 3, 108);
        addGhatHeader(header, "Snan Status", 4, 144);
        addGhatHeader(header, "Last Updated", 5, 116);
        addGhatHeader(header, "Actions", 6, 154);
        return header;
    }

    private Button ghatRowAction(String text) {
        Button button = smallButton(text);
        button.getStyleClass().add("ghat-row-action");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private HBox ghatTableFooter(int total) {
        Label count = muted("Showing 1 to " + Math.min(10, Math.max(total, 0)) + " of " + total + " ghats");
        Button previous = smallButton("\uE76B");
        previous.getStyleClass().add("ghat-page-button");
        previous.setDisable(true);
        Button one = smallButton("1");
        one.getStyleClass().add("ghat-page-button-active");
        Button two = smallButton("2");
        two.getStyleClass().add("ghat-page-button");
        Button three = smallButton("3");
        three.getStyleClass().add("ghat-page-button");
        Button next = smallButton("\uE76C");
        next.getStyleClass().add("ghat-page-button");
        ComboBox<String> pageSize = combo("10", "20", "50");
        pageSize.getStyleClass().add("ghat-page-size");
        HBox pages = new HBox(8, previous, one, two, three, next);
        pages.setAlignment(Pos.CENTER);
        HBox footer = new HBox(12, count, createSpacer(), pages, createSpacer(), muted("Rows per page"), pageSize);
        footer.getStyleClass().add("ghat-table-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
    }

    private void addGhatHeader(GridPane grid, String text, int column, double width) {
        Label label = label(text, "ghat-table-heading");
        addGhatCell(grid, label, column, width);
    }

    private void addGhatCell(GridPane grid, Node node, int column, double width) {
        StackPane wrapper = new StackPane(node);
        wrapper.getStyleClass().add("ghat-table-cell");
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setMinWidth(width);
        wrapper.setPrefWidth(width);
        wrapper.setMaxWidth(width);
        grid.add(wrapper, column, 0);
    }

    private VBox ghatInfoCell(String glyph, String primary, String secondary) {
        Label icon = AppUi.symbolIcon(glyph, "ghat-cell-icon");
        VBox copy = new VBox(2, label(primary, "ghat-cell-primary"), label(secondary, "ghat-cell-secondary"));
        HBox row = new HBox(9, icon, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(row);
    }

    private VBox ghatStatusCell(Ghat ghat) {
        Label dot = label("\uE73E", "ghat-status-dot");
        Label status = label(ghat.published() ? "Published" : "Draft", "ghat-cell-primary");
        Label audience = label(ghat.published() ? "Public" : "Private", "ghat-cell-secondary");
        HBox top = new HBox(8, dot, new VBox(2, status, audience));
        top.setAlignment(Pos.CENTER_LEFT);
        VBox cell = new VBox(top);
        if (!ghat.active()) cell.getStyleClass().add("ghat-status-inactive");
        return cell;
    }

    private VBox ghatSnanCell(Ghat ghat) {
        String title = ghat.operationalState().bathingStatus() == GhatOperationalState.BathingStatus.AVAILABLE
                ? "Snan Available"
                : "Wait Time High";
        String detail = ghat.operationalState().bathingStatus() == GhatOperationalState.BathingStatus.AVAILABLE
                ? "5:00 AM - 10:00 PM"
                : "7:00 AM - 12:00 PM";
        String glyph = ghat.operationalState().bathingStatus() == GhatOperationalState.BathingStatus.AVAILABLE ? "\uEC92" : "\uE916";
        return ghatInfoCell(glyph, title, detail);
    }

    private VBox ghatUpdatedCell(Ghat ghat) {
        Label date = label(formatGhatUpdated(ghat.lastUpdated()), "ghat-cell-primary");
        Label admin = label("by Admin", "ghat-cell-secondary");
        return new VBox(2, date, admin);
    }

    private String formatGhatUpdated(String raw) {
        if (raw == null || raw.isBlank()) return "9 Sep 2026\n02:30 PM";
        String value = raw.trim();
        if (value.matches("\\d{11,}")) {
            try {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(value)), ZoneId.systemDefault())
                        .format(GHAT_ROW_TIME);
            } catch (RuntimeException ignored) {
                return value;
            }
        }
        return value.replace(" | ", "\n");
    }

    private Label ghatTypeBadge(Ghat ghat) {
        Label badge = label(ghatTypeText(ghat), "ghat-type-badge");
        badge.getStyleClass().add(ghat.operationalStatus() == Ghat.OperationalStatus.INFORMATION_ONLY ? "ghat-type-info" : "ghat-type-bathing");
        return badge;
    }

    private String ghatTypeText(Ghat ghat) {
        return ghat.operationalStatus() == Ghat.OperationalStatus.INFORMATION_ONLY ? "Information Only" : "Bathing Ghat";
    }

    private String locationPrimary(Ghat ghat) {
        String area = valueOr("Panchavati", ghat.area());
        int comma = area.indexOf(',');
        return comma > 0 ? area.substring(0, comma).trim() : area;
    }

    private boolean ghatNeedsAttention(Ghat ghat) {
        return ghat.crowdLevel() == Ghat.CrowdLevel.HIGH
                || ghat.crowdLevel() == Ghat.CrowdLevel.CRITICAL
                || (ghat.estimatedWaitMinutes() != null && ghat.estimatedWaitMinutes() >= 30)
                || ghat.operationalState().waterSafety() != GhatOperationalState.WaterSafety.NORMAL;
    }

    private boolean matchesGhatFilters(Ghat ghat) {
        boolean queryMatch = matches(ghatSearchQuery, ghat.name(), ghat.area(), ghat.description(), ghatTypeText(ghat));
        boolean locationMatch = "All Locations".equals(ghatLocationFilter) || valueOr("", ghat.area()).contains(ghatLocationFilter);
        boolean typeMatch = "All Types".equals(ghatTypeFilter) || ghatTypeText(ghat).equals(ghatTypeFilter);
        boolean statusMatch = switch (ghatStatusFilter) {
            case "Published" -> ghat.published();
            case "Draft" -> !ghat.published();
            case "Active" -> ghat.active();
            case "Inactive" -> !ghat.active();
            case "Snan Available" -> ghat.operationalState().bathingStatus() == GhatOperationalState.BathingStatus.AVAILABLE;
            case "Wait High" -> ghat.estimatedWaitMinutes() != null && ghat.estimatedWaitMinutes() >= 30;
            default -> true;
        };
        return queryMatch && locationMatch && typeMatch && statusMatch;
    }

    private int compareManagedGhats(Ghat left, Ghat right) {
        return switch (ghatSort) {
            case "Name Z-A" -> right.name().compareToIgnoreCase(left.name());
            case "Wait High" -> Integer.compare(right.estimatedWaitMinutes() == null ? -1 : right.estimatedWaitMinutes(),
                    left.estimatedWaitMinutes() == null ? -1 : left.estimatedWaitMinutes());
            case "Crowd High" -> Integer.compare(crowdRank(right.crowdLevel()), crowdRank(left.crowdLevel()));
            default -> left.name().compareToIgnoreCase(right.name());
        };
    }

    private int crowdRank(Ghat.CrowdLevel level) {
        return switch (level) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MODERATE -> 2;
            case LOW -> 1;
            default -> 0;
        };
    }

    private HBox ghatEmptyRow() {
        HBox row = new HBox(10, moduleIcon("Ghats & Snan", "pilgrim-row-icon"),
                new VBox(2, strong("No ghats match these filters"), muted("Clear the search or choose All filters to see every record.")));
        row.getStyleClass().add("ghat-table-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void chooseGhatImage(Ghat ghat, Button trigger) {
        java.util.logging.Logger.getLogger(AdminDashboardPage.class.getName())
                .info("Ghat image picker opened: ghatId=" + ghat.id()
                        + ", existingImageUrlPresent=" + (ghat.imageUrl() != null && !ghat.imageUrl().isBlank())
                        + ", existingPublicIdPresent=" + (ghat.imagePublicId() != null && !ghat.imagePublicId().isBlank()));
        java.io.File file = ImageMediaHelper.chooseImage(stage, "Choose Ghat image");
        if (file == null) return;
        try {
            ImageMediaHelper.validateImage(file);
            Ghat previewGhat = copyGhat(ghat, ghat.name(), ghat.area(), ghat.description(), file.toURI().toString(), ghat.crowdLevel(), ghat.estimatedWaitMinutes(),
                    ghat.operationalStatus(), ghat.operationalState().bathingStatus(), ghat.operationalState().waterSafety(), ghat.published(), ghat.active());
            Alert preview = new Alert(Alert.AlertType.CONFIRMATION, "Preview updated. Save this image to the shared Ghat record?", ButtonType.OK, ButtonType.CANCEL);
            preview.setTitle("Save Ghat image");
            preview.setHeaderText(ghat.name());
            preview.setGraphic(ghatImageService.createView(previewGhat, 210, 130));
            AppUi.styleDialog(preview, stage, "ghat-image-dialog", ButtonType.OK);
            if (preview.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            setGhatImageBusy(trigger, "Uploading...");
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    java.util.logging.Logger.getLogger(AdminDashboardPage.class.getName())
                            .info("Ghat Cloudinary upload started: ghatId=" + ghat.id());
                    CloudinaryUploadResult uploaded = new CloudinaryService().uploadImage(file, CloudinaryFolders.GHAT_SNAN);
                    java.util.logging.Logger.getLogger(AdminDashboardPage.class.getName())
                            .info("Ghat Cloudinary upload success: ghatId=" + ghat.id()
                                    + ", secureUrlPresent=" + (uploaded.getSecureUrl() != null && uploaded.getSecureUrl().startsWith("https://"))
                                    + ", publicIdPresent=" + (uploaded.getPublicId() != null && !uploaded.getPublicId().isBlank()));
                    return uploaded;
                } catch (Exception exception) {
                    java.util.logging.Logger.getLogger(AdminDashboardPage.class.getName())
                            .log(java.util.logging.Level.WARNING, "Ghat Cloudinary upload failed: ghatId=" + ghat.id(), exception);
                    throw new java.util.concurrent.CompletionException(exception);
                }
            }).whenComplete((uploaded, error) -> Platform.runLater(() -> {
                if (error != null) {
                    setGhatImageReady(trigger);
                    showInfo("Image update failed", "The selected image could not be uploaded. No Ghat record was changed.");
                    return;
                }
                ghatImageService.invalidate(ghat.imageUrl());
                setGhatImageBusy(trigger, "Saving...");
                updateGhatImage(ghat, uploaded, trigger);
            }));
        } catch (IllegalArgumentException exception) {
            setGhatImageReady(trigger);
            showInfo("Image update failed", exception.getMessage());
        }
    }

    private void showGhatEditor(Ghat source) { showGhatEditor(source, false); }

    private void showGhatEditor(Ghat source, boolean liveOnly) {
        Ghat ghat = source == null ? newGhatDraft() : source;
        String titleText = liveOnly ? "Update Live Ghat Status" : source == null ? "Add New Ghat" : "Edit Ghat";
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(titleText);
        dialog.initOwner(stage);
        dialog.getDialogPane().getStyleClass().add("ghat-editor-dialog-pane");
        dialog.getDialogPane().setPrefWidth(liveOnly ? 640 : 760);
        VBox form = new VBox(14);
        form.getStyleClass().add("ghat-editor-form");
        form.setPadding(new Insets(14));
        TextField name = editorField(ghat.name(), "Ghat Name"); TextField area = editorField(ghat.area(), "Area / Region");
        TextField description = editorField(ghat.description(), "Description"); TextField imageUrl = editorField(ghat.imageUrl(), "Image URL / shared media reference");
        TextField latitude = editorField(numberText(ghat.latitude()), "Latitude"); TextField longitude = editorField(numberText(ghat.longitude()), "Longitude");
        TextField history = editorField(ghat.history().historicalBackground(), "History"); TextField significance = editorField(ghat.history().religiousSignificance(), "Religious Significance");
        TextField simhastha = editorField(ghat.history().simhasthaConnection(), "Simhastha Connection"); TextField facilities = editorField(String.join(", ", ghat.facilities()), "Facilities (comma separated)");
        TextField steps = editorField(numberText(ghat.walking().approximateSteps()), "Steps"); TextField distance = editorField(numberText(ghat.walking().distanceMeters()), "Walking Distance (m)");
        TextField wait = editorField(numberText(ghat.estimatedWaitMinutes()), "Estimated Wait (minutes)"); TextField restriction = editorField(ghat.operationalState().restrictionReason(), "Restriction reason");
        TextField gates = editorField(gateText(ghat), "Gates: Name|STATUS, ..."); TextField zones = editorField(zoneText(ghat), "Zones: Name|STATUS, ...");
        TextField hazards = editorField(hazardText(ghat), "Hazards: TYPE|message, ..."); TextField alert = editorField(ghat.operationalState().priorityAlert().message(), "Priority alert");
        TextField weatherTemp = editorField(numberText(ghat.weather().temperatureCelsius()), "Weather Temperature (C)");
        TextField weatherCondition = editorField(ghat.weather().condition(), "Weather Condition");
        ComboBox<Ghat.WalkingDifficulty> walking = enumBox(Ghat.WalkingDifficulty.values(), ghat.walking().difficulty());
        ComboBox<Ghat.CrowdLevel> crowd = ghatCrowdBox(ghat.crowdLevel());
        ComboBox<Ghat.OperationalStatus> operational = ghatOperationalBox(ghat.operationalStatus());
        ComboBox<GhatOperationalState.BathingStatus> bathing = enumBox(GhatOperationalState.BathingStatus.values(), ghat.operationalState().bathingStatus());
        ComboBox<GhatOperationalState.WaterSafety> water = enumBox(GhatOperationalState.WaterSafety.values(), ghat.operationalState().waterSafety());
        ComboBox<GhatOperationalState.AlertPriority> priority = enumBox(GhatOperationalState.AlertPriority.values(), ghat.operationalState().priorityAlert().priority());
        CheckBox senior = new CheckBox("Senior Friendly"); senior.setSelected(ghat.walking().seniorFriendly());
        CheckBox wheelchair = new CheckBox("Wheelchair Accessible"); wheelchair.setSelected(ghat.walking().wheelchairAccessible());
        CheckBox published = new CheckBox("Published"); published.setSelected(ghat.published());
        CheckBox active = new CheckBox("Active"); active.setSelected(ghat.active());
        senior.getStyleClass().add("ghat-editor-check");
        wheelchair.getStyleClass().add("ghat-editor-check");
        published.getStyleClass().add("ghat-editor-check");
        active.getStyleClass().add("ghat-editor-check");
        HBox flags = new HBox(14, senior, wheelchair, published, active);
        flags.getStyleClass().add("ghat-editor-flags");
        flags.setAlignment(Pos.CENTER_LEFT);
        if (liveOnly) {
            form.getChildren().addAll(
                    ghatEditorHeader(titleText, ghat.name().isBlank() ? "Create operational state for this Ghat." : ghat.name()),
                    ghatEditorSection("Live Operations", ghatEditorGrid(labeled("Crowd Level", crowd), labeled("Estimated Wait (minutes)", wait),
                            labeled("Operational Status", operational), labeled("Snan Status", bathing), labeled("Water Safety", water),
                            labeled("Walking Difficulty", walking))),
                    ghatEditorSection("Access & Conditions", ghatEditorGrid(labeled("Steps", steps), labeled("Walking Distance (m)", distance),
                            labeled("Weather Temperature (C)", weatherTemp), labeled("Weather Condition", weatherCondition),
                            labeled("Restriction reason", restriction), labeled("Priority alert", alert))),
                    ghatEditorSection("Zones & Flags", ghatEditorGrid(labeled("Gates: Name|STATUS, ...", gates), labeled("Zones: Name|STATUS, ...", zones),
                            labeled("Hazards: TYPE|message, ...", hazards), labeled("Alert Priority", priority)), flags));
        } else {
            form.getChildren().addAll(
                    ghatEditorHeader(titleText, "Manage the public Ghat profile and live pilgrim-facing details."),
                    ghatEditorSection("Basic Details", ghatEditorGrid(labeled("Ghat Name", name), labeled("Area / Region", area),
                            labeled("Description", description), labeled("Image URL / shared media reference", imageUrl),
                            labeled("Latitude", latitude), labeled("Longitude", longitude))),
                    ghatEditorSection("History & Facilities", ghatEditorGrid(labeled("History", history), labeled("Religious Significance", significance),
                            labeled("Simhastha Connection", simhastha), labeled("Facilities (comma separated)", facilities))),
                    ghatEditorSection("Operations", ghatEditorGrid(labeled("Walking Difficulty", walking), labeled("Steps", steps),
                            labeled("Walking Distance (m)", distance), labeled("Crowd Level", crowd), labeled("Estimated Wait (minutes)", wait),
                            labeled("Weather Temperature (C)", weatherTemp), labeled("Weather Condition", weatherCondition),
                            labeled("Operational Status", operational), labeled("Snan Status", bathing), labeled("Water Safety", water))),
                    ghatEditorSection("Safety & Publishing", ghatEditorGrid(labeled("Restriction reason", restriction), labeled("Priority alert", alert),
                            labeled("Gates: Name|STATUS, ...", gates), labeled("Zones: Name|STATUS, ...", zones),
                            labeled("Hazards: TYPE|message, ...", hazards), labeled("Alert Priority", priority)), flags));
        }
        ScrollPane scroll = new ScrollPane(form);
        scroll.getStyleClass().add("ghat-editor-scroll");
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(liveOnly ? 520 : 560);
        dialog.getDialogPane().setContent(scroll);
        ButtonType save = new ButtonType(source == null ? "Save Draft / Publish" : "Save", ButtonType.OK.getButtonData()); dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        AppUi.styleDialog(dialog, stage, "ghat-editor-dialog-pane", save);
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (saveButton != null) saveButton.getStyleClass().add("ghat-editor-save");
        if (cancelButton != null) cancelButton.getStyleClass().add("ghat-editor-cancel");
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != save) return;
        if (!liveOnly && name.getText().trim().isBlank()) { showInfo("Ghat name required", "Enter a Ghat name before saving."); return; }
        Ghat.Walking updatedWalking = new Ghat.Walking(walking.getValue(), parseInteger(steps.getText()), parseInteger(distance.getText()), senior.isSelected(), wheelchair.isSelected());
        Ghat.Weather updatedWeather = new Ghat.Weather(parseInteger(weatherTemp.getText()), weatherCondition.getText());
        Ghat result = copyGhat(ghat, liveOnly ? ghat.name() : name.getText(), liveOnly ? ghat.area() : area.getText(), liveOnly ? ghat.description() : description.getText(),
                liveOnly ? ghat.imageUrl() : imageUrl.getText(), crowd.getValue(), parseInteger(wait.getText()), operational.getValue(), bathing.getValue(), water.getValue(), published.isSelected(), active.isSelected(),
                liveOnly ? ghat.latitude() : parseDouble(latitude.getText()), liveOnly ? ghat.longitude() : parseDouble(longitude.getText()),
                liveOnly ? ghat.history() : new Ghat.History(history.getText(), significance.getText(), simhastha.getText(), ghat.history().associatedSacredPlaces(), ghat.history().rituals(), ghat.history().didYouKnow(), ghat.history().imageUrl()),
                updatedWalking, liveOnly ? ghat.facilities() : split(facilities.getText()), updatedWeather,
                parseOperationalState(bathing.getValue(), water.getValue(), restriction.getText(), priority.getValue(), alert.getText(), gates.getText(), zones.getText(), hazards.getText()));
        saveGhat(result);
    }

    private void saveGhat(Ghat ghat) { saveGhat(ghat, false); }

    private void saveGhat(Ghat ghat, boolean imageUpdate) {
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        ghatService.saveGhat(ghat, token).whenComplete((ignored, error) -> Platform.runLater(() -> {
            if (error != null) {
                showInfo(imageUpdate ? "Image update failed" : "Ghat save failed",
                        imageUpdate ? "The shared backend did not accept this image update. No successful save was assumed."
                                : "The shared backend did not accept this Ghat change. No successful save was assumed.");
                return;
            }
            if (imageUpdate) applySavedGhat(ghat);
            else refreshManagedGhats(managedGhatRows);
        }));
    }

    private void updateGhatImage(Ghat ghat, CloudinaryUploadResult uploaded, Button trigger) {
        AppSession.User user = AppSession.currentUser();
        String token = user == null ? "" : user.idToken();
        java.util.logging.Logger.getLogger(AdminDashboardPage.class.getName())
                .info("Ghat image save requested: ghatId=" + ghat.id()
                        + ", authUidPresent=" + (user != null && user.uid() != null && !user.uid().isBlank())
                        + ", secureUrlPresent=" + (uploaded.getSecureUrl() != null && uploaded.getSecureUrl().startsWith("https://"))
                        + ", publicIdPresent=" + (uploaded.getPublicId() != null && !uploaded.getPublicId().isBlank()));
        String oldPublicId = ghat.imagePublicId();
        ghatService.updateGhatImage(ghat, uploaded.getSecureUrl(), uploaded.getPublicId(), token).whenComplete((result, error) -> Platform.runLater(() -> {
            if (error != null) {
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try { new CloudinaryService().deleteImage(uploaded.getPublicId()); }
                    catch (java.io.IOException cleanupFailure) {
                        java.util.logging.Logger.getLogger(AdminDashboardPage.class.getName())
                                .log(java.util.logging.Level.INFO, "New Ghat Cloudinary image cleanup failed after Firestore rejection: ghatId=" + ghat.id(), cleanupFailure);
                    }
                });
                setGhatImageReady(trigger);
                showInfo("Image update failed", "The shared backend rejected this image update. Check the application log for the Firebase status and response.");
                return;
            }
            java.util.logging.Logger.getLogger(AdminDashboardPage.class.getName())
                    .info("Ghat image save accepted by Firestore: ghatId=" + ghat.id());
            applySavedGhat(result.ghat());
            if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(uploaded.getPublicId())) {
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try { new CloudinaryService().deleteImage(oldPublicId); }
                    catch (java.io.IOException cleanupFailure) {
                        java.util.logging.Logger.getLogger(AdminDashboardPage.class.getName())
                                .log(java.util.logging.Level.INFO, "Old Ghat Cloudinary image cleanup failed after successful save: ghatId=" + ghat.id(), cleanupFailure);
                    }
                });
            }
            setGhatImageReady(trigger);
            showInfo("Image updated successfully", "The shared Ghat record was updated in Firestore.");
        }));
    }

    private void setGhatImageBusy(Button button, String text) {
        if (button == null) return;
        button.setText(text);
        button.setDisable(true);
    }

    private void setGhatImageReady(Button button) {
        if (button == null) return;
        button.setText("Change Image");
        button.setDisable(false);
    }

    /** Keeps the Admin row in sync with the exact model that the backend accepted. */
    private void applySavedGhat(Ghat saved) {
        if (managedGhatRows == null) return;
        String savedId = ghatKey(saved.id());
        String savedName = ghatKey(saved.name());
        managedGhats = java.util.stream.Stream.concat(
                        managedGhats.stream().filter(existing -> {
                            String existingId = ghatKey(existing.id());
                            String existingName = ghatKey(existing.name());
                            return !existingId.equals(savedId) && (savedName.isBlank() || !existingName.equals(savedName));
                        }), java.util.stream.Stream.of(saved))
                .toList();
        renderManagedGhats(managedGhatRows, managedGhats);
    }

    private String ghatKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String adminGhatSummary(Ghat ghat) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (ghat.area() != null && !ghat.area().isBlank()) parts.add(ghat.area());
        if (ghat.crowdLevel() != Ghat.CrowdLevel.UNKNOWN) parts.add("Crowd " + enumTitle(ghat.crowdLevel()));
        if (ghat.operationalStatus() != Ghat.OperationalStatus.INFORMATION_ONLY) parts.add("Status " + enumTitle(ghat.operationalStatus()));
        parts.add("Snan " + enumTitle(ghat.operationalState().bathingStatus()));
        parts.add("Water " + enumTitle(ghat.operationalState().waterSafety()));
        if (ghat.estimatedWaitMinutes() != null && ghat.estimatedWaitMinutes() >= 0) parts.add("Wait " + ghat.waitLabel());
        parts.add("Walk " + enumTitle(ghat.walking().difficulty()));
        if (ghat.walking().approximateSteps() != null && ghat.walking().approximateSteps() >= 0) parts.add("Steps " + ghat.walking().approximateSteps());
        if (ghat.weather().temperatureCelsius() != null) parts.add("Weather " + ghat.weather().temperatureCelsius() + "C");
        parts.add(ghat.published() ? "Published" : "Draft");
        parts.add(ghat.active() ? "Active" : "Inactive");
        return String.join(" | ", parts);
    }

    private String enumTitle(Enum<?> value) {
        if (value == null) return "";
        String text = value.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        StringBuilder title = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isBlank()) continue;
            if (!title.isEmpty()) title.append(' ');
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.toString();
    }

    private Ghat newGhatDraft() {
        return new Ghat("ghat-" + java.util.UUID.randomUUID(), "", "", "", null, null, null, null, "",
                Ghat.OperationalStatus.OPEN, Ghat.CrowdLevel.MODERATE, 15, true, new Ghat.Walking(Ghat.WalkingDifficulty.MODERATE, 32, null, false, false), List.of(),
                new Ghat.Weather(27, "Clear"), Ghat.History.unavailable(), "",
                new GhatOperationalState(GhatOperationalState.BathingStatus.AVAILABLE, GhatOperationalState.WaterSafety.NORMAL,
                        List.of(), List.of(), List.of(), List.of(), List.of(), GhatOperationalState.CleaningStatus.NORMAL, "",
                        GhatOperationalState.PriorityAlert.none(), ""), false, true);
    }

    private Ghat withFlags(Ghat ghat, boolean published, boolean active) {
        return copyGhat(ghat, ghat.name(), ghat.area(), ghat.description(), ghat.imageUrl(), ghat.crowdLevel(), ghat.estimatedWaitMinutes(),
                ghat.operationalStatus(), ghat.operationalState().bathingStatus(), ghat.operationalState().waterSafety(), published, active);
    }

    private Ghat copyGhat(Ghat base, String name, String area, String description, String imageUrl, Ghat.CrowdLevel crowd, Integer wait,
            Ghat.OperationalStatus operational, GhatOperationalState.BathingStatus bathing, GhatOperationalState.WaterSafety water, boolean published, boolean active) {
        return copyGhat(base, name, area, description, imageUrl, crowd, wait, operational, bathing, water, published, active,
                base.latitude(), base.longitude(), base.history(), base.walking(), base.facilities(),
                base.weather(),
                new GhatOperationalState(bathing, water, base.operationalState().hazards(), base.operationalState().zones(), base.operationalState().gates(),
                        base.operationalState().facilities(), base.operationalState().accessWindows(), base.operationalState().cleaningStatus(), base.operationalState().restrictionReason(),
                        base.operationalState().priorityAlert(), base.operationalState().lastUpdated()));
    }

    private Ghat copyGhat(Ghat base, String name, String area, String description, String imageUrl, Ghat.CrowdLevel crowd, Integer wait,
            Ghat.OperationalStatus operational, GhatOperationalState.BathingStatus bathing, GhatOperationalState.WaterSafety water, boolean published, boolean active,
            Double latitude, Double longitude, Ghat.History history, Ghat.Walking walking, List<String> facilities, Ghat.Weather weather, GhatOperationalState state) {
        return new Ghat(base.id(), name, area, description, latitude, longitude, base.entryLatitude(), base.entryLongitude(), imageUrl,
                base.imagePublicId(), operational, crowd, wait, bathing == GhatOperationalState.BathingStatus.AVAILABLE, walking, facilities, weather, history,
                String.valueOf(System.currentTimeMillis()), state, published, active);
    }

    private TextField editorField(String value, String prompt) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPromptText(prompt);
        field.getStyleClass().add("ghat-editor-input");
        return field;
    }
    private <T> ComboBox<T> enumBox(T[] values, T selected) {
        ComboBox<T> box = new ComboBox<>();
        box.getItems().addAll(values);
        box.setValue(selected);
        box.getStyleClass().add("ghat-editor-input");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
    private ComboBox<Ghat.CrowdLevel> ghatCrowdBox(Ghat.CrowdLevel selected) {
        ComboBox<Ghat.CrowdLevel> box = new ComboBox<>();
        box.getItems().addAll(Ghat.CrowdLevel.LOW, Ghat.CrowdLevel.MODERATE, Ghat.CrowdLevel.HIGH, Ghat.CrowdLevel.CRITICAL);
        box.setValue(selected == null || selected == Ghat.CrowdLevel.UNKNOWN ? Ghat.CrowdLevel.MODERATE : selected);
        box.getStyleClass().add("ghat-editor-input");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
    private ComboBox<Ghat.OperationalStatus> ghatOperationalBox(Ghat.OperationalStatus selected) {
        ComboBox<Ghat.OperationalStatus> box = new ComboBox<>();
        box.getItems().addAll(Ghat.OperationalStatus.OPEN, Ghat.OperationalStatus.PARTIALLY_RESTRICTED, Ghat.OperationalStatus.RESTRICTED,
                Ghat.OperationalStatus.TEMPORARILY_CLOSED, Ghat.OperationalStatus.EMERGENCY_CLOSED);
        box.setValue(selected == null || selected == Ghat.OperationalStatus.INFORMATION_ONLY ? Ghat.OperationalStatus.OPEN : selected);
        box.getStyleClass().add("ghat-editor-input");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
    private Node ghatEditorHeader(String title, String detail) {
        Label icon = moduleIcon("Ghats & Snan", "ghat-editor-header-icon");
        VBox copy = new VBox(2, label(title, "ghat-editor-title"), label(detail, "ghat-editor-subtitle"));
        HBox header = new HBox(12, icon, copy);
        header.getStyleClass().add("ghat-editor-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox ghatEditorSection(String title, Node... content) {
        VBox section = new VBox(10, label(title, "ghat-editor-section-title"));
        section.getChildren().addAll(content);
        section.getStyleClass().add("ghat-editor-section");
        return section;
    }

    private GridPane ghatEditorGrid(Node... nodes) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("ghat-editor-grid");
        grid.setHgap(12);
        grid.setVgap(10);
        for (int i = 0; i < nodes.length; i++) {
            grid.add(nodes[i], i % 2, i / 2);
            GridPane.setHgrow(nodes[i], Priority.ALWAYS);
        }
        return grid;
    }

    private Node labeled(String text, Node control) {
        VBox box = new VBox(4, label(text, "ghat-editor-field-label"), control);
        box.getStyleClass().add("ghat-editor-field");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }
    private String numberText(Number number) { return number == null ? "" : String.valueOf(number); }
    private Integer parseInteger(String value) { try { return value == null || value.isBlank() ? null : Integer.valueOf(value.trim()); } catch (NumberFormatException ignored) { return null; } }
    private Double parseDouble(String value) { try { return value == null || value.isBlank() ? null : Double.valueOf(value.trim()); } catch (NumberFormatException ignored) { return null; } }
    private List<String> split(String value) { return java.util.Arrays.stream((value == null ? "" : value).split(",")).map(String::trim).filter(text -> !text.isBlank()).toList(); }
    private String gateText(Ghat ghat) { return ghat.operationalState().gates().stream().map(gate -> gate.name() + "|" + gate.status()).collect(java.util.stream.Collectors.joining(", ")); }
    private String zoneText(Ghat ghat) { return ghat.operationalState().zones().stream().map(zone -> zone.name() + "|" + zone.status()).collect(java.util.stream.Collectors.joining(", ")); }
    private String hazardText(Ghat ghat) { return ghat.operationalState().hazards().stream().map(hazard -> hazard.type() + "|" + hazard.message()).collect(java.util.stream.Collectors.joining(", ")); }
    private GhatOperationalState parseOperationalState(GhatOperationalState.BathingStatus bathing, GhatOperationalState.WaterSafety water, String restriction,
            GhatOperationalState.AlertPriority priority, String alert, String gates, String zones, String hazards) {
        return new GhatOperationalState(bathing, water, parseHazards(hazards), parseZones(zones), parseGates(gates), List.of(), List.of(),
                GhatOperationalState.CleaningStatus.NORMAL, restriction, new GhatOperationalState.PriorityAlert(priority, alert), String.valueOf(System.currentTimeMillis()));
    }
    private List<GhatOperationalState.Gate> parseGates(String text) { return split(text).stream().map(value -> { String[] p = value.split("\\|", 2); return new GhatOperationalState.Gate(p[0].trim(), p[0].trim(), p.length > 1 ? safeEnum(GhatOperationalState.GateStatus.class, p[1], GhatOperationalState.GateStatus.OPEN) : GhatOperationalState.GateStatus.OPEN, ""); }).toList(); }
    private List<GhatOperationalState.Zone> parseZones(String text) { return split(text).stream().map(value -> { String[] p = value.split("\\|", 2); return new GhatOperationalState.Zone(p[0].trim(), p[0].trim(), p.length > 1 ? safeEnum(GhatOperationalState.ZoneStatus.class, p[1], GhatOperationalState.ZoneStatus.OPEN) : GhatOperationalState.ZoneStatus.OPEN, Ghat.CrowdLevel.UNKNOWN, bathingAvailable(p.length > 1 ? p[1] : ""), "", ""); }).toList(); }
    private List<GhatOperationalState.Hazard> parseHazards(String text) { return split(text).stream().map(value -> { String[] p = value.split("\\|", 2); return new GhatOperationalState.Hazard(safeEnum(GhatOperationalState.HazardType.class, p[0], GhatOperationalState.HazardType.OTHER), p.length > 1 ? p[1].trim() : p[0].trim(), GhatOperationalState.AlertPriority.ADVISORY); }).toList(); }
    private boolean bathingAvailable(String status) { return !"CLOSED".equalsIgnoreCase(status) && !"RESTRICTED".equalsIgnoreCase(status); }
    private <T extends Enum<T>> T safeEnum(Class<T> type, String value, T fallback) { try { return Enum.valueOf(type, value.trim().toUpperCase().replace(' ', '_').replace('-', '_')); } catch (Exception ignored) { return fallback; } }

    private VBox stayPage() {
        HBox stats = new HBox(12, metric(String.valueOf(AppDataStore.items("stay").size()), "Total Properties"),
                metric("0", "Available"), metric("0", "Booked"), metric("0", "Pending"));
        VBox form = structuredForm("Property", "Type", "Owner", "Location", "Rooms / Units", "Available", "Price", "Status");
        return pageShell("Stay", "Manage approved accommodation listings.",
                stats,
                infoPanel("Stay Administration", form, actionRow("stay", form, "Add", "Edit", "Disable", "View Bookings")),
                listPanel("Published Stay Listings", "stay", "No stay listings published yet.", form));
    }

    private VBox lostFoundPage() {
        List<AppDataStore.LostFoundCaseRecord> cases = AppDataStore.lostFoundCases();
        HBox stats = new HBox(12, metric(String.valueOf(AppDataStore.adminOverview().lostFoundOpenCases()), "Open Cases"),
                metric(String.valueOf(cases.stream().filter(c -> "found".equalsIgnoreCase(c.status)).count()), "Found"),
                metric(String.valueOf(cases.stream().filter(c -> "high".equalsIgnoreCase(c.priority)
                        || "urgent".equalsIgnoreCase(c.priority)).count()), "High Priority"),
                metric(String.valueOf(cases.size()), "Total Reports"));
        VBox rows = new VBox(10);
        if (cases.isEmpty()) {
            rows.getChildren().add(dataRow("Lost & Found", "No open cases",
                    "Missing ID, name, age/gender, last seen and reporter contact will appear here."));
        } else {
            for (AppDataStore.LostFoundCaseRecord item : cases) {
                rows.getChildren().add(lostCaseRow(item));
            }
        }
        return pageShell("Lost & Found Control Center", "Track priority cases and reporter follow-up.",
                stats,
                searchBar("Missing ID / Name / Contact"),
                tabRow("Missing", "Found", "High Priority", "Today"),
                infoPanel("Case Records", rows));
    }

    private HBox lostCaseRow(AppDataStore.LostFoundCaseRecord item) {
        Button view = smallButton("View Case");
        view.setOnAction(event -> showInfo(valueOr(item.caseId, item.name),
                "Case ID: " + item.caseId
                        + "\nType: " + valueOr("Not available", item.type)
                        + "\nName / Item: " + valueOr("Not available", item.name)
                        + "\nAge/Gender: " + valueOr("-", item.age) + " / " + valueOr("-", item.gender)
                        + "\nClothing: " + valueOr("Not available", item.clothing)
                        + "\nIdentification Marks: " + valueOr("Not available", item.identificationMarks)
                        + "\nLast Seen: " + valueOr("Not available", item.lastSeenLocation)
                        + "\nLast Seen Time: " + valueOr("Not available", item.lastSeenDateTime)
                        + "\nReporter: " + valueOr("Not available", item.reporterName)
                        + "\nRelation: " + valueOr("Not available", item.relation)
                        + "\nContact: " + valueOr("Not available", item.contact)
                        + "\nStatus: " + item.status
                        + "\nPriority: " + item.priority));
        Button searching = smallButton("Update Status");
        searching.setOnAction(event -> updateLostCase(item, "searching", searching));
        Button found = smallButton("Mark Found");
        found.setOnAction(event -> updateLostCase(item, "found", found));
        Button reunited = smallButton("Mark Reunited");
        reunited.setOnAction(event -> updateLostCase(item, "reunited", reunited));
        Button close = smallButton("Close Case");
        close.setOnAction(event -> updateLostCase(item, "closed", close));
        HBox row = new HBox(10, moduleIcon("Lost & Found", "pilgrim-row-icon"),
                new VBox(2, strong(valueOr(item.caseId, item.name)),
                        muted(valueOr("Report", item.type) + " | " + valueOr("No location", item.lastSeenLocation)
                                + " | Reporter: " + valueOr("Unknown", item.reporterName)
                                + " | Contact: " + valueOr("Not available", item.contact)
                                + " | Status: " + item.status + " | Priority: " + item.priority)),
                createSpacer(), view, searching, found, reunited, close);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void updateLostCase(AppDataStore.LostFoundCaseRecord item, String status, Button button) {
        if (!confirm("Update Case", "Set " + item.caseId + " to " + status + "?")) {
            return;
        }
        button.setDisable(true);
        try {
            AppDataStore.updateLostFoundStatus(item.caseId, status);
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Case Update Failed", exception.getMessage());
        } finally {
            button.setDisable(false);
        }
    }

    private VBox schedulePage() {
        DatePicker date = new DatePicker(LocalDate.now());
        Button add = smallButton("+ Add Event"); add.setOnAction(event -> showScheduleEventForm(null, date.getValue()));
        Button alerts = smallButton("Manage Alerts"); alerts.setOnAction(event -> showScheduleAlerts(date.getValue()));
        HBox controls = new HBox(10, add, alerts, muted("Date"), date); controls.setAlignment(Pos.CENTER_LEFT);
        VBox rows = new VBox(9, muted("Loading official schedule..."));
        Runnable reload = () -> loadAdminScheduleRows(rows, date.getValue()); date.setOnAction(event -> reload.run()); reload.run();
        return pageShell("All Day Schedule Management", "Manage official Simhastha events, timings and schedule alerts", controls, infoPanel("Official Events", rows));
    }

    private Node announcementsPage() {
        return new AdminAnnouncementView(page -> root.setCenter(scroll(page))).managementPage();
    }
    private void loadAdminScheduleRows(VBox rows, LocalDate date) {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> { try { return scheduleService.eventsForDate(date); } catch (Exception exception) { throw new java.util.concurrent.CompletionException(exception); } })
                .whenComplete((events, error) -> Platform.runLater(() -> {
                    if (error != null) { rows.getChildren().setAll(muted("Schedule could not be refreshed.")); return; }
                    if (events.isEmpty()) { rows.getChildren().setAll(muted("No events scheduled for this date.")); return; }
                    rows.getChildren().setAll(events.stream().map(this::adminScheduleRow).toList());
                }));
    }

    private HBox adminScheduleRow(ScheduleEvent item) {
        Button edit = smallButton("Edit"); edit.setOnAction(event -> showScheduleEventForm(item, item.date()));
        Button reschedule = smallButton("Reschedule"); reschedule.setOnAction(event -> showScheduleEventForm(item, item.date()));
        Button cancel = smallButton(item.cancelled() ? "Restore" : "Cancel"); cancel.setOnAction(event -> saveScheduleEvent(new ScheduleEvent(item.id(), item.title(), item.category(), item.location(), item.date(), item.startTime(), item.endTime(), item.description(), item.organizer(), item.important(), item.note(), !item.cancelled(), item.latitude(), item.longitude(), item.locationId())));
        Button delete = smallButton("Delete"); delete.setOnAction(event -> { if (confirm("Delete Event", "Delete this event permanently? This removes it from user schedules.")) deleteScheduleEvent(item.id()); });
        VBox text = new VBox(2, strong(formatAdminTime(item) + "  " + item.title()), muted(item.category().label() + " | " + item.location() + " | " + (item.cancelled() ? "CANCELLED" : "SCHEDULED") + (item.important() ? " | Important" : "")));
        HBox row = new HBox(10, text, createSpacer(), edit, reschedule, cancel, delete); row.getStyleClass().add("pilgrim-data-row"); row.setAlignment(Pos.CENTER_LEFT); return row;
    }

    private void showScheduleEventForm(ScheduleEvent existing, LocalDate defaultDate) {
        TextField title = AppUi.textField("Event title"); TextField location = AppUi.textField("Location"); TextField start = AppUi.textField("HH:mm"); TextField end = AppUi.textField("HH:mm"); TextField organizer = AppUi.textField("Organizer"); TextField description = AppUi.textField("Description"); TextField note = AppUi.textField("Important note");
        ComboBox<ScheduleCategory> category = new ComboBox<>(); category.getItems().setAll(ScheduleCategory.values()); category.setPromptText("Category"); DatePicker date = new DatePicker(defaultDate); CheckBox important = new CheckBox("Important");
        if (existing != null) { title.setText(existing.title()); location.setText(existing.location()); start.setText(existing.startTime().toString()); end.setText(existing.endTime().toString()); organizer.setText(existing.organizer()); description.setText(existing.description()); note.setText(existing.note()); category.setValue(existing.category()); date.setValue(existing.date()); important.setSelected(existing.important()); }
        VBox form = new VBox(9, title, category, date, start, end, label("Location", "pilgrim-small-gold"), location, organizer, description, note, important); form.setPadding(new Insets(14));
        Alert dialog = new Alert(Alert.AlertType.NONE); dialog.setTitle(existing == null ? "Add Event" : "Edit Event"); dialog.getDialogPane().setContent(form); ButtonType save = new ButtonType(existing == null ? "Save Event" : "Save Changes"); dialog.getButtonTypes().addAll(ButtonType.CANCEL, save); AppUi.styleDialog(dialog, stage, "schedule-admin-dialog", save);
        dialog.showAndWait().ifPresent(result -> { if (result != save) return; try { saveScheduleEvent(new ScheduleEvent(existing == null ? "" : existing.id(), title.getText().trim(), category.getValue(), location.getText().trim(), date.getValue(), LocalTime.parse(start.getText().trim()), LocalTime.parse(end.getText().trim()), description.getText().trim(), organizer.getText().trim(), important.isSelected(), note.getText().trim(), existing != null && existing.cancelled(), existing == null ? null : existing.latitude(), existing == null ? null : existing.longitude(), existing == null ? "" : existing.locationId())); } catch (Exception exception) { showInfo("Event not saved", exception.getMessage()); } });
    }

    private void saveScheduleEvent(ScheduleEvent event) { java.util.concurrent.CompletableFuture.runAsync(() -> { try { scheduleService.saveEvent(event); } catch (Exception exception) { throw new java.util.concurrent.CompletionException(exception); } }).whenComplete((ignored, error) -> Platform.runLater(() -> { if (error != null) showInfo("Event not saved", error.getCause().getMessage()); else showSection("Schedule & Events"); })); }
    private void deleteScheduleEvent(String id) { java.util.concurrent.CompletableFuture.runAsync(() -> { try { scheduleService.deleteEvent(id); } catch (Exception exception) { throw new java.util.concurrent.CompletionException(exception); } }).whenComplete((ignored, error) -> Platform.runLater(() -> { if (error != null) showInfo("Delete failed", error.getCause().getMessage()); else showSection("Schedule & Events"); })); }
    private String formatAdminTime(ScheduleEvent event) { return event.date() + " | " + event.startTime() + "–" + event.endTime(); }
    private void showScheduleAlertForm(ScheduleAlert existing, LocalDate defaultDate) {
        TextField title = AppUi.textField("Alert title"); TextField message = AppUi.textField("Message"); TextField location = AppUi.textField("Location"); TextField start = AppUi.textField("HH:mm (optional)"); TextField end = AppUi.textField("HH:mm (optional)");
        DatePicker date = new DatePicker(defaultDate); ComboBox<String> severity = new ComboBox<>(); severity.getItems().addAll("INFO", "WARNING", "CRITICAL"); severity.setValue("WARNING"); CheckBox active = new CheckBox("Active"); active.setSelected(true);
        if (existing != null) { title.setText(existing.title()); message.setText(existing.message()); location.setText(existing.location()); date.setValue(existing.date()); start.setText(existing.startTime() == null ? "" : existing.startTime().toString()); end.setText(existing.endTime() == null ? "" : existing.endTime().toString()); severity.setValue(existing.severity()); active.setSelected(existing.active()); }
        VBox form = new VBox(9, title, message, location, date, start, end, severity, active); form.setPadding(new Insets(14));
        Alert dialog = new Alert(Alert.AlertType.NONE); dialog.setTitle(existing == null ? "Add Schedule Alert" : "Edit Schedule Alert"); dialog.getDialogPane().setContent(form); ButtonType save = new ButtonType(existing == null ? "Save Alert" : "Save Changes"); dialog.getButtonTypes().addAll(ButtonType.CANCEL, save); AppUi.styleDialog(dialog, stage, "schedule-admin-dialog", save);
        dialog.showAndWait().ifPresent(result -> { if (result != save) return; try { LocalTime from = start.getText().isBlank() ? null : LocalTime.parse(start.getText().trim()); LocalTime to = end.getText().isBlank() ? null : LocalTime.parse(end.getText().trim()); if ((from == null) != (to == null) || (from != null && !to.isAfter(from))) throw new IllegalArgumentException("Alert end time must be after start time."); saveScheduleAlert(new ScheduleAlert(existing == null ? "" : existing.id(), title.getText().trim(), message.getText().trim(), location.getText().trim(), date.getValue(), from, to, severity.getValue(), active.isSelected())); } catch (Exception exception) { showInfo("Alert not saved", exception.getMessage()); } });
    }

    private void showScheduleAlerts(LocalDate date) {
        VBox rows = new VBox(9, muted("Loading alerts...")); Button add = smallButton("+ Add Alert"); add.setOnAction(event -> showScheduleAlertForm(null, date));
        Alert dialog = new Alert(Alert.AlertType.NONE); dialog.setTitle("Manage Schedule Alerts"); dialog.getDialogPane().setContent(new VBox(12, add, rows)); dialog.getButtonTypes().add(ButtonType.CLOSE); AppUi.styleDialog(dialog, stage, "schedule-admin-dialog", ButtonType.CLOSE);
        java.util.concurrent.CompletableFuture.supplyAsync(() -> { try { return scheduleService.alertsForDate(date); } catch (Exception exception) { throw new java.util.concurrent.CompletionException(exception); } }).whenComplete((alerts, error) -> Platform.runLater(() -> { if (error != null) rows.getChildren().setAll(muted("Alerts could not be refreshed.")); else if (alerts.isEmpty()) rows.getChildren().setAll(muted("No alerts for this date.")); else rows.getChildren().setAll(alerts.stream().map(this::adminAlertRow).toList()); })); dialog.showAndWait();
    }

    private HBox adminAlertRow(ScheduleAlert alert) {
        Button edit = smallButton("Edit"); edit.setOnAction(event -> showScheduleAlertForm(alert, alert.date()));
        Button active = smallButton(alert.active() ? "Deactivate" : "Activate"); active.setOnAction(event -> saveScheduleAlert(new ScheduleAlert(alert.id(), alert.title(), alert.message(), alert.location(), alert.date(), alert.startTime(), alert.endTime(), alert.severity(), !alert.active())));
        Button delete = smallButton("Delete"); delete.setOnAction(event -> { if (confirm("Delete Alert", "Delete this alert permanently?")) deleteScheduleAlert(alert.id()); });
        HBox row = new HBox(10, new VBox(2, strong(alert.title()), muted(alert.severity() + " | " + alert.location() + " | " + (alert.active() ? "Active" : "Inactive"))), createSpacer(), edit, active, delete); row.getStyleClass().add("pilgrim-data-row"); row.setAlignment(Pos.CENTER_LEFT); return row;
    }

    private void saveScheduleAlert(ScheduleAlert alert) { java.util.concurrent.CompletableFuture.runAsync(() -> { try { scheduleService.saveAlert(alert); } catch (Exception exception) { throw new java.util.concurrent.CompletionException(exception); } }).whenComplete((ignored, error) -> Platform.runLater(() -> { if (error != null) showInfo("Alert not saved", error.getCause().getMessage()); else showSection("Schedule & Events"); })); }
    private void deleteScheduleAlert(String id) { java.util.concurrent.CompletableFuture.runAsync(() -> { try { scheduleService.deleteAlert(id); } catch (Exception exception) { throw new java.util.concurrent.CompletionException(exception); } }).whenComplete((ignored, error) -> Platform.runLater(() -> { if (error != null) showInfo("Delete failed", error.getCause().getMessage()); else showSection("Schedule & Events"); })); }

    private VBox faqManagementPage() {
        List<AppDataStore.FaqRecord> faqs = AppDataStore.faqs();
        HBox stats = new HBox(12,
                metric(String.valueOf(faqs.size()), "Total FAQs"),
                metric(String.valueOf(faqs.stream().filter(faq -> faq.active).count()), "Published"),
                metric(String.valueOf(faqs.stream().map(faq -> faq.category).distinct().count()), "Categories"));
        TextField search = AppUi.textField("Search question / category");
        ComboBox<String> category = combo("All", "General", "Simhastha 2027", "Ghats & Snan", "Travel & Transport",
                "Stay & Accommodation", "Puja & Rituals", "Emergency & Safety", "Bookings & Payments");
        ComboBox<String> status = combo("All", "Published", "Draft");
        VBox rows = new VBox(9);
        Runnable render = () -> renderAdminFaqRows(rows, search.getText(), category.getValue(), status.getValue());
        search.textProperty().addListener((observable, oldValue, newValue) -> render.run());
        category.setOnAction(event -> render.run());
        status.setOnAction(event -> render.run());
        Button add = new Button("+ Add FAQ");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> showFaqEditor(null));
        HBox filters = new HBox(10, search, category, status, add);
        filters.getStyleClass().addAll("pilgrim-filter-row", "admin-faq-toolbar");
        HBox.setHgrow(search, Priority.ALWAYS);
        render.run();
        return pageShell("FAQs / Help Center", "Manage user-facing help questions and support answers.",
                stats, filters, infoPanel("FAQ Library", rows));
    }

    private void renderAdminFaqRows(VBox rows, String query, String category, String status) {
        rows.getChildren().clear();
        List<AppDataStore.FaqRecord> matches = AppDataStore.faqs().stream()
                .filter(faq -> matches(query, faq.category, faq.question, faq.answer))
                .filter(faq -> "All".equals(valueOr("All", category)) || category.equals(faq.category))
                .filter(faq -> "All".equals(valueOr("All", status))
                        || ("Published".equals(status) && faq.active)
                        || ("Draft".equals(status) && !faq.active))
                .sorted(java.util.Comparator.comparing(faq -> faq.sortOrder))
                .toList();
        if (matches.isEmpty()) {
            rows.getChildren().add(dataRow("FAQs / Help Center", "No matching FAQs", "Add or adjust filters."));
            return;
        }
        matches.forEach(faq -> rows.getChildren().add(adminFaqRow(faq)));
    }

    private HBox adminFaqRow(AppDataStore.FaqRecord faq) {
        VBox copy = new VBox(4, strong(faq.question), muted(faq.category + " | " + (faq.active ? "Published" : "Draft")),
                muted(faq.answer));
        Button details = smallButton("Details");
        details.setOnAction(event -> showInfo(faq.question, faq.answer));
        Button edit = smallButton("Edit");
        edit.setOnAction(event -> showFaqEditor(faq));
        Button publish = smallButton(faq.active ? "Hide" : "Publish");
        publish.setOnAction(event -> {
            AppDataStore.saveFaq(faq.withActive(!faq.active));
            showSection("FAQs / Help Center");
        });
        Button delete = smallButton("Delete");
        delete.setOnAction(event -> {
            if (confirm("Delete FAQ", "Delete this FAQ from the Help Center?")) {
                AppDataStore.deleteFaq(faq.id);
                showSection("FAQs / Help Center");
            }
        });
        HBox row = new HBox(10, moduleIcon("FAQs / Help Center", "pilgrim-row-icon"), copy, createSpacer(),
                details, edit, publish, delete);
        HBox.setHgrow(copy, Priority.ALWAYS);
        row.getStyleClass().addAll("pilgrim-data-row", "admin-faq-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showFaqEditor(AppDataStore.FaqRecord existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add FAQ" : "Edit FAQ");
        ButtonType save = new ButtonType(existing == null ? "Add FAQ" : "Save Changes");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        TextField question = AppUi.textField("Question");
        ComboBox<String> category = combo("General", "Simhastha 2027", "Ghats & Snan", "Travel & Transport",
                "Stay & Accommodation", "Puja & Rituals", "Emergency & Safety", "Bookings & Payments");
        TextArea answer = new TextArea();
        answer.setPromptText("Answer");
        answer.setWrapText(true);
        answer.getStyleClass().add("input-field");
        TextField order = AppUi.textField("Display order");
        CheckBox active = new CheckBox("Publish this FAQ");
        active.setSelected(true);
        if (existing != null) {
            question.setText(existing.question);
            category.setValue(existing.category);
            answer.setText(existing.answer);
            order.setText(existing.sortOrder);
            active.setSelected(existing.active);
        }
        VBox form = new VBox(10, sectionTitle(existing == null ? "Add FAQ" : "Edit FAQ"), question, category,
                answer, order, active);
        form.getStyleClass().add("admin-faq-editor");
        dialog.getDialogPane().setContent(form);
        AppUi.styleDialog(dialog, stage, "profile-dialog-pane", save);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == save) {
            if (question.getText().trim().isBlank() || answer.getText().trim().isBlank()) {
                showInfo("FAQ", "Please enter both question and answer.");
                return;
            }
            AppDataStore.saveFaq(new AppDataStore.FaqRecord(existing == null ? "" : existing.id,
                    category.getValue(), question.getText().trim(), answer.getText().trim(),
                    active.isSelected(), order.getText().trim()));
            showSection("FAQs / Help Center");
        }
    }

    private VBox emergencyPage() {
        long active = emergencyReports.stream().filter(EmergencyReport::unresolved).count();
        long critical = emergencyReports.stream().filter(report -> report.unresolved() && report.priority() == EmergencyReport.Priority.CRITICAL).count();
        long medical = emergencyReports.stream().filter(report -> report.unresolved() && report.emergencyType() == EmergencyReport.EmergencyType.MEDICAL).count();
        long police = emergencyReports.stream().filter(report -> report.unresolved() && report.emergencyType() == EmergencyReport.EmergencyType.POLICE_SECURITY).count();
        long fireCrowd = emergencyReports.stream().filter(report -> report.unresolved() && (report.emergencyType() == EmergencyReport.EmergencyType.FIRE || report.emergencyType() == EmergencyReport.EmergencyType.CROWD_RISK)).count();
        long resolved = emergencyReports.stream().filter(report -> report.status() == EmergencyReport.Status.RESOLVED).count();
        HBox summary = new HBox(10, metric(String.valueOf(active), "Active"), metric(String.valueOf(critical), "Critical"),
                metric(String.valueOf(medical), "Medical"), metric(String.valueOf(police), "Police/Security"),
                metric(String.valueOf(fireCrowd), "Fire/Crowd"), metric(String.valueOf(resolved), "Resolved"));
        VBox queue = new VBox(8);
        if (emergencyReportsLoading) queue.getChildren().add(label("Loading emergency reports...", "description-text"));
        List<EmergencyReport> ordered = emergencyReports.stream().sorted(java.util.Comparator
                .comparingInt((EmergencyReport report) -> report.priority().ordinal())
                .thenComparing(EmergencyReport::createdAt).reversed()).toList();
        if (!emergencyReportsLoading && !emergencyReportsError.isBlank()) queue.getChildren().add(label(emergencyReportsError, "description-text"));
        else if (!emergencyReportsLoading && ordered.isEmpty()) queue.getChildren().add(label("No emergency reports available.", "description-text"));
        ordered.forEach(report -> queue.getChildren().add(emergencyQueueRow(report)));
        VBox serviceRows = new VBox(7);
        if (emergencyServicesLoading) serviceRows.getChildren().add(label("Loading emergency services...", "description-text"));
        else if (!emergencyServicesError.isBlank() && !emergencyServicesPreviewActive) serviceRows.getChildren().add(label("Emergency service management is currently unavailable due to Firestore permissions.", "description-text"));
        else if (emergencyServices.isEmpty()) serviceRows.getChildren().add(label("No emergency services available.", "description-text"));
        TextField search = new TextField(); search.setPromptText("Search service name");
        ComboBox<String> filter = combo(java.util.stream.Stream.concat(java.util.stream.Stream.of("All"), java.util.Arrays.stream(EmergencyService.Category.values()).map(Enum::name)).toArray(String[]::new));
        Runnable renderServices = () -> {
            serviceRows.getChildren().clear();
            if (emergencyServicesPreviewActive) {
                serviceRows.getChildren().add(label(emergencyServicesPreviewStatus(), "description-text"));
            }
            emergencyServices.stream()
                    .filter(service -> service.name().toLowerCase().contains(search.getText().toLowerCase())
                            && ("All".equals(filter.getValue()) || service.category().name().equals(filter.getValue())))
                    .forEach(service -> serviceRows.getChildren().add(emergencyServiceRow(service)));
            if (!emergencyServicesPreviewMessage.isBlank()) {
                serviceRows.getChildren().add(label(emergencyServicesPreviewMessage, "description-text"));
            }
        };
        search.textProperty().addListener((o, a, b) -> renderServices.run()); filter.setOnAction(event -> renderServices.run());
        if (!emergencyServicesLoading && (emergencyServicesError.isBlank() || emergencyServicesPreviewActive)) renderServices.run();
        Button refreshServices = new Button("REFRESH SERVICES"); refreshServices.getStyleClass().add("secondary-button"); refreshServices.setOnAction(event -> loadEmergencyServicesAsync());
        Button addService = new Button("ADD SERVICE"); addService.getStyleClass().add("primary-button"); addService.setOnAction(event -> { editingEmergencyService = null; root.setCenter(scroll(emergencyServiceForm())); });
        return pageShell("Emergency Management", "Live Firestore queue. Critical cases are prioritised first.", summary,
                infoPanel("Emergency Response Queue", queue), infoPanel("Emergency Services", new VBox(8, new HBox(8, search, filter, addService, refreshServices), serviceRows)));
    }

    private HBox emergencyServiceRow(EmergencyService service) {
        Label copy = label(adminEmergencyServiceValue(service.name(), "Unnamed emergency service") + " | "
                + service.category() + " | " + adminEmergencyServiceValue(service.sector(), "Not available")
                + " / " + adminEmergencyServiceValue(service.area(), "Not available") + " | "
                + service.operationalStatus() + " | " + adminEmergencyServiceValue(service.contactNumber(), "Not available")
                + " | " + (service.active() ? "Active" : "Inactive"), "admin-row-detail");
        Button edit = new Button("EDIT"); edit.setOnAction(e -> { editingEmergencyService = service; root.setCenter(scroll(emergencyServiceForm())); });
        Button active = new Button(service.active() ? "DEACTIVATE" : "ACTIVATE"); active.setOnAction(e -> saveEmergencyService(copyActive(service, !service.active()), active));
        Button delete = new Button("DELETE"); delete.setOnAction(e -> deleteEmergencyService(service, delete));
        return new HBox(8, copy, createSpacer(), edit, active, delete);
    }

    private String adminEmergencyServiceValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private VBox emergencyQueueRow(EmergencyReport report) {
        Label title = label(report.trackingId() + "  |  " + report.emergencyType().name().replace('_', ' '), "admin-row-title");
        Label detail = label("Status: " + report.status().name().replace('_', ' ') + " | Priority: " + report.priority()
                + " | Reporter: " + valueOr("Not available", report.reporterName()) + " | Contact: " + valueOr("Not available", report.reporterPhone())
                + " | People: " + report.peopleAffected() + " | " + valueOr("Demo Nashik Location", report.locationLabel())
                + " | Landmark: " + valueOr("Not available", report.landmark()) + " | Coordinates: " + report.latitude() + ", " + report.longitude()
                + " | Description: " + valueOr("Not available", report.description()), "admin-row-detail");
        ComboBox<String> team = combo("Unassigned", "Medical Team", "Police Team", "Fire/Safety Team", "Crowd Control Team", "Volunteer Help Team");
        if (!report.assignedTeamName().isBlank()) team.setValue(report.assignedTeamName());
        ComboBox<String> priority = combo(report.priority().name(), "CRITICAL", "HIGH", "MEDIUM");
        ComboBox<String> status = combo(report.status().name(), "REPORTED", "ACKNOWLEDGED", "TEAM_DISPATCHED", "HELP_ARRIVING", "RESOLVED");
        TextField notes = new TextField(report.adminNotes()); notes.setPromptText("Internal admin notes");
        Button save = new Button("UPDATE"); save.getStyleClass().add("primary-button");
        save.setOnAction(event -> updateEmergencyFromAdmin(report, priority.getValue(), status.getValue(), team.getValue(), notes.getText(), save));
        HBox controls = new HBox(8, priority, status, team, notes, save);
        controls.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(notes, Priority.ALWAYS);
        VBox row = new VBox(6, title, detail, controls); row.getStyleClass().add("admin-data-row");
        if (report.emergencyId().equals(focusedEmergencyReportId)) {
            row.getStyleClass().add("admin-emergency-sos-selected");
            focusedEmergencyReportId = "";
        }
        return row;
    }

    /** Starts after a valid admin session exists. The first successful read establishes a no-popup baseline. */
    private void startAdminSosMonitor() {
        AppSession.User user = AppSession.currentUser();
        if (sosMonitorTask != null && !sosMonitorTask.isCancelled()) return;
        if (!isAdminSession() || !emergencyFirestore.isEnabled() || user == null) {
            System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=SKIP reason=START_PREREQUISITE");
            return;
        }
        sosMonitorExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "admin-sos-monitor");
            thread.setDaemon(true);
            return thread;
        });
        System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=START adminUid=" + user.uid());
        sosMonitorTask = sosMonitorExecutor.scheduleWithFixedDelay(this::pollAdminSosReports, 0, 5, TimeUnit.SECONDS);
    }

    /** Stops the admin-session monitor; called on logout and available for any future page disposal path. */
    public void dispose() {
        if (sosMonitorTask != null) {
            sosMonitorTask.cancel(true);
            sosMonitorTask = null;
        }
        if (sosMonitorExecutor != null) {
            sosMonitorExecutor.shutdownNow();
            sosMonitorExecutor = null;
        }
        sosMonitorPollInProgress = false;
        sosMonitorBaselined = false;
        sosPopupVisible = false;
        sosMonitorFailureLogged = false;
        pendingSosReports.clear();
        notifiedEmergencyIds.clear();
    }

    private void pollAdminSosReports() {
        try {
            System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=POLL");
            if (sosMonitorPollInProgress) {
                System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=SKIP reason=POLL_IN_PROGRESS");
                return;
            }
            if (!isAdminSession() || !emergencyFirestore.isEnabled()) {
                System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=SKIP reason=SESSION_OR_FIRESTORE");
                return;
            }
            AppSession.User user = AppSession.currentUser();
            if (user == null) {
                System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=SKIP reason=NO_ADMIN_SESSION");
                return;
            }
            sosMonitorPollInProgress = true;
            List<EmergencyReport> records = emergencyFirestore.loadEmergencyReports(user.idToken());
            System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=LOAD count=" + records.size());
            Platform.runLater(() -> processAdminSosPoll(records));
        } catch (Exception exception) {
            if (!sosMonitorFailureLogged) {
                sosMonitorFailureLogged = true;
                System.err.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=POLL result=failed error=" + exception.getMessage());
            }
            sosMonitorPollInProgress = false;
        }
    }

    /** Runs on the JavaFX thread so the report cache, seen IDs, and popup queue stay consistent. */
    private void processAdminSosPoll(List<EmergencyReport> records) {
        try {
            if (!isAdminSession()) {
                System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=SKIP reason=SESSION_ENDED");
                return;
            }
            emergencyReports = records == null ? List.of() : records;
            sosMonitorFailureLogged = false;
            if (!sosMonitorBaselined) {
                int baselineCount = 0;
                for (EmergencyReport report : emergencyReports) {
                    if (isNewCriticalSosCandidate(report)) {
                        notifiedEmergencyIds.add(report.emergencyId());
                        baselineCount++;
                    } else {
                        logSosSkip(report, sosSkipReason(report));
                    }
                }
                sosMonitorBaselined = true;
                System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=BASELINE count=" + baselineCount);
                return;
            }

            for (EmergencyReport report : emergencyReports) {
                if (!isNewCriticalSosCandidate(report)) {
                    logSosSkip(report, sosSkipReason(report));
                } else if (!notifiedEmergencyIds.add(report.emergencyId())) {
                    logSosSkip(report, "ALREADY_SEEN");
                } else {
                    logSosCandidate(report);
                    queueSosPopup(report);
                }
            }
            showNextSosPopup();
        } catch (RuntimeException exception) {
            System.err.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=POLL result=ui_failed error=" + exception.getMessage());
        } finally {
            // Never let a UI-side failure permanently suppress later five-second polls.
            sosMonitorPollInProgress = false;
        }
    }

    private void queueSosPopup(EmergencyReport report) {
        pendingSosReports.addLast(report);
        System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=QUEUE emergencyId=" + report.emergencyId());
    }

    private void logSosCandidate(EmergencyReport report) {
        System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=CANDIDATE emergencyId=" + report.emergencyId()
                + " source=" + report.source() + " status=" + report.status() + " priority=" + report.priority()
                + " emergencyType=" + report.emergencyType() + " trackingId=" + report.trackingId()
                + " createdAt=" + report.createdAt() + " userId=" + report.userId());
    }

    private void logSosSkip(EmergencyReport report, String reason) {
        if (report == null) {
            System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=SKIP reason=NULL_REPORT");
            return;
        }
        System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=SKIP reason=" + reason
                + " emergencyId=" + report.emergencyId() + " source=" + report.source()
                + " status=" + report.status() + " priority=" + report.priority());
    }

    private String sosSkipReason(EmergencyReport report) {
        if (report == null) return "NULL_REPORT";
        if (report.source() != EmergencyReport.Source.SOS) return "SOURCE";
        if (report.status() != EmergencyReport.Status.REPORTED) return "STATUS";
        if (report.priority() != EmergencyReport.Priority.CRITICAL) return "PRIORITY";
        if (report.emergencyId().isBlank()) return "MISSING_ID";
        return "UNKNOWN";
    }

    private boolean isNewCriticalSosCandidate(EmergencyReport report) {
        return report != null
                && report.source() == EmergencyReport.Source.SOS
                && report.status() == EmergencyReport.Status.REPORTED
                && report.priority() == EmergencyReport.Priority.CRITICAL
                && !report.emergencyId().isBlank();
    }

    private void showNextSosPopup() {
        if (sosPopupVisible || pendingSosReports.isEmpty()) return;
        Stage owner = activeAdminStage();
        if (owner == null || !isAdminSession()) {
            System.err.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=POPUP emergencyId="
                    + pendingSosReports.peekFirst().emergencyId() + " result=failed reason=STALE_OWNER");
            return;
        }
        EmergencyReport report = pendingSosReports.removeFirst();
        sosPopupVisible = true;
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(owner);
            dialog.setTitle("Emergency SOS Received");
            dialog.getDialogPane().getStyleClass().add("admin-sos-popup");
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());

            Label heading = label("EMERGENCY SOS RECEIVED", "admin-sos-popup-title");
            Label tracking = label("Tracking ID: " + valueOr("Not available", report.trackingId()), "admin-sos-popup-tracking");
            Label details = label("Type: " + report.emergencyType().name().replace('_', ' ')
                    + "\nPriority: " + report.priority()
                    + "\nLocation: " + valueOr("Not available", report.locationLabel())
                    + "\nReporter: " + valueOr("Not available", report.reporterName())
                    + "\nContact: " + valueOr("Not available", report.reporterPhone())
                    + "\nTime: " + valueOr("Not available", report.createdAt()), "admin-sos-popup-detail");
            Label error = label("", "admin-sos-popup-error");
            error.setWrapText(true);
            VBox content = new VBox(8, heading, tracking, details, error);
            content.getStyleClass().add("admin-sos-popup-content");
            dialog.getDialogPane().setContent(content);

            ButtonType acknowledge = new ButtonType("ACKNOWLEDGE", ButtonBar.ButtonData.OK_DONE);
            ButtonType view = new ButtonType("VIEW EMERGENCY", ButtonBar.ButtonData.OTHER);
            ButtonType dismiss = new ButtonType("DISMISS", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(acknowledge, view, dismiss);
            Button acknowledgeButton = (Button) dialog.getDialogPane().lookupButton(acknowledge);
            Button viewButton = (Button) dialog.getDialogPane().lookupButton(view);
            acknowledgeButton.getStyleClass().add("admin-sos-popup-acknowledge");
            viewButton.getStyleClass().add("admin-sos-popup-view");
            acknowledgeButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                acknowledgeSosFromPopup(report, dialog, acknowledgeButton, error);
            });
            viewButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                focusedEmergencyReportId = report.emergencyId();
                dialog.close();
                showSection("Emergency");
            });
            dialog.setOnHidden(event -> {
                sosPopupVisible = false;
                Platform.runLater(this::showNextSosPopup);
            });
            dialog.show();
            System.out.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=POPUP emergencyId=" + report.emergencyId() + " result=success");
        } catch (RuntimeException exception) {
            sosPopupVisible = false;
            notifiedEmergencyIds.remove(report.emergencyId());
            System.err.println("ADMIN_SOS_MONITOR_DIAGNOSTIC action=POPUP emergencyId=" + report.emergencyId()
                    + " result=failed error=" + exception.getMessage());
        }
    }

    /** Returns the currently visible window hosting this dashboard, not a stale navigation stage. */
    private Stage activeAdminStage() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage candidate && candidate.isShowing() && candidate.getScene() != null
                    && candidate.getScene().getRoot() == root) {
                return candidate;
            }
        }
        return stage != null && stage.isShowing() && stage.getScene() != null && stage.getScene().getRoot() == root
                ? stage : null;
    }

    private void acknowledgeSosFromPopup(EmergencyReport report, Dialog<ButtonType> dialog, Button acknowledgeButton, Label error) {
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            error.setText("Admin session is no longer available. Please sign in again.");
            return;
        }
        acknowledgeButton.setDisable(true);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        EmergencyReport updated = new EmergencyReport(report.emergencyId(), report.trackingId(), report.userId(),
                report.reporterName(), report.reporterPhone(), report.emergencyType(), report.description(),
                report.peopleAffected(), report.landmark(), report.latitude(), report.longitude(), report.locationLabel(),
                report.priority(), EmergencyReport.Status.ACKNOWLEDGED, report.assignedTeamId(), report.assignedTeamName(),
                report.assignedTeamType(), report.adminNotes(), EmergencyReport.userStatusText(EmergencyReport.Status.ACKNOWLEDGED),
                report.createdAt(), stamp, stamp, report.dispatchedAt(), report.helpArrivingAt(), report.resolvedAt(), report.source());
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emergencyFirestore.updateEmergencyReport(updated, user.idToken());
                Platform.runLater(() -> {
                    emergencyReports = emergencyReports.stream()
                            .map(item -> item.emergencyId().equals(updated.emergencyId()) ? updated : item).toList();
                    dialog.close();
                    if ("Emergency".equals(selectedSection)) root.setCenter(scroll(emergencyPage()));
                });
            } catch (Exception exception) {
                System.err.println("EMERGENCY_SOS_MONITOR_DIAGNOSTIC action=ACKNOWLEDGE emergencyId="
                        + report.emergencyId() + " result=failed error=" + exception.getMessage());
                Platform.runLater(() -> {
                    acknowledgeButton.setDisable(false);
                    error.setText("Could not acknowledge this SOS. It remains reported; please retry.");
                });
            }
        });
    }

    private void loadEmergencyReportsAsync() {
        if (emergencyReportsLoading || !emergencyFirestore.isEnabled()) return;
        AppSession.User user = AppSession.currentUser(); if (user == null) return;
        emergencyReportsLoading = true;
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try { return emergencyFirestore.loadEmergencyReports(user.idToken()); }
            catch (Exception exception) { System.err.println("EMERGENCY_ADMIN_UPDATE_DIAGNOSTIC action=LOAD result=failed error=" + exception.getMessage()); return null; }
        }).thenAccept(records -> Platform.runLater(() -> {
            emergencyReportsLoading = false;
            if (records == null) emergencyReportsError = "Emergency reports cannot be loaded (Firestore permission denied). Deploy the scoped emergency_reports rules, then reopen this page.";
            else { emergencyReports = records; emergencyReportsError = ""; }
            if ("Emergency".equals(selectedSection)) root.setCenter(scroll(emergencyPage()));
        }));
    }

    private void loadEmergencyServicesAsync() {
        if (emergencyServicesLoading || !emergencyFirestore.isEnabled()) return;
        AppSession.User user = AppSession.currentUser(); if (user == null) return;
        emergencyServicesLoading = true;
        java.util.concurrent.CompletableFuture.supplyAsync(() -> { try { return emergencyFirestore.loadEmergencyServices(user.idToken()); }
            catch (Exception exception) { System.err.println("EMERGENCY_SERVICE_READ_DIAGNOSTIC action=ADMIN_LOAD result=failed error=" + exception.getMessage()); return null; } })
            .thenAccept(records -> Platform.runLater(() -> {
                emergencyServicesLoading = false;
                emergencyServicesPreviewMessage = "";
                if (records == null) {
                    emergencyServicesError = "permission";
                    emergencyServicesPreviewActive = true;
                    emergencyServicesFirestoreAvailable = false;
                    emergencyServices = EmergencyDevelopmentServices.create();
                } else if (records.isEmpty()) {
                    emergencyServicesError = "";
                    emergencyServicesPreviewActive = true;
                    emergencyServicesFirestoreAvailable = true;
                    emergencyServices = EmergencyDevelopmentServices.create();
                } else {
                    emergencyServices = records;
                    emergencyServicesError = "";
                    emergencyServicesPreviewActive = false;
                    emergencyServicesFirestoreAvailable = true;
                }
                System.out.println("EMERGENCY_ADMIN_SERVICE_DIAGNOSTIC action=LOAD count=" + emergencyServices.size()
                        + " source=" + (emergencyServicesPreviewActive ? "preview" : "firestore"));
                if (!emergencyServicesPreviewActive) {
                    emergencyServices.forEach(service -> System.out.println(
                            "EMERGENCY_ADMIN_SERVICE_DIAGNOSTIC action=LOAD serviceId=" + service.serviceId()
                                    + " name=" + service.name() + " category=" + service.category()
                                    + " isActive=" + service.active()));
                }
                if ("Emergency".equals(selectedSection)) root.setCenter(scroll(emergencyPage()));
            }));
    }

    private VBox emergencyServiceForm() {
        EmergencyService s = editingEmergencyService;
        TextField name = new TextField(s == null ? "" : s.name()), sub = new TextField(s == null ? "" : s.subType()), lat = new TextField(s == null ? "" : String.valueOf(s.latitude())), lon = new TextField(s == null ? "" : String.valueOf(s.longitude())), address = new TextField(s == null ? "" : s.address()), sector = new TextField(s == null ? "" : s.sector()), area = new TextField(s == null ? "" : s.area()), landmark = new TextField(s == null ? "" : s.landmark()), contact = new TextField(s == null ? "" : s.contactNumber()), alternate = new TextField(s == null ? "" : s.alternateContact());
        TextArea desc = new TextArea(s == null ? "" : s.description());
        TextArea facilities = new TextArea(s == null ? "" : String.join(", ", s.facilities()));
        name.setPromptText("Enter service name"); sub.setPromptText("e.g. Emergency medical unit");
        contact.setPromptText("Primary contact number"); alternate.setPromptText("Optional alternate contact");
        sector.setPromptText("Sector or zone"); area.setPromptText("Area"); address.setPromptText("Street or access point"); landmark.setPromptText("Nearby landmark");
        lat.setPromptText("20.00..."); lon.setPromptText("73.79...");
        desc.setPromptText("Describe the emergency service and available support"); desc.setPrefRowCount(4);
        facilities.setPromptText("ICU, First Aid, Ambulance, Oxygen, Wheelchair..."); facilities.setPrefRowCount(3);
        ComboBox<String> category = combo(java.util.Arrays.stream(EmergencyService.Category.values()).map(Enum::name).toArray(String[]::new)); category.setValue(s == null ? "HOSPITAL" : s.category().name());
        ComboBox<String> status = combo(java.util.Arrays.stream(EmergencyService.OperationalStatus.values()).map(Enum::name).toArray(String[]::new)); status.setValue(s == null ? "OPEN" : s.operationalStatus().name());
        CheckBox active = new CheckBox("Active"); active.setSelected(s == null || s.active()); Label error = label("", "admin-emergency-form-error");
        Button save = new Button("SAVE SERVICE"); save.getStyleClass().add("primary-button"); save.setOnAction(e -> { try {
            double a=Double.parseDouble(lat.getText()), b=Double.parseDouble(lon.getText());
            if(name.getText().isBlank()||Double.isNaN(a)||Double.isNaN(b)||a==0&&b==0||Math.abs(a)>90||Math.abs(b)>180) throw new IllegalArgumentException("Enter a valid name and latitude/longitude.");
            if(a<19.4||a>20.5||b<73.4||b>74.4) { error.setText("Location appears outside the supported Nashik emergency area. Please verify."); return; }
            if (!emergencyServicesFirestoreAvailable) { error.setText("Preview only — Firebase access unavailable."); return; }
            String id=s==null?"svc-"+System.currentTimeMillis():s.serviceId();
            saveEmergencyService(new EmergencyService(id,name.getText(),EmergencyService.Category.valueOf(category.getValue()),sub.getText(),desc.getText(),a,b,address.getText(),sector.getText(),area.getText(),landmark.getText(),contact.getText(),alternate.getText(),EmergencyService.OperationalStatus.valueOf(status.getValue()),java.util.Arrays.stream(facilities.getText().split(",")).map(String::trim).filter(x->!x.isBlank()).toList(),active.isSelected(),s==null?String.valueOf(System.currentTimeMillis()):s.createdAt(),"",s==null?"":"", ""),save, true);
        } catch(Exception x){error.setText("Enter valid service values.");} });
        Button cancel=new Button("CANCEL"); cancel.getStyleClass().add("admin-emergency-cancel"); cancel.setOnAction(e->showSection("Emergency"));

        for (Node control : List.of(name, sub, lat, lon, address, sector, area, landmark, contact, alternate, desc, facilities, category, status)) {
            control.getStyleClass().add("admin-emergency-input");
            if (control instanceof javafx.scene.control.Control fxControl) fxControl.setMaxWidth(Double.MAX_VALUE);
        }
        GridPane basic = emergencyFormGrid(
                emergencyFormField("Service Name", true, name), emergencyFormField("Category", true, category),
                emergencyFormField("Sub Type", false, sub), emergencyFormField("Operational Status", true, status));
        GridPane contactLocation = emergencyFormGrid(
                emergencyFormField("Contact Number", false, contact), emergencyFormField("Alternate Contact", false, alternate),
                emergencyFormField("Sector", false, sector), emergencyFormField("Area", false, area),
                emergencyFormField("Address", false, address), emergencyFormField("Landmark", false, landmark));
        GridPane coordinates = emergencyFormGrid(
                emergencyFormField("Latitude", true, lat), emergencyFormField("Longitude", true, lon));
        Label coordinateHint = label("Enter the exact service location used on the Emergency map.", "admin-emergency-form-hint");
        VBox mapSelectorSlot = new VBox(8);
        Button selectOnMap = new Button("SELECT ON MAP");
        selectOnMap.getStyleClass().add("secondary-button");
        selectOnMap.setOnAction(event -> openEmergencyLocationSelector(lat, lon, error, mapSelectorSlot));
        VBox visibility = new VBox(3, label("Service Visibility", "admin-emergency-form-label"), active,
                label("Active services are visible on the User Emergency page.", "admin-emergency-form-hint"));
        visibility.getStyleClass().add("admin-emergency-visibility");
        HBox actions = new HBox(10, cancel, save); actions.setAlignment(Pos.CENTER_RIGHT); actions.getStyleClass().add("admin-emergency-form-actions");
        VBox form = new VBox(16);
        form.getStyleClass().add("admin-emergency-service-form");
        if (emergencyServicesPreviewActive) form.getChildren().add(label(emergencyServicesPreviewStatus(), "admin-emergency-preview-banner"));
        form.getChildren().addAll(
                emergencyFormSection("BASIC INFORMATION", basic),
                emergencyFormSection("CONTACT & LOCATION", contactLocation, label("LOCATION COORDINATES", "admin-emergency-form-subheading"), coordinates, coordinateHint, selectOnMap, mapSelectorSlot),
                emergencyFormSection("SERVICE DETAILS", emergencyFormField("Description", false, desc), emergencyFormField("Facilities", false, facilities), visibility),
                error, actions);
        return pageShell("Emergency Service", "Add or edit an Admin-managed emergency service.", infoPanel("SERVICE INFORMATION", form));
    }

    private GridPane emergencyFormGrid(Node... fields) {
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(12);
        for (int i = 0; i < fields.length; i++) { grid.add(fields[i], i % 2, i / 2); GridPane.setHgrow(fields[i], Priority.ALWAYS); }
        return grid;
    }

    private VBox emergencyFormField(String title, boolean required, Node control) {
        Label fieldTitle = label(title + (required ? " *" : ""), "admin-emergency-form-label");
        VBox field = new VBox(5, fieldTitle, control); field.getStyleClass().add("admin-emergency-form-field");
        GridPane.setHgrow(field, Priority.ALWAYS); return field;
    }

    private VBox emergencyFormSection(String title, Node... content) {
        VBox section = new VBox(10, label(title, "admin-emergency-form-section"));
        section.getChildren().addAll(content); section.getStyleClass().add("admin-emergency-form-section-box");
        return section;
    }

    private void openEmergencyLocationSelector(TextField latitudeField, TextField longitudeField, Label validation,
            VBox selectorSlot) {
        String previousLatitude = latitudeField.getText();
        String previousLongitude = longitudeField.getText();
        double initialLatitude = validCoordinateText(previousLatitude, true) ? Double.parseDouble(previousLatitude.trim()) : 20.0097;
        double initialLongitude = validCoordinateText(previousLongitude, false) ? Double.parseDouble(previousLongitude.trim()) : 73.7920;
        Label selected = label("Selected Location\nLatitude: " + formatCoordinate(initialLatitude)
                + "\nLongitude: " + formatCoordinate(initialLongitude), "admin-emergency-form-hint");
        EmergencyLocationSelector selector = new EmergencyLocationSelector((latitude, longitude) -> {
            latitudeField.setText(formatCoordinate(latitude));
            longitudeField.setText(formatCoordinate(longitude));
            selected.setText("Selected Location\nLatitude: " + formatCoordinate(latitude)
                    + "\nLongitude: " + formatCoordinate(longitude));
            if (!isNashikEmergencyCoordinate(latitude, longitude)) {
                validation.setText("Location appears outside the supported Nashik emergency area. Please verify.");
            } else {
                validation.setText("");
            }
        });
        selector.setInitialLocation(initialLatitude, initialLongitude);
        Button cancel = new Button("CANCEL");
        cancel.getStyleClass().add("admin-emergency-cancel");
        cancel.setOnAction(event -> {
            latitudeField.setText(previousLatitude);
            longitudeField.setText(previousLongitude);
            selectorSlot.getChildren().clear();
            validation.setText("");
        });
        Button use = new Button("USE THIS LOCATION");
        use.getStyleClass().add("primary-button");
        use.setOnAction(event -> selectorSlot.getChildren().clear());
        HBox actions = new HBox(10, cancel, createSpacer(), use);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox selectorCard = new VBox(9, label("Select Emergency Service Location", "admin-emergency-form-subheading"),
                selector.node(), selected, actions);
        selectorCard.getStyleClass().add("admin-emergency-location-selector");
        selectorSlot.getChildren().setAll(selectorCard);
    }

    private boolean validCoordinateText(String value, boolean latitude) {
        try {
            double coordinate = Double.parseDouble(value == null ? "" : value.trim());
            return Double.isFinite(coordinate) && coordinate != 0D
                    && (latitude ? coordinate >= -90D && coordinate <= 90D : coordinate >= -180D && coordinate <= 180D);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isNashikEmergencyCoordinate(double latitude, double longitude) {
        return latitude >= 19.4D && latitude <= 20.5D && longitude >= 73.4D && longitude <= 74.4D;
    }

    private String formatCoordinate(double coordinate) {
        return String.format(java.util.Locale.ROOT, "%.5f", coordinate);
    }
    private EmergencyService copyActive(EmergencyService s, boolean value){return new EmergencyService(s.serviceId(),s.name(),s.category(),s.subType(),s.description(),s.latitude(),s.longitude(),s.address(),s.sector(),s.area(),s.landmark(),s.contactNumber(),s.alternateContact(),s.operationalStatus(),s.facilities(),value,s.createdAt(),"",s.createdBy(),"");}
    private void saveEmergencyService(EmergencyService s, Button b) {
        saveEmergencyService(s, b, false);
    }
    /** Preview-row toggles are local; an explicit form save may create the first real record. */
    private void saveEmergencyService(EmergencyService s, Button b, boolean explicitFormSave) {
        if (emergencyServicesPreviewActive && !explicitFormSave) { updatePreviewService(s); return; }
        if (!emergencyServicesFirestoreAvailable) {
            emergencyServicesPreviewMessage = "Preview only — Firebase access unavailable.";
            showSection("Emergency");
            return;
        }
        b.setDisable(true); AppSession.User u=AppSession.currentUser();
        java.util.concurrent.CompletableFuture.runAsync(()->{try{emergencyFirestore.saveEmergencyService(s,u.idToken());Platform.runLater(this::loadEmergencyServicesAsync);}
            catch(Exception x){Platform.runLater(()->{b.setDisable(false);emergencyServicesError="permission";emergencyServicesPreviewMessage="Preview only — Firebase access unavailable.";showSection("Emergency");});}});
    }

    private void updatePreviewService(EmergencyService updated) {
        emergencyServices = emergencyServices.stream().map(service -> service.serviceId().equals(updated.serviceId()) ? updated : service).toList();
        emergencyServicesPreviewMessage = emergencyServicesFirestoreAvailable
                ? "Preview change only — no Firestore service records found."
                : "Preview only — Firebase access unavailable.";
        showSection("Emergency");
    }

    private void deleteEmergencyService(EmergencyService s, Button b) {
        if (!confirm("Delete Emergency Service", "Delete " + s.name() + "?")) return;
        if (emergencyServicesPreviewActive) {
            emergencyServices = emergencyServices.stream().filter(service -> !service.serviceId().equals(s.serviceId())).toList();
            emergencyServicesPreviewMessage = emergencyServicesFirestoreAvailable
                    ? "Preview deletion only — no Firestore service records found."
                    : "Preview only — Firebase access unavailable.";
            showSection("Emergency");
            return;
        }
        b.setDisable(true); AppSession.User u=AppSession.currentUser();
        java.util.concurrent.CompletableFuture.runAsync(()->{try{emergencyFirestore.deleteEmergencyService(s.serviceId(),u.idToken());Platform.runLater(this::loadEmergencyServicesAsync);}
            catch(Exception x){Platform.runLater(()->{b.setDisable(false);emergencyServicesError="permission";emergencyServicesPreviewMessage="Preview only — Firebase access unavailable.";showSection("Emergency");});}});
    }

    private String emergencyServicesPreviewStatus() {
        if (!emergencyServicesPreviewMessage.isBlank()) return emergencyServicesPreviewMessage;
        return emergencyServicesFirestoreAvailable
                ? "Local preview services — no Firestore service records found."
                : "Local preview services — Firebase access unavailable.";
    }

    private void updateEmergencyFromAdmin(EmergencyReport report, String priorityValue, String statusValue, String teamName, String notes, Button button) {
        EmergencyReport.Status status = EmergencyReport.Status.valueOf(statusValue);
        if (status == EmergencyReport.Status.TEAM_DISPATCHED && (teamName == null || "Unassigned".equals(teamName))) { showInfo("Assign a response team", "Assign a team before marking it dispatched."); return; }
        long now = System.currentTimeMillis(); String stamp = String.valueOf(now);
        String team = "Unassigned".equals(teamName) ? "" : teamName;
        EmergencyReport updated = new EmergencyReport(report.emergencyId(), report.trackingId(), report.userId(), report.reporterName(), report.reporterPhone(), report.emergencyType(), report.description(), report.peopleAffected(), report.landmark(), report.latitude(), report.longitude(), report.locationLabel(), EmergencyReport.Priority.valueOf(priorityValue), status, team.isBlank() ? "" : team.toLowerCase().replace(' ', '-'), team, team.isBlank() ? "" : team.split(" ")[0], notes, EmergencyReport.userStatusText(status), report.createdAt(), stamp,
                status.ordinal() >= EmergencyReport.Status.ACKNOWLEDGED.ordinal() ? valueOr(stamp, report.acknowledgedAt()) : report.acknowledgedAt(),
                status.ordinal() >= EmergencyReport.Status.TEAM_DISPATCHED.ordinal() ? valueOr(stamp, report.dispatchedAt()) : report.dispatchedAt(),
                status.ordinal() >= EmergencyReport.Status.HELP_ARRIVING.ordinal() ? valueOr(stamp, report.helpArrivingAt()) : report.helpArrivingAt(),
                status == EmergencyReport.Status.RESOLVED ? stamp : report.resolvedAt(), report.source());
        button.setDisable(true); AppSession.User user = AppSession.currentUser();
        java.util.concurrent.CompletableFuture.runAsync(() -> { try { emergencyFirestore.updateEmergencyReport(updated, user.idToken()); Platform.runLater(this::loadEmergencyReportsAsync); }
            catch (Exception exception) { System.err.println("EMERGENCY_ADMIN_UPDATE_DIAGNOSTIC action=UPDATE emergencyId=" + report.emergencyId() + " result=failed error=" + exception.getMessage()); Platform.runLater(() -> button.setDisable(false)); } });
    }

    private VBox reportsPage() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        long revenue = AppDataStore.bookings().stream()
                .filter(booking -> "PAID".equals(booking.paymentStatus))
                .mapToLong(booking -> booking.amountPaise)
                .sum() / 100;
        HBox stats = new HBox(12,
                metric(String.valueOf(overview.totalUsers()), "Users"),
                metric(String.valueOf(AppDataStore.businesses().size()), "Businesses"),
                metric(String.valueOf(AppDataStore.bookings().size()), "Bookings"),
                metric("Rs " + revenue, "Revenue"),
                metric(String.valueOf(AppDataStore.transportRoutes().size()), "Transport"),
                metric(String.valueOf(AppDataStore.lostFoundCases().size()), "Safety Cases"));
        VBox analytics = new VBox(8,
                dataRow("Users", "Role Distribution", roleDistribution()),
                dataRow("Businesses", "Business Status", businessStatusDistribution()),
                dataRow("Bookings", "Booking Status", bookingStatusDistribution()),
                dataRow("Transport", "Route Status", routeStatusDistribution()),
                dataRow("Lost & Found", "Case Status", lostStatusDistribution()),
                dataRow("Announcements", "Published Notices", String.valueOf(AppDataStore.items("announcement").size())));
        return pageShell("Reports & Analytics", "Use actual available data for platform insights.",
                stats,
                infoPanel("Analytics", analytics));
    }

    private VBox systemPage() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        return pageShell("System", "Technical infrastructure status.",
                infoPanel("Application Status",
                        dataRow("System", "Authentication", "Connected"),
                        dataRow("System", "Database", overview.firebaseConnected() ? "Connected" : "Sync Issue"),
                        dataRow("Bookings", "Payments", "Configured"),
                        dataRow("Users", "Current Session", isAdminSession() ? "Active" : "Inactive"),
                        dataRow("System", "Role Access", "user, business, transport_operator and admin checked from users/{uid}."),
                        dataRow("System", "Application", "Simhastha Connect | Nashik Simhastha 2027")));
    }

    private VBox listPanel(String title, String module, String emptyText) {
        return listPanel(title, module, emptyText, null);
    }

    private VBox listPanel(String title, String module, String emptyText, VBox form) {
        VBox rows = new VBox(10);
        List<AppDataStore.ServiceItem> items = AppDataStore.items(module);
        if (items.isEmpty()) {
            rows.getChildren().add(dataRow(module, "No published items", emptyText));
        } else {
            for (AppDataStore.ServiceItem item : items) {
                rows.getChildren().add(operationalItemRow(module, item, form));
            }
        }
        return infoPanel(title, rows);
    }

    private HBox operationalItemRow(String module, AppDataStore.ServiceItem item, VBox form) {
        Button view = smallButton("View");
        view.setOnAction(event -> showInfo(item.title, item.detail));
        Button edit = smallButton("Edit");
        edit.setDisable(form == null);
        edit.setOnAction(event -> fillOperationalForm(module, item, form));
        Button unpublish = smallButton("Unpublish");
        unpublish.setOnAction(event -> updateOperationalItem(module, item, false, true, unpublish));
        Button disable = smallButton("Disable");
        disable.setOnAction(event -> updateOperationalItem(module, item, false, false, disable));
        HBox row = new HBox(10, moduleIcon(module, "pilgrim-row-icon"),
                new VBox(2, strong(item.title), muted(item.detail)),
                createSpacer(), view, edit, unpublish, disable);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void fillOperationalForm(String module, AppDataStore.ServiceItem item, VBox form) {
        editingOperationalId = item.id;
        editingOperationalModule = module;
        List<TextField> fields = formFields(form);
        if (!fields.isEmpty()) {
            fields.get(0).setText(item.title);
        }
        for (int i = 1; i < fields.size(); i++) {
            String prompt = fields.get(i).getPromptText();
            fields.get(i).setText(detailValue(item.detail, prompt));
        }
    }

    private void updateOperationalItem(String module, AppDataStore.ServiceItem item, boolean published, boolean active,
            Button button) {
        button.setDisable(true);
        try {
            AppDataStore.updateOperationalItemFlags(module, item, published, active);
            showInfo("Updated", item.title + " was updated.");
            refreshAdminData();
        } catch (AppDataStore.ApprovalUpdateException exception) {
            showInfo("Update Failed", exception.getMessage());
        } finally {
            button.setDisable(false);
        }
    }

    private VBox structuredForm(String... prompts) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        java.util.List<TextField> fields = new java.util.ArrayList<>();
        for (int i = 0; i < prompts.length; i++) {
            TextField field = AppUi.textField(prompts[i]);
            fields.add(field);
            field.setMaxWidth(Double.MAX_VALUE);
            grid.add(field, i % 2, i / 2);
            GridPane.setHgrow(field, Priority.ALWAYS);
        }
        VBox box = new VBox(grid);
        box.getStyleClass().add("admin-structured-form");
        box.getProperties().put("orderedTextFields", fields);
        return box;
    }

    private HBox actionRow(String module, VBox form, String... actions) {
        HBox row = new HBox(10);
        for (String action : actions) {
            Button button = smallButton(action);
            if ("Add".equals(action) || "Add Route".equals(action) || "Publish".equals(action) || "Publish Alert".equals(action)
                    || "Add Event".equals(action) || "Update Crowd".equals(action)) {
                button.setOnAction(event -> publishStructuredItem(module, form));
            } else if ("Save Draft".equals(action)) {
                button.setOnAction(event -> saveDraftItem(module, form));
            } else if ("Expire".equals(action) || "Cancel".equals(action) || "Open/Close".equals(action)) {
                button.setOnAction(event -> showInfo(action, "Select an existing row and use Unpublish or Disable."));
            }
            row.getChildren().add(button);
        }
        return row;
    }

    private void publishStructuredItem(String module, VBox form) {
        List<TextField> fields = formFields(form);
        String title = fields.stream()
                .map(field -> field.getText().trim())
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElse("");
        String detail = fields.stream()
                .map(field -> {
                    String value = field.getText().trim();
                    String label = field.getPromptText() == null ? "Field" : field.getPromptText();
                    return value.isBlank() ? "" : label + ": " + value;
                })
                .filter(text -> !text.isBlank())
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
        if (title.isBlank() || detail.isBlank()) {
            showInfo("Missing Details", "Enter at least the main name/title and one detail before publishing.");
            return;
        }
        String id = editingOperationalModule.equals(module) && !editingOperationalId.isBlank()
                ? editingOperationalId
                : "";
        AppDataStore.saveOperationalItem(module, new AppDataStore.ServiceItem(id.isBlank()
                ? "admin-" + module + "-" + java.util.UUID.randomUUID().toString().substring(0, 8)
                : id, module, title, detail, AppDataStore.displayName(module)));
        editingOperationalId = "";
        editingOperationalModule = "";
        fields.forEach(TextField::clear);
        refreshAdminData();
    }

    private void saveDraftItem(String module, VBox form) {
        publishStructuredItem(module, form);
        List<AppDataStore.ServiceItem> items = AppDataStore.items(module);
        if (!items.isEmpty()) {
            AppDataStore.ServiceItem latest = items.get(items.size() - 1);
            try {
                AppDataStore.updateOperationalItemFlags(module, latest, false, true);
            } catch (AppDataStore.ApprovalUpdateException exception) {
                showInfo("Draft Save Failed", exception.getMessage());
            }
        }
    }

    private List<TextField> formFields(VBox form) {
        Object ordered = form.getProperties().get("orderedTextFields");
        if (ordered instanceof List<?> list) {
            return list.stream()
                    .filter(TextField.class::isInstance)
                    .map(TextField.class::cast)
                    .toList();
        }
        return form.lookupAll(".input-field").stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .toList();
    }

    private void clearPujaServiceForm(VBox form) {
        formFields(form).forEach(TextField::clear);
    }

    private String valueAt(List<TextField> fields, int index) {
        return index < fields.size() && fields.get(index).getText() != null ? fields.get(index).getText().trim() : "";
    }

    private boolean parseFlag(String value) {
        return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
                || "published".equalsIgnoreCase(value) || "active".equalsIgnoreCase(value)
                || "official".equalsIgnoreCase(value);
    }

    private String detailValue(String detail, String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        String prefix = label + ": ";
        for (String part : (detail == null ? "" : detail).split("\\|")) {
            String clean = part.trim();
            if (clean.startsWith(prefix)) {
                return clean.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private HBox filterRow(String... prompts) {
        HBox row = new HBox(10);
        row.getStyleClass().add("pilgrim-filter-row");
        for (String prompt : prompts) {
            if (prompt.equals("Role") || prompt.equals("Status") || prompt.equals("Area") || prompt.equals("Payment")) {
                ComboBox<String> combo = new ComboBox<>();
                combo.setPromptText(prompt);
                combo.getItems().addAll("All", "Active", "Pending", "Approved", "Rejected", "Suspended");
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

    private ComboBox<String> combo(String... values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(values);
        combo.setValue(values.length == 0 ? "" : values[0]);
        combo.getStyleClass().add("input-combo");
        combo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(combo, Priority.ALWAYS);
        return combo;
    }

    private boolean matches(String query, String... values) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String needle = query.trim().toLowerCase();
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String roleDistribution() {
        return AppDataStore.users().stream()
                .collect(java.util.stream.Collectors.groupingBy(user -> valueOr("unknown", user.role),
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + " | " + right)
                .orElse("No user records loaded");
    }

    private String businessStatusDistribution() {
        return AppDataStore.businesses().stream()
                .collect(java.util.stream.Collectors.groupingBy(business -> valueOr("unknown", business.status),
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + " | " + right)
                .orElse("No business records loaded");
    }

    private String bookingStatusDistribution() {
        return AppDataStore.bookings().stream()
                .collect(java.util.stream.Collectors.groupingBy(booking -> valueOr("unknown", booking.bookingStatus),
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + " | " + right)
                .orElse("No booking records loaded");
    }

    private String routeStatusDistribution() {
        long published = AppDataStore.transportRoutes().stream().filter(route -> route.published).count();
        long inactive = AppDataStore.transportRoutes().stream().filter(route -> !route.active).count();
        return "Published: " + published + " | Inactive: " + inactive + " | Total: "
                + AppDataStore.transportRoutes().size();
    }

    private String lostStatusDistribution() {
        return AppDataStore.lostFoundCases().stream()
                .collect(java.util.stream.Collectors.groupingBy(item -> valueOr("unknown", item.status),
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + " | " + right)
                .orElse("No lost/found records loaded");
    }

    private HBox tabRow(String... labels) {
        HBox row = new HBox(8);
        for (String text : labels) {
            row.getChildren().add(badge(text));
        }
        return row;
    }

    private HBox searchBar(String prompt) {
        TextField search = AppUi.textField(prompt);
        search.getStyleClass().add("pilgrim-search-field");
        Button button = new Button("Search");
        button.getStyleClass().add("pilgrim-search-button");
        HBox row = new HBox(0, search, button);
        row.getStyleClass().add("pilgrim-search-bar");
        HBox.setHgrow(search, Priority.ALWAYS);
        return row;
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

    private VBox pageShell(String title, String subtitle, Node... sections) {
        VBox content = new VBox(10, topControls());
        if (!"Admin Dashboard".equals(title)) {
            content.getChildren().add(new VBox(2, sectionTitle(title), muted(subtitle)));
        }
        content.getChildren().addAll(sections);
        content.getStyleClass().add("pilgrim-dashboard-main");
        content.setPadding(new Insets(12, 22, 28, 22));
        return content;
    }

    private VBox richCard(String targetSection, String title, String detail, String action) {
        VBox card = new VBox(8, new HBox(10, moduleIcon(targetSection, "pilgrim-card-icon"), badge(action)),
                strong(title), paragraph(detail), arrowAction(action, targetSection));
        card.getStyleClass().add("pilgrim-rich-card");
        return card;
    }

    private VBox infoPanel(String title, String body) {
        return infoPanel(title, paragraph(body));
    }

    private VBox infoPanel(String title, Node... body) {
        VBox panel = new VBox(10, sectionTitle(title));
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

    private Button smallButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("pilgrim-small-action");
        return button;
    }

    private Button arrowAction(String text, String targetSection) {
        Button button = smallButton(text + "  >");
        button.setOnAction(event -> showSection(targetSection));
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

    private Label paragraph(String text) {
        Label label = muted(text);
        label.setMaxWidth(780);
        return label;
    }

    private Label badge(String text) {
        return label(text, "pilgrim-badge");
    }

    private Label statusBadge(String text) {
        Label label = badge(text);
        label.getStyleClass().add(text.contains("No active") ? "admin-status-muted" : "admin-status-ok");
        return label;
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private Label moduleIcon(String module, String styleClass) {
        String glyph = switch (module) {
            case "Dashboard", "Overview" -> "\uE80F";
            case "Live Operations" -> "\uE7C1";
            case "Users" -> "\uE716";
            case "Business Approvals" -> "\uE7BA";
            case "Businesses" -> "\uE719";
            case "Bookings" -> "\uE8A7";
            case "Transport", "Transport Operations" -> "\uE806";
            case "Transport Operators" -> "\uE8EC";
            case "Kumbh Packages", "packages" -> "\uE8F8";
            case "Puja Services", "puja" -> "\uEC29";
            case "Ghats & Snan", "Ghats", "ghat" -> "\uE707";
            case "Stay", "stay" -> "\uE809";
            case "Lost & Found", "lost" -> "\uE721";
            case "Schedule & Events", "Schedule / Events", "schedule" -> "\uE787";
            case "Announcements", "announcement" -> "\uE789";
            case "FAQs / Help Center" -> "\uE9CE";
            case "Emergency", "emergency" -> "\uE95E";
            case "Reports & Analytics" -> "\uE9D2";
            case "System" -> "\uE713";
            case "Logout" -> "\uE7E8";
            default -> "\uE8A5";
        };
        return AppUi.symbolIcon(glyph, styleClass);
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
            String key = imageUrl.toExternalForm();
            Image image = IMAGE_CACHE.computeIfAbsent(key, Image::new);
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

    private String valueOr(String fallback, String value) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showInfo(String title, String message) {
        AppUi.showInfo(title, message, root == null || root.getScene() == null ? null : root.getScene().getWindow());
    }

    private boolean confirm(String title, String message) {
        return AppUi.confirm(title, message, root == null || root.getScene() == null ? null : root.getScene().getWindow());
    }

    /** Connection A editor: line-based repeatable configuration keeps every item data-driven without modal dialogs. */
    private final class PackageEditor {
        final TextField name = AppUi.textField("Package Name");
        final ComboBox<String> category = select("Package Category", "Premium", "Standard", "Budget");
        final TextField theme = AppUi.textField("Package Theme");
        final TextField badge = AppUi.textField("Optional Badge (Best Value / Popular / Family Choice)");
        final TextField origin = AppUi.textField("Origin City");
        final TextField destination = AppUi.textField("Destination");
        final TextField days = AppUi.textField("Days"); final TextField nights = AppUi.textField("Nights");
        final TextArea shortDescription = area("Short Description"); final TextArea description = area("Full Description");
        final TextArea travelOptions = area("One travel option per line — e.g. Flight | Delhi to Nashik | Included | Pickup/Drop");
        final CheckBox selfTravelEnabled = new CheckBox("Enable Self Travel"); final CheckBox simhasthaTravelEnabled = new CheckBox("Enable Simhastha Connect Travel"); final CheckBox flightEnabled = new CheckBox("Enable Flight");
        final TextField defaultOriginAirport = AppUi.textField("Default Origin Airport (IATA, e.g. IXU)"); final TextField destinationAirport = AppUi.textField("Destination Airport (IATA, e.g. ISK)");
        final CheckBox economyEnabled = new CheckBox("Economy"); final CheckBox premiumEconomyEnabled = new CheckBox("Premium Economy"); final CheckBox businessEnabled = new CheckBox("Business"); final CheckBox firstClassEnabled = new CheckBox("First Class");
        final TextField economyCharge = AppUi.textField("Economy package charge (INR)"); final TextField premiumEconomyCharge = AppUi.textField("Premium Economy package charge (INR)"); final TextField businessCharge = AppUi.textField("Business package charge (INR)"); final TextField firstClassCharge = AppUi.textField("First Class package charge (INR)");
        final TextField preferredAirlines = AppUi.textField("Preferred airlines (optional)"); final CheckBox flightAssistance = new CheckBox("Flight assistance included"); final CheckBox airportPickup = new CheckBox("Airport pickup included"); final TextField baggageNote = AppUi.textField("Baggage note"); final TextField travelInstructions = AppUi.textField("Travel instructions");
        final TextArea stayOptions = area("One stay option per line — e.g. Premium Hotel | 4 nights | Nashik | Included");
        final TextArea mealOptions = area("One meal option per line — e.g. Breakfast + Dinner | Included");
        final TextArea facilities = area("One facility per line — e.g. Ramkund Snan | Included");
        final TextArea touristPlaces = area("One tourist place per line — e.g. Trimbakeshwar | Included");
        final TextField itineraryDayTitle = AppUi.textField("Day Title — e.g. Day 1 — Arrival");
        final TextArea itineraryActivities = area("One activity per line — e.g. TRAVEL: Arrival in Nashik");
        final TextField basePrice = AppUi.textField("Base Package Price (INR)"); final TextField startingPrice = AppUi.textField("Starting Price (INR)");
        final TextField originalPrice = AppUi.textField("Original Price, if any (INR)"); final TextField discount = AppUi.textField("Discount %, if configured");
        final TextArea inclusions = area("One inclusion per line"); final TextArea exclusions = area("One exclusion per line"); final TextArea policies = area("One policy per line — e.g. Cancellation: Terms shared during booking");
        final TextField availableFrom = AppUi.textField("Available From (YYYY-MM-DD)"); final TextField availableUntil = AppUi.textField("Available Until (YYYY-MM-DD)"); final TextField departureDates = AppUi.textField("Possible Departure Dates"); final TextField maximumCapacity = AppUi.textField("Maximum Capacity"); final TextField minimumTravellers = AppUi.textField("Minimum Travellers");
        PackageMedia cover; PackageMedia hero; final List<PackageMedia> gallery = new ArrayList<>(); final VBox mediaRows = new VBox(8);
        final String id; final String code; final String createdBy; final String createdAt;

        PackageEditor(ManagedKumbhPackage p) {
            id = p == null ? java.util.UUID.randomUUID().toString() : p.packageId(); code = p == null ? "KPKG-2027-" + java.util.UUID.randomUUID().toString().substring(0, 5).toUpperCase() : p.packageCode(); createdBy = p == null ? (AppSession.currentUser() == null ? "" : AppSession.currentUser().uid()) : p.createdBy(); createdAt = p == null ? String.valueOf(System.currentTimeMillis()) : p.createdAt();
            destination.setText("Nashik – Simhastha 2027"); applyTravelConfig(PackageTravelConfig.defaults(""));
            if (p != null) populate(p);
        }
        Node basic() { return grid(name, category, theme, badge, origin, destination, days, nights, shortDescription, description); }
        Node travel() { return new VBox(10, travelOptions, muted("Simhastha Connect flight configuration — controls package upgrade charges, not live airline fares."), new HBox(12,selfTravelEnabled,simhasthaTravelEnabled,flightEnabled), grid(defaultOriginAirport,destinationAirport), new HBox(12,economyEnabled,premiumEconomyEnabled,businessEnabled,firstClassEnabled), grid(economyCharge,premiumEconomyCharge,businessCharge,firstClassCharge), preferredAirlines, new HBox(12,flightAssistance,airportPickup), baggageNote,travelInstructions); } Node stay() { return stayOptions; } Node meals() { return mealOptions; } Node experience() { return facilities; } Node sightseeing() { return touristPlaces; }
        Node itinerary() { return new VBox(9, itineraryDayTitle, itineraryActivities, muted("Use activity types such as TRAVEL, PICKUP, DROP, STAY, MEAL, GHAT, SNAN, TEMPLE, PUJA, TOURIST_PLACE, EVENT, FREE_TIME, or OTHER.")); }
        Node pricing() { return grid(basePrice, startingPrice, originalPrice, discount, muted("Currency: INR. Optional component prices are recorded in the relevant Travel, Stay, Meals, Experience and Sightseeing entries.")); }
        Node inclusions() { return new VBox(9, inclusions, exclusions); } Node policies() { return policies; } Node availability() { return grid(availableFrom, availableUntil, departureDates, maximumCapacity, minimumTravellers); }
        Node media() { Button coverButton = smallButton("Change Cover Image"); coverButton.setOnAction(e -> chooseMedia(PackageMediaType.COVER)); Button heroButton = smallButton("Change Hero Image"); heroButton.setOnAction(e -> chooseMedia(PackageMediaType.HERO)); Button add = smallButton("+ Add Photo"); add.setOnAction(e -> chooseMedia(PackageMediaType.GALLERY)); refreshMediaRows(); return new VBox(10, muted("Temporary local image references only (JPG, JPEG, PNG). Cloudinary upload is intentionally not enabled."), new HBox(8, coverButton, heroButton, add), mediaRows); }
        private void chooseMedia(PackageMediaType type) { FileChooser chooser = new FileChooser(); chooser.setTitle("Select " + type + " Image"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png")); File selected = chooser.showOpenDialog(stage); if (selected == null) return; PackageMedia candidate = new PackageMedia(java.util.UUID.randomUUID().toString(), selected.getAbsolutePath(), "", "", type, gallery.size(), type != PackageMediaType.GALLERY, String.valueOf(System.currentTimeMillis())); Optional<String> issue = PackageMediaService.temporary().validateReference(candidate.url()); if (issue.isPresent()) { showInfo("Image not added", issue.get()); return; } if (type == PackageMediaType.COVER) cover = candidate; else if (type == PackageMediaType.HERO) hero = candidate; else { if (gallery.size() >= 10) { showInfo("Gallery limit", "A package can have up to 10 gallery images."); return; } if (gallery.stream().anyMatch(m -> m.url().equals(candidate.url()))) { showInfo("Duplicate image", "This photo is already in the gallery."); return; } gallery.add(candidate); } refreshMediaRows(); }
        private void refreshMediaRows() { mediaRows.getChildren().clear(); addMediaRow("Cover Image", cover, PackageMediaType.COVER); addMediaRow("Hero Image", hero, PackageMediaType.HERO); for (int i = 0; i < gallery.size(); i++) addGalleryRow(i); }
        private void addMediaRow(String label, PackageMedia media, PackageMediaType type) { HBox row = new HBox(9, muted(label + ": " + (media == null ? "Not selected" : new File(media.url()).getName()))); if (media != null) { Button remove = smallButton("Remove"); remove.setOnAction(e -> { if (type == PackageMediaType.COVER) cover = null; else hero = null; refreshMediaRows(); }); row.getChildren().add(remove); } row.getStyleClass().add("package-media-admin-row"); mediaRows.getChildren().add(row); }
        private void addGalleryRow(int index) { PackageMedia media = gallery.get(index); TextField caption = AppUi.textField("Caption"); caption.setText(media.caption()); caption.textProperty().addListener((o, a, value) -> gallery.set(index, new PackageMedia(media.mediaId(), media.url(), media.publicId(), value, media.mediaType(), media.sortOrder(), false, media.createdAt()))); Button up = smallButton("↑"); up.setDisable(index == 0); up.setOnAction(e -> { java.util.Collections.swap(gallery, index, index - 1); normalizeGallery(); refreshMediaRows(); }); Button down = smallButton("↓"); down.setDisable(index == gallery.size() - 1); down.setOnAction(e -> { java.util.Collections.swap(gallery, index, index + 1); normalizeGallery(); refreshMediaRows(); }); Button remove = smallButton("Remove"); remove.setOnAction(e -> { gallery.remove(index); normalizeGallery(); refreshMediaRows(); }); HBox row = new HBox(8, muted((index + 1) + ". " + new File(media.url()).getName()), caption, up, down, remove); HBox.setHgrow(caption, Priority.ALWAYS); row.getStyleClass().add("package-media-admin-row"); mediaRows.getChildren().add(row); }
        private void normalizeGallery() { for (int i = 0; i < gallery.size(); i++) { PackageMedia m = gallery.get(i); gallery.set(i, new PackageMedia(m.mediaId(),m.url(),m.publicId(),m.caption(),PackageMediaType.GALLERY,i,false,m.createdAt())); } }
        private void populate(ManagedKumbhPackage p) { name.setText(p.name()); category.setValue(cap(p.category().name())); theme.setText(p.theme()); badge.setText(p.badge()); origin.setText(p.origin()); destination.setText(p.destination()); days.setText(String.valueOf(p.days())); nights.setText(String.valueOf(p.nights())); shortDescription.setText(p.shortDescription()); description.setText(p.description()); travelOptions.setText(lines(p.travelOptions().stream().filter(value -> !value.startsWith(PackageTravelConfig.PREFIX)).toList())); applyTravelConfig(PackageTravelConfig.fromTravelOptions(p.travelOptions(),p.origin())); stayOptions.setText(lines(p.stayOptions())); mealOptions.setText(lines(p.mealOptions())); facilities.setText(lines(p.facilities())); touristPlaces.setText(lines(p.touristPlaces())); if (!p.itinerary().isEmpty()) { itineraryDayTitle.setText(p.itinerary().get(0).title()); itineraryActivities.setText(p.itinerary().stream().flatMap(d -> d.items().stream()).map(KumbhPackage.Item::text).collect(java.util.stream.Collectors.joining("\n"))); } basePrice.setText(String.valueOf(p.basePrice())); startingPrice.setText(String.valueOf(p.startingPrice())); originalPrice.setText(p.originalPrice() == 0 ? "" : String.valueOf(p.originalPrice())); discount.setText(p.discount() == 0 ? "" : String.valueOf(p.discount())); inclusions.setText(lines(p.inclusions())); exclusions.setText(lines(p.exclusions())); policies.setText(p.policies().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(java.util.stream.Collectors.joining("\n"))); availableFrom.setText(p.availableFrom()); availableUntil.setText(p.availableUntil()); departureDates.setText(p.departureDates()); maximumCapacity.setText(String.valueOf(p.maximumCapacity())); minimumTravellers.setText(String.valueOf(p.minimumTravellers())); cover=p.coverImage(); hero=p.heroImage(); gallery.addAll(p.gallery()); }
        ManagedKumbhPackage toPackage(PackageStatus status) { List<KumbhPackage.Item> activities = linesList(itineraryActivities.getText()).stream().map(value -> new KumbhPackage.Item(activityType(value), value, "")).toList(); List<KumbhPackage.Day> itinerary = activities.isEmpty() ? List.of() : List.of(new KumbhPackage.Day(valueOr("Day 1 — Journey", itineraryDayTitle.getText()), activities)); Map<String,String> policyMap = new LinkedHashMap<>(); for(String value : linesList(policies.getText())) { int divider = value.indexOf(':'); policyMap.put(divider < 0 ? "Policy" : value.substring(0, divider).trim(), divider < 0 ? value : value.substring(divider + 1).trim()); } String now = String.valueOf(System.currentTimeMillis()); return new ManagedKumbhPackage(id, code, name.getText().trim(), PackageCategory.valueOf(category.getValue().toUpperCase()), theme.getText().trim(), badge.getText().trim(), origin.getText().trim(), valueOr("Nashik – Simhastha 2027", destination.getText().trim()), number(days), number(nights), shortDescription.getText().trim(), description.getText().trim(), configuredTravelOptions(), linesList(stayOptions.getText()), linesList(mealOptions.getText()), linesList(facilities.getText()), linesList(touristPlaces.getText()), itinerary, number(basePrice), number(startingPrice), number(originalPrice), number(discount), linesList(inclusions.getText()), linesList(exclusions.getText()), policyMap, availableFrom.getText().trim(), availableUntil.getText().trim(), departureDates.getText().trim(), number(maximumCapacity), number(minimumTravellers), status, createdBy, createdAt, now, status == PackageStatus.PUBLISHED ? now : "", cover, hero, List.copyOf(gallery)); }
        private void applyTravelConfig(PackageTravelConfig config) { selfTravelEnabled.setSelected(config.selfTravelEnabled());simhasthaTravelEnabled.setSelected(config.simhasthaConnectEnabled());flightEnabled.setSelected(config.flightEnabled());defaultOriginAirport.setText(config.defaultOriginAirport());destinationAirport.setText(config.destinationAirport());economyEnabled.setSelected(config.allowedCabins().contains(CabinClass.ECONOMY));premiumEconomyEnabled.setSelected(config.allowedCabins().contains(CabinClass.PREMIUM_ECONOMY));businessEnabled.setSelected(config.allowedCabins().contains(CabinClass.BUSINESS));firstClassEnabled.setSelected(config.allowedCabins().contains(CabinClass.FIRST_CLASS));economyCharge.setText(String.valueOf(config.charge(CabinClass.ECONOMY)));premiumEconomyCharge.setText(String.valueOf(config.charge(CabinClass.PREMIUM_ECONOMY)));businessCharge.setText(String.valueOf(config.charge(CabinClass.BUSINESS)));firstClassCharge.setText(String.valueOf(config.charge(CabinClass.FIRST_CLASS)));preferredAirlines.setText(config.preferredAirlines());flightAssistance.setSelected(config.flightAssistanceIncluded());airportPickup.setSelected(config.airportPickupIncluded());baggageNote.setText(config.baggageNote());travelInstructions.setText(config.travelInstructions()); }
        private List<String> configuredTravelOptions() { Set<CabinClass> cabins=EnumSet.noneOf(CabinClass.class);if(economyEnabled.isSelected())cabins.add(CabinClass.ECONOMY);if(premiumEconomyEnabled.isSelected())cabins.add(CabinClass.PREMIUM_ECONOMY);if(businessEnabled.isSelected())cabins.add(CabinClass.BUSINESS);if(firstClassEnabled.isSelected())cabins.add(CabinClass.FIRST_CLASS);Map<CabinClass,Integer> charges=new EnumMap<>(CabinClass.class);charges.put(CabinClass.ECONOMY,number(economyCharge));charges.put(CabinClass.PREMIUM_ECONOMY,number(premiumEconomyCharge));charges.put(CabinClass.BUSINESS,number(businessCharge));charges.put(CabinClass.FIRST_CLASS,number(firstClassCharge));PackageTravelConfig config=new PackageTravelConfig(selfTravelEnabled.isSelected(),simhasthaTravelEnabled.isSelected(),flightEnabled.isSelected(),defaultOriginAirport.getText().trim(),destinationAirport.getText().trim(),cabins,charges,preferredAirlines.getText().trim(),flightAssistance.isSelected(),airportPickup.isSelected(),baggageNote.getText().trim(),travelInstructions.getText().trim());List<String> values=new ArrayList<>(linesList(travelOptions.getText()));values.removeIf(value->value.startsWith(PackageTravelConfig.PREFIX));values.add(config.encode());return List.copyOf(values); }
    }
    private ComboBox<String> select(String prompt, String... values) { ComboBox<String> combo = new ComboBox<>(); combo.getItems().addAll(values); combo.setValue(values[0]); combo.setPromptText(prompt); combo.getStyleClass().add("input-combo"); return combo; }
    private TextArea area(String prompt) { TextArea field = new TextArea(); field.setPromptText(prompt); field.setPrefRowCount(3); field.setWrapText(true); field.getStyleClass().add("input-field"); return field; }
    private Node grid(Node... nodes) { GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); for (int i = 0; i < nodes.length; i++) { grid.add(nodes[i], i % 2, i / 2); GridPane.setHgrow(nodes[i], Priority.ALWAYS); } return grid; }
    private List<String> linesList(String value) { return java.util.Arrays.stream(value == null ? new String[0] : value.split("\\r?\\n")).map(String::trim).filter(s -> !s.isBlank()).toList(); }
    private String lines(List<String> values) { return String.join("\n", values); }
    private int number(TextField field) { try { return Integer.parseInt(field.getText().trim()); } catch (Exception e) { return 0; } }
    private ItineraryItemType activityType(String value) { String key = value == null ? "" : value.trim().toUpperCase().split("[: -]", 2)[0]; try { return ItineraryItemType.valueOf(key); } catch (Exception e) { return ItineraryItemType.OTHER; } }
    private String cap(String value) { return value.substring(0, 1) + value.substring(1).toLowerCase(); }

    private record AdminNotification(String id, String title, String detail, String targetSection) {
    }
}
