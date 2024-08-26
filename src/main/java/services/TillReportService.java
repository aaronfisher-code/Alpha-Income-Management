package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.TillReportDataPoint;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .queryParam("storeId", storeId)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<List<TillReportDataPoint>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public List<TillReportDataPoint> getTillReportDataPointsByKey(int storeId, LocalDate startDate, LocalDate endDate, String key) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/by-key")
                .queryParam("storeId", storeId)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .queryParam("key", key);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<List<TillReportDataPoint>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public void importTillReportDataPoint(TillReportDataPoint dataPoint) {
        HttpEntity<TillReportDataPoint> entity = new HttpEntity<>(dataPoint, createHeaders());
        restTemplate.exchange(apiBaseUrl + "/import", HttpMethod.POST, entity, Void.class);
    }
}
