package services;

import models.Credit;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreditServiceTest {
    @Test void tableDataIncludesZMetadataButSavingChecksOnlyAcceptedAlphaCredits() {
        var paths = new ArrayList<String>(); var methods = new ArrayList<HttpMethod>();
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            assertEquals("Bearer test", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            String path = request.getURI().getPath(); paths.add(path); methods.add(request.getMethod());
            if (path.endsWith("/table-data")) return response(HttpStatus.OK,
                    "[{\"creditNo\":\"CR-1\",\"creditDate\":\"2026-09-10\",\"creditAmount\":100,\"zOfficeAmount\":100,\"zOfficeStatus\":\"Paid\",\"readOnly\":true,\"sourceSystem\":\"Z\"}]");
            return response(HttpStatus.OK, "[]");
        });
        var service = service(rest);
        Credit source = service.getCreditTableData(7, YearMonth.of(2026, 9)).getFirst();
        assertTrue(source.isReadOnly()); assertEquals("Z", source.getSourceSystem()); assertEquals(100, source.getZOfficeAmount());
        assertEquals("Paid", source.getZOfficeStatus());
        var accepted = new Credit(); accepted.setCreditNo("CR-1"); accepted.setStoreID(7); accepted.setCreditDate(LocalDate.of(2026, 9, 10)); accepted.setSupplierID(21);
        service.saveOrUpdateCredit(accepted);
        assertEquals(List.of("/credits/reconciliation/table-data", "/credits", "/credits"), paths);
        assertEquals(List.of(HttpMethod.GET, HttpMethod.GET, HttpMethod.POST), methods);
    }

    @Test void olderApiFallsBackToAcceptedCreditsButOtherFailuresAreNotHidden() {
        for (HttpStatus status : List.of(HttpStatus.NOT_FOUND, HttpStatus.SERVICE_UNAVAILABLE)) {
            var paths = new ArrayList<String>(); var rest = new RestTemplate();
            rest.getInterceptors().add((request, body, execution) -> {
                paths.add(request.getURI().getPath());
                return response(paths.size() == 1 ? status : HttpStatus.OK, "[]");
            });
            if (status == HttpStatus.NOT_FOUND) {
                assertTrue(service(rest).getCreditTableData(7, YearMonth.of(2026, 9)).isEmpty()); assertEquals(2, paths.size());
            } else {
                assertThrows(HttpServerErrorException.class, () -> service(rest).getCreditTableData(7, YearMonth.of(2026, 9)));
                assertEquals(1, paths.size());
            }
        }
    }

    private CreditService service(RestTemplate rest) {
        var properties = new Properties(); properties.setProperty("api.base.url", "https://example.invalid"); properties.setProperty("api.token", "test");
        return new CreditService(rest, properties);
    }
    private static ClientHttpResponse response(HttpStatus status, String json) {
        return new ClientHttpResponse() {
            public HttpStatus getStatusCode() { return status; }
            public String getStatusText() { return status.getReasonPhrase(); }
            public HttpHeaders getHeaders() { var result = new HttpHeaders(); result.setContentType(MediaType.APPLICATION_JSON); return result; }
            public InputStream getBody() { return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)); }
            public void close() {}
        };
    }
}
