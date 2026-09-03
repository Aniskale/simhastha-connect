package com.simhastha.view;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** User-facing announcement UI; data and interaction state remain local to AnnouncementDemoStore. */
public final class UserAnnouncementView {
    private final Consumer<Node> show;
    private boolean dashboardBannerDismissed;
    public UserAnnouncementView(Consumer<Node> show) { this.show = show; }
    public static void openMainAnnouncements(Consumer<Node> navigator) {
        navigator.accept(new UserAnnouncementView(navigator).page());
    }
    public Node page() {
        VBox latest = new VBox(8);
        latest.getStyleClass().add("announcement-list");
        TextField search = AppUi.textField("Search announcements...");
        ComboBox<String> category = combo("Category", List.of("All", "Government Notice", "Police / Security", "Traffic", "Transport", "Ghat / Crowd", "Medical / Emergency", "Event / Religious", "Weather", "Lost & Found", "General"), "All");
        ComboBox<String> priority = combo("Priority", List.of("All", "CRITICAL", "HIGH", "IMPORTANT", "NORMAL"), "All");
        Label unread = badge(AnnouncementDemoStore.unreadCount()+" unread");
        unread.getStyleClass().add("announcement-unread-pill");
        Runnable render = () -> { render(latest, search.getText(), category.getValue(), priority.getValue(), "Today", false); unread.setText(AnnouncementDemoStore.unreadCount()+" unread"); };
        search.textProperty().addListener((o,a,b)->render.run()); category.setOnAction(e->render.run()); priority.setOnAction(e->render.run()); render.run();
        HBox filters = new HBox(12, search, category, priority, unread);
        filters.getStyleClass().addAll("pilgrim-filter-row", "announcement-filter-bar");
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox tabs = new HBox(8); tabs.getStyleClass().add("announcement-tabs");
        for(String name:List.of("Today", "Upcoming", "Expired")){ Button tab=secondary(name); if(name.equals("Today"))tab.getStyleClass().add("announcement-tab-active"); tab.setOnAction(e->{ tabs.getChildren().forEach(n->n.getStyleClass().remove("announcement-tab-active")); tab.getStyleClass().add("announcement-tab-active"); render(latest,search.getText(),category.getValue(),priority.getValue(),name,false); }); tabs.getChildren().add(tab); }
        return shell("Announcements & Live Updates", "Stay informed with official Simhastha updates.", filters, tabs, summaryStrip(), section("Latest Announcements", latest));
    }
    public Node details(AnnouncementDemoStore.Announcement a) {
        AnnouncementDemoStore.markRead(a.id());
        Button back=secondary("← Back to Announcements"); back.setOnAction(e->openMainAnnouncements(show));
        VBox body=new VBox(10, headerCard(a), mapPreview(a));
        if(a.locationChange()) body.getChildren().add(locationChanged(a));
        if(a.actionRequired()) body.getChildren().add(panel("What Should You Do?", new VBox(6, detail("• "+a.actionInstruction()), detail("• Follow police barricade directions"), detail("• Avoid the affected area until the update ends"))));
        if(!a.alternativeLocation().isBlank()) { Button alternate=secondary("Navigate to Alternative"); alternate.setOnAction(e->AnnouncementMapView.navigateTo(a)); body.getChildren().add(panel("Alternative Option", new VBox(6,detail("Original: "+destination(a)),detail("Alternative: "+a.alternativeLocation()),detail(a.alternativeRoute()),alternate))); }
        body.getChildren().add(panel("Announcement Timeline", new VBox(5, detail("Published — "+time(a.publishedAt())), detail("Updated — "+time(a.lastUpdated())), a.locationChange()?detail("Location Changed — "+time(a.lastUpdated())):detail("Live guidance issued"))));
        VBox related=new VBox(7); AnnouncementDemoStore.all().stream().filter(other->!other.id().equals(a.id()) && ("LIVE".equals(other.status()) || "NEW".equals(other.status()) || "UPDATED".equals(other.status()))).limit(3).forEach(other->{ Button b=secondary(other.title());b.setOnAction(e->show.accept(details(other)));related.getChildren().add(b); }); body.getChildren().add(panel("Related Announcements",related));
        VBox content = new VBox(10, label("Announcement Details"), detail("Official Verified • " + a.department()), body);
        content.getStyleClass().add("pilgrim-dashboard-main");
        content.setPadding(new Insets(12, 22, 28, 22));
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("pilgrim-dashboard-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox header = new HBox(back);
        header.setPadding(new Insets(12, 22, 6, 22));
        AnnouncementDetailsPage layout = new AnnouncementDetailsPage();
        layout.setTop(header);
        layout.setCenter(scroll);
        return layout;
    }
    public Node dashboardAlerts() {
        VBox alerts=new VBox(10); AnnouncementDemoStore.all().stream().filter(AnnouncementDemoStore.Announcement::showDashboardAlert).forEach(a->alerts.getChildren().add(dashboardCard(a))); return alerts;
    }
    private Node dashboardCard(AnnouncementDemoStore.Announcement a) {
        if(a.priority()==AnnouncementDemoStore.Priority.CRITICAL && dashboardBannerDismissed) return new VBox();
        Button details=secondary("View Details");details.setOnAction(e->show.accept(details(a))); Button map=secondary("View on Map");map.setDisable(!a.hasLocation());map.setOnAction(e->show.accept(new AnnouncementMapView(show,a).page())); Button navigate=secondary("Navigate");navigate.setDisable(!a.hasLocation());navigate.setOnAction(e->AnnouncementMapView.navigateTo(a));
        VBox card=new VBox(7,badge(a.priority().name()),label((a.priority()==AnnouncementDemoStore.Priority.CRITICAL?"🚨 ":"")+a.title()),detail(a.shortDescription()),detail("New Location: "+destination(a)),detail("Official Verified • Updated "+time(a.lastUpdated())+" • Valid until "+time(a.validUntil())),new HBox(8,details,map,navigate));
        if(a.priority()==AnnouncementDemoStore.Priority.CRITICAL){ Button dismiss=secondary("Dismiss");dismiss.setOnAction(e->{dashboardBannerDismissed=true;show.accept(dashboardAlerts());});card.getChildren().add(dismiss); }
        card.getStyleClass().addAll("pilgrim-rich-card",a.priority()==AnnouncementDemoStore.Priority.CRITICAL?"announcement-critical":"announcement-high");return card;
    }
    private Node headerCard(AnnouncementDemoStore.Announcement a) { VBox card=new VBox(8,badge(a.priority().name()),label((a.priority()==AnnouncementDemoStore.Priority.CRITICAL?"🚨 ":"")+a.title()),detail(a.shortDescription()),detail(a.fullDescription()),detail("Official Verified • "+a.department()),detail("Affected area: "+a.affectedArea()),detail("Published "+time(a.publishedAt())+" • Last updated "+time(a.lastUpdated())),detail("Valid until: "+time(a.validUntil())+" • "+countdown(a)), a.actionRequired()?badge("ACTION REQUIRED — "+a.actionInstruction()):new Label());card.getStyleClass().addAll("pilgrim-rich-card",a.priority()==AnnouncementDemoStore.Priority.CRITICAL?"announcement-critical":"announcement-card");return card; }
    private Node locationChanged(AnnouncementDemoStore.Announcement a){return panel("Location Changed",new VBox(6,detail("Old Location: "+a.previousLocation()),label("↓"),label("New Location: "+a.newLocation()),detail("Reason: "+a.changeReason()),detail("Affected zone: "+a.zone()+" • Effective now")));}
    private Node mapPreview(AnnouncementDemoStore.Announcement a){ if(!a.hasLocation())return panel("Location",detail("Location unavailable for this update.")); Button open=primary("View on Map");open.setOnAction(e->show.accept(new AnnouncementMapView(show,a).page())); Button navigate=secondary("Navigate");navigate.setOnAction(e->AnnouncementMapView.navigateTo(a)); VBox preview=new VBox(7,label("📍 "+destination(a)),detail(a.zone()+" • "+a.sector()+" • "+a.ghat()),detail("Nashik • destination marker preview"),new HBox(8,open,navigate));preview.getStyleClass().add("announcement-map-preview");return panel("Map Preview",preview); }
    private void render(VBox box,String q,String c,String p,String tab,boolean near){ box.getChildren().clear(); for(AnnouncementDemoStore.Announcement a:AnnouncementDemoStore.all())if(matches(a,q,c,p,tab,near))box.getChildren().add(card(a)); if(box.getChildren().isEmpty())box.getChildren().add(panel("No announcements",detail("No local demo announcements match this view."))); }
    private boolean matches(AnnouncementDemoStore.Announcement a,String q,String c,String p,String tab,boolean near){boolean publicStatus="LIVE".equals(a.status())||"NEW".equals(a.status())||"UPDATED".equals(a.status());boolean expired="ENDED".equals(a.status())||a.validUntil().isBefore(java.time.LocalDateTime.now()); boolean tabMatch="Expired".equals(tab)?expired:"Upcoming".equals(tab)?publicStatus&&a.publishedAt().isAfter(java.time.LocalDateTime.now()):publicStatus&&!expired;return tabMatch&&(q==null||q.isBlank()||(a.title()+a.shortDescription()).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))&&(c==null||"All".equals(c)||a.category().equals(c))&&(p==null||"All".equals(p)||a.priority().name().equals(p))&&(!near||a.zone().contains("Godavari")||a.sector().contains("B")||a.affectedArea().contains("Panchavati"));}
    private void renderNear(VBox box){box.getChildren().clear();AnnouncementDemoStore.all().stream().filter(a->a.hasLocation()&&!"ENDED".equals(a.status())).limit(3).forEach(a->box.getChildren().add(card(a)));}
    private Node card(AnnouncementDemoStore.Announcement a){
        Button details=primary("View Details");
        details.setOnAction(e->show.accept(details(a)));
        VBox copy = new VBox(6, label(a.title()), detail(a.shortDescription()), detail("Location: "+destination(a)), detail("Updated: "+time(a.lastUpdated())+"     Valid until: "+time(a.validUntil())));
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label category = badge(a.category());
        category.getStyleClass().add("announcement-category-pill");
        VBox action = new VBox(11, category, ageText(a), details);
        action.setAlignment(Pos.CENTER_RIGHT);
        HBox card=new HBox(18, priorityMark(a), copy, action);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("pilgrim-rich-card","announcement-row","ENDED".equals(a.status())?"announcement-expired":a.priority()==AnnouncementDemoStore.Priority.CRITICAL?"announcement-critical":a.priority()==AnnouncementDemoStore.Priority.NORMAL?"announcement-card":"announcement-high");
        return card;
    }
    private static Node priorityMark(AnnouncementDemoStore.Announcement a) {
        StackPane icon = new StackPane(AppUi.symbolIcon(priorityGlyph(a), "announcement-priority-icon"));
        icon.getStyleClass().addAll("announcement-priority-logo", priorityTone(a));
        VBox mark = new VBox(7, icon, badge(a.priority().name()));
        mark.getStyleClass().add("announcement-priority-mark");
        mark.setAlignment(Pos.CENTER);
        return mark;
    }
    private static Label ageText(AnnouncementDemoStore.Announcement a) {
        Label label = detail(relativeAge(a));
        label.getStyleClass().add("announcement-age");
        Circle dot = new Circle(4);
        dot.getStyleClass().add(switch (a.priority()) {
            case CRITICAL -> "announcement-dot-critical";
            case HIGH, IMPORTANT -> "announcement-dot-warning";
            case NORMAL -> "announcement-dot-normal";
        });
        label.setGraphic(dot);
        label.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        label.setGraphicTextGap(8);
        return label;
    }
    private static HBox summaryStrip() {
        long total = AnnouncementDemoStore.all().stream().filter(a -> !"ENDED".equals(a.status())).count();
        long high = AnnouncementDemoStore.all().stream().filter(a -> a.priority() == AnnouncementDemoStore.Priority.HIGH || a.priority() == AnnouncementDemoStore.Priority.CRITICAL).count();
        long important = AnnouncementDemoStore.all().stream().filter(a -> a.priority() == AnnouncementDemoStore.Priority.IMPORTANT).count();
        HBox strip = new HBox(12,
                metric("Total Announcements", Long.toString(total), "All active updates", "\uE80F", "announcement-metric-saffron"),
                metric("High Priority", Long.toString(high), "Requires attention", "\uE716", "announcement-metric-green"),
                metric("Important Updates", Long.toString(important), "General information", "\uE789", "announcement-metric-gold"),
                metric("Unread", Long.toString(AnnouncementDemoStore.unreadCount()), "Needs review", "\uE7B3", "announcement-metric-violet"));
        strip.getStyleClass().add("announcement-summary-strip");
        strip.getChildren().forEach(child -> HBox.setHgrow(child, Priority.ALWAYS));
        return strip;
    }
    private static HBox metric(String title, String value, String caption, String glyph, String tone) {
        StackPane icon = new StackPane(AppUi.symbolIcon(glyph, "announcement-metric-icon"));
        icon.getStyleClass().addAll("announcement-metric-logo", tone);
        VBox copy = new VBox(2, label(value), detail(title), detail(caption));
        HBox box = new HBox(13, icon, copy);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("announcement-metric");
        return box;
    }
    private static String destination(AnnouncementDemoStore.Announcement a){return a.locationChange()?a.newLocation():a.locationName();} private static String time(java.time.LocalDateTime t){return t.format(DateTimeFormatter.ofPattern("hh:mm a"));} private static String countdown(AnnouncementDemoStore.Announcement a){long m=Duration.between(java.time.LocalDateTime.now(),a.validUntil()).toMinutes();return m>0?"Ends in "+(m/60)+"h "+(m%60)+"m":"Expired";}
    private static String relativeAge(AnnouncementDemoStore.Announcement a){long m=Math.max(1,Duration.between(a.lastUpdated(),java.time.LocalDateTime.now()).toMinutes());return m<60?m+" min ago":(m/60)+" hr ago";}
    private static String priorityGlyph(AnnouncementDemoStore.Announcement a){return switch(a.priority()){case CRITICAL -> "\uE7BA"; case HIGH -> "\uE7BA"; case IMPORTANT -> "\uE789"; case NORMAL -> "\uE783";};}
    private static String priorityTone(AnnouncementDemoStore.Announcement a){return switch(a.priority()){case CRITICAL -> "announcement-priority-critical"; case HIGH -> "announcement-priority-high"; case IMPORTANT -> "announcement-priority-important"; case NORMAL -> "announcement-priority-normal";};}
    private static VBox shell(String title,String subtitle,Node... nodes){VBox titleCopy=new VBox(3,label(title),detail(subtitle));HBox header=new HBox(10,AppUi.symbolIcon("\uE789","announcement-title-icon"),titleCopy);header.setAlignment(Pos.CENTER_LEFT);VBox root=new VBox(14,header);root.getStyleClass().addAll("pilgrim-dashboard-main","announcement-page");root.setPadding(new Insets(16,22,28,22));root.getChildren().addAll(nodes);return root;} private static VBox panel(String title,Node child){VBox box=new VBox(10,label(title),child);box.getStyleClass().add("pilgrim-panel");return box;} private static VBox section(String title,Node child){VBox box=new VBox(9,label(title),child);box.getStyleClass().add("announcement-section");return box;} private static ComboBox<String> combo(String prompt,List<String> vals,String value){ComboBox<String> c=new ComboBox<>();c.getItems().addAll(vals);c.setPromptText(prompt);c.setValue(value);c.getStyleClass().add("input-combo");return c;} private static Button primary(String text){Button b=new Button(text);b.getStyleClass().add("primary-button");return b;} private static Button secondary(String text){Button b=new Button(text);b.getStyleClass().add("pilgrim-small-action");return b;} private static Label label(String text){Label l=new Label(text);l.getStyleClass().add("pilgrim-card-title");l.setWrapText(true);return l;} private static Label detail(String text){Label l=new Label(text);l.getStyleClass().add("pilgrim-card-detail");l.setWrapText(true);return l;} private static Label badge(String text){Label l=new Label(text);l.getStyleClass().add("pilgrim-badge");return l;}
    static final class AnnouncementDetailsPage extends BorderPane { }
}
