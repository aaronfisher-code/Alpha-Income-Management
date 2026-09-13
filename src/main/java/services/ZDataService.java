package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.Credit;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ZDataService {
    private final String apiBaseUrl;
    private final String apiToken;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public ZDataService() throws IOException {
        var properties = new Properties();
        try (var input = ZDataService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) throw new IOException("application.properties was not found");
            properties.load(input);
        }
        apiBaseUrl = properties.getProperty("api.base.url") + "/z-data";
        apiToken = properties.getProperty("api.token");
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(longProperty(properties, "api.connect.timeout.seconds", 10)));
        requests.setReadTimeout(Duration.ofSeconds(longProperty(properties, "api.read.timeout.seconds", 60)));
        restTemplate = new RestTemplate(requests);
    }

    public List<Credit> getCredits(int storeId, YearMonth month) {
        String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .path("/credits")
                .queryParam("storeId", storeId)
                .queryParam("startDate", month.atDay(1))
                .queryParam("endDate", month.atEndOfMonth())
                .toUriString();
        JsonNode rows = get(url).path("rows");
        var credits = new ArrayList<Credit>();
        rows.forEach(row -> credits.add(toCredit(row, storeId)));
        return credits;
    }

    public ConnectionStatus getStatus(int storeId) {
        String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .path("/status").queryParam("storeId", storeId).toUriString();
        try {
            return mapper.treeToValue(get(url), ConnectionStatus.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Alpha API returned an invalid Z agent status", exception);
        }
    }

    private JsonNode get(String url) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        try {
            return mapper.readTree(response.getBody());
        } catch (IOException exception) {
            throw new IllegalStateException("Alpha API returned invalid live Z data", exception);
        }
    }

    private static Credit toCredit(JsonNode row, int storeId) {
        var credit = new Credit();
        credit.setStoreID(storeId);
        credit.setCreditNo(row.path("creditNumber").asText());
        credit.setCreditDate(LocalDate.parse(row.path("creditDate").asText()));
        credit.setSupplierName(row.path("supplierName").asText());
        credit.setReferenceInvoiceNo(row.path("referenceInvoiceNumber").asText(""));
        credit.setCreditAmount(row.path("amountIncludingGst").asDouble());
        credit.setNotes("Z status: " + row.path("status").asText("Unknown"));
        credit.setReadOnly(true);
        credit.setSourceSystem("Z");
        return credit;
    }

    private static long longProperty(Properties properties, String name, long fallback) {
        try { return Long.parseLong(properties.getProperty(name, Long.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public record ConnectionStatus(String siteId, int storeId, boolean connected, String connectedSinceUtc) {}
}
