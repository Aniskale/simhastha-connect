package com.simhastha.packages;

/** Selected Stay metadata retained alongside the existing single STAY PackageOption. */
public record HotelStaySelection(String hotelId, String hotelName, String locality, String roomCategory,
        String rating, String facilities, String imageReference, int upgradeCharge) { }
