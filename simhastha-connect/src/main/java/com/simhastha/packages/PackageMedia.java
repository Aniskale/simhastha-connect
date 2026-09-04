package com.simhastha.packages;

/** Metadata only. url may be a temporary local reference today and a Cloudinary URL later. */
public record PackageMedia(String mediaId, String url, String publicId, String caption, PackageMediaType mediaType,
        int sortOrder, boolean primary, String createdAt) { }
