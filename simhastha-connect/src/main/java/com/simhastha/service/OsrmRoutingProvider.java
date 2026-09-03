package com.simhastha.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/** OSRM development provider. Geometry parsing is deliberately deferred until an approved JSON client is configured. */
public final class OsrmRoutingProvider implements RoutingProvider {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final String endpoint;
    public OsrmRoutingProvider() { this(System.getenv().getOrDefault("OSRM_BASE_URL", "https://router.project-osrm.org")); }
    public OsrmRoutingProvider(String endpoint) { this.endpoint = endpoint.replaceAll("/$", ""); }
    @Override public Optional<Route> route(Point source, Point destination, String mode) throws Exception {
        String profile = "driving".equalsIgnoreCase(mode) ? "driving" : "foot";
        URI uri = URI.create(endpoint + "/route/v1/" + profile + "/" + source.longitude() + "," + source.latitude()
                + ";" + destination.longitude() + "," + destination.latitude() + "?overview=false");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || !response.body().contains("\"code\":\"Ok\"")) return Optional.empty();
        double distance = number(response.body(), "\"distance\":");
        double duration = number(response.body(), "\"duration\":");
        return Optional.of(new Route(java.util.List.of(), distance, duration));
    }
    private static double number(String body, String key) {
        int start = body.indexOf(key); if (start < 0) return 0; start += key.length(); int end = start;
        while (end < body.length() && "0123456789.-".indexOf(body.charAt(end)) >= 0) end++;
        try { return Double.parseDouble(body.substring(start, end)); } catch (RuntimeException ignored) { return 0; }
    }
}
