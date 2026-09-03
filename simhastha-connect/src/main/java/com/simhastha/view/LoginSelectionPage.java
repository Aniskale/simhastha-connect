package com.simhastha.view;

import java.net.URL;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class LoginSelectionPage {

    public Scene createScene(Stage stage) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("themed-content-page");
        page.setTop(AppUi.createHeader(stage, "Choose Your Portal", "Continue your Simhastha journey", () -> {
            WelcomePage welcomePage = new WelcomePage();
            NavigationUtil.navigate(stage, welcomePage.createScene(stage));
        }));
        page.setCenter(scroll(createPortalOptions(stage)));

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        return AppUi.createScene(root, this);
    }

    private VBox createPortalOptions(Stage stage) {
        VBox userCard = createPortalCard(
                "\uE77B",
                "PILGRIM / USER",
                "Access transport, ghats, snan information, events, accommodation, puja services, emergency assistance and Lost & Found.",
                "ENTER USER PORTAL",
                "portal-card-user");
        Button userButton = (Button) userCard.getChildren().get(userCard.getChildren().size() - 1);
        userButton.setOnAction(event -> {
            UserAuthPage userAuthPage = new UserAuthPage();
            NavigationUtil.navigate(stage, userAuthPage.createScene(stage));
        });

        VBox businessCard = createPortalCard(
                "\uE821",
                "BUSINESS & PARTNER",
                "For local businesses, service providers and transport operators participating in the Simhastha ecosystem.",
                "BUSINESS & PARTNER PORTAL",
                "portal-card-partner");
        Button businessButton = (Button) businessCard.getChildren().get(businessCard.getChildren().size() - 1);
        businessButton.setOnAction(event -> {
            BusinessPartnerPage businessPartnerPage = new BusinessPartnerPage();
            NavigationUtil.navigate(stage, businessPartnerPage.createScene(stage));
        });

        FlowPane showcase = new FlowPane(28, 24, userCard, businessCard);
        showcase.getStyleClass().add("portal-showcase");
        showcase.setAlignment(Pos.CENTER);
        HBox.setHgrow(userCard, Priority.ALWAYS);
        HBox.setHgrow(businessCard, Priority.ALWAYS);

        VBox cardArea = new VBox(20, showcase, createTrustStrip());
        cardArea.setAlignment(Pos.CENTER);
        cardArea.setPadding(new Insets(18, 44, 30, 44));
        return cardArea;
    }

    private VBox createPortalCard(String iconText, String titleText, String descriptionText, String buttonText,
            String variantClass) {
        Label icon = AppUi.symbolIcon(iconText, "portal-card-icon");
        StackPane iconShell = new StackPane(icon);
        iconShell.getStyleClass().add("portal-card-icon-shell");

        Label title = new Label(titleText);
        title.getStyleClass().add("portal-title");
        title.setWrapText(true);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(390);

        Label description = new Label(descriptionText);
        description.getStyleClass().add("description-text");
        description.setWrapText(true);
        description.setAlignment(Pos.CENTER);
        description.setMaxWidth(430);

        HBox features = createPortalFeatureStrip(variantClass);

        Button button = new Button(buttonText);
        button.getStyleClass().add("primary-button");
        button.setGraphic(AppUi.symbolIcon("\uE72A", "button-icon"));
        button.setContentDisplay(ContentDisplay.RIGHT);
        button.setGraphicTextGap(10);
        button.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(18, iconShell, title, AppUi.createOrnamentLine(), description, features, AppUi.spacer(), button);
        card.getStyleClass().addAll("portal-card", variantClass);
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(420);
        card.setPrefWidth(520);
        card.setMaxWidth(560);
        card.setMinHeight(390);
        return card;
    }

    private HBox createPortalFeatureStrip(String variantClass) {
        boolean partner = variantClass.contains("partner");
        HBox features = new HBox(0,
                portalFeature(partner ? "\uE807" : "\uE707", partner ? "Business Directory" : "Ghats & Snan"),
                portalFeature(partner ? "\uE716" : "\uE806", partner ? "Service Providers" : "Transport Info"),
                portalFeature(partner ? "\uE7C1" : "\uEC29", partner ? "Transport Operators" : "Puja Services"),
                portalFeature(partner ? "\uE789" : "\uE95E", partner ? "Official Updates" : "Emergency Help"));
        features.getStyleClass().add("portal-feature-strip");
        features.setAlignment(Pos.CENTER);
        return features;
    }

    private VBox portalFeature(String iconText, String titleText) {
        Label icon = AppUi.symbolIcon(iconText, "portal-feature-icon");
        Label title = new Label(titleText);
        title.getStyleClass().add("portal-feature-title");
        title.setWrapText(true);
        title.setMaxWidth(90);
        VBox feature = new VBox(5, icon, title);
        feature.getStyleClass().add("portal-feature-item");
        feature.setAlignment(Pos.CENTER);
        HBox.setHgrow(feature, Priority.ALWAYS);
        return feature;
    }

    private HBox createTrustStrip() {
        HBox strip = new HBox(26,
                createTrustItem("\uE72E", "Trusted & Secure", "Protected login and safe data handling"),
                createTrustItem("\uE789", "Official Updates", "Ready for verified live information"),
                createTrustItem("\uE717", "24/7 Support", "Emergency and help modules easy to reach"));
        strip.getStyleClass().add("portal-trust-strip");
        strip.setAlignment(Pos.CENTER);
        return strip;
    }

    private HBox createTrustItem(String iconText, String titleText, String detailText) {
        Label icon = AppUi.symbolIcon(iconText, "portal-trust-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("portal-trust-title");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("portal-trust-detail");
        detail.setWrapText(true);

        VBox text = new VBox(3, title, detail);
        HBox item = new HBox(10, icon, text);
        item.getStyleClass().add("portal-trust-item");
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private ImageView createImage(String path, double width, double height) {
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

    private ScrollPane scroll(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }
}
