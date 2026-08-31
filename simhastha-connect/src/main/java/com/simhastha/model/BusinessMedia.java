package com.simhastha.model;

public record BusinessMedia(String mediaId, String businessId, String ownerId, String url,
        String publicId, String type, boolean cover, String createdAt, boolean active) {

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }
}
