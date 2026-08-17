package com.simhastha.view;

import java.net.URL;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class LoginSelectionPage {

    public Scene createScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-background");

        root.setTop(createHeader(stage));
        root.setCenter(createPortalOptions(stage));

        Scene scene = new Scene(root, 1200, 750);
        addTheme(scene);
        return scene;
    }

    private BorderPane createHeader(Stage stage) {
        Button backButton = new Button("< Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(event -> {
            WelcomePage welcomePage = new WelcomePage();
            stage.setScene(welcomePage.createScene(stage));
        });

        Label brand = new Label("SIMHASTHA CONNECT");
        brand.getStyleClass().add("page-brand");

        Label heading = new Label("Choose Your Portal");
        heading.getStyleClass().add("page-heading");

        Label subtitle = new Label("Continue your Simhastha journey");
        subtitle.getStyleClass().add("page-subtitle");

        VBox titleBox = new VBox(6, brand, heading, subtitle, createOrnamentLine());
        titleBox.setAlignment(Pos.CENTER);

        BorderPane header = new BorderPane();
        header.setLeft(backButton);
        header.setCenter(titleBox);
        header.setPadding(new Insets(24, 38, 8, 38));
        return header;
    }

    private HBox createPortalOptions(Stage stage) {
        VBox userCard = createPortalCard(
                "PILGRIM / USER",
                "Access transport, ghats, snan information, events, accommodation, puja services, emergency assistance and Lost & Found.",
                "USER PORTAL");
        Button userButton = (Button) userCard.getChildren().get(userCard.getChildren().size() - 1);
        userButton.setOnAction(event -> {
            UserAuthPage userAuthPage = new UserAuthPage();
            stage.setScene(userAuthPage.createScene(stage));
        });

        VBox businessCard = createPortalCard(
                "BUSINESS & PARTNER",
                "For local businesses, service providers and transport operators participating in the Simhastha ecosystem.",
                "BUSINESS & PARTNER PORTAL");
        Button businessButton = (Button) businessCard.getChildren().get(businessCard.getChildren().size() - 1);
        businessButton.setOnAction(event -> {
            BusinessPartnerPage businessPartnerPage = new BusinessPartnerPage();
            stage.setScene(businessPartnerPage.createScene(stage));
        });

        StackPane visual = createDecorativeSide();

        HBox cards = new HBox(24, userCard, businessCard);
        cards.setAlignment(Pos.CENTER);

        HBox trustStrip = createTrustStrip();

        VBox cardArea = new VBox(24, visual, cards, trustStrip);
        cardArea.setAlignment(Pos.CENTER);
        cardArea.setPadding(new Insets(15, 44, 55, 44));

        HBox.setHgrow(userCard, Priority.ALWAYS);
        HBox.setHgrow(businessCard, Priority.ALWAYS);

        HBox wrapper = new HBox(cardArea);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private HBox createTrustStrip() {
        HBox strip = new HBox(26,
                createTrustItem("Trusted & Secure", "Data safe with simple protected login"),
                createTrustItem("Real-time Ready", "Designed for future Firebase updates"),
                createTrustItem("24/7 Support", "Emergency and help modules ready"));
        strip.getStyleClass().add("portal-trust-strip");
        strip.setAlignment(Pos.CENTER);
        return strip;
    }

    private VBox createTrustItem(String titleText, String detailText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("portal-trust-title");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("portal-trust-detail");
        detail.setWrapText(true);

        VBox item = new VBox(4, title, detail);
        item.setAlignment(Pos.CENTER);
        return item;
    }

    private VBox createPortalCard(String titleText, String descriptionText, String buttonText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("portal-title");
        title.setWrapText(true);

        Label description = new Label(descriptionText);
        description.getStyleClass().add("description-text");
        description.setWrapText(true);

        Button button = new Button(buttonText);
        button.getStyleClass().add("primary-button");
        button.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(18, title, description, button);
        card.getStyleClass().add("portal-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(370);
        card.setMaxWidth(460);
        card.setMinHeight(245);
        return card;
    }

    private StackPane createDecorativeSide() {
        Circle outer = new Circle(78);
        outer.getStyleClass().add("mandala-outer-small");

        Circle inner = new Circle(38);
        inner.getStyleClass().add("mandala-inner-small");

        Label text = new Label("Ramkund - Godavari - Nashik");
        text.getStyleClass().add("visual-caption");

        StackPane stack = new StackPane(outer, inner, text);
        stack.getStyleClass().add("portal-center-visual");
        stack.setMinHeight(150);
        return stack;
    }

    private HBox createOrnamentLine() {
        Line left = new Line(0, 0, 90, 0);
        left.getStyleClass().add("decor-line");

        Circle dot = new Circle(4);
        dot.getStyleClass().add("decor-dot");

        Line right = new Line(0, 0, 90, 0);
        right.getStyleClass().add("decor-line");

        HBox line = new HBox(12, left, dot, right);
        line.setAlignment(Pos.CENTER);
        return line;
    }

    private void addTheme(Scene scene) {
        URL cssUrl = getClass().getResource("/css/simhastha-theme.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }
}
