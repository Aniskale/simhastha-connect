package com.simhastha.model;

public class CloudinaryUploadResult {

    private final String secureUrl;
    private final String publicId;

    public CloudinaryUploadResult(String secureUrl, String publicId) {
        this.secureUrl = secureUrl;
        this.publicId = publicId;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public String getPublicId() {
        return publicId;
    }
}