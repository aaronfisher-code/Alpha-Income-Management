package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.AccountPaymentContactDataPoint;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AccountPaymentContactService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public AccountPaymentContactService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        Properties properties = new Properties();
        properties.load(AccountPaymentContactService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/account-payment-contacts";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<AccountPaymentContactDataPoint> getAllAccountPaymentContacts(int storeId) {
        String url = apiBaseUrl + "?storeId=" + storeId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            List<BackendAccountPaymentContactDataPoint> backendContacts = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            return backendContacts.stream()
                    .map(this::convertToFrontendModel)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public void addAccountPaymentContact(AccountPaymentContactDataPoint contact) {
        HttpEntity<BackendAccountPaymentContactDataPoint> entity = new HttpEntity<>(convertToBackendModel(contact), createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void updateAccountPaymentContact(AccountPaymentContactDataPoint contact) {
        String url = apiBaseUrl + "/" + contact.getContactID();
        HttpEntity<BackendAccountPaymentContactDataPoint> entity = new HttpEntity<>(convertToBackendModel(contact), createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteAccountPaymentContact(int contactId) {
        String url = apiBaseUrl + "/" + contactId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public AccountPaymentContactDataPoint getContactByName(String name, int storeId) {
        String url = apiBaseUrl + "/by-name?name=" + name + "&storeId=" + storeId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<BackendAccountPaymentContactDataPoint> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, BackendAccountPaymentContactDataPoint.class);
        return convertToFrontendModel(response.getBody());
    }

    private AccountPaymentContactDataPoint convertToFrontendModel(BackendAccountPaymentContactDataPoint backendContact) {
        AccountPaymentContactDataPoint frontendContact = new AccountPaymentContactDataPoint(
                backendContact.getContactID(),
                backendContact.getContactName(),
                backendContact.getStoreID()
        );
        frontendContact.setAccountCode(backendContact.getAccountCode());
        return frontendContact;
    }

    private BackendAccountPaymentContactDataPoint convertToBackendModel(AccountPaymentContactDataPoint frontendContact) {
        BackendAccountPaymentContactDataPoint backendContact = new BackendAccountPaymentContactDataPoint();
        backendContact.setContactID(frontendContact.getContactID());
        backendContact.setContactName(frontendContact.getContactName());
        backendContact.setStoreID(frontendContact.getStoreID());
        backendContact.setAccountCode(frontendContact.getAccountCode());
        return backendContact;
    }

    // Inner class to represent the backend model
    private static class BackendAccountPaymentContactDataPoint {
        private int contactID;
        private String contactName;
        private int storeID;
        private String accountCode;

        // Getters and setters
        public int getContactID() {
            return contactID;
        }
        public void setContactID(int contactID) {
            this.contactID = contactID;
        }
        public String getContactName() {
            return contactName;
        }
        public void setContactName(String contactName) {
            this.contactName = contactName;
        }
        public int getStoreID() {
            return storeID;
        }
        public void setStoreID(int storeID) {
            this.storeID = storeID;
        }
        public String getAccountCode() {
            return accountCode;
        }
        public void setAccountCode(String accountCode) {
            this.accountCode = accountCode;
        }
    }
}
