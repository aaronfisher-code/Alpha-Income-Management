package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.AccountPayment;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.Properties;

public class AccountPaymentService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public AccountPaymentService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Properties properties = new Properties();
        properties.load(AccountPaymentService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/account-payments";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<AccountPayment> getAccountPaymentsForMonth(int storeId, YearMonth yearMonth) {
        String url = apiBaseUrl + "?storeId=" + storeId + "&yearMonth=" + yearMonth;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public void addAccountPayment(AccountPayment payment) {
        HttpEntity<AccountPayment> entity = new HttpEntity<>(payment, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void updateAccountPayment(String originalInvoiceNo, AccountPayment payment) {
        String url = apiBaseUrl + "/" + URLEncoder.encode(originalInvoiceNo, StandardCharsets.UTF_8);
        HttpEntity<AccountPayment> entity = new HttpEntity<>(payment, createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteAccountPayment(int storeId, String invoiceNumber) {
        String url = apiBaseUrl + "/" + storeId + "/" + URLEncoder.encode(invoiceNumber, StandardCharsets.UTF_8);
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public double getTotalPayment(int storeId, YearMonth yearMonth, PaymentType type) {
        String url = apiBaseUrl + "/total?storeId=" + storeId + "&yearMonth=" + yearMonth + "&type=" + type;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Double> response = restTemplate.exchange(url, HttpMethod.GET, entity, Double.class);
        return response.getBody();
    }

    public enum PaymentType {
        CPA, TAC, OTHER
    }
}