package com.simhastha.view;

import com.simhastha.controller.OperatorAuthController;
import com.simhastha.service.AuthService;
import com.simhastha.util.AppNavigator;
import com.simhastha.util.NavigationUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OperatorAuthPage {

    private final OperatorAuthController controller = new OperatorAuthController();
    private static final Map<String, OperatorAccount> registeredOperators = new HashMap<>();

    private VBox formSlot;

    public Scene createScene(Stage stage) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("themed-content-page");
        page.setTop(AppUi.createHeader(stage, "Transport Operator Login / Registration",
                "For buses, routes, timings and fare information", () -> {
                    BusinessPartnerPage businessPartnerPage = new BusinessPartnerPage();
                    NavigationUtil.navigate(stage, businessPartnerPage.createScene(stage));
                }));

        formSlot = new VBox();
        formSlot.setAlignment(Pos.CENTER);
        showLoginForm();
        StackPane center = new StackPane(createSplitShell());
        center.setPadding(new Insets(18, 24, 36, 24));
        page.setCenter(scroll(center));

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        return AppUi.createScene(root, this);
    }

    private HBox createSplitShell() {
        StackPane visual = createVisualPanel();
        HBox shell = new HBox(0, visual, formSlot);
        shell.getStyleClass().addAll("auth-split-shell", "operator-auth-shell");
        shell.setAlignment(Pos.CENTER);
        shell.setMaxWidth(950);
        shell.setMaxHeight(560);
        HBox.setHgrow(formSlot, Priority.ALWAYS);
        return shell;
    }

    private StackPane createVisualPanel() {
        ImageView background = createImage("/images/welcome-light.png", 380, 520);
        background.getStyleClass().add("auth-visual-image");

        Label icon = AppUi.symbolIcon("\uE806", "auth-large-icon");
        Label title = new Label("Transport Operator Portal");
        title.getStyleClass().add("auth-visual-title");
        title.setWrapText(true);
        title.setMaxWidth(300);

        Label subtitle = new Label("Manage buses, routes, timings and fare information");
        subtitle.getStyleClass().add("auth-visual-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(13,
                feature("\uE81D", "Route Planning", "Keep transport routes organized"),
                feature("\uE787", "Timings", "Prepare schedules for pilgrim movement"),
                feature("\uE8EC", "Fare Information", "Keep service information clear"));
        features.setPadding(new Insets(18, 0, 0, 0));

        VBox content = new VBox(12, icon, title, subtitle, features);
        content.setPadding(new Insets(34));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(350);

        StackPane panel = new StackPane(background, content);
        panel.getStyleClass().add("auth-visual-panel");
        panel.setPrefSize(380, 520);
        return panel;
    }

    private void showLoginForm() {
        Button loginTab = tabButton("LOGIN", true, this::showLoginForm);
        Button registerTab = tabButton("REGISTER", false, this::showRegisterForm);
        HBox tabs = tabs(loginTab, registerTab);

        TextField emailMobile = AppUi.textField("Operator Email");
        PasswordField password = AppUi.passwordField("Password");

        Button loginButton = primaryButton("LOGIN");
        loginButton.setOnAction(event -> {
            String userId = emailMobile.getText().trim().toLowerCase();
            String userPassword = password.getText().trim();

            if (userId.isEmpty() || userPassword.isEmpty()) {
                showInfo("Validation", "Please enter Operator Email and Password.");
            } else if (controller.isFirebaseEnabled()) {
                runAuth(loginButton, controller.login(userId, userPassword));
            } else if (!registeredOperators.containsKey(userId)) {
                showInfo("Login Failed", "Operator account not found. Please register first.");
            } else if (!registeredOperators.get(userId).password.equals(userPassword)) {
                showInfo("Login Failed", "Incorrect password. Please try again.");
            } else {
                Stage currentStage = (Stage) loginButton.getScene().getWindow();
                OperatorDashboardPage dashboardPage = new OperatorDashboardPage(registeredOperators.get(userId));
                NavigationUtil.navigate(currentStage, dashboardPage.createScene(currentStage));
            }
        });

        Button forgotButton = linkButton("Forgot Password?", () -> sendReset(emailMobile));
        replaceNode(createFormCard("Transport Operator Login", tabs, emailMobile, AppUi.passwordFieldWithToggle(password),
                loginButton, forgotButton));
    }

    private void showRegisterForm() {
        Button loginTab = tabButton("LOGIN", false, this::showLoginForm);
        Button registerTab = tabButton("REGISTER", true, this::showRegisterForm);
        HBox tabs = tabs(loginTab, registerTab);

        TextField organization = AppUi.textField("Operator / Organization Name");
        TextField contactPerson = AppUi.textField("Contact Person");
        TextField mobile = AppUi.textField("Mobile Number");
        TextField email = AppUi.textField("Email");
        ComboBox<String> serviceType = new ComboBox<>();
        serviceType.getItems().addAll("Bus", "Shuttle", "Taxi", "Tempo Traveller", "Other");
        serviceType.setPromptText("Vehicle / Service Type");
        serviceType.getStyleClass().add("input-combo");
        serviceType.setMaxWidth(Double.MAX_VALUE);
        serviceType.setVisibleRowCount(5);
        PasswordField password = AppUi.passwordField("Password");
        PasswordField confirmPassword = AppUi.passwordField("Confirm Password");

        Button registerButton = primaryButton("REGISTER OPERATOR");
        registerButton.setOnAction(event -> {
            if (isEmpty(organization) || isEmpty(contactPerson) || isEmpty(mobile) || isEmpty(email)
                    || serviceType.getValue() == null || isEmpty(password) || isEmpty(confirmPassword)) {
                showInfo("Validation", "Please fill all fields before registering the operator.");
            } else if (!password.getText().trim().equals(confirmPassword.getText().trim())) {
                showInfo("Validation", "Password and Confirm Password must match.");
            } else if (registeredOperators.containsKey(email.getText().trim().toLowerCase())
                    || registeredOperators.containsKey(mobile.getText().trim().toLowerCase())) {
                showInfo("Validation", "This email or mobile number is already registered.");
            } else {
                OperatorAccount account = new OperatorAccount(
                        organization.getText().trim(),
                        contactPerson.getText().trim(),
                        mobile.getText().trim(),
                        email.getText().trim(),
                        serviceType.getValue(),
                        password.getText().trim());
                if (controller.isFirebaseEnabled()) {
                    registerButton.setDisable(true);
                    registerButton.setText("SUBMITTING...");
                    controller.registerOperator(account, password.getText().trim())
                            .whenComplete((result, error) -> Platform.runLater(() -> {
                                registerButton.setDisable(false);
                                registerButton.setText("REGISTER OPERATOR");
                                if (error != null || result == null || !result.success()) {
                                    showInfo("Registration Failed",
                                            result == null ? "Registration failed." : result.message());
                                    return;
                                }
                                showInfo("Operator Registered", "Transport operator account is active. You can login now.");
                                showLoginForm();
                            }));
                } else {
                    registeredOperators.put(account.email.toLowerCase(), account);
                    registeredOperators.put(account.mobile.toLowerCase(), account);
                    showInfo("Operator Registered",
                            "Transport operator account is active. You can login now.");
                    showLoginForm();
                }
            }
        });

        GridPane fields = twoColumnFields(
                organization, contactPerson,
                mobile, email,
                serviceType,
                AppUi.passwordFieldWithToggle(password), AppUi.passwordFieldWithToggle(confirmPassword));
        replaceNode(createFormCard("Register Transport Operator", tabs, fields, registerButton));
    }

    private VBox createFormCard(String titleText, HBox tabs, javafx.scene.Node... fields) {
        Label title = new Label(titleText);
        title.getStyleClass().add("form-title");

        VBox card = new VBox(13);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(440);
        card.getChildren().addAll(title, tabs);
        card.getChildren().addAll(fields);
        return card;
    }

    private HBox feature(String iconText, String titleText, String detailText) {
        Label icon = AppUi.symbolIcon(iconText, "auth-feature-icon");
        Label title = new Label(titleText);
        title.getStyleClass().add("auth-feature-title");
        Label detail = new Label(detailText);
        detail.getStyleClass().add("auth-feature-detail");
        detail.setWrapText(true);
        HBox row = new HBox(10, icon, new VBox(2, title, detail));
        row.getStyleClass().add("feature-row");
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
        return tabs;
    }

    private GridPane twoColumnFields(javafx.scene.Node... fields) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("auth-field-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        for (int index = 0; index < fields.length; index++) {
            javafx.scene.Node field = fields[index];
            if (field instanceof javafx.scene.control.Control control) {
                control.setMaxWidth(Double.MAX_VALUE);
            }
            grid.add(field, index % 2, index / 2);
            GridPane.setHgrow(field, Priority.ALWAYS);
        }
        return grid;
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

    private void sendReset(TextField emailField) {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showInfo("Forgot Password", "Please enter your email first.");
            return;
        }
        controller.resetPassword(email).thenAccept(message -> Platform.runLater(() -> showInfo("Forgot Password", message)));
    }

    private void replaceNode(javafx.scene.Node node) {
        formSlot.getChildren().setAll(node);
        formSlot.setPadding(new Insets(28, 34, 28, 34));
    }

    private void runAuth(Button button, java.util.concurrent.CompletableFuture<AuthService.AuthOutcome> action) {
        button.setDisable(true);
        button.setText("PLEASE WAIT...");
        action.whenComplete((result, error) -> Platform.runLater(() -> {
            button.setDisable(false);
            button.setText("LOGIN");
            if (error != null || result == null || !result.success()) {
                showInfo("Login Failed", result == null ? "Unable to login." : result.message());
                return;
            }
            Stage stage = (Stage) button.getScene().getWindow();
            AppNavigator.openDashboardFor(stage, result.user(), this::showInfo);
        }));
    }

    private ImageView createImage(String path, double width, double height) {
        URL imageUrl = AppResources.url(getClass(), path);
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        if (imageUrl != null) {
            Image image = new Image(imageUrl.toExternalForm());
            imageView.setImage(image);
            applyCoverViewport(imageView, image, width, height);
        }
        return imageView;
    }

    private ScrollPane scroll(javafx.scene.Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private void applyCoverViewport(ImageView imageView, Image image, double width, double height) {
        double scale = Math.max(width / image.getWidth(), height / image.getHeight());
        double cropWidth = width / scale;
        double cropHeight = height / scale;
        double x = Math.max(0, (image.getWidth() - cropWidth) * 0.72);
        double y = Math.max(0, (image.getHeight() - cropHeight) * 0.45);
        imageView.setViewport(new Rectangle2D(x, y, cropWidth, cropHeight));
    }

    private boolean isEmpty(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private void showInfo(String title, String message) {
        AppUi.showInfo(title, message, formSlot == null || formSlot.getScene() == null ? null : formSlot.getScene().getWindow());
    }

    public static class OperatorAccount {
        public final String organizationName;
        public final String contactPerson;
        public final String mobile;
        public final String email;
        public final String serviceType;
        public final List<String> schedules = new ArrayList<>();
        public final List<String> bookings = new ArrayList<>();
        private final String password;

        public OperatorAccount(String organizationName, String contactPerson, String mobile, String email,
                String serviceType, String password) {
            this.organizationName = organizationName;
            this.contactPerson = contactPerson;
            this.mobile = mobile;
            this.email = email;
            this.serviceType = serviceType;
            this.password = password;
            schedules.add(serviceType + " - Nashik Road to Ramkund - 07:00 AM");
            schedules.add(serviceType + " - Trimbakeshwar Route - 11:30 AM");
            bookings.add("12 seat requests pending");
            bookings.add("4 confirmed group bookings");
        }
    }
}
