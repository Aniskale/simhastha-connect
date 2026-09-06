package com.simhastha.packages;

import java.util.*;

/** Translates present line-based Admin catalogue fields into stable, user-selectable options. */
public final class PackageCustomizationOptions {
    private PackageCustomizationOptions() { }
    public static List<PackageOption> options(KumbhPackage p, PackageOptionType type) {
        List<String> source = switch (type) { case TRAVEL -> p.travelChoices().isEmpty() ? List.of(p.travel()) : p.travelChoices().stream().filter(value -> !value.startsWith(PackageTravelConfig.PREFIX)).toList(); case STAY -> p.stayChoices().isEmpty() ? List.of(p.stay()) : p.stayChoices(); case MEAL -> p.mealChoices().isEmpty() ? List.of(p.meals()) : p.mealChoices(); case KUMBH -> p.facilities(); case SIGHTSEEING -> p.sightseeing(); default -> List.of(); };
        List<PackageOption> parsed = new ArrayList<>(); int index = 0; for (String line : source) { if (line == null || line.isBlank()) continue; parsed.add(parse(p, type, line, index++)); }
        if (!parsed.isEmpty()) return parsed;
        return defaults(p, type);
    }
    public static List<PackageOption> defaults(KumbhPackage p, PackageOptionType type) {
        return switch (type) {
            case TRAVEL_CLASS -> travelClasses(p);
            case ROOM -> rooms(p);
            case LOCAL_TRANSPORT -> List.of(option(p,type,"shared-shuttle","Shared Shuttle | Included",0,true,false,false), option(p,type,"private-sedan","Private Sedan | Private local transport",3000,false,false,false), option(p,type,"suv","SUV | Family local transport",5000,false,false,false));
            case PUJA_ADDON -> p.facilities().stream().anyMatch(value -> value.toLowerCase().contains("puja")) ? List.of(option(p,type,"puja-assistance","Puja Assistance | Package add-on",800,false,false,false), option(p,type,"priority-darshan","Priority Darshan Assistance",1200,false,false,false), option(p,type,"extra-meal","Additional Meal",350,false,false,true)) : List.of();
            default -> List.of();
        };
    }
    private static List<PackageOption> travelClasses(KumbhPackage p) { String t=p.travel().toLowerCase(); if(t.contains("train")) return List.of(option(p,PackageOptionType.TRAVEL_CLASS,"sleeper","Sleeper",0,true,false,false),option(p,PackageOptionType.TRAVEL_CLASS,"3a","3A | Air-conditioned berth",1200,false,false,false),option(p,PackageOptionType.TRAVEL_CLASS,"2a","2A | Premium berth",2200,false,false,false)); if(t.contains("flight")) return List.of(option(p,PackageOptionType.TRAVEL_CLASS,"economy","Economy",0,true,false,false),option(p,PackageOptionType.TRAVEL_CLASS,"premium-economy","Premium Economy",3500,false,false,false),option(p,PackageOptionType.TRAVEL_CLASS,"business","Business",9500,false,false,false)); if(t.contains("bus")) return List.of(option(p,PackageOptionType.TRAVEL_CLASS,"standard-bus","Shared Simhastha Bus",0,true,false,false),option(p,PackageOptionType.TRAVEL_CLASS,"ac-sleeper","AC Sleeper",900,false,false,false)); return List.of(); }
    private static List<PackageOption> rooms(KumbhPackage p) { if (p.stay().equalsIgnoreCase("No Stay")) return List.of(); return List.of(option(p,PackageOptionType.ROOM,"double","Double Room | Standard occupancy",0,true,false,false),option(p,PackageOptionType.ROOM,"deluxe","Deluxe Room",1500,false,false,false),option(p,PackageOptionType.ROOM,"suite","Suite",4500,false,false,false)); }
    private static PackageOption parse(KumbhPackage p, PackageOptionType type, String source, int index) { String[] fields=source.split("\\|"); String label=fields[0].trim(); String lower=source.toLowerCase(); int price=price(source); boolean included=lower.contains("included") || price==0; boolean mandatory=type==PackageOptionType.KUMBH && included; return option(p,type,label.toLowerCase().replaceAll("[^a-z0-9]+","-"),source,price,included,mandatory,type==PackageOptionType.PUJA_ADDON); }
    private static int price(String value) { java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(?:₹|rs\\.?|inr)\\s*([0-9,]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(value); if(!matcher.find()) return 0; try{return Integer.parseInt(matcher.group(1).replace(",",""));}catch(Exception e){return 0;} }
    private static PackageOption option(KumbhPackage p, PackageOptionType type, String suffix, String label, int price, boolean included, boolean mandatory, boolean quantity) { return new PackageOption(p.id()+":"+type+":"+suffix,type,label,label,price,included,mandatory,quantity); }
}
