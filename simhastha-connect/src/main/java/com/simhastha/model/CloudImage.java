package com.simhastha.model;

public record CloudImage(String url, String publicId) {
    public CloudImage {
        url = url == null ? "" : url.trim();
        publicId = publicId == null ? "" : publicId.trim();
    }

    public boolean hasImage() {
        return !url.isBlank() && !publicId.isBlank();
    }

    public String serialize() {
        return escape(url) + "|" + escape(publicId);
    }

    public static CloudImage parse(String value) {
        if (value == null || value.isBlank()) {
            return new CloudImage("", "");
        }
        String[] parts = value.split("(?<!\\\\)\\|", 2);
        return new CloudImage(unescape(parts[0]), parts.length > 1 ? unescape(parts[1]) : "");
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("|", "\\|");
    }

    private static String unescape(String value) {
        return (value == null ? "" : value).replace("\\|", "|").replace("\\\\", "\\");
    }
}
