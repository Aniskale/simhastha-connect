package com.simhastha.view;

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

public class BusinessOwnerDashboardPage {

    private final BusinessAuthPage.BusinessAccount account;
    private VBox serviceList;

    public BusinessOwnerDashboardPage(BusinessAuthPage.BusinessAccount account) {
        this.account = account;
    }

    public Scene createScene(Stage stage) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("management-page");
        page.setTop(createHeader(stage));
        page.setCenter(createScrollableDashboard());

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        return AppUi.createScene(root, this);
    }

    private HBox createHeader(Stage stage) {
        Label brand = new Label("SIMHASTHA CONNECT");
        brand.getStyleClass().add("page-brand");

        Label title = new Label("Business Management");
        title.getStyleClass().add("page-heading");

        Label subtitle = new Label(account.businessName + " • " + account.category + " • " + account.location);
        subtitle.getStyleClass().add("page-subtitle");

        VBox titleBox = new VBox(3, brand, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Button logout = new Button("Logout");
        logout.getStyleClass().add("back-button");
        logout.setOnAction(event -> {
            AppSession.clear();
            BusinessAuthPage authPage = new BusinessAuthPage();
            stage.setScene(authPage.createScene(stage));
        });

        HBox header = new HBox(18, titleBox, AppUi.spacer(), AppUi.createThemeToggle(), logout);
        header.getStyleClass().add("management-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 42, 10, 42));
        return header;
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
                statCard("Bookings", "3", "New and confirmed requests"),
                statCard("Active Services", String.valueOf(account.services.size()), "Visible to pilgrims"),
                statCard("Today's Reach", "248", "Pilgrim profile views"),
                statCard("Revenue Leads", "Rs 18.4k", "Estimated open value"));
        stats.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.add(createBookingsPanel(), 0, 0);
        grid.add(createServicesPanel(), 1, 0);
        grid.add(createAnalyticsPanel(), 0, 1);
        grid.add(createOwnerToolsPanel(), 1, 1);

        VBox content = new VBox(18, stats, grid);
        content.setPadding(new Insets(16, 42, 42, 42));
        content.setAlignment(Pos.TOP_CENTER);
        return content;
    }

    private VBox createBookingsPanel() {
        VBox list = new VBox(10);
        for (String booking : account.bookings) {
            list.getChildren().add(infoRow("\uE8A7", booking, "Track and respond from this dashboard"));
        }

        VBox panel = new VBox(14, sectionTitle("Recent Booking Activity"), list);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(500);
        panel.setMinHeight(330);
        return panel;
    }

    private VBox createAnalyticsPanel() {
        VBox list = new VBox(10,
                metricRow("Inquiry Conversion", "68%", 0.68, "Pending to confirmed request ratio"),
                metricRow("Peak Demand", "6 PM - 9 PM", 0.82, "Best slot for more staff and stock"),
                metricRow("Top Listing", account.category, 0.74, "Most viewed business category"),
                infoRow("\uE8A5", "Repeat pilgrim interest: 34%", "Good signal for packages and combos"));

        VBox panel = new VBox(14, sectionTitle("Business Analytics"), list);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(500);
        panel.setMinHeight(300);
        return panel;
    }

    private VBox createOwnerToolsPanel() {
        VBox list = new VBox(10,
                infoRow("\uE8C7", "Offer & pricing control", "Update festival rates, combos and availability"),
                infoRow("\uE7BA", "Stock / room readiness", "Mark food stock, rooms or puja slots as limited"),
                infoRow("\uE8F9", "Service area visibility", "Show exact ghat, road or landmark coverage"),
                infoRow("\uE9D2", "Reviews & trust score", "Track ratings and owner response quality"));

        VBox panel = new VBox(14, sectionTitle("Owner Growth Tools"), list);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(560);
        panel.setMinHeight(300);
        return panel;
    }

    private VBox createServicesPanel() {
        serviceList = new VBox(9);
        refreshServices();

        TextField serviceInput = AppUi.textField("Add service, product, room, package or offer");
        Button addButton = new Button("ADD");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> {
            String value = serviceInput.getText().trim();
            if (!value.isEmpty()) {
                account.services.add(value);
                AppDataStore.requestApproval("Business Listing",
                        account.businessName + " - " + value,
                        account.category + " | " + account.location + " | " + account.mobile,
                        "business");
                serviceInput.clear();
                refreshServices();
            }
        });

        HBox addRow = new HBox(10, serviceInput, addButton);
        HBox.setHgrow(serviceInput, Priority.ALWAYS);

        VBox panel = new VBox(14, sectionTitle("Manage Business Listings"), serviceList, addRow);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(560);
        panel.setMinHeight(330);
        return panel;
    }

    private void refreshServices() {
        serviceList.getChildren().clear();
        for (String service : account.services) {
            Button remove = new Button("Remove");
            remove.getStyleClass().add("text-button");
            remove.setOnAction(event -> {
                account.services.remove(service);
                refreshServices();
            });

            Label label = new Label(service);
            label.getStyleClass().add("management-row-title");
            label.setWrapText(true);

            HBox row = new HBox(12, AppUi.symbolIcon("\uE8D4", "management-row-icon"), label, AppUi.spacer(), remove);
            row.getStyleClass().add("management-row");
            row.setAlignment(Pos.CENTER_LEFT);
            serviceList.getChildren().add(row);
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
