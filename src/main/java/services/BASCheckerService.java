package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.BASCheckerDataPoint;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.YearMonth;
import java.util.Properties;

public class BASCheckerService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public BASCheckerService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Properties properties = new Properties();
        properties.load(BASCheckerService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/bas-checker";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public BASCheckerDataPoint getBASData(int storeId, YearMonth yearMonth) {
        String url = apiBaseUrl + "?storeId=" + storeId + "&yearMonth=" + yearMonth;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<BASCheckerDataPoint> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, BASCheckerDataPoint.class);
        return response.getBody();
    }

    public void updateBASData(BASCheckerDataPoint basDataPoint) {
        HttpEntity<BASCheckerDataPoint> entity = new HttpEntity<>(basDataPoint, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }
}
