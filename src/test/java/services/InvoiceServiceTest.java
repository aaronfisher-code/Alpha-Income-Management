package services;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceServiceTest {
    @Test
    void requestsAllReconciliationRowsWithStoreMonthAndAuthentication() {
        List<String> paths = new ArrayList<>();
        var invoices = service(HttpStatus.OK, paths).getReconciliationTableData(7, YearMonth.of(2026, 9));
        assertEquals(List.of("/invoices/reconciliation/table-data"), paths);
        assertEquals(1, invoices.size());
        assertEquals("123", invoices.getFirst().getInvoiceNo());
        assertTrue(invoices.getFirst().isImportExists());
    }

    @Test
    void olderApiUsesExistingMonthlyAndReferenceLookupPath() {
        List<String> paths = new ArrayList<>();
        assertEquals(1, service(HttpStatus.NOT_FOUND, paths).getReconciliationTableData(7, YearMonth.of(2026, 9)).size());
        assertEquals(List.of("/invoices/reconciliation/table-data", "/invoices/table-data"), paths);
    }

    @Test
    void pharmacyFailureIsNotTreatedAsMissingInvoicesOrAnOlderApi() {
        List<String> paths = new ArrayList<>();
        assertThrows(HttpServerErrorException.class,
                () -> service(HttpStatus.SERVICE_UNAVAILABLE, paths).getReconciliationTableData(7, YearMonth.of(2026, 9)));
        assertEquals(1, paths.size());
    }

    private InvoiceService service(HttpStatus reconciliationStatus, List<String> paths) {
        RestTemplate rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            assertEquals(HttpMethod.GET, request.getMethod());
            assertEquals("storeId=7&yearMonth=2026-09", request.getURI().getQuery());
            assertEquals("Bearer test-token", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            String path = request.getURI().getPath();
            paths.add(path);
            HttpStatus status = path.equals("/invoices/reconciliation/table-data") ? reconciliationStatus : HttpStatus.OK;
            return new ClientHttpResponse() {
                public HttpStatus getStatusCode() { return status; }
                public String getStatusText() { return status.getReasonPhrase(); }
                public HttpHeaders getHeaders() {
                    var headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    return headers;
                }
                public InputStream getBody() {
                    return new ByteArrayInputStream("[{\"invoiceNo\":\"123\",\"importExists\":true}]".getBytes(StandardCharsets.UTF_8));
                }
                public void close() {}
            };
        });
        Properties properties = new Properties();
        properties.setProperty("api.base.url", "https://example.invalid");
        properties.setProperty("api.token", "test-token");
        return new InvoiceService(rest, properties);
    }
}
