package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.Invoice;
import models.CellDataPoint;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

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

    public Invoice getInvoice(String invoiceId, int storeId) {
        String url = apiBaseUrl + "/"
                + URLEncoder.encode(invoiceId, StandardCharsets.UTF_8)
                + "?storeId=" + storeId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Invoice> resp =
                restTemplate.exchange(url, HttpMethod.GET, entity, Invoice.class);
        return resp.getBody();
    }

    public List<Invoice> getAllInvoices(int storeId, YearMonth yearMonth) {
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
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public boolean checkDuplicateInvoice(String invoiceId, int storeId, int supplierId) {
        String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .path("/check-duplicate")
                .queryParam("invoiceId", URLEncoder.encode(invoiceId, UTF_8))
                .queryParam("storeId", storeId)
                .queryParam("supplierId", supplierId)
                .toUriString();
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
        return Boolean.TRUE.equals(response.getBody());
    }

    public void addInvoice(Invoice invoice) {
        HttpEntity<Invoice> entity = new HttpEntity<>(invoice, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void updateInvoice(Invoice invoice, String originalInvoiceNo, int storeId, int supplierId) {
        String url = apiBaseUrl + "/"
                + URLEncoder.encode(originalInvoiceNo, UTF_8)
                + "?storeId=" + storeId
                + "&supplierId=" + supplierId;
        HttpEntity<Invoice> entity = new HttpEntity<>(invoice, createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteInvoice(String invoiceId, int storeId, int supplierId) {
        String url = apiBaseUrl + "/"
                + URLEncoder.encode(invoiceId, UTF_8)
                + "?storeId=" + storeId
                + "&supplierId=" + supplierId;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public void importInvoiceData(int storeId, List<CellDataPoint> dataPoints) {
        int batchSize = 1000;
        List<List<CellDataPoint>> batches = new ArrayList<>();

        // Split into batches
        for (int i = 0; i < dataPoints.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, dataPoints.size());
            batches.add(dataPoints.subList(i, endIndex));
        }

        // Process each batch
        for (List<CellDataPoint> batch : batches) {
            String url = apiBaseUrl + "/import-data?storeId=" + storeId;
            HttpEntity<List<CellDataPoint>> entity = new HttpEntity<>(batch, createHeaders());
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        }
    }
}
