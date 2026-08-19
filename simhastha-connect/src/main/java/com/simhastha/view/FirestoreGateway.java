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
        String json = get(collectionUrl("appItems"));
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
        String json = get(collectionUrl("approvalRequests"));
        List<AppDataStore.ApprovalRequest> approvals = new ArrayList<>();
        for (Document document : parseDocuments(json)) {
            String type = field(document.fields, "type");
            String title = field(document.fields, "title");
            String detail = field(document.fields, "detail");
            String targetModule = field(document.fields, "targetModule");
            if (notBlank(type) && notBlank(title) && notBlank(targetModule)) {
                approvals.add(new AppDataStore.ApprovalRequest(document.id, type, title, detail, targetModule));
            }
        }
        return approvals;
    }

    public void saveItem(String module, AppDataStore.ServiceItem item) {
        patch(documentUrl("appItems", item.id), itemJson(module, item));
    }

    public void deleteItem(AppDataStore.ServiceItem item) {
        delete(documentUrl("appItems", item.id));
    }

    public void saveApproval(AppDataStore.ApprovalRequest request) {
        patch(documentUrl("approvalRequests", request.id), approvalJson(request));
    }

    public void deleteApproval(AppDataStore.ApprovalRequest request) {
        delete(documentUrl("approvalRequests", request.id));
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Firestore read failed: " + response.statusCode());
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
        sendWithoutBlocking(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .DELETE()
                .build());
    }

    private void sendWithoutBlocking(HttpRequest request) {
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private String collectionUrl(String collection) {
        return String.format(ROOT, enc(config.projectId())) + "/" + collection + "?key=" + enc(config.apiKey());
    }

    private String documentUrl(String collection, String documentId) {
        return String.format(ROOT, enc(config.projectId())) + "/" + collection + "/" + enc(documentId)
                + "?key=" + enc(config.apiKey());
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
                fieldJson("targetModule", request.targetModule));
    }

    private String fieldsJson(String... fields) {
        return "{\"fields\":{" + String.join(",", fields) + "}}";
    }

    private String fieldJson(String name, String value) {
        return "\"" + escape(name) + "\":{\"stringValue\":\"" + escape(value == null ? "" : value) + "\"}";
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
}
