package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.InvoiceSupplier;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class InvoiceSupplierService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public InvoiceSupplierService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        Properties properties = new Properties();
        properties.load(InvoiceSupplierService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/invoice-suppliers";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<InvoiceSupplier> getAllInvoiceSuppliers(int storeId) {
        String url = apiBaseUrl + "?storeId=" + storeId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            List<BackendInvoiceSupplier> backendSuppliers = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            return backendSuppliers.stream()
                    .map(this::convertToFrontendModel)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public InvoiceSupplier getInvoiceSupplierByName(String supplierName, int storeId) {
        String url = apiBaseUrl + "/by-name?supplierName=" + URLEncoder.encode(supplierName, StandardCharsets.UTF_8) + "&storeId=" + storeId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<BackendInvoiceSupplier> response = restTemplate.exchange(url, HttpMethod.GET, entity, BackendInvoiceSupplier.class);
        return convertToFrontendModel(response.getBody());
    }

    public void addInvoiceSupplier(InvoiceSupplier invoiceSupplier) {
        HttpEntity<BackendInvoiceSupplier> entity = new HttpEntity<>(convertToBackendModel(invoiceSupplier), createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void updateInvoiceSupplier(InvoiceSupplier invoiceSupplier) {
        String url = apiBaseUrl + "/" + invoiceSupplier.getContactID();
        HttpEntity<BackendInvoiceSupplier> entity = new HttpEntity<>(convertToBackendModel(invoiceSupplier), createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteInvoiceSupplier(int supplierId) {
        String url = apiBaseUrl + "/" + supplierId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    private InvoiceSupplier convertToFrontendModel(BackendInvoiceSupplier backendSupplier) {
        return new InvoiceSupplier(
                backendSupplier.getContactID(),
                backendSupplier.getSupplierName(),
                backendSupplier.getStoreID()
        );
    }

    private BackendInvoiceSupplier convertToBackendModel(InvoiceSupplier frontendSupplier) {
        BackendInvoiceSupplier backendSupplier = new BackendInvoiceSupplier();
        backendSupplier.setContactID(frontendSupplier.getContactID());
        backendSupplier.setSupplierName(frontendSupplier.getSupplierName());
        backendSupplier.setStoreID(frontendSupplier.getStoreID());
        return backendSupplier;
    }

    private static class BackendInvoiceSupplier {
        private int contactID;
        private String supplierName;
        private int storeID;
        public int getContactID() { return contactID; }
        public void setContactID(int contactID) { this.contactID = contactID; }
        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
        public int getStoreID() { return storeID; }
        public void setStoreID(int storeID) { this.storeID = storeID; }
    }
}
