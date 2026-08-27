package com.simhastha.view;

import com.simhastha.model.PublicBusinessItem;
import com.simhastha.model.PublicBusinessListing;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class BusinessDetailsPage {

    private final String businessId;
    private final PublicBusinessListing business;
    private final Runnable onBack;

    public BusinessDetailsPage(String businessId, PublicBusinessListing business, Runnable onBack) {
        this.businessId = businessId == null ? "" : businessId;
        this.business = business;
        this.onBack = onBack;
    }

    public Node createContent() {
        Button back = new Button("Back to Businesses");
        back.getStyleClass().add("marketplace-secondary-button");
        back.setOnAction(event -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        VBox headerText = new VBox(5,
                label(business.name(), "marketplace-detail-title"),
                label("Business ID: " + businessId, "marketplace-state-title"),
                label(business.displayCategory(), "marketplace-price"));
        HBox header = new HBox(12, headerText, spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox summary = new VBox(9);
        summary.getStyleClass().add("pilgrim-panel");
        add(summary, "Location", business.location());
        add(summary, "Description", business.description());
        add(summary, "Hours", business.operatingHours());
        add(summary, "Pricing", business.priceRange());

        VBox services = new VBox(9, label("Available Services", "pilgrim-section-title"));
        services.getStyleClass().add("pilgrim-panel");
        if (business.items().isEmpty()) {
            services.getChildren().add(label("Service details will appear here when the business owner adds them.",
                    "marketplace-card-detail"));
        } else {
            for (PublicBusinessItem item : business.items()) {
                services.getChildren().add(serviceRow(item));
            }
        }

        VBox content = new VBox(14, header, summary, services);
        content.getStyleClass().add("pilgrim-dashboard-main");
        content.setPadding(new Insets(12, 22, 28, 22));
        return content;
    }

    private HBox serviceRow(PublicBusinessItem item) {
        VBox text = new VBox(3,
                label(valueOr(item.name(), "Service"), "pilgrim-card-title"),
                label(serviceDetail(item), "marketplace-card-detail"));
        HBox row = new HBox(10, AppUi.symbolIcon("\uE8D4", "pilgrim-row-icon"), text);
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String serviceDetail(PublicBusinessItem item) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (item.itemType() != null && !item.itemType().isBlank()) parts.add(item.itemType());
        if (item.price() != null && !item.price().isBlank() && !"0".equals(item.price().trim())) {
            parts.add("\u20B9" + item.price().trim());
        }
        if (item.availability() != null && !item.availability().isBlank()) parts.add(item.availability());
        if (item.description() != null && !item.description().isBlank()) parts.add(item.description());
        return parts.isEmpty() ? "Details will be updated by the business owner." : String.join(" | ", parts);
    }

    private void add(VBox box, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        box.getChildren().add(new HBox(10, label(key, "business-overview-key"), spacer(),
                label(value, "business-overview-value")));
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
