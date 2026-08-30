package com.simhastha.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Local-development media storage for Admin-selected Ghat images.  The returned
 * URI points at an application-managed file, never the transient FileChooser
 * source.  A hosted media provider can replace this implementation later.
 */
public final class GhatImageStorageService {
    private final Path storageDirectory;
    private final Path legacyStorageDirectory;

    public GhatImageStorageService() {
        this(Path.of(System.getProperty("user.home"), ".simhastha-connect", "data", "ghat-images"));
    }

    GhatImageStorageService(Path storageDirectory) {
        this.storageDirectory = storageDirectory.toAbsolutePath().normalize();
        this.legacyStorageDirectory = Path.of(System.getProperty("user.home"), ".simhastha-connect", "ghat-images").toAbsolutePath().normalize();
    }

    public String store(Path selectedFile, String ghatId) throws IOException {
        if (selectedFile == null || !Files.isRegularFile(selectedFile) || !Files.isReadable(selectedFile)) {
            throw new IOException("The selected Ghat image file is unavailable.");
        }
        Files.createDirectories(storageDirectory);
        String safeId = ghatId == null ? "ghat" : ghatId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        String extension = extensionOf(selectedFile.getFileName().toString());
        Path destination = storageDirectory.resolve(safeId + "-" + UUID.randomUUID() + extension).normalize();
        if (!destination.startsWith(storageDirectory)) throw new IOException("Invalid Ghat image destination.");
        Files.copy(selectedFile, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toUri().toString();
    }

    public boolean isManagedReference(String source) {
        try {
            if (source == null || source.isBlank()) return false;
            Path path = Path.of(java.net.URI.create(source)).toAbsolutePath().normalize();
            return path.startsWith(storageDirectory) || path.startsWith(legacyStorageDirectory);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return ".jpg";
        String extension = filename.substring(dot).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case ".png", ".jpg", ".jpeg", ".webp" -> extension;
            default -> ".jpg";
        };
    }
}
