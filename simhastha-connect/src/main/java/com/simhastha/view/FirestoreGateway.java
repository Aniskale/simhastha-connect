package com.simhastha.view;

import com.simhastha.packages.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public final class FirestoreGateway {

    private static final String ROOT = "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents";
    private static final Logger LOGGER = Logger.getLogger(FirestoreGateway.class.getName());
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\".*?\"fields\"\\s*:\\s*\\{(.*?)\\}\\s*(?:,\\s*\"createTime\"|,\\s*\"updateTime\"|\\})", Pattern.DOTALL);
    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\\{\\s*\"stringValue\"\\s*:\\s*\"(.*?)\"\\s*\\}", Pattern.DOTALL);

    private final FirebaseConfig config;
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
                    firstNonBlank(field(fields, "name"), field(fields, "itemName")),
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
                    field(fields, "updatedAt")));
        }
        return records;
    }

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

    private PackageBooking packageBookingFrom(Document document) {
        String fields = document.fields;
        PrimaryContact contact = new PrimaryContact(field(fields, "primaryContactName"),
                field(fields, "primaryContactMobile"), field(fields, "primaryContactEmail"));
        return new PackageBooking(valueOr(document.id, field(fields, "bookingId")), field(fields, "userId"),
                field(fields, "packageId"), field(fields, "packageName"), field(fields, "route"), field(fields, "duration"),
                contact, decodeTravellers(field(fields, "travellers")), decodeStringMap(field(fields, "selections")),
                decodeIntegerMap(field(fields, "componentPrices")), parseInt(numberField(fields, "baseAmount"), 0),
                parseInt(numberField(fields, "finalAmount"), 0), firstNonBlank(field(fields, "currency"), "INR"),
                field(fields, "paymentMode"), field(fields, "paymentStatus"), field(fields, "bookingStatus"),
                firstNonBlank(field(fields, "applicationReference"), field(fields, "bookingId")), field(fields, "createdAt"));
    }

    private String encodeStringMap(Map<String, String> values) {
        return values.entrySet().stream().map(entry -> encode(entry.getKey()) + ":" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private Map<String, String> decodeStringMap(String value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value == null || value.isBlank()) return result;
        for (String entry : value.split(";")) { String[] pair = entry.split(":", 2); if (pair.length == 2) result.put(decode(pair[0]), decode(pair[1])); }
        return result;
    }

    private String encodeIntegerMap(Map<String, Integer> values) {
        return values.entrySet().stream().map(entry -> encode(entry.getKey()) + ":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private Map<String, Integer> decodeIntegerMap(String value) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (value == null || value.isBlank()) return result;
        for (String entry : value.split(";")) { String[] pair = entry.split(":", 2); if (pair.length == 2) result.put(decode(pair[0]), parseInt(pair[1], 0)); }
        return result;
    }

    private String encodeTravellers(List<PackageBooking.BookingTraveller> travellers) {
        return travellers.stream().map(traveller -> String.join(":", encode(traveller.fullName()), Integer.toString(traveller.age()),
                encode(traveller.gender()), encode(traveller.idType()), encode(traveller.maskedId())))
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private List<PackageBooking.BookingTraveller> decodeTravellers(String value) {
        List<PackageBooking.BookingTraveller> result = new ArrayList<>();
        if (value == null || value.isBlank()) return result;
        for (String entry : value.split(";")) { String[] parts = entry.split(":", -1); if (parts.length == 5) result.add(new PackageBooking.BookingTraveller(decode(parts[0]), parseInt(parts[1], 0), decode(parts[2]), decode(parts[3]), decode(parts[4]))); }
        return result;
    }

    private String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)); }
    private String decode(String value) { try { return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); } catch (IllegalArgumentException ignored) { return ""; } }

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

    public void saveBooking(AppDataStore.BookingRecord booking, String businessOwnerId, String idToken)
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
        sendAuthorizedPatch(documentUri("bookings", booking.bookingId), json, idToken);
    }

    /** Kumbh-package confirmations have their own collection and never share the generic bookings schema. */
    public void savePackageBooking(PackageBooking booking, String idToken) throws IOException, InterruptedException {
        String json = fieldsJson(
                fieldJson("bookingId", booking.bookingId()), fieldJson("userId", booking.userId()),
                fieldJson("packageId", booking.packageId()), fieldJson("packageName", booking.packageName()),
                fieldJson("route", booking.route()), fieldJson("duration", booking.duration()),
                fieldJson("primaryContactName", booking.primaryContact().fullName()),
                fieldJson("primaryContactMobile", booking.primaryContact().mobileNumber()),
                fieldJson("primaryContactEmail", booking.primaryContact().emailAddress()),
                numberFieldJson("travellerCount", booking.travellers().size()),
                fieldJson("travellers", encodeTravellers(booking.travellers())),
                fieldJson("selections", encodeStringMap(booking.selections())),
                fieldJson("componentPrices", encodeIntegerMap(booking.componentPrices())),
                numberFieldJson("baseAmount", booking.baseAmount()), numberFieldJson("finalAmount", booking.finalAmount()),
                fieldJson("currency", booking.currency()), fieldJson("paymentMode", booking.paymentMode()),
                fieldJson("paymentStatus", booking.paymentStatus()), fieldJson("bookingStatus", booking.bookingStatus()),
                fieldJson("applicationReference", booking.demoReference()), fieldJson("createdAt", booking.createdAt()));
        URI uri = documentUri("package_bookings", booking.bookingId());
        String path = "package_bookings/" + booking.bookingId();
        boolean tokenPresent = idToken != null && !idToken.isBlank();
        LOGGER.info(() -> "Kumbh package booking Firestore CREATE: bookingId=" + booking.bookingId()
                + ", ownerUid=" + booking.userId() + ", packageId=" + booking.packageId()
                + ", path=" + path + ", operation=CREATE, authTokenPresent=" + tokenPresent);
        HttpResponse<String> response = client.send(authorizedBuilder(uri, idToken)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
        String firestoreMessage = firestoreErrorMessage(response.body());
        if (response.statusCode() >= 400) {
            LOGGER.warning(() -> "Kumbh package booking Firestore CREATE failed: bookingId=" + booking.bookingId()
                    + ", ownerUid=" + booking.userId() + ", path=" + path + ", operation=CREATE, httpStatus="
                    + response.statusCode() + ", firestoreError=" + firestoreMessage);
            throw new FirestoreWriteException(response.statusCode(), firestoreMessage);
        }
        LOGGER.info(() -> "Kumbh package booking Firestore CREATE succeeded: bookingId=" + booking.bookingId()
                + ", ownerUid=" + booking.userId() + ", path=" + path + ", operation=CREATE, httpStatus="
                + response.statusCode());
    }

    public List<PackageBooking> loadPackageBookingsForUser(String userId, String idToken)
            throws IOException, InterruptedException {
        String query = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"package_bookings\"}],"
                + "\"where\":{\"fieldFilter\":{\"field\":{\"fieldPath\":\"userId\"},\"op\":\"EQUAL\","
                + "\"value\":{\"stringValue\":\"" + escape(userId) + "\"}}}}}";
        String json = post(URI.create(String.format(ROOT, enc(config.projectId())) + ":runQuery?key=" + enc(config.apiKey())), query, idToken);
        List<PackageBooking> records = new ArrayList<>();
        for (Document document : parseDocuments(json)) records.add(packageBookingFrom(document));
        return records;
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

    public void saveItem(String module, AppDataStore.ServiceItem item) {
        patch(documentUrl("appItems", item.id), itemJson(module, item));
    }

    /** Package documents are intentionally a dedicated collection rather than generic operational items. */
    public List<ManagedKumbhPackage> loadKumbhPackages(String idToken) throws IOException, InterruptedException {
        List<ManagedKumbhPackage> result = new ArrayList<>();
        List<Document> documents;
        if (idToken == null || idToken.isBlank()) {
            String query = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"kumbh_packages\"}],\"where\":{\"fieldFilter\":{\"field\":{\"fieldPath\":\"status\"},\"op\":\"EQUAL\",\"value\":{\"stringValue\":\"PUBLISHED\"}}}}}";
            documents = parseDocuments(post(URI.create(String.format(ROOT, enc(config.projectId())) + ":runQuery?key=" + enc(config.apiKey())), query, ""));
        } else {
            documents = loadCollectionDocuments("kumbh_packages", idToken);
        }
        for (Document document : documents) {
            String fields = document.fields;
            List<String> activities = splitLines(field(fields, "itineraryActivities"));
            List<KumbhPackage.Item> items = new ArrayList<>();
            for (String activity : activities) items.add(new KumbhPackage.Item(ItineraryItemType.OTHER, activity, ""));
            List<KumbhPackage.Day> itinerary = items.isEmpty() ? List.of() : List.of(new KumbhPackage.Day(valueOr("Day 1 — Journey", field(fields, "itineraryDayTitle")), items));
            Map<String, String> policies = new LinkedHashMap<>();
            for (String policy : splitLines(field(fields, "policies"))) { int split = policy.indexOf(':'); policies.put(split < 0 ? "Policy" : policy.substring(0, split).trim(), split < 0 ? policy : policy.substring(split + 1).trim()); }
            result.add(new ManagedKumbhPackage(document.id, field(fields, "packageCode"), field(fields, "name"), category(field(fields, "category")), field(fields, "theme"), field(fields, "badge"), field(fields, "origin"), valueOr("Nashik – Simhastha 2027", field(fields, "destination")), integer(fields, "days"), integer(fields, "nights"), field(fields, "shortDescription"), field(fields, "description"), splitLines(field(fields, "travelOptions")), splitLines(field(fields, "stayOptions")), splitLines(field(fields, "mealOptions")), splitLines(field(fields, "facilities")), splitLines(field(fields, "touristPlaces")), itinerary, integer(fields, "basePrice"), integer(fields, "startingPrice"), integer(fields, "originalPrice"), integer(fields, "discount"), splitLines(field(fields, "inclusions")), splitLines(field(fields, "exclusions")), policies, field(fields, "availableFrom"), field(fields, "availableUntil"), field(fields, "departureDates"), integer(fields, "maximumCapacity"), integer(fields, "minimumTravellers"), status(field(fields, "status")), field(fields, "createdBy"), field(fields, "createdAt"), field(fields, "updatedAt"), field(fields, "publishedAt"), media(field(fields, "coverImage"), PackageMediaType.COVER), media(field(fields, "heroImage"), PackageMediaType.HERO), gallery(field(fields, "gallery"))));
        }
        return result;
    }

    public void saveKumbhPackage(ManagedKumbhPackage p, String idToken) throws IOException, InterruptedException {
        String itineraryTitle = p.itinerary().isEmpty() ? "" : p.itinerary().get(0).title();
        String itineraryActivities = p.itinerary().isEmpty() ? "" : p.itinerary().stream().flatMap(day -> day.items().stream()).map(KumbhPackage.Item::text).collect(java.util.stream.Collectors.joining("\n"));
        String policies = p.policies().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(java.util.stream.Collectors.joining("\n"));
        String json = fieldsJson(fieldJson("packageId", p.packageId()), fieldJson("packageCode", p.packageCode()), fieldJson("name", p.name()), fieldJson("category", p.category().name()), fieldJson("theme", p.theme()), fieldJson("badge", p.badge()), fieldJson("origin", p.origin()), fieldJson("destination", p.destination()), numberFieldJson("days", p.days()), numberFieldJson("nights", p.nights()), fieldJson("shortDescription", p.shortDescription()), fieldJson("description", p.description()), fieldJson("travelOptions", joinLines(p.travelOptions())), fieldJson("stayOptions", joinLines(p.stayOptions())), fieldJson("mealOptions", joinLines(p.mealOptions())), fieldJson("facilities", joinLines(p.facilities())), fieldJson("touristPlaces", joinLines(p.touristPlaces())), fieldJson("itineraryDayTitle", itineraryTitle), fieldJson("itineraryActivities", itineraryActivities), numberFieldJson("basePrice", p.basePrice()), numberFieldJson("startingPrice", p.startingPrice()), numberFieldJson("originalPrice", p.originalPrice()), numberFieldJson("discount", p.discount()), fieldJson("inclusions", joinLines(p.inclusions())), fieldJson("exclusions", joinLines(p.exclusions())), fieldJson("policies", policies), fieldJson("availableFrom", p.availableFrom()), fieldJson("availableUntil", p.availableUntil()), fieldJson("departureDates", p.departureDates()), numberFieldJson("maximumCapacity", p.maximumCapacity()), numberFieldJson("minimumTravellers", p.minimumTravellers()), fieldJson("status", p.status().name()), fieldJson("createdBy", p.createdBy()), fieldJson("createdAt", p.createdAt()), fieldJson("updatedAt", p.updatedAt()), fieldJson("publishedAt", p.publishedAt()), fieldJson("coverImage", mediaValue(p.coverImage())), fieldJson("heroImage", mediaValue(p.heroImage())), fieldJson("gallery", galleryValue(p.gallery())));
        sendAuthorizedPatch(documentUri("kumbh_packages", p.packageId()), json, idToken);
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
        if (response.statusCode() >= 400) {
            throw new IOException("Firestore write failed: " + response.statusCode());
        }
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

    private String firestoreErrorMessage(String body) {
        Matcher matcher = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL)
                .matcher(body == null ? "" : body);
        if (matcher.find()) return unescape(matcher.group(1)).replaceAll("[\\r\\n]+", " ").trim();
        String compact = (body == null ? "" : body).replaceAll("[\\r\\n]+", " ").trim();
        return compact.isBlank() ? "No Firestore error message returned." : compact.substring(0, Math.min(600, compact.length()));
    }

    private static final class FirestoreWriteException extends IOException {
        private FirestoreWriteException(int status, String message) {
            super("Firestore write failed: HTTP " + status + " — " + message);
        }
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private int integer(String fields, String name) { return parseInt(numberField(fields, name), 0); }
    private PackageCategory category(String value) { try { return PackageCategory.valueOf(value); } catch (Exception ignored) { return PackageCategory.STANDARD; } }
    private PackageStatus status(String value) { try { return PackageStatus.valueOf(value); } catch (Exception ignored) { return PackageStatus.DRAFT; } }
    private List<String> splitLines(String value) { return java.util.Arrays.stream(value == null ? new String[0] : value.split("\\r?\\n")).map(String::trim).filter(v -> !v.isEmpty()).toList(); }
    private String joinLines(List<String> values) { return String.join("\n", values == null ? List.of() : values); }
    // Compact escaped metadata string; no image bytes/base64 are persisted in Firestore.
    private PackageMedia media(String value, PackageMediaType fallbackType) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.split("\\|", -1);
        try { return new PackageMedia(part(parts, 0), part(parts, 1), part(parts, 2), part(parts, 3), PackageMediaType.valueOf(part(parts, 4, fallbackType.name())), parseInt(part(parts, 5, "0"), 0), Boolean.parseBoolean(part(parts, 6, "false")), part(parts, 7)); } catch (Exception ignored) { return null; }
    }
    private List<PackageMedia> gallery(String value) { List<PackageMedia> result = new ArrayList<>(); for (String line : splitLines(value)) { PackageMedia media = media(line, PackageMediaType.GALLERY); if (media != null) result.add(media); } return result.stream().sorted(java.util.Comparator.comparingInt(PackageMedia::sortOrder)).toList(); }
    private String mediaValue(PackageMedia media) { return media == null ? "" : String.join("|", safe(media.mediaId()), safe(media.url()), safe(media.publicId()), safe(media.caption()), media.mediaType().name(), String.valueOf(media.sortOrder()), String.valueOf(media.primary()), safe(media.createdAt())); }
    private String galleryValue(List<PackageMedia> media) { return media == null ? "" : media.stream().sorted(java.util.Comparator.comparingInt(PackageMedia::sortOrder)).map(this::mediaValue).collect(java.util.stream.Collectors.joining("\n")); }
    private String part(String[] value, int index) { return part(value, index, ""); }
    private String part(String[] value, int index, String fallback) { return index < value.length ? value[index] : fallback; }
    private String safe(String value) { return value == null ? "" : value.replace("|", " ").replace("\n", " ").replace("\r", " "); }

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
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\");
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
