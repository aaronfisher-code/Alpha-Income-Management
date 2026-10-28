package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.Invoice;
import models.CellDataPoint;
import models.ScannedInvoice;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

public class InvoiceService {
    private static final int REVIEW_BATCH_SIZE = 50;
    private String apiBaseUrl;
    private String apiRootUrl;
    private String apiToken;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public InvoiceService() throws IOException {
        this(loadProperties());
    }

    private InvoiceService(Properties properties) throws IOException {
        this(ApiHttpClient.create(properties), properties);
    }

    InvoiceService(RestTemplate restTemplate, Properties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.apiToken = properties.getProperty("api.token");
        this.apiRootUrl = properties.getProperty("api.base.url");
        this.apiBaseUrl = apiRootUrl + "/invoices";
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (var input = InvoiceService.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
        }
        return properties;
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

    /** Includes Z-Office invoices that have not yet been saved in Alpha. */
    public List<Invoice> getReconciliationTableData(int storeId, YearMonth yearMonth) {
        String url = apiBaseUrl + "/reconciliation/table-data?storeId=" + storeId + "&yearMonth=" + yearMonth;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(createHeaders()), String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Invoice>>() {});
        } catch (HttpClientErrorException.NotFound olderApi) {
            // A desktop release can be installed before the API update.
            return getInvoiceTableData(storeId, yearMonth);
        } catch (IOException exception) {
            throw new IllegalStateException("Error parsing invoice reconciliation data", exception);
        }
    }

    /** A database-only lookup for the scanned references, including invoices not yet saved in Alpha. */
    public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> rows) {
        return invoiceAmountRequest((hasCredits(rows) ? "/reconciliation/documents/cached" : "/reconciliation/cached") + "?storeId=" + storeId, rows);
    }

    /** Warm the shared window once, then use at most two simultaneous requests for the remaining rows. */
    public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> rows) {
        return loadReviewChunks(rows, chunk -> invoiceAmountRequest(
                (hasCredits(chunk) ? "/reconciliation/documents/resolve" : "/reconciliation/resolve")
                        + "?storeId=" + storeId, chunk), (completed, total) -> {});
    }

    public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> rows,
            BiConsumer<Integer, Integer> progress) {
        // Delegate each bounded request through the existing method so alternate service implementations work too.
        return loadReviewChunks(rows, chunk -> getInvoiceAmountsForReview(storeId, chunk), progress);
    }

    private List<Invoice> loadReviewChunks(List<ScannedInvoice> rows,
            Function<List<ScannedInvoice>, List<Invoice>> fetch, BiConsumer<Integer, Integer> progress) {
        List<ScannedInvoice> requested = rows.stream().filter(row -> !row.invoiceNo().isBlank()).distinct().toList();
        List<List<ScannedInvoice>> chunks = new ArrayList<>();
        for (int start = 0; start < requested.size(); start += REVIEW_BATCH_SIZE) {
            chunks.add(requested.subList(start, Math.min(start + REVIEW_BATCH_SIZE, requested.size())));
        }
        progress.accept(0, requested.size());
        if (chunks.isEmpty()) return List.of();
        List<List<Invoice>> responses = new ArrayList<>(Collections.nCopies(chunks.size(), null));
        // Let the first request populate the shared MySQL window before starting concurrent readers.
        responses.set(0, fetch.apply(chunks.getFirst()));
        int completed = chunks.getFirst().size();
        progress.accept(completed, requested.size());
        if (chunks.size() > 1) {
            var executor = Executors.newFixedThreadPool(2, Thread.ofVirtual().name("invoice-review-", 0).factory());
            var completion = new ExecutorCompletionService<ReviewChunk>(executor);
            List<Future<ReviewChunk>> pending = new ArrayList<>();
            try {
                for (int index = 1; index < chunks.size(); index++) {
                    int position = index;
                    pending.add(completion.submit(() -> new ReviewChunk(position, fetch.apply(chunks.get(position)))));
                }
                for (int remaining = chunks.size() - 1; remaining > 0; remaining--) {
                    ReviewChunk result = completion.take().get();
                    responses.set(result.index(), result.rows());
                    completed += chunks.get(result.index()).size();
                    progress.accept(completed, requested.size());
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Document review loading was interrupted", exception);
            } catch (ExecutionException exception) {
                if (exception.getCause() instanceof RuntimeException failure) throw failure;
                throw new IllegalStateException("Unable to load document review records", exception.getCause());
            } finally {
                pending.forEach(future -> future.cancel(true));
                executor.shutdownNow();
            }
        }
        // Merge in request order even when responses complete out of order. Preserve ambiguity within each response.
        Map<String, List<Invoice>> byReference = new LinkedHashMap<>();
        for (int index = 0; index < chunks.size(); index++) {
            Map<String, List<Invoice>> batch = new LinkedHashMap<>();
            responses.get(index).forEach(invoice -> batch.computeIfAbsent(utils.InvoiceReconciler.importKey(invoice),
                    ignored -> new ArrayList<>()).add(invoice));
            chunks.get(index).forEach(row -> utils.InvoiceReconciler.lookupReferences(row).forEach(reference ->
                    byReference.remove(utils.InvoiceReconciler.lookupKey(row, reference))));
            byReference.putAll(batch);
        }
        return byReference.values().stream().flatMap(List::stream).toList();
    }

    private record ReviewChunk(int index, List<Invoice> rows) {}

    public List<Invoice> refreshInvoiceAmount(int storeId, ScannedInvoice row) {
        return row.documentType() == ScannedInvoice.DocumentType.CREDIT
                ? amountRequestAt(apiRootUrl + "/credits/reconciliation/refresh?storeId=" + storeId, row)
                : invoiceAmountRequest("/reconciliation/refresh?storeId=" + storeId, row);
    }

    private List<Invoice> invoiceAmountRequest(String path, Object body) {
        return amountRequestAt(apiBaseUrl + path, body);
    }

    private static boolean hasCredits(List<ScannedInvoice> rows) {
        return rows.stream().anyMatch(row -> row.documentType() == ScannedInvoice.DocumentType.CREDIT);
    }

    private List<Invoice> amountRequestAt(String url, Object body) {
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(body, createHeaders()), String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Invoice>>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read cached document amounts", exception);
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

    public void saveOrUpdateInvoice(Invoice invoice, String originalInvoiceNo, int originalSupplierId) {
        boolean duplicate = checkDuplicateInvoice(invoice.getInvoiceNo(), invoice.getStoreID(), invoice.getSupplierID());
        if (!duplicate && originalInvoiceNo != null && !originalInvoiceNo.isBlank()
                && originalSupplierId > 0
                && (!originalInvoiceNo.equals(invoice.getInvoiceNo()) || originalSupplierId != invoice.getSupplierID())
                && checkDuplicateInvoice(originalInvoiceNo, invoice.getStoreID(), originalSupplierId)) {
            updateInvoice(invoice, originalInvoiceNo, invoice.getStoreID(), originalSupplierId);
            return;
        }
        if (!duplicate) {
            addInvoice(invoice);
            return;
        }
        // Updating by the duplicate's natural key deliberately overwrites it,
        // which also handles an accepted row whose reference did not change.
        updateInvoice(invoice, invoice.getInvoiceNo(), invoice.getStoreID(), invoice.getSupplierID());
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
