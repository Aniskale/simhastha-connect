package com.simhastha.view;

import java.net.URL;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class IntroPage {

    private ParallelTransition entrance;
    private Timeline handoff;

    public Scene createScene(Stage stage) {
        StackPane root = new StackPane();
        root.getStyleClass().add("intro-root");
        root.setUserData(this);

        ImageView background = new ImageView(loadImage("/images/welcome-light.png"));
        background.getStyleClass().add("intro-background-image");
        background.setPreserveRatio(false);

        Rectangle overlay = new Rectangle();
        overlay.getStyleClass().add("intro-background-overlay");
        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());

        VBox content = createContent();
        root.getChildren().addAll(background, overlay, createParticleLayer(root), createTopLine(), content);

        Scene scene = new Scene(root, 1200, 680);
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        ThemeManager.addTheme(scene, this);
        playIntro(stage, content);
        return scene;
    }

    private VBox createContent() {
        ImageView logo = new ImageView(loadImage("/images/sclogo.png"));
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setFitWidth(128);
        logo.setFitHeight(128);
        logo.getStyleClass().add("intro-logo");

        Label nameOne = new Label("SIMHASTHA");
        nameOne.getStyleClass().add("intro-title-main");

        Label nameTwo = new Label("CONNECT");
        nameTwo.getStyleClass().add("intro-title-accent");

        Label subtitle = new Label("Nashik Simhastha 2027");
        subtitle.getStyleClass().add("intro-subtitle");

        Label sanskrit = new Label("\u0950 \u0924\u094D\u0930\u094D\u092F\u0902\u092C\u0915\u0902 \u092F\u091C\u093E\u092E\u0939\u0947 \u0938\u0941\u0917\u0928\u094D\u0927\u093F\u0902 \u092A\u0941\u0937\u094D\u091F\u093F\u0935\u0930\u094D\u0927\u0928\u092E\u094D");
        sanskrit.getStyleClass().add("intro-sanskrit");

        Label detail = new Label("One platform for pilgrims, services, transport and emergency support");
        detail.getStyleClass().add("intro-detail");

        HBox divider = createDivider();
        HBox chips = new HBox(10,
                chip("Pilgrim Services"),
                chip("Transport Network"),
                chip("Emergency Ready"));
        chips.setAlignment(Pos.CENTER);

        VBox loader = createLoader();

        VBox content = new VBox(10, logo, nameOne, nameTwo, subtitle, sanskrit, divider, detail, chips,
                createTraditionalLoaderScene(), loader);
        content.getStyleClass().add("intro-content");
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(38, 56, 42, 56));
        content.setOpacity(0);
        content.setScaleX(0.92);
        content.setScaleY(0.92);
        return content;
    }

    private VBox createLoader() {
        HBox dots = new HBox(10);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            Circle dot = new Circle(5);
            dot.getStyleClass().add("intro-loading-dot");
            dots.getChildren().add(dot);
            playDotPulse(dot, i * 150);
        }

        Region railFill = new Region();
        railFill.getStyleClass().add("intro-loading-fill");
        railFill.setMinWidth(34);
        railFill.setPrefWidth(34);
        railFill.setMaxWidth(34);

        StackPane rail = new StackPane(railFill);
        rail.getStyleClass().add("intro-loading-rail");
        rail.setMinSize(190, 5);
        rail.setPrefSize(190, 5);
        rail.setMaxSize(190, 5);
        StackPane.setAlignment(railFill, Pos.CENTER_LEFT);
        playRailSweep(railFill);

        Label loading = new Label("Preparing your spiritual journey");
        loading.getStyleClass().add("intro-loading-text");

        VBox loader = new VBox(8, rail, loading, dots);
        loader.setAlignment(Pos.CENTER);
        loader.getStyleClass().add("intro-loader");
        return loader;
    }

    private StackPane createTraditionalLoaderScene() {
        Line base = new Line(0, 0, 360, 0);
        base.getStyleClass().add("intro-ritual-line");
        HBox lamps = new HBox(44, diya(), samai(), diya());
        lamps.setAlignment(Pos.CENTER);
        StackPane scene = new StackPane(base, lamps);
        scene.getStyleClass().add("intro-ritual-scene");
        return scene;
    }

    private StackPane diya() {
        Polygon bowl = new Polygon(0, 18, 58, 18, 47, 28, 11, 28);
        bowl.getStyleClass().add("intro-diya-bowl");
        Circle glow = new Circle(20);
        glow.getStyleClass().add("intro-diya-glow");
        Polygon flame = new Polygon(12, 26, 23, 0, 34, 26);
        flame.getStyleClass().add("intro-diya-flame");
        StackPane lamp = new StackPane(glow, bowl, flame);
        lamp.getStyleClass().add("intro-diya");
        StackPane.setAlignment(flame, Pos.TOP_CENTER);
        StackPane.setAlignment(bowl, Pos.BOTTOM_CENTER);
        return lamp;
    }

    private StackPane samai() {
        Circle glow = new Circle(24);
        glow.getStyleClass().add("intro-diya-glow");
        Polygon flame = new Polygon(10, 26, 21, 0, 32, 26);
        flame.getStyleClass().add("intro-diya-flame");
        Rectangle stem = new Rectangle(6, 34);
        stem.getStyleClass().add("intro-samai-stem");
        Polygon base = new Polygon(0, 30, 56, 30, 44, 40, 12, 40);
        base.getStyleClass().add("intro-diya-bowl");
        StackPane lamp = new StackPane(glow, stem, base, flame);
        lamp.getStyleClass().add("intro-samai");
        StackPane.setAlignment(flame, Pos.TOP_CENTER);
        StackPane.setAlignment(stem, Pos.CENTER);
        StackPane.setAlignment(base, Pos.BOTTOM_CENTER);
        return lamp;
    }

    private HBox createTopLine() {
        Label left = new Label("SIMHASTHA CONNECT");
        left.getStyleClass().add("intro-topline-text");
        Label right = new Label("Secure Desktop Experience");
        right.getStyleClass().add("intro-topline-text");

        HBox row = new HBox(18, left, new Region(), right);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        row.getStyleClass().add("intro-topline");
        row.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(row, Pos.TOP_CENTER);
        StackPane.setMargin(row, new Insets(20, 34, 0, 34));
        return row;
    }

    private Pane createParticleLayer(StackPane root) {
        Pane layer = new Pane();
        layer.setMouseTransparent(true);
        layer.getStyleClass().add("intro-particle-layer");
        layer.prefWidthProperty().bind(root.widthProperty());
        layer.prefHeightProperty().bind(root.heightProperty());

        double[][] points = {
                {120, 128, 3}, {236, 210, 2}, {384, 158, 3}, {555, 118, 2},
                {724, 152, 3}, {886, 232, 2}, {1075, 156, 3}, {1160, 440, 2},
                {250, 520, 3}, {470, 572, 2}, {720, 545, 3}, {980, 530, 2}
        };

        for (int i = 0; i < points.length; i++) {
            Circle particle = new Circle(points[i][0], points[i][1], points[i][2]);
            particle.getStyleClass().add("intro-live-particle");
            layer.getChildren().add(particle);
            playParticleFloat(particle, i);
        }

        return layer;
    }

    private Label chip(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("intro-chip");
        return label;
    }

    private HBox createDivider() {
        Line left = new Line(0, 0, 90, 0);
        Line right = new Line(0, 0, 90, 0);
        left.getStyleClass().add("intro-divider-line");
        right.getStyleClass().add("intro-divider-line");

        Label mark = new Label("2027");
        mark.getStyleClass().add("intro-divider-mark");

        HBox divider = new HBox(14, left, mark, right);
        divider.setAlignment(Pos.CENTER);
        return divider;
    }

    private void playIntro(Stage stage, VBox content) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(850), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(1000), content);
        scaleIn.setFromX(0.92);
        scaleIn.setFromY(0.92);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        entrance = new ParallelTransition(fadeIn, scaleIn);
        entrance.play();

        handoff = new Timeline(new KeyFrame(Duration.seconds(7), event -> {
            WelcomePage welcomePage = new WelcomePage();
            NavigationUtil.navigate(stage, welcomePage.createScene(stage));
        }));
        handoff.play();
    }

    private void playDotPulse(Circle dot, int delayMillis) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(620), dot);
        scale.setDelay(Duration.millis(delayMillis));
        scale.setFromX(0.72);
        scale.setFromY(0.72);
        scale.setToX(1.18);
        scale.setToY(1.18);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.play();
    }

    private void playRailSweep(Region railFill) {
        TranslateTransition sweep = new TranslateTransition(Duration.millis(1500), railFill);
        sweep.setFromX(-78);
        sweep.setToX(78);
        sweep.setAutoReverse(true);
        sweep.setCycleCount(Animation.INDEFINITE);
        sweep.play();
    }

    private void playParticleFloat(Node particle, int index) {
        TranslateTransition floatUp = new TranslateTransition(Duration.millis(2400 + index * 130), particle);
        floatUp.setFromY(0);
        floatUp.setToY(-16 - (index % 3) * 5);
        floatUp.setAutoReverse(true);
        floatUp.setCycleCount(Animation.INDEFINITE);

        FadeTransition shimmer = new FadeTransition(Duration.millis(1700 + index * 80), particle);
        shimmer.setFromValue(0.26);
        shimmer.setToValue(0.88);
        shimmer.setAutoReverse(true);
        shimmer.setCycleCount(Animation.INDEFINITE);

        new ParallelTransition(floatUp, shimmer).play();
    }

    private Image loadImage(String path) {
        URL imageUrl = getClass().getResource(path);
        if (imageUrl == null) {
            return null;
        }
        return new Image(imageUrl.toExternalForm());
    }
}
