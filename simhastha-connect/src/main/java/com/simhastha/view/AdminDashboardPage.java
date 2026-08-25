package com.simhastha.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class AdminDashboardPage {

    private static final DateTimeFormatter ADMIN_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy | hh:mm a");
    private static final List<String> SECTIONS = List.of(
            "Overview", "Live Operations", "Users", "Business Approvals", "Businesses", "Bookings",
            "Transport", "Transport Operators", "Puja Services", "Ghats & Snan", "Stay", "Lost & Found",
            "Schedule & Events", "Announcements", "Emergency", "Reports & Analytics", "System", "Logout");

    private BorderPane page;
    private Stage stage;
    private VBox sidebar;
    private VBox contentHost;
    private VBox approvalList;
    private VBox dataList;
    private ComboBox<String> moduleSelect;
    private String selectedSection = "Overview";

    public Scene createScene(Stage stage) {
        this.stage = stage;
        if (!isAdminSession()) {
            return createAccessDeniedScene(stage);
        }

        page = new BorderPane();
        page.getStyleClass().add("admin-dashboard-root");
        page.setCenter(loadingPanel());

        Scene scene = new Scene(page, 1200, 680);
        ThemeManager.addTheme(scene, this);
        ThemeManager.addListener(() -> ThemeManager.applyTo(page));
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
                    stage.setScene(new LoginSelectionPage().createScene(stage));
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
            stage.setScene(new AdminAuthPage().createScene(stage));
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

    private HBox createHeader() {
        Label title = new Label("Admin Dashboard");
        title.getStyleClass().add("admin-page-title");
        Label subtitle = new Label("Operations & Governance Control");
        subtitle.getStyleClass().add("page-subtitle");

        AppSession.User user = AppSession.currentUser();
        HBox profile = adminProfile(user);
        Label time = new Label(currentTimeText());
        time.getStyleClass().add("admin-time-chip");
        Label connection = compactSystemChip(AppDataStore.adminOverview());

        HBox header = new HBox(12, new VBox(2, title, subtitle), AppUi.spacer(), connection, notificationBell(),
                time, AppUi.createThemeToggle(), profile);
        header.getStyleClass().add("admin-topbar");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private StackPane notificationBell() {
        Label bell = AppUi.symbolIcon("\uE7F4", "admin-bell-icon");
        int unread = AppDataStore.pendingApprovals().size();
        Label badge = new Label(String.valueOf(unread));
        badge.getStyleClass().add("admin-badge");
        badge.setVisible(unread > 0);
        StackPane stack = new StackPane(bell, badge);
        stack.getStyleClass().add("admin-bell");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        return stack;
    }

    private HBox adminProfile(AppSession.User user) {
        Label avatar = AppUi.symbolIcon("\uE77B", "admin-avatar-icon");
        Label name = new Label(user == null || user.displayName().isBlank() ? "Admin" : user.displayName());
        name.getStyleClass().add("admin-profile-name");
        Label role = new Label("Administrator");
        role.getStyleClass().add("admin-profile-role");
        HBox profile = new HBox(9, avatar, new VBox(1, name, role));
        profile.getStyleClass().add("admin-profile-chip");
        profile.setAlignment(Pos.CENTER_LEFT);
        return profile;
    }

    private VBox loadingPanel() {
        VBox panel = new VBox(10, sectionTitle("Loading Administration Data"),
                rowDetail("Fetching Firestore overview, business approvals and platform modules..."));
        panel.getStyleClass().add("management-panel");
        panel.setPadding(new Insets(24));
        panel.setMaxWidth(620);
        return panel;
    }

    private void refreshAdminData() {
        String token = AppSession.currentUser() == null ? "" : AppSession.currentUser().idToken();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            AppDataStore.refreshFirebaseData(token);
            AppDataStore.refreshAdminOverview(token);
        }).whenComplete((ignored, error) -> Platform.runLater(() -> {
            page.setCenter(createAdminShell());
        }));
    }

    private HBox createAdminShell() {
        sidebar = createSidebar();
        contentHost = new VBox();
        contentHost.getStyleClass().add("admin-content-host");
        renderSelectedSection();

        ScrollPane contentScroll = new ScrollPane(contentHost);
        contentScroll.getStyleClass().add("page-scroll");
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox workspace = new VBox(12, createHeader(), contentScroll);
        workspace.getStyleClass().add("admin-workspace");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        HBox shell = new HBox(sidebar, workspace);
        shell.getStyleClass().add("admin-shell");
        HBox.setHgrow(workspace, Priority.ALWAYS);
        return shell;
    }

    private VBox createSidebar() {
        VBox nav = new VBox(6);
        nav.getStyleClass().add("admin-sidebar");
        nav.getChildren().add(adminSidebarBrand());
        VBox menu = new VBox(5);
        menu.getStyleClass().add("admin-sidebar-menu");
        for (String section : SECTIONS) {
            if ("Logout".equals(section)) {
                continue;
            }
            Button button = new Button(section);
            button.setGraphic(AppUi.symbolIcon(sectionIcon(section),
                    section.equals(selectedSection) ? "admin-nav-icon-active" : "admin-nav-icon"));
            button.getStyleClass().add(section.equals(selectedSection) ? "admin-nav-active" : "admin-nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setOnAction(event -> {
                selectedSection = section;
                sidebar.getChildren().setAll(createSidebar().getChildren());
                renderSelectedSection();
            });
            menu.getChildren().add(button);
        }
        ScrollPane menuScroll = new ScrollPane(menu);
        menuScroll.getStyleClass().add("admin-menu-scroll");
        menuScroll.setFitToWidth(true);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(menuScroll, Priority.ALWAYS);

        Button logout = new Button("Logout");
        logout.setGraphic(AppUi.symbolIcon("\uE7E8", "admin-nav-icon-danger"));
        logout.getStyleClass().add("admin-sidebar-action");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.setOnAction(event -> {
            AppSession.clear();
            stage.setScene(new AdminAuthPage().createScene(stage));
        });
        nav.getChildren().addAll(menuScroll, logout);
        return nav;
    }

    private HBox adminSidebarBrand() {
        ImageView logo = createImage("/images/sclogo.png", 52, 52);
        logo.getStyleClass().add("admin-sidebar-logo-image");
        Label name = new Label("SIMHASTHA\nCONNECT");
        name.getStyleClass().add("admin-sidebar-brand-strong");
        Label detail = new Label("ADMINISTRATION\nNashik Simhastha 2027");
        detail.getStyleClass().add("admin-sidebar-tagline");
        HBox brand = new HBox(10, logo, new VBox(1, name, detail));
        brand.getStyleClass().add("admin-sidebar-brand");
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    private void renderSelectedSection() {
        if (contentHost != null) {
            contentHost.getChildren().setAll(sectionHeader(selectedSection), sectionContent(selectedSection));
        }
    }

    private VBox sectionContent(String section) {
        return switch (section) {
            case "Overview" -> overviewSection();
            case "Live Operations" -> liveOperationsSection();
            case "Users" -> usersSection();
            case "Business Approvals" -> createApprovalPanel();
            case "Businesses" -> businessesSection();
            case "Bookings" -> createBookingsPanel();
            case "Transport" -> createModuleManager("transport", "Transport", "Publish approved user-facing routes and fare guidance.");
            case "Transport Operators" -> transportOperatorsSection();
            case "Puja Services" -> createModuleManager("puja", "Puja Services", "Manage public puja support listings.");
            case "Ghats & Snan" -> createModuleManager("ghat", "Ghats & Snan", "Manage bathing ghat and crowd guidance.");
            case "Stay" -> createModuleManager("stay", "Stay", "Manage accommodation and camp guidance.");
            case "Lost & Found" -> createModuleManager("lost", "Lost & Found", "Manage public lost/found help information.");
            case "Schedule & Events" -> createModuleManager("schedule", "Schedule & Events", "Publish event schedule and operations windows.");
            case "Announcements" -> createModuleManager("announcement", "Announcements", "Publish official notices and alerts.");
            case "Emergency" -> createModuleManager("emergency", "Emergency", "Maintain verified emergency contacts.");
            case "Reports & Analytics" -> reportsSection();
            case "System" -> systemSection();
            default -> overviewSection();
        };
    }

    private VBox sectionHeader(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("admin-section-heading");
        Label detail = new Label(sectionSubtitle(titleText));
        detail.getStyleClass().add("page-subtitle");
        detail.setWrapText(true);
        VBox header = new VBox(4, title, detail);
        header.getStyleClass().add("admin-section-header");
        return header;
    }

    private String sectionSubtitle(String section) {
        return switch (section) {
            case "Overview" -> "Firestore-backed platform snapshot with live operational signals.";
            case "Business Approvals" -> "Approve or reject pending business registrations from Firestore.";
            case "Reports & Analytics" -> "High-level platform metrics and analytics foundations.";
            case "System" -> "Firebase connection, admin bootstrap and operational safeguards.";
            default -> "Monitor, manage and publish platform-wide Simhastha Connect data.";
        };
    }

    private VBox overviewSection() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        TilePane kpis = kpiGrid();
        kpis.getChildren().addAll(
                kpiCard("Total Users", overview.totalUsers(), "Registered platform accounts", "\uE716"),
                kpiCard("Approved Businesses", overview.approvedBusinesses(), "Published marketplace providers", "\uE719"),
                kpiCard("Pending Approvals", overview.pendingBusinesses(), "Waiting for admin decision", "\uE7BA"),
                kpiCard("Transport Operators", overview.transportOperators(), "Registered operator profiles", "\uE806"),
                kpiCard("Active Routes", overview.activeRoutes(), "Published transport items", "\uE707"),
                kpiCard("Today's Bookings", overview.todaysBookings(), "Booking records available", "\uE8A7"),
                kpiCard("Active Events", overview.activeEvents(), "Published schedule items", "\uE787"),
                kpiCard("Open Lost & Found Cases", overview.lostFoundOpenCases(), "Open help cases/items", "\uE721"));

        HBox lower = new HBox(14, recentActivityPanel(), operationsStatusPanel());
        HBox.setHgrow(lower.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lower.getChildren().get(1), Priority.ALWAYS);

        VBox content = new VBox(14, overviewHero(), sectionTitle("Operational Overview"), kpis, lower,
                liveOperationsPreview());
        if (!overview.firebaseConnected()) {
            content.getChildren().add(1, syncIssueBanner());
        }
        return content;
    }

    private VBox liveOperationsSection() {
        return new VBox(16, liveOperationsPreview(), recentActivityPanel(),
                emptyPanel("Live command stream", "Realtime incident, crowd, transport and alert streams can be connected in Part 2."));
    }

    private VBox usersSection() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        TilePane stats = kpiGrid();
        stats.getChildren().addAll(
                kpiCard("Total Users", overview.totalUsers(), "Firestore users collection"),
                kpiCard("Admin Access", isAdminSession() ? 1 : 0, "Current active admin session"));
        return moduleTemplate("Users", "Review platform identities and role safety.", stats,
                emptyPanel("User management foundation",
                "Role-filtered user table, suspension actions and profile inspection are reserved for Part 2."));
    }

    private VBox businessesSection() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        TilePane stats = kpiGrid();
        stats.getChildren().addAll(
                kpiCard("Approved Businesses", overview.approvedBusinesses(), "Firestore businesses approved"),
                kpiCard("Pending Businesses", overview.pendingBusinesses(), "Needs admin review"));
        return moduleTemplate("Businesses", "Monitor approved business records and public listings.", stats,
                listPanel("Approved Business Listings", AppDataStore.items("business"),
                        "No approved business listings are published yet."));
    }

    private VBox transportOperatorsSection() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        TilePane stats = kpiGrid();
        stats.getChildren().addAll(
                kpiCard("Transport Operators", overview.transportOperators(), "Firestore transportOperators collection"),
                kpiCard("Active Routes", overview.activeRoutes(), "Published user routes"));
        return moduleTemplate("Transport Operators", "Monitor operator profiles and route publishing readiness.", stats,
                emptyPanel("Transport operator registry foundation",
                "Operator profile table, suspend actions and operator detail review are reserved for Part 2."));
    }

    private VBox reportsSection() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        TilePane cards = kpiGrid();
        cards.getChildren().addAll(
                kpiCard("Users", overview.totalUsers(), "Current platform reach", "\uE716"),
                kpiCard("Businesses", overview.approvedBusinesses(), "Approved providers", "\uE719"),
                kpiCard("Bookings", overview.todaysBookings(), "Booking records visible to admin", "\uE8A7"),
                kpiCard("Open Lost & Found", overview.lostFoundOpenCases(), "Open support items", "\uE721"));
        return moduleTemplate("Reports & Analytics", "Platform, business, transport, bookings and safety insights.", cards,
                emptyPanel("Analytics foundation",
                "Charts, exports, date filters and operational reports are intended for Part 2."));
    }

    private StackPane overviewHero() {
        ImageView image = createImage("/images/ramkund_sunrise.jpg", 980, 148);
        image.getStyleClass().add("admin-hero-image");
        Rectangle clip = new Rectangle(980, 148);
        clip.setArcWidth(22);
        clip.setArcHeight(22);
        image.setClip(clip);

        Label eyebrow = new Label("SIMHASTHA CONNECT");
        eyebrow.getStyleClass().add("admin-hero-eyebrow");
        Label title = new Label("Administration & Operations Control Center");
        title.getStyleClass().add("admin-hero-title");
        Label detail = new Label("Unified control for Simhastha services, safety and public operations.");
        detail.getStyleClass().add("admin-hero-detail");
        detail.setWrapText(true);
        HBox badges = new HBox(8,
                statusChip("Admin Active", true),
                statusChip("Operations", true));
        VBox text = new VBox(6, eyebrow, title, detail, badges);
        text.setPadding(new Insets(18, 20, 18, 22));
        StackPane hero = new StackPane(image, text);
        hero.getStyleClass().add("admin-hero");
        StackPane.setAlignment(text, Pos.CENTER_LEFT);
        return hero;
    }

    private VBox systemSection() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        VBox bootstrap = new VBox(9,
                infoRow("\uE8D7", "First admin bootstrap",
                        "Create a Firebase Authentication user, then create users/{uid} with uid, name, email, role = admin, status = active, createdAt."),
                infoRow("\uE72E", "No public admin registration",
                        "Admin access remains Firebase Auth plus Firestore role/status verification only."),
                infoRow("\uE753", "Firebase project",
                        "Configured through existing firebase.properties, expected project: superxkhumbh."));
        bootstrap.getStyleClass().add("management-panel");
        bootstrap.setPadding(new Insets(16));
        return moduleTemplate("System", "Connection health, admin access and application safety.", statusBanner(overview),
                createFirebasePanel(), bootstrap);
    }

    private VBox createApprovalPanel() {
        approvalList = new VBox(9);
        refreshApprovals();
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        TilePane stats = kpiGrid();
        stats.getChildren().addAll(
                kpiCard("Pending Requests", AppDataStore.pendingApprovals().size(), "Awaiting admin decision", "\uE7BA"),
                kpiCard("Approved Today", 0, "Daily approval analytics in Part 2", "\uE73E"),
                kpiCard("Rejected", 0, "Rejection analytics in Part 2", "\uE711"),
                kpiCard("Total Businesses", overview.approvedBusinesses() + overview.pendingBusinesses(),
                        "Approved plus pending records", "\uE719"));
        VBox panel = new VBox(14, sectionTitle("Pending Request Queue"), approvalList);
        panel.getStyleClass().add("management-panel");
        panel.setMinHeight(360);
        return moduleTemplate("Business Approvals", "Review and manage pending business registrations.", stats, panel);
    }

    private VBox createModuleManager(String module, String titleText, String helperText) {
        moduleSelect = new ComboBox<>();
        moduleSelect.getItems().addAll("transport", "puja", "ghat", "emergency", "stay", "lost", "schedule",
                "business", "announcement");
        moduleSelect.setValue(module);
        moduleSelect.getStyleClass().add("input-combo");
        moduleSelect.setOnAction(event -> {
            selectedSection = moduleSectionName(moduleSelect.getValue());
            renderSelectedSection();
        });

        TextField titleInput = AppUi.textField("Title / time / name");
        TextField detailInput = AppUi.textField("Details / number / route / instruction");
        Button add = new Button("Publish to User App");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> {
            if (!titleInput.getText().trim().isEmpty() && !detailInput.getText().trim().isEmpty()) {
                AppDataStore.addItem(moduleSelect.getValue(), titleInput.getText().trim(), detailInput.getText().trim());
                titleInput.clear();
                detailInput.clear();
                refreshDataList();
                refreshAdminData();
            }
        });

        dataList = new VBox(9);
        refreshDataList();
        VBox panel = new VBox(12, sectionTitle(titleText + " Management"), rowDetail(helperText),
                moduleSelect, titleInput, detailInput, add, dataList);
        panel.getStyleClass().add("management-panel");
        return moduleTemplate(titleText, helperText, panel);
    }

    private String moduleSectionName(String module) {
        return switch (module) {
            case "transport" -> "Transport";
            case "puja" -> "Puja Services";
            case "ghat" -> "Ghats & Snan";
            case "stay" -> "Stay";
            case "lost" -> "Lost & Found";
            case "schedule" -> "Schedule & Events";
            case "announcement" -> "Announcements";
            case "emergency" -> "Emergency";
            case "business" -> "Businesses";
            default -> "Overview";
        };
    }

    private VBox createBookingsPanel() {
        long successful = AppDataStore.bookings().stream().filter(booking -> "PAID".equals(booking.paymentStatus)).count();
        long pending = AppDataStore.bookings().stream().filter(booking -> "PENDING".equals(booking.paymentStatus)).count();
        long failed = AppDataStore.bookings().stream().filter(booking -> "FAILED".equals(booking.paymentStatus)).count();
        long collected = AppDataStore.bookings().stream()
                .filter(booking -> "PAID".equals(booking.paymentStatus))
                .mapToLong(booking -> booking.amountPaise)
                .sum();

        TilePane summary = kpiGrid();
        summary.getChildren().addAll(
                kpiCard("Total Transactions", AppDataStore.bookings().size(), "Central payment records"),
                kpiCard("Successful", successful, "Verified paid bookings"),
                kpiCard("Pending", pending, "Processing or awaiting webhook"),
                kpiCard("Failed", failed, "Failed or cancelled payments"),
                kpiCard("Total Collected", "Rs " + (collected / 100), "Paid amount only"));

        VBox rows = new VBox(9);
        if (AppDataStore.bookings().isEmpty()) {
            rows.getChildren().add(infoRow("\uE8A5", "No transactions yet", "Paid module bookings will appear here."));
        } else {
            for (AppDataStore.BookingRecord booking : AppDataStore.bookings()) {
                rows.getChildren().add(infoRow("\uE8A7", booking.title + " | " + booking.paymentStatus,
                        "Booking: " + booking.bookingId + " | User: " + booking.userId + " | Module: "
                                + booking.moduleType + " | Amount: Rs " + (booking.amountPaise / 100)));
            }
        }
        VBox panel = new VBox(14, summary, rows);
        panel.getStyleClass().add("management-panel");
        return moduleTemplate("Bookings", "Monitor booking and payment records without changing payment internals.", panel);
    }

    private VBox createFirebasePanel() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        VBox list = new VBox(9,
                infoRow("\uE753", "Authentication", "Connected"),
                infoRow("\uE8D7", "Database",
                        overview.firebaseConnected() ? "Connected" : systemStateLabel(overview)),
                infoRow("\uE8C7", "Payments", "Payment foundation configured through existing payment service."),
                infoRow("\uE7C3", "Current Session", isAdminSession() ? "Active" : "Inactive"),
                infoRow("\uE8A5", "Role-based access",
                        "user, business, transport_operator and admin are checked from users/{uid}."),
                infoRow("\uE8FD", "Sync Detail",
                        valueOr("No sync issues reported.", overview.message())));
        VBox panel = new VBox(14, sectionTitle("Application Control Logic"), list);
        panel.getStyleClass().add("management-panel");
        return panel;
    }

    private VBox moduleTemplate(String titleText, String detailText, javafx.scene.Node... body) {
        Label icon = AppUi.symbolIcon(sectionIcon(titleText), "admin-module-title-icon");
        Label title = sectionTitle(titleText);
        Label detail = rowDetail(detailText);
        VBox copy = new VBox(2, title, detail);
        HBox head = new HBox(10, icon, copy);
        head.getStyleClass().add("admin-module-title-row");
        head.setAlignment(Pos.CENTER_LEFT);

        VBox shell = new VBox(14, head);
        shell.getChildren().addAll(body);
        return shell;
    }

    private VBox recentActivityPanel() {
        VBox rows = new VBox(9);
        if (!AppDataStore.pendingApprovals().isEmpty()) {
            for (AppDataStore.ApprovalRequest request : AppDataStore.pendingApprovals().stream().limit(5).toList()) {
                rows.getChildren().add(infoRow("\uE8A7", "Pending " + request.type,
                        request.title + " | " + AppDataStore.displayName(request.targetModule)));
            }
        }
        if (!AppDataStore.bookings().isEmpty()) {
            for (AppDataStore.BookingRecord booking : AppDataStore.bookings().stream().limit(5).toList()) {
                rows.getChildren().add(infoRow("\uE8C7", booking.paymentStatus + " booking",
                        booking.title + " | Rs " + (booking.amountPaise / 100)));
            }
        }
        if (rows.getChildren().isEmpty()) {
            rows.getChildren().add(infoRow("\uE73E", "No recent activity",
                    "Firestore-backed approvals and local booking events will appear here."));
        }
        VBox panel = new VBox(14, sectionTitle("Recent Activity"), rows);
        panel.getStyleClass().add("management-panel");
        return panel;
    }

    private VBox operationsStatusPanel() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        VBox rows = new VBox(9,
                statusRow("Transport", overview.activeRoutes() > 0 ? "Normal" : "No active update"),
                statusRow("Ghats", AppDataStore.items("ghat").isEmpty() ? "No active update" : "Guidance published"),
                statusRow("Emergency", AppDataStore.items("emergency").isEmpty() ? "No active alerts" : "Contacts active"),
                statusRow("Announcements", overview.activeAnnouncements() + " active"),
                infoRow("\uE787", "Updated", currentTimeText()));
        VBox panel = new VBox(14, sectionTitle("Operations Status"), rows);
        panel.getStyleClass().add("management-panel");
        return panel;
    }

    private VBox liveOperationsPreview() {
        AppDataStore.AdminOverview overview = AppDataStore.adminOverview();
        TilePane cards = kpiGrid();
        cards.getChildren().addAll(
                liveOperationCard("Transport", overview.activeRoutes() > 0 ? "Normal" : "No active update",
                        overview.activeRoutes() + " published route(s)", "View Details"),
                liveOperationCard("Ghats", AppDataStore.items("ghat").isEmpty() ? "No active update" : "Guidance published",
                        AppDataStore.items("ghat").size() + " public guidance item(s)", "View Details"),
                liveOperationCard("Emergency", AppDataStore.items("emergency").isEmpty() ? "No active alerts" : "Contacts active",
                        AppDataStore.items("emergency").size() + " verified contact item(s)", "View Details"),
                liveOperationCard("Schedule / Events", overview.activeEvents() > 0 ? "Published" : "No active update",
                        overview.activeEvents() + " active event item(s)", "View Details"));
        return moduleTemplate("Live Operations", "Quick operational signals. Detailed management stays in each module.", cards);
    }

    private VBox liveOperationCard(String title, String status, String detail, String actionText) {
        Label icon = AppUi.symbolIcon(sectionIcon(title), "admin-kpi-icon");
        Label titleLabel = rowTitle(title);
        Label detailLabel = rowDetail(detail);
        Label updated = rowDetail("Last updated: " + currentTimeText());
        Button action = new Button(actionText);
        action.getStyleClass().add("admin-small-action");
        action.setOnAction(event -> showInfo(title, detail));
        HBox top = new HBox(9, icon, titleLabel, AppUi.spacer(), statusChip(status, !"No active update".equals(status)));
        top.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(8, top, detailLabel, updated, action);
        card.getStyleClass().add("management-stat-card");
        return card;
    }

    private HBox statusRow(String title, String status) {
        HBox row = new HBox(10, rowTitle(title), AppUi.spacer(), statusChip(status, !status.contains("No active update")));
        row.getStyleClass().add("management-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox listPanel(String titleText, List<AppDataStore.ServiceItem> items, String emptyText) {
        VBox rows = new VBox(9);
        if (items.isEmpty()) {
            rows.getChildren().add(infoRow("\uE73E", "Empty", emptyText));
        } else {
            for (AppDataStore.ServiceItem item : items) {
                rows.getChildren().add(infoRow("\uE8D4", item.title, item.detail));
            }
        }
        VBox panel = new VBox(14, sectionTitle(titleText), rows);
        panel.getStyleClass().add("management-panel");
        return panel;
    }

    private VBox emptyPanel(String titleText, String detailText) {
        VBox panel = new VBox(9, infoRow("\uE73E", titleText, detailText));
        panel.getStyleClass().add("management-panel");
        return panel;
    }

    private HBox statusBanner(AppDataStore.AdminOverview overview) {
        Label firebase = compactSystemChip(overview);
        Label admin = statusChip("Admin Active", true);
        Label unread = statusChip(AppDataStore.pendingApprovals().size() + " Notifications",
                AppDataStore.pendingApprovals().isEmpty());
        HBox banner = new HBox(10, firebase, admin, unread, AppUi.spacer(),
                rowDetail(overview.firebaseConnected() ? "Operational overview ready." : "Data sync temporarily unavailable."));
        banner.getStyleClass().add("admin-status-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        return banner;
    }

    private HBox syncIssueBanner() {
        Button retry = new Button("\uE72C");
        retry.getStyleClass().add("admin-icon-button");
        retry.setOnAction(event -> refreshAdminData());
        Label text = rowDetail("Some operational data could not be refreshed.");
        HBox banner = new HBox(10, statusChip("Data Sync Issue", false), text, AppUi.spacer(), retry);
        banner.getStyleClass().add("admin-sync-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        return banner;
    }

    private TilePane kpiGrid() {
        TilePane grid = new TilePane();
        grid.getStyleClass().add("admin-kpi-grid");
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPrefColumns(4);
        return grid;
    }

    private VBox kpiCard(String title, long value, String detail) {
        return kpiCard(title, String.valueOf(value), detail, "\uE9D2");
    }

    private VBox kpiCard(String title, String value, String detail) {
        return kpiCard(title, value, detail, "\uE9D2");
    }

    private VBox kpiCard(String title, long value, String detail, String iconText) {
        return kpiCard(title, String.valueOf(value), detail, iconText);
    }

    private VBox kpiCard(String title, String value, String detail, String iconText) {
        Label icon = AppUi.symbolIcon(iconText, "admin-kpi-icon");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("management-stat-value");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("management-stat-title");
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("management-stat-detail");
        detailLabel.setWrapText(true);
        HBox top = new HBox(icon, AppUi.spacer(), valueLabel);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(7, top, titleLabel, detailLabel);
        card.getStyleClass().add("management-stat-card");
        card.setMinWidth(210);
        card.setPrefWidth(240);
        return card;
    }

    private Label statusChip(String text, boolean ok) {
        Label chip = new Label(text);
        chip.getStyleClass().add(ok ? "status-chip-ok" : "status-chip-warn");
        return chip;
    }

    private void refreshApprovals() {
        approvalList.getChildren().clear();
        if (AppDataStore.pendingApprovals().isEmpty()) {
            approvalList.getChildren().add(infoRow("\uE73E", "No pending requests", "New business registrations will appear here."));
            return;
        }

        for (AppDataStore.ApprovalRequest request : new java.util.ArrayList<>(AppDataStore.pendingApprovals())) {
            Button approve = new Button("Approve");
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

            Button reject = new Button("Reject");
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

            Button view = new Button("View Details");
            view.getStyleClass().add("admin-neutral-action");
            view.setOnAction(event -> showInfo(request.title, request.detail));

            VBox text = new VBox(3, rowTitle(request.title),
                    rowDetail("Owner UID: " + valueOr("Available after Firestore refresh", request.ownerId)),
                    rowDetail(request.detail + " | Status: Pending"));
            HBox row = new HBox(12, AppUi.symbolIcon("\uE8A7", "management-row-icon"), text, AppUi.spacer(), view,
                    approve, reject);
            row.getStyleClass().add("management-row");
            row.setAlignment(Pos.CENTER_LEFT);
            approvalList.getChildren().add(row);
        }
    }

    private void refreshDataList() {
        if (dataList == null || moduleSelect == null) {
            return;
        }
        dataList.getChildren().clear();
        String module = moduleSelect.getValue();
        List<AppDataStore.ServiceItem> items = AppDataStore.items(module);
        if (items.isEmpty()) {
            dataList.getChildren().add(infoRow("\uE73E", "No published items",
                    "Use the form above to publish the first item for this module."));
            return;
        }
        for (AppDataStore.ServiceItem item : new java.util.ArrayList<>(items)) {
            Button remove = new Button("Remove");
            remove.getStyleClass().add("text-button");
            remove.setOnAction(event -> {
                AppDataStore.removeItem(module, item);
                refreshDataList();
                refreshAdminData();
            });

            HBox row = new HBox(12, AppUi.symbolIcon("\uE8D4", "management-row-icon"),
                    new VBox(2, rowTitle(item.title), rowDetail(item.detail)), AppUi.spacer(), remove);
            row.getStyleClass().add("management-row");
            row.setAlignment(Pos.CENTER_LEFT);
            dataList.getChildren().add(row);
        }
    }

    private HBox infoRow(String iconText, String title, String detail) {
        HBox row = new HBox(12, AppUi.symbolIcon(iconText, "management-row-icon"),
                new VBox(2, rowTitle(title), rowDetail(detail)));
        row.getStyleClass().add("management-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("management-section-title");
        return label;
    }

    private Label rowTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("management-row-title");
        label.setWrapText(true);
        return label;
    }

    private Label rowDetail(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("management-row-detail");
        label.setWrapText(true);
        return label;
    }

    private Label compactSystemChip(AppDataStore.AdminOverview overview) {
        return statusChip(systemStateLabel(overview), overview.firebaseConnected());
    }

    private String systemStateLabel(AppDataStore.AdminOverview overview) {
        if (overview.firebaseConnected()) {
            return "System Online";
        }
        String message = overview.message() == null ? "" : overview.message().toLowerCase();
        if (message.contains("permission") || message.contains("access")) {
            return "Data Sync Issue";
        }
        if (message.contains("configured") || message.contains("offline")) {
            return "Offline Mode";
        }
        return "Data Sync Issue";
    }

    private String sectionIcon(String section) {
        return switch (section) {
            case "Overview" -> "\uE80F";
            case "Live Operations" -> "\uE7C1";
            case "Users" -> "\uE716";
            case "Business Approvals" -> "\uE7BA";
            case "Businesses" -> "\uE719";
            case "Bookings" -> "\uE8A7";
            case "Transport" -> "\uE806";
            case "Transport Operators" -> "\uE8EC";
            case "Puja Services" -> "\uEC29";
            case "Ghats & Snan" -> "\uE707";
            case "Stay" -> "\uE809";
            case "Lost & Found" -> "\uE721";
            case "Schedule & Events" -> "\uE787";
            case "Announcements" -> "\uE789";
            case "Emergency" -> "\uE95E";
            case "Reports & Analytics" -> "\uE9D2";
            case "System" -> "\uE713";
            default -> "\uE8A5";
        };
    }

    private ImageView createImage(String path, double width, double height) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        java.net.URL imageUrl = getClass().getResource(path);
        if (imageUrl != null) {
            imageView.setImage(new Image(imageUrl.toExternalForm()));
        }
        return imageView;
    }

    private String currentTimeText() {
        return LocalDateTime.now().format(ADMIN_TIME);
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
}
