package com.simhastha.view;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
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
    private static final List<String> SECTIONS = List.of(
            "Dashboard", "Live Operations", "Users", "Business Approvals", "Businesses", "Bookings",
            "Transport", "Transport Operators", "Kumbh Packages", "Puja Services", "Ghats & Snan", "Stay", "Lost & Found",
            "Schedule & Events", "Announcements", "Emergency", "Reports & Analytics", "System");

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
            case "Emergency" -> emergencyPage();
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

        List<AdminNotification> notificationItems = notifications();
        long notificationCount = notificationItems.stream().filter(item -> !readNotifications.contains(item.id())).count();
        Button notifications = roundButton(notificationCount > 0 ? "\uE7F4 " + notificationCount : "\uE7F4",
                "Notifications");
        notifications.setOnAction(event -> showNotificationCenter(notifications, notificationItems));
        HBox actions = new HBox(10, title, createSpacer(), AppUi.createThemeToggle(),
                notifications, roundButton("\uE77B", "Admin Profile"));
        actions.getStyleClass().add("pilgrim-top-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private List<AdminNotification> notifications() {
        List<AdminNotification> items = new java.util.ArrayList<>();
        for (AppDataStore.ApprovalRequest request : AppDataStore.pendingApprovals()) {
            items.add(new AdminNotification("approval-" + request.ownerId, "New Business Registration",
                    request.title + " is waiting for review.", "Business Approvals"));
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
                items.add(new AdminNotification("operator-" + operator.operatorId, "Transport Operator Registration",
                        valueOr(operator.operatorId, operator.organizationName) + " is in the operator registry.",
                        "Transport Operators"));
            }
        }
        for (AppDataStore.RouteRecord route : AppDataStore.transportRoutes()) {
            if (!route.published) {
                items.add(new AdminNotification("route-" + route.routeId, "Transport Route Submission",
                        valueOr(route.routeId, route.routeName) + " is not published yet.", "Transport"));
            }
        }
        for (AppDataStore.LostFoundCaseRecord lostCase : AppDataStore.lostFoundCases()) {
            if ("high".equalsIgnoreCase(lostCase.priority) || "urgent".equalsIgnoreCase(lostCase.priority)) {
                items.add(new AdminNotification("lost-" + lostCase.caseId, "High Priority Lost & Found",
                        valueOr(lostCase.caseId, lostCase.name) + " needs attention.", "Lost & Found"));
            }
        }
        AppDataStore.bookings().stream()
                .filter(booking -> "PENDING".equalsIgnoreCase(booking.bookingStatus)
                        || "FAILED".equalsIgnoreCase(booking.paymentStatus))
                .limit(6)
                .forEach(booking -> items.add(new AdminNotification("booking-" + booking.bookingId,
                        "Booking Activity", booking.bookingId + " | " + booking.title, "Bookings")));
        return items.stream().distinct().toList();
    }

    private void showNotificationCenter(Button anchor, List<AdminNotification> items) {
        ContextMenu menu = new ContextMenu();
        if (items.isEmpty()) {
            MenuItem empty = new MenuItem("No new operational notifications");
            empty.setDisable(true);
            menu.getItems().add(empty);
        } else {
            for (AdminNotification item : items) {
                MenuItem menuItem = new MenuItem((readNotifications.contains(item.id()) ? "" : "* ")
                        + item.title() + " - " + item.detail());
                menuItem.setOnAction(event -> {
                    readNotifications.add(item.id());
                    showSection(item.targetSection());
                });
                menu.getItems().add(menuItem);
            }
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem readAll = new MenuItem("Mark all read");
            readAll.setOnAction(event -> {
                items.forEach(item -> readNotifications.add(item.id()));
                showSection(selectedSection);
            });
            menu.getItems().add(readAll);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
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
        VBox form = structuredForm("Package Name", "Package Type", "Source City", "Destination", "Duration",
                "Start Date", "End Date", "Price", "Seats / Capacity", "Hotel / Stay", "Itinerary", "Highlights");
        return pageShell("Kumbh Packages", "Manage published pilgrimage packages and booking readiness.",
                infoPanel("Package Manager", form, actionRow("packages", form, "Add", "Edit", "Publish", "Disable")),
                listPanel("Published Kumbh Packages", "packages", "No Kumbh packages published yet.", form));
    }

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
        VBox form = structuredForm("Ghat Name", "Location", "Map URL", "Snan Date", "Snan Time",
                "Crowd Level", "Safety Status", "Entry Status", "Exit Status", "Medical Support",
                "Police / Security", "Facilities", "Instructions");
        return pageShell("Ghats & Snan", "Manage bathing ghat status and public guidance.",
                infoPanel("Ghat Operations", form, actionRow("ghat", form, "Update Crowd", "Open/Close", "Add Guidance", "Publish Alert")),
                listPanel("Published Ghat Guidance", "ghat", "No ghat guidance published yet.", form));
    }

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
        VBox form = structuredForm("Event", "Date", "Start", "End", "Location", "Type", "Status");
        return pageShell("Schedule & Events", "Manage official event schedule and operational windows.",
                infoPanel("Schedule Manager", form, actionRow("schedule", form, "Add Event", "Edit", "Publish", "Cancel")),
                listPanel("Published Events", "schedule", "No schedule items published yet.", form));
    }

    private VBox announcementsPage() {
        VBox form = structuredForm("Title", "Category", "Priority", "Message", "Location optional",
                "Start Time", "Expiry Time");
        return pageShell("Announcements", "Compose and publish official notices.",
                tabRow("General", "Traffic", "Ghat", "Emergency", "Event", "Weather/Operational"),
                infoPanel("Announcement Composer", form, actionRow("announcement", form, "Save Draft", "Publish", "Expire")),
                listPanel("Published Announcements", "announcement", "No announcements published yet.", form));
    }

    private VBox emergencyPage() {
        VBox form = structuredForm("Name", "Type", "Phone", "Location", "Map Link", "Availability", "Verified", "Priority", "Active");
        return pageShell("Emergency Operations Center", "Maintain verified emergency information.",
                tabRow("Emergency Contacts", "Hospitals", "Medical Camps", "Police", "Women Safety", "Active Alerts"),
                infoPanel("Verified Emergency Information", form, actionRow("emergency", form, "Add", "Edit", "Verify", "Publish Alert")),
                listPanel("Published Emergency Items", "emergency", "No emergency items published yet.", form));
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private record AdminNotification(String id, String title, String detail, String targetSection) {
    }
}
