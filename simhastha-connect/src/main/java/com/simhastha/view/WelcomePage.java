package com.simhastha.view;

import java.net.URL;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class WelcomePage {

    private MediaPlayer mediaPlayer;
    private boolean musicPlaying;

    public Scene createScene(Stage stage) {
        StackPane root = new StackPane();
        root.getStyleClass().add("main-background");

        ImageView backgroundImage = createOptionalImage("/images/kumbh-background.jpg", 1200, 750);
        backgroundImage.getStyleClass().add("background-image");

        BorderPane content = new BorderPane();
        content.setTop(createHeroArea());
        content.setCenter(createMainArea(stage));
        content.setBottom(createBottomArea(stage));

        root.getChildren().addAll(backgroundImage, createBackgroundDecoration(), content);

        Scene scene = new Scene(root, 1200, 750);
        backgroundImage.fitWidthProperty().bind(scene.widthProperty());
        backgroundImage.fitHeightProperty().bind(scene.heightProperty());
        addTheme(scene);
        playTitleAnimation(content);

        return scene;
    }

    private VBox createHeroArea() {
        Label title = new Label("SIMHASTHA CONNECT");
        title.getStyleClass().add("hero-title");

        Label tagline = new Label("ONE PLATFORM FOR A BETTER SIMHASTHA EXPERIENCE");
        tagline.getStyleClass().add("hero-tagline");

        Label eventName = new Label("Nashik Simhastha 2027");
        eventName.getStyleClass().add("event-pill");

        VBox heroBox = new VBox(5, createLogoBox(), title, tagline, eventName, createOrnamentLine());
        heroBox.setAlignment(Pos.CENTER);
        heroBox.setPadding(new Insets(10, 28, 4, 28));
        return heroBox;
    }

    private StackPane createLogoBox() {
        StackPane logoBox = new StackPane();
        logoBox.getStyleClass().add("premium-logo-box");
        logoBox.setPrefSize(72, 72);
        logoBox.setMaxSize(72, 72);

        ImageView logoImage = createOptionalImage("/images/simhastha-logo.png", 56, 56);

        if (logoImage.getImage() == null) {
            Label placeholder = new Label("SIMHASTHA\nCONNECT");
            placeholder.getStyleClass().add("logo-placeholder-text");
            logoBox.getChildren().add(placeholder);
        } else {
            logoImage.setPreserveRatio(true);
            logoImage.setFitWidth(56);
            logoImage.setFitHeight(56);
            logoBox.getChildren().add(logoImage);
        }

        return logoBox;
    }

    private HBox createMainArea(Stage stage) {
        VBox leftPanel = createIntroPanel();
        StackPane rightPanel = createTraditionalVisual();

        HBox mainArea = new HBox(26, leftPanel, rightPanel);
        mainArea.setAlignment(Pos.CENTER);
        mainArea.setPadding(new Insets(6, 54, 6, 54));
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        return mainArea;
    }

    private VBox createIntroPanel() {
        Label introTitle = new Label("Your Digital Companion for Nashik Simhastha 2027");
        introTitle.getStyleClass().add("section-title");
        introTitle.setWrapText(true);

        Label introText = new Label(
                "Important Simhastha information, pilgrim assistance and local services brought together in one simple desktop application.");
        introText.getStyleClass().add("description-text");
        introText.setWrapText(true);

        Label moduleHeading = new Label("Core Services");
        moduleHeading.getStyleClass().add("small-heading");

        HBox moduleGrid = new HBox(12, createModuleColumn(true), createModuleColumn(false));
        moduleGrid.setAlignment(Pos.CENTER);

        VBox panel = new VBox(12, introTitle, introText, moduleHeading, moduleGrid);
        panel.getStyleClass().add("glass-panel");
        panel.setMaxWidth(570);
        panel.setAlignment(Pos.CENTER_LEFT);
        return panel;
    }

    private VBox createModuleColumn(boolean firstColumn) {
        VBox column = new VBox(10);

        if (firstColumn) {
            column.getChildren().addAll(
                    createModuleCard("Transport", "PRIMARY SERVICE", true),
                    createModuleCard("Ghats & Snan", "Ramkund guidance", false),
                    createModuleCard("Events", "Schedules", false),
                    createModuleCard("Stay", "Accommodation", false));
        } else {
            column.getChildren().addAll(
                    createModuleCard("Puja Services", "Ritual support", false),
                    createModuleCard("Local Services", "Nearby help", false),
                    createModuleCard("Emergency Help", "Quick assistance", false),
                    createModuleCard("Lost & Found", "Visitor support", false));
        }

        HBox.setHgrow(column, Priority.ALWAYS);
        return column;
    }

    private VBox createModuleCard(String title, String detail, boolean primary) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(primary ? "module-title-primary" : "module-title");

        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add(primary ? "module-badge-primary" : "module-detail");

        VBox card = new VBox(3, titleLabel, detailLabel);
        card.getStyleClass().add(primary ? "module-card-primary" : "module-card");
        card.setMinWidth(190);
        return card;
    }

    private StackPane createTraditionalVisual() {
        Arc templeArch = new Arc(0, 4, 180, 130, 0, 180);
        templeArch.setType(ArcType.OPEN);
        templeArch.getStyleClass().add("temple-arch");

        Arc innerArch = new Arc(0, 10, 132, 96, 0, 180);
        innerArch.setType(ArcType.OPEN);
        innerArch.getStyleClass().add("temple-arch-soft");

        Line pillarLeft = new Line(-180, -5, -180, 122);
        pillarLeft.getStyleClass().add("arch-pillar");

        Line pillarRight = new Line(180, -5, 180, 122);
        pillarRight.getStyleClass().add("arch-pillar");

        VBox ghatSteps = new VBox(6);
        ghatSteps.setAlignment(Pos.BOTTOM_CENTER);
        ghatSteps.getChildren().addAll(createStep(140), createStep(190), createStep(240), createStep(295), createStep(350));
        StackPane.setAlignment(ghatSteps, Pos.BOTTOM_CENTER);
        StackPane.setMargin(ghatSteps, new Insets(0, 0, 26, 0));

        HBox flags = new HBox(92, createFlag(), createFlag());
        flags.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(flags, Pos.TOP_CENTER);
        StackPane.setMargin(flags, new Insets(28, 0, 0, 0));

        HBox diyas = new HBox(92, createDiya(), createDiya());
        diyas.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(diyas, Pos.BOTTOM_CENTER);
        StackPane.setMargin(diyas, new Insets(0, 0, 90, 0));

        ImageView temple = createOptionalImage("/images/temple-silhouette.png", 340, 145);
        StackPane.setAlignment(temple, Pos.TOP_CENTER);
        StackPane.setMargin(temple, new Insets(44, 0, 0, 0));

        ImageView ghat = createOptionalImage("/images/ghat-silhouette.png", 360, 118);
        StackPane.setAlignment(ghat, Pos.BOTTOM_CENTER);
        StackPane.setMargin(ghat, new Insets(0, 0, 42, 0));

        ImageView pilgrim = createOptionalImage("/images/pilgrim-silhouette.png", 120, 155);
        StackPane.setAlignment(pilgrim, Pos.BOTTOM_LEFT);
        StackPane.setMargin(pilgrim, new Insets(0, 0, 70, 54));

        Label sanskrit = new Label("\u0950 \u0924\u094D\u0930\u094D\u092F\u092E\u094D\u092C\u0915\u0902 \u092F\u091C\u093E\u092E\u0939\u0947 \u0938\u0941\u0917\u0928\u094D\u0927\u093F\u0902 \u092A\u0941\u0937\u094D\u091F\u093F\u0935\u0930\u094D\u0927\u0928\u092E\u094D\u0964");
        sanskrit.getStyleClass().add("sanskrit-line");
        sanskrit.setWrapText(true);

        Label caption = new Label("Trimbakeshwar \u2022 Nashik");
        caption.getStyleClass().add("visual-caption");

        VBox centerText = new VBox(8, sanskrit, caption);
        centerText.setAlignment(Pos.CENTER);
        centerText.setMaxWidth(360);
        StackPane.setAlignment(centerText, Pos.CENTER);
        StackPane.setMargin(centerText, new Insets(28, 30, 0, 30));

        StackPane visual = new StackPane(templeArch, innerArch, pillarLeft, pillarRight, temple, flags, ghatSteps, ghat,
                pilgrim, diyas, centerText);
        visual.getStyleClass().add("visual-panel");
        visual.setMaxWidth(520);
        visual.setMinHeight(315);
        visual.setPrefHeight(325);
        return visual;
    }

    private Rectangle createStep(double width) {
        Rectangle rectangle = new Rectangle(width, 10);
        rectangle.getStyleClass().add("ghat-step");
        return rectangle;
    }

    private StackPane createFlag() {
        Line pole = new Line(0, 0, 0, 72);
        pole.getStyleClass().add("flag-pole");

        Polygon flag = new Polygon(0, 0, 62, 12, 0, 28);
        flag.getStyleClass().add("saffron-flag");
        StackPane.setAlignment(flag, Pos.TOP_LEFT);
        StackPane.setMargin(flag, new Insets(0, 0, 0, 2));

        StackPane flagBox = new StackPane(pole, flag);
        flagBox.setPrefSize(70, 78);
        return flagBox;
    }

    private StackPane createDiya() {
        ImageView diyaImage = createOptionalImage("/images/diya.png", 54, 46);

        if (diyaImage.getImage() != null) {
            StackPane imageBox = new StackPane(diyaImage);
            imageBox.setPrefSize(54, 46);
            return imageBox;
        }

        Circle glow = new Circle(21);
        glow.getStyleClass().add("diya-glow");

        Arc flame = new Arc(0, -4, 9, 17, 70, 240);
        flame.setType(ArcType.ROUND);
        flame.getStyleClass().add("diya-flame");

        Arc bowl = new Arc(0, 13, 24, 13, 180, 180);
        bowl.setType(ArcType.ROUND);
        bowl.getStyleClass().add("diya-bowl");

        StackPane diya = new StackPane(glow, flame, bowl);
        diya.setPrefSize(54, 46);
        return diya;
    }

    private HBox createBottomArea(Stage stage) {
        Button exploreButton = new Button("EXPLORE SIMHASTHA CONNECT");
        exploreButton.getStyleClass().add("primary-button");
        exploreButton.setOnAction(event -> openLoginSelection(stage));

        Button musicButton = createMusicButton();

        HBox buttonRow = new HBox(18, exploreButton, musicButton);
        buttonRow.setAlignment(Pos.CENTER);

        Label footer = new Label("Nashik Simhastha 2027 - Digital Pilgrim Assistance Platform");
        footer.getStyleClass().add("footer-text");

        VBox bottom = new VBox(7, buttonRow, footer);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(2, 28, 12, 28));

        HBox wrapper = new HBox(bottom);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private StackPane createBackgroundDecoration() {
        StackPane decoration = new StackPane();
        decoration.setMouseTransparent(true);

        Line leftLine = new Line(-390, 0, -170, 0);
        leftLine.getStyleClass().add("decor-line");

        Line rightLine = new Line(170, 0, 390, 0);
        rightLine.getStyleClass().add("decor-line");

        Circle center = new Circle(5);
        center.getStyleClass().add("decor-dot");

        HBox waveBox = new HBox(12, leftLine, center, rightLine);
        waveBox.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(waveBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(waveBox, new Insets(0, 0, 88, 0));

        decoration.getChildren().add(waveBox);
        return decoration;
    }

    private HBox createOrnamentLine() {
        Line left = new Line(0, 0, 120, 0);
        left.getStyleClass().add("decor-line");

        Circle dot = new Circle(4);
        dot.getStyleClass().add("decor-dot");

        Line right = new Line(0, 0, 120, 0);
        right.getStyleClass().add("decor-line");

        HBox box = new HBox(12, left, dot, right);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Button createMusicButton() {
        Button musicButton = new Button("Music OFF");
        musicButton.getStyleClass().add("music-button");

        URL musicUrl = getClass().getResource("/audio/welcome-music.mp3");

        if (musicUrl == null) {
            musicButton.setText("Music Not Added");
            musicButton.setDisable(true);
            return musicButton;
        }

        try {
            Media media = new Media(musicUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setVolume(0.32);

            musicButton.setOnAction(event -> {
                if (musicPlaying) {
                    mediaPlayer.pause();
                    musicPlaying = false;
                    musicButton.setText("Music OFF");
                } else {
                    mediaPlayer.play();
                    musicPlaying = true;
                    musicButton.setText("Music ON");
                }
            });
        } catch (Exception exception) {
            musicButton.setText("Music Not Available");
            musicButton.setDisable(true);
        }

        return musicButton;
    }

    private ImageView createOptionalImage(String path, double width, double height) {
        URL imageUrl = getClass().getResource(path);
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);

        if (imageUrl != null) {
            imageView.setImage(new Image(imageUrl.toExternalForm()));
        }

        return imageView;
    }

    private void openLoginSelection(Stage stage) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

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
