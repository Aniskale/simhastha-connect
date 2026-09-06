package com.simhastha.packages;

import java.util.List;
import static com.simhastha.packages.ItineraryItemType.*;

/** In-memory development catalogue for Part 1; replaceable by a future provider-backed repository. */
public final class PackageRepository {
    private static final FirestorePackageRepository ADMIN_SOURCE = new FirestorePackageRepository();
    private PackageRepository() { }
    public static final List<String> CITIES = List.of("Mumbai", "Pune", "Delhi", "Nagpur", "Ahmedabad", "Surat", "Indore", "Bhopal", "Jaipur", "Hyderabad", "Bengaluru", "Chennai", "Kolkata", "Lucknow", "Varanasi", "Patna", "Raipur", "Goa", "Kochi", "Chandigarh", "Amritsar", "Chhatrapati Sambhajinagar", "Nanded");
    /**
     * Published admin records are the production source. The seeded list below is deliberately
     * available only when Firebase is explicitly disabled.
     * A successful but empty Firestore catalogue remains empty and does not mix with demo data.
     */
    public static List<KumbhPackage> packages() {
        return packages("");
    }
    public static List<KumbhPackage> packages(String idToken) {
        try {
            List<KumbhPackage> published = ADMIN_SOURCE.getPublishedPackagesForUsers(idToken);
            return ADMIN_SOURCE.hasProductionSource() ? published : developmentPackages();
        } catch (java.io.IOException | InterruptedException packageReadFailure) {
            if (packageReadFailure instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to load the published Kumbh catalogue.", packageReadFailure);
        }
    }
    public static FirestorePackageRepository adminSource() { return ADMIN_SOURCE; }
    private static List<KumbhPackage> developmentPackages() { return List.of(
        p("delhi-premium", "Delhi to Nashik Premium Simhastha Experience", "Delhi", PackageCategory.PREMIUM, 5, 4, 39999, "Flight + Private Vehicle", "Premium Hotel", "Breakfast + Lunch + Dinner", "Premium Experience", "Ramkund Snan", "Ghat Darshan", "Temple Darshan", "Trimbakeshwar", "Panchavati", "Nashik Sightseeing", "Puja Assistance"),
        p("mumbai-premium", "Mumbai to Nashik Divine Premium Journey", "Mumbai", PackageCategory.PREMIUM, 4, 3, 28999, "Private AC Vehicle", "Premium Hotel", "Full Meals", "Simhastha Special", "Ramkund Snan", "Ghat Darshan", "Kumbh Assistance", "Panchavati", "Nashik Sightseeing", "Senior Citizen Assistance"),
        p("bengaluru-premium", "Bengaluru to Nashik Kumbh Grand Tour", "Bengaluru", PackageCategory.PREMIUM, 5, 4, 44999, "Flight + Private Vehicle", "Premium Hotel", "Full Meals", "Complete Kumbh", "Trimbakeshwar", "Panchavati", "Kalaram Temple", "Nashik Sightseeing", "Priority Assistance"),
        p("mumbai-standard", "Mumbai to Nashik Simhastha Essentials", "Mumbai", PackageCategory.STANDARD, 3, 2, 8999, "Train / Bus", "Hotel", "Breakfast + Dinner", "Family Pilgrimage", "Ramkund Snan", "Ghat Darshan", "Temple Darshan", "Panchavati"),
        p("pune-standard", "Pune to Nashik Spiritual Getaway", "Pune", PackageCategory.STANDARD, 3, 2, 7499, "Bus", "Hotel", "Breakfast", "Spiritual Journey", "Kumbh Assistance", "Ghat Darshan", "Kalaram Temple"),
        p("nagpur-standard", "Nagpur to Nashik Complete Kumbh", "Nagpur", PackageCategory.STANDARD, 4, 3, 12999, "Train", "Hotel", "Breakfast + Dinner", "Complete Kumbh", "Ramkund Snan", "Temple Darshan", "Trimbakeshwar", "Nashik Sightseeing"),
        p("ahmedabad-standard", "Ahmedabad to Nashik Darshan Tour", "Ahmedabad", PackageCategory.STANDARD, 4, 3, 11999, "Train / Bus", "Hotel", "Breakfast + Dinner", "Temple Darshan", "Ghat Darshan", "Panchavati", "Trimbakeshwar"),
        p("surat-standard", "Surat to Nashik Family Pilgrimage", "Surat", PackageCategory.STANDARD, 3, 2, 9999, "Bus", "Hotel", "Breakfast + Dinner", "Family Pilgrimage", "Ramkund Snan", "Kumbh Assistance", "Nashik Sightseeing"),
        p("mumbai-budget", "Mumbai to Nashik One Day Kumbh Darshan", "Mumbai", PackageCategory.BUDGET, 1, 0, 2999, "Bus", "No Stay", "No Meals", "Budget Pilgrimage", "Ramkund Snan", "Ghat Darshan"),
        p("pune-budget", "Pune to Nashik Budget Pilgrimage", "Pune", PackageCategory.BUDGET, 2, 1, 4599, "Bus", "Dharamshala", "Basic Meals", "Budget Pilgrimage", "Kumbh Assistance", "Temple Darshan", "Panchavati"),
        p("indore-budget", "Indore to Nashik Spiritual Saver", "Indore", PackageCategory.BUDGET, 3, 2, 7999, "Train", "Budget Hotel", "Breakfast", "Spiritual Journey", "Ramkund Snan", "Ghat Darshan", "Trimbakeshwar"),
        p("aurangabad-budget", "Chhatrapati Sambhajinagar to Nashik Yatra", "Chhatrapati Sambhajinagar", PackageCategory.BUDGET, 2, 1, 3999, "Self Travel", "Tent", "Basic Meals", "Budget Pilgrimage", "Kumbh Assistance", "Panchavati")
    ); }
    private static KumbhPackage p(String id, String title, String city, PackageCategory category, int days, int nights, int price, String travel, String stay, String meals, String theme, String... points) {
        List<String> facilities = List.of(points); List<String> sightseeing = facilities.stream().filter(x -> x.equals("Panchavati") || x.equals("Trimbakeshwar") || x.equals("Nashik Sightseeing") || x.equals("Kalaram Temple")).toList();
        String arrivalPoint = nashikArrivalPoint(travel);
        List<KumbhPackage.Day> plan = List.of(
            new KumbhPackage.Day("Day 1 — Arrival", List.of(new KumbhPackage.Item(TRAVEL, travel + " from " + city, null), new KumbhPackage.Item(PICKUP, "Simhastha Connect arrival assistance", arrivalPoint), new KumbhPackage.Item(STAY, stay + " check-in", null), new KumbhPackage.Item(MEAL, meals, null))),
            new KumbhPackage.Day("Day 2 — Kumbh Experience", List.of(new KumbhPackage.Item(SNAN, "Ramkund Snan", "Ramkund"), new KumbhPackage.Item(GHAT, "Godavari Ghat Darshan", "Ramkund"), new KumbhPackage.Item(TEMPLE, "Kalaram Temple visit", "Kalaram Temple"))),
            new KumbhPackage.Day("Day " + days + " — Departure", List.of(new KumbhPackage.Item(DROP, "Return departure assistance", arrivalPoint))) );
        List<String> travelOptions = List.of("Self Travel", PackageTravelConfig.defaults(city).encode());
        return new KumbhPackage(id, title, city, category, days, nights, price, 0, travel, stay, meals, List.of(theme), facilities, sightseeing, List.of("Configured travel", "Arrival pickup / departure drop", stay, meals, "Local Kumbh assistance"), List.of("Personal expenses", "Unselected meals", "Shopping", "Paid Puja unless listed"), plan, null, null, List.of(), travelOptions, List.of(stay), List.of(meals));
    }
    private static String nashikArrivalPoint(String travel) { String mode = travel.toLowerCase(java.util.Locale.ROOT); if (mode.contains("flight")) return "Ozar Airport"; if (mode.contains("train")) return "Nashik Road Railway Station"; return "CBS Nashik"; }
}
