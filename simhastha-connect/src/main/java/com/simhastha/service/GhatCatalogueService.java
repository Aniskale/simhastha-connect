package com.simhastha.service;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import java.util.List;

/** Development catalogue only. It is never persisted and backend records always take precedence. */
public final class GhatCatalogueService {
    public enum Region { ALL, NASHIK_GODAVARI, TRIMBAKESHWAR }
    public List<Ghat> catalogue() { return List.of(
        g("ramkund", "Ramkund", "Panchavati, Nashik", 20.0059, 73.7890, "/images/ramkund_sunrise.jpg", ramkundHistory()),
        g("ahilyabai-holkar", "Ahilyabai Holkar Ghat", "Panchavati, Nashik", null, null, pending()),
        g("kapila", "Kapila Ghat", "Panchavati, Nashik", null, null, pending()), g("naroshankar", "Naroshankar Ghat", "Panchavati, Nashik", null, null, pending()),
        g("laxman-kund", "Laxman Kund Ghat", "Panchavati, Nashik", null, null, pending()), g("ram-ghat", "Ram Ghat", "Panchavati, Nashik", null, null, pending()),
        g("tapovan", "Tapovan Ghat", "Tapovan, Nashik", 20.0132, 73.8080, "https://cdn.s3waas.gov.in/s3b3967a0e938dc2a6340e258630febd5a/uploads/2018/03/2018030979-300x225.jpg", tapovanHistory()), g("sita", "Sita Ghat", "Panchavati, Nashik", null, null, pending()),
        g("ganga-godavari", "Ganga / Godavari Ghat", "Panchavati, Nashik", null, null, gangaGodavari()), g("dasak", "Dasak Ghat", "Nashik", null, null, pending()),
        g("goda-park", "Goda Park Ghat", "Nashik", null, null, pending()), g("sangam", "Sangam Ghat", "Nashik", null, null, pending()),
        g("choudhary", "Choudhary Ghat", "Nashik", null, null, pending()), g("teerthraj", "Teerthraj Ghat", "Nashik", null, null, pending()),
        g("kushavart", "Kushavart Tirtha — Trimbakeshwar", "Trimbakeshwar", 19.9325, 73.5309, "https://cdn.s3waas.gov.in/s3b3967a0e938dc2a6340e258630febd5a/uploads/2018/03/2018030990-300x225.jpg", kushavartHistory())); }
    public Region regionOf(Ghat ghat) { return ghat.name().contains("Trimbakeshwar") || ghat.id().equals("kushavart") ? Region.TRIMBAKESHWAR : Region.NASHIK_GODAVARI; }
    private Ghat g(String id,String name,String area,Double lat,Double lon,Ghat.History history) { return g(id,name,area,lat,lon,"",history); }
    private Ghat g(String id,String name,String area,Double lat,Double lon,String imageUrl,Ghat.History history) { return new Ghat(id,name,area,"Catalogue information",lat,lon,null,null,imageUrl,Ghat.OperationalStatus.INFORMATION_ONLY,Ghat.CrowdLevel.UNKNOWN,null,false,new Ghat.Walking(Ghat.WalkingDifficulty.MODERATE,null,null,false,false),List.of(),Ghat.Weather.unavailable(),history,"",GhatOperationalState.unavailable()); }
    private Ghat.History ramkundHistory() { return new Ghat.History("Located on the Godavari River in Nashik; built in 1696 by Chitrarao Khatav and later repaired by Gopikabai during the Peshwa period.","Nashik district describes Ramkund as one of Nashik's holiest locations; tradition associates Lord Rama bathing here during exile.","A significant Nashik Godavari context during Simhastha.","Asthivilaya Tirtha; Ganga Godavari Temple is adjacent.","Ash immersion is associated with Asthivilaya Tirtha.","Source: Nashik District, Government of Maharashtra.",""); }
    private Ghat.History gangaGodavari() { return new Ghat.History("The Ganga Godavari Temple is adjacent to Ramkund and was built in 1775 by Gopikabai Peshwe.","Temple information is separate from operational Ghat status.","The district describes a special Simhastha-period opening tradition.","Ganga Godavari Temple, adjacent to Ramkund.","Information pending verification.","Source: Nashik District, Government of Maharashtra.",""); }
    private Ghat.History tapovanHistory() { return new Ghat.History("Tapovan lies downstream of Panchavati on the Godavari River.","The Nashik District tourism page describes Tapovan's association with the Ramayana tradition.","The district notes that many sadhus camp at Tapovan during Simhastha.","Ram Parnakuti and Laxmana temples are noted nearby.","Information pending verification.","Source and image: Nashik District, Government of Maharashtra.",""); }
    private Ghat.History kushavartHistory() { return new Ghat.History("Kushavart Tirtha is in Trimbakeshwar town, distinct from Nashik city ghats.","The district describes it as a sacred kund associated with the Godavari's re-emergence.","Its Trimbakeshwar bathing context is separate from Nashik city arrangements.","Near Trimbakeshwar Jyotirlinga Temple.","Information pending verification.","Source and image: Nashik District, Government of Maharashtra.",""); }
    private Ghat.History pending() { return new Ghat.History("Detailed verified history will be added by the Simhastha administration.","Information pending verification.","Information pending verification.","Information pending verification.","Information pending verification.","Catalogue information pending final administration dataset.",""); }
}
