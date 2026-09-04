package com.simhastha.view;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Reusable local-only image selection and preview control. */
public final class ImagePickerPane extends VBox {
    private static final long MAX_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private final boolean multiple;
    private final int maxImages;
    private final FlowPane previews = new FlowPane(8, 8);
    private final List<File> selectedFiles = new java.util.ArrayList<>();
    private final Consumer<List<File>> onChanged;
    private final Button selectButton;

    public ImagePickerPane(String title, int maxImages, Consumer<List<File>> onChanged) {
        this.multiple = maxImages > 1;
        this.maxImages = Math.max(1, maxImages);
        this.onChanged = onChanged;
        getStyleClass().add("media-picker-pane");
        Label heading = new Label(title);
        heading.getStyleClass().add("business-row-title");
        selectButton = new Button(multiple ? "+ Add Photos" : "Select Image");
        selectButton.getStyleClass().add("business-text-action");
        selectButton.setOnAction(event -> choose(selectButton.getScene() == null ? null : selectButton.getScene().getWindow()));
        HBox controls = new HBox(8, heading, spacer(), selectButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        previews.setPadding(new Insets(2, 0, 0, 0));
        getChildren().addAll(controls, previews);
        refresh();
    }

    public List<File> selectedFiles() {
        return List.copyOf(selectedFiles);
    }

    public void replace(File file) {
        selectedFiles.clear();
        if (file != null && validate(file)) selectedFiles.add(file);
        refresh();
    }

    public void replaceAll(Collection<File> files) {
        selectedFiles.clear();
        if (files != null) {
            for (File file : files) {
                if (selectedFiles.size() >= maxImages) break;
                if (file != null && file.isFile()) selectedFiles.add(file);
            }
        }
        refresh();
    }

    public void removeAll() {
        selectedFiles.clear();
        refresh();
    }

    private void choose(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(multiple ? "Add Photos" : "Select Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        List<File> files;
        if (multiple) {
            files = chooser.showOpenMultipleDialog(owner);
        } else {
            File file = chooser.showOpenDialog(owner);
            files = file == null ? List.of() : List.of(file);
        }
        if (files == null || files.isEmpty() || files.get(0) == null) return;
        if (!multiple) selectedFiles.clear();
        for (File file : files) {
            if (selectedFiles.size() >= maxImages) break;
            if (validate(file)) selectedFiles.add(file);
        }
        refresh();
    }

    private boolean validate(File file) {
        String name = file == null ? "" : file.getName().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1);
        if (file == null || !file.isFile() || !EXTENSIONS.contains(extension)) {
            error("Please choose a JPG, JPEG, PNG or supported WEBP image.");
            return false;
        }
        if (file.length() > MAX_BYTES) {
            error("Image must be 10 MB or smaller.");
            return false;
        }
        Image image = new Image(file.toURI().toString(), 720, 420, true, true, false);
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            error("This image could not be previewed. Choose another file.");
            return false;
        }
        return true;
    }

    private void refresh() {
        previews.getChildren().clear();
        for (File file : selectedFiles) {
            ImageView image = new ImageView(new Image(file.toURI().toString(), 104, 68, true, true));
            image.setFitWidth(104);
            image.setFitHeight(68);
            image.setPreserveRatio(false);
            Button remove = new Button("Remove");
            remove.getStyleClass().add("text-button");
            remove.setOnAction(event -> { selectedFiles.remove(file); refresh(); });
            VBox tile = new VBox(4, new StackPane(image), remove);
            tile.getStyleClass().add("media-picker-thumb");
            previews.getChildren().add(tile);
        }
        if (selectedFiles.isEmpty()) previews.getChildren().add(new Label("No image selected yet."));
        if (!multiple) selectButton.setText(selectedFiles.isEmpty() ? "Select Image" : "Replace Image");
        if (onChanged != null) onChanged.accept(selectedFiles());
    }

    private javafx.scene.Node spacer() {
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void error(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setTitle("Image");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
