package com.simhastha.model;

public record UserProfile(String uid, String name, String email, String mobile, String role, String status,
        String profilePhotoUrl, String profilePhotoPublicId) {

    public UserProfile(String uid, String name, String email, String mobile, String role, String status) {
        this(uid, name, email, mobile, role, status, "", "");
    }
}
