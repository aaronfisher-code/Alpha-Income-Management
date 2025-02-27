package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.CellDataPoint;
import models.Employment;
import models.Store;
import models.User;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmploymentService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public EmploymentService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        Properties properties = new Properties();
        properties.load(EmploymentService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/employments";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public void addEmployments(List<Employment> employments){
        int batchSize = 500;
        List<List<Employment>> batches = new ArrayList<>();

        // Split into batches
        for (int i = 0; i < employments.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, employments.size());
            batches.add(employments.subList(i, endIndex));
        }

        // Process each batch
        for (List<Employment> batch : batches) {
            HttpEntity<List<Employment>> entity = new HttpEntity<>(batch, createHeaders());
            restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
        }
    }

    public void deleteEmploymentsForUser(User user) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(user.getUserID()), StandardCharsets.UTF_8);
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
}