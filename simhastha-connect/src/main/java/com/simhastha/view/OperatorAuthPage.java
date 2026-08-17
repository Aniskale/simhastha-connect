package com.simhastha.view;

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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class OperatorAuthPage {

    private VBox formArea;

    public Scene createScene(Stage stage) {
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
            BusinessPartnerPage businessPartnerPage = new BusinessPartnerPage();
            stage.setScene(businessPartnerPage.createScene(stage));
        });

        Label brand = new Label("SIMHASTHA CONNECT");
        brand.getStyleClass().add("page-brand");

        Label title = new Label("Transport Operator Login / Registration");
        title.getStyleClass().add("page-heading");

        Label subtitle = new Label("For buses, routes, timings and fare information");
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
        Button registerTab = new Button("REGISTER");
        loginTab.getStyleClass().add("segment-button-active");
        registerTab.getStyleClass().add("segment-button");
        registerTab.setOnAction(event -> showRegisterForm());

        HBox tabs = new HBox(loginTab, registerTab);
        tabs.getStyleClass().add("segment-box");

        TextField emailMobile = createTextField("Operator Email / Mobile");
        PasswordField password = createPasswordField("Password");

        Button loginButton = new Button("LOGIN");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> showInfo("Operator Login",
                "Transport operator authentication will be connected to Firebase later."));

        VBox form = createFormCard("Transport Operator Login", tabs, emailMobile, password, loginButton);
        replaceForm(form);
    }

    private void showRegisterForm() {
        Button loginTab = new Button("LOGIN");
        Button registerTab = new Button("REGISTER");
        loginTab.getStyleClass().add("segment-button");
        registerTab.getStyleClass().add("segment-button-active");
        loginTab.setOnAction(event -> showLoginForm());

        HBox tabs = new HBox(loginTab, registerTab);
        tabs.getStyleClass().add("segment-box");

        TextField operatorName = createTextField("Operator Name");
        TextField companyName = createTextField("Transport Company Name");
        TextField mobile = createTextField("Mobile Number");
        TextField email = createTextField("Email");
        PasswordField password = createPasswordField("Password");
        PasswordField confirmPassword = createPasswordField("Confirm Password");

        Button registerButton = new Button("REGISTER OPERATOR");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setOnAction(event -> {
            if (isEmpty(operatorName) || isEmpty(companyName) || isEmpty(mobile) || isEmpty(email) || isEmpty(password)
                    || isEmpty(confirmPassword)) {
                showInfo("Validation", "Please fill all fields before registering the operator.");
            } else {
                showInfo("Operator Registration", "Transport operator authentication will be connected to Firebase later.");
            }
        });

        VBox form = createFormCard("Register Transport Operator", tabs, operatorName, companyName, mobile, email,
                password, confirmPassword, registerButton);
        replaceForm(form);
    }

    private VBox createFormCard(String titleText, HBox tabs, javafx.scene.Node... fields) {
        Label title = new Label(titleText);
        title.getStyleClass().add("form-title");

        VBox card = new VBox(13);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(500);
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
        StackPane center = new StackPane(form);
        center.setPadding(new Insets(20, 20, 42, 20));
        formArea.getChildren().setAll(center);
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
