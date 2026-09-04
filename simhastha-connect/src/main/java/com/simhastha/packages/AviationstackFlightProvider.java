package com.simhastha.packages;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/** Aviationstack schedule/status adapter. It deliberately never represents a package charge as an airline fare. */
public final class AviationstackFlightProvider implements FlightProvider {
    private static final URI BASE = URI.create("https://api.aviationstack.com/v1/flights");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    @Override public List<FlightOption> search(FlightSearchRequest request) throws FlightProviderException {
        String key = Optional.ofNullable(System.getenv("AVIATIONSTACK_API_KEY")).filter(v -> !v.isBlank()).orElse(System.getProperty("aviationstack.api.key", ""));
        if (key.isBlank()) throw new FlightProviderException("Flight schedules are not configured. Set AVIATIONSTACK_API_KEY to enable live Aviationstack results.");
        try {
            // Free-plan-safe live/status lookup: the selected departure date remains a UI preference,
            // but is not sent as a future-schedule filter to the real-time Flights endpoint.
            String query = "?access_key=" + encode(key) + "&dep_iata=" + encode(request.originAirport()) + "&arr_iata=" + encode(request.destinationAirport()) + "&limit=25";
            String endpoint = BASE + query;
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString());
            AviationstackError error = error(response.body());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                logDiagnostic(response.statusCode(), response.body(), error, redact(endpoint, key), key);
                throw new FlightProviderException(userMessage(response.statusCode(), error));
            }
            if (error != null) {
                logDiagnostic(response.statusCode(), response.body(), error, redact(endpoint, key), key);
                throw new FlightProviderException(userMessage(response.statusCode(), error));
            }
            String requestedDeparture = iata(request.originAirport());
            String requestedArrival = iata(request.destinationAirport());
            return parse(response.body()).stream()
                    .filter(flight -> requestedDeparture.equals(iata(flight.departure().airportCode()))
                            && requestedArrival.equals(iata(flight.arrival().airportCode())))
                    .toList();
        } catch (FlightProviderException exception) { throw exception; }
        catch (java.net.http.HttpTimeoutException exception) { throw new FlightProviderException("Flight search timed out. Please try again.", exception); }
        catch (Exception exception) { throw new FlightProviderException("Unable to load live flight schedules.", exception); }
    }
    private List<FlightOption> parse(String json) {
        List<FlightOption> results = new ArrayList<>();
        for (String entry : dataObjects(json)) {
            String airline = string(nested(entry, "airline"), "name"); String number = string(nested(entry, "flight"), "iata");
            String dep = nested(entry, "departure"); String arr = nested(entry, "arrival");
            if (airline.isBlank() || number.isBlank() || dep.isBlank() || arr.isBlank()) continue;
            FlightSegment departure = new FlightSegment(string(dep,"iata"), string(dep,"airport"), string(dep,"scheduled"), string(dep,"terminal"), string(dep,"gate"));
            FlightSegment arrival = new FlightSegment(string(arr,"iata"), string(arr,"airport"), string(arr,"scheduled"), string(arr,"terminal"), string(arr,"gate"));
            results.add(new FlightOption(airline,number,departure,arrival,"Schedule supplied by Aviationstack",status(string(entry,"flight_status")), string(nested(entry,"aircraft"),"registration")));
        }
        return List.copyOf(results);
    }
    private static List<String> dataObjects(String json) {
        int data = json.indexOf("\"data\""); int start = data < 0 ? -1 : json.indexOf('[', data); if (start < 0) return List.of();
        List<String> objects = new ArrayList<>(); int depth = 0; int objectStart = -1; boolean quoted = false;
        for (int i = start + 1; i < json.length(); i++) { char c = json.charAt(i); if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) quoted = !quoted; if (quoted) continue; if (c == '{') { if (depth++ == 0) objectStart = i; } else if (c == '}' && --depth == 0 && objectStart >= 0) objects.add(json.substring(objectStart, i + 1)); else if (c == ']' && depth == 0) break; }
        return objects;
    }
    private static String nested(String source,String key) { Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*\\{(.*?)\\}",Pattern.DOTALL).matcher(source);return m.find()?m.group(1):""; }
    private static String string(String source,String... keys) { String found=source; for(String key:keys){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|null)",Pattern.DOTALL).matcher(found);if(!m.find()) return "";found=m.group(1)==null?"":m.group(1);} return found; }
    private static AviationstackError error(String json) { if (json == null || !json.contains("\"error\"")) return null; String details = nested(json, "error"); String code = string(details, "code"); String message = string(details, "message"); return new AviationstackError(code, message); }
    private static void logDiagnostic(int status, String body, AviationstackError error, String endpoint, String key) {
        System.err.println("[Aviationstack] HTTP status: " + status);
        System.err.println("[Aviationstack] Request endpoint: " + endpoint);
        System.err.println("[Aviationstack] Response body: " + redact(body, key));
        if (error != null) { System.err.println("[Aviationstack] Error code: " + error.code()); System.err.println("[Aviationstack] Error message: " + error.message()); }
    }
    private static String userMessage(int status, AviationstackError error) { if (error == null) return status == 429 ? "Aviationstack rate limit reached. Please try again later." : "Aviationstack is unavailable (HTTP " + status + ")."; String detail = (error.code().isBlank() ? "" : error.code()) + (error.message().isBlank() ? "" : (error.code().isBlank() ? "" : " — ") + error.message()); return detail.isBlank() ? "Aviationstack is unavailable (HTTP " + status + ")." : "Aviationstack: " + detail; }
    private static String redact(String value, String key) { if (value == null) return ""; String safe = value.replaceAll("(?i)(access_key=)[^&\\s\\\"]+", "$1REDACTED"); return key == null || key.isBlank() ? safe : safe.replace(key, "REDACTED").replace(encode(key), "REDACTED"); }
    private static String iata(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private static FlightStatus status(String value) { try{return FlightStatus.valueOf(value==null?"UNKNOWN":value.toUpperCase(Locale.ROOT));}catch(Exception ignored){return FlightStatus.UNKNOWN;} }
    private static String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
    private record AviationstackError(String code, String message) { }
}
