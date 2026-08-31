package com.simhastha.view;

import com.simhastha.model.BusinessMedia;
import com.simhastha.model.PublicBusinessItem;
import com.simhastha.model.PublicBusinessListing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class BusinessDetailsPage {

    private final String businessId;
    private final PublicBusinessListing business;
    private final Runnable onBack;
    private final Runnable onLocate;
    private final Runnable onRoute;
    private final Consumer<PublicBusinessItem> onBook;

    public BusinessDetailsPage(String businessId, PublicBusinessListing business, Runnable onBack,
            Runnable onLocate, Runnable onRoute) {
        this(businessId, business, onBack, onLocate, onRoute, null);
    }

    public BusinessDetailsPage(String businessId, PublicBusinessListing business, Runnable onBack,
            Runnable onLocate, Runnable onRoute, Consumer<PublicBusinessItem> onBook) {
        this.businessId = businessId == null ? "" : businessId;
        this.business = business;
        this.onBack = onBack;
        this.onLocate = onLocate;
        this.onRoute = onRoute;
        this.onBook = onBook;
    }

    public Node createContent() {
        Button back = new Button("Back to Businesses");
        back.getStyleClass().add("marketplace-secondary-button");
        back.setOnAction(event -> { if (onBack != null) onBack.run(); });

        VBox title = new VBox(5,
                label(business.name(), "business-detail-title"),
                label(valueOr(business.displayCategory(), "Verified Business"), "business-detail-category"),
                label(valueOr(business.displayLocation(), "Location will be updated by the owner."),
                        "business-detail-location"));
        HBox header = new HBox(14, title, spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);
        title.setMaxWidth(760);
        HBox.setHgrow(title, Priority.ALWAYS);

        VBox content = new VBox(16, header, heroGallery(), summarySection(), servicesSection(),
                facilitiesSection(), locationSection(), contactSection());
        content.getStyleClass().addAll("pilgrim-dashboard-main", "business-detail-page");
        content.setPadding(new Insets(18, 26, 30, 26));
        content.setMaxWidth(1120);
        return content;
    }

    private StackPane heroGallery() {
        List<BusinessMedia> photos = business.activeMedia();
        StackPane frame;
        if (!photos.isEmpty()) {
            ImageView image = new ImageView(new Image(photos.get(0).url(), true));
            image.setFitHeight(260);
            image.setPreserveRatio(true);
            image.setSmooth(true);
            frame = new StackPane(image);
            image.fitWidthProperty().bind(frame.widthProperty());
        } else {
            frame = new StackPane(AppUi.symbolIcon("\uE719", "business-detail-hero-icon"));
            frame.getChildren().add(label("Photos will appear here when the owner uploads real media.",
                    "business-detail-hero-note"));
        }
        frame.getStyleClass().add("business-detail-hero");
        frame.setMaxWidth(Double.MAX_VALUE);
        frame.setMinHeight(260);
        frame.setPrefHeight(260);
        return frame;
    }

    private VBox summarySection() {
        VBox box = panel("Business Summary");
        addPair(box, "Business ID", businessId);
        addPair(box, "About", business.description());
        addPair(box, "Opening Info", business.operatingHours());
        addPair(box, "Price Range", priceText());
        return box;
    }

    private VBox servicesSection() {
        VBox box = panel("Available Services");
        if (business.items() == null || business.items().isEmpty()) {
            box.getChildren().add(label("Service details will appear here when the business owner adds them.",
                    "marketplace-card-detail"));
            return box;
        }
        for (PublicBusinessItem item : business.items()) {
            box.getChildren().add(serviceRow(item));
        }
        return box;
    }

    private VBox facilitiesSection() {
        FlowPane chips = new FlowPane(8, 8);
        if (business.items() != null) {
            business.items().stream()
                    .map(PublicBusinessItem::facilities)
                    .filter(value -> value != null && !value.isBlank())
                    .flatMap(value -> List.of(value.split(",")).stream())
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .forEach(value -> chips.getChildren().add(label(value, "business-detail-chip")));
        }
        VBox box = panel("Facilities");
        box.getChildren().add(chips.getChildren().isEmpty()
                ? label("Facilities will be updated by the business owner.", "marketplace-card-detail")
                : chips);
        return box;
    }

    private VBox locationSection() {
        Button locate = new Button("View on Map");
        locate.getStyleClass().add("marketplace-secondary-action");
        locate.setDisable(!business.hasCoordinates());
        locate.setOnAction(event -> { if (onLocate != null) onLocate.run(); });

        Button route = new Button("Get Route");
        route.getStyleClass().add("marketplace-primary-action");
        route.setDisable(!business.hasCoordinates());
        route.setOnAction(event -> { if (onRoute != null) onRoute.run(); });

        VBox box = panel("Location");
        addPair(box, "Address", business.displayLocation());
        addPair(box, "Coordinates", business.hasCoordinates()
                ? business.latitude() + ", " + business.longitude()
                : "Location not selected yet");
        box.getChildren().add(new HBox(10, locate, route));
        return box;
    }

    private VBox contactSection() {
        VBox box = panel("Contact");
        addPair(box, "Phone", business.mobile());
        addPair(box, "Email", business.email());
        return box;
    }

    private HBox serviceRow(PublicBusinessItem item) {
        Node photo = servicePhoto(item);
        VBox text = new VBox(4,
                label(valueOr(item.name(), "Service"), "pilgrim-card-title"),
                label(serviceDetail(item), "marketplace-card-detail"));
        text.setMaxWidth(760);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(12, photo, text, spacer());
        if (item.bookingEnabled()) {
            Button book = new Button("Book Now");
            book.getStyleClass().add("marketplace-primary-action");
            book.setOnAction(event -> { if (onBook != null) onBook.accept(item); });
            row.getChildren().add(book);
        }
        row.getStyleClass().add("pilgrim-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node servicePhoto(PublicBusinessItem item) {
        if (item.hasPhoto()) {
            ImageView image = new ImageView(new Image(item.photoUrl(), 54, 54, true, true));
            image.setFitWidth(54);
            image.setFitHeight(54);
            image.setPreserveRatio(false);
            image.setSmooth(true);
            StackPane frame = new StackPane(image);
            frame.getStyleClass().add("media-picker-thumb");
            return frame;
        }
        return AppUi.symbolIcon("\uE8D4", "pilgrim-row-icon");
    }

    private String serviceDetail(PublicBusinessItem item) {
        List<String> parts = new ArrayList<>();
        add(parts, item.itemType());
        if (item.price() != null && !item.price().isBlank() && !"0".equals(item.price().trim())) {
            parts.add("\u20B9" + item.price().trim());
        }
        add(parts, item.availability());
        add(parts, item.facilities());
        add(parts, item.description());
        return parts.isEmpty() ? "Details will be updated by the business owner." : String.join(" | ", parts);
    }

    private String priceText() {
        if (business.priceRange() != null && !business.priceRange().isBlank()) return business.priceRange();
        if (business.items() == null) return "";
        return business.items().stream()
                .map(PublicBusinessItem::price)
                .filter(value -> value != null && !value.isBlank() && !"0".equals(value.trim()))
                .findFirst()
                .map(value -> "Starts at \u20B9" + value.trim())
                .orElse("");
    }

    private VBox panel(String title) {
        VBox box = new VBox(10, label(title, "pilgrim-section-title"));
        box.getStyleClass().add("pilgrim-panel");
        return box;
    }

    private void addPair(VBox box, String key, String value) {
        if (value == null || value.isBlank()) return;
        Label keyLabel = label(key, "business-overview-key");
        Label valueLabel = label(value, "business-overview-value");
        valueLabel.setMaxWidth(820);
        VBox row = new VBox(4, keyLabel, valueLabel);
        box.getChildren().add(row);
    }

    private void add(List<String> parts, String value) {
        if (value != null && !value.isBlank()) parts.add(value.trim());
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
