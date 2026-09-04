package com.simhastha.view;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageUploadService {

    private static final long MAX_IMAGE_BYTES = Long.getLong("cloudinary.maxImageBytes", 5L * 1024L * 1024L);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final ImageUploadService INSTANCE = new ImageUploadService();

    private final HttpClient client = HttpClient.newBuilder().build();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "simhastha-image-upload");
        thread.setDaemon(true);
        return thread;
    });

    private ImageUploadService() {
    }

    public static ImageUploadService get() {
        return INSTANCE;
    }

    public CompletableFuture<ImageUploadResult> uploadImage(Path file, String folder) {
        return CompletableFuture.supplyAsync(() -> uploadImageBlocking(file, folder), executor);
    }

    public CompletableFuture<ImageUploadResult> replaceImage(Path file, String folder, String oldPublicId) {
        return uploadImage(file, folder).whenComplete((result, error) -> {
            if (error == null && oldPublicId != null && !oldPublicId.isBlank()
                    && result != null && !oldPublicId.equals(result.publicId())) {
                deleteImage(oldPublicId);
            }
        });
    }

    public CompletableFuture<Void> deleteImage(String publicId) {
        return CompletableFuture.runAsync(() -> {
            if (publicId == null || publicId.isBlank()) {
                return;
            }
            CloudinaryConfig config = CloudinaryConfig.load();
            if (!config.enabled()) {
                throw new ImageUploadException("Cloudinary is not configured.");
            }
            long timestamp = Instant.now().getEpochSecond();
            String signature = sha1("public_id=" + publicId + "&timestamp=" + timestamp + config.apiSecret());
            String body = "public_id=" + enc(publicId)
                    + "&timestamp=" + timestamp
                    + "&api_key=" + enc(config.apiKey())
                    + "&signature=" + enc(signature);
            HttpRequest request = HttpRequest.newBuilder(config.destroyUri())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            send(request, "Cloudinary image delete failed.");
        }, executor);
    }

    public void validateImage(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            throw new ImageUploadException("Please select a valid image file.");
        }
        String name = file.getFileName().toString();
        String extension = extension(name);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ImageUploadException("Only JPG, JPEG, PNG and WEBP images are supported.");
        }
        try {
            long size = Files.size(file);
            if (size <= 0 || size > MAX_IMAGE_BYTES) {
                throw new ImageUploadException("Image size must be between 1 byte and "
                        + (MAX_IMAGE_BYTES / (1024 * 1024)) + " MB.");
            }
        } catch (IOException exception) {
            throw new ImageUploadException("Image file could not be read.", exception);
        }
    }

    private ImageUploadResult uploadImageBlocking(Path file, String folder) {
        validateImage(file);
        CloudinaryConfig config = CloudinaryConfig.load();
        if (!config.enabled()) {
            throw new ImageUploadException("Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET.");
        }
        try {
            String boundary = "----SimhasthaCloudinary" + UUID.randomUUID();
            byte[] image = Files.readAllBytes(file);
            long timestamp = Instant.now().getEpochSecond();
            String safeFolder = "simhastha-connect/" + cleanFolder(folder);
            String signature = sha1("folder=" + safeFolder + "&timestamp=" + timestamp + config.apiSecret());
            byte[] body = multipart(boundary, file.getFileName().toString(), image, safeFolder, timestamp,
                    config.apiKey(), signature);
            HttpRequest request = HttpRequest.newBuilder(config.uploadUri())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            String response = send(request, "Cloudinary image upload failed.");
            String secureUrl = jsonValue(response, "secure_url");
            String publicId = jsonValue(response, "public_id");
            if (secureUrl.isBlank() || publicId.isBlank()) {
                throw new ImageUploadException("Cloudinary response did not include image URL/public ID.");
            }
            return new ImageUploadResult(secureUrl, publicId, extension(file.getFileName().toString()),
                    file.getFileName().toString());
        } catch (IOException exception) {
            throw new ImageUploadException("Image upload failed.", exception);
        }
    }

    private String send(HttpRequest request, String message) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ImageUploadException(message + " HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new ImageUploadException(message, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ImageUploadException(message, exception);
        }
    }

    private static byte[] multipart(String boundary, String fileName, byte[] image, String folder, long timestamp,
            String apiKey, String signature) throws IOException {
        String prefix = part(boundary, "folder", folder)
                + part(boundary, "timestamp", String.valueOf(timestamp))
                + part(boundary, "api_key", apiKey)
                + part(boundary, "signature", signature)
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName.replace("\"", "") + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[prefixBytes.length + image.length + suffixBytes.length];
        System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        System.arraycopy(image, 0, body, prefixBytes.length, image.length);
        System.arraycopy(suffixBytes, 0, body, prefixBytes.length + image.length, suffixBytes.length);
        return body;
    }

    private static String part(String boundary, String name, String value) {
        return "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
    }

    private static String cleanFolder(String folder) {
        String clean = folder == null ? "" : folder.trim().replaceAll("[^a-zA-Z0-9/_-]", "-");
        return clean.isBlank() ? "general" : clean;
    }

    private static String extension(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private static String sha1(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ImageUploadException("Cloudinary signature could not be created.", exception);
        }
    }

    private static String jsonValue(String json, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1).replace("\\/", "/") : "";
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private record CloudinaryConfig(String cloudName, String apiKey, String apiSecret) {
        private static CloudinaryConfig load() {
            Properties properties = new Properties();
            try (java.io.InputStream input = ImageUploadService.class.getResourceAsStream("/cloudinary.properties")) {
                if (input != null) {
                    properties.load(input);
                }
            } catch (IOException ignored) {
                // Cloudinary configuration is optional until image upload is used.
            }
            return new CloudinaryConfig(
                    firstNonBlank(System.getProperty("cloudinary.cloudName"), System.getenv("CLOUDINARY_CLOUD_NAME"),
                            properties.getProperty("cloudinary.cloudName")),
                    firstNonBlank(System.getProperty("cloudinary.apiKey"), System.getenv("CLOUDINARY_API_KEY"),
                            properties.getProperty("cloudinary.apiKey")),
                    firstNonBlank(System.getProperty("cloudinary.apiSecret"), System.getenv("CLOUDINARY_API_SECRET"),
                            properties.getProperty("cloudinary.apiSecret")));
        }

        private boolean enabled() {
            return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
        }

        private URI uploadUri() {
            return URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");
        }

        private URI destroyUri() {
            return URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy");
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record ImageUploadResult(String secureUrl, String publicId, String format, String originalFileName) {
    }

    public static final class ImageUploadException extends RuntimeException {
        private ImageUploadException(String message) {
            super(message);
        }

        private ImageUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
