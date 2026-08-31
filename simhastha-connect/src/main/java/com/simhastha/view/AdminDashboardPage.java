package com.simhastha.view;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import com.simhastha.service.GhatCatalogueService;
import com.simhastha.service.GhatImageService;
import com.simhastha.service.GhatImageStorageService;
import com.simhastha.service.GhatService;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
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

import com.simhastha.schedule.ScheduleCategory;
import com.simhastha.schedule.ScheduleEvent;
import com.simhastha.schedule.ScheduleAlert;
import com.simhastha.schedule.ScheduleService;

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
    private final GhatService ghatService = new GhatService(new FirestoreGateway(FirebaseConfig.load()));
    private final GhatCatalogueService ghatCatalogueService = new GhatCatalogueService();
    private final GhatImageService ghatImageService = new GhatImageService();
    private final GhatImageStorageService ghatImageStorageService = new GhatImageStorageService();
    private List<Ghat> managedGhats = List.of();
    private VBox managedGhatRows;
    private final ScheduleService scheduleService = new ScheduleService();

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
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            AppDataStore.refreshFirebaseData(token);
            AppDataStore.refreshAdminOverview(token);
        }).whenComplete((ignored, error) -> Platform.runLater(() -> showSection(selectedSection)));
    }

    private void showSection(String section) {
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
                metricRow("Pending Approvals", String.valueOf(AppDataStore.pendingApprovals().size()), "Business registrations"),
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
        VBox form = structuredForm("Puja Name", "Temple / Ghat", "Puja Type", "Price", "Available Slots",
                "Pandit / Provider", "Verification Status", "Booking Status");
        return pageShell("Puja Services", "Manage verified puja services and booking readiness.",
                infoPanel("Puja Service Editor", form, actionRow("puja", form, "Add", "Edit", "Verify", "Disable", "View Bookings")),
                listPanel("Published Puja Services", "puja", "No puja services published yet.", form));
    }

    private VBox ghatsPage() {
        VBox rows = new VBox(10);
        managedGhatRows = rows;
        List<Ghat> initial = managedGhats.isEmpty() ? ghatCatalogueService.catalogue() : managedGhats;
        renderManagedGhats(rows, initial);
        Button add = new Button("Add New Ghat"); add.getStyleClass().add("primary-button"); add.setOnAction(event -> showGhatEditor(null));
        Button refresh = smallButton("Refresh"); refresh.setOnAction(event -> refreshManagedGhats(rows));
        HBox actions = new HBox(8, add, refresh); actions.setAlignment(Pos.CENTER_LEFT);
        refreshManagedGhats(rows);
        return pageShell("Ghats & Snan", "Manage the same Ghat records and live operational state used by pilgrims.",
                infoPanel("Shared Ghat Management", new VBox(8,
                        muted("Catalogue information is available immediately. Published backend records override the matching catalogue Ghat on the User page."), actions, rows)));
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
        for (Ghat ghat : ghats) rows.getChildren().add(managedGhatRow(ghat));
    }

    private HBox managedGhatRow(Ghat ghat) {
        ImageView image = ghatImageService.createView(ghat, 74, 52);
        Button edit = smallButton("Edit"); edit.setOnAction(event -> showGhatEditor(ghat));
        Button live = smallButton("Update Live Status"); live.setOnAction(event -> showGhatEditor(ghat, true));
        Button imageAction = smallButton("Change Image"); imageAction.setOnAction(event -> chooseGhatImage(ghat));
        Button publish = smallButton(ghat.published() ? "Unpublish" : "Publish"); publish.setOnAction(event -> saveGhat(withFlags(ghat, !ghat.published(), ghat.active())));
        Button active = smallButton(ghat.active() ? "Deactivate" : "Activate"); active.setOnAction(event -> saveGhat(withFlags(ghat, ghat.published(), !ghat.active())));
        VBox summary = new VBox(2, strong(ghat.name()), muted(ghat.area() + " | " + ghat.crowdLevel() + " | " + ghat.operationalStatus()
                + " | Bathing " + ghat.operationalState().bathingStatus() + " | Water " + ghat.operationalState().waterSafety()
                + " | Wait " + ghat.waitLabel() + " | " + (ghat.published() ? "Published" : "Draft") + " | " + (ghat.active() ? "Active" : "Inactive")));
        HBox row = new HBox(9, image, summary, createSpacer(), edit, live, imageAction, publish, active);
        HBox.setHgrow(summary, Priority.ALWAYS); row.getStyleClass().add("pilgrim-data-row"); row.setAlignment(Pos.CENTER_LEFT); return row;
    }

    private void chooseGhatImage(Ghat ghat) {
        System.out.println("CHANGE IMAGE clicked for ghatId = " + ghat.id());
        System.out.println("GHAT ID = " + ghat.id());
        System.out.println("OLD IMAGE = " + ghat.imageUrl());
        FileChooser chooser = new FileChooser(); chooser.setTitle("Choose Ghat image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            System.out.println("FILE CHOSEN = " + file.getAbsolutePath());
            System.out.println("FILE EXISTS = " + file.isFile() + ", readable = " + file.canRead());
            System.out.println("FILE SIZE = " + file.length());
            String managedImageUrl = ghatImageStorageService.store(file.toPath(), ghat.id());
            System.out.println("NEW IMAGE REFERENCE = " + managedImageUrl);
            System.out.println("NEW IMAGE = " + managedImageUrl);
            Ghat previewGhat = copyGhat(ghat, ghat.name(), ghat.area(), ghat.description(), managedImageUrl, ghat.crowdLevel(), ghat.estimatedWaitMinutes(),
                    ghat.operationalStatus(), ghat.operationalState().bathingStatus(), ghat.operationalState().waterSafety(), ghat.published(), ghat.active());
            Alert preview = new Alert(Alert.AlertType.CONFIRMATION, "Preview updated. Save this image to the shared Ghat record?", ButtonType.OK, ButtonType.CANCEL);
            preview.setTitle("Save Ghat image");
            preview.setHeaderText(ghat.name());
            preview.setGraphic(ghatImageService.createView(previewGhat, 210, 130));
            if (preview.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            ghatImageService.invalidate(ghat.imageUrl());
            ghatImageService.invalidate(managedImageUrl);
            updateGhatImage(previewGhat);
        } catch (java.io.IOException exception) {
            showInfo("Image update failed", "The selected image could not be copied into durable Ghat media. No Ghat record was changed.");
        }
    }

    private void showGhatEditor(Ghat source) { showGhatEditor(source, false); }

    private void showGhatEditor(Ghat source, boolean liveOnly) {
        Ghat ghat = source == null ? newGhatDraft() : source;
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(liveOnly ? "Update Live Ghat Status" : source == null ? "Add New Ghat" : "Edit Ghat");
        VBox form = new VBox(8); form.setPadding(new Insets(10));
        TextField name = editorField(ghat.name(), "Ghat Name"); TextField area = editorField(ghat.area(), "Area / Region");
        TextField description = editorField(ghat.description(), "Description"); TextField imageUrl = editorField(ghat.imageUrl(), "Image URL / shared media reference");
        TextField latitude = editorField(numberText(ghat.latitude()), "Latitude"); TextField longitude = editorField(numberText(ghat.longitude()), "Longitude");
        TextField history = editorField(ghat.history().historicalBackground(), "History"); TextField significance = editorField(ghat.history().religiousSignificance(), "Religious Significance");
        TextField simhastha = editorField(ghat.history().simhasthaConnection(), "Simhastha Connection"); TextField facilities = editorField(String.join(", ", ghat.facilities()), "Facilities (comma separated)");
        TextField steps = editorField(numberText(ghat.walking().approximateSteps()), "Steps"); TextField distance = editorField(numberText(ghat.walking().distanceMeters()), "Walking Distance (m)");
        TextField wait = editorField(numberText(ghat.estimatedWaitMinutes()), "Estimated Wait (minutes)"); TextField restriction = editorField(ghat.operationalState().restrictionReason(), "Restriction reason");
        TextField gates = editorField(gateText(ghat), "Gates: Name|STATUS, ..."); TextField zones = editorField(zoneText(ghat), "Zones: Name|STATUS, ...");
        TextField hazards = editorField(hazardText(ghat), "Hazards: TYPE|message, ..."); TextField alert = editorField(ghat.operationalState().priorityAlert().message(), "Priority alert");
        ComboBox<Ghat.WalkingDifficulty> walking = enumBox(Ghat.WalkingDifficulty.values(), ghat.walking().difficulty());
        ComboBox<Ghat.CrowdLevel> crowd = enumBox(Ghat.CrowdLevel.values(), ghat.crowdLevel());
        ComboBox<Ghat.OperationalStatus> operational = enumBox(Ghat.OperationalStatus.values(), ghat.operationalStatus());
        ComboBox<GhatOperationalState.BathingStatus> bathing = enumBox(GhatOperationalState.BathingStatus.values(), ghat.operationalState().bathingStatus());
        ComboBox<GhatOperationalState.WaterSafety> water = enumBox(GhatOperationalState.WaterSafety.values(), ghat.operationalState().waterSafety());
        ComboBox<GhatOperationalState.AlertPriority> priority = enumBox(GhatOperationalState.AlertPriority.values(), ghat.operationalState().priorityAlert().priority());
        CheckBox senior = new CheckBox("Senior Friendly"); senior.setSelected(ghat.walking().seniorFriendly());
        CheckBox wheelchair = new CheckBox("Wheelchair Accessible"); wheelchair.setSelected(ghat.walking().wheelchairAccessible());
        CheckBox published = new CheckBox("Published"); published.setSelected(ghat.published());
        CheckBox active = new CheckBox("Active"); active.setSelected(ghat.active());
        if (liveOnly) form.getChildren().addAll(labeled("Crowd Level", crowd), wait, labeled("Operational Status", operational), labeled("Bathing Status", bathing),
                labeled("Water Safety", water), restriction, gates, zones, hazards, labeled("Alert Priority", priority), alert, published, active);
        else form.getChildren().addAll(name, area, description, imageUrl, latitude, longitude, history, significance, simhastha, labeled("Walking Difficulty", walking), steps, distance,
                senior, wheelchair, facilities, labeled("Crowd Level", crowd), wait, labeled("Operational Status", operational), labeled("Bathing Status", bathing), labeled("Water Safety", water),
                restriction, gates, zones, hazards, labeled("Alert Priority", priority), alert, published, active);
        ScrollPane scroll = new ScrollPane(form); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(520); dialog.getDialogPane().setContent(scroll);
        ButtonType save = new ButtonType(source == null ? "Save Draft / Publish" : "Save", ButtonType.OK.getButtonData()); dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != save) return;
        if (!liveOnly && name.getText().trim().isBlank()) { showInfo("Ghat name required", "Enter a Ghat name before saving."); return; }
        Ghat result = copyGhat(ghat, liveOnly ? ghat.name() : name.getText(), liveOnly ? ghat.area() : area.getText(), liveOnly ? ghat.description() : description.getText(),
                liveOnly ? ghat.imageUrl() : imageUrl.getText(), crowd.getValue(), parseInteger(wait.getText()), operational.getValue(), bathing.getValue(), water.getValue(), published.isSelected(), active.isSelected(),
                liveOnly ? ghat.latitude() : parseDouble(latitude.getText()), liveOnly ? ghat.longitude() : parseDouble(longitude.getText()),
                liveOnly ? ghat.history() : new Ghat.History(history.getText(), significance.getText(), simhastha.getText(), ghat.history().associatedSacredPlaces(), ghat.history().rituals(), ghat.history().didYouKnow(), ghat.history().imageUrl()),
                liveOnly ? ghat.walking() : new Ghat.Walking(walking.getValue(), parseInteger(steps.getText()), parseInteger(distance.getText()), senior.isSelected(), wheelchair.isSelected()),
                liveOnly ? ghat.facilities() : split(facilities.getText()), parseOperationalState(bathing.getValue(), water.getValue(), restriction.getText(), priority.getValue(), alert.getText(), gates.getText(), zones.getText(), hazards.getText()));
        saveGhat(result);
    }

    private void saveGhat(Ghat ghat) { saveGhat(ghat, false); }

    private void saveGhat(Ghat ghat, boolean imageUpdate) {
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        if (imageUpdate) System.out.println("SAVE CALLED = ghatId=" + ghat.id() + ", imageUrl=" + ghat.imageUrl());
        ghatService.saveGhat(ghat, token).whenComplete((ignored, error) -> Platform.runLater(() -> {
            if (error != null) {
                if (imageUpdate) System.out.println("SAVE RESULT = failed: " + error.getClass().getSimpleName());
                showInfo(imageUpdate ? "Image update failed" : "Ghat save failed",
                        imageUpdate ? "The shared backend did not accept this image update. No successful save was assumed."
                                : "The shared backend did not accept this Ghat change. No successful save was assumed.");
                return;
            }
            if (imageUpdate) System.out.println("SAVE RESULT = success");
            if (imageUpdate) applySavedGhat(ghat);
            else refreshManagedGhats(managedGhatRows);
        }));
    }

    private void updateGhatImage(Ghat ghat) {
        AppSession.User user = AppSession.currentUser();
        String token = user == null ? "" : user.idToken();
        System.out.println("AUTH UID = " + (user == null ? "" : user.uid()));
        System.out.println("AUTH ROLE = " + (user == null ? "" : user.role()));
        System.out.println("SAVE CALLED = image-only ghatId=" + ghat.id() + ", imageUrl=" + ghat.imageUrl());
        ghatService.updateGhatImage(ghat, ghat.imageUrl(), token).whenComplete((result, error) -> Platform.runLater(() -> {
            if (error != null) {
                System.out.println("EXCEPTION = " + error);
                showInfo("Image update failed", "The shared backend rejected this image update. Check the application log for the Firebase status and response.");
                return;
            }
            System.out.println("SAVE RESULT = " + (result.locallySaved() ? "local fallback" : "backend success"));
            applySavedGhat(result.ghat());
            showInfo(result.locallySaved() ? "Saved locally" : "Image updated successfully",
                    result.locallySaved() ? "Cloud sync is currently unavailable. This Ghat image was saved locally and will remain available on this device."
                            : "The shared Ghat record was updated and verified from the backend.");
        }));
    }

    /** Keeps the Admin row in sync with the exact model that the backend accepted. */
    private void applySavedGhat(Ghat saved) {
        if (managedGhatRows == null) return;
        managedGhats = java.util.stream.Stream.concat(
                        managedGhats.stream().filter(existing -> !existing.id().equals(saved.id())), java.util.stream.Stream.of(saved))
                .toList();
        renderManagedGhats(managedGhatRows, managedGhats);
    }

    private Ghat newGhatDraft() {
        return new Ghat("ghat-" + java.util.UUID.randomUUID(), "", "", "", null, null, null, null, "",
                Ghat.OperationalStatus.INFORMATION_ONLY, Ghat.CrowdLevel.UNKNOWN, null, false, Ghat.Walking.unknown(), List.of(),
                Ghat.Weather.unavailable(), Ghat.History.unavailable(), "", GhatOperationalState.unavailable(), false, true);
    }

    private Ghat withFlags(Ghat ghat, boolean published, boolean active) {
        return copyGhat(ghat, ghat.name(), ghat.area(), ghat.description(), ghat.imageUrl(), ghat.crowdLevel(), ghat.estimatedWaitMinutes(),
                ghat.operationalStatus(), ghat.operationalState().bathingStatus(), ghat.operationalState().waterSafety(), published, active);
    }

    private Ghat copyGhat(Ghat base, String name, String area, String description, String imageUrl, Ghat.CrowdLevel crowd, Integer wait,
            Ghat.OperationalStatus operational, GhatOperationalState.BathingStatus bathing, GhatOperationalState.WaterSafety water, boolean published, boolean active) {
        return copyGhat(base, name, area, description, imageUrl, crowd, wait, operational, bathing, water, published, active,
                base.latitude(), base.longitude(), base.history(), base.walking(), base.facilities(),
                new GhatOperationalState(bathing, water, base.operationalState().hazards(), base.operationalState().zones(), base.operationalState().gates(),
                        base.operationalState().facilities(), base.operationalState().accessWindows(), base.operationalState().cleaningStatus(), base.operationalState().restrictionReason(),
                        base.operationalState().priorityAlert(), base.operationalState().lastUpdated()));
    }

    private Ghat copyGhat(Ghat base, String name, String area, String description, String imageUrl, Ghat.CrowdLevel crowd, Integer wait,
            Ghat.OperationalStatus operational, GhatOperationalState.BathingStatus bathing, GhatOperationalState.WaterSafety water, boolean published, boolean active,
            Double latitude, Double longitude, Ghat.History history, Ghat.Walking walking, List<String> facilities, GhatOperationalState state) {
        return new Ghat(base.id(), name, area, description, latitude, longitude, base.entryLatitude(), base.entryLongitude(), imageUrl, operational, crowd, wait,
                bathing == GhatOperationalState.BathingStatus.AVAILABLE, walking, facilities, base.weather(), history, String.valueOf(System.currentTimeMillis()), state, published, active);
    }

    private TextField editorField(String value, String prompt) { TextField field = new TextField(value == null ? "" : value); field.setPromptText(prompt); return field; }
    private <T> ComboBox<T> enumBox(T[] values, T selected) { ComboBox<T> box = new ComboBox<>(); box.getItems().addAll(values); box.setValue(selected); return box; }
    private Node labeled(String text, Node control) { return new VBox(3, muted(text), control); }
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
        Alert dialog = new Alert(Alert.AlertType.NONE); dialog.setTitle(existing == null ? "Add Event" : "Edit Event"); dialog.getDialogPane().setContent(form); ButtonType save = new ButtonType(existing == null ? "Save Event" : "Save Changes"); dialog.getButtonTypes().addAll(ButtonType.CANCEL, save);
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
        Alert dialog = new Alert(Alert.AlertType.NONE); dialog.setTitle(existing == null ? "Add Schedule Alert" : "Edit Schedule Alert"); dialog.getDialogPane().setContent(form); ButtonType save = new ButtonType(existing == null ? "Save Alert" : "Save Changes"); dialog.getButtonTypes().addAll(ButtonType.CANCEL, save);
        dialog.showAndWait().ifPresent(result -> { if (result != save) return; try { LocalTime from = start.getText().isBlank() ? null : LocalTime.parse(start.getText().trim()); LocalTime to = end.getText().isBlank() ? null : LocalTime.parse(end.getText().trim()); if ((from == null) != (to == null) || (from != null && !to.isAfter(from))) throw new IllegalArgumentException("Alert end time must be after start time."); saveScheduleAlert(new ScheduleAlert(existing == null ? "" : existing.id(), title.getText().trim(), message.getText().trim(), location.getText().trim(), date.getValue(), from, to, severity.getValue(), active.isSelected())); } catch (Exception exception) { showInfo("Alert not saved", exception.getMessage()); } });
    }

    private void showScheduleAlerts(LocalDate date) {
        VBox rows = new VBox(9, muted("Loading alerts...")); Button add = smallButton("+ Add Alert"); add.setOnAction(event -> showScheduleAlertForm(null, date));
        Alert dialog = new Alert(Alert.AlertType.NONE); dialog.setTitle("Manage Schedule Alerts"); dialog.getDialogPane().setContent(new VBox(12, add, rows)); dialog.getButtonTypes().add(ButtonType.CLOSE);
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
        for (int i = 0; i < prompts.length; i++) {
            TextField field = AppUi.textField(prompts[i]);
            field.setMaxWidth(Double.MAX_VALUE);
            grid.add(field, i % 2, i / 2);
            GridPane.setHgrow(field, Priority.ALWAYS);
        }
        VBox box = new VBox(grid);
        box.getStyleClass().add("admin-structured-form");
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
        return form.lookupAll(".input-field").stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .toList();
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
