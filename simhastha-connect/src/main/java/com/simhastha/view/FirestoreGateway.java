package com.simhastha.view;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FirestoreGateway {

    private static final String ROOT = "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents";
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

    public UserProfile loadUserProfile(String uid, String idToken) throws IOException, InterruptedException {
        HttpRequest request = authorizedBuilder(documentUri("users", uid), idToken)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() >= 400) {
            throw new IOException("Firestore profile read failed: " + response.statusCode());
        }
        List<Document> documents = parseDocuments(response.body());
        String fields = documents.isEmpty() ? extractFieldsObject(response.body()) : documents.get(0).fields;
        return new UserProfile(
                valueOr(uid, field(fields, "uid")),
                field(fields, "name"),
                field(fields, "email"),
                field(fields, "mobile"),
                field(fields, "role"),
                field(fields, "status"));
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
                fieldJson("status", "pending"),
                boolFieldJson("approved", false),
                fieldJson("createdAt", String.valueOf(System.currentTimeMillis())));
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
                fieldJson("status", "pending"),
                boolFieldJson("approved", false),
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
        String json = fieldsJson(
                fieldJson("status", status),
                boolFieldJson("approved", approved),
                fieldJson("updatedAt", String.valueOf(System.currentTimeMillis())));
        sendAuthorizedPatch(URI.create(documentUrl(collection, documentId)
                        + "&updateMask.fieldPaths=status&updateMask.fieldPaths=approved&updateMask.fieldPaths=updatedAt"),
                json, idToken);
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

    private String fieldsJson(String... fields) {
        return "{\"fields\":{" + String.join(",", fields) + "}}";
    }

    private String fieldJson(String name, String value) {
        return "\"" + escape(name) + "\":{\"stringValue\":\"" + escape(value == null ? "" : value) + "\"}";
    }

    private String boolFieldJson(String name, boolean value) {
        return "\"" + escape(name) + "\":{\"booleanValue\":" + value + "}";
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

    private String extractFieldsObject(String json) {
        Matcher matcher = Pattern.compile("\"fields\"\\s*:\\s*\\{(.*)\\}\\s*(?:,\\s*\"createTime\"|,\\s*\"updateTime\"|\\})",
                Pattern.DOTALL).matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : "";
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
}
