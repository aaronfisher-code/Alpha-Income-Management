package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.User;
import models.Permission;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class UserPermissionService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public UserPermissionService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        Properties properties = new Properties();
        properties.load(UserPermissionService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/user-permissions";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public void addUserPermission(User user, Permission permission) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", user.getUsername());
        requestBody.put("permissionID", permission.getPermissionID());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void deletePermissionsForUser(User user) {
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(apiBaseUrl + "/" + user.getUsername(), HttpMethod.DELETE, entity, Void.class);
    }
}
