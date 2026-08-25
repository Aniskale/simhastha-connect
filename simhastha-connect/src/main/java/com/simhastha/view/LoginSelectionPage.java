package com.simhastha.view;

import java.net.URL;

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
            stage.setScene(welcomePage.createScene(stage));
        }));
        page.setCenter(createPortalOptions(stage));

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
            stage.setScene(userAuthPage.createScene(stage));
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
            stage.setScene(businessPartnerPage.createScene(stage));
        });

        StackPane visual = createScenicFeature();

        HBox showcase = new HBox(24, userCard, visual, businessCard);
        showcase.getStyleClass().add("portal-showcase");
        showcase.setAlignment(Pos.CENTER);
        HBox.setHgrow(userCard, Priority.ALWAYS);
        HBox.setHgrow(businessCard, Priority.ALWAYS);

        VBox cardArea = new VBox(20, showcase, createTrustStrip());
        cardArea.setAlignment(Pos.CENTER);
        cardArea.setPadding(new Insets(18, 44, 34, 44));
        return cardArea;
    }

    private VBox createPortalCard(String iconText, String titleText, String descriptionText, String buttonText,
            String variantClass) {
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

        VBox card = new VBox(16, icon, title, description, AppUi.spacer(), button);
        card.getStyleClass().addAll("portal-card", variantClass);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(350);
        card.setMaxWidth(430);
        card.setMinHeight(292);
        return card;
    }

    private StackPane createScenicFeature() {
        ImageView image = createImage("/images/ramkund_sunrise.jpg", 300, 310);
        image.getStyleClass().add("portal-scenic-image");

        Rectangle clip = new Rectangle(300, 310);
        clip.setArcWidth(160);
        clip.setArcHeight(160);
        image.setClip(clip);

        Label caption = new Label("Ramkund \u2022 Godavari \u2022 Nashik");
        caption.getStyleClass().add("portal-visual-caption");
        StackPane.setAlignment(caption, Pos.BOTTOM_CENTER);
        StackPane.setMargin(caption, new Insets(0, 18, 18, 18));

        StackPane visual = new StackPane(image, caption);
        visual.getStyleClass().add("portal-center-visual");
        visual.setMinSize(318, 330);
        visual.setMaxSize(340, 330);
        return visual;
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
}
