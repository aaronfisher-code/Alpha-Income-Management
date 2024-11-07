package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.Employment;
import models.User;
import models.Permission;
import models.UserPermissionDTO;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    public void addUserPermissions(List<UserPermissionDTO> userPermissions) {
        int batchSize = 500;
        List<List<UserPermissionDTO>> batches = new ArrayList<>();

        // Split into batches
        for (int i = 0; i < userPermissions.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, userPermissions.size());
            batches.add(userPermissions.subList(i, endIndex));
        }

        // Process each batch
        for (List<UserPermissionDTO> batch : batches) {
            HttpEntity<List<UserPermissionDTO>> entity = new HttpEntity<>(batch, createHeaders());
            restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
        }
    }

    public void deletePermissionsForUser(User user) {
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(user.getUserID()), StandardCharsets.UTF_8), HttpMethod.DELETE, entity, Void.class);
    }
}
