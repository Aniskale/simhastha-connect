package com.simhastha.view;

import com.simhastha.packages.PackageBooking;
import com.simhastha.packages.PackageBookingRepository;
import com.simhastha.util.AppSession;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Kumbh-specific entries embedded in the existing My Bookings screen. */
public final class PackageBookingMyBookingsPane {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM uuuu").withZone(ZoneId.systemDefault());
    private final Consumer<Node> navigate;
    private final Runnable backToBookings;
    private final PackageBookingRepository repository = new PackageBookingRepository();

    public PackageBookingMyBookingsPane(Consumer<Node> navigate, Runnable backToBookings) {
        this.navigate = navigate;
        this.backToBookings = backToBookings;
    }

    public Node create() {
        VBox content = new VBox(8, label("Kumbh Package Bookings", "pilgrim-section-title"),
                label("Loading your package bookings…", "pilgrim-muted"));
        AppSession.User user = AppSession.currentUser();
        if (user == null) {
            content.getChildren().setAll(label("Kumbh Package Bookings", "pilgrim-section-title"),
                    label("Login is required to view package bookings.", "pilgrim-muted"));
            return content;
        }
        repository.findByUser(user.uid(), user.idToken()).whenComplete((bookings, error) -> Platform.runLater(() -> {
            content.getChildren().clear();
            content.getChildren().add(label("Kumbh Package Bookings", "pilgrim-section-title"));
            if (error != null) {
                content.getChildren().add(label("Package bookings could not be loaded. Please try again.", "pilgrim-muted"));
            } else if (bookings.isEmpty()) {
                content.getChildren().add(label("No Kumbh package bookings yet.", "pilgrim-muted"));
            } else {
                bookings.forEach(booking -> content.getChildren().add(bookingRow(booking)));
            }
        }));
        return content;
    }

    private Node bookingRow(PackageBooking booking) {
        VBox copy = new VBox(2, label(booking.packageName(), "pilgrim-row-title"),
                label(booking.bookingId() + " • " + booking.route() + " • " + displayDate(booking.createdAt()), "pilgrim-muted"),
                label("Travellers: " + booking.travellers().size() + " • ₹" + String.format("%,d", booking.finalAmount()), "pilgrim-muted"),
                label("Confirmed — Demo • Demo Payment / DEMO_SUCCESS", "pilgrim-muted"));
        Button details = new Button("View Details");
        details.getStyleClass().add("pilgrim-small-action");
        details.setOnAction(event -> navigate.accept(detailsPage(booking)));
        HBox row = new HBox(10, copy, spacer(), details);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("pilgrim-data-row");
        return row;
    }

    private Node detailsPage(PackageBooking booking) {
        VBox travellerRows = new VBox(5);
        booking.travellers().forEach(traveller -> travellerRows.getChildren().add(label(
                traveller.fullName() + " • Age: " + traveller.age() + " • " + traveller.gender() + "\n"
                        + traveller.idType() + ": " + traveller.maskedId(), "pilgrim-muted")));
        VBox selectionRows = new VBox(5);
        booking.selections().forEach((key, value) -> selectionRows.getChildren().add(label(
                key.replace('_', ' ') + ": " + readableSelection(value), "pilgrim-muted")));
        VBox priceRows = new VBox(5);
        booking.componentPrices().forEach((name, amount) -> priceRows.getChildren().add(label(name + ": " + amountText(amount), "pilgrim-muted")));
        priceRows.getChildren().add(label("Final amount: " + amountText(booking.finalAmount()), "pilgrim-row-title"));
        Button back = new Button("Back to My Bookings");
        back.getStyleClass().add("pilgrim-small-action");
        back.setOnAction(event -> backToBookings.run());
        VBox page = new VBox(10, label("Kumbh Package Booking Details", "pilgrim-section-title"),
                section("Booking Info", line("Booking ID", booking.bookingId()), line("Booking date", displayDate(booking.createdAt())), line("Booking status", "Confirmed — Demo")),
                section("Package", line("Package", booking.packageName()), line("Route", booking.route()), line("Duration", booking.duration())),
                section("Primary Contact", line("Name", booking.primaryContact().fullName()), line("Mobile", booking.primaryContact().mobileNumber()), line("Email", booking.primaryContact().emailAddress())),
                section("Travellers", travellerRows), section("Customization", selectionRows),
                section("Payment", line("Mode", "Demo Payment"), line("Status", "DEMO_SUCCESS"), line("Demo Reference", booking.demoReference())),
                section("Price", priceRows), back);
        page.getStyleClass().add("pilgrim-dashboard-main");
        return page;
    }

    private VBox section(String title, Node... content) {
        VBox box = new VBox(5, label(title, "pilgrim-row-title"));
        box.getChildren().addAll(content);
        box.getStyleClass().add("pilgrim-card");
        return box;
    }

    private Label line(String name, String value) { return label(name + ": " + value, "pilgrim-muted"); }
    private String readableSelection(String value) { int separator = value == null ? -1 : value.indexOf('|'); return separator < 0 ? String.valueOf(value) : value.substring(separator + 1).replace("|", " • "); }
    private String amountText(int amount) { return amount == 0 ? "Included" : "₹" + String.format("%,d", amount); }
    private String displayDate(String value) { try { return DATE.format(Instant.parse(value)); } catch (Exception ignored) { return value == null || value.isBlank() ? "Not available" : value; } }
    private Label label(String value, String style) { Label label = new Label(value); label.setWrapText(true); label.getStyleClass().add(style); return label; }
    private Region spacer() { Region region = new Region(); HBox.setHgrow(region, Priority.ALWAYS); return region; }
}
