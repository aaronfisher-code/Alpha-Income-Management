package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.Invoice;
import models.CellDataPoint;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.Properties;

public class InvoiceService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public InvoiceService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Properties properties = new Properties();
        properties.load(InvoiceService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/invoices";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public Invoice getInvoice(String invoiceId) {
        String url = apiBaseUrl + "/" + invoiceId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Invoice> response = restTemplate.exchange(url, HttpMethod.GET, entity, Invoice.class);
        return response.getBody();
    }

    public List<Invoice> getAllInvoices(int storeId, YearMonth yearMonth) {
        String url = apiBaseUrl + "?storeId=" + storeId + "&yearMonth=" + yearMonth;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Invoice>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public double getTotalInvoiceAmount(int storeId, YearMonth yearMonth) {
        String url = apiBaseUrl + "/total?storeId=" + storeId + "&yearMonth=" + yearMonth;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Double> response = restTemplate.exchange(url, HttpMethod.GET, entity, Double.class);
        return response.getBody();
    }

    public List<Invoice> getInvoiceTableData(int storeId, YearMonth yearMonth) {
        String url = apiBaseUrl + "/table-data?storeId=" + storeId + "&yearMonth=" + yearMonth;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Invoice>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public boolean checkDuplicateInvoice(String invoiceId, int storeId, int supplierId) {
        String url = apiBaseUrl + "/check-duplicate?invoiceId=" + invoiceId + "&storeId=" + storeId + "&supplierId=" + supplierId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
        return response.getBody();
    }

    public void addInvoice(Invoice invoice) {
        HttpEntity<Invoice> entity = new HttpEntity<>(invoice, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void updateInvoice(Invoice invoice, String originalInvoiceNo) {
        String url = apiBaseUrl + "/" + originalInvoiceNo;
        HttpEntity<Invoice> entity = new HttpEntity<>(invoice, createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteInvoice(String invoiceId, int storeId) {
        String url = apiBaseUrl + "/" + invoiceId + "?storeId=" + storeId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public void importInvoiceData(int storeId, CellDataPoint cdp) {
        String url = apiBaseUrl + "/import-data?storeId=" + storeId;
        HttpEntity<CellDataPoint> entity = new HttpEntity<>(cdp, createHeaders());
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }
}
