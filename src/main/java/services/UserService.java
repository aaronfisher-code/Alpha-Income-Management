package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.User;
import models.Employment;
import models.Permission;
import models.Store;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public class UserService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public UserService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Properties properties = new Properties();
        properties.load(UserService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/users";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public User getUserByUsername(String username) {
        String url = apiBaseUrl + "/by-username/" + URLEncoder.encode(username, StandardCharsets.UTF_8);
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<User> response = restTemplate.exchange(url, HttpMethod.GET, entity, User.class);
        return response.getBody();
    }

    public User getUserByID(int userID) {
        String url = apiBaseUrl + "/by-id/" + URLEncoder.encode(String.valueOf(userID), StandardCharsets.UTF_8);
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<User> response = restTemplate.exchange(url, HttpMethod.GET, entity, User.class);
        return response.getBody();
    }

    public List<User> getAllUsers() {
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(apiBaseUrl, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public List<User> getAllUserEmployments(int storeId) {
        String url = apiBaseUrl + "/store/" + storeId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public List<Employment> getEmploymentsForUser(int userID) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(userID),StandardCharsets.UTF_8) + "/employments";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public User verifyPassword(String username, String password) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(username,StandardCharsets.UTF_8) + "/verify-password";
        HttpEntity<String> entity = new HttpEntity<>(password, createHeaders());
        ResponseEntity<User> response = restTemplate.exchange(url, HttpMethod.POST, entity, User.class);
        return response.getBody();
    }

    public void updateUserPassword(int userID, String newPassword) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(userID),StandardCharsets.UTF_8) + "/password";
        HttpEntity<String> entity = new HttpEntity<>(newPassword, createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public boolean isPasswordResetRequested(int userID) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(userID),StandardCharsets.UTF_8) + "/password-reset-requested";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
        return response.getBody();
    }

    public List<Permission> getUserPermissions(int userID) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(userID),StandardCharsets.UTF_8) + "/permissions";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public List<Store> getStoresForUser(int userID) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(userID),StandardCharsets.UTF_8) + "/stores";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public void addUser(User user) {
        HttpEntity<User> entity = new HttpEntity<>(user, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void updateUser(User user) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8);
        HttpEntity<User> entity = new HttpEntity<>(user, createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteUser(int userID) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(userID),StandardCharsets.UTF_8);
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public void resetUserPassword(int userID) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(String.valueOf(userID),StandardCharsets.UTF_8) + "/reset-password";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }
}
