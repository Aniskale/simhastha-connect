package com.simhastha.service;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Temporary local fallback for Ghat overrides while Firestore access is unavailable. */
public final class LocalGhatOverrideStore {
    private static final Path DEFAULT_FILE = Path.of(System.getProperty("user.home"), ".simhastha-connect", "data", "ghat-overrides.json");
    private final Path file;

    public LocalGhatOverrideStore() { this(DEFAULT_FILE); }
    LocalGhatOverrideStore(Path file) { this.file = file.toAbsolutePath().normalize(); }

    public synchronized Map<String, Ghat> load() {
        if (!Files.isRegularFile(file)) return Map.of();
        try { return decode(Files.readString(file, StandardCharsets.UTF_8)); }
        catch (Exception ignored) { return Map.of(); }
    }

    public synchronized void save(Ghat ghat) throws IOException {
        Map<String, Ghat> overrides = new LinkedHashMap<>(load());
        overrides.put(ghat.id(), ghat);
        write(overrides);
    }

    public Path file() { return file; }

    private void write(Map<String, Ghat> overrides) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, encode(overrides.values()), StandardCharsets.UTF_8);
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static String encode(Iterable<Ghat> ghats) {
        List<String> values = new ArrayList<>();
        for (Ghat ghat : ghats) values.add(object(ghat));
        return "{\"version\":1,\"ghats\":[" + String.join(",", values) + "]}";
    }

    private static String object(Ghat ghat) {
        GhatOperationalState state = ghat.operationalState();
        return "{" + fields(
                pair("id", ghat.id()), pair("name", ghat.name()), pair("area", ghat.area()), pair("description", ghat.description()),
                pair("imageUrl", ghat.imageUrl()), pair("imagePublicId", ghat.imagePublicId()), pair("latitude", ghat.latitude()), pair("longitude", ghat.longitude()),
                pair("entryLatitude", ghat.entryLatitude()), pair("entryLongitude", ghat.entryLongitude()),
                pair("operationalStatus", ghat.operationalStatus().name()), pair("crowdLevel", ghat.crowdLevel().name()), pair("estimatedWait", ghat.estimatedWaitMinutes()),
                pair("bathingAvailable", ghat.bathingAvailable()), pair("walkingDifficulty", ghat.walking().difficulty().name()),
                pair("steps", ghat.walking().approximateSteps()), pair("distance", ghat.walking().distanceMeters()), pair("seniorFriendly", ghat.walking().seniorFriendly()), pair("wheelchair", ghat.walking().wheelchairAccessible()),
                pair("facilities", String.join("\u001F", ghat.facilities())), pair("history", ghat.history().historicalBackground()), pair("significance", ghat.history().religiousSignificance()), pair("simhastha", ghat.history().simhasthaConnection()),
                pair("bathingStatus", state.bathingStatus().name()), pair("waterSafety", state.waterSafety().name()), pair("restriction", state.restrictionReason()),
                pair("alertPriority", state.priorityAlert().priority().name()), pair("alert", state.priorityAlert().message()), pair("published", ghat.published()), pair("active", ghat.active()), pair("lastUpdated", ghat.lastUpdated())) + "}";
    }

    private static String fields(String... values) { return String.join(",", values); }
    private static String pair(String key, Object value) { return "\"" + key + "\":\"" + escape(value == null ? "" : String.valueOf(value)) + "\""; }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
    private static String unescape(String value) { return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\"); }

    private static Map<String, Ghat> decode(String json) {
        Map<String, Ghat> result = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("\\{\\\"id\\\":\\\"((?:\\\\.|[^\\\"])*)\\\"(.*?)\\}").matcher(json == null ? "" : json);
        while (matcher.find()) {
            String object = "{\"id\":\"" + matcher.group(1) + "\"" + matcher.group(2) + "}";
            Ghat ghat = ghat(object);
            if (!ghat.id().isBlank()) result.put(ghat.id(), ghat);
        }
        return result;
    }

    private static Ghat ghat(String json) {
        Ghat.Walking walking = new Ghat.Walking(enumValue(Ghat.WalkingDifficulty.class, value(json, "walkingDifficulty"), Ghat.WalkingDifficulty.MODERATE),
                integer(json, "steps"), integer(json, "distance"), bool(json, "seniorFriendly"), bool(json, "wheelchair"));
        GhatOperationalState state = new GhatOperationalState(
                enumValue(GhatOperationalState.BathingStatus.class, value(json, "bathingStatus"), GhatOperationalState.BathingStatus.UNAVAILABLE),
                enumValue(GhatOperationalState.WaterSafety.class, value(json, "waterSafety"), GhatOperationalState.WaterSafety.CAUTION), List.of(), List.of(), List.of(), List.of(), List.of(),
                GhatOperationalState.CleaningStatus.NORMAL, value(json, "restriction"),
                new GhatOperationalState.PriorityAlert(enumValue(GhatOperationalState.AlertPriority.class, value(json, "alertPriority"), GhatOperationalState.AlertPriority.INFO), value(json, "alert")), "");
        return new Ghat(value(json, "id"), value(json, "name"), value(json, "area"), value(json, "description"), decimal(json, "latitude"), decimal(json, "longitude"),
                decimal(json, "entryLatitude"), decimal(json, "entryLongitude"), value(json, "imageUrl"), value(json, "imagePublicId"),
                enumValue(Ghat.OperationalStatus.class, value(json, "operationalStatus"), Ghat.OperationalStatus.INFORMATION_ONLY),
                enumValue(Ghat.CrowdLevel.class, value(json, "crowdLevel"), Ghat.CrowdLevel.UNKNOWN), integer(json, "estimatedWait"), bool(json, "bathingAvailable"), walking,
                split(value(json, "facilities")), Ghat.Weather.unavailable(), new Ghat.History(value(json, "history"), value(json, "significance"), value(json, "simhastha"), "", "", "", ""),
                value(json, "lastUpdated"), state, bool(json, "published"), bool(json, "active"));
    }

    private static String value(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\":\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
        return matcher.find() ? unescape(matcher.group(1)) : "";
    }
    private static Integer integer(String json, String key) { try { String value = value(json, key); return value.isBlank() ? null : Integer.valueOf(value); } catch (Exception ignored) { return null; } }
    private static Double decimal(String json, String key) { try { String value = value(json, key); return value.isBlank() ? null : Double.valueOf(value); } catch (Exception ignored) { return null; } }
    private static boolean bool(String json, String key) { return Boolean.parseBoolean(value(json, key)); }
    private static List<String> split(String value) { return value.isBlank() ? List.of() : List.of(value.split("\u001F", -1)); }
    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) { try { return value.isBlank() ? fallback : Enum.valueOf(type, value); } catch (Exception ignored) { return fallback; } }
}
