package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Coordinate-scoped live weather with a short cache and a graceful no-network fallback. */
public final class WeatherService {
    public interface Provider { Optional<Ghat.Weather> fetch(double latitude, double longitude); }
    public record HourlyForecast(String time, Integer temperatureCelsius, String condition) {
        public HourlyForecast { time = time == null ? "" : time; condition = condition == null ? "" : condition; }
    }
    public record WeatherSnapshot(Ghat.Weather current, Integer windSpeedKmh, List<HourlyForecast> forecast, boolean live) {
        public WeatherSnapshot { current = current == null ? Ghat.Weather.unavailable() : current; forecast = forecast == null ? List.of() : List.copyOf(forecast); }
        public static WeatherSnapshot unavailable() { return new WeatherSnapshot(Ghat.Weather.unavailable(), null, List.of(), false); }
    }

    private static final Logger LOGGER = Logger.getLogger(WeatherService.class.getName());
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(Long.getLong("ghats.weather.timeout.seconds", 8));
    private static final Duration CACHE_TTL = Duration.ofMinutes(Long.getLong("ghats.weather.cache.minutes", 15));
    private final Provider provider;
    private final OpenMeteoProvider openMeteoProvider;
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    /** Uses the keyless Open-Meteo forecast endpoint; no application secret is required. */
    public WeatherService() { openMeteoProvider = new OpenMeteoProvider(); provider = openMeteoProvider; }
    /** Supports an existing injected provider and deterministic tests. */
    public WeatherService(Provider provider) { this.provider = provider == null ? (lat, lon) -> Optional.empty() : provider; openMeteoProvider = provider instanceof OpenMeteoProvider live ? live : null; }

    /** Existing current-weather API retained for callers that do not need forecast/wind. */
    public Ghat.Weather weather(Double latitude, Double longitude) { return weatherAt(latitude, longitude).current(); }
    public WeatherSnapshot weatherFor(Ghat ghat) {
        if (ghat == null) return WeatherSnapshot.unavailable();
        if (validCoordinates(ghat.latitude(), ghat.longitude())) return weatherAt(ghat.latitude(), ghat.longitude());
        // Future/admin Ghats without verified coordinates may use their own area
        // text as a last resort; no other Ghat's coordinate is substituted.
        if (openMeteoProvider == null || ghat.area().isBlank()) return WeatherSnapshot.unavailable();
        return openMeteoProvider.geocode(ghat.area() + ", Nashik, Maharashtra")
                .map(point -> weatherAt(point.latitude(), point.longitude())).orElseGet(WeatherSnapshot::unavailable);
    }

    public WeatherSnapshot weatherAt(Double latitude, Double longitude) {
        if (!validCoordinates(latitude, longitude)) return WeatherSnapshot.unavailable();
        String key = cacheKey(latitude, longitude);
        Entry cached = cache.get(key);
        if (cached != null && System.currentTimeMillis() - cached.loadedAt < CACHE_TTL.toMillis()) return cached.snapshot;
        WeatherSnapshot loaded = load(latitude, longitude);
        cache.put(key, new Entry(loaded, System.currentTimeMillis()));
        return loaded;
    }

    private WeatherSnapshot load(Double latitude, Double longitude) {
        try {
            if (openMeteoProvider != null) return openMeteoProvider.fetchSnapshot(latitude, longitude).orElseGet(WeatherSnapshot::unavailable);
            return provider.fetch(latitude, longitude).map(value -> new WeatherSnapshot(value, null, List.of(), value.available())).orElseGet(WeatherSnapshot::unavailable);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Live Ghat weather request failed for " + cacheKey(latitude, longitude), exception);
            return WeatherSnapshot.unavailable();
        }
    }

    private static boolean validCoordinates(Double latitude, Double longitude) { return latitude != null && longitude != null && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180; }
    private static String cacheKey(double latitude, double longitude) { return Math.round(latitude * 10_000d) + ":" + Math.round(longitude * 10_000d); }
    private record Entry(WeatherSnapshot snapshot, long loadedAt) { }

    private static final class OpenMeteoProvider implements Provider {
        private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast";
        private static final String GEOCODING_ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search";
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
        @Override public Optional<Ghat.Weather> fetch(double latitude, double longitude) { return fetchSnapshot(latitude, longitude).map(WeatherSnapshot::current); }

        Optional<WeatherSnapshot> fetchSnapshot(double latitude, double longitude) {
            String url = ENDPOINT + "?latitude=" + latitude + "&longitude=" + longitude
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&hourly=temperature_2m,weather_code,precipitation_probability&forecast_hours=4&timezone=auto&wind_speed_unit=kmh";
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().timeout(REQUEST_TIMEOUT).header("Accept", "application/json").build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) { LOGGER.warning("Live Ghat weather endpoint returned HTTP " + response.statusCode()); return Optional.empty(); }
                return Optional.of(parse(response.body()));
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Live Ghat weather endpoint is unavailable", exception);
                return Optional.empty();
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Malformed live Ghat weather response", exception);
                return Optional.empty();
            }
        }

        Optional<Point> geocode(String place) {
            try {
                String url = GEOCODING_ENDPOINT + "?name=" + java.net.URLEncoder.encode(place, StandardCharsets.UTF_8) + "&count=1&language=en&format=json";
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().timeout(REQUEST_TIMEOUT).header("Accept", "application/json").build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) return Optional.empty();
                Double latitude = decimal(response.body(), "latitude"), longitude = decimal(response.body(), "longitude");
                return latitude == null || longitude == null ? Optional.empty() : Optional.of(new Point(latitude, longitude));
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Ghat-area weather geocoding is unavailable", exception);
                return Optional.empty();
            }
        }

        private static WeatherSnapshot parse(String json) {
            String current = object(json, "current");
            Integer temperature = integer(current, "temperature_2m"), humidity = integer(current, "relative_humidity_2m"), code = integer(current, "weather_code"), wind = integer(current, "wind_speed_10m");
            if (temperature == null || code == null) throw new IllegalArgumentException("Weather response lacks current conditions");
            String hourly = object(json, "hourly");
            List<String> times = strings(hourly, "time"); List<Integer> temperatures = integers(hourly, "temperature_2m"); List<Integer> codes = integers(hourly, "weather_code");
            List<HourlyForecast> forecast = new ArrayList<>();
            for (int i = 0; i < Math.min(3, Math.min(times.size(), Math.min(temperatures.size(), codes.size()))); i++) forecast.add(new HourlyForecast(times.get(i), temperatures.get(i), condition(codes.get(i))));
            List<Integer> rain = integers(hourly, "precipitation_probability");
            return new WeatherSnapshot(new Ghat.Weather(temperature, condition(code), rain.isEmpty() ? null : rain.get(0), humidity, string(current, "time")), wind, forecast, true);
        }

        private static String condition(Integer code) { if (code == null) return "Unavailable"; return switch (code) { case 0 -> "Clear"; case 1, 2 -> "Partly Cloudy"; case 3 -> "Cloudy"; case 45, 48 -> "Fog"; case 51, 53, 55, 56, 57 -> "Drizzle"; case 61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"; case 71, 73, 75, 77, 85, 86 -> "Snow"; case 95, 96, 99 -> "Thunderstorm"; default -> "Weather advisory"; }; }
        private static String object(String json, String key) { Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{(.*?)\\}(?=\\s*,\\s*\\\"|\\s*\\})", Pattern.DOTALL).matcher(json); if (!matcher.find()) throw new IllegalArgumentException("Missing " + key); return matcher.group(1); }
        private static String string(String json, String key) { Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json); return matcher.find() ? matcher.group(1) : ""; }
        private static Integer integer(String json, String key) { Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json); return matcher.find() ? (int) Math.round(Double.parseDouble(matcher.group(1))) : null; }
        private static Double decimal(String json, String key) { Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json); return matcher.find() ? Double.parseDouble(matcher.group(1)) : null; }
        private static List<String> strings(String json, String key) { Matcher matcher = Pattern.compile("\\\"([^\\\"]*)\\\"").matcher(array(json, key)); List<String> values = new ArrayList<>(); while (matcher.find()) values.add(matcher.group(1)); return values; }
        private static List<Integer> integers(String json, String key) { Matcher matcher = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(array(json, key)); List<Integer> values = new ArrayList<>(); while (matcher.find()) values.add((int) Math.round(Double.parseDouble(matcher.group()))); return values; }
        private static String array(String json, String key) { Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL).matcher(json); return matcher.find() ? matcher.group(1) : ""; }
        private record Point(double latitude, double longitude) { }
    }
}
