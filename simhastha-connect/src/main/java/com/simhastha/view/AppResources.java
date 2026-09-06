package com.simhastha.view;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

final class AppResources {
    private AppResources() {
    }

    static URL url(Class<?> owner, String resourcePath) {
        URL classpath = owner.getResource(resourcePath);
        if (classpath != null) {
            return classpath;
        }

        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        for (Path base : resourceRoots()) {
            Path candidate = base.resolve(normalized);
            if (Files.exists(candidate)) {
                try {
                    return candidate.toUri().toURL();
                } catch (MalformedURLException ignored) {
                    // Try the next fallback path.
                }
            }
        }
        return null;
    }

    static String externalForm(Class<?> owner, String resourcePath) {
        URL url = url(owner, resourcePath);
        return url == null ? null : url.toExternalForm();
    }

    private static Path[] resourceRoots() {
        return new Path[] {
                Path.of("src", "main", "resources"),
                Path.of("simhastha-connect", "src", "main", "resources")
        };
    }
}
