package com.simhastha.view;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class NotificationCenter {

    private NotificationCenter() {
    }

    public record NotificationItem(String id, String title, String detail, String category, String severity,
            String target) {
    }

    public static StackPane bell(int unreadCount, Runnable openAction) {
        Button button = new Button();
        button.setGraphic(AppUi.symbolIcon(AppUi.notificationBellGlyph(), "notification-bell-icon"));
        button.getStyleClass().add("notification-bell-button");
        button.setOnAction(event -> openAction.run());

        StackPane wrapper = new StackPane(button);
        wrapper.setMinSize(44, 44);
        wrapper.setPrefSize(44, 44);
        if (unreadCount > 0) {
            Label badge = new Label(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            badge.getStyleClass().add("notification-badge");
            wrapper.getChildren().add(badge);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(1, 1, 0, 0));
        }
        return wrapper;
    }

    public static void show(Window owner, String title, String subtitle, List<NotificationItem> items,
            Set<String> readIds, Consumer<String> openTarget) {
        if (owner == null) {
            return;
        }
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox rows = new VBox(9);
        rows.getStyleClass().add("notification-list");
        if (items.isEmpty()) {
            rows.getChildren().add(emptyState());
        } else {
            for (NotificationItem item : items) {
                rows.getChildren().add(row(item, readIds.contains(item.id()), () -> {
                    readIds.add(item.id());
                    popup.hide();
                    if (item.target() != null && !item.target().isBlank()) {
                        openTarget.accept(item.target());
                    }
                }));
            }
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.getStyleClass().add("notification-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(430);

        long unread = items.stream().filter(item -> !readIds.contains(item.id())).count();
        Button close = new Button();
        close.setGraphic(AppUi.symbolIcon("\uE711", "notification-close-icon"));
        close.getStyleClass().add("notification-icon-button");
        close.setOnAction(event -> popup.hide());

        Button readAll = new Button("Mark all read");
        readAll.getStyleClass().add("notification-text-button");
        readAll.setDisable(items.isEmpty() || unread == 0);
        readAll.setOnAction(event -> {
            items.forEach(item -> readIds.add(item.id()));
            popup.hide();
        });

        HBox header = new HBox(10, new VBox(2, label(title, "notification-title"),
                label(subtitle, "notification-subtitle")), spacer(), close);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox meta = new HBox(10, pill(unread + " unread"), pill(items.size() + " total"), spacer(), readAll);
        meta.setAlignment(Pos.CENTER_LEFT);

        VBox drawer = new VBox(13, header, meta, scroll);
        drawer.getStyleClass().add("notification-drawer");
        if (owner instanceof Stage stage && stage.getScene() != null) {
            drawer.getStylesheets().setAll(stage.getScene().getStylesheets());
            if (stage.getScene().getRoot().getStyleClass().contains("theme-dark")) {
                drawer.getStyleClass().add("theme-dark");
            }
        }
        drawer.setPrefWidth(390);
        drawer.setMinWidth(390);
        drawer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            }
        });

        popup.getContent().add(drawer);
        double x = owner.getX() + owner.getWidth() - 414;
        double y = owner.getY() + 72;
        popup.show(owner, Math.max(owner.getX() + 12, x), y);
        drawer.requestFocus();
    }

    private static Node row(NotificationItem item, boolean read, Runnable openAction) {
        Label dot = new Label();
        dot.getStyleClass().addAll("notification-dot", severityClass(item.severity()));
        Label title = label(item.title(), "notification-row-title");
        Label detail = label(item.detail(), "notification-row-detail");
        Label category = label(item.category(), "notification-category");
        VBox copy = new VBox(3, new HBox(8, title, category), detail);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Button open = new Button("Open");
        open.getStyleClass().add("notification-open-button");
        open.setOnAction(event -> {
            event.consume();
            openAction.run();
        });

        HBox row = new HBox(10, dot, copy, open);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(read ? "notification-row-read" : "notification-row");
        row.setOnMouseClicked(event -> openAction.run());
        return row;
    }

    private static VBox emptyState() {
        return new VBox(4, label("All caught up", "notification-empty-title"),
                label("Important booking, admin and operational updates will appear here.",
                        "notification-row-detail"));
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private static Label pill(String text) {
        return label(text, "notification-summary-pill");
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static String severityClass(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase()) {
            case "critical", "urgent", "failed" -> "notification-dot-critical";
            case "warning", "pending" -> "notification-dot-warning";
            case "success", "confirmed", "approved" -> "notification-dot-success";
            default -> "notification-dot-info";
        };
    }
}
