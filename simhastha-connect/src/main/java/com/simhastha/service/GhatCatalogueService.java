package com.simhastha.service;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import java.util.List;

/** Development catalogue only. It is never persisted and backend records always take precedence. */
public final class GhatCatalogueService {
    public enum Region { ALL, NASHIK_GODAVARI, TRIMBAKESHWAR }
    public List<Ghat> catalogue() { return List.of(
        g("ramkund", "Ramkund", "Panchavati, Nashik", 20.0059, 73.7890, "/images/ramkund_sunrise.jpg", ramkundHistory()),
        g("ahilyabai-holkar", "Ahilyabai Holkar Ghat", "Panchavati, Nashik", 20.0063, 73.7895, localHistory("Ahilyabai Holkar Ghat", "Panchavati riverfront near Ramkund", "heritage access, calm movement and nearby temple visits")),
        g("kapila", "Kapila Ghat", "Panchavati, Nashik", 20.0048, 73.7881, localHistory("Kapila Ghat", "the Panchavati Godavari bank", "smaller-group Snan planning and access to nearby support points")), g("naroshankar", "Naroshankar Ghat", "Panchavati, Nashik", 20.0067, 73.7902, localHistory("Naroshankar Ghat", "the Naroshankar temple side of Panchavati", "darshan movement, riverfront access and crowd-managed Snan routes")),
        g("laxman-kund", "Laxman Kund Ghat", "Panchavati, Nashik", 20.0052, 73.7904, localHistory("Laxman Kund Ghat", "the Panchavati kund and temple circuit", "family pilgrim movement and short walking access")), g("ram-ghat", "Ram Ghat", "Panchavati, Nashik", 20.0056, 73.7886, localHistory("Ram Ghat", "Ramkund and the central Panchavati riverfront", "high-importance Snan access and darshan coordination")),
        g("tapovan", "Tapovan Ghat", "Tapovan, Nashik", 20.0132, 73.8080, "https://cdn.s3waas.gov.in/s3b3967a0e938dc2a6340e258630febd5a/uploads/2018/03/2018030979-300x225.jpg", tapovanHistory()), g("sita", "Sita Ghat", "Panchavati, Nashik", 20.0060, 73.7911, localHistory("Sita Ghat", "the Panchavati pilgrimage circuit", "temple-side darshan routes and managed bathing movement")),
        g("ganga-godavari", "Ganga / Godavari Ghat", "Panchavati, Nashik", 20.0061, 73.7891, gangaGodavari()), g("dasak", "Dasak Ghat", "Nashik", 19.9652, 73.8650, localHistory("Dasak Ghat", "the eastern Nashik Godavari stretch", "distributed crowd movement away from the central Panchavati zone")),
        g("goda-park", "Goda Park Ghat", "Nashik", 20.0079, 73.7786, localHistory("Goda Park Ghat", "the Goda Park riverfront area", "easy walking access, family waiting points and city-side support services")), g("sangam", "Sangam Ghat", "Nashik", 20.0120, 73.8035, localHistory("Sangam Ghat", "the river confluence side of Nashik", "Snan planning, movement dispersal and emergency-route coordination")),
        g("choudhary", "Choudhary Ghat", "Nashik", 20.0042, 73.7872, localHistory("Choudhary Ghat", "the central Nashik riverfront", "short-stay visits, support access and crowd-managed movement")), g("teerthraj", "Teerthraj Ghat", "Nashik", 20.0069, 73.7884, localHistory("Teerthraj Ghat", "the Panchavati sacred riverfront", "pilgrim assembly, darshan movement and Snan coordination")),
        g("kushavart", "Kushavart Tirtha — Trimbakeshwar", "Trimbakeshwar", 19.9325, 73.5309, "https://cdn.s3waas.gov.in/s3b3967a0e938dc2a6340e258630febd5a/uploads/2018/03/2018030990-300x225.jpg", kushavartHistory())); }
    public Region regionOf(Ghat ghat) { return ghat.name().contains("Trimbakeshwar") || ghat.id().equals("kushavart") ? Region.TRIMBAKESHWAR : Region.NASHIK_GODAVARI; }
    private Ghat g(String id,String name,String area,Double lat,Double lon,Ghat.History history) { return g(id,name,area,lat,lon,"",history); }
    private Ghat g(String id,String name,String area,Double lat,Double lon,String imageUrl,Ghat.History history) {
        return new Ghat(id,name,area,"Sacred riverfront information. Live operational updates unavailable.",lat,lon,null,null,imageUrl,
                Ghat.OperationalStatus.INFORMATION_ONLY,Ghat.CrowdLevel.UNKNOWN,null,false,
                Ghat.Walking.unknown(),List.of(),Ghat.Weather.unavailable(),history,"",
                GhatOperationalState.unavailable(),true,true);
    }
    private Ghat.History ramkundHistory() { return new Ghat.History("Located on the Godavari River in Nashik; built in 1696 by Chitrarao Khatav and later repaired by Gopikabai during the Peshwa period.","Nashik district describes Ramkund as one of Nashik's holiest locations; tradition associates Lord Rama bathing here during exile.","A significant Nashik Godavari context during Simhastha.","Asthivilaya Tirtha; Ganga Godavari Temple is adjacent.","Ash immersion is associated with Asthivilaya Tirtha.","Source: Nashik District, Government of Maharashtra.",""); }
    private Ghat.History gangaGodavari() { return new Ghat.History("The Ganga Godavari Temple is adjacent to Ramkund and was built in 1775 by Gopikabai Peshwe.","Temple information is separate from operational Ghat status.","The district describes a special Simhastha-period opening tradition.","Ganga Godavari Temple, adjacent to Ramkund.","Information pending verification.","Source: Nashik District, Government of Maharashtra.",""); }
    private Ghat.History tapovanHistory() { return new Ghat.History("Tapovan lies downstream of Panchavati on the Godavari River.","The Nashik District tourism page describes Tapovan's association with the Ramayana tradition.","The district notes that many sadhus camp at Tapovan during Simhastha.","Ram Parnakuti and Laxmana temples are noted nearby.","Information pending verification.","Source and image: Nashik District, Government of Maharashtra.",""); }
    private Ghat.History kushavartHistory() { return new Ghat.History("Kushavart Tirtha is in Trimbakeshwar town, distinct from Nashik city ghats.","The district describes it as a sacred kund associated with the Godavari's re-emergence.","Its Trimbakeshwar bathing context is separate from Nashik city arrangements.","Near Trimbakeshwar Jyotirlinga Temple.","Information pending verification.","Source and image: Nashik District, Government of Maharashtra.",""); }
    private Ghat.History localHistory(String name, String setting, String use) { return new Ghat.History(
            name + " is part of " + setting + " used by pilgrims for darshan, movement and Snan planning during Simhastha.",
            "The Ghat supports " + use + " around the sacred Godavari riverfront.",
            "During Simhastha, its crowd level, access routes and bathing status are managed through official operational updates.",
            "Nearby temples, river steps, help points and Panchavati routes help pilgrims move through the wider Nashik pilgrimage circuit.",
            "Pilgrims should follow the live status, gate directions and water-safety guidance shown for this Ghat before entering the Snan area.",
            "Live data from the administration updates this page when available.",
            ""); }
}
