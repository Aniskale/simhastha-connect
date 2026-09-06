package com.simhastha.view;

import com.simhastha.packages.*;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

/** Part 1 catalogue UI. Presentation-only polish: package search and repository behaviour remain unchanged. */
public final class KumbhPackagesPage {
    private final Consumer<Node> navigate;
    private final Runnable openMyBookings;
    private final PackageService service = new PackageService();
    private final ComboBox<String> city = combo("Starting From", PackageRepository.CITIES, true);
    private final DatePicker date = new DatePicker(LocalDate.now());
    private final ComboBox<String> duration = combo("Duration", List.of("Any duration", "1 Day", "2D / 1N", "3D / 2N", "4D / 3N", "5D / 4N", "6D / 5N", "7 Days+"), false);
    private final ComboBox<String> category = combo("All Packages", List.of("All Packages", "Premium", "Standard", "Budget"), false);
    private final ComboBox<String> travellers = combo("Travellers", List.of("1 Adult", "2 Adults", "2 Adults, 1 Child", "2 Adults, 1 Senior Citizen", "Family"), false);
    private final ComboBox<String> sort = combo("Recommended", List.of("Recommended", "Popularity", "Price Low to High", "Price High to Low", "Duration Short to Long", "Duration Long to Short"), false);
    private final Map<String, CheckBox> filters = new LinkedHashMap<>();
    private VBox resultList;
    private Label resultTitle;
    private Label resultCount;
    private boolean updatingFilters;
    private final AtomicLong searchGeneration = new AtomicLong();
    private static final List<LocationMapping> ITINERARY_LOCATIONS = List.of(
            new LocationMapping(new ItineraryLocation("Godavari Ghat (Ganga Ghat), Panchavati, Nashik, Maharashtra", 20.008064, 73.792294), "godavari ghat", "ghat darshan", "ramkund ghat"),
            new LocationMapping(new ItineraryLocation("Ramkund, Panchavati, Nashik, Maharashtra", 20.0072639, 73.7924901), "ramkund", "ram kund"),
            new LocationMapping(new ItineraryLocation("Shri Kalaram Mandir, Panchavati, Nashik, Maharashtra", 20.00695, 73.79522), "kalaram", "kala ram", "shri kalaram mandir"),
            new LocationMapping(new ItineraryLocation("Panchavati, Nashik, Maharashtra", 20.00669, 73.79357), "panchavati", "panchvati"),
            new LocationMapping(new ItineraryLocation("Trimbakeshwar Temple, Nashik District, Maharashtra", 19.93213, 73.53075), "trimbakeshwar", "trimbak"),
            new LocationMapping(new ItineraryLocation("Nashik International Airport (Ozar), Maharashtra", 20.12000, 73.91333), "ozar", "ojhar", "airport"),
            new LocationMapping(new ItineraryLocation("Nashik Road Railway Station, Nashik, Maharashtra", 19.94806, 73.84208), "nashik road", "railway"),
            new LocationMapping(new ItineraryLocation("Nashik Central Bus Stand (CBS), Maharashtra", 20.0011961, 73.7820646), "cbs", "central bus", "arrival assistance", "departure assistance", "pickup", "drop", "arrival in nashik")
    );

    public KumbhPackagesPage(Consumer<Node> navigate) {
        this(navigate, () -> { });
    }

    public KumbhPackagesPage(Consumer<Node> navigate, Runnable openMyBookings) {
        this.navigate = navigate;
        this.openMyBookings = openMyBookings;
        date.setDayCellFactory(p -> new DateCell() { @Override public void updateItem(LocalDate value, boolean empty) { super.updateItem(value, empty); setDisable(empty || value.isBefore(LocalDate.now())); } });
    }

    public Node create() {
        ImageView heroImage = new ImageView(); URL heroUrl = AppResources.url(getClass(), "/images/welcome-light.png"); if (heroUrl != null) heroImage.setImage(new Image(heroUrl.toExternalForm(), 1400, 280, true, true, true)); heroImage.setPreserveRatio(false); heroImage.setFitHeight(136);
        Region heroOverlay = new Region(); heroOverlay.getStyleClass().add("package-hero-overlay");
        HBox benefits = new HBox(7, text("Trusted Packages", "package-hero-feature"), text("Best Prices", "package-hero-feature"), text("Verified Travel", "package-hero-feature"), text("24/7 Assistance", "package-hero-feature"));
        VBox heroCopy = new VBox(4, text("SIMHASTHA 2027 • NASHIK", "package-eyebrow"), text("Kumbh Packages", "package-title"), text("Complete Simhastha journeys from your city to Nashik", "package-subtitle"), benefits); heroCopy.getStyleClass().add("package-hero-copy"); heroCopy.setAlignment(Pos.CENTER_LEFT);
        StackPane hero = new StackPane(heroImage, heroOverlay, heroCopy); hero.getStyleClass().add("package-hero"); hero.setMinHeight(136); hero.setPrefHeight(136); heroImage.fitWidthProperty().bind(hero.widthProperty()); StackPane.setAlignment(heroCopy, Pos.CENTER_LEFT); StackPane.setMargin(heroCopy, new Insets(15, 22, 15, 22));
        Button search = button("Search Packages", "package-search-button"); search.setOnAction(e -> refresh());
        Node[] searchFields = { searchField("FROM", city, "Choose your city"), searchField("TO", fixed("Nashik\nSimhastha 2027"), "Sacred destination"), searchField("DEPARTURE", date, "Choose a date"), searchField("DURATION", duration, "Select duration"), searchField("TRAVELLERS", travellers, "Adults & children"), searchField("CATEGORY", category, "All packages"), search };
        GridPane fields = new GridPane(); fields.getStyleClass().add("package-search-fields"); fields.setHgap(0); fields.setVgap(0); fields.setMinWidth(0); fields.setMaxWidth(Double.MAX_VALUE);
        for (int index = 0; index < searchFields.length; index++) { ColumnConstraints column = new ColumnConstraints(); if (index == searchFields.length - 1) { column.setMinWidth(155); column.setPrefWidth(155); column.setMaxWidth(155); } else { column.setMinWidth(0); column.setHgrow(Priority.ALWAYS); column.setFillWidth(true); } fields.getColumnConstraints().add(column); fields.add(searchFields[index], index, 0); GridPane.setHgrow(searchFields[index], index == searchFields.length - 1 ? Priority.NEVER : Priority.ALWAYS); }
        VBox searchPanel = new VBox(fields); searchPanel.getStyleClass().add("package-search-panel");
        resultTitle = text("Packages to Nashik", "package-results-title"); resultCount = text("", "package-result-count");
        HBox resultHeader = new HBox(12, new VBox(2, resultTitle, resultCount), spacer(), text("Package catalogue • Live travel availability confirmed during booking", "package-note"), text("Sort by", "package-sort-label"), sort);
        resultHeader.setMinWidth(0); resultHeader.setMaxWidth(Double.MAX_VALUE); resultHeader.setAlignment(Pos.CENTER_LEFT); resultHeader.getStyleClass().add("package-result-header"); sort.setOnAction(e -> refresh());
        resultList = new VBox(14); resultList.setMinWidth(0); resultList.setMaxWidth(Double.MAX_VALUE); resultList.setFillWidth(true);
        VBox results = new VBox(12, resultHeader, resultList); results.setMinWidth(0); results.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(results, Priority.ALWAYS);
        HBox body = new HBox(16, filterPanel(), results); body.setMinWidth(0); body.setMaxWidth(Double.MAX_VALUE); body.setAlignment(Pos.TOP_LEFT); body.getStyleClass().add("package-body");
        VBox page = new VBox(13, hero, searchPanel, body); page.setMinWidth(0); page.setMaxWidth(Double.MAX_VALUE); page.getStyleClass().add("package-page"); page.setPadding(new Insets(16, 22, 30, 22)); constrainToOuterViewport(page); refresh(); return page;
    }

    private VBox filterPanel() {
        Button clear = button("Clear All", "package-clear-button"); clear.setOnAction(e -> clearFilters());
        HBox heading = new HBox(text("Filters", "package-filter-title"), spacer(), clear); heading.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(10, heading); box.getStyleClass().add("package-filter-panel");
        group(box, "Package Type", "Premium", "Standard", "Budget"); group(box, "Duration", "1 Day", "2D / 1N", "3D / 2N", "4D / 3N", "5D / 4N", "6D / 5N+");
        group(box, "Budget", "Under ₹5,000", "₹5,000 – ₹10,000", "₹10,000 – ₹20,000", "₹20,000 – ₹35,000", "₹35,000 – ₹50,000", "₹50,000+");
        group(box, "Travel Mode", "Flight", "Train", "Bus", "Private Vehicle", "Self Travel"); group(box, "Stay", "Premium Hotel", "Hotel", "Budget Hotel", "Dharamshala", "Tent", "Ashram", "No Stay");
        group(box, "Meals", "Breakfast", "Breakfast + Dinner", "Full Meals", "No Meals"); group(box, "Package Theme", "Simhastha Special", "Premium Experience", "Complete Kumbh", "Family Pilgrimage", "Senior Citizen Friendly", "Spiritual Journey", "Temple Darshan", "Ghat & Snan", "Nashik Sightseeing", "Budget Pilgrimage"); group(box, "Facilities", "Puja Included", "Snan Assistance", "Nashik Sightseeing Included", "Senior Citizen Friendly"); return box;
    }
    private void group(VBox box, String name, String... values) { VBox group = new VBox(5); group.getStyleClass().add("package-filter-group"); group.getChildren().add(text(name, "package-filter-heading")); for (String value : values) { CheckBox check = new CheckBox(value); check.getStyleClass().add("package-check"); check.selectedProperty().addListener((o, old, selected) -> refresh()); filters.put(name + ":" + value, check); group.getChildren().add(check); } box.getChildren().add(group); }
    private void clearFilters() {
        updatingFilters = true;
        try {
            filters.values().forEach(c -> c.setSelected(false));
            city.setValue(null);
            duration.setValue("Any duration");
            category.setValue("All Packages");
        } finally {
            updatingFilters = false;
        }
        refresh();
    }

    private void refresh() {
        if (resultList == null || updatingFilters) return;
        PackageSearchCriteria criteria = new PackageSearchCriteria(city.getValue(), date.getValue(), duration.getValue(), parseCategory(category.getValue()), selected("Package Type"), selected("Duration"), selected("Budget"), selected("Travel Mode"), selected("Stay"), selected("Meals"), selected("Package Theme"), selected("Facilities"));
        String selectedSort = sort.getValue();
        String origin = city.getValue() == null || city.getValue().isBlank() ? null : city.getValue();
        long generation = searchGeneration.incrementAndGet();
        resultTitle.setText(origin == null ? "Packages to Nashik" : origin + " → Nashik");
        resultCount.setText("Loading packages…");
        resultList.getChildren().setAll(loadingState());
        String token = com.simhastha.util.AppSession.currentUser() == null ? "" : com.simhastha.util.AppSession.currentUser().idToken();
        service.searchAsync(criteria, selectedSort, token).whenComplete((packages, error) -> Platform.runLater(() -> {
            if (generation != searchGeneration.get() || resultList == null) return;
            if (error != null) {
                Button retry = button("Try Again", "package-clear-button");
                retry.setOnAction(e -> refresh());
                resultCount.setText("Could not load packages");
                resultList.getChildren().setAll(new VBox(9, text("Packages could not be loaded. Please try again.", "package-empty"), retry));
                return;
            }
            resultCount.setText(packages.size() + " Kumbh package" + (packages.size() == 1 ? "" : "s") + " available");
            if (packages.isEmpty()) {
                Button clear = button("Clear Filters", "package-clear-button");
                clear.setOnAction(e -> clearFilters());
                resultList.getChildren().setAll(new VBox(9, text("No Kumbh packages match your search.", "package-empty"), clear));
            } else resultList.getChildren().setAll(packages.stream().map(this::card).toList());
        }));
    }

    private Node loadingState() { return new VBox(9, text("Loading the Kumbh package catalogue…", "package-loading")); }

    private Node card(KumbhPackage p) {
        StackPane image = packageImage(p, 230, 188, "package-card-image"); VBox imageZone = new VBox(image); imageZone.getStyleClass().add("package-image-zone"); imageZone.setMinWidth(230); imageZone.setPrefWidth(230); imageZone.setMaxWidth(230);
        HBox badges = new HBox(7, badge(p.category().name(), "package-badge-" + p.category().name().toLowerCase()), badge(secondaryBadge(p), "package-badge-secondary"));
        VBox features = new VBox(6, new HBox(7, featureChip("Travel", p.travel()), featureChip("Stay", p.stay()), featureChip("Meals", p.meals())));
        VBox highlights = new VBox(4, text("PACKAGE HIGHLIGHTS", "package-highlights-label")); p.facilities().stream().limit(3).forEach(f -> highlights.getChildren().add(text("✓  " + f, "package-highlight"))); if (p.facilities().size() > 3) highlights.getChildren().add(text("+ " + (p.facilities().size() - 3) + " more experiences", "package-more"));
        VBox infoSection = new VBox(8, badges, text(p.title(), "package-card-title"), text(p.duration(), "package-duration"), text(p.origin() + " → Nashik" + routeSuffix(p), "package-route"), features, highlights); infoSection.setMinWidth(0); infoSection.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(infoSection, Priority.ALWAYS);
        Label priceAmount = text("₹" + String.format("%,d", p.price()), "package-price"); priceAmount.setWrapText(false);
        VBox priceActionSection = new VBox(5, text("Starting From", "package-price-caption"), priceAmount, text("per person", "package-price-person")); if (p.discount() > 0) priceActionSection.getChildren().add(text(p.discount() + "% OFF", "package-discount")); Button view = button("View Package", "package-view-button"); view.setOnAction(e -> navigate.accept(details(p))); Button itinerary = button("View Itinerary", "package-secondary-button"); itinerary.setOnAction(e -> navigate.accept(details(p))); priceActionSection.getChildren().addAll(view, itinerary); priceActionSection.setAlignment(Pos.CENTER_RIGHT); priceActionSection.setMinWidth(160); priceActionSection.setPrefWidth(160); priceActionSection.setMaxWidth(160); HBox.setHgrow(priceActionSection, Priority.NEVER); priceActionSection.getStyleClass().add("package-price-panel");
        HBox packageCard = new HBox(16); packageCard.getChildren().addAll(imageZone, infoSection, priceActionSection); packageCard.setMinWidth(0); packageCard.setMaxWidth(Double.MAX_VALUE); packageCard.setAlignment(Pos.CENTER_LEFT); packageCard.getStyleClass().addAll("package-card", "package-card-" + p.category().name().toLowerCase()); return packageCard;
    }

    private void constrainToOuterViewport(Node page) {
        page.sceneProperty().addListener((observable, oldScene, scene) -> {
            if (scene == null) return;
            Platform.runLater(() -> {
                Node current = page;
                while ((current = current.getParent()) != null) if (current instanceof ScrollPane scrollPane) { scrollPane.setFitToWidth(true); scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); scrollPane.setHvalue(0); return; }
            });
        });
    }

    private Node details(KumbhPackage p) {
        Button back = button("← Back to packages", "package-secondary-button");
        back.getStyleClass().add("package-detail-back-button");
        back.setOnAction(e -> navigate.accept(create()));

        Button customize = button("Customize Package", "package-search-button");
        customize.setOnAction(e -> navigate.accept(new KumbhPackageCustomizationPage(p, navigate, () -> navigate.accept(details(p)), openMyBookings).create()));

        Label title = text(p.title(), "package-detail-title");
        title.setWrapText(true);
        title.setMaxWidth(520);

        StackPane image = detailHeroImage(p);
        Region overlay = new Region();
        overlay.setMouseTransparent(true);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        overlay.getStyleClass().add("package-detail-hero-overlay");

        VBox heroText = new VBox(7,
                badge(p.category().name(), "package-badge-" + p.category().name().toLowerCase()),
                title,
                text(p.origin() + " → Nashik  •  " + p.duration() + "  •  " + p.travel(), "package-detail-hero-meta"),
                back,
                text("Starting From ₹" + String.format("%,d", p.price()) + " / person", "package-detail-price"));
        heroText.setMaxWidth(550);
        StackPane.setAlignment(heroText, Pos.CENTER_LEFT);
        StackPane.setMargin(heroText, new Insets(22, 24, 22, 24));
        StackPane.setAlignment(customize, Pos.CENTER_RIGHT);
        StackPane.setMargin(customize, new Insets(22, 24, 22, 24));

        StackPane detailHero = new StackPane(image, overlay, heroText, customize);
        detailHero.setMinWidth(0);
        detailHero.setMaxWidth(Double.MAX_VALUE);
        detailHero.setMinHeight(250);
        detailHero.setPrefHeight(250);
        detailHero.setMaxHeight(250);
        detailHero.getStyleClass().add("package-detail-hero");
        VBox content = new VBox(15, detailHero, mediaGallery(p), section("Overview", rows(Map.of("Origin", p.origin(), "Destination", "Nashik – Simhastha 2027", "Package category", p.category().name(), "Duration", p.duration(), "Travel mode", p.travel(), "Stay", p.stay(), "Meals", p.meals(), "Kumbh facilities", String.join(", ", p.facilities()), "Suitable for", String.join(", ", p.themes())))), section("Itinerary", itinerary(p)), section("Travel", text(p.travel() + " → Nashik arrival → Simhastha Connect pickup\nReturn: Nashik hotel → departure drop. Travel options can be customized in the next step.", "package-detail-text")), section("Stay", text(p.stay() + " • " + p.nights() + " Nights\nHotel selection will be available during package customization.", "package-detail-text")), section("Meals", text(p.meals() + "\nMeal selections can be customized later.", "package-detail-text")), section("Kumbh Experience", statusList(p.facilities())), section("Nashik Sightseeing", statusList(p.sightseeing())), section("Inclusions", bulletList(p.inclusions())), section("Exclusions", bulletList(p.exclusions())), section("Policies", text("Cancellation, date changes, no-show, child and hotel rules will be confirmed during customization. Kumbh schedules may change for safety or official directions.", "package-detail-text")), section("Important Information", text("This is a package catalogue. Live travel availability is confirmed during booking.", "package-detail-text")), section("Customize Your Journey", text("Travel • Stay • Meals • Kumbh Experience • Nashik Sightseeing • Add-ons\nChange flight, train class, hotel, room, meals, sightseeing, Puja or vehicle in Part 2.", "package-detail-text")));
        content.getStyleClass().add("package-page"); content.setPadding(new Insets(16, 22, 30, 22)); return content;
    }

    private Node itinerary(KumbhPackage p) { VBox timeline = new VBox(12); timeline.getStyleClass().add("package-itinerary"); for (KumbhPackage.Day day : p.itinerary()) { VBox itemBox = new VBox(7, text(day.title(), "package-day-title")); for (KumbhPackage.Item item : day.items()) { HBox line = new HBox(9, text("●", "package-timeline-dot"), text(item.text(), "package-detail-text")); line.setAlignment(Pos.CENTER_LEFT); ItineraryLocation place = resolveItineraryLocation(item.location(), item.text()); if (place != null) { Button location = button("View Location", "package-location-button"); location.setOnAction(e -> openMap(place)); line.getChildren().add(location); } itemBox.getChildren().add(line); } timeline.getChildren().add(itemBox); } return timeline; }
    private Node section(String title, Node content) { VBox section = new VBox(9, text(title, "package-section-title"), content); section.getStyleClass().add("package-section"); return section; }
    private Node rows(Map<String, String> data) { GridPane grid = new GridPane(); grid.setHgap(20); grid.setVgap(9); int row = 0; for (Map.Entry<String, String> entry : data.entrySet()) { grid.add(text(entry.getKey(), "package-overview-key"), 0, row); grid.add(text(entry.getValue(), "package-detail-text"), 1, row++); } return grid; }
    private Node bulletList(List<String> values) { return text("• " + String.join("\n• ", values), "package-detail-text"); }
    private Node statusList(List<String> values) { VBox box = new VBox(7); if (values.isEmpty()) box.getChildren().add(statusRow("No locations configured", "Not Included", "package-status-none")); else values.forEach(value -> box.getChildren().add(statusRow(value, "Included", "package-status-included"))); box.getChildren().add(statusRow("Further experiences", "Optional", "package-status-optional")); return box; }
    private Node statusRow(String name, String state, String css) { HBox row = new HBox(10, text(name, "package-detail-text"), spacer(), badge(state, css)); row.getStyleClass().add("package-status-row"); return row; }

    private Node mediaGallery(KumbhPackage p) { if (p.gallery() == null || p.gallery().isEmpty()) return new VBox(); VBox main = new VBox(6); StackPane selected = mediaImage(p.gallery().get(0), 620, 250, "package-gallery-main"); Label caption = text(p.gallery().get(0).caption(), "package-gallery-caption"); main.getChildren().addAll(selected, caption); HBox thumbs = new HBox(8); p.gallery().stream().limit(5).forEach(media -> { Button thumb = new Button(); thumb.setGraphic(mediaImage(media, 104, 70, "package-gallery-thumb")); thumb.getStyleClass().add("package-gallery-thumb-button"); thumb.setOnAction(e -> { selected.getChildren().setAll(mediaImage(media, 620, 250, "package-gallery-main").getChildren()); caption.setText(media.caption()); }); thumbs.getChildren().add(thumb); }); return section("Package Gallery", new VBox(9, main, thumbs)); }
    private StackPane packageImage(KumbhPackage p, double width, double height, String css) { boolean card = width < 500; PackageMedia media = card ? p.coverImage() : p.heroImage(); if (media == null) media = card ? (p.gallery() == null || p.gallery().isEmpty() ? null : p.gallery().get(0)) : p.coverImage(); if (media == null && !card && p.gallery() != null && !p.gallery().isEmpty()) media = p.gallery().get(0); return media == null ? localImage(imagePath(p), width, height, css) : mediaImage(media, width, height, css); }
    private StackPane detailHeroImage(KumbhPackage p) { PackageMedia media=p.heroImage();if(media==null)media=p.coverImage();if(media==null&&p.gallery()!=null&&!p.gallery().isEmpty())media=p.gallery().get(0);String reference=media==null?Optional.ofNullable(AppResources.url(getClass(), imagePath(p))).map(URL::toExternalForm).orElse(""):PackageMediaService.temporary().resolveReference(media);StackPane frame=new StackPane();frame.setMinWidth(0);frame.setMaxWidth(Double.MAX_VALUE);frame.setMinHeight(250);frame.setPrefHeight(250);frame.setMaxHeight(250);frame.getStyleClass().add("package-detail-image");Rectangle clip=new Rectangle();clip.widthProperty().bind(frame.widthProperty());clip.heightProperty().bind(frame.heightProperty());clip.setArcWidth(18);clip.setArcHeight(18);frame.setClip(clip);try{Image image=new Image(reference,false);if(image.isError()||image.getWidth()<=0){frame.getChildren().add(text("NASHIK SIMHASTHA 2027","package-image-fallback"));return frame;}ImageView view=new ImageView(image);view.setPreserveRatio(true);view.setSmooth(true);frame.widthProperty().addListener((o,a,b)->fitHeroCover(view,image,frame.getWidth(),250));fitHeroCover(view,image,frame.getWidth(),250);frame.getChildren().add(view);}catch(Exception ignored){frame.getChildren().add(text("NASHIK SIMHASTHA 2027","package-image-fallback"));}return frame; }
    private void fitHeroCover(ImageView view,Image image,double width,double height){if(width<=0||image.getWidth()<=0||image.getHeight()<=0)return;double imageRatio=image.getWidth()/image.getHeight(),targetRatio=width/height;view.setFitWidth(imageRatio<targetRatio?width:0);view.setFitHeight(imageRatio<targetRatio?0:height);}
    private StackPane mediaImage(PackageMedia media, double width, double height, String css) { return imageFrame(PackageMediaService.temporary().resolveReference(media), width, height, css); }
    private StackPane localImage(String path, double width, double height, String css) { URL url = AppResources.url(getClass(), path); return imageFrame(url == null ? "" : url.toExternalForm(), width, height, css); }
    private StackPane imageFrame(String reference, double width, double height, String css) { StackPane frame = new StackPane(); frame.setMinSize(width, height); frame.setPrefSize(width, height); frame.setMaxSize(width, height); frame.getStyleClass().add(css); frame.getChildren().add(text("NASHIK SIMHASTHA 2027", "package-image-fallback")); try { Image image = new Image(reference, width * 2, height * 2, true, true, true); ImageView view = new ImageView(image); view.setPreserveRatio(true); Runnable renderImage = () -> { if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) return; double sourceRatio = image.getWidth() / image.getHeight(), targetRatio = width / height, cropWidth = image.getWidth(), cropHeight = image.getHeight(); if (sourceRatio > targetRatio) cropWidth = cropHeight * targetRatio; else cropHeight = cropWidth / targetRatio; view.setViewport(new Rectangle2D((image.getWidth() - cropWidth) / 2, (image.getHeight() - cropHeight) / 2, cropWidth, cropHeight)); view.setFitWidth(width); view.setFitHeight(height); Rectangle clip = new Rectangle(width, height); clip.setArcWidth(18); clip.setArcHeight(18); view.setClip(clip); if (!frame.getChildren().contains(view)) frame.getChildren().add(view); }; image.progressProperty().addListener((o, old, value) -> renderImage.run()); renderImage.run(); } catch (Exception ignored) { } return frame; }
    private String imagePath(KumbhPackage p) { String t = (p.title() + " " + String.join(" ", p.facilities())).toLowerCase(); if (t.contains("trimbakeshwar")) return "/images/trimbakeshwar.jpg"; if (t.contains("ramkund") || p.days() == 1) return "/images/ramkund_sunrise.jpg"; if (p.category() == PackageCategory.PREMIUM) return "/images/godavari_kumbh.jpg"; return "/images/welcome-light.png"; }
    private String routeSuffix(KumbhPackage p) { return p.sightseeing().isEmpty() ? "" : " → " + String.join(" → ", p.sightseeing().stream().limit(2).toList()); }
    private String secondaryBadge(KumbhPackage p) { return switch (p.category()) { case PREMIUM -> "PREMIUM EXPERIENCE"; case STANDARD -> "BEST VALUE"; case BUDGET -> "VALUE PICK"; }; }
    private Node featureChip(String label, String value) { return text(label + "  " + value, "package-feature-chip"); }
    private Set<String> selected(String group) { Set<String> result = new HashSet<>(); filters.forEach((key, check) -> { if (key.startsWith(group + ":") && check.isSelected()) result.add(key.substring(group.length() + 1)); }); return result; }
    private PackageCategory parseCategory(String selected) { return selected == null || selected.equals("All Packages") ? null : PackageCategory.valueOf(selected.toUpperCase()); }
    private static ComboBox<String> combo(String prompt, List<String> values, boolean editable) { ComboBox<String> combo = new ComboBox<>(); combo.getItems().addAll(values); combo.setPromptText(prompt); combo.setEditable(editable); combo.getStyleClass().add("package-combo"); if (!editable) combo.setValue(values.get(0)); return combo; }
    private Node searchField(String label, Node control, String hint) { VBox box = new VBox(3, text(label, "package-field-label"), control, text(hint, "package-field-hint")); box.setMinWidth(0); box.setMaxWidth(Double.MAX_VALUE); if (control instanceof Region region) { region.setMinWidth(0); region.setMaxWidth(Double.MAX_VALUE); } box.getStyleClass().addAll("package-search-field", "package-search-field-" + label.toLowerCase().replace(' ', '-')); HBox.setHgrow(box, Priority.ALWAYS); return box; }
    private Label fixed(String value) { Label label = text(value, "package-fixed-field"); label.setTextOverrun(OverrunStyle.CLIP); label.setWrapText(true); return label; }
    private Label text(String value, String css) { Label label = new Label(value); label.setWrapText(true); label.getStyleClass().add(css); return label; }
    private Label badge(String value, String css) { return text(value, css); }
    private Button button(String value, String css) { Button button = new Button(value); button.getStyleClass().add(css); return button; }
    private Region spacer() { Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS); return spacer; }
    private void info(String message) { new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait(); }
    /** Resolves only real Nashik / Simhastha itinerary places before opening the live map. */
    private ItineraryLocation resolveItineraryLocation(String location, String activity) {
        String value = ((location == null ? "" : location) + " " + (activity == null ? "" : activity)).toLowerCase(Locale.ROOT);
        return ITINERARY_LOCATIONS.stream().filter(mapping -> mapping.matches(value)).map(LocationMapping::place).findFirst().orElse(null);
    }

    /** Opens Google Maps directions; no origin is supplied so Maps can use the user's current location. */
    private void openMap(ItineraryLocation place) { try { String destination = URLEncoder.encode(place.name(), StandardCharsets.UTF_8); Desktop.getDesktop().browse(URI.create("https://www.google.com/maps/dir/?api=1&destination=" + destination + "&travelmode=driving")); } catch (Exception exception) { info("Directions to: " + place.name() + " (" + place.latitude() + ", " + place.longitude() + ")"); } }
    private record ItineraryLocation(String name, double latitude, double longitude) { }
    private record LocationMapping(ItineraryLocation place, String... aliases) { boolean matches(String value) { return Arrays.stream(aliases).anyMatch(value::contains); } }
}
