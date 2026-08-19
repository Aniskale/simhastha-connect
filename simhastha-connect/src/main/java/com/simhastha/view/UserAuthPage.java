package com.simhastha.view;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UserAuthPage {

    private static final Map<String, String> registeredUsers = new HashMap<>();

    private VBox formSlot;
    private Stage currentStage;

    public Scene createScene(Stage stage) {
        currentStage = stage;

        BorderPane page = new BorderPane();
        page.getStyleClass().add("themed-content-page");
        page.setTop(AppUi.createHeader(stage, "User Authentication", "Login or create your pilgrim account", () -> {
            LoginSelectionPage loginSelectionPage = new LoginSelectionPage();
            stage.setScene(loginSelectionPage.createScene(stage));
        }));

        formSlot = new VBox();
        formSlot.setAlignment(Pos.CENTER);
        showLoginForm();
        StackPane center = new StackPane(createSplitShell());
        center.setPadding(new Insets(14, 24, 42, 24));
        page.setCenter(center);

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        return AppUi.createScene(root, this);
    }

    private HBox createSplitShell() {
        StackPane visual = createAuthVisualPanel(
                "\u0965 \u0950 \u0928\u092E\u0903 \u0936\u093F\u0935\u093E\u092F \u0965",
                "Welcome Back!",
                "Login to continue your Simhastha journey",
                "/images/ramkund_sunrise.jpg",
                new String[][] {
                        { "\uE77B", "Personalized Experience", "Get services tailored for your visit" },
                        { "\uE789", "Real-time Alerts & Updates", "Important updates at your fingertips" },
                        { "\uE8A7", "Quick Access to Services", "All essential modules in one place" },
                        { "\uE72E", "Secure & Reliable", "Your data is safe and protected" }
                });

        HBox shell = new HBox(0, visual, formSlot);
        shell.getStyleClass().addAll("auth-split-shell", "user-auth-shell");
        shell.setAlignment(Pos.CENTER);
        HBox.setHgrow(formSlot, Priority.ALWAYS);
        shell.setMaxWidth(900);
        shell.setMaxHeight(500);

        return shell;
    }

    private void showLoginForm() {
        Button loginTab = tabButton("LOGIN", true, this::showLoginForm);
        Button createTab = tabButton("CREATE ACCOUNT", false, this::showCreateAccountForm);
        HBox tabs = tabs(loginTab, createTab);

        TextField emailMobile = AppUi.textField("Email / Mobile");
        PasswordField password = AppUi.passwordField("Password");

        Button loginButton = primaryButton("LOGIN");
        loginButton.setOnAction(event -> {
            String userId = emailMobile.getText().trim();
            String userPassword = password.getText().trim();
            if (AppDataStore.isAdmin(userId, userPassword)) {
                AdminDashboardPage adminDashboardPage = new AdminDashboardPage();
                currentStage.setScene(adminDashboardPage.createScene(currentStage));
            } else {
                DashboardPage dashboardPage = new DashboardPage();
                currentStage.setScene(dashboardPage.createScene(currentStage));
            }
        });

        Button forgotButton = linkButton("Forgot Password?", () -> showInfo("Forgot Password",
                "Password recovery will be added later."));
        Button createAccount = linkButton("Don't have an account? Create Account", this::showCreateAccountForm);

        replaceForm(createFormCard("Pilgrim / User Login", tabs, emailMobile, password, loginButton, forgotButton,
                createAccount));
    }

    private void showCreateAccountForm() {
        Button loginTab = tabButton("LOGIN", false, this::showLoginForm);
        Button createTab = tabButton("CREATE ACCOUNT", true, this::showCreateAccountForm);
        HBox tabs = tabs(loginTab, createTab);

        TextField fullName = AppUi.textField("Full Name");
        TextField mobile = AppUi.textField("Mobile Number");
        TextField email = AppUi.textField("Email");
        PasswordField password = AppUi.passwordField("Password");
        PasswordField confirmPassword = AppUi.passwordField("Confirm Password");

        Button createButton = primaryButton("CREATE ACCOUNT");
        createButton.setOnAction(event -> {
            if (isEmpty(fullName) || isEmpty(email) || isEmpty(mobile) || isEmpty(password) || isEmpty(confirmPassword)) {
                showInfo("Validation", "Please fill all fields before creating an account.");
            } else if (!password.getText().trim().equals(confirmPassword.getText().trim())) {
                showInfo("Validation", "Password and Confirm Password must match.");
            } else if (registeredUsers.containsKey(email.getText().trim().toLowerCase())
                    || registeredUsers.containsKey(mobile.getText().trim().toLowerCase())) {
                showInfo("Validation", "This email or mobile number is already registered.");
            } else {
                registeredUsers.put(email.getText().trim().toLowerCase(), password.getText().trim());
                registeredUsers.put(mobile.getText().trim().toLowerCase(), password.getText().trim());
                showInfo("Account Created", "Registration successful. You can login now.");
                showLoginForm();
            }
        });

        Button loginLink = linkButton("Already registered? Login", this::showLoginForm);
        ScrollPane formScroll = new ScrollPane(createFormCard("Create Pilgrim Account", tabs, fullName, mobile, email,
                password, confirmPassword, createButton, loginLink));
        formScroll.getStyleClass().add("form-card-scroll");
        formScroll.setFitToWidth(true);
        formScroll.setMaxHeight(455);
        replaceNode(formScroll);
    }

    private VBox createFormCard(String titleText, HBox tabs, javafx.scene.Node... fields) {
        Label title = new Label(titleText);
        title.getStyleClass().add("form-title");

        VBox card = new VBox(13);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(400);
        card.getChildren().addAll(title, tabs);
        card.getChildren().addAll(fields);
        return card;
    }

    private StackPane createAuthVisualPanel(String mantraText, String titleText, String subtitleText, String imagePath,
            String[][] featuresData) {
        ImageView background = createImage(imagePath, 360, 500);
        background.getStyleClass().add("auth-visual-image");

        Label mantra = new Label(mantraText);
        mantra.getStyleClass().add("auth-mantra");

        Label title = new Label(titleText);
        title.getStyleClass().add("auth-visual-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("auth-visual-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(13);
        for (String[] feature : featuresData) {
            features.getChildren().add(createFeatureLine(feature[0], feature[1], feature[2]));
        }
        features.setPadding(new Insets(18, 0, 0, 0));

        VBox content = new VBox(9, mantra, title, subtitle, features);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(30));
        content.setMaxWidth(335);

        StackPane panel = new StackPane(background, content);
        panel.getStyleClass().add("auth-visual-panel");
        panel.setPrefSize(360, 500);
        return panel;
    }

    private HBox createFeatureLine(String iconText, String titleText, String detailText) {
        Label icon = AppUi.symbolIcon(iconText, "auth-feature-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("auth-feature-title");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("auth-feature-detail");
        detail.setWrapText(true);

        HBox row = new HBox(10, icon, new VBox(2, title, detail));
        row.getStyleClass().add("feature-row");
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private Button tabButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(active ? "segment-button-active" : "segment-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private HBox tabs(Button... buttons) {
        HBox tabs = new HBox(buttons);
        tabs.getStyleClass().add("segment-box");
        tabs.setAlignment(Pos.CENTER_LEFT);
        return tabs;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private Button linkButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("text-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void replaceForm(VBox form) {
        replaceNode(form);
    }

    private void replaceNode(javafx.scene.Node node) {
        formSlot.getChildren().setAll(node);
        formSlot.setPadding(new Insets(28, 34, 28, 34));
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

    private boolean isEmpty(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
