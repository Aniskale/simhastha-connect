package com.simhastha.view;

import java.net.URL;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
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
        root.getChildren().addAll(background, overlay, createTopLine(), content);

        Scene scene = new Scene(root, 1200, 680);
        background.fitWidthProperty().bind(scene.widthProperty());
        background.fitHeightProperty().bind(scene.heightProperty());
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

        VBox content = new VBox(10, logo, nameOne, nameTwo, subtitle, sanskrit, divider, detail, chips);
        content.getStyleClass().add("intro-content");
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(38, 56, 42, 56));
        content.setOpacity(0);
        content.setScaleX(0.92);
        content.setScaleY(0.92);
        return content;
    }

    private HBox createTopLine() {
        Label left = new Label("SIMHASTHA CONNECT");
        left.getStyleClass().add("intro-topline-text");
        Label right = new Label("Secure Desktop Experience");
        right.getStyleClass().add("intro-topline-text");

        HBox row = new HBox(18, left, new javafx.scene.layout.Region(), right);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        row.getStyleClass().add("intro-topline");
        row.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(row, Pos.TOP_CENTER);
        StackPane.setMargin(row, new Insets(20, 34, 0, 34));
        return row;
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

        handoff = new Timeline(new KeyFrame(Duration.seconds(8), event -> {
            WelcomePage welcomePage = new WelcomePage();
            stage.setScene(welcomePage.createScene(stage));
        }));
        handoff.play();
    }

    private Image loadImage(String path) {
        URL imageUrl = getClass().getResource(path);
        if (imageUrl == null) {
            return null;
        }
        return new Image(imageUrl.toExternalForm());
    }
}
