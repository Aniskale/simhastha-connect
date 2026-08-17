package com.simhastha.view;

import java.util.HashMap;
import java.util.Map;
import java.net.URL;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class UserAuthPage {

    private static final Map<String, String> registeredUsers = new HashMap<>();

    private VBox formArea;
    private Stage currentStage;

    public Scene createScene(Stage stage) {
        currentStage = stage;

        BorderPane page = new BorderPane();
        page.getStyleClass().add("main-background");
        page.setTop(createHeader(stage));

        formArea = new VBox();
        formArea.setAlignment(Pos.CENTER);
        showLoginForm();

        ScrollPane scrollPane = new ScrollPane(formArea);
        scrollPane.getStyleClass().add("page-scroll");
        scrollPane.setFitToWidth(true);
        page.setCenter(scrollPane);

        Scene scene = new Scene(page, 1200, 750);
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

        Label title = new Label("User Authentication");
        title.getStyleClass().add("page-heading");

        Label subtitle = new Label("Login or create your pilgrim account");
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

    private void showLoginForm() {
        Button loginTab = new Button("LOGIN");
        Button createTab = new Button("CREATE ACCOUNT");
        loginTab.getStyleClass().add("segment-button-active");
        createTab.getStyleClass().add("segment-button");
        createTab.setOnAction(event -> showCreateAccountForm());

        HBox tabs = new HBox(loginTab, createTab);
        tabs.getStyleClass().add("segment-box");
        tabs.setAlignment(Pos.CENTER);

        TextField emailMobile = createTextField("Email / Mobile");
        PasswordField password = createPasswordField("Password");

        Button loginButton = new Button("LOGIN");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> {
            String userId = emailMobile.getText().trim().toLowerCase();
            String userPassword = password.getText().trim();

            if (userId.isEmpty() || userPassword.isEmpty()) {
                showInfo("Validation", "Please enter Email / Mobile and Password.");
            } else if (!registeredUsers.containsKey(userId)) {
                showInfo("Login Failed", "Account not found. Please create an account first.");
            } else if (!registeredUsers.get(userId).equals(userPassword)) {
                showInfo("Login Failed", "Incorrect password. Please try again.");
            } else {
                DashboardPage dashboardPage = new DashboardPage();
                currentStage.setScene(dashboardPage.createScene(currentStage));
            }
        });

        Button forgotButton = new Button("Forgot Password?");
        forgotButton.getStyleClass().add("text-button");
        forgotButton.setOnAction(event -> showInfo("Forgot Password", "Password recovery will be added later."));

        Button createAccount = new Button("Don't have an account? Create Account");
        createAccount.getStyleClass().add("text-button");
        createAccount.setOnAction(event -> showCreateAccountForm());

        VBox form = createFormCard("Pilgrim / User Login", tabs, emailMobile, password, loginButton, forgotButton,
                createAccount);
        replaceForm(form);
    }

    private void showCreateAccountForm() {
        Button loginTab = new Button("LOGIN");
        Button createTab = new Button("CREATE ACCOUNT");
        loginTab.getStyleClass().add("segment-button");
        createTab.getStyleClass().add("segment-button-active");
        loginTab.setOnAction(event -> showLoginForm());

        HBox tabs = new HBox(loginTab, createTab);
        tabs.getStyleClass().add("segment-box");
        tabs.setAlignment(Pos.CENTER);

        TextField fullName = createTextField("Full Name");
        TextField email = createTextField("Email");
        TextField mobile = createTextField("Mobile Number");
        PasswordField password = createPasswordField("Password");
        PasswordField confirmPassword = createPasswordField("Confirm Password");

        Button createButton = new Button("CREATE ACCOUNT");
        createButton.getStyleClass().add("primary-button");
        createButton.setMaxWidth(Double.MAX_VALUE);
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

        Button loginLink = new Button("Already registered? Login");
        loginLink.getStyleClass().add("text-button");
        loginLink.setOnAction(event -> showLoginForm());

        VBox form = createFormCard("Create Pilgrim Account", tabs, fullName, email, mobile, password, confirmPassword,
                createButton, loginLink);
        replaceForm(form);
    }

    private VBox createFormCard(String titleText, HBox tabs, javafx.scene.Node... fields) {
        Label title = new Label(titleText);
        title.getStyleClass().add("form-title");

        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(460);
        card.getChildren().addAll(title, tabs);
        card.getChildren().addAll(fields);
        return card;
    }

    private TextField createTextField(String prompt) {
        TextField textField = new TextField();
        textField.setPromptText(prompt);
        textField.getStyleClass().add("input-field");
        return textField;
    }

    private PasswordField createPasswordField(String prompt) {
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(prompt);
        passwordField.getStyleClass().add("input-field");
        return passwordField;
    }

    private boolean isEmpty(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private void replaceForm(VBox form) {
        HBox splitLayout = new HBox(0, createAuthVisualPanel(), form);
        splitLayout.getStyleClass().add("auth-split-shell");
        splitLayout.setAlignment(Pos.CENTER);

        StackPane center = new StackPane(splitLayout);
        center.setPadding(new Insets(22, 20, 42, 20));
        formArea.getChildren().setAll(center);
    }

    private StackPane createAuthVisualPanel() {
        ImageView background = createOptionalImage("/images/kumbh-background.jpg", 420, 520);
        background.getStyleClass().add("auth-visual-image");

        Label mantra = new Label("|| \u0950 \u0928\u092E\u0903 \u0936\u093F\u0935\u093E\u092F ||");
        mantra.getStyleClass().add("auth-mantra");

        Label welcome = new Label("Welcome Back!");
        welcome.getStyleClass().add("auth-visual-title");

        Label subtitle = new Label("Login to continue your Simhastha journey");
        subtitle.getStyleClass().add("auth-visual-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(16,
                createFeatureLine("Personalized Experience", "Get services tailored for your visit"),
                createFeatureLine("Real-time Alerts & Updates", "Important updates at your fingertips"),
                createFeatureLine("Quick Access to Services", "All essential modules in one place"),
                createFeatureLine("Secure & Reliable", "Your data is safe and protected"));
        features.setPadding(new Insets(24, 0, 0, 0));

        VBox content = new VBox(10, mantra, welcome, subtitle, features);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(38));
        content.setMaxWidth(390);

        StackPane panel = new StackPane(background, content);
        panel.getStyleClass().add("auth-visual-panel");
        panel.setPrefSize(420, 520);
        return panel;
    }

    private VBox createFeatureLine(String titleText, String detailText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("auth-feature-title");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("auth-feature-detail");
        detail.setWrapText(true);

        return new VBox(3, title, detail);
    }

    private ImageView createOptionalImage(String path, double width, double height) {
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void addTheme(Scene scene) {
        URL cssUrl = getClass().getResource("/css/simhastha-theme.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }
}
