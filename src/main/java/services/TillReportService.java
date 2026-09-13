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

public class TillReportService {
    private final String apiBaseUrl;
    private final String apiToken;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final boolean liveZEnabled;

    public TillReportService() throws IOException {
        this(LiveZConfiguration.loadProperties());
    }

    TillReportService(Properties properties) {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(longProperty(properties, "api.connect.timeout.seconds", 10)));
        requests.setReadTimeout(Duration.ofSeconds(longProperty(properties, "api.read.timeout.seconds", 60)));
        this.restTemplate = new RestTemplate(requests);
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/till-report";
        this.liveZEnabled = LiveZConfiguration.from(properties).enabled();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<TillReportDataPoint> getTillReportDataPoints(int storeId, LocalDate startDate, LocalDate endDate) {
        try {
            URI uri = URI.create(apiBaseUrl + "?storeId=" + storeId +
                    "&startDate=" + startDate +
                    "&endDate=" + endDate);

            HttpEntity<?> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    String.class);

            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (IOException | RestClientException e) {
            throw requestFailure(e);
        }
    }

    public List<TillReportDataPoint> getTillReportDataPointsByKey(int storeId, LocalDate startDate, LocalDate endDate, String key) {
        try {
            String baseUrl = apiBaseUrl + "/by-key";
            URI uri = URI.create(baseUrl + "?storeId=" + storeId +
                    "&startDate=" + startDate +
                    "&endDate=" + endDate +
                    "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8));

            HttpEntity<?> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    String.class);

            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (IOException | RestClientException e) {
            throw requestFailure(e);
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

    private RuntimeException requestFailure(Exception cause) {
        if (liveZEnabled) {
            return new LiveZUnavailableException(
                    "Live Z till and script data is unavailable. Check the pharmacy forwarder connection.", cause);
        }
        return new RuntimeException("Error retrieving till report data", cause);
    }

    private static long longProperty(Properties properties, String name, long fallback) {
        try { return Long.parseLong(properties.getProperty(name, Long.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
