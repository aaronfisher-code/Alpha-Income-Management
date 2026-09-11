package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.DocumentAiModels.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Typed client for Alpha API's store-scoped Drive and review-queue endpoints. */
public final class DocumentAiService {
    private final String baseUrl;
    private final String apiToken;
    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public DocumentAiService() throws IOException {
        Properties properties = new Properties();
        try (var input = DocumentAiService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) throw new IOException("application.properties was not found");
            properties.load(input);
        }
        apiToken = properties.getProperty("api.token", "");
        baseUrl = properties.getProperty("api.base.url", "").replaceFirst("/+$", "") + "/document-ai";
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout(properties, "api.connect.timeout.seconds", 10));
        requestFactory.setReadTimeout(timeout(properties, "api.read.timeout.seconds", 60));
        rest = new RestTemplate(requestFactory);
    }

    public DriveStatus driveStatus(int storeId) { return get("/stores/" + storeId + "/drive/status", DriveStatus.class); }
    public AuthorizationResponse connect(int storeId) {
        return exchange("/stores/" + storeId + "/drive/connect", HttpMethod.POST, null, AuthorizationResponse.class);
    }
    public void disconnect(int storeId) {
        exchange("/stores/" + storeId + "/drive", HttpMethod.DELETE, null, Void.class);
    }
    public List<DriveFolder> folders(int storeId, String parentId) {
        String suffix = parentId == null || parentId.isBlank() ? "" : "?parentId=" + enc(parentId);
        return getList("/stores/" + storeId + "/drive/folders" + suffix, new TypeReference<>() {});
    }
    public List<WatchFolder> watchFolders(int storeId) {
        return getList("/stores/" + storeId + "/drive/watch-folders", new TypeReference<>() {});
    }
    public List<WatchFolder> saveWatchFolders(int storeId, List<WatchFolderInput> folders) {
        return exchangeList("/stores/" + storeId + "/drive/watch-folders", HttpMethod.PUT, folders,
                new TypeReference<>() {});
    }
    public ScanSchedule schedule(int storeId) { return get("/stores/" + storeId + "/schedule", ScanSchedule.class); }
    public ScanSchedule saveSchedule(int storeId, ScanScheduleInput input) {
        return exchange("/stores/" + storeId + "/schedule", HttpMethod.PUT, input, ScanSchedule.class);
    }
    public void scanNow(int storeId) { exchange("/stores/" + storeId + "/scan-now", HttpMethod.POST, null, Void.class); }
    public List<BatchSummary> queue(int storeId) {
        return getList("/stores/" + storeId + "/review-queue", new TypeReference<>() {});
    }
    public BatchDetail batch(long batchId, int storeId) {
        return get("/review-batches/" + batchId + "?storeId=" + storeId, BatchDetail.class);
    }
    public byte[] pdf(long documentId, int storeId) {
        return exchange("/review-documents/" + documentId + "/pdf?storeId=" + storeId,
                HttpMethod.GET, null, byte[].class);
    }
    public void complete(long batchId, int storeId) {
        complete(batchId, storeId, List.of());
    }
    public void complete(long batchId, int storeId, List<EnteredDocument> enteredDocuments) {
        exchange("/review-batches/" + batchId + "/complete?storeId=" + storeId, HttpMethod.POST,
                enteredDocuments == null ? List.of() : enteredDocuments, Void.class);
    }
    public void retry(long batchId, int storeId) {
        exchange("/review-batches/" + batchId + "/retry?storeId=" + storeId, HttpMethod.POST, null, Void.class);
    }
    public void dismiss(long batchId, int storeId) {
        exchange("/review-batches/" + batchId + "?storeId=" + storeId, HttpMethod.DELETE, null, Void.class);
    }
    public void dismissDocument(long documentId, int storeId) {
        exchange("/review-documents/" + documentId + "?storeId=" + storeId, HttpMethod.DELETE, null, Void.class);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        headers.set("X-Alpha-Session", UserService.getDocumentAiSession());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    private <T> T get(String path, Class<T> type) { return exchange(path, HttpMethod.GET, null, type); }
    private <T> T exchange(String path, HttpMethod method, Object body, Class<T> type) {
        return rest.exchange(baseUrl + path, method, new HttpEntity<>(body, headers()), type).getBody();
    }
    private <T> List<T> getList(String path, TypeReference<List<T>> type) {
        return exchangeList(path, HttpMethod.GET, null, type);
    }
    private <T> List<T> exchangeList(String path, HttpMethod method, Object body, TypeReference<List<T>> type) {
        String json = rest.exchange(baseUrl + path, method, new HttpEntity<>(body, headers()), String.class).getBody();
        try { return mapper.readValue(json == null ? "[]" : json, type); }
        catch (IOException exception) { throw new IllegalStateException("Alpha API returned invalid document AI data", exception); }
    }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private static Duration timeout(Properties properties, String key, int defaultSeconds) throws IOException {
        String configured = properties.getProperty(key, Integer.toString(defaultSeconds)).trim();
        try {
            int seconds = Integer.parseInt(configured);
            if (seconds < 1) throw new NumberFormatException("must be positive");
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException exception) {
            throw new IOException(key + " must be a positive whole number", exception);
        }
    }
}
