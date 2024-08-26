package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.EODDataPoint;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

public class EODService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;

    public EODService() throws IOException {
        this.restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        restTemplate.getMessageConverters().addFirst(converter);
        Properties properties = new Properties();
        properties.load(EODService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/eod";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<EODDataPoint> getEODDataPoints(int storeId, LocalDate startDate, LocalDate endDate) {
        String url = String.format("%s?storeId=%d&startDate=%s&endDate=%s",
                apiBaseUrl, storeId, startDate, endDate);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<List<EODDataPoint>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public void updateEODDataPoint(EODDataPoint eodDataPoint) {
        HttpEntity<EODDataPoint> entity = new HttpEntity<>(eodDataPoint, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.PUT, entity, Void.class);
    }

    public void insertEODDataPoint(EODDataPoint eodDataPoint) {
        HttpEntity<EODDataPoint> entity = new HttpEntity<>(eodDataPoint, createHeaders());
        restTemplate.exchange(apiBaseUrl, HttpMethod.POST, entity, EODDataPoint.class);
    }
}