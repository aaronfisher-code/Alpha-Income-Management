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
    void cachedResolveAndRefreshRequestsUseTheirExplicitBulkEndpoints() {
        List<String> paths = new ArrayList<>();
        RestTemplate rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            assertEquals(HttpMethod.POST, request.getMethod());
            assertEquals("storeId=7", request.getURI().getQuery());
            assertEquals("Bearer test-token", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            paths.add(request.getURI().getPath());
            assertTrue(new String(body, StandardCharsets.UTF_8).contains("I000123"));
            return new ClientHttpResponse() {
                public HttpStatus getStatusCode() { return HttpStatus.OK; }
                public String getStatusText() { return "OK"; }
                public HttpHeaders getHeaders() { var h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); return h; }
                public InputStream getBody() { return new ByteArrayInputStream("[{\"invoiceNo\":\"000123\",\"importedInvoiceAmount\":0,\"importExists\":true}]".getBytes(StandardCharsets.UTF_8)); }
                public void close() {}
            };
        });
        var properties = new Properties(); properties.setProperty("api.base.url", "https://example.invalid"); properties.setProperty("api.token", "test-token");
        var service = new InvoiceService(rest, properties);
        var row = new models.ScannedInvoice("invoice.pdf", "Symbion", "I000123", java.time.LocalDate.of(2026, 9, 10),
                null, models.ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
        assertEquals(0, service.getCachedInvoiceAmounts(7, List.of(row)).getFirst().getImportedInvoiceAmount());
        assertTrue(service.getInvoiceAmountsForReview(7, List.of(row)).getFirst().isImportExists());
        assertTrue(service.refreshInvoiceAmount(7, row).getFirst().isImportExists());
        assertEquals(List.of("/invoices/reconciliation/cached", "/invoices/reconciliation/resolve", "/invoices/reconciliation/refresh"), paths);
    }

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

    @Test
    void fiveHundredRowsAreBatchedAndDuplicateResponsesRemainUnambiguous() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        RestTemplate rest = new RestTemplate();
        var sizes = new ArrayList<Integer>();
        rest.getInterceptors().add((request, body, execution) -> {
            assertEquals("/invoices/reconciliation/resolve", request.getURI().getPath());
            var requested = mapper.readTree(body);
            sizes.add(requested.size());
            assertTrue(requested.size() <= 50);
            var response = mapper.createArrayNode();
            for (var row : requested) {
                response.addObject().put("invoiceNo", row.path("invoiceNo").asText())
                        .put("importedInvoiceAmount", 100).put("importExists", true);
            }
            return jsonResponse(response.toString());
        });
        var properties = new Properties();
        properties.setProperty("api.base.url", "https://example.invalid");
        var service = new InvoiceService(rest, properties);
        var scanned = new ArrayList<models.ScannedInvoice>();
        for (int i = 0; i < 500; i++) scanned.add(new models.ScannedInvoice("file" + i, "Supplier", "INV" + i,
                java.time.LocalDate.of(2026, 9, 10), null, models.ScannedInvoice.DocumentType.INVOICE, 10000, 0.99));
        // A different source PDF can contain the same reference in a later HTTP chunk.
        scanned.set(499, new models.ScannedInvoice("another.pdf", "Supplier", "INV0", java.time.LocalDate.of(2026, 9, 10),
                null, models.ScannedInvoice.DocumentType.INVOICE, 10000, 0.99));
        assertEquals(499, service.getInvoiceAmountsForReview(7, scanned).size());
        assertEquals(10, sizes.size());
        assertTrue(service.getInvoiceAmountsForReview(7, List.of()).isEmpty());
        assertEquals(10, sizes.size());
    }

    @Test
    void genuineAmbiguitySurvivesChunkMergingAndFailedChunksDoNotReturnPartialData() {
        var rest = new RestTemplate();
        var count = new java.util.concurrent.atomic.AtomicInteger();
        rest.getInterceptors().add((request, body, execution) -> {
            if (count.incrementAndGet() == 3) throw new java.io.IOException("connection lost");
            return jsonResponse("[{\"invoiceNo\":\"123\",\"importExists\":true},{\"invoiceNo\":\"123\",\"importExists\":true}]");
        });
        var properties = new Properties(); properties.setProperty("api.base.url", "https://example.invalid");
        var service = new InvoiceService(rest, properties);
        var row = new models.ScannedInvoice("invoice.pdf", "Supplier", "123", java.time.LocalDate.of(2026, 9, 10),
                null, models.ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
        assertEquals(2, service.getInvoiceAmountsForReview(7, List.of(row)).size());
        var rows = java.util.stream.IntStream.range(0, 100).mapToObj(i -> new models.ScannedInvoice("file" + i, "Supplier",
                "REF" + i, row.invoiceDate(), null, row.documentType(), row.amountCents(), 0.99)).toList();
        assertThrows(org.springframework.web.client.ResourceAccessException.class,
                () -> service.getInvoiceAmountsForReview(7, rows));
    }

    private static ClientHttpResponse jsonResponse(String json) {
        return new ClientHttpResponse() {
            public HttpStatus getStatusCode() { return HttpStatus.OK; }
            public String getStatusText() { return "OK"; }
            public HttpHeaders getHeaders() { var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); return headers; }
            public InputStream getBody() { return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)); }
            public void close() {}
        };
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
