package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Permission;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class PermissionService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public PermissionService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        Properties properties = new Properties();
        properties.load(PermissionService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/permissions";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<Permission> getAllPermissions() {
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(apiBaseUrl, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }
}
