package com.simhastha.view;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class WelcomePage {

    private ImageView backgroundImage;
    private Image sunlightImage;
    private Image nightImage;
    private Label dateLabel;
    private Label timeLabel;
    private Button lightModeButton;
    private Button darkModeButton;
    private StackPane root;

    public Scene createScene(Stage stage) {
        root = new StackPane();
        root.getStyleClass().add("welcome-root");

        sunlightImage = loadImage("/images/welcome-light.png");
        nightImage = loadImage("/images/welcome-dark.png");
        backgroundImage = createCoverImageView(ThemeManager.isDark() ? nightImage : sunlightImage);
        backgroundImage.getStyleClass().add("welcome-background-image");

        BorderPane page = new BorderPane();
        page.getStyleClass().add("welcome-page");
        page.setTop(createHeader());
        page.setCenter(scroll(createMainContent(stage)));

        root.getChildren().addAll(backgroundImage, createHeroOverlay(), page);

        Scene scene = new Scene(root, 1200, 680);
        bindCoverImage();
        addTheme(scene);
        ThemeManager.addListener(() -> {
            ThemeManager.applyTo(root);
            backgroundImage.setImage(ThemeManager.isDark() ? nightImage : sunlightImage);
            updateThemeButtons();
            updateImageViewport();
        });
        startClock();
        updateThemeButtons();
        playTitleAnimation(page);
        return scene;
    }

    private BorderPane createHeader() {
        BorderPane header = new BorderPane();
        header.getStyleClass().add("welcome-header");

        HBox brand = new HBox(14, createLogoMark(), createBrandText());
        brand.getStyleClass().add("welcome-brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        HBox controls = createTopUtilityBar();
        controls.setAlignment(Pos.TOP_RIGHT);

        header.setLeft(brand);
        header.setRight(controls);
        header.setPadding(new Insets(18, 30, 0, 36));
        return header;
    }

    private StackPane createLogoMark() {
        ImageView mark = new ImageView(loadImage("/images/sclogo.png"));
        mark.setPreserveRatio(true);
        mark.setSmooth(true);
        mark.setFitWidth(74);
        mark.setFitHeight(74);
        mark.getStyleClass().add("welcome-logo-image");

        StackPane logo = new StackPane(mark);
        logo.getStyleClass().add("welcome-logo-shell");
        logo.setMinSize(78, 78);
        logo.setMaxSize(78, 78);
        return logo;
    }

    private VBox createBrandText() {
        Label name = new Label("SIMHASTHA");
        name.getStyleClass().add("welcome-brand-name");

        Label connect = new Label("CONNECT");
        connect.getStyleClass().add("welcome-brand-connect");

        Label event = new Label("Nashik Simhastha 2027");
        event.getStyleClass().add("welcome-brand-event");

        VBox text = new VBox(0, name, connect, event);
        text.setAlignment(Pos.CENTER_LEFT);
        return text;
    }

    private HBox createThemeToggle() {
        lightModeButton = new Button();
        darkModeButton = new Button();
        lightModeButton.setGraphic(createSymbolIcon("\uE706", "theme-toggle-icon"));
        darkModeButton.setGraphic(createSymbolIcon("\uE708", "theme-toggle-icon"));
        lightModeButton.getStyleClass().add("theme-toggle-button");
        darkModeButton.getStyleClass().add("theme-toggle-button");
        lightModeButton.setAccessibleText("Light theme");
        darkModeButton.setAccessibleText("Dark theme");

        lightModeButton.setOnAction(event -> switchTheme(false));
        darkModeButton.setOnAction(event -> switchTheme(true));

        HBox toggle = new HBox(4, lightModeButton, darkModeButton);
        toggle.getStyleClass().add("theme-toggle");
        return toggle;
    }

    private HBox createTopUtilityBar() {
        dateLabel = new Label();
        timeLabel = new Label();
        Label location = new Label("Nashik,\nMaharashtra");
        dateLabel.getStyleClass().add("utility-text");
        timeLabel.getStyleClass().add("utility-text");
        location.getStyleClass().add("utility-text");

        HBox bar = new HBox(18,
                createUtilityItem("\uE787", dateLabel),
                createUtilitySeparator(),
                createUtilityItem("\uE121", timeLabel),
                createUtilitySeparator(),
                createUtilityItem("\uE81D", location),
                createUtilitySeparator(),
                createThemeToggle(),
                createTopIconButton(AppUi.notificationBellGlyph(), "3", "Notifications"),
                createTopIconButton("\uE77B", null, "Profile"));
        bar.getStyleClass().add("utility-bar");
        bar.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    private HBox createUtilityItem(String iconText, Label value) {
        Label icon = createSymbolIcon(iconText, "utility-icon");

        HBox item = new HBox(9, icon, value);
        item.getStyleClass().add("utility-item");
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private Region createUtilitySeparator() {
        Region separator = new Region();
        separator.getStyleClass().add("utility-separator");
        separator.setMinSize(1, 42);
        separator.setPrefSize(1, 42);
        separator.setMaxSize(1, 42);
        return separator;
    }

    private Button createTopIconButton(String iconText, String badgeText, String accessibleText) {
        Label icon = createSymbolIcon(iconText, "top-action-icon");

        StackPane graphic = new StackPane(icon);
        if (badgeText != null) {
            Label badge = new Label(badgeText);
            badge.getStyleClass().add("notification-badge");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(-7, -7, 0, 0));
            graphic.getChildren().add(badge);
        }

        Button button = new Button();
        button.setGraphic(graphic);
        button.setAccessibleText(accessibleText);
        button.getStyleClass().add("top-action-button");
        return button;
    }

    private VBox createMainContent(Stage stage) {
        HBox hero = createHeroContent(stage);
        HBox sectionTitle = createSectionTitle("EXPLORE WHAT WE OFFER");
        FlowPane modules = createModuleStrip(stage);
        HBox updates = createUpdatesSection(stage);
        HBox trust = createTrustStrip();

        VBox content = new VBox(14, hero, sectionTitle, modules, updates, trust);
        content.getStyleClass().add("welcome-main-content");
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(8, 26, 10, 26));
        return content;
    }

    private HBox createHeroContent(Stage stage) {
        Label welcome = new Label("Welcome to");
        welcome.getStyleClass().add("welcome-kicker");

        Label titleTop = new Label("SIMHASTHA");
        titleTop.getStyleClass().add("welcome-title-main");

        Label titleAccent = new Label("CONNECT");
        titleAccent.getStyleClass().add("welcome-title-accent");

        Label description = new Label("Your Digital Companion for a Safe,\nSmooth & Divine Simhastha Experience");
        description.getStyleClass().add("welcome-description");
        description.setWrapText(true);
        description.setMaxWidth(380);

        Button exploreButton = new Button("EXPLORE SIMHASTHA");
        exploreButton.getStyleClass().add("primary-cta");
        exploreButton.setGraphic(createSymbolIcon("\uE734", "cta-icon"));
        exploreButton.setContentDisplay(ContentDisplay.LEFT);
        exploreButton.setGraphicTextGap(10);
        exploreButton.setOnAction(event -> openLoginSelection(stage));

        Button loginButton = new Button("LOGIN / SIGN UP");
        loginButton.getStyleClass().add("secondary-cta");
        loginButton.setGraphic(createSymbolIcon("\uE77B", "cta-icon"));
        loginButton.setContentDisplay(ContentDisplay.LEFT);
        loginButton.setGraphicTextGap(10);
        loginButton.setOnAction(event -> openLoginSelection(stage));

        HBox actions = new HBox(18, exploreButton, loginButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(7, welcome, titleTop, titleAccent, createDecorativeDivider(), description, actions);
        copy.getStyleClass().add("welcome-copy");
        copy.setAlignment(Pos.CENTER_LEFT);

        HBox hero = new HBox(copy, createFlexibleSpace());
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(0, 24, 0, 24));
        return hero;
    }

    private HBox createSectionTitle(String titleText) {
        Line left = new Line(0, 0, 170, 0);
        left.getStyleClass().add("welcome-section-line");

        Label title = new Label(titleText);
        title.getStyleClass().add("welcome-section-title");

        Line right = new Line(0, 0, 170, 0);
        right.getStyleClass().add("welcome-section-line");

        HBox titleRow = new HBox(16, left, title, right);
        titleRow.setAlignment(Pos.CENTER);
        return titleRow;
    }

    private FlowPane createModuleStrip(Stage stage) {
        FlowPane modules = new FlowPane(12, 12,
                createModuleCard(stage, "\uE806", "Transport", "Routes, Timings &\nTravel Information"),
                createModuleCard(stage, "\uEC29", "Puja Services", "Book Puja, Pandit &\nReligious Services"),
                createModuleCard(stage, "\uE9A6", "Ghats & Snan", "Ghat Information &\nSnan Guide"),
                createModuleCard(stage, "\uE809", "Stay", "Hotels, Dharamshalas\n& Accommodation"),
                createModuleCard(stage, "\uE787", "Events", "Aarti, Shahi Snan &\nUpcoming Events"),
                createModuleCard(stage, "\uE95E", "Emergency", "Hospitals, Police &\nEmergency Help"),
                createModuleCard(stage, "\uE721", "Lost & Found", "Report or Search\nLost Items"),
                createModuleCard(stage, "\uE8D4", "Food", "Food, Prasadam &\nNearby Restaurants"));
        modules.setAlignment(Pos.CENTER);
        return modules;
    }

    private Button createModuleCard(Stage stage, String iconText, String titleText, String detailText) {
        Label icon = createSymbolIcon(iconText, "welcome-module-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("welcome-module-title");
        title.setWrapText(true);
        title.setMaxWidth(112);

        Label detail = new Label(detailText);
        detail.getStyleClass().add("welcome-module-detail");
        detail.setWrapText(true);
        detail.setMaxWidth(112);

        VBox content = new VBox(7, icon, title, detail);
        content.setAlignment(Pos.CENTER);

        Button card = new Button();
        card.setGraphic(content);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.getStyleClass().add("welcome-module-card");
        card.setOnAction(event -> openLoginSelection(stage));
        return card;
    }

    private HBox createUpdatesSection(Stage stage) {
        VBox liveUpdates = new VBox(12);
        liveUpdates.getStyleClass().add("live-updates-panel");

        HBox liveHeading = new HBox(10, createPanelTitle("OFFICIAL UPDATES"), createPanelBadge("\uE789"));
        liveHeading.setAlignment(Pos.CENTER_LEFT);

        HBox updateItems = new HBox(16,
                createUpdateItem("\uE81D", "Route Updates", "Official route notices\nwill appear here."),
                createUpdateItem("\uE787", "Snan Schedule", "Verified event dates\nwill appear here."),
                createUpdateItem("\uE806", "Transport", "Bus and shuttle notices\nwill appear here."),
                createUpdateItem("\uE9CA", "Weather", "Weather advisories\nwill appear here."),
                createUpdateItem("\uE7BA", "Safety", "Police and safety alerts\nwill appear here."));
        updateItems.setAlignment(Pos.CENTER_LEFT);

        liveUpdates.getChildren().addAll(liveHeading, updateItems);

        VBox emergency = new VBox(12);
        emergency.getStyleClass().add("emergency-panel");

        HBox emergencyHeading = new HBox(10, createPanelBadge("\uEA18"), createPanelTitle("NEED EMERGENCY HELP?"));
        emergencyHeading.setAlignment(Pos.CENTER_LEFT);

        Label supportText = new Label("We are here for you 24/7");
        supportText.getStyleClass().add("emergency-text");

        HBox emergencyActions = new HBox(12,
                createEmergencyAction(stage, "\uE95E", "Ambulance"),
                createEmergencyAction(stage, "\uE72E", "Police"),
                createEmergencyAction(stage, "\uE95E", "Hospitals"));
        emergencyActions.setAlignment(Pos.CENTER);

        emergency.getChildren().addAll(emergencyHeading, supportText, emergencyActions);

        HBox row = new HBox(16, liveUpdates, emergency);
        row.setAlignment(Pos.CENTER);
        HBox.setHgrow(liveUpdates, Priority.ALWAYS);
        return row;
    }

    private Label createPanelTitle(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("welcome-panel-title");
        return title;
    }

    private Label createPanelBadge(String badgeText) {
        return createSymbolIcon(badgeText, "welcome-panel-badge");
    }

    private VBox createUpdateItem(String iconText, String titleText, String detailText) {
        Label icon = createSymbolIcon(iconText, "update-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("update-title");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("update-detail");

        VBox item = new VBox(5, icon, title, detail);
        item.getStyleClass().add("update-item");
        item.setAlignment(Pos.CENTER);
        return item;
    }

    private Button createEmergencyAction(Stage stage, String iconText, String titleText) {
        Label icon = createSymbolIcon(iconText, "emergency-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("emergency-action-title");

        VBox content = new VBox(5, icon, title);
        content.setAlignment(Pos.CENTER);

        Button action = new Button();
        action.setGraphic(content);
        action.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        action.getStyleClass().add("emergency-action");
        action.setOnAction(event -> openLoginSelection(stage));
        return action;
    }

    private HBox createTrustStrip() {
        HBox strip = new HBox(28,
                createTrustCard("\uE72E", "Trusted & Secure", "Your data is safe with us."),
                createTrustCard("\uE789", "Official Updates", "Verified alerts will appear here."),
                createTrustCard("\uE717", "Help Desk", "Support options are easy to find."),
                createTrustCard("\uE716", "Pilgrim Services", "Built for Simhastha visitors."));
        strip.getStyleClass().add("welcome-trust-strip");
        strip.setAlignment(Pos.CENTER);
        return strip;
    }

    private HBox createTrustCard(String iconText, String titleText, String detailText) {
        Label icon = createSymbolIcon(iconText, "trust-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("trust-title");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("trust-detail");

        VBox text = new VBox(3, title, detail);
        HBox card = new HBox(12, icon, text);
        card.getStyleClass().add("trust-card");
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    private Label createSymbolIcon(String iconText, String styleClass) {
        Label icon = new Label(iconText);
        icon.getStyleClass().addAll("symbol-icon", styleClass);
        return icon;
    }

    private HBox createDecorativeDivider() {
        Line left = new Line(0, 0, 180, 0);
        left.getStyleClass().add("welcome-divider-line");

        Label lotus = new Label("\u2735");
        lotus.getStyleClass().add("welcome-divider-lotus");

        Line right = new Line(0, 0, 180, 0);
        right.getStyleClass().add("welcome-divider-line");

        HBox divider = new HBox(16, left, lotus, right);
        divider.setAlignment(Pos.CENTER_LEFT);
        return divider;
    }

    private StackPane createHeroOverlay() {
        Rectangle wash = new Rectangle();
        wash.getStyleClass().add("welcome-left-wash");
        wash.widthProperty().bind(root.widthProperty());
        wash.heightProperty().bind(root.heightProperty());

        StackPane overlay = new StackPane(wash);
        overlay.setMouseTransparent(true);
        return overlay;
    }

    private HBox createFooter() {
        HBox footer = new HBox(28,
                createFooterFeature("\u26E8", "Trusted & Secure", "Your data is safe with us"),
                createFooterFeature("\u25CC", "Real-time Updates", "Stay informed with live alerts"),
                createFooterFeature("\u260E", "24/7 Support", "We are here to help you anytime"),
                createFlexibleSpace(),
                createFooterBrand());
        footer.getStyleClass().add("welcome-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(11, 34, 11, 42));
        return footer;
    }

    private HBox createFooterFeature(String iconText, String titleText, String detailText) {
        Label icon = new Label(iconText);
        icon.getStyleClass().add("footer-feature-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("footer-feature-title");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("footer-feature-detail");

        VBox text = new VBox(4, title, detail);
        HBox feature = new HBox(12, icon, text);
        feature.getStyleClass().add("footer-feature");
        feature.setAlignment(Pos.CENTER_LEFT);
        return feature;
    }

    private VBox createFooterBrand() {
        Label skyline = new Label("\u25B1 \u25B3 \u25B2 \u25B3 \u25B1");
        skyline.getStyleClass().add("footer-skyline");

        Label copyright = new Label("\u00A9 2027 Simhastha Connect. All rights reserved.");
        copyright.getStyleClass().add("footer-copyright");

        VBox brand = new VBox(4, skyline, copyright);
        brand.setAlignment(Pos.CENTER_RIGHT);
        return brand;
    }

    private void switchTheme(boolean darkMode) {
        ThemeManager.setTheme(darkMode ? ThemeManager.AppTheme.DARK : ThemeManager.AppTheme.LIGHT);
        backgroundImage.setImage(ThemeManager.isDark() ? nightImage : sunlightImage);
        ThemeManager.applyTo(root);
        updateThemeButtons();
        updateImageViewport();
    }

    private void updateThemeButtons() {
        if (lightModeButton == null || darkModeButton == null) {
            return;
        }

        lightModeButton.getStyleClass().remove("theme-toggle-button-active");
        darkModeButton.getStyleClass().remove("theme-toggle-button-active");

        if (ThemeManager.isDark()) {
            darkModeButton.getStyleClass().add("theme-toggle-button-active");
        } else {
            lightModeButton.getStyleClass().add("theme-toggle-button-active");
        }
    }

    private void startClock() {
        updateDateTime();
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateDateTime()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updateDateTime() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy\nEEEE");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        dateLabel.setText(LocalDate.now().format(dateFormatter));
        timeLabel.setText(LocalTime.now().format(timeFormatter));
    }

    private ImageView createCoverImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return imageView;
    }

    private void bindCoverImage() {
        backgroundImage.fitWidthProperty().bind(root.widthProperty());
        backgroundImage.fitHeightProperty().bind(root.heightProperty());

        ChangeListener<Number> viewportListener = (observable, oldValue, newValue) -> updateImageViewport();
        root.widthProperty().addListener(viewportListener);
        root.heightProperty().addListener(viewportListener);
        backgroundImage.imageProperty().addListener((observable, oldValue, newValue) -> updateImageViewport());
        updateImageViewport();
    }

    private void updateImageViewport() {
        Image image = backgroundImage.getImage();
        double viewWidth = backgroundImage.getFitWidth();
        double viewHeight = backgroundImage.getFitHeight();

        if (image == null || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double scale = Math.max(viewWidth / imageWidth, viewHeight / imageHeight);
        double cropWidth = viewWidth / scale;
        double cropHeight = viewHeight / scale;
        double xBias = ThemeManager.isDark() ? 0.48 : 0.50;
        double yBias = ThemeManager.isDark() ? 0.40 : 0.40;
        double x = Math.max(0, (imageWidth - cropWidth) * xBias);
        double y = Math.max(0, (imageHeight - cropHeight) * yBias);

        backgroundImage.setViewport(new javafx.geometry.Rectangle2D(x, y, cropWidth, cropHeight));
    }

    private Image loadImage(String path) {
        URL imageUrl = getClass().getResource(path);
        if (imageUrl == null) {
            return null;
        }
        return new Image(imageUrl.toExternalForm());
    }

    private Region createFlexibleSpace() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private ScrollPane scroll(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("welcome-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private void openLoginSelection(Stage stage) {
        LoginSelectionPage loginSelectionPage = new LoginSelectionPage();
        NavigationUtil.navigate(stage, loginSelectionPage.createScene(stage));
    }

    private void playTitleAnimation(BorderPane content) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(650), content);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        fadeTransition.play();
    }

    private void addTheme(Scene scene) {
        ThemeManager.addTheme(scene, this);
    }
}
