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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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

    private static boolean darkModeSelected;

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
        root.getStyleClass().addAll("welcome-root", darkModeSelected ? "welcome-root-dark" : "welcome-root-light");

        sunlightImage = loadImage("/images/welcome-light.png");
        nightImage = loadImage("/images/welcome-dark.png");
        backgroundImage = createCoverImageView(darkModeSelected ? nightImage : sunlightImage);
        backgroundImage.getStyleClass().add("welcome-background-image");

        BorderPane page = new BorderPane();
        page.getStyleClass().add("welcome-page");
        page.setTop(createHeader());
        page.setCenter(createHeroContent(stage));
        page.setBottom(createFooter());

        root.getChildren().addAll(backgroundImage, createHeroOverlay(), page);

        Scene scene = new Scene(root, 1200, 750);
        bindCoverImage(scene);
        addTheme(scene);
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
        header.setPadding(new Insets(24, 32, 0, 40));
        return header;
    }

    private StackPane createLogoMark() {
        ImageView mark = new ImageView(loadImage("/images/sclogo.png"));
        mark.setPreserveRatio(true);
        mark.setSmooth(true);
        mark.setFitWidth(88);
        mark.setFitHeight(88);
        mark.getStyleClass().add("welcome-logo-image");

        StackPane logo = new StackPane(mark);
        logo.getStyleClass().add("welcome-logo-shell");
        logo.setMinSize(92, 92);
        logo.setMaxSize(92, 92);
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
        lightModeButton = new Button("\u2600");
        darkModeButton = new Button("\u263E");
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
                createUtilityItem("\uD83D\uDCC5", dateLabel),
                createUtilitySeparator(),
                createUtilityItem("\u25F7", timeLabel),
                createUtilitySeparator(),
                createUtilityItem("\u25CE", location),
                createUtilitySeparator(),
                createThemeToggle(),
                createTopIconButton("\uD83D\uDD14", "3", "Notifications"),
                createTopIconButton("\u25CB", null, "Profile"));
        bar.getStyleClass().add("utility-bar");
        bar.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    private HBox createUtilityItem(String iconText, Label value) {
        Label icon = new Label(iconText);
        icon.getStyleClass().add("utility-icon");

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
        Label icon = new Label(iconText);
        icon.getStyleClass().add("top-action-icon");

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

    private HBox createHeroContent(Stage stage) {
        Label welcome = new Label("Welcome to");
        welcome.getStyleClass().add("welcome-kicker");

        Label title = new Label("SIMHASTHA CONNECT");
        title.getStyleClass().add("welcome-title");
        title.setMinWidth(720);

        Label verse = new Label("\u0965 \u0938\u0930\u094D\u0935\u0947 \u092D\u0935\u0928\u094D\u0924\u0941 \u0938\u0941\u0916\u093F\u0928\u0903 \u0938\u0930\u094D\u0935\u0947 \u0938\u0928\u094D\u0924\u0941 \u0928\u093F\u0930\u093E\u092E\u092F\u093E\u0903 \u0965");
        verse.getStyleClass().add("welcome-sanskrit-text");

        Label description = new Label("Your digital companion for a safe, smooth and\ndivine Simhastha experience in Nashik.");
        description.getStyleClass().add("welcome-description");

        Button exploreButton = new Button("EXPLORE SIMHASTHA   \u2192");
        exploreButton.getStyleClass().add("primary-cta");
        exploreButton.setGraphic(createButtonLogo());
        exploreButton.setContentDisplay(ContentDisplay.LEFT);
        exploreButton.setGraphicTextGap(10);
        exploreButton.setOnAction(event -> openLoginSelection(stage));

        Button loginButton = new Button("LOGIN / SIGN UP");
        loginButton.getStyleClass().add("secondary-cta");
        loginButton.setGraphic(createButtonLogo());
        loginButton.setContentDisplay(ContentDisplay.LEFT);
        loginButton.setGraphicTextGap(10);
        loginButton.setOnAction(event -> openLoginSelection(stage));

        HBox actions = new HBox(18, exploreButton, loginButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(14, welcome, title, createDecorativeDivider(), verse, description, actions);
        copy.getStyleClass().add("welcome-copy");
        copy.setAlignment(Pos.CENTER_LEFT);

        HBox hero = new HBox(copy, createFlexibleSpace());
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(0, 40, 8, 54));
        return hero;
    }

    private ImageView createButtonLogo() {
        ImageView logo = new ImageView(loadImage("/images/sclogo.png"));
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setFitWidth(19);
        logo.setFitHeight(19);
        logo.getStyleClass().add("cta-logo");
        return logo;
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
        darkModeSelected = darkMode;
        backgroundImage.setImage(darkModeSelected ? nightImage : sunlightImage);
        root.getStyleClass().removeAll("welcome-root-light", "welcome-root-dark");
        root.getStyleClass().add(darkModeSelected ? "welcome-root-dark" : "welcome-root-light");
        updateThemeButtons();
        updateImageViewport();
    }

    private void updateThemeButtons() {
        if (lightModeButton == null || darkModeButton == null) {
            return;
        }

        lightModeButton.getStyleClass().remove("theme-toggle-button-active");
        darkModeButton.getStyleClass().remove("theme-toggle-button-active");

        if (darkModeSelected) {
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

    private void bindCoverImage(Scene scene) {
        backgroundImage.fitWidthProperty().bind(scene.widthProperty());
        backgroundImage.fitHeightProperty().bind(scene.heightProperty());

        ChangeListener<Number> viewportListener = (observable, oldValue, newValue) -> updateImageViewport();
        scene.widthProperty().addListener(viewportListener);
        scene.heightProperty().addListener(viewportListener);
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
        double xBias = darkModeSelected ? 0.48 : 0.50;
        double yBias = darkModeSelected ? 0.40 : 0.40;
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

    private void openLoginSelection(Stage stage) {
        LoginSelectionPage loginSelectionPage = new LoginSelectionPage();
        stage.setScene(loginSelectionPage.createScene(stage));
    }

    private void playTitleAnimation(BorderPane content) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(650), content);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        fadeTransition.play();
    }

    private void addTheme(Scene scene) {
        URL cssUrl = getClass().getResource("/css/simhastha-theme.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }
}
