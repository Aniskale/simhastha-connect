package com.simhastha.view;

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

public class BusinessAuthPage {

    private static final Map<String, BusinessAccount> registeredBusinesses = new HashMap<>();

    private VBox formSlot;

    public Scene createScene(Stage stage) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("themed-content-page");
        page.setTop(AppUi.createHeader(stage, "Business Login / Registration",
                "For local services in the Simhastha ecosystem", () -> {
                    BusinessPartnerPage businessPartnerPage = new BusinessPartnerPage();
                    stage.setScene(businessPartnerPage.createScene(stage));
                }));

        formSlot = new VBox();
        formSlot.setAlignment(Pos.CENTER);
        showLoginForm();
        StackPane center = new StackPane(createSplitShell());
        center.setPadding(new Insets(18, 24, 36, 24));
        page.setCenter(center);

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        return AppUi.createScene(root, this);
    }

    private HBox createSplitShell() {
        StackPane visual = createVisualPanel();
        HBox shell = new HBox(0, visual, formSlot);
        shell.getStyleClass().addAll("auth-split-shell", "business-auth-shell");
        shell.setAlignment(Pos.CENTER);
        shell.setMaxWidth(950);
        shell.setMaxHeight(560);
        HBox.setHgrow(formSlot, Priority.ALWAYS);
        return shell;
    }

    private StackPane createVisualPanel() {
        ImageView background = createImage("/images/welcome-light.png", 380, 520);
        background.getStyleClass().add("auth-visual-image");

        Label icon = AppUi.symbolIcon("\uE719", "auth-large-icon");
        Label title = new Label("Local Business Portal");
        title.getStyleClass().add("auth-visual-title");

        Label subtitle = new Label("Connect your services with Simhastha pilgrims");
        subtitle.getStyleClass().add("auth-visual-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(13,
                feature("\uE8D4", "Food & Prasadam", "Show trusted local services"),
                feature("\uE809", "Stay & Accommodation", "Help pilgrims find support"),
                feature("\uEC29", "Puja Services", "Make religious services discoverable"));
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
        Button registerTab = tabButton("CREATE BUSINESS ACCOUNT", false, this::showRegisterForm);
        HBox tabs = tabs(loginTab, registerTab);

        TextField emailMobile = AppUi.textField("Business Email");
        PasswordField password = AppUi.passwordField("Password");

        Button loginButton = primaryButton("LOGIN");
        loginButton.setOnAction(event -> {
            String userId = emailMobile.getText().trim().toLowerCase();
            String userPassword = password.getText().trim();

            if (userId.isEmpty() || userPassword.isEmpty()) {
                showInfo("Validation", "Please enter Business Email and Password.");
            } else if (AuthService.isFirebaseEnabled()) {
                runAuth(loginButton, AuthService.login(userId, userPassword, "business"));
            } else if (!registeredBusinesses.containsKey(userId)) {
                showInfo("Login Failed", "Business account not found. Please create an account first.");
            } else if (!registeredBusinesses.get(userId).password.equals(userPassword)) {
                showInfo("Login Failed", "Incorrect password. Please try again.");
            } else {
                BusinessOwnerDashboardPage dashboardPage = new BusinessOwnerDashboardPage(registeredBusinesses.get(userId));
                ((Stage) loginButton.getScene().getWindow()).setScene(dashboardPage.createScene((Stage) loginButton.getScene().getWindow()));
            }
        });

        Button forgotButton = linkButton("Forgot Password?", () -> sendReset(emailMobile));
        replaceNode(createFormCard("Local Business Login", tabs, emailMobile, password, loginButton, forgotButton));
    }

    private void showRegisterForm() {
        Button loginTab = tabButton("LOGIN", false, this::showLoginForm);
        Button registerTab = tabButton("CREATE BUSINESS ACCOUNT", true, this::showRegisterForm);
        HBox tabs = tabs(loginTab, registerTab);

        TextField businessName = AppUi.textField("Business Name");
        TextField ownerName = AppUi.textField("Owner / Contact Person");
        ComboBox<String> businessType = new ComboBox<>();
        businessType.getItems().addAll(
                "Food & Prasadam",
                "Tea / Snacks / Water",
                "Accommodation / Hotel",
                "Dharamshala",
                "Tent / Camp Stay",
                "Puja Service",
                "Pandit / Ritual Service",
                "Religious Items Shop",
                "General Store",
                "Medical / Pharmacy",
                "Ambulance / First Aid Support",
                "Parking Service",
                "Cloakroom / Locker",
                "Mobile Charging",
                "Guide / Information Desk",
                "Donation / NGO Service",
                "Photography / Printing",
                "Sanitation / Cleaning Service",
                "Local Service",
                "Other");
        businessType.setPromptText("Business Category");
        businessType.getStyleClass().add("input-combo");
        businessType.setMaxWidth(Double.MAX_VALUE);
        businessType.setVisibleRowCount(7);
        TextField mobile = AppUi.textField("Mobile Number");
        TextField email = AppUi.textField("Email");
        TextField location = AppUi.textField("Location");
        PasswordField password = AppUi.passwordField("Password");
        PasswordField confirmPassword = AppUi.passwordField("Confirm Password");

        Button registerButton = primaryButton("REGISTER BUSINESS");
        registerButton.setOnAction(event -> {
            if (isEmpty(ownerName) || isEmpty(businessName) || businessType.getValue() == null || isEmpty(mobile)
                    || isEmpty(email) || isEmpty(location) || isEmpty(password) || isEmpty(confirmPassword)) {
                showInfo("Validation", "Please fill all fields before registering the business.");
            } else if (!password.getText().trim().equals(confirmPassword.getText().trim())) {
                showInfo("Validation", "Password and Confirm Password must match.");
            } else if (registeredBusinesses.containsKey(email.getText().trim().toLowerCase())
                    || registeredBusinesses.containsKey(mobile.getText().trim().toLowerCase())) {
                showInfo("Validation", "This email or mobile number is already registered.");
            } else {
                BusinessAccount account = new BusinessAccount(
                        businessName.getText().trim(),
                        ownerName.getText().trim(),
                        businessType.getValue(),
                        mobile.getText().trim(),
                        email.getText().trim(),
                        location.getText().trim(),
                        password.getText().trim());
                if (AuthService.isFirebaseEnabled()) {
                    registerButton.setDisable(true);
                    registerButton.setText("SUBMITTING...");
                    AuthService.registerBusiness(account, password.getText().trim())
                            .whenComplete((result, error) -> Platform.runLater(() -> {
                                registerButton.setDisable(false);
                                registerButton.setText("REGISTER BUSINESS");
                                showInfo(error == null ? "Business Registered" : "Registration Failed",
                                        result == null ? "Registration failed." : result.message());
                                showLoginForm();
                            }));
                } else {
                    registeredBusinesses.put(account.email.toLowerCase(), account);
                    registeredBusinesses.put(account.mobile.toLowerCase(), account);
                    AppDataStore.requestApproval("Business Registration",
                            account.businessName,
                            account.category + " | " + account.location + " | " + account.mobile,
                            "business");
                    showInfo("Business Registered",
                            "Request sent to Admin Dashboard. After approval, this business appears in the user Business page.");
                    showLoginForm();
                }
            }
        });

        GridPane fields = twoColumnFields(
                businessName, ownerName,
                businessType, mobile,
                email, location,
                password, confirmPassword);
        replaceNode(createFormCard("Create Business Account", tabs, fields, registerButton));
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
        AuthService.resetPassword(email).thenAccept(message -> Platform.runLater(() -> showInfo("Forgot Password", message)));
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
            openDashboardFor(stage, result.user());
        }));
    }

    private void openDashboardFor(Stage stage, AppSession.User user) {
        switch (user.role()) {
            case "admin" -> stage.setScene(new AdminDashboardPage().createScene(stage));
            case "business" -> stage.setScene(new BusinessOwnerDashboardPage(
                    new BusinessAccount(user.displayName(), user.displayName(), "Business", "", user.email(), "", ""))
                    .createScene(stage));
            case "transport_operator" -> stage.setScene(new OperatorDashboardPage(
                    new OperatorAuthPage.OperatorAccount(user.displayName(), user.displayName(), "", user.email(),
                            "Transport", "")).createScene(stage));
            case "user" -> stage.setScene(new DashboardPage().createScene(stage));
            default -> showInfo("Login Failed", "Account role is not valid.");
        }
    }

    private ImageView createImage(String path, double width, double height) {
        URL imageUrl = getClass().getResource(path);
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

    private void applyCoverViewport(ImageView imageView, Image image, double width, double height) {
        double scale = Math.max(width / image.getWidth(), height / image.getHeight());
        double cropWidth = width / scale;
        double cropHeight = height / scale;
        double x = Math.max(0, (image.getWidth() - cropWidth) * 0.70);
        double y = Math.max(0, (image.getHeight() - cropHeight) * 0.45);
        imageView.setViewport(new Rectangle2D(x, y, cropWidth, cropHeight));
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

    public static class BusinessAccount {
        public final String businessName;
        public final String ownerName;
        public final String category;
        public final String mobile;
        public final String email;
        public final String location;
        public final List<String> services = new ArrayList<>();
        public final List<String> bookings = new ArrayList<>();
        private final String password;

        public BusinessAccount(String businessName, String ownerName, String category, String mobile, String email,
                String location, String password) {
            this.businessName = businessName;
            this.ownerName = ownerName;
            this.category = category;
            this.mobile = mobile;
            this.email = email;
            this.location = location;
            this.password = password;
            services.add(category + " - Standard Listing");
            services.add("Festival visitor support");
            bookings.add("2 pending inquiries");
            bookings.add("1 confirmed service request");
        }
    }
}
