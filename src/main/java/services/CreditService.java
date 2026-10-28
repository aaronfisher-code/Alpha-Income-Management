package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.Credit;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.Properties;

public class CreditService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public CreditService() throws IOException {
        this(loadProperties());
    }

    private CreditService(Properties properties) throws IOException {
        this(ApiHttpClient.create(properties), properties);
    }

    CreditService(RestTemplate restTemplate, Properties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/credits";
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (var input = CreditService.class.getClassLoader().getResourceAsStream("application.properties")) { properties.load(input); }
        return properties;
    }

    /** Display-only source credits; CRUD and duplicate detection continue to read accepted Alpha records. */
    public List<Credit> getCreditTableData(int storeId, YearMonth month) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(apiBaseUrl + "/reconciliation/table-data?storeId=" + storeId + "&yearMonth=" + month,
                    HttpMethod.GET, new HttpEntity<>(createHeaders()), String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Credit>>() {});
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound olderApi) {
            return getAllCredits(storeId, month);
        } catch (IOException exception) { throw new IllegalStateException("Unable to read cached Z-Office credit data", exception); }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<Credit> getAllCredits(int storeId, YearMonth yearMonth) {
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

    public void addCredit(Credit credit) {
        HttpEntity<Credit> entity = new HttpEntity<>(credit, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, Void.class);
    }

    public void saveOrUpdateCredit(Credit credit) {
        Credit duplicate = getAllCredits(credit.getStoreID(), YearMonth.from(credit.getCreditDate())).stream()
                .filter(existing -> existing.getSupplierID() == credit.getSupplierID()
                        && existing.getCreditNo() != null
                        && existing.getCreditNo().equalsIgnoreCase(credit.getCreditNo()))
                .findFirst()
                .orElse(null);
        if (duplicate == null) {
            addCredit(credit);
            return;
        }
        credit.setCreditID(duplicate.getCreditID());
        updateCredit(credit);
    }

    public void updateCredit(Credit credit) {
        String url = apiBaseUrl + "/" + credit.getCreditID();
        HttpEntity<Credit> entity = new HttpEntity<>(credit, createHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    public void deleteCredit(int creditID) {
        String url = apiBaseUrl + "/" + creditID;
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
}
