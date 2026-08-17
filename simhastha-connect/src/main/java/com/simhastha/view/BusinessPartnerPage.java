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
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class BusinessPartnerPage {

    public Scene createScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-background");
        root.setTop(createHeader(stage));
        root.setCenter(createSelectionCards(stage));

        Scene scene = new Scene(root, 1200, 750);
        addTheme(scene);
        return scene;
    }

    private BorderPane createHeader(Stage stage) {
        Button backButton = new Button("< Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(event -> {
            LoginSelectionPage loginSelectionPage = new LoginSelectionPage();
            stage.setScene(loginSelectionPage.createScene(stage));
        });

        Label brand = new Label("SIMHASTHA CONNECT");
        brand.getStyleClass().add("page-brand");

        Label title = new Label("Business & Partner");
        title.getStyleClass().add("page-heading");

        Label subtitle = new Label("Select your service role");
        subtitle.getStyleClass().add("page-subtitle");

        VBox titleBox = new VBox(5, brand, title, subtitle, createOrnamentLine());
        titleBox.setAlignment(Pos.CENTER);

        BorderPane header = new BorderPane();
        header.setLeft(backButton);
        header.setCenter(titleBox);
        header.setPadding(new Insets(24, 38, 8, 38));
        return header;
    }

    private HBox createOrnamentLine() {
        Line left = new Line(0, 0, 82, 0);
        left.getStyleClass().add("decor-line");

        Circle dot = new Circle(4);
        dot.getStyleClass().add("decor-dot");

        Line right = new Line(0, 0, 82, 0);
        right.getStyleClass().add("decor-line");

        HBox line = new HBox(12, left, dot, right);
        line.setAlignment(Pos.CENTER);
        return line;
    }

    private HBox createSelectionCards(Stage stage) {
        VBox localBusiness = createCard(
                "LOCAL BUSINESS",
                "For shops, accommodation providers, food services, puja services and other local businesses.",
                "BUSINESS LOGIN / REGISTER");
        Button businessButton = (Button) localBusiness.getChildren().get(localBusiness.getChildren().size() - 1);
        businessButton.setOnAction(event -> {
            BusinessAuthPage businessAuthPage = new BusinessAuthPage();
            stage.setScene(businessAuthPage.createScene(stage));
        });

        VBox transportOperator = createCard(
                "TRANSPORT OPERATOR",
                "For bus operators managing buses, routes, timings and fare information.",
                "OPERATOR LOGIN / REGISTER");
        Button operatorButton = (Button) transportOperator.getChildren().get(transportOperator.getChildren().size() - 1);
        operatorButton.setOnAction(event -> {
            OperatorAuthPage operatorAuthPage = new OperatorAuthPage();
            stage.setScene(operatorAuthPage.createScene(stage));
        });

        HBox cards = new HBox(24, localBusiness, transportOperator);
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(30, 44, 70, 44));
        HBox.setHgrow(localBusiness, Priority.ALWAYS);
        HBox.setHgrow(transportOperator, Priority.ALWAYS);
        return cards;
    }

    private VBox createCard(String titleText, String descriptionText, String buttonText) {
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
        card.setMinWidth(360);
        card.setMaxWidth(450);
        card.setMinHeight(250);
        return card;
    }

    private void addTheme(Scene scene) {
        URL cssUrl = getClass().getResource("/css/simhastha-theme.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }
}
