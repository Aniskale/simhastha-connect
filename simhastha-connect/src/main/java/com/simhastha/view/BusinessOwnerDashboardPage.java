package com.simhastha.view;

import com.simhastha.controller.BusinessDashboardController;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.model.BusinessMedia;
import com.simhastha.model.BusinessLocation;
import com.simhastha.model.BusinessProfileUpdate;
import com.simhastha.model.PublicBusinessItem;
import com.simhastha.util.AppSession;
import com.simhastha.util.NavigationUtil;

import java.io.File;
import java.net.URI;
import com.simhastha.config.CloudinaryFolders;
import com.simhastha.model.CloudImage;
import com.simhastha.model.CloudinaryUploadResult;
import com.simhastha.service.CloudinaryService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** One owner-scoped dashboard. It resolves a business from AppSession, never a UI id. */
public class BusinessOwnerDashboardPage {
    private static final Logger LOGGER = Logger.getLogger(BusinessOwnerDashboardPage.class.getName());
    private static final List<Item> LOCAL_ITEMS = new ArrayList<>();
    private static final Map<String, File> LOCAL_ITEM_PHOTOS = new java.util.HashMap<>();
    private final BusinessAuthPage.BusinessAccount fallbackAccount;
    private final BusinessDashboardController controller = new BusinessDashboardController();
    private final Map<String, Button> navigation = new java.util.LinkedHashMap<>();
    private final Set<String> readNotifications = new java.util.HashSet<>();
    private final List<Item> items = new ArrayList<>();
    private BorderPane root;
    private Business business;
    private String loadMessage = "";

    public BusinessOwnerDashboardPage(BusinessAuthPage.BusinessAccount account) { this.fallbackAccount = account; }

    public Scene createScene(Stage stage) {
        resolveBusiness();
        root = new BorderPane(); root.getStyleClass().add("business-dashboard-root"); root.setLeft(buildSidebar(stage)); showDashboard();
        Scene scene = new Scene(root, 1200, 680); ThemeManager.addTheme(scene, this); ThemeManager.addListener(() -> ThemeManager.applyTo(scene.getRoot())); return scene;
    }

    private void resolveBusiness() {
        AppSession.User user = AppSession.currentUser();
        String ownerId = user != null && user.uid() != null && !user.uid().isBlank() ? user.uid() : fallbackAccount.email;
        business = new Business(ownerId, ownerId, fallbackAccount.businessName, fallbackAccount.ownerName,
                fallbackAccount.category, "ACTIVE", "", "", fallbackAccount.location, fallbackAccount.address,
                fallbackAccount.area, fallbackAccount.city, fallbackAccount.latitude, fallbackAccount.longitude,
                fallbackAccount.locationUpdatedAt, fallbackAccount.mobile, fallbackAccount.email, "", "",
                fallbackAccount.logoUrl, fallbackAccount.logoPublicId, fallbackAccount.coverPhotoUrl,
                fallbackAccount.coverPhotoPublicId, fallbackAccount.galleryImages);
        items.clear();
        if (user != null && controller.isFirebaseEnabled()) {
            try {
                FirestoreGateway.BusinessProfile profile = controller.findBusinessForOwner(user.uid(), user.idToken())
                        .orElse(null);
                if (profile == null || !user.uid().equals(profile.ownerId())) { business = null; return; }
                business = new Business(profile.businessId(), profile.ownerId(), profile.businessName(),
                        profile.ownerName(), profile.category(), profile.status(), profile.approved(),
                        profile.description(), profile.location(), profile.address(), profile.area(), profile.city(),
                        profile.latitude(), profile.longitude(), profile.locationUpdatedAt(), profile.mobile(),
                        profile.email(), profile.operatingHours(), profile.priceRange(), profile.logoUrl(),
                        profile.logoPublicId(), profile.coverPhotoUrl(), profile.coverPhotoPublicId(),
                        profile.galleryImages());
                try {
                    for (FirestoreGateway.BusinessInventoryItem value :
                            controller.findInventory(business.businessId, business.ownerId, user.idToken())) {
                        Item item = Item.from(value);
                        items.add(item);
                        if (item.active) rememberPublicItem(item);
                    }
                    loadMessage = "";
                } catch (Exception inventoryException) {
                    LOGGER.log(Level.FINE, "Online business items are unavailable for owner " + ownerId,
                            inventoryException);
                    loadLocalItems();
                    loadMessage = "Online item sync is blocked by Firestore permissions. Showing saved session items.";
                }
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Business dashboard details could not be refreshed for owner " + ownerId,
                        exception);
                loadLocalItems();
                loadMessage = "Online item sync is blocked by Firestore permissions. Showing saved session items.";
            }
        } else {
            loadLocalItems();
            if (items.isEmpty()) {
                for (String service : fallbackAccount.services) {
                    Item item = new Item(UUID.randomUUID().toString(), ownerId, ownerId, type(), "Service", service,
                            "", "0", "0", "0", "0", "0", "", "Available", true);
                    LOCAL_ITEMS.add(item);
                    items.add(item);
                }
            }
        }
    }

    private VBox buildSidebar(Stage stage) {
        ImageView logo = new ImageView(); var url = getClass().getResource("/images/sclogo.png"); if (url != null) logo.setImage(new Image(url.toExternalForm())); logo.setFitWidth(46); logo.setFitHeight(46); logo.setPreserveRatio(true);
        HBox brand = new HBox(9, logo, new VBox(1, label("SIMHASTHA\nCONNECT", "business-sidebar-brand"), label("Nashik Simhastha 2027", "business-sidebar-tagline"))); brand.setAlignment(Pos.CENTER_LEFT);
        VBox nav = new VBox(3, nav("dashboard", "Dashboard", "\uE80F"), nav("business", "My Business", "\uE719"), nav("services", inventoryLabel(), "\uE8D4"), nav("bookings", "Bookings / Requests", "\uE8A7"), nav("customers", "Customers", "\uE7EF"), nav("reviews", "Reviews & Feedback", "\uE87D"), nav("notifications", "Notifications", AppUi.notificationBellGlyph()), nav("payments", "Payments & Settlements", "\uE8A1"), nav("profile", "Profile", "\uE7FD"), nav("settings", "Settings", "\uE8B8"));
        Button logout = nav("logout", "Logout", "\uE7E8"); logout.getStyleClass().add("business-sidebar-logout"); logout.setOnAction(e -> { AppSession.clear(); NavigationUtil.navigate(stage, new BusinessAuthPage().createScene(stage)); });
        VBox box = new VBox(14, brand, nav, spacer(), logout); box.getStyleClass().add("business-sidebar"); box.setPadding(new Insets(18, 13, 16, 13)); box.setPrefWidth(250); return box;
    }
    private Button nav(String key, String text, String icon) { Button b = new Button(text, AppUi.symbolIcon(icon, "business-nav-icon")); b.setMaxWidth(Double.MAX_VALUE); b.getStyleClass().add("business-nav-button"); navigation.put(key,b); b.setOnAction(e -> { if (key.equals("logout")) return; setActive(key); if (key.equals("dashboard")) showDashboard(); else if (key.equals("services")) showServices(); else if (key.equals("bookings")) showBookings(); else if (key.equals("customers")) showCustomers(); else if (key.equals("reviews")) showReviews(); else if (key.equals("notifications")) showNotifications(); else if (key.equals("payments")) showPayments(); else if (key.equals("profile")) showProfile(); else if (key.equals("settings")) showSettings(); else showBusinessProfile(); }); return b; }
    private void showDashboard() { setActive("dashboard"); root.setCenter(scroll(buildDashboard())); }
    private void showServices() { root.setCenter(scroll(pageShell(managementTitle(), managementHint(), buildInventoryPanel()))); }
    private void showBookings() { root.setCenter(scroll(pageShell("Bookings / Requests", "Booking records connected to your resolved business.", bookingsPanel(false)))); }
    private void showCustomers() { root.setCenter(scroll(pageShell("Customers", "Customers with bookings for this business only.", customersPanel()))); }
    private void showReviews() { root.setCenter(scroll(pageShell("Reviews & Feedback", "Customer feedback for this business will appear here.", empty("No reviews yet.", "Customer feedback will appear here.")))); }
    private void showNotifications() { root.setCenter(scroll(pageShell("Notifications", "Business-specific operational updates.", notificationsPanel()))); }
    private void showProfile() { root.setCenter(scroll(pageShell("Profile", "Business owner identity and registration details.", profilePanel()))); }
    private void showPayments() { root.setCenter(scroll(pageShell("Payments & Settlements", "Verified booking payments collected through SIMHASTHA CONNECT.", paymentsPanel()))); }
    private void showSettings() { CheckBox bookingAlerts=new CheckBox("Show booking-request alerts in this workspace"); bookingAlerts.setSelected(true); CheckBox paymentAlerts=new CheckBox("Show verified payment-status alerts"); paymentAlerts.setSelected(true); CheckBox autoRefresh=new CheckBox("Refresh dashboard data when opening a section"); autoRefresh.setSelected(true); CheckBox compactRows=new CheckBox("Use compact rows for bookings and payments"); Button save=new Button("Save Preferences"); save.getStyleClass().add("primary-button"); save.setOnAction(e->show("Settings", "Preferences saved for this session.")); VBox preferences=new VBox(10, label("Notification preferences", "business-row-title"), bookingAlerts, paymentAlerts, label("Dashboard preferences", "business-row-title"), autoRefresh, compactRows, label("These preferences affect only this business workspace.", "business-row-detail"), save); root.setCenter(scroll(pageShell("Settings", "Personal workspace preferences.", panel(section("Appearance"), label("Use the visible Sun button for Light mode and Moon button for Dark mode.", "business-row-detail"), AppUi.createThemeToggle()), panel(section("Workspace Preferences"),preferences), panel(section("Account Security"),label("Business approval, category, payments, and account permissions are managed centrally by SIMHASTHA CONNECT.", "business-row-detail"))))); }
    private void showBusinessProfile() { root.setCenter(scroll(pageShell("My Business", "Your resolved business registration details.", profilePanel()))); }

    private VBox buildDashboard() {
        if (business == null) return pageShell("Business Dashboard", "No business is linked to this account.", empty("No business is linked to this account.", "Please contact support if you believe this is incorrect."));
        List<AppDataStore.BookingRecord> bookings = bookings();
        HBox stats = new HBox(14, stat("Today's Bookings", String.valueOf(bookings.stream().filter(b -> LocalDate.now().toString().equals(b.dateText)).count()), "Bookings scheduled today"), stat("Pending Requests", String.valueOf(bookings.stream().filter(this::pending).count()), "Awaiting confirmation"), stat("Total Customers", String.valueOf(bookings.stream().map(b -> b.customerName).filter(n -> n != null && !n.isBlank()).distinct().count()), "Customers with bookings"), stat("Business Status", displayStatus(), statusMessage()));
        GridPane top = grid(62,38); Node recent=bookingsPanel(true), overview=overview(); top.add(recent,0,0); top.add(overview,1,0); GridPane.setHgrow(recent,Priority.ALWAYS); GridPane.setHgrow(overview,Priority.ALWAYS);
        GridPane bottom=grid(50,50); bottom.add(recentActivity(bookings),0,0); bottom.add(adminUpdates(),1,0);
        VBox out=new VBox(18, header(),stats,top,bottom); out.getStyleClass().add("business-dashboard-content"); out.setPadding(new Insets(22,28,32,28)); return out;
    }
    private HBox header() { String owner=business==null?fallbackAccount.ownerName:business.ownerName; VBox title=new VBox(3,label("Welcome, "+text(owner,"Business Owner"),"business-welcome"),label(text(business==null?"":business.name,"Your Business"),"business-name")); HBox box=new HBox(14,title,spacer(),label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",Locale.ENGLISH)),"business-header-date"),AppUi.createThemeToggle(),notificationBell(),AppUi.createProfileChip(text(owner,"Business Owner"),()->{setActive("profile");showProfile();}),statusBadge(displayStatus())); box.setAlignment(Pos.CENTER_LEFT); box.getStyleClass().add("business-dashboard-header"); return box; }
    private VBox overview() { VBox data=new VBox(9,overviewRow("Business Name",business.name),overviewRow("Category",business.category),overviewRow("Business Status",displayStatus()),overviewRow("Active services / items",String.valueOf(items.stream().filter(i->i.active).count())),overviewRow("Availability",items.stream().anyMatch(i->i.active&&"Available".equalsIgnoreCase(i.availability))?"Available":"Not available"),overviewRow("Verification",business.approved.equalsIgnoreCase("true")?"Verified":"Not verified")); Button manage=new Button("Manage Business"); manage.getStyleClass().add("primary-button"); manage.setDisable(!canManage()); manage.setOnAction(e->{setActive("services");showServices();}); return panel(section("Business Overview"),data,manage); }
    private VBox bookingsPanel(boolean compact) { VBox rows=new VBox(8); List<AppDataStore.BookingRecord> bookings=bookings(); if(bookings.isEmpty()) rows.getChildren().add(empty("No bookings yet","New booking requests will appear here.")); else bookings.stream().limit(compact?5:Integer.MAX_VALUE).forEach(b->rows.getChildren().add(booking(b, !compact))); Button all=new Button("View All"); all.getStyleClass().add("business-text-action"); all.setOnAction(e->{setActive("bookings");showBookings();}); HBox title=new HBox(section("Recent Bookings"),spacer(),all); title.setAlignment(Pos.CENTER_LEFT); return panel(title,rows); }
    private VBox customersPanel() { VBox rows=new VBox(8); java.util.Map<String,List<AppDataStore.BookingRecord>> groups=bookings().stream().filter(b->b.userId!=null&&!b.userId.isBlank()).collect(Collectors.groupingBy(b->b.userId)); if(groups.isEmpty())rows.getChildren().add(empty("No customers yet", "Customers appear after they book with this business.")); else groups.values().forEach(list->{AppDataStore.BookingRecord latest=list.get(list.size()-1); Button view=new Button("View history");view.getStyleClass().add("business-text-action");view.setOnAction(e->showCustomerHistory(list));HBox row=new HBox(10,new VBox(2,label(text(latest.customerName,"Customer"),"business-row-title"),label(list.size()+" booking(s) • Latest: "+text(latest.bookingStatus,"Pending"),"business-row-detail")),spacer(),view);row.getStyleClass().add("business-data-row");row.setAlignment(Pos.CENTER_LEFT);rows.getChildren().add(row);}); return panel(section("Business Customers"),rows); }
    private void showCustomerHistory(List<AppDataStore.BookingRecord> history) { VBox rows=new VBox(8); history.forEach(b->rows.getChildren().add(simple(text(b.title,"Service"),text(b.dateText,"Date not available")+" • "+text(b.bookingStatus,"Pending")))); root.setCenter(scroll(pageShell(text(history.get(0).customerName,"Customer"),"Bookings with this business only.",panel(section("Booking History"),rows)))); }
    private VBox notificationsPanel() { VBox rows=new VBox(8); List<NotificationCenter.NotificationItem> data=businessNotifications(); if(data.isEmpty())rows.getChildren().add(empty("No notifications", "Business booking updates will appear here.")); else data.forEach(n->rows.getChildren().add(simple(n.title(), n.detail()))); return panel(section("Operational Notifications"),rows); }
    private VBox paymentsPanel() { List<AppDataStore.BookingRecord> paid=bookings().stream().filter(b->"PAID".equalsIgnoreCase(b.paymentStatus)||"VERIFIED".equalsIgnoreCase(b.paymentStatus)).toList(); long verifiedAmount=paid.stream().mapToLong(b->b.amountPaise).sum(); HBox summary=new HBox(14,stat("Verified Payments",String.valueOf(paid.size()),"Razorpay-verified booking records"),stat("Verified Collection","₹"+(verifiedAmount/100),"Historical payment total"),stat("Settlement Status",paid.isEmpty()?"No data":"Tracked","Settlement release is server controlled")); VBox rows=new VBox(8); if(paid.isEmpty())rows.getChildren().add(empty("No verified payments yet","Verified Razorpay booking payments will appear here.")); else paid.forEach(b->{VBox info=new VBox(2,label(text(b.customerName,"Customer")+" • "+text(b.title,"Service"),"business-row-title"),label("Booking: "+b.bookingId+" • ₹"+(b.amountPaise/100)+" • Payment: "+b.paymentStatus,"business-row-detail"));HBox row=new HBox(10,AppUi.symbolIcon("\uE8A1","business-row-icon"),info,spacer(),statusBadge(text(b.bookingStatus,"Pending")));row.setAlignment(Pos.CENTER_LEFT);row.getStyleClass().add("business-data-row");rows.getChildren().add(row);}); Button refresh=new Button("Refresh Records");refresh.getStyleClass().add("business-text-action");refresh.setOnAction(e->showPayments()); return new VBox(16,summary,panel(section("Verified Payment Records"),rows,refresh),panel(section("Settlement Control"),label("SIMHASTHA CONNECT keeps Razorpay payment verification separate from settlement release. This page is read-only until a secure server-side Razorpay payout/Route integration is configured.","business-row-detail"))); }
    private VBox profilePanel() { if(business==null)return panel(empty("No business is linked to this account.","Profile details are not available.")); Button edit=new Button("Edit Business Profile");edit.getStyleClass().add("primary-button");edit.setOnAction(e->editBusinessProfile()); Button locationButton=new Button("Select Business Location on Map");locationButton.getStyleClass().add("primary-button");locationButton.setOnAction(e->updateBusinessLocation()); HBox actions=new HBox(10,edit,locationButton);actions.setAlignment(Pos.CENTER_LEFT); return panel(section("Business Profile"),businessMediaPanel(),overviewRow("Owner Name",business.ownerName),overviewRow("Business Name",business.name),overviewRow("Business Category",business.category),overviewRow("Email",text(business.email,fallbackAccount.email)),overviewRow("Phone",text(business.mobile,fallbackAccount.mobile)),overviewRow("Location",business.displayLocation()),overviewRow("Coordinates",business.hasCoordinates()?business.latitude+", "+business.longitude:"Not selected"),overviewRow("Approval Status",displayStatus()),overviewRow("Verification",business.approved.equalsIgnoreCase("true")?"Verified":"Not verified"),actions); }
    private VBox businessMediaPanel() {
        ImageView logo = ImageMediaHelper.imageView(text(business.logoUrl, ImageMediaHelper.FALLBACK_IMAGE), 96, 96);
        ImageView cover = ImageMediaHelper.imageView(text(business.coverPhotoUrl, business.logoUrl), 260, 120);
        Button logoButton = smallMediaButton("Upload / Change Logo");
        logoButton.setOnAction(event -> uploadBusinessImage("logo"));
        Button coverButton = smallMediaButton("Upload / Change Cover");
        coverButton.setOnAction(event -> uploadBusinessImage("cover"));
        Button addPhotos = smallMediaButton("+ Add Photos");
        addPhotos.setOnAction(event -> uploadBusinessImage("gallery"));
        HBox previews = new HBox(10, logo, cover);
        previews.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(8, logoButton, coverButton, addPhotos);
        FlowPane gallery = new FlowPane(8, 8);
        for (CloudImage image : business.galleryImages) {
            ImageView thumb = ImageMediaHelper.imageView(image.url(), 112, 78);
            Button remove = smallMediaButton("Remove");
            remove.setOnAction(event -> removeBusinessGalleryImage(image));
            gallery.getChildren().add(new VBox(4, thumb, remove));
        }
        if (business.galleryImages.isEmpty()) {
            gallery.getChildren().add(label("No gallery photos yet.", "business-row-detail"));
        }
        return new VBox(10, section("Business Media"), previews, actions, gallery);
    }
    private Button smallMediaButton(String text) { Button button=new Button(text); button.getStyleClass().add("business-text-action"); button.setDisable(!canManage()); return button; }
    private void uploadBusinessImage(String kind) {
        if (!canManage()) return;
        File file = ImageMediaHelper.chooseImage(root.getScene() == null ? null : root.getScene().getWindow(),
                "Choose Business Image");
        if (file == null) return;
        try { ImageMediaHelper.validateImage(file); } catch (IllegalArgumentException exception) { show("Image", exception.getMessage()); return; }
        AppSession.User user = AppSession.currentUser();
        if (user == null || !controller.isFirebaseEnabled()) { show("Image", "Please login with Firebase before changing business media."); return; }
        String oldPublicId = "logo".equals(kind) ? business.logoPublicId : "cover".equals(kind) ? business.coverPhotoPublicId : "";
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                CloudinaryService cloudinary = new CloudinaryService();
                String folder = "logo".equals(kind) ? CloudinaryFolders.BUSINESS_LOGO : CloudinaryFolders.BUSINESS_GALLERY;
                CloudinaryUploadResult uploaded = cloudinary.uploadImage(file, folder);
                Business updated = business.withMedia(kind, uploaded);
                controller.updateBusinessMedia(updated.businessId, updated.logoUrl, updated.logoPublicId,
                        updated.coverPhotoUrl, updated.coverPhotoPublicId, updated.galleryImages, user.idToken());
                if (!oldPublicId.isBlank()) {
                    try { cloudinary.deleteImage(oldPublicId); } catch (java.io.IOException ignored) { }
                }
                return updated;
            } catch (Exception exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((updated, error) -> javafx.application.Platform.runLater(() -> {
            if (error != null) { show("Image", "Business media could not be saved."); return; }
            business = updated;
            rememberBusinessMedia();
            showBusinessProfile();
        }));
    }
    private void removeBusinessGalleryImage(CloudImage image) {
        if (!canManage() || image == null) return;
        AppSession.User user = AppSession.currentUser();
        if (user == null || !controller.isFirebaseEnabled()) return;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                List<CloudImage> updatedGallery = business.galleryImages.stream()
                        .filter(existing -> !existing.publicId().equals(image.publicId())).toList();
                controller.updateBusinessMedia(business.businessId, business.logoUrl, business.logoPublicId,
                        business.coverPhotoUrl, business.coverPhotoPublicId, updatedGallery, user.idToken());
                if (!image.publicId().isBlank()) new CloudinaryService().deleteImage(image.publicId());
                business = business.withGallery(updatedGallery);
            } catch (Exception exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((ignored, error) -> javafx.application.Platform.runLater(() -> {
            if (error != null) show("Image", "Gallery photo could not be removed.");
            rememberBusinessMedia();
            showBusinessProfile();
        }));
    }
    private void rememberBusinessMedia(){if(business==null)return;List<BusinessMedia> media=new ArrayList<>();if(!business.logoUrl.isBlank())media.add(new BusinessMedia("logo-"+business.businessId,business.businessId,business.ownerId,business.logoUrl,business.logoPublicId,"logo",false,String.valueOf(System.currentTimeMillis()),true));if(!business.coverPhotoUrl.isBlank())media.add(new BusinessMedia("cover-"+business.businessId,business.businessId,business.ownerId,business.coverPhotoUrl,business.coverPhotoPublicId,"cover",true,String.valueOf(System.currentTimeMillis()),true));for(CloudImage image:business.galleryImages)media.add(new BusinessMedia("gallery-"+Math.abs((image.url()+image.publicId()).hashCode()),business.businessId,business.ownerId,image.url(),image.publicId(),"gallery",media.stream().noneMatch(BusinessMedia::cover),String.valueOf(System.currentTimeMillis()),true));AppDataStore.rememberBusinessMedia(business.businessId,media);if(business.ownerId!=null&&!business.ownerId.equals(business.businessId))AppDataStore.rememberBusinessMedia(business.ownerId,media);}
    private void editBusinessProfile(){ if(business==null)return; Dialog<BusinessProfileUpdate> dialog=new Dialog<>();dialog.setTitle("Edit Business Profile");dialog.getDialogPane().getButtonTypes().addAll(new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE),new javafx.scene.control.ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE));TextField name=field(business.name,"Business Name");TextArea desc=new TextArea(text(business.description,""));desc.setPromptText("Business Description");desc.setPrefRowCount(3);TextField phone=field(text(business.mobile,fallbackAccount.mobile),"Phone");TextField email=field(text(business.email,fallbackAccount.email),"Email");TextField address=field(business.address,"Address");TextField area=field(business.area,"Area");TextField city=field(business.city,"City");TextField hours=field(business.operatingHours,"Opening information");TextField price=field(business.priceRange,"Price range");GridPane form=new GridPane();form.setHgap(10);form.setVgap(9);Node[] fields={name,desc,phone,email,address,area,city,hours,price};String[] labels={"Business Name","Description","Phone","Email","Address","Area","City","Opening Info","Price Range"};for(int i=0;i<fields.length;i++){form.add(label(labels[i],"business-overview-key"),0,i);form.add(fields[i],1,i);GridPane.setHgrow(fields[i],Priority.ALWAYS);}dialog.getDialogPane().setContent(form);dialog.setResultConverter(button->{if(button.getButtonData()!=javafx.scene.control.ButtonBar.ButtonData.OK_DONE)return null;if(name.getText().trim().isEmpty()){show("Validation","Business name is required.");return null;}return new BusinessProfileUpdate(business.businessId,business.ownerId,name.getText().trim(),desc.getText().trim(),phone.getText().trim(),email.getText().trim(),address.getText().trim(),area.getText().trim(),city.getText().trim(),hours.getText().trim(),price.getText().trim());});dialog.showAndWait().ifPresent(update->{AppSession.User user=AppSession.currentUser();if(user!=null&&controller.isFirebaseEnabled())try{controller.updateBusinessProfile(update,user.idToken());}catch(Exception exception){LOGGER.log(Level.WARNING,"Business profile could not be saved online for "+business.businessId,exception);show("Profile","Profile is updated in this session but could not be saved online.");}business=business.withProfile(update);showBusinessProfile();});}
    private void updateBusinessLocation(){ if(business==null)return; try{BusinessLocationSelectorDialog.show(business.location()).ifPresent(value->{ if(!value.hasCoordinates()){show("Location","Please select a valid map point before saving.");return;} AppSession.User user=AppSession.currentUser(); if(user!=null&&controller.isFirebaseEnabled())try{controller.updateBusinessLocation(business.businessId,value,user.idToken());}catch(Exception exception){LOGGER.log(Level.WARNING,"Business location could not be saved online for "+business.businessId,exception);show("Location","Location is updated in this session but could not be saved online.");} business=business.withLocation(value); showBusinessProfile(); });}catch(RuntimeException exception){LOGGER.log(Level.SEVERE,"Business location selector could not be opened.",exception);show("Select Business Location on Map","Map could not be opened. Please try again.");} }
    private VBox recentActivity(List<AppDataStore.BookingRecord> data) { VBox rows=new VBox(8); if(data.isEmpty())rows.getChildren().add(empty("No recent activity","Business activity will appear here.")); else data.stream().limit(3).forEach(b->rows.getChildren().add(simple("Booking "+text(b.bookingStatus,"updated"),text(b.title,"Service")+" • "+text(b.updatedAt,"Date not available")))); return panel(section("Recent Activity"),rows); }
    private VBox adminUpdates() { VBox rows=new VBox(8); List<AppDataStore.ServiceItem> notices=AppDataStore.items("announcement"); if(notices.isEmpty())rows.getChildren().add(empty("No new admin updates","Important business updates will appear here.")); else notices.stream().limit(3).forEach(n->rows.getChildren().add(simple(n.title,n.detail))); return panel(section("Admin Updates"),rows); }
    private VBox buildInventoryPanel() { if(business==null)return panel(empty("No business is linked to this account.","Inventory cannot be managed.")); VBox rows=new VBox(8); List<Item> visible=items.stream().filter(i->i.active).toList(); if(visible.isEmpty())rows.getChildren().add(empty("No services/items added yet.","Add an operational item when your business is active.")); else visible.forEach(i->rows.getChildren().add(itemRow(i))); Button add=new Button(addLabel()); add.getStyleClass().add("primary-button"); add.setDisable(!canManage()); add.setOnAction(e->editItem(null)); VBox content=panel(section(managementTitle()),rows,add); if(!canManage())content.getChildren().add(label(statusMessage(),"business-row-detail")); if(!loadMessage.isBlank())content.getChildren().add(label(loadMessage,"business-row-detail")); return content; }
    private HBox itemRow(Item item) { VBox info=new VBox(2,label(item.name,"business-row-title"),label(itemSummary(item),"business-row-detail")); Node visual=itemVisual(item); Button edit=new Button("Edit"); edit.getStyleClass().add("business-text-action"); edit.setDisable(!canManage()||!owns(item)); edit.setOnAction(e->editItem(item)); Button deactivate=new Button("Deactivate"); deactivate.getStyleClass().add("text-button"); deactivate.setDisable(!canManage()||!owns(item)); deactivate.setOnAction(e->{boolean oldActive=item.active;item.active=false;if(save(item)){rememberPublicItem(item);showServices();}else{item.active=oldActive;}}); HBox row=new HBox(10,visual,info,spacer(),edit,deactivate); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("business-data-row"); return row; }
    private Node itemVisual(Item item){File photo=LOCAL_ITEM_PHOTOS.get(item.id);if(photo!=null&&photo.isFile()){ImageView image=new ImageView(new Image(photo.toURI().toString(),46,46,true,true));image.setFitWidth(46);image.setFitHeight(46);image.setPreserveRatio(false);return image;}return AppUi.symbolIcon("\uE8D4","business-row-icon");}
    private void editItem(Item existing) {
        if(!canManage() || (existing!=null&&!owns(existing))) return;
        ItemFormSpec spec = itemFormSpec();
        Dialog<ButtonTypeResult> dialog=new Dialog<>(); dialog.setTitle(existing==null?addLabel():"Edit "+itemNoun()); javafx.scene.control.ButtonType saveButtonType=new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE); javafx.scene.control.ButtonType cancelButtonType=new javafx.scene.control.ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE); dialog.getDialogPane().getButtonTypes().addAll(saveButtonType,cancelButtonType);
        TextField name=field(existing==null?"":existing.name,spec.namePrompt()); TextField itemType=field(existing==null?spec.defaultType():existing.itemType,spec.typePrompt()); TextField price=field(existing==null?"0":existing.price,spec.pricePrompt()); TextField capacity=field(existing==null?"0":existing.capacity,spec.capacityPrompt()); TextField total=field(existing==null?"0":existing.totalUnits,spec.totalPrompt()); TextField available=field(existing==null?"0":existing.availableUnits,spec.availablePrompt()); TextField stock=field(existing==null?"0":existing.stock,spec.stockPrompt()); TextField facilities=field(existing==null?"":existing.facilities,spec.facilitiesPrompt()); TextArea description=new TextArea(existing==null?"":existing.description); description.setPromptText(spec.descriptionPrompt()); description.setPrefRowCount(2); CheckBox availability=new CheckBox(spec.availabilityLabel()); availability.setSelected(existing==null||"Available".equalsIgnoreCase(existing.availability));
        ImagePickerPane itemPhoto=new ImagePickerPane(spec.photoLabel(),1,null);
        if(existing!=null)itemPhoto.replace(LOCAL_ITEM_PHOTOS.get(existing.id));
        GridPane form=new GridPane(); form.setHgap(10); form.setVgap(9); Node[] fields={name,itemType,price,capacity,total,available,stock,facilities,description,availability}; String[] captions={spec.nameLabel(),spec.typeLabel(),spec.priceLabel(),spec.capacityLabel(),spec.totalLabel(),spec.availableLabel(),spec.stockLabel(),spec.facilitiesLabel(),spec.descriptionLabel(),"Availability"}; for(int i=0;i<fields.length;i++){form.add(label(captions[i],"business-overview-key"),0,i);form.add(fields[i],1,i); GridPane.setHgrow(fields[i],Priority.ALWAYS);}
        VBox dialogContent = new VBox(10, form, itemPhoto);
        ScrollPane dialogScroll = new ScrollPane(dialogContent);
        dialogScroll.setFitToWidth(true);
        dialogScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dialogScroll.setPrefViewportHeight(430);
        dialogScroll.setMinViewportHeight(300);
        dialog.getDialogPane().setContent(dialogScroll);
        dialog.setResizable(true);
        AppUi.styleDialog(dialog, root==null||root.getScene()==null?null:root.getScene().getWindow(), "business-item-dialog", saveButtonType);
        dialog.setResultConverter(button->{ if(button.getButtonData()!=javafx.scene.control.ButtonBar.ButtonData.OK_DONE)return null; if(name.getText().trim().isEmpty()||!validNumbers(price.getText(),capacity.getText(),total.getText(),available.getText(),stock.getText())||number(available.getText())>number(total.getText())){ show("Validation","Enter a name, non-negative values, and available units not greater than total units."); return null;} return new ButtonTypeResult(name.getText().trim(),itemType.getText().trim(),price.getText().trim(),capacity.getText().trim(),total.getText().trim(),available.getText().trim(),stock.getText().trim(),facilities.getText().trim(),description.getText().trim(),availability.isSelected()); });
        dialog.showAndWait().ifPresent(result->{ Item item=existing==null?new Item(UUID.randomUUID().toString(),business.businessId,business.ownerId,type(),result.itemType(),result.name(),result.description(),result.price(),result.capacity(),result.total(),result.available(),result.stock(),result.facilities(),result.availableFlag()?"Available":"Unavailable",true):existing; ItemSnapshot snapshot=existing==null?null:ItemSnapshot.from(existing); if(existing!=null){item.name=result.name();item.itemType=result.itemType();item.description=result.description();item.price=result.price();item.capacity=result.capacity();item.totalUnits=result.total();item.availableUnits=result.available();item.stock=result.stock();item.facilities=result.facilities();item.availability=result.availableFlag()?"Available":"Unavailable";} itemPhoto.selectedFiles().stream().findFirst().ifPresent(file->{LOCAL_ITEM_PHOTOS.put(item.id,file);AppDataStore.rememberBusinessItemPhoto(item.id,file.toURI().toString());}); if(save(item)){upsertLocalItem(item); if(existing==null&&!items.stream().anyMatch(value->value.id.equals(item.id)))items.add(item);rememberPublicItem(item);showServices();}else if(snapshot!=null){snapshot.restore(existing);showServices();} });
    }
    private boolean save(Item item) { if(!owns(item))return false; AppSession.User user=AppSession.currentUser(); if(user!=null&&controller.isFirebaseEnabled())try{controller.saveInventoryItem(item.toGateway(),user.idToken());loadMessage="";return true;}catch(Exception e){LOGGER.log(Level.WARNING,"Business inventory item could not be saved online for business "+item.businessId,e);loadMessage="Saved in this session. Online sync is blocked by Firestore permissions.";show("Saved locally",loadMessage);return true;} return true; }
    private void loadLocalItems(){ if(business==null)return; for(Item item:LOCAL_ITEMS)if(item.businessId.equals(business.businessId)&&item.ownerId.equals(business.ownerId)){if(items.stream().noneMatch(value->value.id.equals(item.id)))items.add(item);if(item.active)rememberPublicItem(item);} }
    private void upsertLocalItem(Item item){ for(int i=0;i<LOCAL_ITEMS.size();i++){ if(LOCAL_ITEMS.get(i).id.equals(item.id)){LOCAL_ITEMS.set(i,item);return;} } LOCAL_ITEMS.add(item); }
    private void rememberPublicItem(Item item){if(item==null||business==null)return;String photo=AppDataStore.businessItemPhotoFor(item.id);File local=LOCAL_ITEM_PHOTOS.get(item.id);if((photo==null||photo.isBlank())&&local!=null&&local.isFile()){photo=local.toURI().toString();AppDataStore.rememberBusinessItemPhoto(item.id,photo);}PublicBusinessItem publicItem=new PublicBusinessItem(item.id,item.businessId,item.name,item.itemType,item.description,item.price,item.totalUnits,item.availableUnits,item.stock,item.facilities,item.availability,photo);if(item.active)AppDataStore.rememberBusinessItem(publicItem);else AppDataStore.removeBusinessItem(item.businessId,item.id);}
    private TextField field(String value,String prompt){TextField f=AppUi.textField(prompt);f.setText(value);return f;} private boolean validNumbers(String... values){for(String value:values)if(number(value)<0)return false;return true;} private long number(String v){try{return Long.parseLong(v.trim());}catch(Exception e){return -1;}} private void show(String title,String message){AppUi.showInfo(title,message,root==null||root.getScene()==null?null:root.getScene().getWindow());}
    private HBox booking(AppDataStore.BookingRecord b, boolean actions){VBox text=new VBox(2,label(text(b.customerName,"Customer"),"business-row-title"),label(text(b.title,"Service")+" • "+text(b.dateText,"Date not available")+" • Qty: "+b.quantity+" • ₹"+(b.amountPaise/100),"business-row-detail"),label("Payment: "+text(b.paymentStatus,"Unknown")+" • Status: "+text(b.bookingStatus,"Pending"),"business-row-detail")); HBox row=new HBox(10,AppUi.symbolIcon("\uE8A7","business-row-icon"),text,spacer(),statusBadge(text(b.bookingStatus,b.paymentStatus))); if(actions&&canManage()&&ownsBooking(b)){ if("PENDING".equalsIgnoreCase(b.bookingStatus)){Button accept=action("Accept",b,"CONFIRMED");Button reject=action("Reject",b,"REJECTED");row.getChildren().addAll(accept,reject);} else if("CONFIRMED".equalsIgnoreCase(b.bookingStatus)){row.getChildren().add(action("Mark Completed",b,"COMPLETED"));} } row.setAlignment(Pos.CENTER_LEFT);row.getStyleClass().add("business-data-row");return row;}
    private Button action(String title, AppDataStore.BookingRecord booking, String status){Button button=new Button(title);button.getStyleClass().add("business-text-action");button.setOnAction(e->{if(!canManage()||!ownsBooking(booking)||!allowed(booking.bookingStatus,status))return; AppDataStore.updateBookingStatus(booking.bookingId,status,booking.paymentStatus); showBookings();});return button;}
    private boolean ownsBooking(AppDataStore.BookingRecord booking){return business!=null&&business.businessId.equals(booking.businessId)&&business.ownerId.equals(currentOwnerId());}
    private boolean allowed(String from,String to){return ("PENDING".equalsIgnoreCase(from)&&("CONFIRMED".equals(to)||"REJECTED".equals(to)))||("CONFIRMED".equalsIgnoreCase(from)&&"COMPLETED".equals(to));}
    private String currentOwnerId(){AppSession.User user=AppSession.currentUser();return user!=null&&user.uid()!=null&&!user.uid().isBlank()?user.uid():fallbackAccount.email;}
    private List<AppDataStore.BookingRecord> bookings(){ if(business==null)return List.of(); AppSession.User user=AppSession.currentUser(); if(user!=null&&controller.isFirebaseEnabled())try{return controller.findBookingsForBusiness(business.businessId, user.idToken()).stream().filter(this::ownsBooking).toList();}catch(Exception ignored){} return AppDataStore.bookingsForBusiness(business.businessId);} private boolean pending(AppDataStore.BookingRecord b){return "PENDING".equalsIgnoreCase(b.bookingStatus)||"PENDING".equalsIgnoreCase(b.paymentStatus);}
    private javafx.scene.layout.StackPane notificationBell(){List<NotificationCenter.NotificationItem> items=businessNotifications();int unread=(int)items.stream().filter(n->!readNotifications.contains(n.id())).count();return NotificationCenter.bell(unread,this::showNotificationDrawer);}
    private void showNotificationDrawer(){NotificationCenter.show(root.getScene()==null?null:root.getScene().getWindow(),"Business Notifications","Bookings, payments and admin updates",businessNotifications(),readNotifications,this::openNotificationTarget);}
    private void openNotificationTarget(String target){setActive(target); if("bookings".equals(target))showBookings(); else if("payments".equals(target))showPayments(); else if("profile".equals(target))showProfile(); else showNotifications();}
    private List<NotificationCenter.NotificationItem> businessNotifications(){List<NotificationCenter.NotificationItem> out=new ArrayList<>(); for(AppDataStore.BookingRecord b:bookings()){String status=text(b.bookingStatus,"Updated");String payment=text(b.paymentStatus,"Unknown");String target=("PAID".equalsIgnoreCase(payment)||"VERIFIED".equalsIgnoreCase(payment))?"payments":"bookings";out.add(new NotificationCenter.NotificationItem("business-booking-"+b.bookingId,"Booking "+status,text(b.customerName,"Customer")+" | "+text(b.title,"Service")+" | Payment: "+payment,"Booking",severity(status,payment),target));} AppDataStore.items("announcement").stream().limit(4).forEach(n->out.add(new NotificationCenter.NotificationItem("business-admin-"+n.id,n.title,n.detail,"Admin","info","notifications"))); if(business!=null&&!(business.status.equalsIgnoreCase("APPROVED")||business.status.equalsIgnoreCase("ACTIVE")))out.add(new NotificationCenter.NotificationItem("business-status-"+business.businessId,"Registration "+displayStatus(),statusMessage(),"Account","pending","profile")); return out.stream().limit(12).toList();}
    private String severity(String status,String payment){if("FAILED".equalsIgnoreCase(payment)||"REJECTED".equalsIgnoreCase(status))return "failed"; if("PENDING".equalsIgnoreCase(payment)||"PENDING".equalsIgnoreCase(status)||status.toUpperCase(Locale.ROOT).contains("PENDING"))return "pending"; if("PAID".equalsIgnoreCase(payment)||"VERIFIED".equalsIgnoreCase(payment)||"CONFIRMED".equalsIgnoreCase(status)||"COMPLETED".equalsIgnoreCase(status))return "confirmed"; return "info";}
    private String type(){return category(business==null?fallbackAccount.category:business.category).name();} private Type category(String value){String c=text(value,"").toLowerCase(Locale.ROOT);if(c.contains("tent")||c.contains("camp"))return Type.TENT;if(c.contains("hotel"))return Type.HOTEL;if(c.contains("stay")||c.contains("lodge")||c.contains("dharamshala")||c.contains("accommodation"))return Type.STAY;if(c.contains("puja")||c.contains("pandit"))return Type.PUJA;if(c.contains("food")||c.contains("restaurant")||c.contains("snack"))return Type.FOOD;if(c.contains("shop")||c.contains("retail")||c.contains("store")||c.contains("pharmacy"))return Type.RETAIL;if(c.contains("travel")||c.contains("tour")||c.contains("guide")||c.contains("package"))return Type.TRAVEL;if(c.contains("parking"))return Type.PARKING;if(c.contains("toilet")||c.contains("sanitation"))return Type.TOILET;return Type.SERVICES;}
    private String inventoryLabel(){ Type value=category(business==null?fallbackAccount.category:business.category); if(value==Type.TENT)return "My Tents"; if(value==Type.HOTEL||value==Type.STAY)return "My Rooms / Stay"; if(value==Type.PUJA)return "My Puja Services"; if(value==Type.FOOD)return "My Menu"; if(value==Type.RETAIL)return "My Products"; if(value==Type.TRAVEL)return "My Packages"; if(value==Type.PARKING)return "My Parking Slots"; if(value==Type.TOILET)return "My Facilities"; return "My Services"; }
    private String managementTitle(){return "Manage "+inventoryLabel().replace("My ","");}
    private String addLabel(){return "Add "+itemNoun();}
    private String itemNoun(){ Type value=category(business.category); if(value==Type.TENT)return "Tent"; if(value==Type.HOTEL||value==Type.STAY)return "Room / Stay"; if(value==Type.PUJA)return "Puja Service"; if(value==Type.FOOD)return "Menu Item"; if(value==Type.RETAIL)return "Product"; if(value==Type.TRAVEL)return "Package"; if(value==Type.PARKING)return "Parking Slot"; if(value==Type.TOILET)return "Facility"; return "Service"; }
    private ItemFormSpec itemFormSpec(){Type value=category(business==null?fallbackAccount.category:business.category);return switch(value){case TENT -> new ItemFormSpec("Tent Name","Tent Type","Rate (₹ / night)","Person Capacity","Total Tents","Available Tents","Extra Stock","Facilities / Amenities","Tent Description","Available for bookings","Tent photo (optional)","Tent","Deluxe tent / family tent","AC, bedding, charging, water","Describe stay rules and inclusions");case HOTEL, STAY -> new ItemFormSpec("Room / Stay Name","Stay Type","Rate (₹ / night)","Guest Capacity","Total Rooms","Available Rooms","Extra Stock","Facilities / Amenities","Room / Stay Description","Available for bookings","Room / stay photo (optional)","Room / Stay","AC room / dormitory / lodge","AC, bedding, hot water, locker","Describe room facilities and check-in notes");case PUJA -> new ItemFormSpec("Puja Service Name","Service Type","Price (₹)","People Covered","Total Slots","Available Slots","Puja Items Stock","Puja Inclusions","Service Description","Available for bookings","Puja service photo (optional)","Puja Service","Abhishek / archana / pandit service","Samagri, prasad, language, duration","Describe timing, ritual details and inclusions");case FOOD -> new ItemFormSpec("Dish / Menu Item Name","Food Category","Price (₹)","Serving Capacity","Total Servings","Available Servings","Stock / Quantity","Ingredients / Options","Menu Description","Available for orders","Menu item photo (optional)","Menu Item","Thali / snacks / water","Veg, Jain, spicy, prasad option","Describe taste, serving size and availability");case RETAIL -> new ItemFormSpec("Product Name","Product Category","Price (₹)","Pack Size","Total Units","Available Units","Stock / Quantity","Product Options","Product Description","Available for sale","Product photo (optional)","Product","Essentials / clothes / medical","Sizes, colors, brands, variants","Describe product details and pilgrim use");case TRAVEL -> new ItemFormSpec("Package / Guide Service","Package Type","Price (₹)","Group Capacity","Total Slots","Available Slots","Vehicle / Guide Count","Route / Inclusions","Package Description","Available for bookings","Package photo (optional)","Package","Local guide / tour / travel package","Pickup, route, language, duration","Describe route, timing and support included");case PARKING -> new ItemFormSpec("Parking Area Name","Parking Type","Price (₹ / duration)","Vehicle Capacity","Total Parking Slots","Available Slots","Reserved Slots / Stock","Vehicle Options","Parking Description","Available for bookings","Parking area photo (optional)","Parking Slot","2-wheeler / 4-wheeler / bus parking","CCTV, security, covered, distance","Describe entry, timing and nearby landmark");case TOILET -> new ItemFormSpec("Facility Name","Facility Type","Price (₹)","User Capacity","Total Units","Available Units","Stock / Supplies","Facility Options","Facility Description","Available for use","Facility photo (optional)","Facility","Toilet / bath / sanitation","Women, men, accessible, water, cleaning","Describe hygiene, timing and access details");case SERVICES -> new ItemFormSpec("Service Name","Service Type","Price (₹)","Service Capacity","Total Units / Slots","Available Units / Slots","Stock / Quantity","Facilities / Options","Service Description","Available for bookings","Service photo (optional)","Service","Pilgrim service","Facilities, options, inclusions","Describe the service and important notes");};}
    private String managementHint(){return canManage()?"Manage only your business's operational items.":statusMessage();}
    private boolean canManage(){return business!=null&&(business.status.equalsIgnoreCase("APPROVED")||business.status.equalsIgnoreCase("ACTIVE"));}
    private boolean owns(Item item){return business!=null&&business.ownerId.equals(item.ownerId)&&business.businessId.equals(item.businessId);}
    private String displayStatus(){if(business==null)return "Unavailable";String s=text(business.status,"PENDING").toUpperCase(Locale.ROOT);return s.equals("APPROVED")?"Active":s.substring(0,1)+s.substring(1).toLowerCase(Locale.ROOT);}
    private String statusMessage(){if(business==null)return "No business is linked to this account."; String value=business.status.toUpperCase(Locale.ROOT); if(value.equals("APPROVED")||value.equals("ACTIVE"))return "Business Active"; if(value.equals("REJECTED"))return "Registration Rejected"; if(value.equals("SUSPENDED"))return "Business Temporarily Suspended"; return "Your business registration is under review.";}
    private String itemSummary(Item i){List<String> s=new ArrayList<>();s.add("₹"+i.price);if(!i.capacity.equals("0"))s.add("Capacity: "+i.capacity);if(!i.totalUnits.equals("0"))s.add("Available: "+i.availableUnits+" / "+i.totalUnits);if(!i.stock.equals("0"))s.add("Stock: "+i.stock);s.add(i.availability);return String.join(" • ",s);}
    private VBox pageShell(String title,String subtitle,Node...nodes){VBox box=new VBox(16,new VBox(3,label(title,"business-welcome"),label(subtitle,"business-header-date")));box.getChildren().addAll(nodes);box.getStyleClass().add("business-dashboard-content");box.setPadding(new Insets(22,28,32,28));return box;} private VBox panel(Node...nodes){VBox b=new VBox(13,nodes);b.getStyleClass().add("business-panel");return b;} private VBox stat(String title,String value,String detail){VBox c=new VBox(5,label(value,"business-stat-value"),label(title,"business-stat-title"),label(detail,"business-stat-detail"));c.getStyleClass().add("business-summary-card");HBox.setHgrow(c,Priority.ALWAYS);return c;} private HBox overviewRow(String k,String v){HBox r=new HBox(12,label(k,"business-overview-key"),spacer(),label(text(v,"No data yet"),"business-overview-value"));r.setAlignment(Pos.CENTER_LEFT);return r;} private HBox simple(String t,String d){HBox r=new HBox(10,AppUi.symbolIcon("\uE8A5","business-row-icon"),new VBox(2,label(t,"business-row-title"),label(d,"business-row-detail")));r.getStyleClass().add("business-data-row");return r;} private VBox empty(String t,String d){return new VBox(3,label(t,"business-empty-title"),label(d,"business-row-detail"));} private Label section(String t){return label(t,"business-section-title");} private Label statusBadge(String t){return label(t,"business-status-badge");} private Label label(String t,String style){Label l=new Label(t);l.getStyleClass().addAll(style.split(" "));l.setWrapText(true);return l;} private Region spacer(){Region r=new Region();HBox.setHgrow(r,Priority.ALWAYS);VBox.setVgrow(r,Priority.ALWAYS);return r;} private GridPane grid(double a,double b){GridPane g=new GridPane();g.setHgap(16);g.setVgap(16);var x=new javafx.scene.layout.ColumnConstraints();x.setPercentWidth(a);var y=new javafx.scene.layout.ColumnConstraints();y.setPercentWidth(b);g.getColumnConstraints().addAll(x,y);return g;} private void setActive(String key){navigation.forEach((k,b)->{b.getStyleClass().remove("business-nav-active");if(k.equals(key))b.getStyleClass().add("business-nav-active");});} private ScrollPane scroll(Node n){ScrollPane s=new ScrollPane(n);s.setFitToWidth(true);s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);s.getStyleClass().add("business-scroll");return s;} private String text(String v,String fallback){return v==null||v.isBlank()?fallback:v;}
    private enum Type { TENT, HOTEL, STAY, PUJA, FOOD, RETAIL, TRAVEL, PARKING, TOILET, SERVICES }
    private record ItemFormSpec(String nameLabel,String typeLabel,String priceLabel,String capacityLabel,String totalLabel,String availableLabel,String stockLabel,String facilitiesLabel,String descriptionLabel,String availabilityLabel,String photoLabel,String defaultType,String typePrompt,String facilitiesPrompt,String descriptionPrompt) {
        String namePrompt(){return nameLabel;}
        String pricePrompt(){return priceLabel.replace(" (₹ / night)","").replace(" (₹ / duration)","").replace(" (₹)","");}
        String capacityPrompt(){return capacityLabel;}
        String totalPrompt(){return totalLabel;}
        String availablePrompt(){return availableLabel;}
        String stockPrompt(){return stockLabel;}
    }
    private static final class Business {
        final String businessId, ownerId, name, ownerName, category, status, approved, description;
        final String location, address, area, city, latitude, longitude, locationUpdatedAt;
        final String mobile, email, operatingHours, priceRange;
        final String logoUrl, logoPublicId, coverPhotoUrl, coverPhotoPublicId;
        final List<CloudImage> galleryImages;

        Business(String businessId, String ownerId, String name, String ownerName, String category, String status,
                String approved, String description, String location, String address, String area, String city,
                String latitude, String longitude, String locationUpdatedAt, String mobile, String email,
                String operatingHours, String priceRange, String logoUrl, String logoPublicId, String coverPhotoUrl,
                String coverPhotoPublicId, List<CloudImage> galleryImages) {
            this.businessId = safe(businessId);
            this.ownerId = safe(ownerId);
            this.name = safe(name);
            this.ownerName = safe(ownerName);
            this.category = safe(category);
            this.status = status == null || status.isBlank() ? "PENDING" : status;
            this.approved = safe(approved);
            this.description = safe(description);
            this.location = safe(location);
            this.address = safe(address);
            this.area = safe(area);
            this.city = safe(city);
            this.latitude = safe(latitude);
            this.longitude = safe(longitude);
            this.locationUpdatedAt = safe(locationUpdatedAt);
            this.mobile = safe(mobile);
            this.email = safe(email);
            this.operatingHours = safe(operatingHours);
            this.priceRange = safe(priceRange);
            this.logoUrl = safe(logoUrl);
            this.logoPublicId = safe(logoPublicId);
            this.coverPhotoUrl = safe(coverPhotoUrl);
            this.coverPhotoPublicId = safe(coverPhotoPublicId);
            this.galleryImages = galleryImages == null ? List.of() : List.copyOf(galleryImages);
        }

        BusinessLocation location() {
            return new BusinessLocation(address, area, city, latitude, longitude, locationUpdatedAt);
        }

        Business withLocation(BusinessLocation value) {
            return new Business(businessId, ownerId, name, ownerName, category, status, approved, description,
                    value.displayText(), value.address(), value.area(), value.city(), value.latitude(),
                    value.longitude(), value.locationUpdatedAt(), mobile, email, operatingHours, priceRange,
                    logoUrl, logoPublicId, coverPhotoUrl, coverPhotoPublicId, galleryImages);
        }

        Business withProfile(BusinessProfileUpdate update) {
            return new Business(businessId, ownerId, update.businessName(), ownerName, category, status, approved,
                    update.description(), joinLocation(update.address(), update.area(), update.city()),
                    update.address(), update.area(), update.city(), latitude, longitude, locationUpdatedAt,
                    update.mobile(), update.email(), update.operatingHours(), update.priceRange(),
                    logoUrl, logoPublicId, coverPhotoUrl, coverPhotoPublicId, galleryImages);
        }

        Business withMedia(String kind, CloudinaryUploadResult upload) {
            if ("logo".equals(kind)) {
                return new Business(businessId, ownerId, name, ownerName, category, status, approved, description,
                        location, address, area, city, latitude, longitude, locationUpdatedAt, mobile, email,
                        operatingHours, priceRange, upload.getSecureUrl(), upload.getPublicId(), coverPhotoUrl,
                        coverPhotoPublicId, galleryImages);
            }
            if ("cover".equals(kind)) {
                return new Business(businessId, ownerId, name, ownerName, category, status, approved, description,
                        location, address, area, city, latitude, longitude, locationUpdatedAt, mobile, email,
                        operatingHours, priceRange, logoUrl, logoPublicId, upload.getSecureUrl(), upload.getPublicId(),
                        galleryImages);
            }
            List<CloudImage> updated = new ArrayList<>(galleryImages);
            updated.add(new CloudImage(upload.getSecureUrl(), upload.getPublicId()));
            return withGallery(updated);
        }

        Business withGallery(List<CloudImage> images) {
            return new Business(businessId, ownerId, name, ownerName, category, status, approved, description,
                    location, address, area, city, latitude, longitude, locationUpdatedAt, mobile, email,
                    operatingHours, priceRange, logoUrl, logoPublicId, coverPhotoUrl, coverPhotoPublicId, images);
        }

        boolean hasCoordinates() {
            return parse(latitude) != null && parse(longitude) != null;
        }

        String displayLocation() {
            String joined = joinLocation(address, area, city);
            return joined.isBlank() ? location : joined;
        }

        private static String joinLocation(String address, String area, String city) {
            List<String> parts = new ArrayList<>();
            add(parts, address);
            add(parts, area);
            add(parts, city);
            return String.join(", ", parts);
        }

        private static void add(List<String> parts, String value) {
            if (value != null && !value.isBlank()
                    && parts.stream().noneMatch(existing -> existing.equalsIgnoreCase(value.trim()))) {
                parts.add(value.trim());
            }
        }

        private static Double parse(String value) {
            try {
                if (value == null || value.isBlank()) return null;
                double parsed = Double.parseDouble(value.trim());
                return Double.isFinite(parsed) ? parsed : null;
            } catch (Exception exception) {
                return null;
            }
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
    private static final class Item { final String id,businessId,ownerId,category; String itemType,name,description,price,capacity,totalUnits,availableUnits,stock,facilities,availability; boolean active; Item(String id,String b,String o,String c,String type,String name,String d,String p,String cap,String total,String avail,String stock,String facilities,String availability,boolean active){this.id=id;businessId=b;ownerId=o;category=c;itemType=type;this.name=name;description=d;price=p;capacity=cap;totalUnits=total;availableUnits=avail;this.stock=stock;this.facilities=facilities;this.availability=availability;this.active=active;} static Item from(FirestoreGateway.BusinessInventoryItem i){return new Item(i.itemId(),i.businessId(),i.ownerId(),i.category(),i.itemType(),i.name(),i.description(),i.price(),i.capacity(),i.totalUnits(),i.availableUnits(),i.stock(),i.facilities(),i.availability(),i.active());} FirestoreGateway.BusinessInventoryItem toGateway(){return new FirestoreGateway.BusinessInventoryItem(id,businessId,ownerId,category,itemType,name,description,price,capacity,totalUnits,availableUnits,stock,facilities,availability,active);} }
    private record ItemSnapshot(String itemType,String name,String description,String price,String capacity,String totalUnits,String availableUnits,String stock,String facilities,String availability,boolean active){static ItemSnapshot from(Item item){return new ItemSnapshot(item.itemType,item.name,item.description,item.price,item.capacity,item.totalUnits,item.availableUnits,item.stock,item.facilities,item.availability,item.active);}void restore(Item item){item.itemType=itemType;item.name=name;item.description=description;item.price=price;item.capacity=capacity;item.totalUnits=totalUnits;item.availableUnits=availableUnits;item.stock=stock;item.facilities=facilities;item.availability=availability;item.active=active;}}
    private record ButtonTypeResult(String name,String itemType,String price,String capacity,String total,String available,String stock,String facilities,String description,boolean availableFlag) {}
}
