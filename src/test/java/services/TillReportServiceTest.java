package services;

import models.TillReportDataPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class TillReportServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);
    private static final String CACHED = """
            [{"assignedDate":"2026-09-15","key":"Govt Recovery","amount":999},
             {"assignedDate":"2026-09-15","key":"Script Count","quantity":999},
             {"assignedDate":"2026-09-16","key":"Total Takings","amount":3000}]
            """;
    private static final String FRESH = """
            [{"assignedDate":"2026-09-15","key":"Govt Recovery","amount":1000},
             {"assignedDate":"2026-09-16","key":"Govt Recovery","amount":1500},
             {"assignedDate":"2026-09-16","key":"Script Count","quantity":50},
             {"assignedDate":"2026-09-30","key":"Govt Recovery","amount":2500},
             {"assignedDate":"2026-09-30","key":"Script Count","quantity":75}]
            """;

    @Test
    void allReportConsumersReceiveLiveDailyRecoveryAndFullMonthTotal() {
        var requests = new ArrayList<Request>();
        var service = service(true, requests, request -> ok(
                request.method().equals(HttpMethod.POST) ? FRESH : CACHED));

        var rows = service.getTillReportDataPoints(1, START, END);

        assertEquals(2, requests.size());
        assertEquals(HttpMethod.GET, requests.getFirst().method());
        assertEquals("/till-report", requests.getFirst().uri().getPath());
        assertEquals(HttpMethod.POST, requests.getLast().method());
        assertEquals("/till-report/refresh/script-totals", requests.getLast().uri().getPath());
        assertEquals("storeId=1&startDate=2026-09-01&endDate=2026-09-30",
                requests.getLast().uri().getQuery());
        assertEquals(5000, rows.stream().filter(row -> "Govt Recovery".equals(row.getKey()))
                .mapToDouble(TillReportDataPoint::getAmount).sum());
        assertEquals(125, rows.stream().mapToDouble(TillReportDataPoint::getQuantity).sum());
        assertEquals(2500, rows.stream().filter(row -> END.equals(row.getAssignedDate())
                && "Govt Recovery".equals(row.getKey())).findFirst().orElseThrow().getAmount());
        assertEquals(1, rows.stream().filter(row -> "Total Takings".equals(row.getKey())).count());
        assertEquals(1, rows.stream().filter(row -> START.plusDays(14).equals(row.getAssignedDate())
                && "Govt Recovery".equals(row.getKey())).count());
    }

    @Test
    void freshEmptyRangeRemovesPreviouslyImportedScriptValues() {
        var requests = new ArrayList<Request>();
        var rows = service(true, requests, request -> ok(
                request.method().equals(HttpMethod.POST) ? "[]" : CACHED))
                .getTillReportDataPoints(1, START, END);

        assertEquals(List.of("Total Takings"), rows.stream().map(TillReportDataPoint::getKey).toList());
    }

    @Test
    void offlineAgentFailsReportInsteadOfReturningOldCsvIncome() {
        var requests = new ArrayList<Request>();
        var service = service(true, requests, request -> request.method().equals(HttpMethod.POST)
                ? new Reply(HttpStatus.SERVICE_UNAVAILABLE, "{\"error\":\"Agent offline\"}") : ok(CACHED));

        assertThrows(LiveZUnavailableException.class, () -> service.getTillReportDataPoints(1, START, END));
        assertEquals(2, requests.size());
    }

    @Test
    void repeatedReportRequestsFetchNewValuesEachTime() {
        var requests = new ArrayList<Request>();
        var service = service(true, requests, request -> ok(request.method().equals(HttpMethod.POST)
                ? FRESH.replace("2500", Integer.toString(2500 + requests.size())) : CACHED));

        var first = service.getTillReportDataPoints(1, START, END);
        var second = service.getTillReportDataPoints(1, START, END);

        assertEquals(4, requests.size());
        assertEquals(2, second.stream().mapToDouble(TillReportDataPoint::getAmount).sum()
                - first.stream().mapToDouble(TillReportDataPoint::getAmount).sum());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Govt Recovery", "Script Count"})
    void scriptMetricReadsUseFreshKeyedEndpoint(String key) {
        var requests = new ArrayList<Request>();
        service(true, requests, request -> ok("[]")).getTillReportDataPointsByKey(1, START, END, key);

        assertEquals(1, requests.size());
        assertEquals(HttpMethod.POST, requests.getFirst().method());
        assertEquals("/till-report/refresh/by-key", requests.getFirst().uri().getPath());
        assertTrue(requests.getFirst().uri().getRawQuery().contains("key=" + key.replace(" ", "+")));
    }

    @Test
    void nonScriptKeyDoesNotQueryScripts() {
        var requests = new ArrayList<Request>();
        service(true, requests, request -> ok("[]"))
                .getTillReportDataPointsByKey(1, START, END, "Total Takings");

        assertEquals(1, requests.size());
        assertEquals(HttpMethod.GET, requests.getFirst().method());
        assertEquals("/till-report/by-key", requests.getFirst().uri().getPath());
    }

    @Test
    void manualImportModeRetainsCachedReads() {
        var requests = new ArrayList<Request>();
        var rows = service(false, requests, request -> ok(CACHED)).getTillReportDataPoints(1, START, END);

        assertEquals(1, requests.size());
        assertEquals(HttpMethod.GET, requests.getFirst().method());
        assertEquals(999, rows.getFirst().getAmount());
    }

    @Test
    void exportRefreshesAllMetricsForEntireMonthInOneRequest() {
        var requests = new ArrayList<Request>();
        var rows = service(true, requests, request -> ok(FRESH))
                .getTillReportDataPointsForExport(1, START, END);

        assertEquals(1, requests.size());
        assertEquals(HttpMethod.POST, requests.getFirst().method());
        assertEquals("/till-report/refresh", requests.getFirst().uri().getPath());
        assertEquals("storeId=1&startDate=2026-09-01&endDate=2026-09-30", requests.getFirst().uri().getQuery());
        assertEquals(5, rows.size());
    }

    @Test
    void failedExportRefreshDoesNotFallBackToCachedFigures() {
        var requests = new ArrayList<Request>();
        var service = service(true, requests, request -> new Reply(HttpStatus.SERVICE_UNAVAILABLE, "Offline"));

        assertThrows(LiveZUnavailableException.class, () -> service.getTillReportDataPointsForExport(1, START, END));
        assertEquals(1, requests.size());
    }

    @Test
    void manualExportReadsImportedFiguresWithoutContactingZ() {
        var requests = new ArrayList<Request>();
        var rows = service(false, requests, request -> ok(CACHED))
                .getTillReportDataPointsForExport(1, START, END);

        assertEquals(1, requests.size());
        assertEquals(HttpMethod.GET, requests.getFirst().method());
        assertEquals(999, rows.getFirst().getAmount());
    }

    private TillReportService service(boolean live, List<Request> requests, Function<Request, Reply> replies) {
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            assertEquals("Bearer test-token", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            var captured = new Request(request.getURI(), request.getMethod());
            requests.add(captured);
            var reply = replies.apply(captured);
            return new ClientHttpResponse() {
                public HttpStatus getStatusCode() { return reply.status(); }
                public String getStatusText() { return reply.status().getReasonPhrase(); }
                public HttpHeaders getHeaders() {
                    var headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    return headers;
                }
                public InputStream getBody() {
                    return new ByteArrayInputStream(reply.body().getBytes(StandardCharsets.UTF_8));
                }
                public void close() {}
            };
        });
        var properties = new Properties();
        properties.setProperty("api.base.url", "https://example.invalid");
        properties.setProperty("api.token", "test-token");
        properties.setProperty("z.live.enabled", Boolean.toString(live));
        return new TillReportService(rest, properties);
    }

    private static Reply ok(String body) { return new Reply(HttpStatus.OK, body); }
    private record Reply(HttpStatus status, String body) {}
    private record Request(URI uri, HttpMethod method) {}
}
