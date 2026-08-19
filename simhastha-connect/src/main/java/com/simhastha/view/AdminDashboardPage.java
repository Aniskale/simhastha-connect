package com.simhastha.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminDashboardPage {

    private VBox approvalList;
    private VBox dataList;
    private ComboBox<String> moduleSelect;

    public Scene createScene(Stage stage) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("management-page");
        page.setTop(createHeader(stage));
        page.setCenter(createScroll());

        ThemedBackgroundPane root = new ThemedBackgroundPane(page);
        Scene scene = AppUi.createScene(root, this);
        Platform.runLater(this::showPendingPopup);
        return scene;
    }

    private void showPendingPopup() {
        if (AppDataStore.pendingApprovals().isEmpty()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Admin Approval Queue");
        alert.setHeaderText("New request waiting");
        alert.setContentText(AppDataStore.pendingApprovals().size()
                + " business/transport request is waiting for approval.");
        alert.show();
    }

    private HBox createHeader(Stage stage) {
        Label brand = new Label("SIMHASTHA CONNECT");
        brand.getStyleClass().add("page-brand");
        Label title = new Label("Admin Control Center");
        title.getStyleClass().add("page-heading");
        Label subtitle = new Label("Approvals, live data and module content management");
        subtitle.getStyleClass().add("page-subtitle");

        Button userView = new Button("Open User Dashboard");
        userView.getStyleClass().add("back-button");
        userView.setOnAction(event -> stage.setScene(new DashboardPage().createScene(stage)));

        HBox header = new HBox(18, new VBox(3, brand, title, subtitle), AppUi.spacer(), AppUi.createThemeToggle(),
                userView);
        header.getStyleClass().add("management-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 38, 8, 38));
        return header;
    }

    private ScrollPane createScroll() {
        ScrollPane scroll = new ScrollPane(createContent());
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox createContent() {
        HBox stats = new HBox(14,
                statCard("Pending Requests", String.valueOf(AppDataStore.pendingApprovals().size()),
                        "Business and transport approval queue"),
                statCard("Transport Items", String.valueOf(AppDataStore.items("transport").size()),
                        "Approved user-facing routes"),
                statCard("Business Listings", String.valueOf(AppDataStore.items("business").size()),
                        "Approved local services"),
                statCard("Emergency Contacts", String.valueOf(AppDataStore.items("emergency").size()),
                        "Visible to pilgrims"));
        stats.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.add(createApprovalPanel(), 0, 0);
        grid.add(createDataPanel(), 1, 0);
        grid.add(createFirebasePanel(), 0, 1, 2, 1);

        VBox content = new VBox(18, stats, grid);
        content.setPadding(new Insets(14, 38, 40, 38));
        return content;
    }

    private VBox createApprovalPanel() {
        approvalList = new VBox(9);
        refreshApprovals();

        VBox panel = new VBox(14, sectionTitle("Registration Approval Requests"), approvalList);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(570);
        panel.setMinHeight(360);
        return panel;
    }

    private VBox createDataPanel() {
        moduleSelect = new ComboBox<>();
        moduleSelect.getItems().addAll("transport", "puja", "ghat", "emergency", "stay", "lost", "schedule",
                "business", "announcement");
        moduleSelect.setValue("transport");
        moduleSelect.getStyleClass().add("input-combo");
        moduleSelect.setOnAction(event -> refreshDataList());

        TextField titleInput = AppUi.textField("Title / time / name");
        TextField detailInput = AppUi.textField("Details / number / route / instruction");
        Button add = new Button("ADD TO USER APP");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> {
            if (!titleInput.getText().trim().isEmpty() && !detailInput.getText().trim().isEmpty()) {
                AppDataStore.addItem(moduleSelect.getValue(), titleInput.getText().trim(), detailInput.getText().trim());
                titleInput.clear();
                detailInput.clear();
                refreshDataList();
            }
        });

        dataList = new VBox(9);
        refreshDataList();

        VBox panel = new VBox(12, sectionTitle("User Page Data Manager"), moduleSelect, titleInput, detailInput, add,
                dataList);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(570);
        panel.setMinHeight(360);
        return panel;
    }

    private VBox createFirebasePanel() {
        VBox list = new VBox(9,
                infoRow("\uE753", "Firebase status",
                        AppDataStore.isFirebaseEnabled() ? "Connected mode enabled from firebase.properties."
                                : "Local mode active. Add firebase.properties to sync with Firestore."),
                infoRow("\uE8A5", "Firebase-ready flow",
                        "This local store is the single place to replace with Firestore later."),
                infoRow("\uE7BA", "Approval gate",
                        "Business and transport registrations stay pending until admin approves them."),
                infoRow("\uE8FD", "User dashboard sync",
                        "Approved data appears in user Transport, Business, Stay and other module pages."));
        VBox panel = new VBox(14, sectionTitle("Application Control Logic"), list);
        panel.getStyleClass().add("management-panel");
        panel.setPrefWidth(1160);
        return panel;
    }

    private void refreshApprovals() {
        approvalList.getChildren().clear();
        if (AppDataStore.pendingApprovals().isEmpty()) {
            approvalList.getChildren().add(infoRow("\uE73E", "No pending requests", "New registrations will appear here."));
            return;
        }

        for (AppDataStore.ApprovalRequest request : new java.util.ArrayList<>(AppDataStore.pendingApprovals())) {
            Button approve = new Button("Approve");
            approve.getStyleClass().add("primary-button");
            approve.setOnAction(event -> {
                AppDataStore.approve(request);
                refreshApprovals();
                refreshDataList();
            });

            Button reject = new Button("Reject");
            reject.getStyleClass().add("text-button");
            reject.setOnAction(event -> {
                AppDataStore.reject(request);
                refreshApprovals();
            });

            VBox text = new VBox(2, rowTitle(request.title + "  [" + request.type + "]"),
                    rowDetail(request.detail + " | Target: " + AppDataStore.displayName(request.targetModule)));
            HBox row = new HBox(12, AppUi.symbolIcon("\uE8A7", "management-row-icon"), text, AppUi.spacer(), approve,
                    reject);
            row.getStyleClass().add("management-row");
            row.setAlignment(Pos.CENTER_LEFT);
            approvalList.getChildren().add(row);
        }
    }

    private void refreshDataList() {
        if (dataList == null || moduleSelect == null) {
            return;
        }
        dataList.getChildren().clear();
        String module = moduleSelect.getValue();
        for (AppDataStore.ServiceItem item : new java.util.ArrayList<>(AppDataStore.items(module))) {
            Button remove = new Button("Remove");
            remove.getStyleClass().add("text-button");
            remove.setOnAction(event -> {
                AppDataStore.removeItem(module, item);
                refreshDataList();
            });

            HBox row = new HBox(12, AppUi.symbolIcon("\uE8D4", "management-row-icon"),
                    new VBox(2, rowTitle(item.title), rowDetail(item.detail)), AppUi.spacer(), remove);
            row.getStyleClass().add("management-row");
            row.setAlignment(Pos.CENTER_LEFT);
            dataList.getChildren().add(row);
        }
    }

    private VBox statCard(String title, String value, String detail) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("management-stat-value");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("management-stat-title");
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("management-stat-detail");
        detailLabel.setWrapText(true);
        VBox card = new VBox(5, valueLabel, titleLabel, detailLabel);
        card.getStyleClass().add("management-stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox infoRow(String iconText, String title, String detail) {
        HBox row = new HBox(12, AppUi.symbolIcon(iconText, "management-row-icon"),
                new VBox(2, rowTitle(title), rowDetail(detail)));
        row.getStyleClass().add("management-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("management-section-title");
        return label;
    }

    private Label rowTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("management-row-title");
        label.setWrapText(true);
        return label;
    }

    private Label rowDetail(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("management-row-detail");
        label.setWrapText(true);
        return label;
    }
}
