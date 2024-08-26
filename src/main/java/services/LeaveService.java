package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.LeaveRequest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

public class LeaveService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public LeaveService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Properties properties = new Properties();
        properties.load(LeaveService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/leave";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<LeaveRequest> getLeaveRequests(int storeId, LocalDate startDate, LocalDate endDate) {
        String url = apiBaseUrl + "?storeId=" + storeId + "&startDate=" + startDate + "&endDate=" + endDate;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public void addLeaveRequest(LeaveRequest leaveRequest) {
        HttpEntity<LeaveRequest> entity = new HttpEntity<>(leaveRequest, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void updateLeaveRequest(LeaveRequest leaveRequest) {
        String url = apiBaseUrl + "/" + leaveRequest.getLeaveID();
        HttpEntity<LeaveRequest> entity = new HttpEntity<>(leaveRequest, createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteLeaveRequest(int leaveId) {
        String url = apiBaseUrl + "/" + leaveId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
}
