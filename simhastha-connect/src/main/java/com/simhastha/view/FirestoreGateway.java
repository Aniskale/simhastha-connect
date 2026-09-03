package com.simhastha.view;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import com.simhastha.model.LostFoundReport;
import com.simhastha.model.OfficialHelpLocation;
import com.simhastha.service.GhatRepository;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public final class FirestoreGateway implements GhatRepository {

    private static final String ROOT = "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents";
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\".*?\"fields\"\\s*:\\s*\\{(.*?)\\}\\s*(?:,\\s*\"createTime\"|,\\s*\"updateTime\"|\\})", Pattern.DOTALL);
    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\\{\\s*\"stringValue\"\\s*:\\s*\"(.*?)\"\\s*\\}", Pattern.DOTALL);

    private final FirebaseConfig config;
    private static final Logger LOGGER = Logger.getLogger(FirestoreGateway.class.getName());
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public FirestoreGateway(FirebaseConfig config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public List<AppDataStore.ServiceItem> loadItems() throws IOException, InterruptedException {
        return loadItems("");
    }

    public List<AppDataStore.ServiceItem> loadItems(String idToken) throws IOException, InterruptedException {
        String json = get(collectionUrl("appItems"), idToken);
        List<AppDataStore.ServiceItem> items = new ArrayList<>();
        for (Document document : parseDocuments(json)) {
            String module = field(document.fields, "module");
            String title = field(document.fields, "title");
            String detail = field(document.fields, "detail");
            String category = field(document.fields, "category");
            if (notBlank(module) && notBlank(title) && notBlank(detail)) {
                items.add(new AppDataStore.ServiceItem(document.id, module, title, detail, category));
            }
        }
        return items;
    }

    /** Reads the ghat collection as structured data; controllers never parse Firestore fields directly. */
    public List<Ghat> loadGhats(String idToken) throws IOException, InterruptedException {
        if (!isEnabled()) {
            return List.of();
        }
        List<Ghat> ghats = new ArrayList<>();
        for (Document document : loadPublishedGhatDocuments(idToken)) {
            Ghat ghat = parseGhat(document);
            if (ghat != null && ghat.published() && ghat.active()) ghats.add(ghat);
        }
        return ghats;
    }

    /** Admin-only listing includes drafts and inactive records from the same ghats collection. */
    public List<Ghat> loadAdminGhats(String idToken) throws IOException, InterruptedException {
        if (!isEnabled()) return List.of();
        List<Ghat> ghats = new ArrayList<>();
        for (Document document : loadCollectionDocuments("ghats", idToken)) {
            Ghat ghat = parseGhat(document);
            if (ghat != null) ghats.add(ghat);
        }
        return ghats;
    }

    @Override
    public Ghat loadGhat(String ghatId, String idToken) throws IOException, InterruptedException {
        HttpRequest request = authorizedBuilder(documentUri("ghats", ghatId), idToken)
                .timeout(Duration.ofSeconds(8)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw firestoreFailure("GET", documentUri("ghats", ghatId), response);
        }
        return parseDocuments(response.body()).stream().map(this::parseGhat).filter(java.util.Objects::nonNull)
                .findFirst().orElseThrow(() -> new IOException("Ghat document could not be parsed: " + ghatId));
    }

    private Ghat parseGhat(Document document) {
            try {
                String fields = document.fields;
                String name = firstNonBlank(field(fields, "ghatName"), field(fields, "name"), field(fields, "title"));
                if (!notBlank(name)) return null;
                Ghat ghat = new Ghat(document.id, name, firstNonBlank(field(fields, "area"), field(fields, "location")),
                    field(fields, "description"), decimalField(fields, "latitude"), decimalField(fields, "longitude"),
                    decimalField(fields, "entryLatitude"), decimalField(fields, "entryLongitude"), field(fields, "imageUrl"),
                    enumValue(Ghat.OperationalStatus.class, field(fields, "operationalStatus"), Ghat.OperationalStatus.INFORMATION_ONLY),
                    enumValue(Ghat.CrowdLevel.class, field(fields, "crowdLevel"), Ghat.CrowdLevel.UNKNOWN),
                    integerField(fields, "estimatedWaitMinutes"), "true".equalsIgnoreCase(boolField(fields, "bathingAvailable")),
                    new Ghat.Walking(enumValue(Ghat.WalkingDifficulty.class, field(fields, "walkingDifficulty"), Ghat.WalkingDifficulty.MODERATE),
                            integerField(fields, "approximateSteps"), integerField(fields, "walkingDistanceMeters"),
                            "true".equalsIgnoreCase(boolField(fields, "seniorFriendly")),
                            "true".equalsIgnoreCase(boolField(fields, "wheelchairAccessible"))),
                    stringListField(fields, "facilities"),
                    new Ghat.Weather(integerField(fields, "weatherTemperatureCelsius"), field(fields, "weatherCondition")),
                    new Ghat.History(field(fields, "historicalBackground"), field(fields, "religiousSignificance"),
                            field(fields, "simhasthaConnection"), field(fields, "associatedSacredPlaces"),
                            field(fields, "rituals"), field(fields, "didYouKnow"), field(fields, "historyImageUrl")),
                    firstNonBlank(field(fields, "lastUpdated"), field(fields, "updatedAt"), timestampField(fields, "updatedAt")),
                    operationalState(fields), !"false".equalsIgnoreCase(boolField(fields, "published")),
                    !"false".equalsIgnoreCase(boolField(fields, "active")));
                return ghat;
            } catch (RuntimeException exception) {
                LOGGER.warning("Skipping malformed ghat document " + document.id + ": " + exception.getMessage());
                return null;
            }
    }

    /** Persists the shared Ghat model directly; no Admin-only document shape is used. */
    public void saveGhat(Ghat ghat, String idToken) throws IOException, InterruptedException {
        String now = String.valueOf(System.currentTimeMillis());
        String json = fieldsJson(
                fieldJson("ghatName", ghat.name()), fieldJson("area", ghat.area()), fieldJson("description", ghat.description()),
                fieldJson("imageUrl", ghat.imageUrl()), numberOrNullFieldJson("latitude", ghat.latitude()), numberOrNullFieldJson("longitude", ghat.longitude()),
                numberOrNullFieldJson("entryLatitude", ghat.entryLatitude()), numberOrNullFieldJson("entryLongitude", ghat.entryLongitude()),
                fieldJson("operationalStatus", ghat.operationalStatus().name()), fieldJson("crowdLevel", ghat.crowdLevel().name()),
                numberOrNullFieldJson("estimatedWaitMinutes", ghat.estimatedWaitMinutes()), boolFieldJson("bathingAvailable", ghat.bathingAvailable()),
                fieldJson("walkingDifficulty", ghat.walking().difficulty().name()), numberOrNullFieldJson("approximateSteps", ghat.walking().approximateSteps()),
                numberOrNullFieldJson("walkingDistanceMeters", ghat.walking().distanceMeters()), boolFieldJson("seniorFriendly", ghat.walking().seniorFriendly()),
                boolFieldJson("wheelchairAccessible", ghat.walking().wheelchairAccessible()), stringArrayFieldJson("facilities", ghat.facilities()),
                fieldJson("historicalBackground", ghat.history().historicalBackground()), fieldJson("religiousSignificance", ghat.history().religiousSignificance()),
                fieldJson("simhasthaConnection", ghat.history().simhasthaConnection()), fieldJson("associatedSacredPlaces", ghat.history().associatedSacredPlaces()),
                fieldJson("rituals", ghat.history().rituals()), fieldJson("didYouKnow", ghat.history().didYouKnow()), fieldJson("historyImageUrl", ghat.history().imageUrl()),
                fieldJson("bathingStatus", ghat.operationalState().bathingStatus().name()),
                fieldJson("waterSafety", ghat.operationalState().waterSafety().name()), fieldJson("restrictionReason", ghat.operationalState().restrictionReason()),
                fieldJson("alertPriority", ghat.operationalState().priorityAlert().priority().name()), fieldJson("alertMessage", ghat.operationalState().priorityAlert().message()),
                stringArrayFieldJson("gates", ghat.operationalState().gates().stream().map(gate -> gate.name() + "|" + gate.status() + "|" + gate.note()).toList()),
                stringArrayFieldJson("zones", ghat.operationalState().zones().stream().map(zone -> zone.name() + "|" + zone.status() + "|" + zone.crowdLevel() + "|" + (zone.bathingAvailable() ? "AVAILABLE" : "UNAVAILABLE") + "|" + zone.hazard()).toList()),
                stringArrayFieldJson("hazards", ghat.operationalState().hazards().stream().map(hazard -> hazard.type() + "|" + hazard.message() + "|" + hazard.priority()).toList()),
                stringArrayFieldJson("facilityStatuses", ghat.operationalState().facilities().stream().map(facility -> facility.name() + "|" + facility.status()).toList()),
                stringArrayFieldJson("accessWindows", ghat.operationalState().accessWindows().stream().map(window -> window.start() + "|" + window.end() + "|" + window.accessStatus() + "|" + window.note()).toList()),
                fieldJson("cleaningStatus", ghat.operationalState().cleaningStatus().name()), fieldJson("operationalUpdatedAt", ghat.operationalState().lastUpdated()),
                boolFieldJson("published", ghat.published()), boolFieldJson("active", ghat.active()), fieldJson("updatedAt", now));
        sendAuthorizedPatch(documentUri("ghats", ghat.id()), json, idToken);
    }

    /** Image-only update avoids overwriting operational data during Admin Change Image. */
    @Override
    public void updateGhatImage(String ghatId, String imageUrl, String idToken) throws IOException, InterruptedException {
        String json = fieldsJson(fieldJson("imageUrl", imageUrl), fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        URI document = documentUri("ghats", ghatId);
        System.out.println("FIREBASE PROJECT ID = " + config.projectId());
        System.out.println("FIREBASE DOCUMENT PATH = " + document.getPath());
        System.out.println("UPDATE PAYLOAD = " + json);
        sendAuthorizedPatch(document, json, idToken);
    }

    private GhatOperationalState operationalState(String fields) {
        GhatOperationalState.BathingStatus bathing = enumValue(GhatOperationalState.BathingStatus.class,
                field(fields, "bathingStatus"), "true".equalsIgnoreCase(boolField(fields, "bathingAvailable")) ? GhatOperationalState.BathingStatus.AVAILABLE : GhatOperationalState.BathingStatus.UNAVAILABLE);
        GhatOperationalState.WaterSafety water = enumValue(GhatOperationalState.WaterSafety.class, field(fields, "waterSafety"), GhatOperationalState.WaterSafety.CAUTION);
        return new GhatOperationalState(bathing, water, hazards(stringListField(fields, "hazards")), zones(stringListField(fields, "zones")),
                gates(stringListField(fields, "gates")), facilities(stringListField(fields, "facilityStatuses")), accessWindows(stringListField(fields, "accessWindows")),
                enumValue(GhatOperationalState.CleaningStatus.class, field(fields, "cleaningStatus"), GhatOperationalState.CleaningStatus.NORMAL),
                field(fields, "restrictionReason"), new GhatOperationalState.PriorityAlert(enumValue(GhatOperationalState.AlertPriority.class,
                        field(fields, "alertPriority"), GhatOperationalState.AlertPriority.INFO), field(fields, "alertMessage")),
                firstNonBlank(field(fields, "operationalUpdatedAt"), timestampField(fields, "operationalUpdatedAt")));
    }

    private List<GhatOperationalState.Hazard> hazards(List<String> values) { return values.stream().map(value -> { String[] parts = value.split("\\|", 3); return new GhatOperationalState.Hazard(enumValue(GhatOperationalState.HazardType.class, parts[0], GhatOperationalState.HazardType.OTHER), parts.length > 1 ? parts[1] : value, parts.length > 2 ? enumValue(GhatOperationalState.AlertPriority.class, parts[2], GhatOperationalState.AlertPriority.ADVISORY) : GhatOperationalState.AlertPriority.ADVISORY); }).toList(); }
    private List<GhatOperationalState.Zone> zones(List<String> values) { return values.stream().map(value -> { String[] p = value.split("\\|", 5); return new GhatOperationalState.Zone(p[0], p[0], p.length > 1 ? enumValue(GhatOperationalState.ZoneStatus.class, p[1], GhatOperationalState.ZoneStatus.OPEN) : GhatOperationalState.ZoneStatus.OPEN, p.length > 2 ? enumValue(Ghat.CrowdLevel.class, p[2], Ghat.CrowdLevel.MODERATE) : Ghat.CrowdLevel.MODERATE, p.length <= 3 || "AVAILABLE".equalsIgnoreCase(p[3]), p.length > 4 ? p[4] : "", ""); }).toList(); }
    private List<GhatOperationalState.Gate> gates(List<String> values) { return values.stream().map(value -> { String[] p = value.split("\\|", 3); return new GhatOperationalState.Gate(p[0], p[0], p.length > 1 ? enumValue(GhatOperationalState.GateStatus.class, p[1], GhatOperationalState.GateStatus.OPEN) : GhatOperationalState.GateStatus.OPEN, p.length > 2 ? p[2] : ""); }).toList(); }
    private List<GhatOperationalState.Facility> facilities(List<String> values) { return values.stream().map(value -> { String[] p = value.split("\\|", 2); return new GhatOperationalState.Facility(p[0], p.length > 1 ? enumValue(GhatOperationalState.FacilityStatus.class, p[1], GhatOperationalState.FacilityStatus.UNAVAILABLE) : GhatOperationalState.FacilityStatus.AVAILABLE); }).toList(); }
    private List<GhatOperationalState.AccessWindow> accessWindows(List<String> values) { return values.stream().map(value -> { try { String[] p = value.split("\\|", 4); return new GhatOperationalState.AccessWindow(java.time.LocalTime.parse(p[0]), java.time.LocalTime.parse(p[1]), enumValue(GhatOperationalState.ZoneStatus.class, p[2], GhatOperationalState.ZoneStatus.OPEN), p.length > 3 ? p[3] : ""); } catch (Exception exception) { LOGGER.warning("Ignoring malformed ghat access window: " + value); return null; } }).filter(java.util.Objects::nonNull).toList(); }

    public List<AppDataStore.ApprovalRequest> loadApprovals() throws IOException, InterruptedException {
        return loadApprovals("");
    }

    public List<AppDataStore.ApprovalRequest> loadApprovals(String idToken) throws IOException, InterruptedException {
        String json = get(collectionUrl("approvalRequests"), idToken);
        List<AppDataStore.ApprovalRequest> approvals = new ArrayList<>();
        for (Document document : parseDocuments(json)) {
            String type = field(document.fields, "type");
            String title = field(document.fields, "title");
            String detail = field(document.fields, "detail");
            String targetModule = field(document.fields, "targetModule");
            String ownerId = field(document.fields, "ownerId");
            String status = field(document.fields, "status");
            if (notBlank(type) && notBlank(title) && notBlank(targetModule)
                    && (status.isBlank() || "pending".equals(status))) {
                approvals.add(new AppDataStore.ApprovalRequest(document.id, type, title, detail, targetModule, ownerId));
            }
        }
        return approvals;
    }

    public List<AppDataStore.ApprovalRequest> loadPendingBusinessApprovals(String idToken)
            throws IOException, InterruptedException {
        String json = get(collectionUrl("businesses"), idToken);
        List<AppDataStore.ApprovalRequest> approvals = new ArrayList<>();
        for (Document document : parseDocuments(json)) {
            String ownerId = ownerIdFromBusinessDocument(document);
            String status = field(document.fields, "status");
            String approved = boolField(document.fields, "approved");
            if (!notBlank(ownerId) || (!"pending".equals(status) && !"false".equals(approved))) {
                continue;
            }

            String businessName = valueOr("Business Registration", field(document.fields, "businessName"));
            String category = field(document.fields, "category");
            String location = field(document.fields, "location");
            String mobile = field(document.fields, "mobile");
            String detail = String.join(" | ", java.util.stream.Stream.of(category, location, mobile)
                    .filter(this::notBlank)
                    .toList());
            approvals.add(new AppDataStore.ApprovalRequest(
                    "business-" + ownerId,
                    "Business Registration",
                    businessName,
                    detail,
                    "business",
                    ownerId));
        }
        return approvals;
    }

    public List<AppDataStore.UserRecord> loadUsers(String idToken) throws IOException, InterruptedException {
        List<AppDataStore.UserRecord> records = new ArrayList<>();
        for (Document document : loadCollectionDocuments("users", idToken)) {
            String fields = document.fields;
            records.add(new AppDataStore.UserRecord(
                    valueOr(document.id, field(fields, "uid")),
                    firstNonBlank(field(fields, "name"), field(fields, "displayName")),
                    field(fields, "email"),
                    field(fields, "mobile"),
                    field(fields, "role"),
                    field(fields, "status"),
                    field(fields, "createdAt"),
                    field(fields, "updatedAt")));
        }
        return records;
    }

    public List<AppDataStore.BusinessRecord> loadBusinesses(String idToken) throws IOException, InterruptedException {
        List<AppDataStore.BusinessRecord> records = new ArrayList<>();
        for (Document document : loadCollectionDocuments("businesses", idToken)) {
            String fields = document.fields;
            String ownerId = ownerIdFromBusinessDocument(document);
            String approved = boolField(fields, "approved");
            records.add(new AppDataStore.BusinessRecord(
                    document.id,
                    ownerId,
                    field(fields, "businessName"),
                    field(fields, "ownerName"),
                    field(fields, "category"),
                    field(fields, "description"),
                    field(fields, "location"),
                    field(fields, "mobile"),
                    field(fields, "email"),
                    firstNonBlank(field(fields, "operatingHours"), operatingHours(fields)),
                    field(fields, "priceRange"),
                    field(fields, "status"),
                    "true".equalsIgnoreCase(approved),
                    field(fields, "createdAt"),
                    field(fields, "updatedAt")));
        }
        return records;
    }

    public List<AppDataStore.TransportOperatorRecord> loadTransportOperators(String idToken)
            throws IOException, InterruptedException {
        List<AppDataStore.TransportOperatorRecord> records = new ArrayList<>();
        for (Document document : loadCollectionDocuments("transportOperators", idToken)) {
            String fields = document.fields;
            records.add(new AppDataStore.TransportOperatorRecord(
                    document.id,
                    firstNonBlank(field(fields, "ownerId"), document.id),
                    field(fields, "organizationName"),
                    field(fields, "contactPerson"),
                    field(fields, "mobile"),
                    field(fields, "email"),
                    field(fields, "serviceType"),
                    field(fields, "status"),
                    numberField(fields, "vehicleCount"),
                    numberField(fields, "routesSubmitted"),
                    numberField(fields, "activeRoutes"),
                    field(fields, "createdAt"),
                    field(fields, "updatedAt")));
        }
        return records;
    }

    public List<AppDataStore.BookingRecord> loadBookings(String idToken) throws IOException, InterruptedException {
        List<AppDataStore.BookingRecord> records = new ArrayList<>();
        for (Document document : loadCollectionDocuments("bookings", idToken)) {
            records.add(bookingFrom(document));
        }
        return records;
    }

    public List<AppDataStore.BookingRecord> loadBookingsForField(String fieldName, String value, String idToken)
            throws IOException, InterruptedException {
        if (!notBlank(value)) {
            return List.of();
        }
        String query = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"bookings\"}],"
                + "\"where\":{\"fieldFilter\":{\"field\":{\"fieldPath\":\"" + escape(fieldName) + "\"},"
                + "\"op\":\"EQUAL\",\"value\":{\"stringValue\":\"" + escape(value) + "\"}}}}}";
        String json = post(URI.create(String.format(ROOT, enc(config.projectId())) + ":runQuery?key=" + enc(config.apiKey())),
                query, idToken);
        List<AppDataStore.BookingRecord> result = new ArrayList<>();
        for (Document document : parseDocuments(json)) {
            result.add(bookingFrom(document));
        }
        return result;
    }

    public List<AppDataStore.RouteRecord> loadTransportRoutes(String idToken) throws IOException, InterruptedException {
        List<AppDataStore.RouteRecord> records = new ArrayList<>();
        for (Document document : loadCollectionDocuments("transportRoutes", idToken)) {
            String fields = document.fields;
            records.add(new AppDataStore.RouteRecord(
                    document.id,
                    field(fields, "routeName"),
                    field(fields, "from"),
                    field(fields, "to"),
                    firstNonBlank(field(fields, "via"), field(fields, "stops")),
                    field(fields, "mode"),
                    field(fields, "startTime"),
                    field(fields, "endTime"),
                    field(fields, "fare"),
                    field(fields, "duration"),
                    firstNonBlank(field(fields, "operatorId"), field(fields, "ownerId")),
                    "true".equalsIgnoreCase(boolField(fields, "official")),
                    field(fields, "mapUrl"),
                    field(fields, "liveSourceUrl"),
                    "true".equalsIgnoreCase(boolField(fields, "published")),
                    !"false".equalsIgnoreCase(boolField(fields, "active")),
                    field(fields, "createdAt"),
                    field(fields, "updatedAt")));
        }
        return records;
    }

    public List<AppDataStore.ServiceItem> loadPublicModuleItems(String idToken) throws IOException, InterruptedException {
        List<AppDataStore.ServiceItem> items = new ArrayList<>();
        loadPublicCollection(items, "transportRoutes", "transport", "routeName", "Transport");
        loadPublicCollection(items, "businesses", "business", "businessName", "Business");
        loadPublicCollection(items, "kumbhPackages", "packages", "packageName", "Kumbh Package");
        loadPublicCollection(items, "pujaServices", "puja", "pujaName", "Puja");
        loadPublicCollection(items, "ghats", "ghat", "ghatName", "Ghat");
        loadPublicCollection(items, "stays", "stay", "property", "Stay");
        loadPublicCollection(items, "events", "schedule", "eventName", "Schedule");
        loadPublicCollection(items, "announcements", "announcement", "title", "Announcement");
        loadPublicCollection(items, "liveUpdates", "announcement", "title", "Live Update");
        loadPublicCollection(items, "emergencyContacts", "emergency", "name", "Emergency");
        return items;
    }

    public List<AppDataStore.ServiceItem> loadAdminOperationalItems(String idToken) {
        List<AppDataStore.ServiceItem> items = new ArrayList<>();
        loadAdminCollection(items, "kumbhPackages", "packages", "packageName", "Kumbh Package", idToken);
        loadAdminCollection(items, "pujaServices", "puja", "pujaName", "Puja", idToken);
        loadAdminCollection(items, "ghats", "ghat", "ghatName", "Ghat", idToken);
        loadAdminCollection(items, "stays", "stay", "property", "Stay", idToken);
        loadAdminCollection(items, "events", "schedule", "eventName", "Schedule", idToken);
        loadAdminCollection(items, "announcements", "announcement", "title", "Announcement", idToken);
        loadAdminCollection(items, "liveUpdates", "announcement", "title", "Live Update", idToken);
        loadAdminCollection(items, "emergencyContacts", "emergency", "name", "Emergency", idToken);
        return items;
    }

    private void loadAdminCollection(List<AppDataStore.ServiceItem> target, String collection, String module,
            String titleField, String fallbackCategory, String idToken) {
        for (Document document : loadOptionalCollectionDocuments(collection, idToken)) {
            String fields = document.fields;
            String title = firstNonBlank(field(fields, titleField), field(fields, "title"), field(fields, "businessName"));
            if (notBlank(title)) {
                target.add(new AppDataStore.ServiceItem(document.id, module, title, publicDetail(fields), fallbackCategory));
            }
        }
    }

    public List<AppDataStore.LostFoundCaseRecord> loadLostFoundCases(String idToken)
            throws IOException, InterruptedException {
        List<AppDataStore.LostFoundCaseRecord> records = new ArrayList<>();
        for (Document document : loadCollectionDocuments("lostFoundReports", idToken)) {
            String fields = document.fields;
            records.add(new AppDataStore.LostFoundCaseRecord(
                    document.id,
                    firstNonBlank(field(fields, "type"), field(fields, "reportType")),
                    firstNonBlank(field(fields, "name"), field(fields, "itemName"), field(fields, "title")),
                    field(fields, "age"),
                    field(fields, "gender"),
                    field(fields, "clothing"),
                    field(fields, "identificationMarks"),
                    firstNonBlank(field(fields, "lastSeenLocation"), field(fields, "location")),
                    firstNonBlank(field(fields, "lastSeenDateTime"), field(fields, "lastSeenAt")),
                    field(fields, "reporterName"),
                    field(fields, "relation"),
                    firstNonBlank(field(fields, "contact"), field(fields, "mobile")),
                    firstNonBlank(field(fields, "status"), "open"),
                    firstNonBlank(field(fields, "priority"), "normal"),
                    field(fields, "createdAt"),
                    field(fields, "updatedAt"),
                    field(fields, "trackingId"),
                    field(fields, "imageUrl"),
                    field(fields, "category"),
                    "true".equals(boolField(fields, "isDemo"))));
        }
        return records;
    }

    public void saveLostFoundReport(LostFoundReport report, String idToken) throws IOException, InterruptedException {
        AppSession.User session = AppSession.currentUser();
        System.out.println("LOST_FOUND_CREATE_DIAGNOSTIC"
                + " tokenPresent=" + (idToken != null && !idToken.isBlank())
                + " sessionUid=" + safeDiagnostic(session == null ? "" : session.uid())
                + " sessionRole=" + safeDiagnostic(session == null ? "" : session.role())
                + " reportUserId=" + safeDiagnostic(report.userId())
                + " reporterUserId=" + safeDiagnostic(report.userId())
                + " reportId=" + safeDiagnostic(report.reportId())
                + " httpMethod=POST"
                + " status=" + safeDiagnostic(report.status())
                + " reportType=" + safeDiagnostic(report.reportType())
                + " paymentStatus=<not-applicable>"
                + " projectId=" + safeDiagnostic(config.projectId()));
        List<String> fields = new ArrayList<>();
        fields.add(fieldJson("reportId", report.reportId()));
        fields.add(fieldJson("trackingId", report.trackingId()));
        fields.add(fieldJson("userId", report.userId()));
        fields.add(fieldJson("reporterUid", report.userId()));
        fields.add(fieldJson("reportType", report.reportType()));
        fields.add(fieldJson("category", report.category()));
        fields.add(fieldJson("title", report.title()));
        addOptionalStringField(fields, "description", report.description());
        if (!report.imageUrls().isEmpty()) addOptionalStringField(fields, "imageUrl", report.imageUrls().get(0));
        addOptionalStringField(fields, "incidentDate", report.incidentDate());
        addOptionalStringField(fields, "incidentTime", report.incidentTime());
        addOptionalStringField(fields, "reporterName", report.reporterName());
        addOptionalStringField(fields, "reporterPhone", report.reporterPhone());
        addOptionalStringField(fields, "relation", report.relation());
        fields.add(fieldJson("priority", report.priority()));
        fields.add(fieldJson("priorityReason", report.priorityReason()));
        fields.add(fieldJson("status", report.status()));
        fields.add(fieldJson("verificationStatus", report.verificationStatus()));
        fields.add(fieldJson("createdAt", report.createdAt()));
        fields.add(fieldJson("updatedAt", report.updatedAt()));
        System.out.println("LOST_FOUND_CREATE_DIAGNOSTIC payloadFields=" + fields.stream()
                .map(field -> field.substring(1, field.indexOf('"', 1))).toList());
        String json = fieldsJson(fields.toArray(String[]::new));
        URI collection = URI.create(collectionUrl("lostFoundReports") + "&documentId=" + enc(report.reportId()));
        sendAuthorizedPost(collection, json, idToken);
    }

    public List<LostFoundReport> loadLostFoundReportsForUser(String userId, String idToken) throws IOException, InterruptedException {
        List<LostFoundReport> reports = new ArrayList<>();
        for (Document document : loadQueryDocuments("lostFoundReports", "userId", userId, idToken)) reports.add(lostFoundReport(document));
        return reports;
    }

    /** Development-only, admin-authorised Firestore seed. Stable IDs make this operation idempotent. */
    public int seedLostFoundDemoReports(String idToken) throws IOException, InterruptedException {
        AppSession.User session = AppSession.currentUser();
        if (session == null || !session.isAdmin()) throw new SecurityException("An admin session is required to seed Lost & Found demo reports.");
        int inserted = 0;
        for (LostFoundDemo demo : lostFoundDemos()) {
            if (documentExists("lostFoundReports", demo.id(), idToken)) continue;
            List<String> fields = new ArrayList<>();
            fields.add(fieldJson("reportId", demo.id()));
            fields.add(fieldJson("trackingId", demo.trackingId()));
            fields.add(fieldJson("userId", "demo-lost-found-owner"));
            fields.add(fieldJson("reporterUid", "demo-lost-found-owner"));
            fields.add(fieldJson("reportType", demo.reportType()));
            fields.add(fieldJson("category", demo.category()));
            fields.add(fieldJson("title", demo.title()));
            fields.add(fieldJson("description", demo.description()));
            fields.add(fieldJson("priority", demo.priority()));
            fields.add(fieldJson("priorityReason", "Development demonstration record"));
            fields.add(fieldJson("status", demo.status()));
            fields.add(fieldJson("verificationStatus", demo.verificationStatus()));
            fields.add(boolFieldJson("isDemo", true));
            fields.add(fieldJson("createdAt", demo.createdAt()));
            fields.add(fieldJson("updatedAt", demo.createdAt()));
            URI collection = URI.create(collectionUrl("lostFoundReports") + "&documentId=" + enc(demo.id()));
            sendAuthorizedPost(collection, fieldsJson(fields.toArray(String[]::new)), idToken);
            inserted++;
        }
        return inserted;
    }

    public List<OfficialHelpLocation> loadOfficialHelpLocations(String idToken) throws IOException, InterruptedException {
        List<OfficialHelpLocation> locations = new ArrayList<>();
        try {
            HelpCenterQueryResult result = loadVerifiedActiveHelpCenterDocuments(idToken);
            for (Document document : result.documents()) {
                String f = document.fields;
                Double latitude = numberOrNull(f, "latitude");
                Double longitude = numberOrNull(f, "longitude");
                if (latitude == null || longitude == null) continue;
                OfficialHelpLocation location = new OfficialHelpLocation(document.id, field(f, "name"), field(f, "type"), field(f, "area"),
                        latitude, longitude, field(f, "address"), field(f, "landmark"),
                        firstNonBlank(field(f, "contactNumber"), field(f, "phone")), field(f, "openingHours"), stringListField(f, "services"),
                        field(f, "verificationStatus"), "true".equals(boolField(f, "active")), field(f, "createdAt"), field(f, "updatedAt"));
                if (isUsableOfficialHelpLocation(location)) locations.add(location);
            }
            System.out.println("HELP_CENTER_LOAD statusCode=" + result.statusCode() + " documentsReceived=" + result.documents().size()
                    + " verifiedActiveCount=" + locations.size() + " error=<none>");
        } catch (IOException | InterruptedException error) {
            System.out.println("HELP_CENTER_LOAD statusCode=" + statusCodeFrom(error) + " documentsReceived=0 verifiedActiveCount=0 error=" + safeHelpCenterError(error));
            throw error;
        }
        return locations;
    }

    /** Admin-only listing. This intentionally does not apply the public visibility filter. */
    public List<OfficialHelpLocation> loadOfficialHelpLocationsForAdmin(String idToken) throws IOException, InterruptedException {
        requireVerifiedAdminSession(idToken);
        List<OfficialHelpLocation> locations = new ArrayList<>();
        for (Document document : loadCollectionDocuments("officialHelpLocations", idToken)) {
            OfficialHelpLocation location = officialHelpLocation(document);
            if (location != null) locations.add(location);
        }
        return locations;
    }

    public void saveOfficialHelpLocation(OfficialHelpLocation location, boolean create, String idToken)
            throws IOException, InterruptedException {
        requireVerifiedAdminSession(idToken);
        if (location == null || !notBlank(location.id()) || !notBlank(location.name()) || !notBlank(location.type())
                || !Double.isFinite(location.latitude()) || !Double.isFinite(location.longitude())
                || location.latitude() < -90 || location.latitude() > 90 || location.longitude() < -180 || location.longitude() > 180) {
            throw new IllegalArgumentException("Enter a center name, type, and valid latitude and longitude.");
        }
        AppSession.User admin = AppSession.currentUser();
        if (create) {
            System.out.println("ADMIN_HELP_CENTER_CREATE authPresent=" + (idToken != null && !idToken.isBlank())
                    + " role=" + (admin == null ? "<none>" : admin.role()) + " locationId=" + location.id()
                    + " name=" + location.name() + " verificationStatus=" + location.verificationStatus() + " active=" + location.active());
        }
        List<String> fields = new ArrayList<>();
        fields.add(fieldJson("locationId", location.id()));
        fields.add(fieldJson("name", location.name()));
        fields.add(fieldJson("type", location.type()));
        addOptionalStringField(fields, "area", location.area());
        addOptionalStringField(fields, "address", location.address());
        fields.add(numberOrNullFieldJson("latitude", location.latitude()));
        fields.add(numberOrNullFieldJson("longitude", location.longitude()));
        addOptionalStringField(fields, "contactNumber", location.phone());
        addOptionalStringField(fields, "landmark", location.landmark());
        addOptionalStringField(fields, "openingHours", location.openingHours());
        fields.add(stringArrayFieldJson("services", location.services()));
        fields.add(fieldJson("verificationStatus", location.verificationStatus()));
        fields.add(boolFieldJson("active", location.active()));
        fields.add(fieldJson("createdAt", location.createdAt()));
        fields.add(fieldJson("updatedAt", location.updatedAt()));
        String json = fieldsJson(fields.toArray(String[]::new));
        try {
            if (create) {
                URI collection = URI.create(collectionUrl("officialHelpLocations") + "&documentId=" + enc(location.id()));
                sendAuthorizedPost(collection, json, idToken);
            } else {
                sendAuthorizedPatch(documentUri("officialHelpLocations", location.id()), json, idToken);
            }
        } catch (IOException error) {
            System.out.println("ADMIN_HELP_CENTER_WRITE statusCode=" + statusCodeFrom(error) + " responseBody=" + safeHelpCenterError(error));
            throw error;
        }
    }

    public void deleteOfficialHelpLocation(String locationId, String idToken) throws IOException, InterruptedException {
        requireVerifiedAdminSession(idToken);
        if (!notBlank(locationId)) throw new IllegalArgumentException("Help center reference is unavailable.");
        URI uri = documentUri("officialHelpLocations", locationId);
        HttpResponse<String> response = client.send(authorizedBuilder(uri, idToken).timeout(Duration.ofSeconds(8)).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            System.out.println("ADMIN_HELP_CENTER_WRITE statusCode=" + response.statusCode() + " responseBody=" + response.body());
            throw firestoreFailure("DELETE", uri, response);
        }
    }

    private void requireVerifiedAdminSession(String idToken) throws IOException, InterruptedException {
        AppSession.User session = AppSession.currentUser();
        if (session == null || !session.isAdmin() || idToken == null || idToken.isBlank()) {
            throw new SecurityException("An authenticated admin session is required.");
        }
        UserProfile profile = loadUserProfile(session.uid(), idToken);
        if (!"admin".equalsIgnoreCase(profile.role())) {
            throw new SecurityException("users/" + session.uid() + " must have role = admin.");
        }
    }

    private OfficialHelpLocation officialHelpLocation(Document document) {
        String f = document.fields;
        Double latitude = numberOrNull(f, "latitude");
        Double longitude = numberOrNull(f, "longitude");
        if (latitude == null || longitude == null) return null;
        return new OfficialHelpLocation(firstNonBlank(field(f, "locationId"), document.id), field(f, "name"), field(f, "type"), field(f, "area"),
                latitude, longitude, field(f, "address"), field(f, "landmark"), firstNonBlank(field(f, "contactNumber"), field(f, "phone")),
                field(f, "openingHours"), stringListField(f, "services"), field(f, "verificationStatus"), "true".equals(boolField(f, "active")),
                field(f, "createdAt"), field(f, "updatedAt"));
    }

    private HelpCenterQueryResult loadVerifiedActiveHelpCenterDocuments(String idToken) throws IOException, InterruptedException {
        String query = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"officialHelpLocations\"}],\"where\":{\"compositeFilter\":{\"op\":\"AND\",\"filters\":["
                + "{\"fieldFilter\":{\"field\":{\"fieldPath\":\"verificationStatus\"},\"op\":\"EQUAL\",\"value\":{\"stringValue\":\"VERIFIED\"}}},"
                + "{\"fieldFilter\":{\"field\":{\"fieldPath\":\"active\"},\"op\":\"EQUAL\",\"value\":{\"booleanValue\":true}}}]}}}}";
        URI endpoint = URI.create(String.format(ROOT, enc(config.projectId())) + ":runQuery?key=" + enc(config.apiKey()));
        HttpResponse<String> response = client.send(authorizedBuilder(endpoint, idToken)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(query))
                .build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) throw new IOException("Firestore help-center query failed: HTTP " + response.statusCode());
        return new HelpCenterQueryResult(response.statusCode(), parseDocuments(response.body()));
    }

    private boolean isUsableOfficialHelpLocation(OfficialHelpLocation location) {
        return location.publiclyVisible() && notBlank(location.name()) && notBlank(location.type())
                && Double.isFinite(location.latitude()) && Double.isFinite(location.longitude())
                && location.latitude() >= -90 && location.latitude() <= 90
                && location.longitude() >= -180 && location.longitude() <= 180;
    }

    private String statusCodeFrom(Exception error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        java.util.regex.Matcher matcher = Pattern.compile("HTTP\\s+(\\d{3})").matcher(message);
        return matcher.find() ? matcher.group(1) : "<unknown>";
    }

    private String safeHelpCenterError(Exception error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message.replaceAll("[\\r\\n]", " ");
    }

    /** Admin-only, idempotent development seed for the official-help-center dataset. */
    public int seedOfficialHelpLocations(String idToken) throws IOException, InterruptedException {
        AppSession.User session = AppSession.currentUser();
        if (session == null || !session.isAdmin()) throw new SecurityException("An admin session is required to seed help centers.");
        int inserted = 0;
        for (OfficialHelpSeed seed : officialHelpSeeds()) {
            if (documentExists("officialHelpLocations", seed.locationId(), idToken)) continue;
            String now = "2026-09-03T09:00:00Z";
            List<String> fields = new ArrayList<>();
            fields.add(fieldJson("locationId", seed.locationId()));
            fields.add(fieldJson("name", seed.name()));
            fields.add(fieldJson("type", "HELP_CENTER"));
            fields.add(fieldJson("area", seed.area()));
            fields.add(fieldJson("address", seed.address()));
            fields.add(numberOrNullFieldJson("latitude", seed.latitude()));
            fields.add(numberOrNullFieldJson("longitude", seed.longitude()));
            fields.add(fieldJson("contactNumber", ""));
            fields.add(stringArrayFieldJson("services", seed.services()));
            fields.add(fieldJson("verificationStatus", "VERIFIED"));
            fields.add(boolFieldJson("active", true));
            fields.add(fieldJson("createdAt", now));
            fields.add(fieldJson("updatedAt", now));
            URI collection = URI.create(collectionUrl("officialHelpLocations") + "&documentId=" + enc(seed.locationId()));
            sendAuthorizedPost(collection, fieldsJson(fields.toArray(String[]::new)), idToken);
            inserted++;
        }
        return inserted;
    }

    private LostFoundReport lostFoundReport(Document d) {
        String f = d.fields;
        String imageUrl = field(f, "imageUrl");
        return new LostFoundReport(d.id, firstNonBlank(field(f, "trackingId"), d.id), field(f, "userId"), field(f, "reportType"), field(f, "category"), field(f, "title"), field(f, "description"), imageUrl.isBlank() ? List.of() : List.of(imageUrl), field(f, "location"), numberOrNull(f, "latitude"), numberOrNull(f, "longitude"), field(f, "landmark"), field(f, "incidentDate"), field(f, "incidentTime"), field(f, "reporterName"), field(f, "reporterPhone"), field(f, "relation"), field(f, "priority"), field(f, "priorityReason"), field(f, "status"), field(f, "verificationStatus"), field(f, "assignedAdminId"), field(f, "assignedAuthorityId"), field(f, "foundLocationId"), field(f, "collectionInstructions"), field(f, "createdAt"), field(f, "updatedAt"), field(f, "resolvedAt"));
    }

    private boolean documentExists(String collection, String documentId, String idToken) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(authorizedBuilder(documentUri(collection, documentId), idToken)
                .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return false;
        if (response.statusCode() >= 400) throw firestoreFailure("GET", documentUri(collection, documentId), response);
        return true;
    }

    private List<LostFoundDemo> lostFoundDemos() {
        String createdAt = "2026-09-03T09:00:00Z";
        return List.of(
                new LostFoundDemo("demo-lf-001", "SC-LF-DEMO-001", "LOST", "Missing Person", "Demo Person One", "Fictional demo missing-person report for administrative testing.", "HIGH", "SUBMITTED", "PENDING", createdAt),
                new LostFoundDemo("demo-lf-002", "SC-LF-DEMO-002", "LOST", "Missing Child", "Demo Child Two", "Fictional demo child report; record is marked found for workflow testing.", "CRITICAL", "FOUND", "VERIFIED", createdAt),
                new LostFoundDemo("demo-lf-003", "SC-LF-DEMO-003", "LOST", "Lost Mobile", "Demo Mobile Three", "Fictional demo lost mobile report.", "NORMAL", "SUBMITTED", "PENDING", createdAt),
                new LostFoundDemo("demo-lf-004", "SC-LF-DEMO-004", "LOST", "Lost Bag / Luggage", "Demo Bag Four", "Fictional demo bag report; record is marked found for workflow testing.", "NORMAL", "FOUND", "VERIFIED", createdAt),
                new LostFoundDemo("demo-lf-005", "SC-LF-DEMO-005", "LOST", "Lost Documents", "Demo Documents Five", "Fictional demo document report.", "MEDIUM", "SUBMITTED", "PENDING", createdAt),
                new LostFoundDemo("demo-lf-006", "SC-LF-DEMO-006", "FOUND", "Found Wallet", "Demo Wallet Six", "Fictional demo found-wallet report awaiting owner verification.", "MEDIUM", "SUBMITTED", "PENDING", createdAt),
                new LostFoundDemo("demo-lf-007", "SC-LF-DEMO-007", "FOUND", "Found Item", "Demo Item Seven", "Fictional demo found-item report resolved for workflow testing.", "NORMAL", "RESOLVED", "VERIFIED", createdAt));
    }

    private List<OfficialHelpSeed> officialHelpSeeds() {
        List<String> services = List.of("Lost & Found Assistance", "Pilgrim Guidance", "Emergency Referral");
        return List.of(
                new OfficialHelpSeed("help-ramkund", "Ramkund Pilgrim Help Center", "Ramkund", "Ramkund area, Panchavati, Nashik, Maharashtra", 20.0064, 73.7904, services),
                new OfficialHelpSeed("help-panchavati", "Panchavati Simhastha Help Center", "Panchavati", "Panchavati area, Nashik, Maharashtra", 20.0103, 73.7960, services),
                new OfficialHelpSeed("help-trimbakeshwar", "Trimbakeshwar Pilgrim Assistance Center", "Trimbakeshwar", "Trimbakeshwar area, Nashik district, Maharashtra", 19.9320, 73.5297, services),
                new OfficialHelpSeed("help-tapovan", "Tapovan Simhastha Help Center", "Tapovan", "Tapovan area, Nashik, Maharashtra", 20.0216, 73.7784, services),
                new OfficialHelpSeed("help-sadhugram", "Sadhugram Pilgrim Help Center", "Sadhugram", "Sadhugram area, Nashik, Maharashtra", 19.9987, 73.7638, services));
    }

    private record LostFoundDemo(String id, String trackingId, String reportType, String category, String title,
            String description, String priority, String status, String verificationStatus, String createdAt) { }
    private record OfficialHelpSeed(String locationId, String name, String area, String address, double latitude,
            double longitude, List<String> services) { }
    private record HelpCenterQueryResult(int statusCode, List<Document> documents) { }

    private void loadPublicCollection(List<AppDataStore.ServiceItem> target, String collection, String module,
            String titleField, String fallbackCategory) throws IOException, InterruptedException {
        List<Document> documents;
        try {
            documents = loadCollectionDocuments(collection, "");
        } catch (IOException exception) {
            return;
        }
        for (Document document : documents) {
            String fields = document.fields;
            String status = field(fields, "status").toLowerCase(Locale.ROOT);
            String published = boolField(fields, "published");
            String approved = boolField(fields, "approved");
            String active = boolField(fields, "active");
            String verified = boolField(fields, "verified");
            String expiresAt = firstNonBlank(field(fields, "expiresAt"), field(fields, "expiryTime"));
            if ("false".equals(published) || "false".equals(active) || "rejected".equals(status)
                    || "suspended".equals(status) || "disabled".equals(status) || "closed".equals(status)) {
                continue;
            }
            if (isExpired(expiresAt)) {
                continue;
            }
            if (!"true".equals(published) && !"true".equals(approved) && !"true".equals(verified)
                    && !"approved".equals(status) && !"active".equals(status)) {
                continue;
            }
            String title = firstNonBlank(field(fields, titleField), field(fields, "title"), field(fields, "businessName"));
            if (!notBlank(title)) {
                continue;
            }
            target.add(new AppDataStore.ServiceItem(document.id, module, title, publicDetail(fields), fallbackCategory));
        }
    }

    private AppDataStore.BookingRecord bookingFrom(Document document) {
        String fields = document.fields;
        return new AppDataStore.BookingRecord(
                valueOr(document.id, field(fields, "bookingId")),
                field(fields, "userId"),
                firstNonBlank(field(fields, "moduleType"), field(fields, "serviceType")),
                firstNonBlank(field(fields, "catalogItemId"), field(fields, "itemId")),
                field(fields, "businessId"),
                firstNonBlank(field(fields, "title"), field(fields, "businessName"), field(fields, "serviceName")),
                firstNonBlank(field(fields, "customerName"), field(fields, "userName")),
                firstNonBlank(field(fields, "bookingDate"), field(fields, "dateText")),
                field(fields, "location"),
                parseInt(firstNonBlank(numberField(fields, "quantity"), numberField(fields, "quantityOrGuests")), 1),
                parseInt(numberField(fields, "nights"), 1),
                parseLong(firstNonBlank(numberField(fields, "amountPaise"), numberField(fields, "amount")), 0),
                firstNonBlank(field(fields, "currency"), "INR"),
                firstNonBlank(field(fields, "bookingStatus"), "PENDING"),
                firstNonBlank(field(fields, "paymentStatus"), "UNPAID"),
                field(fields, "internalPaymentId"),
                field(fields, "razorpayPaymentId"));
    }

    public void updateApprovalRequestStatus(String requestId, String status, String idToken)
            throws IOException, InterruptedException {
        String adminUid = AppSession.currentUser() == null ? "" : AppSession.currentUser().uid();
        String json = fieldsJson(
                fieldJson("status", status),
                fieldJson("reviewedAt", String.valueOf(System.currentTimeMillis())),
                fieldJson("reviewedBy", adminUid));
        sendAuthorizedPatch(URI.create(documentUrl("approvalRequests", requestId)
                        + "&updateMask.fieldPaths=status&updateMask.fieldPaths=reviewedAt&updateMask.fieldPaths=reviewedBy"),
                json, idToken);
    }

    public void saveOperationalItem(String module, AppDataStore.ServiceItem item, String idToken)
            throws IOException, InterruptedException {
        String collection = collectionForModule(module);
        String titleField = titleFieldForModule(module);
        String adminUid = AppSession.currentUser() == null ? "" : AppSession.currentUser().uid();
        List<String> fields = new ArrayList<>();
        fields.add(fieldJson(titleField, item.title));
        fields.add(fieldJson("title", item.title));
        fields.add(fieldJson("description", item.detail));
        fields.add(fieldJson("category", item.category));
        fields.add(fieldJson("status", "active"));
        fields.add(boolFieldJson("published", true));
        fields.add(boolFieldJson("active", true));
        fields.add(boolFieldJson("verified", true));
        fields.add(fieldJson("createdAt", String.valueOf(System.currentTimeMillis())));
        fields.add(fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        fields.add(fieldJson("updatedBy", adminUid));
        fields.add(fieldJson("publishedAt", String.valueOf(System.currentTimeMillis())));
        fields.add(fieldJson("publishedBy", adminUid));
        fields.addAll(moduleFields(module, item.detail));
        String json = fieldsJson(fields.toArray(new String[0]));
        sendAuthorizedPatch(documentUri(collection, item.id), json, idToken);
        saveItem(module, item, idToken);
    }

    private List<String> moduleFields(String module, String detail) {
        List<String> fields = new ArrayList<>();
        switch (module) {
            case "packages" -> {
                fields.add(fieldJson("packageType", detailValue(detail, "Package Type")));
                fields.add(fieldJson("sourceCity", detailValue(detail, "Source City")));
                fields.add(fieldJson("destination", detailValue(detail, "Destination")));
                fields.add(fieldJson("duration", detailValue(detail, "Duration")));
                fields.add(fieldJson("startDate", detailValue(detail, "Start Date")));
                fields.add(fieldJson("endDate", detailValue(detail, "End Date")));
                fields.add(fieldJson("price", detailValue(detail, "Price")));
                fields.add(fieldJson("capacity", detailValue(detail, "Seats / Capacity")));
                fields.add(fieldJson("availableSeats", detailValue(detail, "Seats / Capacity")));
                fields.add(fieldJson("hotelStay", detailValue(detail, "Hotel / Stay")));
                fields.add(fieldJson("itinerary", detailValue(detail, "Itinerary")));
                fields.add(fieldJson("highlights", detailValue(detail, "Highlights")));
            }
            case "puja" -> {
                fields.add(fieldJson("templeGhat", detailValue(detail, "Temple / Ghat")));
                fields.add(fieldJson("pujaType", detailValue(detail, "Puja Type")));
                fields.add(fieldJson("price", detailValue(detail, "Price")));
                fields.add(fieldJson("availableSlots", detailValue(detail, "Available Slots")));
                fields.add(fieldJson("provider", detailValue(detail, "Pandit / Provider")));
                fields.add(fieldJson("verificationStatus", detailValue(detail, "Verification Status")));
                fields.add(fieldJson("bookingStatus", detailValue(detail, "Booking Status")));
            }
            case "ghat" -> {
                fields.add(fieldJson("location", detailValue(detail, "Location")));
                fields.add(fieldJson("mapUrl", detailValue(detail, "Map URL")));
                fields.add(fieldJson("snanDate", detailValue(detail, "Snan Date")));
                fields.add(fieldJson("snanTime", detailValue(detail, "Snan Time")));
                fields.add(fieldJson("crowdLevel", detailValue(detail, "Crowd Level")));
                fields.add(fieldJson("safetyStatus", detailValue(detail, "Safety Status")));
                fields.add(fieldJson("entryStatus", detailValue(detail, "Entry Status")));
                fields.add(fieldJson("exitStatus", detailValue(detail, "Exit Status")));
                fields.add(fieldJson("medicalSupport", detailValue(detail, "Medical Support")));
                fields.add(fieldJson("policeSecurity", detailValue(detail, "Police / Security")));
                fields.add(fieldJson("facilities", detailValue(detail, "Facilities")));
                fields.add(fieldJson("instructions", detailValue(detail, "Instructions")));
            }
            case "stay" -> {
                fields.add(fieldJson("type", detailValue(detail, "Type")));
                fields.add(fieldJson("owner", detailValue(detail, "Owner")));
                fields.add(fieldJson("location", detailValue(detail, "Location")));
                fields.add(fieldJson("roomsUnits", detailValue(detail, "Rooms / Units")));
                fields.add(fieldJson("available", detailValue(detail, "Available")));
                fields.add(fieldJson("price", detailValue(detail, "Price")));
            }
            case "schedule" -> {
                fields.add(fieldJson("eventType", detailValue(detail, "Type")));
                fields.add(fieldJson("date", detailValue(detail, "Date")));
                fields.add(fieldJson("start", detailValue(detail, "Start")));
                fields.add(fieldJson("end", detailValue(detail, "End")));
                fields.add(fieldJson("location", detailValue(detail, "Location")));
            }
            case "announcement" -> {
                fields.add(fieldJson("message", detailValue(detail, "Message")));
                fields.add(fieldJson("priority", detailValue(detail, "Priority")));
                fields.add(fieldJson("location", detailValue(detail, "Location optional")));
                fields.add(fieldJson("startAt", detailValue(detail, "Start Time")));
                fields.add(fieldJson("expiresAt", detailValue(detail, "Expiry Time")));
            }
            case "emergency" -> {
                fields.add(fieldJson("type", detailValue(detail, "Type")));
                fields.add(fieldJson("phone", detailValue(detail, "Phone")));
                fields.add(fieldJson("location", detailValue(detail, "Location")));
                fields.add(fieldJson("mapLink", detailValue(detail, "Map Link")));
                fields.add(fieldJson("availability", detailValue(detail, "Availability")));
                fields.add(fieldJson("priority", detailValue(detail, "Priority")));
            }
            default -> {
            }
        }
        return fields;
    }

    private String detailValue(String detail, String label) {
        String prefix = label + ": ";
        for (String part : (detail == null ? "" : detail).split("\\|")) {
            String clean = part.trim();
            if (clean.startsWith(prefix)) {
                return clean.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    public AppDataStore.AdminOverview loadAdminOverview(String idToken) throws IOException, InterruptedException {
        List<Document> users = loadCollectionDocuments("users", idToken);
        List<Document> businesses = loadOptionalCollectionDocuments("businesses", idToken);
        List<Document> operators = loadOptionalCollectionDocuments("transportOperators", idToken);
        List<Document> appItems = loadOptionalCollectionDocuments("appItems", idToken);
        List<Document> routes = loadOptionalCollectionDocuments("transportRoutes", idToken);
        List<Document> bookings = loadOptionalCollectionDocuments("bookings", idToken);
        List<Document> lostFoundReports = loadOptionalCollectionDocuments("lostFoundReports", idToken);

        int approvedBusinesses = countMatching(businesses, document ->
                "approved".equals(field(document.fields, "status")) || "true".equals(boolField(document.fields, "approved")));
        int pendingBusinesses = countMatching(businesses, document ->
                "pending".equals(field(document.fields, "status")) || "false".equals(boolField(document.fields, "approved")));
        int activeOperators = countMatching(operators, document ->
                !"rejected".equals(field(document.fields, "status"))
                        && !"disabled".equals(field(document.fields, "status"))
                        && !"suspended".equals(field(document.fields, "status")));
        int activeRoutes = Math.max(countModuleItems(appItems, "transport"),
                countMatching(routes, document -> "true".equals(boolField(document.fields, "published"))
                        && !"false".equals(boolField(document.fields, "active"))));
        int activeEvents = countModuleItems(appItems, "schedule");
        int activeAnnouncements = countModuleItems(appItems, "announcement");
        int openLostFound = lostFoundReports.isEmpty()
                ? countModuleItems(appItems, "lost")
                : countMatching(lostFoundReports, document -> {
                    String status = field(document.fields, "status");
                    return status.isBlank() || "open".equals(status) || "pending".equals(status);
                });

        return new AppDataStore.AdminOverview(
                users.size(),
                approvedBusinesses,
                pendingBusinesses,
                activeOperators,
                activeRoutes,
                bookings.size(),
                activeEvents,
                openLostFound,
                activeAnnouncements,
                true,
                "");
    }

    public UserProfile loadUserProfile(String uid, String idToken) throws IOException, InterruptedException {
        if (!notBlank(uid)) {
            throw new MalformedProfileException("Firebase authentication succeeded but did not return a UID.");
        }
        HttpRequest request = authorizedBuilder(documentUri("users", uid), idToken)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new PermissionDeniedException("Firestore permission denied while reading users/" + uid + ".");
        }
        if (response.statusCode() >= 400) {
            throw new IOException("Firestore profile read failed: " + response.statusCode());
        }
        String body = response.body();
        String profileUid = valueOr(uid, field(body, "uid")).trim();
        String name = field(body, "name").trim();
        String email = field(body, "email").trim();
        String mobile = field(body, "mobile").trim();
        String role = field(body, "role").trim();
        String status = field(body, "status").trim();
        if (!notBlank(role) && !notBlank(status)) {
            throw new MalformedProfileException("Firestore profile at users/" + uid + " is missing role and status.");
        }
        if (!notBlank(role)) {
            throw new MalformedProfileException("Firestore profile at users/" + uid + " is missing role.");
        }
        if (!notBlank(status)) {
            throw new MalformedProfileException("Firestore profile at users/" + uid + " is missing status.");
        }
        return new UserProfile(
                profileUid,
                name,
                email,
                mobile,
                role,
                status);
    }

    public void saveUserProfile(UserProfile profile, String idToken) throws IOException, InterruptedException {
        String json = fieldsJson(
                fieldJson("uid", profile.uid()),
                fieldJson("name", profile.name()),
                fieldJson("email", profile.email()),
                fieldJson("mobile", profile.mobile()),
                fieldJson("role", profile.role()),
                fieldJson("status", profile.status()),
                fieldJson("createdAt", String.valueOf(System.currentTimeMillis())),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(documentUri("users", profile.uid()), json, idToken);
    }

    public void saveBusinessProfile(String uid, BusinessAuthPage.BusinessAccount account, String idToken)
            throws IOException, InterruptedException {
        String json = fieldsJson(
                fieldJson("ownerId", uid),
                fieldJson("businessName", account.businessName),
                fieldJson("ownerName", account.ownerName),
                fieldJson("category", account.category),
                fieldJson("mobile", account.mobile),
                fieldJson("email", account.email),
                fieldJson("location", account.location),
                fieldJson("description", account.category + " service for Simhastha pilgrims"),
                fieldJson("operatingHours", "Not provided"),
                fieldJson("priceRange", "Not provided"),
                fieldJson("status", "pending"),
                boolFieldJson("approved", false),
                fieldJson("createdAt", String.valueOf(System.currentTimeMillis())),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(documentUri("businesses", uid), json, idToken);
    }

    /**
     * Business registrations are stored under the authenticated owner's UID.  Keeping
     * this lookup server-side of the UI prevents an owner from choosing another
     * business document by id.
     */
    public BusinessProfile loadBusinessForOwner(String ownerId, String idToken) throws IOException, InterruptedException {
        if (!notBlank(ownerId)) {
            return null;
        }
        HttpRequest request = authorizedBuilder(documentUri("businesses", ownerId), idToken)
                .timeout(Duration.ofSeconds(8)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() >= 400) {
            throw new IOException("Business profile read failed: " + response.statusCode());
        }
        String fields = extractFieldsObject(response.body());
        String resolvedOwnerId = valueOr(ownerId, field(fields, "ownerId"));
        return new BusinessProfile(ownerId, resolvedOwnerId, field(fields, "businessName"), field(fields, "ownerName"),
                field(fields, "category"), field(fields, "location"), field(fields, "description"),
                field(fields, "status"), boolField(fields, "approved"));
    }

    public List<BusinessInventoryItem> loadBusinessItems(String businessId, String ownerId, String idToken)
            throws IOException, InterruptedException {
        // Use an owner-constrained Firestore query.  Listing the entire collection
        // would be rejected by the owner-scoped security rule and could leak data.
        String query = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"businessItems\"}],"
                + "\"where\":{\"fieldFilter\":{\"field\":{\"fieldPath\":\"ownerId\"},"
                + "\"op\":\"EQUAL\",\"value\":{\"stringValue\":\"" + escape(ownerId) + "\"}}}}}";
        String json = post(URI.create(String.format(ROOT, enc(config.projectId())) + ":runQuery?key=" + enc(config.apiKey())), query, idToken);
        List<BusinessInventoryItem> result = new ArrayList<>();
        for (Document document : parseDocuments(json)) {
            String fields = document.fields;
            if (!businessId.equals(field(fields, "businessId")) || !ownerId.equals(field(fields, "ownerId"))) {
                continue;
            }
            result.add(new BusinessInventoryItem(document.id, field(fields, "businessId"), field(fields, "ownerId"),
                    field(fields, "category"), field(fields, "itemType"), field(fields, "name"),
                    field(fields, "description"), field(fields, "price"), field(fields, "capacity"),
                    field(fields, "totalUnits"), field(fields, "availableUnits"), field(fields, "stock"),
                    field(fields, "facilities"), field(fields, "availability"),
                    "true".equalsIgnoreCase(boolField(fields, "active"))));
        }
        return result;
    }

    public void saveBusinessItem(BusinessInventoryItem item, String idToken) throws IOException, InterruptedException {
        String json = fieldsJson(fieldJson("businessId", item.businessId()), fieldJson("ownerId", item.ownerId()),
                fieldJson("category", item.category()), fieldJson("itemType", item.itemType()), fieldJson("name", item.name()),
                fieldJson("description", item.description()), fieldJson("price", item.price()), fieldJson("capacity", item.capacity()),
                fieldJson("totalUnits", item.totalUnits()), fieldJson("availableUnits", item.availableUnits()),
                fieldJson("stock", item.stock()), fieldJson("facilities", item.facilities()), fieldJson("availability", item.availability()),
                boolFieldJson("active", item.active()), fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(documentUri("businessItems", item.itemId()), json, idToken);
    }

    public void saveTransportOperatorProfile(String uid, OperatorAuthPage.OperatorAccount account, String idToken)
            throws IOException, InterruptedException {
        String json = fieldsJson(
                fieldJson("ownerId", uid),
                fieldJson("organizationName", account.organizationName),
                fieldJson("contactPerson", account.contactPerson),
                fieldJson("mobile", account.mobile),
                fieldJson("email", account.email),
                fieldJson("serviceType", account.serviceType),
                fieldJson("status", "active"),
                boolFieldJson("approved", true),
                fieldJson("createdAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(documentUri("transportOperators", uid), json, idToken);
    }

    public void updateUserStatus(String uid, String status, String idToken) throws IOException, InterruptedException {
        String json = fieldsJson(
                fieldJson("status", status),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(URI.create(documentUrl("users", uid)
                + "&updateMask.fieldPaths=status&updateMask.fieldPaths=updatedAt"), json, idToken);
    }

    public void updateDocumentStatus(String collection, String documentId, String status, boolean approved,
            String idToken) throws IOException, InterruptedException {
        String adminUid = AppSession.currentUser() == null ? "" : AppSession.currentUser().uid();
        String json = fieldsJson(
                fieldJson("status", status),
                boolFieldJson("approved", approved),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())),
                fieldJson(approved ? "approvedAt" : "reviewedAt", String.valueOf(System.currentTimeMillis())),
                fieldJson(approved ? "approvedBy" : "reviewedBy", adminUid));
        sendAuthorizedPatch(URI.create(documentUrl(collection, documentId)
                        + "&updateMask.fieldPaths=status&updateMask.fieldPaths=approved&updateMask.fieldPaths=updatedAt"
                        + "&updateMask.fieldPaths=" + (approved ? "approvedAt" : "reviewedAt")
                        + "&updateMask.fieldPaths=" + (approved ? "approvedBy" : "reviewedBy")),
                json, idToken);
    }

    public void updateBookingStatus(String bookingId, String bookingStatus, String paymentStatus, String idToken)
            throws IOException, InterruptedException {
        String json = fieldsJson(
                fieldJson("bookingStatus", bookingStatus),
                fieldJson("paymentStatus", paymentStatus),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(URI.create(documentUrl("bookings", bookingId)
                        + "&updateMask.fieldPaths=bookingStatus&updateMask.fieldPaths=paymentStatus&updateMask.fieldPaths=updatedAt"),
                json, idToken);
    }

    public void createBooking(AppDataStore.BookingRecord booking, String businessOwnerId, String idToken)
            throws IOException, InterruptedException {
        String json = fieldsJson(
                fieldJson("bookingId", booking.bookingId),
                fieldJson("userId", booking.userId),
                fieldJson("moduleType", booking.moduleType),
                fieldJson("catalogItemId", booking.catalogItemId),
                fieldJson("businessId", booking.businessId),
                fieldJson("businessOwnerId", businessOwnerId),
                fieldJson("title", booking.title),
                fieldJson("customerName", booking.customerName),
                fieldJson("bookingDate", booking.dateText),
                fieldJson("location", booking.location),
                numberFieldJson("quantity", booking.quantity),
                numberFieldJson("nights", booking.nights),
                numberFieldJson("amountPaise", booking.amountPaise),
                fieldJson("currency", booking.currency),
                fieldJson("bookingStatus", booking.bookingStatus),
                fieldJson("paymentStatus", booking.paymentStatus),
                fieldJson("internalPaymentId", booking.internalPaymentId),
                fieldJson("razorpayPaymentId", booking.razorpayPaymentId),
                fieldJson("createdAt", booking.createdAt),
                fieldJson("updatedAt", booking.updatedAt));
        AppSession.User user = AppSession.currentUser();
        System.out.println("BOOKING CREATE: tokenPresent=" + (idToken != null && !idToken.isBlank())
                + ", uid=" + (user == null ? "<none>" : user.uid())
                + ", role=" + (user == null ? "<none>" : user.role())
                + ", bookingUserId=" + booking.userId + ", module=" + booking.moduleType
                + ", bookingId=" + booking.bookingId + ", method=POST");
        URI collection = URI.create(collectionUrl("bookings") + "&documentId=" + enc(booking.bookingId));
        sendAuthorizedPost(collection, json, idToken);
    }

    public void saveTransportRoute(AppDataStore.RouteRecord route, String idToken)
            throws IOException, InterruptedException {
        String adminUid = AppSession.currentUser() == null ? "" : AppSession.currentUser().uid();
        String now = String.valueOf(System.currentTimeMillis());
        String json = fieldsJson(
                fieldJson("routeName", route.routeName),
                fieldJson("from", route.from),
                fieldJson("to", route.to),
                fieldJson("via", route.via),
                fieldJson("mode", route.mode),
                fieldJson("startTime", route.startTime),
                fieldJson("endTime", route.endTime),
                fieldJson("fare", route.fare),
                fieldJson("duration", route.duration),
                fieldJson("operatorId", route.operatorId),
                boolFieldJson("official", route.official),
                fieldJson("mapUrl", route.mapUrl),
                fieldJson("liveSourceUrl", route.liveSourceUrl),
                boolFieldJson("published", route.published),
                boolFieldJson("active", route.active),
                fieldJson("status", route.active ? "active" : "disabled"),
                fieldJson("createdAt", valueOr(now, route.createdAt)),
                fieldJson("updatedAt", now),
                fieldJson("updatedBy", adminUid));
        sendAuthorizedPatch(documentUri("transportRoutes", route.routeId), json, idToken);
    }

    public void updateRouteFlags(String routeId, boolean published, boolean active, String idToken)
            throws IOException, InterruptedException {
        String json = fieldsJson(
                boolFieldJson("published", published),
                boolFieldJson("active", active),
                fieldJson("status", active ? "active" : "disabled"),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(URI.create(documentUrl("transportRoutes", routeId)
                        + "&updateMask.fieldPaths=published&updateMask.fieldPaths=active"
                        + "&updateMask.fieldPaths=status&updateMask.fieldPaths=updatedAt"),
                json, idToken);
    }

    public void updateOperationalItemFlags(String module, String documentId, boolean published, boolean active,
            String idToken) throws IOException, InterruptedException {
        String json = fieldsJson(
                boolFieldJson("published", published),
                boolFieldJson("active", active),
                fieldJson("status", active ? "active" : "disabled"),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(URI.create(documentUrl(collectionForModule(module), documentId)
                        + "&updateMask.fieldPaths=published&updateMask.fieldPaths=active"
                        + "&updateMask.fieldPaths=status&updateMask.fieldPaths=updatedAt"),
                json, idToken);
    }

    public void updateLostFoundStatus(String caseId, String status, String note, String idToken)
            throws IOException, InterruptedException {
        String adminUid = AppSession.currentUser() == null ? "" : AppSession.currentUser().uid();
        String now = String.valueOf(System.currentTimeMillis());
        String json = fieldsJson(
                fieldJson("status", status),
                fieldJson("adminNote", note),
                fieldJson("foundAt", ("found".equals(status) || "reunited".equals(status)) ? now : ""),
                fieldJson("updatedAt", now),
                fieldJson("updatedBy", adminUid));
        sendAuthorizedPatch(URI.create(documentUrl("lostFoundReports", caseId)
                        + "&updateMask.fieldPaths=status&updateMask.fieldPaths=adminNote"
                        + "&updateMask.fieldPaths=foundAt&updateMask.fieldPaths=updatedAt&updateMask.fieldPaths=updatedBy"),
                json, idToken);
    }

    /** Owner-safe status update: it intentionally avoids admin-only fields. */
    public void markLostFoundReportFoundByOwner(String reportId, String idToken)
            throws IOException, InterruptedException {
        String json = fieldsJson(fieldJson("status", "FOUND"),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(URI.create(documentUrl("lostFoundReports", reportId)
                        + "&updateMask.fieldPaths=status&updateMask.fieldPaths=updatedAt"), json, idToken);
    }

    public void saveItem(String module, AppDataStore.ServiceItem item) {
        patch(documentUrl("appItems", item.id), itemJson(module, item));
    }

    public void saveItem(String module, AppDataStore.ServiceItem item, String idToken) throws IOException, InterruptedException {
        sendAuthorizedPatch(documentUri("appItems", item.id), itemJson(module, item), idToken);
    }

    public void deleteItem(AppDataStore.ServiceItem item) {
        delete(documentUrl("appItems", item.id));
    }

    public void deleteItem(AppDataStore.ServiceItem item, String idToken) {
        delete(documentUrl("appItems", item.id), idToken);
    }

    public void saveApproval(AppDataStore.ApprovalRequest request) {
        patch(documentUrl("approvalRequests", request.id), approvalJson(request));
    }

    public void saveApproval(AppDataStore.ApprovalRequest request, String idToken) throws IOException, InterruptedException {
        sendAuthorizedPatch(documentUri("approvalRequests", request.id), approvalJson(request), idToken);
    }

    public void deleteApproval(AppDataStore.ApprovalRequest request) {
        delete(documentUrl("approvalRequests", request.id));
    }

    public void deleteApproval(AppDataStore.ApprovalRequest request, String idToken) {
        delete(documentUrl("approvalRequests", request.id), idToken);
    }

    private String get(String url) throws IOException, InterruptedException {
        return get(url, "");
    }

    private String get(String url, String idToken) throws IOException, InterruptedException {
        HttpRequest request = authorizedBuilder(URI.create(url), idToken)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Firestore read failed: " + response.statusCode());
        }
        return response.body();
    }

    private String post(URI uri, String json, String idToken) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(authorizedBuilder(uri, idToken)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Firestore query failed: " + response.statusCode());
        }
        return response.body();
    }

    private List<Document> loadCollectionDocuments(String collection, String idToken)
            throws IOException, InterruptedException {
        try {
            return parseDocuments(get(collectionUrl(collection), idToken));
        } catch (IOException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage();
            if (message.contains("404")) {
                return List.of();
            }
            throw exception;
        }
    }

    private List<Document> loadQueryDocuments(String collection, String fieldName, String value, String idToken)
            throws IOException, InterruptedException {
        String query = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"" + escape(collection) + "\"}],"
                + "\"where\":{\"fieldFilter\":{\"field\":{\"fieldPath\":\"" + escape(fieldName) + "\"},"
                + "\"op\":\"EQUAL\",\"value\":{\"stringValue\":\"" + escape(value) + "\"}}}}}";
        String url = String.format(ROOT, enc(config.projectId())) + ":runQuery?key=" + enc(config.apiKey());
        URI endpoint = URI.create(url);
        String json = post(endpoint, query, idToken);
        return parseDocuments(json);
    }

    private void patch(String url, String json) {
        sendWithoutBlocking(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    private void delete(String url) {
        delete(url, "");
    }

    private void delete(String url, String idToken) {
        sendWithoutBlocking(authorizedBuilder(URI.create(url), idToken)
                .timeout(Duration.ofSeconds(8))
                .DELETE()
                .build());
    }

    private void sendWithoutBlocking(HttpRequest request) {
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private void sendAuthorizedPatch(URI uri, String json, String idToken) throws IOException, InterruptedException {
        HttpRequest request = authorizedBuilder(uri, idToken)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP STATUS = " + response.statusCode());
        if (response.statusCode() >= 400) {
            throw firestoreFailure("PATCH", uri, response);
        }
    }

    private IOException firestoreFailure(String method, URI uri, HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body();
        System.out.println("FIREBASE DOCUMENT PATH = " + uri.getPath());
        System.out.println("FIREBASE RESPONSE BODY = " + body);
        return new IOException("Firestore " + method + " failed: HTTP " + response.statusCode() + " body=" + body);
    }

    private HttpRequest.Builder authorizedBuilder(URI uri, String idToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        if (idToken != null && !idToken.isBlank()) {
            builder.header("Authorization", "Bearer " + idToken);
        }
        return builder;
    }

    private String collectionUrl(String collection) {
        return String.format(ROOT, enc(config.projectId())) + "/" + collection + "?key=" + enc(config.apiKey());
    }

    private String documentUrl(String collection, String documentId) {
        return String.format(ROOT, enc(config.projectId())) + "/" + collection + "/" + enc(documentId)
                + "?key=" + enc(config.apiKey());
    }

    private URI documentUri(String collection, String documentId) {
        return URI.create(documentUrl(collection, documentId));
    }

    private String itemJson(String module, AppDataStore.ServiceItem item) {
        return fieldsJson(
                fieldJson("module", module),
                fieldJson("title", item.title),
                fieldJson("detail", item.detail),
                fieldJson("category", item.category));
    }

    private String approvalJson(AppDataStore.ApprovalRequest request) {
        return fieldsJson(
                fieldJson("type", request.type),
                fieldJson("title", request.title),
                fieldJson("detail", request.detail),
                fieldJson("targetModule", request.targetModule),
                fieldJson("ownerId", request.ownerId),
                fieldJson("status", "pending"),
                fieldJson("createdAt", String.valueOf(System.currentTimeMillis())));
    }

    private String fieldsJson(String... fields) {
        return "{\"fields\":{" + String.join(",", fields) + "}}";
    }

    private String fieldJson(String name, String value) {
        return "\"" + escape(name) + "\":{\"stringValue\":\"" + escape(value == null ? "" : value) + "\"}";
    }

    private String boolFieldJson(String name, boolean value) {
        return "\"" + escape(name) + "\":{\"booleanValue\":" + value + "}";
    }

    private String numberFieldJson(String name, long value) {
        return "\"" + escape(name) + "\":{\"integerValue\":\"" + value + "\"}";
    }

    private String numberOrNullFieldJson(String name, Number value) {
        return value == null ? "\"" + escape(name) + "\":{\"nullValue\":null}"
                : "\"" + escape(name) + "\":{\"doubleValue\":" + value + "}";
    }

    private String stringArrayFieldJson(String name, List<String> values) {
        String entries = (values == null ? List.<String>of() : values).stream()
                .map(value -> "{\"stringValue\":\"" + escape(value) + "\"}").collect(java.util.stream.Collectors.joining(","));
        return "\"" + escape(name) + "\":{\"arrayValue\":{\"values\":[" + entries + "]}}";
    }

    private List<Document> parseDocuments(String json) {
        List<Document> documents = new ArrayList<>();
        Matcher matcher = DOCUMENT_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            String name = unescape(matcher.group(1));
            String id = name.substring(name.lastIndexOf('/') + 1);
            documents.add(new Document(id, matcher.group(2)));
        }
        return documents;
    }

    private String field(String fieldsJson, String name) {
        Matcher matcher = Pattern.compile(String.format(STRING_FIELD_PATTERN.pattern(), Pattern.quote(name)), Pattern.DOTALL)
                .matcher(fieldsJson == null ? "" : fieldsJson);
        if (!matcher.find()) {
            return "";
        }
        return unescape(matcher.group(1));
    }

    private String boolField(String fieldsJson, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name)
                + "\"\\s*:\\s*\\{\\s*\"booleanValue\"\\s*:\\s*(true|false)\\s*\\}", Pattern.DOTALL)
                .matcher(fieldsJson == null ? "" : fieldsJson);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String numberField(String fieldsJson, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name)
                + "\"\\s*:\\s*\\{\\s*\"(?:integerValue|doubleValue)\"\\s*:\\s*\"?(.*?)\"?\\s*\\}", Pattern.DOTALL)
                .matcher(fieldsJson == null ? "" : fieldsJson);
        return matcher.find() ? matcher.group(1) : "";
    }

    private void addOptionalStringField(List<String> fields, String name, String value) {
        if (value != null && !value.isBlank()) fields.add(fieldJson(name, value));
    }

    private void sendAuthorizedPost(URI uri, String json, String idToken) throws IOException, InterruptedException {
        HttpRequest request = authorizedBuilder(uri, idToken)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP STATUS = " + response.statusCode());
        if (response.statusCode() >= 400) throw firestoreFailure("POST", uri, response);
    }

    private Double numberOrNull(String fieldsJson, String name) {
        try { return Double.valueOf(numberField(fieldsJson, name)); } catch (RuntimeException ignored) { return null; }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String safeDiagnostic(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception exception) {
            return fallback;
        }
    }

    /** Query constraint is required because Firestore rules must be able to prove every returned ghat is public. */
    private List<Document> loadPublishedGhatDocuments(String idToken) throws IOException, InterruptedException {
        String query = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"ghats\"}],\"where\":{\"compositeFilter\":{\"op\":\"AND\",\"filters\":["
                + "{\"fieldFilter\":{\"field\":{\"fieldPath\":\"published\"},\"op\":\"EQUAL\",\"value\":{\"booleanValue\":true}}},"
                + "{\"fieldFilter\":{\"field\":{\"fieldPath\":\"active\"},\"op\":\"EQUAL\",\"value\":{\"booleanValue\":true}}}]}}}}";
        String json = post(URI.create(String.format(ROOT, enc(config.projectId())) + ":runQuery?key=" + enc(config.apiKey())),
                query, idToken);
        return parseDocuments(json);
    }

    private Integer integerField(String fieldsJson, String name) {
        String value = numberField(fieldsJson, name);
        if (!notBlank(value)) return null;
        try { return Integer.valueOf(value.trim()); } catch (NumberFormatException exception) { return null; }
    }

    private Double decimalField(String fieldsJson, String name) {
        String value = numberField(fieldsJson, name);
        if (!notBlank(value)) return null;
        try { return Double.valueOf(value.trim()); } catch (NumberFormatException exception) { return null; }
    }

    private String timestampField(String fieldsJson, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name)
                + "\"\\s*:\\s*\\{\\s*\"timestampValue\"\\s*:\\s*\"(.*?)\"\\s*\\}", Pattern.DOTALL)
                .matcher(fieldsJson == null ? "" : fieldsJson);
        return matcher.find() ? unescape(matcher.group(1)) : "";
    }

    private List<String> stringListField(String fieldsJson, String name) {
        String plainValue = field(fieldsJson, name);
        if (notBlank(plainValue)) return splitValues(plainValue);
        Matcher fieldMatcher = Pattern.compile("\"" + Pattern.quote(name)
                + "\"\\s*:\\s*\\{\\s*\"arrayValue\"\\s*:\\s*\\{\\s*\"values\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL)
                .matcher(fieldsJson == null ? "" : fieldsJson);
        if (!fieldMatcher.find()) return List.of();
        Matcher values = Pattern.compile("\"stringValue\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL).matcher(fieldMatcher.group(1));
        List<String> result = new ArrayList<>();
        while (values.find()) { String value = unescape(values.group(1)).trim(); if (!value.isBlank()) result.add(value); }
        return result;
    }

    private List<String> splitValues(String value) {
        if (!notBlank(value)) return List.of();
        return java.util.Arrays.stream(value.split("[,|]"))
                .map(String::trim).filter(this::notBlank).toList();
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        if (!notBlank(value)) return fallback;
        try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_')); }
        catch (IllegalArgumentException exception) { return fallback; }
    }

    private List<Document> loadOptionalCollectionDocuments(String collection, String idToken) {
        try {
            return loadCollectionDocuments(collection, idToken);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private boolean isExpired(String value) {
        if (!notBlank(value)) {
            return false;
        }
        try {
            return Long.parseLong(value.trim()) < System.currentTimeMillis();
        } catch (Exception exception) {
            return false;
        }
    }

    private String operatingHours(String fields) {
        String open = field(fields, "openingTime");
        String close = field(fields, "closingTime");
        if (notBlank(open) || notBlank(close)) {
            return valueOr("Open", open) + " - " + valueOr("Close", close);
        }
        return "";
    }

    private String publicDetail(String fields) {
        return java.util.stream.Stream.of(
                field(fields, "description"),
                field(fields, "location"),
                field(fields, "date"),
                field(fields, "time"),
                field(fields, "price"),
                field(fields, "fare"),
                field(fields, "crowdLevel"),
                field(fields, "safetyStatus"),
                field(fields, "message"),
                field(fields, "phone"))
                .filter(this::notBlank)
                .distinct()
                .reduce((left, right) -> left + " | " + right)
                .orElse("Official update");
    }

    private String collectionForModule(String module) {
        return switch (module) {
            case "transport" -> "transportRoutes";
            case "packages" -> "kumbhPackages";
            case "puja" -> "pujaServices";
            case "ghat" -> "ghats";
            case "stay" -> "stays";
            case "schedule" -> "events";
            case "announcement" -> "announcements";
            case "emergency" -> "emergencyContacts";
            case "lost" -> "lostFoundReports";
            default -> "appItems";
        };
    }

    private String titleFieldForModule(String module) {
        return switch (module) {
            case "transport" -> "routeName";
            case "packages" -> "packageName";
            case "puja" -> "pujaName";
            case "ghat" -> "ghatName";
            case "stay" -> "property";
            case "schedule" -> "eventName";
            case "emergency" -> "name";
            default -> "title";
        };
    }

    private String ownerIdFromBusinessDocument(Document document) {
        String ownerId = field(document.fields, "ownerId");
        if (notBlank(ownerId)) {
            return ownerId.trim();
        }
        return looksLikeFirebaseUid(document.id) ? document.id.trim() : "";
    }

    private boolean looksLikeFirebaseUid(String value) {
        return notBlank(value) && !value.contains("/") && value.length() >= 8;
    }

    private int countModuleItems(List<Document> documents, String module) {
        return countMatching(documents, document -> module.equals(field(document.fields, "module")));
    }

    private int countMatching(List<Document> documents, java.util.function.Predicate<Document> predicate) {
        int count = 0;
        for (Document document : documents) {
            if (predicate.test(document)) {
                count++;
            }
        }
        return count;
    }

    private String extractFieldsObject(String json) {
        String source = json == null ? "" : json;
        Matcher matcher = Pattern.compile("\"fields\"\\s*:\\s*\\{", Pattern.DOTALL).matcher(source);
        if (!matcher.find()) {
            return "";
        }
        int start = matcher.end();
        int depth = 1;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < source.length(); index++) {
            char current = source.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, index);
                }
            }
        }
        return "";
    }

    private String valueOr(String fallback, String value) {
        return notBlank(value) ? value : fallback;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private record Document(String id, String fields) {
    }

    public record UserProfile(String uid, String name, String email, String mobile, String role, String status) {
    }

    public record BusinessProfile(String businessId, String ownerId, String businessName, String ownerName,
            String category, String location, String description, String status, String approved) {
    }

    public record BusinessInventoryItem(String itemId, String businessId, String ownerId, String category,
            String itemType, String name, String description, String price, String capacity, String totalUnits,
            String availableUnits, String stock, String facilities, String availability, boolean active) {
    }

    public static class PermissionDeniedException extends IOException {
        public PermissionDeniedException(String message) {
            super(message);
        }
    }

    public static class MalformedProfileException extends IOException {
        public MalformedProfileException(String message) {
            super(message);
        }
    }
}
