package com.simhastha.view;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class DashboardPage {

    public Scene createScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-background");

        root.setLeft(createSidebar(stage));
        root.setCenter(createMainDashboard());

        Scene scene = new Scene(root, 1200, 750);
        addTheme(scene);
        return scene;
    }

    private VBox createSidebar(Stage stage) {
        Label logo = new Label("\u0938\u093F\u0902\u0939\u0938\u094D\u0925\n2027");
        logo.getStyleClass().add("sidebar-logo");

        VBox menu = new VBox(8);
        menu.getChildren().addAll(
                createNavButton("Dashboard", true),
                createNavButton("Transport Routes", false),
                createNavButton("Daily Kumbh Schedule", false),
                createNavButton("Snan & Important Ghats", false),
                createNavButton("Stay Booking Help", false),
                createNavButton("Puja Services", false),
                createNavButton("Local Business", false),
                createNavButton("Emergency Help", false),
                createNavButton("Lost & Found", false),
                createNavButton("Help / About", false));

        Button logout = new Button("Logout");
        logout.getStyleClass().add("sidebar-logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(event -> {
            UserAuthPage userAuthPage = new UserAuthPage();
            stage.setScene(userAuthPage.createScene(stage));
        });

        VBox sidebar = new VBox(18, logo, menu, createSpacer(), logout);
        sidebar.getStyleClass().add("dashboard-sidebar");
        sidebar.setPadding(new Insets(22, 14, 18, 14));
        sidebar.setPrefWidth(230);
        return sidebar;
    }

    private Button createNavButton(String text, boolean active) {
        Button button = new Button(text);
        button.getStyleClass().add(active ? "sidebar-button-active" : "sidebar-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> showComingSoon(text));
        return button;
    }

    private VBox createMainDashboard() {
        Label title = new Label("SIMHASTHA CONNECT");
        title.getStyleClass().add("dashboard-title");

        Label subtitle = new Label("Nashik Simhastha Kumbh 2027");
        subtitle.getStyleClass().add("dashboard-subtitle");

        Label welcome = new Label("Welcome, User");
        welcome.getStyleClass().add("dashboard-user");

        HBox topBar = new HBox(20, new VBox(2, title, subtitle), createSpacer(), welcome);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("dashboard-topbar");

        StackPane hero = createHeroBanner();
        HBox stats = createStatsRow();
        GridPane modules = createModuleGrid();

        VBox leftContent = new VBox(16, topBar, hero, stats, modules);
        HBox.setHgrow(leftContent, Priority.ALWAYS);

        VBox rightPanel = createRightPanel();

        HBox body = new HBox(18, leftContent, rightPanel);
        body.setPadding(new Insets(22));
        HBox.setHgrow(leftContent, Priority.ALWAYS);

        VBox page = new VBox(body, createBottomNavigation());
        VBox.setVgrow(body, Priority.ALWAYS);
        return page;
    }

    private StackPane createHeroBanner() {
        ImageView bannerImage = createOptionalImage("/images/kumbh-background.jpg", 760, 170);
        bannerImage.getStyleClass().add("dashboard-banner-image");

        Label lineOne = new Label("|| Har Har Mahadev ||");
        lineOne.getStyleClass().add("dashboard-hero-small");

        Label lineTwo = new Label("Nashik Simhastha Kumbh 2027");
        lineTwo.getStyleClass().add("dashboard-hero-title");

        Label lineThree = new Label("Aaple swagat aahe");
        lineThree.getStyleClass().add("dashboard-hero-small");

        VBox text = new VBox(6, lineOne, lineTwo, lineThree);
        text.setAlignment(Pos.CENTER);

        VBox dateBadge = createDateBadge();
        StackPane.setAlignment(dateBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(dateBadge, new Insets(18, 22, 0, 0));

        StackPane hero = new StackPane(bannerImage, createTempleLine(), text, dateBadge);
        hero.getStyleClass().add("dashboard-hero");
        hero.setMinHeight(190);
        return hero;
    }

    private VBox createDateBadge() {
        Label day = new Label("21");
        day.getStyleClass().add("dashboard-date-day");

        Label month = new Label("May 2027");
        month.getStyleClass().add("dashboard-date-text");

        Label place = new Label("Shukla Paksha");
        place.getStyleClass().add("dashboard-date-small");

        VBox badge = new VBox(3, day, month, place);
        badge.getStyleClass().add("dashboard-date-badge");
        badge.setAlignment(Pos.CENTER);
        return badge;
    }

    private HBox createTempleLine() {
        Line left = new Line(0, 0, 150, 0);
        left.getStyleClass().add("dashboard-ornament-line");

        Circle dot = new Circle(5);
        dot.getStyleClass().add("dashboard-ornament-dot");

        Line right = new Line(0, 0, 150, 0);
        right.getStyleClass().add("dashboard-ornament-line");

        HBox box = new HBox(14, left, dot, right);
        box.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(box, Pos.BOTTOM_CENTER);
        StackPane.setMargin(box, new Insets(0, 0, 20, 0));
        return box;
    }

    private GridPane createModuleGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);

        List<DashboardModule> modules = loadDashboardModules();

        for (int i = 0; i < modules.size(); i++) {
            DashboardModule module = modules.get(i);
            VBox card = createDashboardCard(module);
            grid.add(card, i % 4, i / 4);
        }

        return grid;
    }

    private List<DashboardModule> loadDashboardModules() {
        List<DashboardModule> modules = new ArrayList<>();
        modules.add(new DashboardModule("BUS", "Transport Routes", "Buses, routes, timings, fare"));
        modules.add(new DashboardModule("CAL", "Kumbh Schedule", "Snan, aarti, events, dates"));
        modules.add(new DashboardModule("GHAT", "Snan & Ghats", "Ramkund, crowd level, maps"));
        modules.add(new DashboardModule("STAY", "Stay Finder", "Hotels, dharmashala, camps"));
        modules.add(new DashboardModule("PUJA", "Puja Services", "Ritual services and booking help"));
        modules.add(new DashboardModule("SHOP", "Local Business", "Food, shops, local services"));
        modules.add(new DashboardModule("SOS", "Emergency Help", "Police, ambulance, help center"));
        modules.add(new DashboardModule("FIND", "Lost & Found", "Report lost or found items"));
        return modules;
    }

    private HBox createStatsRow() {
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
                createStatCard("Live Modules", "8"),
                createStatCard("Emergency", "24x7"),
                createStatCard("Main Focus", "Transport"),
                createStatCard("Festival", "2027"));
        return stats;
    }

    private VBox createStatCard(String labelText, String valueText) {
        Label value = new Label(valueText);
        value.getStyleClass().add("dashboard-stat-value");

        Label label = new Label(labelText);
        label.getStyleClass().add("dashboard-stat-label");

        VBox card = new VBox(4, value, label);
        card.getStyleClass().add("dashboard-stat-card");
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createDashboardCard(DashboardModule module) {
        Label icon = new Label(module.shortName);
        icon.getStyleClass().add("dashboard-card-icon");

        Label title = new Label(module.title);
        title.getStyleClass().add("dashboard-card-title");
        title.setWrapText(true);

        Label detail = new Label(module.description);
        detail.getStyleClass().add("dashboard-card-detail");
        detail.setWrapText(true);

        VBox card = new VBox(9, icon, title, detail);
        card.getStyleClass().add("dashboard-card");
        card.setAlignment(Pos.CENTER);
        card.setMinSize(155, 132);
        card.setOnMouseClicked(event -> showComingSoon(module.title));
        return card;
    }

    private static class DashboardModule {
        private String shortName;
        private String title;
        private String description;

        private DashboardModule(String shortName, String title, String description) {
            this.shortName = shortName;
            this.title = title;
            this.description = description;
        }
    }

    private VBox createRightPanel() {
        VBox highlights = createInfoBox("Today Highlights",
                "Brahma Muhurta Snan - 04:00 AM",
                "Sandhya Aarti - 05:00 PM",
                "Cultural Program - 06:30 PM");

        VBox emergency = createInfoBox("Emergency Contacts",
                "Control Room: 0253-XXXXXXX",
                "Ambulance: 108",
                "Police: 100",
                "Fire Brigade: 101");

        VBox weather = createInfoBox("Weather Update",
                "Temperature: 28 C",
                "Condition: Partly Cloudy",
                "Wind Speed: 12 km/h");

        VBox panel = new VBox(15, highlights, emergency, weather);
        panel.setPrefWidth(310);
        return panel;
    }

    private HBox createBottomNavigation() {
        HBox bottomNav = new HBox(42,
                createBottomItem("Dashboard"),
                createBottomItem("Search"),
                createBottomItem("Schedule"),
                createBottomItem("Updates"),
                createBottomItem("My Account"));
        bottomNav.getStyleClass().add("dashboard-bottom-nav");
        bottomNav.setAlignment(Pos.CENTER);
        bottomNav.setPadding(new Insets(12, 22, 14, 22));
        return bottomNav;
    }

    private Button createBottomItem(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("dashboard-bottom-button");
        button.setOnAction(event -> showComingSoon(text));
        return button;
    }

    private VBox createInfoBox(String titleText, String... lines) {
        Label title = new Label(titleText);
        title.getStyleClass().add("dashboard-info-title");

        VBox box = new VBox(9, title);
        box.getStyleClass().add("dashboard-info-box");

        for (String line : lines) {
            Label label = new Label(line);
            label.getStyleClass().add("dashboard-info-line");
            label.setWrapText(true);
            box.getChildren().add(label);
        }

        return box;
    }

    private javafx.scene.layout.Region createSpacer() {
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private ImageView createOptionalImage(String path, double width, double height) {
        URL imageUrl = getClass().getResource(path);
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);

        if (imageUrl != null) {
            imageView.setImage(new Image(imageUrl.toExternalForm()));
        }

        return imageView;
    }

    private void showComingSoon(String moduleName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(moduleName);
        alert.setHeaderText(null);
        alert.setContentText(moduleName + " module will be added next.");
        alert.showAndWait();
    }

    private void addTheme(Scene scene) {
        URL cssUrl = getClass().getResource("/css/simhastha-theme.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }
}
