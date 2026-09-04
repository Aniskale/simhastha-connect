package com.simhastha.packages;

/** Cabin choices controlled by the package's saved Simhastha Connect travel configuration. */
public enum CabinClass {
    ECONOMY("Economy"), PREMIUM_ECONOMY("Premium Economy"), BUSINESS("Business"), FIRST_CLASS("First Class");

    private final String label;
    CabinClass(String label) { this.label = label; }
    public String label() { return label; }
}
