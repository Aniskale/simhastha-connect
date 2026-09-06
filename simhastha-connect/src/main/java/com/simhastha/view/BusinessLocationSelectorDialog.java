package com.simhastha.view;

import com.simhastha.model.BusinessLocation;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class BusinessLocationSelectorDialog {
    private static final Logger LOGGER = Logger.getLogger(BusinessLocationSelectorDialog.class.getName());

    private BusinessLocationSelectorDialog() {
    }

    public static Optional<BusinessLocation> show(BusinessLocation initial) {
        LOGGER.info("Business location selector opened.");
        Dialog<BusinessLocation> dialog = new Dialog<>();
        dialog.setTitle("Select Business Location on Map");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Confirm Location", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));

        TextField address = AppUi.textField("Address");
        TextField area = AppUi.textField("Area");
        TextField city = AppUi.textField("City");
        TextField latitude = AppUi.textField("Latitude");
        TextField longitude = AppUi.textField("Longitude");
        if (initial != null) {
            address.setText(initial.address());
            area.setText(initial.area());
            city.setText(initial.city());
            latitude.setText(initial.latitude());
            longitude.setText(initial.longitude());
        }
        AtomicLong selectionVersion = new AtomicLong();

        Label status = new Label("Loading map...");
        status.getStyleClass().add("business-row-detail");
        Node map = MapWebViewFactory.businessSelector(initial, (lat, lon) -> {
            String latText = String.format(java.util.Locale.US, "%.6f", lat);
            String lonText = String.format(java.util.Locale.US, "%.6f", lon);
            latitude.setText(latText);
            longitude.setText(lonText);
            long version = selectionVersion.incrementAndGet();
            status.setText("Resolving selected location...");
            LOGGER.info("Marker moved: lat=" + latText + ", lon=" + lonText);
            LOGGER.info("Reverse geocode started for lat=" + latText + ", lon=" + lonText);
            BusinessLocationReverseGeocoder.reverse(lat, lon).whenComplete((resolved, failure) ->
                    javafx.application.Platform.runLater(() -> {
                        if (version != selectionVersion.get()) return;
                        if (failure != null || resolved == null) {
                            LOGGER.log(Level.WARNING, "Reverse geocode failed for " + latText + ", " + lonText,
                                    failure);
                            status.setText("Selected coordinates: " + latText + ", " + lonText
                                    + " (address details unavailable)");
                            return;
                        }
                        address.setText(resolved.address());
                        area.setText(resolved.area());
                        city.setText(resolved.city());
                        status.setText("Selected: " + resolved.address());
                        LOGGER.info("Reverse geocode resolved: address=" + resolved.address() + ", area="
                                + resolved.area() + ", city=" + resolved.city());
                    }));
        }, () -> status.setText("Click or drag the marker to select location."),
                () -> { LOGGER.warning("Business location selector map reported a load failure."); status.setText("Map could not be opened. Please try again."); });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.add(new Label("Address"), 0, 0); form.add(address, 1, 0);
        form.add(new Label("Area"), 0, 1); form.add(area, 1, 1);
        form.add(new Label("City"), 0, 2); form.add(city, 1, 2);
        form.add(new Label("Latitude"), 0, 3); form.add(latitude, 1, 3);
        form.add(new Label("Longitude"), 0, 4); form.add(longitude, 1, 4);
        GridPane.setHgrow(address, Priority.ALWAYS);
        GridPane.setHgrow(area, Priority.ALWAYS);
        GridPane.setHgrow(city, Priority.ALWAYS);

        VBox content = new VBox(12, status, map, form);
        VBox.setVgrow(map, Priority.ALWAYS);
        content.setPadding(new Insets(10));
        content.setPrefWidth(920);
        content.setPrefHeight(680);
        dialog.getDialogPane().setContent(content);
        dialog.setOnShown(event -> javafx.application.Platform.runLater(
                () -> MapWebViewFactory.invalidateBusinessSelectorMap(map)));
        dialog.setResultConverter(button -> {
            if (button.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
            try {
                BusinessLocation selected = new BusinessLocation(address.getText().trim(), area.getText().trim(),
                        city.getText().trim(), latitude.getText().trim(), longitude.getText().trim(),
                        String.valueOf(System.currentTimeMillis()));
                LOGGER.info("Confirmed business location: address=" + selected.address() + ", area="
                        + selected.area() + ", city=" + selected.city() + ", lat=" + selected.latitude()
                        + ", lon=" + selected.longitude());
                return selected.hasCoordinates() ? selected : null;
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Business location selection failed.", exception);
                return null;
            }
        });
        return dialog.showAndWait();
    }
}
