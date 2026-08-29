package com.simhastha.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BusinessPartnerPage {

    public Scene createScene(Stage stage) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("themed-content-page");
        page.setTop(AppUi.createHeader(stage, "Business & Partner", "Select your service role", () -> {
            LoginSelectionPage loginSelectionPage = new LoginSelectionPage();
            NavigationUtil.navigate(stage, loginSelectionPage.createScene(stage));
        }));
        page.setCenter(createSelectionCards(stage));

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        return AppUi.createScene(root, this);
    }

    private HBox createSelectionCards(Stage stage) {
        VBox localBusiness = createCard(
                "\uE719",
                "LOCAL BUSINESS",
                "For shops, accommodation providers, food services, puja services and other local businesses.",
                "BUSINESS LOGIN / REGISTER",
                "business-role-card",
                new String[] { "Food, stay, puja and shops", "Pilgrim-facing service profile", "Ready for verified business onboarding" });
        Button businessButton = (Button) localBusiness.getChildren().get(localBusiness.getChildren().size() - 1);
        businessButton.setOnAction(event -> {
            BusinessAuthPage businessAuthPage = new BusinessAuthPage();
            NavigationUtil.navigate(stage, businessAuthPage.createScene(stage));
        });

        VBox transportOperator = createCard(
                "\uE806",
                "TRANSPORT OPERATOR",
                "For bus operators managing buses, routes, timings and fare information.",
                "OPERATOR LOGIN / REGISTER",
                "transport-role-card",
                new String[] { "Routes and timetable support", "Vehicle/service information", "Operator-ready registration flow" });
        Button operatorButton = (Button) transportOperator.getChildren().get(transportOperator.getChildren().size() - 1);
        operatorButton.setOnAction(event -> {
            OperatorAuthPage operatorAuthPage = new OperatorAuthPage();
            NavigationUtil.navigate(stage, operatorAuthPage.createScene(stage));
        });

        HBox cards = new HBox(26, localBusiness, transportOperator);
        cards.getStyleClass().add("business-role-grid");
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(18, 76, 80, 76));
        HBox.setHgrow(localBusiness, Priority.ALWAYS);
        HBox.setHgrow(transportOperator, Priority.ALWAYS);
        return cards;
    }

    private VBox createCard(String iconText, String titleText, String descriptionText, String buttonText, String variant,
            String[] featureTexts) {
        Label icon = AppUi.symbolIcon(iconText, "portal-card-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("portal-title");
        title.setWrapText(true);

        Label description = new Label(descriptionText);
        description.getStyleClass().add("description-text");
        description.setWrapText(true);

        Button button = new Button(buttonText);
        button.getStyleClass().add("primary-button");
        button.setGraphic(AppUi.symbolIcon("\uE72A", "button-icon"));
        button.setContentDisplay(ContentDisplay.RIGHT);
        button.setGraphicTextGap(10);
        button.setMaxWidth(Double.MAX_VALUE);

        VBox featureList = new VBox(8);
        featureList.getStyleClass().add("role-feature-list");
        for (String featureText : featureTexts) {
            featureList.getChildren().add(createFeatureRow(featureText));
        }

        VBox card = new VBox(16, icon, title, description, featureList, button);
        card.getStyleClass().addAll("portal-card", "role-card", variant);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(395);
        card.setMaxWidth(470);
        card.setMinHeight(330);
        return card;
    }

    private HBox createFeatureRow(String text) {
        Label icon = AppUi.symbolIcon("\uE73E", "role-feature-icon");

        Label label = new Label(text);
        label.getStyleClass().add("role-feature-text");
        label.setWrapText(true);

        HBox row = new HBox(9, icon, label);
        row.getStyleClass().add("role-feature-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
