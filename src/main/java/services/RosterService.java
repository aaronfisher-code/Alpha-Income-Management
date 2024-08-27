package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.Shift;
import models.SpecialDateObj;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

public class RosterService {
    private String apiBaseUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public RosterService() throws IOException {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Properties properties = new Properties();
        properties.load(RosterService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.apiToken = properties.getProperty("api.token");
        this.apiBaseUrl = properties.getProperty("api.base.url") + "/roster";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        return headers;
    }

    public List<Shift> getShifts(int storeId, LocalDate startDate, LocalDate endDate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/shifts")
                .queryParam("storeId", storeId)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Shift>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public List<Shift> getShiftModifications(int storeId, LocalDate startDate, LocalDate endDate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/shift-modifications")
                .queryParam("storeId", storeId)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Shift>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public void addShift(Shift shift) {
        HttpEntity<Shift> entity = new HttpEntity<>(shift, createHeaders());
        restTemplate.exchange(apiBaseUrl + "/shifts", HttpMethod.POST, entity, Void.class);
    }

    public void updateShift(Shift shift) {
        HttpEntity<Shift> entity = new HttpEntity<>(shift, createHeaders());
        restTemplate.exchange(apiBaseUrl + "/shifts/" + shift.getShiftID(), HttpMethod.PUT, entity, Void.class);
    }

    public void deleteShift(int shiftId) {
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(apiBaseUrl + "/shifts/" + shiftId, HttpMethod.DELETE, entity, Void.class);
    }

    public void addShiftModification(Shift modification) {
        HttpEntity<Shift> entity = new HttpEntity<>(modification, createHeaders());
        restTemplate.exchange(apiBaseUrl + "/shift-modifications", HttpMethod.POST, entity, Void.class);
    }

    public void deleteShiftModifications(int shiftId) {
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(apiBaseUrl + "/shift-modifications/" + shiftId, HttpMethod.DELETE, entity, Void.class);
    }

    public void deleteShiftModifications(int shiftId, LocalDate cutoffDate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/shift-modifications/" + shiftId + "/cutoff")
                .queryParam("cutoffDate", cutoffDate);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(builder.toUriString(), HttpMethod.DELETE, entity, Void.class);
    }

    public void updateShiftEndDate(int shiftId, LocalDate endDate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/shifts/" + shiftId + "/end-date")
                .queryParam("endDate", endDate);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        restTemplate.exchange(builder.toUriString(), HttpMethod.PUT, entity, Void.class);
    }

    public SpecialDateObj getSpecialDateInfo(LocalDate date) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/special-date")
                .queryParam("date", date);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<SpecialDateObj> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                SpecialDateObj.class);

        return response.getBody();
    }

    public List<SpecialDateObj> getSpecialDates(LocalDate startDate, LocalDate endDate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/special-dates")
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate);

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }

    public void addSpecialDate(SpecialDateObj specialDateObj) {
        HttpEntity<SpecialDateObj> entity = new HttpEntity<>(specialDateObj, createHeaders());
        restTemplate.exchange(apiBaseUrl + "/special-date", HttpMethod.POST, entity, Void.class);
    }

    public void updateSpecialDate(SpecialDateObj specialDateObj) {
        HttpEntity<SpecialDateObj> entity = new HttpEntity<>(specialDateObj, createHeaders());
        restTemplate.exchange(apiBaseUrl + "/special-date", HttpMethod.PUT, entity, Void.class);
    }
}
