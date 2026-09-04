package com.simhastha.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.simhastha.config.CloudinaryConfig;
import com.simhastha.model.CloudinaryUploadResult;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudinaryService {

    private static final Logger LOGGER = Logger.getLogger(CloudinaryService.class.getName());
    private final Cloudinary cloudinary;

    public CloudinaryService() {
        this.cloudinary = CloudinaryConfig.getCloudinary();
    }

    public CloudinaryUploadResult uploadImage(File file, String folder)
            throws IOException {

        validateImage(file);

        LOGGER.info("Cloudinary image upload started: folder=" + folder
                + ", fileName=" + file.getName()
                + ", sizeBytes=" + file.length());

        Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(
                    file,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", UUID.randomUUID().toString(),
                            "resource_type", "image"
                    )
            );
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Cloudinary image upload failed: folder=" + folder
                    + ", fileName=" + file.getName(), exception);
            throw exception;
        }

        Object secureUrlValue = result.get("secure_url");
        Object publicIdValue = result.get("public_id");
        String secureUrl = secureUrlValue == null ? "" : String.valueOf(secureUrlValue);
        String publicId = publicIdValue == null ? "" : String.valueOf(publicIdValue);
        LOGGER.info("Cloudinary image upload success: folder=" + folder
                + ", secureUrlPresent=" + (secureUrl != null && secureUrl.startsWith("https://"))
                + ", publicIdPresent=" + (publicId != null && !publicId.isBlank()));

        return new CloudinaryUploadResult(secureUrl, publicId);
    }

    public boolean deleteImage(String publicId) throws IOException {

        if (publicId == null || publicId.isBlank()) {
            return false;
        }

        Map<?, ?> result = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
        );

        return "ok".equalsIgnoreCase(
                String.valueOf(result.get("result"))
        );
    }

    public static void validateImage(File file) {

        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid image file.");
        }

        long maxSize = 10L * 1024L * 1024L;

        if (file.length() > maxSize) {
            throw new IllegalArgumentException(
                    "Image size must be below 10 MB."
            );
        }

        String name = file.getName().toLowerCase();

        if (!(name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".webp"))) {

            throw new IllegalArgumentException(
                    "Only JPG, JPEG, PNG and WEBP images are supported."
            );
        }
    }
}
