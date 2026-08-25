package com.simhastha.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminAuthPage {

    public Scene createScene(Stage stage) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("themed-content-page");
        page.setTop(AppUi.createHeader(stage, "Admin Login", "Secure Firebase role-based access", () -> {
            LoginSelectionPage loginSelectionPage = new LoginSelectionPage();
            stage.setScene(loginSelectionPage.createScene(stage));
        }));

        TextField email = AppUi.textField("Admin Email");
        PasswordField password = AppUi.passwordField("Password");
        Button login = new Button("LOGIN AS ADMIN");
        login.getStyleClass().add("primary-button");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(event -> {
            String userEmail = email.getText().trim();
            String userPassword = password.getText().trim();
            if (userEmail.isEmpty() || userPassword.isEmpty()) {
                showInfo("Validation", "Please enter admin email and password.");
                return;
            }
            runAdminLogin(stage, login, userEmail, userPassword);
        });

        VBox card = new VBox(13,
                label("Admin Authentication", "form-title"),
                label("Admin access is granted only after Firebase login and Firestore role verification.",
                        "description-text"),
                email,
                AppUi.passwordFieldWithToggle(password),
                login);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(36));
        page.setCenter(center);

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        return AppUi.createScene(root, this);
    }

    private void runAdminLogin(Stage stage, Button button, String email, String password) {
        button.setDisable(true);
        button.setText("PLEASE WAIT...");
        AuthService.login(email, password, "admin").whenComplete((result, error) -> Platform.runLater(() -> {
            button.setDisable(false);
            button.setText("LOGIN AS ADMIN");
            if (error != null || result == null || !result.success()) {
                showInfo("Admin Access Denied", result == null ? "Unable to login." : result.message());
                return;
            }
            AppNavigator.openDashboardFor(stage, result.user(), this::showInfo);
        }));
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
