package com.simhastha.packages;

import java.io.File;
import java.net.URI;
import java.util.Optional;

/** Stable media boundary; replace this provider with Cloudinary later without changing package UI/models. */
public interface PackageMediaService {
    Optional<String> validateReference(String reference);
    String resolveReference(PackageMedia media);
    default boolean isUsable(PackageMedia media) { return media != null && validateReference(media.url()).isEmpty(); }
    static PackageMediaService temporary() { return TemporaryPackageMediaService.INSTANCE; }
}

final class TemporaryPackageMediaService implements PackageMediaService {
    static final TemporaryPackageMediaService INSTANCE = new TemporaryPackageMediaService();
    private TemporaryPackageMediaService() { }
    @Override public Optional<String> validateReference(String reference) {
        if (reference == null || reference.isBlank()) return Optional.of("Choose an image reference.");
        String lower = reference.toLowerCase(); if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) return Optional.of("Use a JPG, JPEG or PNG image.");
        try { if (!lower.startsWith("http://") && !lower.startsWith("https://") && !new File(reference).isFile()) return Optional.of("The selected local image is no longer available."); } catch (Exception ignored) { return Optional.of("Invalid image reference."); }
        return Optional.empty();
    }
    @Override public String resolveReference(PackageMedia media) { if (media == null || media.url() == null) return ""; String value = media.url(); return value.startsWith("http://") || value.startsWith("https://") || value.startsWith("file:") ? value : new File(value).toURI().toString(); }
}
