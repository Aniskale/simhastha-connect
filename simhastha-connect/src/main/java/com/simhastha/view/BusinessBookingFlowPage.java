package com.simhastha.view;

import com.simhastha.model.PublicBusinessItem;
import com.simhastha.model.PublicBusinessListing;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Shared in-shell form and review flow for all marketplace business inventory. */
public final class BusinessBookingFlowPage {
    private final PublicBusinessListing business;
    private final PublicBusinessItem item;
    private final Runnable onBack;
    private final Consumer<Selection> onPay;

    public BusinessBookingFlowPage(PublicBusinessListing business, PublicBusinessItem item, Runnable onBack,
            Consumer<Selection> onPay) {
        this.business = business; this.item = item; this.onBack = onBack; this.onPay = onPay;
    }

    public Node createContent() {
        boolean locker = business.displayCategory().toLowerCase().contains("locker");
        DatePicker start = new DatePicker(LocalDate.now().plusDays(1));
        DatePicker end = new DatePicker(LocalDate.now().plusDays(2));
        TextField quantity = AppUi.textField(locker ? "Locker quantity" : "Rooms / units"); quantity.setText("1");
        TextField guests = AppUi.textField(locker ? "Duration hours" : "Guests"); guests.setText(locker ? "2" : "2");
        TextField name = AppUi.textField("Guest name");
        TextField phone = AppUi.textField("Phone");
        TextField email = AppUi.textField("Email");
        TextArea request = new TextArea(); request.setPromptText("Special request (optional)"); request.setPrefRowCount(3);
        VBox form = panel("Booking details", title(), row("Selected", item.name()), row("Price", "₹" + item.price()),
                row(locker ? "Date" : "Check-in", start), locker ? row("Start time", AppUi.textField("Start time")) : row("Check-out", end),
                row(locker ? "Locker quantity" : "Rooms / units", quantity), row(locker ? "Duration hours" : "Guests", guests),
                row("Guest details", name), row("Phone", phone), row("Email", email), request);
        Button review = new Button("Continue to Review"); review.getStyleClass().add("marketplace-primary-action");
        review.setOnAction(event -> {
            int units = positive(quantity.getText()); int nights = locker ? Math.max(1, positive(guests.getText()))
                    : Math.max(1, (int) ChronoUnit.DAYS.between(start.getValue(), end.getValue()));
            if (units < 1 || start.getValue() == null || (!locker && !end.getValue().isAfter(start.getValue()))) return;
            form.getChildren().setAll(reviewPage(units, nights, name.getText(), phone.getText(), email.getText(), request.getText(), onBack));
        });
        form.getChildren().add(review);
        VBox page = new VBox(16, form); page.setPadding(new Insets(22, 28, 32, 28)); page.getStyleClass().add("pilgrim-dashboard-main"); return page;
    }
    private Node reviewPage(int units, int nights, String name, String phone, String email, String request, Runnable back) {
        long total = price() * units * nights;
        Button backButton = new Button("Back"); backButton.getStyleClass().add("marketplace-secondary-action"); backButton.setOnAction(e -> { if (onBack != null) onBack.run(); });
        Button pay = new Button("Proceed to Pay ₹" + total); pay.getStyleClass().add("marketplace-primary-action");
        pay.setOnAction(e -> { if (onPay != null) onPay.accept(new Selection(units, nights, name, phone, email, request)); });
        return panel("Review Booking", title(), row("Business", business.name()), row("Selected unit", item.name()),
                row("Quantity", String.valueOf(units)), row("Nights / duration", String.valueOf(nights)), row("Customer", name),
                row("Total payable", "₹" + total), new HBox(10, backButton, pay));
    }
    private VBox panel(String heading, Node... nodes) { VBox b = new VBox(10, new Label(heading)); b.getStyleClass().add("pilgrim-panel"); b.getChildren().addAll(nodes); return b; }
    private Label title() { Label l = new Label("Book " + item.name() + " · " + business.name()); l.getStyleClass().add("business-detail-title"); return l; }
    private HBox row(String key, Node value) { Label k = new Label(key); k.getStyleClass().add("business-overview-key"); HBox r = new HBox(12, k, spacer(), value); HBox.setHgrow(value, Priority.ALWAYS); return r; }
    private HBox row(String key, String value) { Label v = new Label(value == null ? "" : value); v.getStyleClass().add("business-overview-value"); return row(key, v); }
    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }
    private int positive(String value) { try { return Math.max(0, Integer.parseInt(value.trim())); } catch (Exception e) { return 0; } }
    private long price() { try { return Long.parseLong(item.price().trim()); } catch (Exception e) { return 0; } }
    public record Selection(int quantity, int nights, String name, String phone, String email, String specialRequest) { }
}
