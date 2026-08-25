package com.simhastha.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public final class AppUi {

    private AppUi() {
    }

    public static Scene createScene(ThemedBackgroundPane root, Object owner) {
        Scene scene = new Scene(root, 1200, 680);
        ThemeManager.addTheme(scene, owner);
        ThemeManager.addListener(() -> ThemeManager.applyTo(root));
        return scene;
    }

    public static BorderPane createHeader(Stage stage, String titleText, String subtitleText, Runnable backAction) {
        Button backButton = new Button("\u2190  Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(event -> backAction.run());

        Label brand = new Label("SIMHASTHA CONNECT");
        brand.getStyleClass().add("page-brand");

        Label title = new Label(titleText);
        title.getStyleClass().add("page-heading");
        title.setWrapText(true);

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);

        VBox titleBox = new VBox(4, brand, title, subtitle, createOrnamentLine());
        titleBox.setAlignment(Pos.CENTER);

        HBox toggle = createThemeToggle();
        toggle.setAlignment(Pos.CENTER_RIGHT);

        BorderPane header = new BorderPane();
        header.getStyleClass().add("top-header");
        header.setLeft(backButton);
        header.setCenter(titleBox);
        header.setRight(toggle);
        header.setPadding(new Insets(18, 38, 8, 38));
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        BorderPane.setAlignment(toggle, Pos.TOP_RIGHT);
        return header;
    }

    public static HBox createThemeToggle() {
        Button light = new Button();
        Button dark = new Button();
        light.setGraphic(symbolIcon("\uE706", "theme-toggle-icon"));
        dark.setGraphic(symbolIcon("\uE708", "theme-toggle-icon"));
        light.getStyleClass().add("theme-toggle-button");
        dark.getStyleClass().add("theme-toggle-button");
        light.setAccessibleText("Light theme");
        dark.setAccessibleText("Dark theme");

        Runnable sync = () -> {
            light.getStyleClass().remove("theme-toggle-button-active");
            dark.getStyleClass().remove("theme-toggle-button-active");
            (ThemeManager.isDark() ? dark : light).getStyleClass().add("theme-toggle-button-active");
        };

        light.setOnAction(event -> {
            ThemeManager.setTheme(ThemeManager.AppTheme.LIGHT);
            sync.run();
        });
        dark.setOnAction(event -> {
            ThemeManager.setTheme(ThemeManager.AppTheme.DARK);
            sync.run();
        });
        ThemeManager.addListener(sync);
        sync.run();

        HBox toggle = new HBox(4, light, dark);
        toggle.getStyleClass().add("theme-toggle");
        return toggle;
    }

    public static HBox createOrnamentLine() {
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

    public static Label symbolIcon(String iconText, String styleClass) {
        Label icon = new Label(iconText);
        icon.getStyleClass().addAll("symbol-icon", styleClass);
        return icon;
    }

    public static TextField textField(String prompt) {
        TextField textField = new TextField();
        textField.setPromptText(prompt);
        textField.getStyleClass().add("input-field");
        return textField;
    }

    public static PasswordField passwordField(String prompt) {
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(prompt);
        passwordField.getStyleClass().add("input-field");
        return passwordField;
    }

    public static StackPane passwordFieldWithToggle(PasswordField passwordField) {
        TextField visibleField = new TextField();
        visibleField.promptTextProperty().bind(passwordField.promptTextProperty());
        visibleField.textProperty().bindBidirectional(passwordField.textProperty());
        visibleField.getStyleClass().add("input-field");
        visibleField.setManaged(false);
        visibleField.setVisible(false);

        passwordField.setMaxWidth(Double.MAX_VALUE);
        visibleField.setMaxWidth(Double.MAX_VALUE);

        Button toggle = new Button("\uE890");
        toggle.getStyleClass().add("password-toggle-button");
        toggle.setAccessibleText("Show password");
        toggle.setTooltip(new Tooltip("Show password"));
        toggle.setOnAction(event -> {
            TextInputControl source = visibleField.isVisible() ? visibleField : passwordField;
            int caret = source.getCaretPosition();
            boolean showPassword = !visibleField.isVisible();
            visibleField.setVisible(showPassword);
            visibleField.setManaged(showPassword);
            passwordField.setVisible(!showPassword);
            passwordField.setManaged(!showPassword);
            toggle.setText(showPassword ? "\uE8F5" : "\uE890");
            toggle.setAccessibleText(showPassword ? "Hide password" : "Show password");
            toggle.setTooltip(new Tooltip(showPassword ? "Hide password" : "Show password"));

            TextInputControl target = showPassword ? visibleField : passwordField;
            target.requestFocus();
            target.positionCaret(Math.min(caret, target.getLength()));
        });

        StackPane wrapper = new StackPane(passwordField, visibleField, toggle);
        wrapper.getStyleClass().add("password-field-wrapper");
        wrapper.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(toggle, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggle, new Insets(0, 7, 0, 0));
        return wrapper;
    }

    public static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
