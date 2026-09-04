package com.simhastha.model;

public record MapLocation(String id, String name, MapLocationType type, String address, String area,
        String city, double latitude, double longitude, String openTime, String closeTime,
        boolean verified, boolean crowded, String status, String phone, String description,
        String businessId, String category) {
}
