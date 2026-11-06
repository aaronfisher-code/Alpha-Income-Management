package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.AccountPayment;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.Properties;

public class AccountPaymentService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public AccountPaymentService() throws IOException {
        this(loadProperties());
    }

    private AccountPaymentService(Properties properties) throws IOException {
        this(ApiHttpClient.create(properties), properties);
    }

    AccountPaymentService(RestTemplate restTemplate, Properties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/account-payments";
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (var input = AccountPaymentService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) throw new IOException("Missing application.properties");
            properties.load(input);
        }
        return properties;
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
        String urlTemplate = apiBaseUrl + "/{originalInvoiceNo}";
        restTemplate.exchange(
                urlTemplate,
                HttpMethod.PUT,
                new HttpEntity<>(payment, createHeaders()),
                Void.class,
                originalInvoiceNo   // RestTemplate will encode the '@' → "%40" exactly once
        );
    }

    public void deleteAccountPayment(AccountPayment payment) {
        int storeId = payment.getStoreID();
        String invoiceNumber = payment.getInvoiceNumber();
        String url = apiBaseUrl + "/{storeId}/{invoiceNumber}";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class, storeId, invoiceNumber);
        boolean stillPresent = getAccountPaymentsForMonth(storeId, YearMonth.from(payment.getInvDate())).stream()
                .anyMatch(row -> row.getStoreID() == storeId && invoiceNumber.equals(row.getInvoiceNumber()));
        if (stillPresent) {
            throw new IllegalStateException("The payment is still present after refreshing. Deletion was not confirmed.");
        }
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
