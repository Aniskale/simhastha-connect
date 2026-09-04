package com.simhastha.model;

public record BusinessProfileUpdate(String businessId, String ownerId, String businessName,
        String description, String mobile, String email, String address, String area,
        String city, String operatingHours, String priceRange) {
}
