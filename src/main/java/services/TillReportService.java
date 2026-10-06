package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.TillReportDataPoint;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class TillReportService {
    private static final Set<String> SCRIPT_KEYS = Set.of("Script Count", "Govt Recovery");
    private final String apiBaseUrl;
    private final String apiToken;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final boolean liveZEnabled;

    public TillReportService() throws IOException {
        this(LiveZConfiguration.loadProperties());
    }

    TillReportService(Properties properties) {
        this(createRestTemplate(properties), properties);
    }

    TillReportService(RestTemplate restTemplate, Properties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/till-report";
        this.liveZEnabled = LiveZConfiguration.from(properties).enabled();
    }

    private static RestTemplate createRestTemplate(Properties properties) {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(longProperty(properties, "api.connect.timeout.seconds", 10)));
        requests.setReadTimeout(Duration.ofSeconds(longProperty(properties, "api.read.timeout.seconds", 60)));
        return new RestTemplate(requests);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<TillReportDataPoint> getTillReportDataPoints(int storeId, LocalDate startDate, LocalDate endDate) {
        var rows = new ArrayList<>(requestTillReportDataPoints(storeId, startDate, endDate, false));
        if (liveZEnabled) {
            // Refresh the whole requested range once. In particular, historical
            // months must not depend on the scheduled export's seven-day overlap.
            var scripts = refreshScriptTotals(storeId, startDate, endDate);
            rows.removeIf(row -> SCRIPT_KEYS.contains(row.getKey()));
            rows.addAll(scripts);
        }
        return rows;
    }

    public List<TillReportDataPoint> refreshScriptTotals(int storeId, LocalDate startDate, LocalDate endDate) {
        return requestTillReportDataPoints(storeId, startDate, endDate, "/refresh/script-totals", true);
    }

    public List<TillReportDataPoint> refreshTillReportDataPoints(int storeId, LocalDate startDate, LocalDate endDate) {
        return requestTillReportDataPoints(storeId, startDate, endDate, true);
    }

    /** Exports need fresh till metrics as well as fresh dispensing totals. */
    public List<TillReportDataPoint> getTillReportDataPointsForExport(int storeId, LocalDate startDate, LocalDate endDate) {
        return liveZEnabled ? refreshTillReportDataPoints(storeId, startDate, endDate)
                : getTillReportDataPoints(storeId, startDate, endDate);
    }

    private List<TillReportDataPoint> requestTillReportDataPoints(
            int storeId, LocalDate startDate, LocalDate endDate, boolean refresh) {
        return requestTillReportDataPoints(storeId, startDate, endDate, refresh ? "/refresh" : "", refresh);
    }

    private List<TillReportDataPoint> requestTillReportDataPoints(
            int storeId, LocalDate startDate, LocalDate endDate, String path, boolean refresh) {
        try {
            URI uri = URI.create(apiBaseUrl + path + "?storeId=" + storeId +
                    "&startDate=" + startDate +
                    "&endDate=" + endDate);

            HttpEntity<?> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    refresh ? HttpMethod.POST : HttpMethod.GET,
                    entity,
                    String.class);

            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (IOException | RestClientException e) {
            throw requestFailure(e, refresh);
        }
    }

    public List<TillReportDataPoint> getTillReportDataPointsByKey(int storeId, LocalDate startDate, LocalDate endDate, String key) {
        return requestTillReportDataPointsByKey(
                storeId, startDate, endDate, key, liveZEnabled && SCRIPT_KEYS.contains(key));
    }

    public List<TillReportDataPoint> refreshTillReportDataPointsByKey(int storeId, LocalDate startDate, LocalDate endDate, String key) {
        return requestTillReportDataPointsByKey(storeId, startDate, endDate, key, true);
    }

    private List<TillReportDataPoint> requestTillReportDataPointsByKey(
            int storeId, LocalDate startDate, LocalDate endDate, String key, boolean refresh) {
        try {
            String baseUrl = apiBaseUrl + (refresh ? "/refresh/by-key" : "/by-key");
            URI uri = URI.create(baseUrl + "?storeId=" + storeId +
                    "&startDate=" + startDate +
                    "&endDate=" + endDate +
                    "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8));

            HttpEntity<?> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    refresh ? HttpMethod.POST : HttpMethod.GET,
                    entity,
                    String.class);

            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (IOException | RestClientException e) {
            throw requestFailure(e, refresh);
        }
    }

    public void importTillReportData(List<TillReportDataPoint> dataPoints) {
        int batchSize = 1000;
        List<List<TillReportDataPoint>> batches = new ArrayList<>();
        // Split into batches
        for (int i = 0; i < dataPoints.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, dataPoints.size());
            batches.add(dataPoints.subList(i, endIndex));
        }
        // Process each batch
        for (List<TillReportDataPoint> batch : batches) {
            HttpEntity<List<TillReportDataPoint>> entity = new HttpEntity<>(batch, createHeaders());
            restTemplate.exchange(apiBaseUrl + "/import", HttpMethod.POST, entity, Void.class);
        }
    }

    private RuntimeException requestFailure(Exception cause, boolean liveRefresh) {
        if (liveZEnabled && liveRefresh) {
            return new LiveZUnavailableException(
                    "Fresh pharmacy till data is unavailable. Check the Z forwarder connection.", cause);
        }
        return new RuntimeException("Error retrieving cached till report data from Alpha API", cause);
    }

    private static long longProperty(Properties properties, String name, long fallback) {
        try { return Long.parseLong(properties.getProperty(name, Long.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
