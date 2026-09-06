package com.simhastha.packages;

import java.util.*;

/** Persisted with the package's travel options so Admin and pilgrims use the same configuration. */
public record PackageTravelConfig(boolean selfTravelEnabled, boolean simhasthaConnectEnabled, boolean flightEnabled,
        String defaultOriginAirport, String destinationAirport, Set<CabinClass> allowedCabins,
        Map<CabinClass, Integer> cabinCharges, String preferredAirlines, boolean flightAssistanceIncluded,
        boolean airportPickupIncluded, String baggageNote, String travelInstructions) {
    public static final String PREFIX = "SC_TRAVEL_CONFIG|";

    public static PackageTravelConfig fromTravelOptions(List<String> options, String packageOrigin) {
        return options == null ? defaults(packageOrigin) : options.stream().filter(v -> v != null && v.startsWith(PREFIX))
                .findFirst().map(PackageTravelConfig::decode).orElseGet(() -> defaults(packageOrigin));
    }
    public static PackageTravelConfig defaults(String origin) {
        Map<CabinClass,Integer> charges = new EnumMap<>(CabinClass.class);
        charges.put(CabinClass.ECONOMY, 4000); charges.put(CabinClass.PREMIUM_ECONOMY, 6500);
        charges.put(CabinClass.BUSINESS, 12000); charges.put(CabinClass.FIRST_CLASS, 20000);
        return new PackageTravelConfig(true, true, true, airportFor(origin), "ISK", EnumSet.allOf(CabinClass.class), charges,
                "", true, true, "Baggage allowance is confirmed by the operating airline.", "Flight reservation/request remains pending provider confirmation.");
    }
    public int charge(CabinClass cabin) { return Math.max(0, cabinCharges.getOrDefault(cabin, 0)); }
    public String encode() {
        StringBuilder text = new StringBuilder(PREFIX).append("self=").append(selfTravelEnabled).append("|connect=").append(simhasthaConnectEnabled)
                .append("|flight=").append(flightEnabled).append("|origin=").append(clean(defaultOriginAirport)).append("|destination=").append(clean(destinationAirport))
                .append("|cabins=").append(allowedCabins.stream().map(Enum::name).sorted().reduce((a,b)->a+","+b).orElse(""));
        for (CabinClass cabin : CabinClass.values()) text.append("|").append(cabin.name()).append("=").append(charge(cabin));
        return text.append("|airlines=").append(clean(preferredAirlines)).append("|assistance=").append(flightAssistanceIncluded)
                .append("|pickup=").append(airportPickupIncluded).append("|baggage=").append(clean(baggageNote)).append("|instructions=").append(clean(travelInstructions)).toString();
    }
    private static PackageTravelConfig decode(String encoded) {
        Map<String,String> values = new HashMap<>();
        for (String part : encoded.split("\\|")) { int at = part.indexOf('='); if (at > 0) values.put(part.substring(0, at), part.substring(at + 1)); }
        PackageTravelConfig base = defaults(values.getOrDefault("origin", ""));
        Set<CabinClass> cabins = EnumSet.noneOf(CabinClass.class);
        for (String value : values.getOrDefault("cabins", "").split(",")) try { cabins.add(CabinClass.valueOf(value)); } catch (Exception ignored) { }
        if (cabins.isEmpty()) cabins = EnumSet.copyOf(base.allowedCabins());
        Map<CabinClass,Integer> charges = new EnumMap<>(CabinClass.class);
        for (CabinClass cabin : CabinClass.values()) charges.put(cabin, number(values.get(cabin.name()), base.charge(cabin)));
        return new PackageTravelConfig(bool(values,"self",base.selfTravelEnabled()), bool(values,"connect",base.simhasthaConnectEnabled()), bool(values,"flight",base.flightEnabled()),
                values.getOrDefault("origin",base.defaultOriginAirport()), values.getOrDefault("destination",base.destinationAirport()), cabins, charges,
                values.getOrDefault("airlines",""), bool(values,"assistance",base.flightAssistanceIncluded()), bool(values,"pickup",base.airportPickupIncluded()),
                values.getOrDefault("baggage",base.baggageNote()), values.getOrDefault("instructions",base.travelInstructions()));
    }
    private static boolean bool(Map<String,String> values,String key,boolean fallback) { return values.containsKey(key) ? Boolean.parseBoolean(values.get(key)) : fallback; }
    private static int number(String text,int fallback) { try { return Integer.parseInt(text); } catch (Exception ignored) { return fallback; } }
    private static String clean(String text) { return text == null ? "" : text.replace("|", "/").replace("\n", " "); }
    private static String airportFor(String origin) { String city = origin == null ? "" : origin.toLowerCase(Locale.ROOT); if (city.contains("sambhajinagar") || city.contains("aurangabad")) return "IXU"; if (city.contains("mumbai")) return "BOM"; if (city.contains("pune")) return "PNQ"; if (city.contains("delhi")) return "DEL"; if (city.contains("bengaluru")) return "BLR"; return "IXU"; }
}
