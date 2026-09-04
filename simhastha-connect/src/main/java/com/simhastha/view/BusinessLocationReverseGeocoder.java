package com.simhastha.view;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight asynchronous Nominatim reverse geocoder for the business location selector. */
final class BusinessLocationReverseGeocoder {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final Pattern JSON_VALUE = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"((?:\\\\\\.|[^\\\"])*)\\\"");

    private BusinessLocationReverseGeocoder() {
    }

    static CompletableFuture<ResolvedAddress> reverse(double latitude, double longitude) {
        String query = "format=jsonv2&addressdetails=1&zoom=18&lat="
                + URLEncoder.encode(String.format(java.util.Locale.US, "%.6f", latitude), StandardCharsets.UTF_8)
                + "&lon=" + URLEncoder.encode(String.format(java.util.Locale.US, "%.6f", longitude), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://nominatim.openstreetmap.org/reverse?" + query))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "SimhasthaConnect/1.0 BusinessLocationSelector")
                .header("Accept-Language", "en")
                .GET().build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("Reverse geocoder returned HTTP " + response.statusCode());
                    }
                    return parse(response.body());
                });
    }

    private static ResolvedAddress parse(String json) {
        String road = value(json, "road");
        String houseNumber = value(json, "house_number");
        String address = join(" ", houseNumber, road);
        if (address.isBlank()) address = first(json, "amenity", "building", "name", "neighbourhood", "display_name");
        String area = first(json, "suburb", "neighbourhood", "quarter", "locality", "village", "hamlet",
                "city_district", "state_district", "county");
        String city = first(json, "city", "town", "municipality", "village");
        if (address.isBlank() && area.isBlank() && city.isBlank()) {
            throw new IllegalStateException("Reverse geocoder returned no usable address details.");
        }
        return new ResolvedAddress(address, area, city);
    }

    private static String first(String json, String... keys) {
        for (String key : keys) {
            String value = value(json, key);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static String join(String separator, String... values) {
        return String.join(separator, List.of(values).stream().filter(value -> value != null && !value.isBlank()).toList());
    }

    private static String value(String json, String key) {
        Matcher matcher = JSON_VALUE.matcher(json == null ? "" : json);
        while (matcher.find()) {
            if (key.equals(matcher.group(1))) return unescape(matcher.group(2)).trim();
        }
        return "";
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    record ResolvedAddress(String address, String area, String city) {
    }
}
