package com.simhastha.packages;

/** Stable package-customisation add-on data; this is not a Puja Services booking. */
public record PackageAddonSelection(String id, String name, String description, String category,
        int upgradeCharge) { }
