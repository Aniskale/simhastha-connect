package com.simhastha.model;

/** Metadata returned by a future media provider; local file paths are never persisted here. */
public record MediaReference(String mediaId, String url, String publicId, String fileName,
        String type, String uploadedAt, boolean primary, int sortOrder) {
}
