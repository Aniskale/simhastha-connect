package com.simhastha.view;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** User-facing announcement UI; data and interaction state remain local to AnnouncementDemoStore. */
public final class UserAnnouncementView {
    private final Consumer<Node> show;
    private boolean dashboardBannerDismissed;
    public UserAnnouncementView(Consumer<Node> show) { this.show = show; }
    public static void openMainAnnouncements(Consumer<Node> navigator) {
        navigator.accept(new UserAnnouncementView(navigator).page());
    }
    public Node page() {
        VBox latest = new VBox(10);
        TextField search = AppUi.textField("Search announcements");
        ComboBox<String> category = combo("Category", List.of("All", "Government Notice", "Police / Security", "Traffic", "Transport", "Ghat / Crowd", "Medical / Emergency", "Event / Religious", "Weather", "Lost & Found", "General"), "All");
        ComboBox<String> priority = combo("Priority", List.of("All", "CRITICAL", "HIGH", "IMPORTANT", "NORMAL"), "All");
        Label unread = badge(AnnouncementDemoStore.unreadCount()+" unread");
        Runnable render = () -> { render(latest, search.getText(), category.getValue(), priority.getValue(), "Today", false); unread.setText(AnnouncementDemoStore.unreadCount()+" unread"); };
        search.textProperty().addListener((o,a,b)->render.run()); category.setOnAction(e->render.run()); priority.setOnAction(e->render.run()); render.run();
        HBox filters = new HBox(10, search, category, priority, unread); filters.getStyleClass().add("pilgrim-filter-row"); HBox.setHgrow(search, Priority.ALWAYS);
        HBox tabs = new HBox(8); tabs.getStyleClass().add("announcement-tabs");
        for(String name:List.of("Today", "Upcoming", "Expired")){ Button tab=secondary(name); if(name.equals("Today"))tab.getStyleClass().add("announcement-tab-active"); tab.setOnAction(e->{ tabs.getChildren().forEach(n->n.getStyleClass().remove("announcement-tab-active")); tab.getStyleClass().add("announcement-tab-active"); render(latest,search.getText(),category.getValue(),priority.getValue(),name,false); }); tabs.getChildren().add(tab); }
        return shell("Announcements & Live Updates", "Stay informed with official Simhastha updates.", filters, tabs, panel("Latest", latest));
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
    private Node card(AnnouncementDemoStore.Announcement a){Button details=primary("View Details");details.setOnAction(e->show.accept(details(a)));VBox card=new VBox(7,new HBox(8,badge(a.priority().name()),badge(a.category()),badge("Official"),AnnouncementDemoStore.isRead(a.id())?new Label():badge("NEW")),label(a.title()),detail(a.shortDescription()),detail("📍 "+destination(a)),detail("🕒 Updated "+time(a.lastUpdated())+"   ⏳ Valid until "+time(a.validUntil())),a.actionRequired()?badge("ACTION REQUIRED"):new Label(),details);card.getStyleClass().addAll("pilgrim-rich-card","ENDED".equals(a.status())?"announcement-expired":a.priority()==AnnouncementDemoStore.Priority.CRITICAL?"announcement-critical":"announcement-card");return card;}
    private static String destination(AnnouncementDemoStore.Announcement a){return a.locationChange()?a.newLocation():a.locationName();} private static String time(java.time.LocalDateTime t){return t.format(DateTimeFormatter.ofPattern("hh:mm a"));} private static String countdown(AnnouncementDemoStore.Announcement a){long m=Duration.between(java.time.LocalDateTime.now(),a.validUntil()).toMinutes();return m>0?"Ends in "+(m/60)+"h "+(m%60)+"m":"Expired";}
    private static VBox shell(String title,String subtitle,Node... nodes){VBox root=new VBox(10,label(title),detail(subtitle));root.getStyleClass().add("pilgrim-dashboard-main");root.setPadding(new Insets(12,22,28,22));root.getChildren().addAll(nodes);return root;} private static VBox panel(String title,Node child){VBox box=new VBox(10,label(title),child);box.getStyleClass().add("pilgrim-panel");return box;} private static ComboBox<String> combo(String prompt,List<String> vals,String value){ComboBox<String> c=new ComboBox<>();c.getItems().addAll(vals);c.setPromptText(prompt);c.setValue(value);c.getStyleClass().add("input-combo");return c;} private static Button primary(String text){Button b=new Button(text);b.getStyleClass().add("primary-button");return b;} private static Button secondary(String text){Button b=new Button(text);b.getStyleClass().add("pilgrim-small-action");return b;} private static Label label(String text){Label l=new Label(text);l.getStyleClass().add("pilgrim-card-title");l.setWrapText(true);return l;} private static Label detail(String text){Label l=new Label(text);l.getStyleClass().add("pilgrim-card-detail");l.setWrapText(true);return l;} private static Label badge(String text){Label l=new Label(text);l.getStyleClass().add("pilgrim-badge");return l;}
    static final class AnnouncementDetailsPage extends BorderPane { }
}
