package com.simhastha.packages;

/** Stable local-transport metadata retained alongside the active LOCAL_TRANSPORT PackageOption. */
public record LocalTransportSelection(String id, String name, String description, int upgradeCharge) { }
