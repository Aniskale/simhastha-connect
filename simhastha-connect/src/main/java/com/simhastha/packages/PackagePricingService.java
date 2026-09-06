package com.simhastha.packages;

import java.util.*;
/** Central calculation for Part 2; UI only asks this service for the current breakdown. */
public final class PackagePricingService {
    public Pricing calculate(KumbhPackage pkg, PackageCustomization state) {
        Map<String,Integer> lines = new LinkedHashMap<>(); lines.put("Base Package", Math.max(0, pkg.price()));
        for (PackageOptionType type : List.of(PackageOptionType.TRAVEL, PackageOptionType.TRAVEL_CLASS, PackageOptionType.STAY, PackageOptionType.ROOM, PackageOptionType.MEAL, PackageOptionType.LOCAL_TRANSPORT)) add(lines, state.selected(type), state);
        for (PackageOption option : state.optionalSelections()) add(lines, option, state);
        int total = lines.values().stream().mapToInt(Integer::intValue).sum(); return new Pricing(Collections.unmodifiableMap(lines), Math.max(0, total));
    }
    private void add(Map<String,Integer> lines, PackageOption option, PackageCustomization state) { if (option != null && !option.included() && option.price() > 0) lines.merge(option.label(), option.price() * state.quantity(option), Integer::sum); }
    public record Pricing(Map<String, Integer> components, int total) { }
}
