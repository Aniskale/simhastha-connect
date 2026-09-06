package com.simhastha.packages;

/** Stable Kumbh experience data retained independently from its rendered card. */
public record KumbhExperienceSelection(String id, String name, String description, boolean included,
        int upgradeCharge) { }
