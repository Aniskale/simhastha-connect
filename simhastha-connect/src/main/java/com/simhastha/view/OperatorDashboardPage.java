package com.simhastha.view;

import com.simhastha.util.AppSession;
import com.simhastha.util.NavigationUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OperatorDashboardPage {

    private final OperatorAuthPage.OperatorAccount account;
    private final Set<String> readNotifications = new java.util.HashSet<>();
    private BorderPane page;
    private VBox scheduleList;

    public OperatorDashboardPage(OperatorAuthPage.OperatorAccount account) {
        this.account = account;
    }

    public Scene createScene(Stage stage) {
        page = new BorderPane();
        page.getStyleClass().add("management-page");
        page.setTop(createHeader(stage));
        page.setCenter(createScrollableDashboard());

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        return AppUi.createScene(root, this);
    }

    private HBox createHeader(Stage stage) {
        Label brand = new Label("SIMHASTHA CONNECT");
        brand.getStyleClass().add("page-brand");

        Label title = new Label("Transport Management");
        title.getStyleClass().add("page-heading");

        Label subtitle = new Label(account.organizationName + " • " + account.serviceType + " • " + account.contactPerson);
        subtitle.getStyleClass().add("page-subtitle");

        VBox titleBox = new VBox(3, brand, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Button logout = new Button("Logout");
        logout.getStyleClass().add("back-button");
        logout.setOnAction(event -> {
            AppSession.clear();
            OperatorAuthPage authPage = new OperatorAuthPage();
            NavigationUtil.navigate(stage, authPage.createScene(stage));
        });

        HBox header = new HBox(18, titleBox, AppUi.spacer(), AppUi.createThemeToggle(), notificationBell(),
                AppUi.createProfileChip(account.contactPerson,
                        () -> AppUi.showInfo("Transport Profile",
                                account.contactPerson + "\n" + account.organizationName,
                                page.getScene() == null ? null : page.getScene().getWindow())),
                logout);
        header.getStyleClass().add("management-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 42, 10, 42));
        return header;
    }

    private javafx.scene.layout.StackPane notificationBell() {
        List<NotificationCenter.NotificationItem> items = operatorNotifications();
        int unread = (int) items.stream().filter(item -> !readNotifications.contains(item.id())).count();
        return NotificationCenter.bell(unread, this::showNotificationDrawer);
    }

    private void showNotificationDrawer() {
        NotificationCenter.show(page.getScene() == null ? null : page.getScene().getWindow(),
                "Transport Notifications", "Seat requests, route status and operations",
                operatorNotifications(), readNotifications, this::openNotificationTarget);
    }

    private void openNotificationTarget(String target) {
        page.setCenter(createScrollableDashboard());
    }

    private List<NotificationCenter.NotificationItem> operatorNotifications() {
        List<NotificationCenter.NotificationItem> items = new ArrayList<>();
        for (String booking : account.bookings) {
            items.add(new NotificationCenter.NotificationItem("operator-booking-" + booking.hashCode(),
                    "Seat request update", booking, "Booking", "pending", "bookings"));
        }
        for (String schedule : account.schedules) {
            items.add(new NotificationCenter.NotificationItem("operator-schedule-" + schedule.hashCode(),
                    "Route schedule active", schedule, "Schedule", "confirmed", "schedules"));
        }
        AppSession.User user = AppSession.currentUser();
        String operatorId = user == null || user.uid() == null || user.uid().isBlank() ? account.email : user.uid();
        AppDataStore.transportRoutes().stream()
                .filter(route -> operatorId.equals(route.operatorId))
                .forEach(route -> items.add(new NotificationCenter.NotificationItem("operator-route-" + route.routeId,
                        route.published ? "Route published" : "Route awaiting admin publish",
                        route.routeName + " | " + route.via, "Route", route.published ? "confirmed" : "pending",
                        "routes")));
        items.add(new NotificationCenter.NotificationItem("operator-delay-guidance",
                "Delay alerts ready", "Use transport operations to notify pilgrims about traffic or crowd impact.",
                "Operations", "info", "operations"));
        return items.stream().limit(12).toList();
    }

    private ScrollPane createScrollableDashboard() {
        ScrollPane scroll = new ScrollPane(createDashboardContent());
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox createDashboardContent() {
        HBox stats = new HBox(14,
                statCard("Bookings", "16", "Seat and group requests"),
                statCard("Schedules", String.valueOf(account.schedules.size()), "Active transport routes"),
                statCard("Occupancy", "74%", "Average booked seats"),
                statCard("Toll + Fuel", "Rs 8.7k", "Today's route estimate"));
        stats.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.add(createBookingPanel(), 0, 0);
        grid.add(createSchedulePanel(), 1, 0);
        grid.add(createRouteAnalyticsPanel(), 0, 1);
        grid.add(createOperationsPanel(), 1, 1);

        VBox content = new VBox(18, stats, grid);
        content.setPadding(new Insets(16, 42, 42, 42));
        content.setAlignment(Pos.TOP_CENTER);
        return content;
    }

    private VBox createBookingPanel() {
        VBox list = new VBox(10);
        for (String booking : account.bookings) {
            list.getChildren().add(infoRow("\uE8A7", booking, "Monitor requests and prepare route response"));
        }

        VBox panel = new VBox(14, sectionTitle("Booking & Seat Requests"), list);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(500);
        panel.setMinHeight(330);
        return panel;
    }

    private VBox createRouteAnalyticsPanel() {
        VBox list = new VBox(10,
                metricRow("Route Occupancy", "74%", 0.74, "Seat fill across active schedules"),
                metricRow("On-time Performance", "91%", 0.91, "Departures within planned window"),
                metricRow("Demand Hotspot", "Ramkund", 0.86, "Highest pickup and drop request zone"),
                infoRow("\uE8B7", "Peak movement: 5 AM - 9 AM", "Add extra shuttle or temporary trip if demand rises"));

        VBox panel = new VBox(14, sectionTitle("Route Analytics"), list);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(500);
        panel.setMinHeight(300);
        return panel;
    }

    private VBox createOperationsPanel() {
        VBox list = new VBox(10,
                infoRow("\uE7C1", "Toll & fuel tracker", "Estimate toll, diesel/CNG cost and route margin"),
                infoRow("\uE8FD", "Driver and vehicle readiness", "Mark driver, permit, fitness and emergency kit status"),
                infoRow("\uE8B5", "Fare slab control", "Set adult, child, group and return-trip fare"),
                infoRow("\uE8A1", "Delay alerts", "Notify pilgrims when traffic or ghat crowding affects timing"));

        VBox panel = new VBox(14, sectionTitle("Transport Operations"), list);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(560);
        panel.setMinHeight(300);
        return panel;
    }

    private VBox createSchedulePanel() {
        scheduleList = new VBox(9);
        refreshSchedules();

        TextField scheduleInput = AppUi.textField("Add bus, route, time or service note");
        Button addButton = new Button("ADD");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> {
            String value = scheduleInput.getText().trim();
            if (!value.isEmpty()) {
                addButton.setDisable(true);
                account.schedules.add(value);
                AppSession.User user = AppSession.currentUser();
                try {
                    AppDataStore.saveRoute(new AppDataStore.RouteRecord(
                            "route-" + UUID.randomUUID().toString().substring(0, 8),
                            account.organizationName + " - " + value,
                            "", "", value, account.serviceType, "", "", "", "",
                            user == null ? account.email : user.uid(), false, "", "",
                            false, true, "", ""));
                } catch (AppDataStore.ApprovalUpdateException exception) {
                    AppDataStore.requestApproval("Transport Schedule",
                            account.organizationName + " - " + value,
                            account.serviceType + " | Contact: " + account.mobile,
                            "transport");
                }
                scheduleInput.clear();
                refreshSchedules();
                addButton.setDisable(false);
            }
        });

        HBox addRow = new HBox(10, scheduleInput, addButton);
        HBox.setHgrow(scheduleInput, Priority.ALWAYS);

        VBox panel = new VBox(14, sectionTitle("Manage Buses & Schedules"), scheduleList, addRow);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(560);
        panel.setMinHeight(330);
        return panel;
    }

    private void refreshSchedules() {
        scheduleList.getChildren().clear();
        for (String schedule : account.schedules) {
            Button remove = new Button("Remove");
            remove.getStyleClass().add("text-button");
            remove.setOnAction(event -> {
                account.schedules.remove(schedule);
                refreshSchedules();
            });

            Label label = new Label(schedule);
            label.getStyleClass().add("management-row-title");
            label.setWrapText(true);

            HBox row = new HBox(12, AppUi.symbolIcon("\uE806", "management-row-icon"), label, AppUi.spacer(), remove);
            row.getStyleClass().add("management-row");
            row.setAlignment(Pos.CENTER_LEFT);
            scheduleList.getChildren().add(row);
        }
    }

    private VBox statCard(String title, String value, String detail) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("management-stat-value");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("management-stat-title");

        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("management-stat-detail");
        detailLabel.setWrapText(true);

        VBox card = new VBox(5, valueLabel, titleLabel, detailLabel);
        card.getStyleClass().add("management-stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox infoRow(String iconText, String title, String detail) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("management-row-title");
        titleLabel.setWrapText(true);
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("management-row-detail");
        detailLabel.setWrapText(true);
        VBox text = new VBox(2, titleLabel, detailLabel);
        HBox row = new HBox(12, AppUi.symbolIcon(iconText, "management-row-icon"), text);
        row.getStyleClass().add("management-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox metricRow(String title, String value, double progress, String detail) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("management-metric-value");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("management-row-title");

        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("management-row-detail");
        detailLabel.setWrapText(true);

        javafx.scene.layout.Region track = new javafx.scene.layout.Region();
        track.getStyleClass().add("management-progress-track");
        track.setPrefWidth(170);
        track.setMaxWidth(170);

        javafx.scene.layout.Region fill = new javafx.scene.layout.Region();
        fill.getStyleClass().add("management-progress-fill");
        fill.setPrefWidth(Math.max(24, 170 * progress));

        javafx.scene.layout.StackPane progressBar = new javafx.scene.layout.StackPane(track, fill);
        progressBar.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(2, titleLabel, detailLabel);
        HBox row = new HBox(12, text, AppUi.spacer(), progressBar, valueLabel);
        row.getStyleClass().add("management-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("management-section-title");
        return label;
    }
}
