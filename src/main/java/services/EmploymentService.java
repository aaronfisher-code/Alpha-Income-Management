package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.Employment;
import models.Store;
import models.User;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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

    public void addEmployment(User user, Store store) {
        Employment employment = new Employment();
        employment.setUsername(user.getUsername());
        employment.setStoreID(store.getStoreID());

        HttpEntity<Employment> entity = new HttpEntity<>(employment, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void deleteEmploymentsForUser(User user) {
        String url = apiBaseUrl + "/" + user.getUsername();
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
}