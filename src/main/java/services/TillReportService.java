package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.TillReportDataPoint;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

public class TillReportService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public TillReportService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Properties properties = new Properties();
        properties.load(TillReportService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/till-report";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<TillReportDataPoint> getTillReportDataPoints(int storeId, LocalDate startDate, LocalDate endDate) {
        try {
            URI uri = new URI(apiBaseUrl + "?storeId=" + storeId +
                    "&startDate=" + startDate +
                    "&endDate=" + endDate);

            HttpEntity<?> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    String.class);

            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Error executing request", e);
        }
    }

    public List<TillReportDataPoint> getTillReportDataPointsByKey(int storeId, LocalDate startDate, LocalDate endDate, String key) {
        try {
            String baseUrl = apiBaseUrl + "/by-key";
            URI uri = new URI(baseUrl + "?storeId=" + storeId +
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
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Error executing request", e);
        }
    }

    public void importTillReportDataPoint(TillReportDataPoint dataPoint) {
        HttpEntity<TillReportDataPoint> entity = new HttpEntity<>(dataPoint, createHeaders());
        restTemplate.exchange(apiBaseUrl + "/import", HttpMethod.POST, entity, Void.class);
    }
}