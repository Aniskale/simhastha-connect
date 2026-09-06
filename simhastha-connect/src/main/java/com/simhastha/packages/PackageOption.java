package com.simhastha.packages;

/** A selectable catalogue component; ids remain stable in a Part 3 booking snapshot. */
public record PackageOption(String id, PackageOptionType type, String label, String description, int price,
        boolean included, boolean mandatory, boolean quantityAllowed) { }
