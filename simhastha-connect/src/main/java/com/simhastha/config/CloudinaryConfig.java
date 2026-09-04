package com.simhastha.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class CloudinaryConfig {

    private static Cloudinary cloudinary;

    private CloudinaryConfig() {
    }

    public static Cloudinary getCloudinary() {
        if (cloudinary == null) {
            Properties properties = new Properties();

            try (InputStream input = CloudinaryConfig.class
                    .getClassLoader()
                    .getResourceAsStream("cloudinary.properties")) {

                if (input == null) {
                    throw new IllegalStateException(
                            "cloudinary.properties file not found."
                    );
                }

                properties.load(input);

            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to load Cloudinary configuration.",
                        e
                );
            }

            String cloudName = properties.getProperty("cloudinary.cloudName");
            String apiKey = properties.getProperty("cloudinary.apiKey");
            String apiSecret = properties.getProperty("cloudinary.apiSecret");

            if (cloudName == null || cloudName.isBlank()
                    || apiKey == null || apiKey.isBlank()
                    || apiSecret == null || apiSecret.isBlank()) {

                throw new IllegalStateException(
                        "Cloudinary credentials are missing."
                );
            }

            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName.trim(),
                    "api_key", apiKey.trim(),
                    "api_secret", apiSecret.trim(),
                    "secure", true
            ));
        }

        return cloudinary;
    }
}