package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class GeminiInvoiceScanServiceTest {
	private static final ObjectMapper JSON = new ObjectMapper();
	private final Map<String, String> originalSettings = new java.util.HashMap<>();
	@TempDir Path temporary;

	@AfterEach void restoreSettings() {
		originalSettings.forEach((key, value) -> {
			if (value == null) System.clearProperty(key); else System.setProperty(key, value);
		});
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 5 })
	void retriesOnlyTheFailedRangeInSmallerChunksAndPreservesOriginalPages(int failedStart) throws Exception {
		settings();
		try (FakeGemini http = new FakeGemini(failedStart, false)) {
			List<Integer> progress = new CopyOnWriteArrayList<>();
			var result = service(http).scan(pdf(10).toFile(), GeminiInvoiceScanService.ScanKind.STATEMENT,
					(completed, total) -> { assertEquals(3, total); progress.add(completed); });
			assertEquals(10, result.rows().size());
			assertEquals(10, result.pageTransactionCounts().size());
			for (int page = 1; page <= 10; page++) assertEquals(1, result.pageTransactionCounts().get(page));
			assertTrue(result.rows().stream().allMatch(row -> row.sourceFile().equals("statement.pdf")));
			for (var row : result.rows()) {
				int page = Integer.parseInt(row.invoiceNo().substring(4));
				assertEquals(page - 1, result.locations().get(row).get(models.PdfEvidenceField.REFERENCE).pageIndex());
			}
			assertEquals(List.of(1, 2, 3), progress);
			assertEquals(5, http.requests.size()); // Three original chunks plus two smaller retries.
			assertEquals(1, http.requests.stream().filter(request -> startPage(request) == failedStart
					&& pageCount(request) == 4).count());
			assertEquals(1, http.requests.stream().filter(request -> startPage(request) == failedStart
					&& pageCount(request) == 2).count());
			assertEquals(1, http.requests.stream().filter(request -> startPage(request) == failedStart + 2
					&& pageCount(request) == 2).count());
			assertEquals(http.uploads.get(), http.deleted.get());
		}
	}

	@Test
	void failedSmallerChunksStopWithoutAcceptingPartialDataOrStartingFurtherRetryTrees() throws Exception {
		settings();
		try (FakeGemini http = new FakeGemini(1, true)) {
			IOException failure = assertThrows(IOException.class,
					() -> service(http).scan(pdf(4).toFile(), GeminiInvoiceScanService.ScanKind.STATEMENT));
			assertTrue(failure.getMessage().contains("pages 1–2"));
			assertTrue(failure.getMessage().contains("accounted for 1 of 2 pages"));
			assertEquals(2, http.requests.size());
			assertEquals(http.uploads.get(), http.deleted.get());
		}
	}

	@Test
	void coverageErrorsDescribeMissingPageNumbersAndCountMismatches() throws Exception {
		try (FakeGemini http = new FakeGemini(0, false)) {
			var scanner = service(http);
			ObjectNode missing = annotation(4, 1);
			((ObjectNode) missing.path("transactions").get(1)).putNull("page_number");
			assertEquals("transaction 2 has a missing or invalid page_number",
					scanner.parseResponse(missing.toString(), "unavailable.pdf").pageCoverageFailure());
			ObjectNode mismatch = annotation(4, 1);
			((ObjectNode) mismatch.path("page_checks").get(2)).put("relevant_transaction_count", 2);
			assertEquals("page 3 declares 2 transactions but 1 were returned",
					scanner.parseResponse(mismatch.toString(), "unavailable.pdf").pageCoverageFailure());
			assertTrue(scanner.parseResponse(mismatch.toString(), "unavailable.pdf").pageTransactionCounts().isEmpty());
			assertEquals("", scanner.parseResponse(annotation(4, 1).toString(), "unavailable.pdf").pageCoverageFailure());
		}
	}

	@Test
	void pageContainersCalculateCountsAndRetainBlankAmountsWithoutLosingEmptyPages() throws Exception {
		try (FakeGemini http = new FakeGemini(0, false)) {
			ObjectNode grouped = annotation(1, 1);
			var rows = (com.fasterxml.jackson.databind.node.ArrayNode) grouped.path("transactions");
			ObjectNode template = ((ObjectNode) rows.get(0)).deepCopy();
			rows.removeAll();
			for (int index = 0; index < 70; index++) {
				ObjectNode row = template.deepCopy();
				row.put("reference", "INV-" + index);
				if (index < 16) {
					row.put("amount", "");
					row.putObject("field_confidence").put("amount", 0);
				}
				((ObjectNode) row.path("evidence").path("reference")).remove("page_number");
				rows.add(row);
			}
			grouped = grouped(grouped, 2);
			var result = service(http).parseResponse(grouped.toString(), "unavailable.pdf");
			assertEquals(70, result.rows().size());
			assertEquals(Map.of(1, 70, 2, 0), result.pageTransactionCounts());
			assertEquals("", result.pageCoverageFailure());
			assertEquals(16, result.rows().stream().filter(row -> row.amountCents() == 0
					&& Double.valueOf(0).equals(row.fieldConfidences().amount())).count());
			((ObjectNode) grouped.path("pages").get(1)).put("page_number", 1);
			assertTrue(service(http).parseResponse(grouped.toString(), "unavailable.pdf")
					.pageCoverageFailure().contains("appears twice"));
		}
	}

	private void settings() {
		for (var setting : Map.of("gemini.statement.pages-per-request", "4",
				"gemini.statement.completeness-attempts", "2", "gemini.statement.parallel.max-concurrent", "2").entrySet()) {
			originalSettings.put(setting.getKey(), System.getProperty(setting.getKey()));
			System.setProperty(setting.getKey(), setting.getValue());
		}
	}

	@Test
	void selectsPayableBeforeDiscountAndIgnoresTaxSummaryEvenWhenMisclassified() throws Exception {
		try (FakeGemini http = new FakeGemini(0, false)) {
			ObjectNode response = grouped(annotation(3, 1), 3);
			ObjectNode cover = (ObjectNode) response.path("pages").get(0);
			cover.put("page_type", "statement_cover");
			addTotal(cover, "103817.11", "Total Amount Payable", "amount_payable").put("confidence", 0.9);
			addTotal(cover, "102940.74", "Net Amount Payable", "amount_payable");
			ObjectNode summary = (ObjectNode) response.path("pages").get(1);
			summary.put("page_type", "statement_summary");
			addTotal(summary, "103817.11", "Total Amount Payable", "amount_payable");
			ObjectNode tax = (ObjectNode) response.path("pages").get(2);
			tax.put("page_type", "tax_summary");
			addTotal(tax, "102940.74", "Statement Balance", "amount_payable");
			var result = service(http).parseResponse(response.toString(), "unavailable.pdf");
			assertEquals(10381711L, result.statementAmountCents());
			assertEquals(0.99, result.statementAmountConfidence());
			assertEquals(4, result.statementTotals().size());
			// Keep a genuine variance; the chosen total must not be fitted to the row sum.
			assertEquals(30000, result.rows().stream().mapToLong(row -> row.amountCents()).sum());
		}
	}

	@Test
	void conflictingPayableTotalsRemainUnavailableInsteadOfChoosingTheClosestToTransactions() throws Exception {
		try (FakeGemini http = new FakeGemini(0, false)) {
			ObjectNode response = grouped(annotation(2, 1), 2);
			addTotal((ObjectNode) response.path("pages").get(0), "200.00", "Amount Payable", "amount_payable");
			addTotal((ObjectNode) response.path("pages").get(1), "250.00", "Amount Payable", "amount_payable");
			var result = service(http).parseResponse(response.toString(), "unavailable.pdf");
			assertEquals(20000, result.rows().stream().mapToLong(row -> row.amountCents()).sum());
			assertNull(result.statementAmountCents());
			assertNull(result.statementAmountConfidence());
			assertEquals(2, result.statementTotals().size());
		}
	}

	@Test
	void discountedTaxOpeningAndPageTotalsCannotBecomeThePrintedStatementTotal() throws Exception {
		try (FakeGemini http = new FakeGemini(0, false)) {
			ObjectNode response = grouped(annotation(1, 1), 1);
			ObjectNode page = (ObjectNode) response.path("pages").get(0);
			addTotal(page, "90.00", "Pay by due date", "discounted_payable");
			addTotal(page, "90.00", "Statement Balance", "tax_balance");
			addTotal(page, "100.00", "Opening Balance", "opening_balance");
			addTotal(page, "100.00", "Page Total", "page_subtotal");
			response.put("statement_amount", "90.00"); // Stale legacy fields must not override classified pages.
			var result = service(http).parseResponse(response.toString(), "unavailable.pdf");
			assertNull(result.statementAmountCents());
			assertNull(result.statementAmountConfidence());
		}
	}

	@Test
	void wholeStatementTotalsTakePrecedenceOverOtherClosingBalances() throws Exception {
		try (FakeGemini http = new FakeGemini(0, false)) {
			ObjectNode response = grouped(annotation(2, 1), 2);
			addTotal((ObjectNode) response.path("pages").get(0), "150.00", "Statement Total", "statement_total");
			addTotal((ObjectNode) response.path("pages").get(1), "200.00", "Closing Balance", "closing_balance");
			assertEquals(15000L, service(http).parseResponse(response.toString(), "unavailable.pdf").statementAmountCents());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "0.00", "-25.50" })
	void legitimateClosingBalancesCanBeZeroOrNegativeAndIncludeGst(String amount) throws Exception {
		try (FakeGemini http = new FakeGemini(0, false)) {
			ObjectNode response = grouped(annotation(1, 1), 1);
			ObjectNode page = (ObjectNode) response.path("pages").get(0);
			page.put("page_type", "statement_summary");
			addTotal(page, amount, "Closing balance including GST", "closing_balance");
			assertEquals(new java.math.BigDecimal(amount).movePointRight(2).longValueExact(),
					service(http).parseResponse(response.toString(), "unavailable.pdf").statementAmountCents());
		}
	}

	@Test
	void legacyAmountsRemainReadableWhenNoClassifiedPageTotalsExist() throws Exception {
		try (FakeGemini http = new FakeGemini(0, false)) {
			ObjectNode response = annotation(1, 1);
			response.put("statement_amount", "100.00");
			response.put("statement_amount_confidence", 0.8);
			var result = service(http).parseResponse(response.toString(), "unavailable.pdf");
			assertEquals(10000L, result.statementAmountCents());
			assertEquals(0.8, result.statementAmountConfidence());
		}
	}

	@Test
	void payableSelectionAndPageProvenanceSurviveChunkRetriesAndMerging() throws Exception {
		settings();
		try (FakeGemini http = new FakeGemini(5, false, true)) {
			var result = service(http).scan(pdf(10).toFile(), GeminiInvoiceScanService.ScanKind.STATEMENT);
			assertEquals(10381711L, result.statementAmountCents());
			assertTrue(result.statementTotals().stream().anyMatch(total -> total.pageNumber() == 8
					&& total.amountCents() == 10381711L));
			assertTrue(result.statementTotals().stream().anyMatch(total -> total.pageNumber() == 9
					&& total.amountCents() == 10294074L && total.pageType().equals("tax_summary")));
			assertEquals(10, result.rows().size());
		}
	}

	private Path pdf(int pages) throws IOException {
		Path path = temporary.resolve("statement.pdf");
		try (PDDocument document = new PDDocument()) {
			for (int page = 0; page < pages; page++) document.addPage(new PDPage());
			document.save(path.toFile());
		}
		return path;
	}

	private static GeminiInvoiceScanService service(HttpClient http) {
		return new GeminiInvoiceScanService("test-key", "gemini-test", URI.create("https://gemini.invalid"),
				http, JSON, Duration.ofSeconds(30), Duration.ofSeconds(30), "low");
	}

	private static int pageCount(JsonNode request) {
		return request.path("generationConfig").path("responseFormat").path("text").path("schema")
				.path("properties").path("pages").path("minItems").asInt();
	}

	private static int startPage(JsonNode request) {
		String prompt = request.path("contents").get(0).path("parts").get(0).path("text").asText();
		var match = Pattern.compile("original statement pages (\\d+) through").matcher(prompt);
		if (!match.find()) throw new AssertionError("Missing original page range");
		return Integer.parseInt(match.group(1));
	}

	private static ObjectNode annotation(int pages, int start) {
		ObjectNode root = JSON.createObjectNode();
		root.put("supplier_name", "Supplier");
		root.put("document_date", "2026-10-31");
		root.put("statement_period_start", "2026-10-01");
		root.put("statement_period_end", "2026-10-31");
		var checks = root.putArray("page_checks");
		var rows = root.putArray("transactions");
		for (int page = 1; page <= pages; page++) {
			checks.addObject().put("page_number", page).put("relevant_transaction_count", 1);
			ObjectNode row = rows.addObject();
			row.put("reference", "INV-" + (start + page - 1));
			row.put("transaction_date", "2026-10-10");
			row.put("transaction_type", "invoice");
			row.put("amount", "100.00");
			row.put("page_number", page);
			var box = row.putObject("evidence").putObject("reference");
			box.put("page_number", page);
			box.putArray("box_2d").add(100).add(100).add(200).add(200);
		}
		return root;
	}

	private static ObjectNode grouped(ObjectNode annotation, int count) {
		ObjectNode grouped = annotation.deepCopy();
		grouped.remove("transactions");
		grouped.remove("page_checks");
		var pages = grouped.putArray("pages");
		for (int page = 1; page <= count; page++) {
			ObjectNode entry = pages.addObject().put("page_number", page).put("page_type", "transaction_table");
			entry.putArray("statement_totals");
			var rows = entry.putArray("transactions");
			for (JsonNode row : annotation.path("transactions")) {
				if (row.path("page_number").asInt() == page) {
					ObjectNode copy = ((ObjectNode) row).deepCopy();
					copy.remove("page_number");
					for (JsonNode box : copy.path("evidence")) {
						if (box.isObject()) ((ObjectNode) box).remove("page_number");
					}
					rows.add(copy);
				}
			}
		}
		return grouped;
	}

	private static ObjectNode addTotal(ObjectNode page, String amount, String label, String kind) {
		return ((com.fasterxml.jackson.databind.node.ArrayNode) page.path("statement_totals"))
				.addObject().put("amount", amount).put("label", label).put("kind", kind).put("confidence", 0.99);
	}

	private static final class FakeGemini extends HttpClient {
		final List<JsonNode> requests = new CopyOnWriteArrayList<>();
		final AtomicInteger uploads = new AtomicInteger();
		final AtomicInteger deleted = new AtomicInteger();
		private final int failedStart;
		private final boolean alwaysInvalid;
		private final boolean totalsCase;
		private final AtomicBoolean failed = new AtomicBoolean();
		private final HttpClient defaults = HttpClient.newHttpClient();

		FakeGemini(int failedStart, boolean alwaysInvalid) { this(failedStart, alwaysInvalid, false); }
		FakeGemini(int failedStart, boolean alwaysInvalid, boolean totalsCase) {
			this.failedStart = failedStart; this.alwaysInvalid = alwaysInvalid; this.totalsCase = totalsCase;
		}

		@Override @SuppressWarnings("unchecked")
		public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
			Map<String, List<String>> headers = Map.of();
			String body;
			if (request.method().equals("DELETE")) { deleted.incrementAndGet(); body = null; }
			else if (request.uri().getPath().equals("/upload/v1beta/files")) {
				headers = Map.of("X-Goog-Upload-URL", List.of("https://gemini.invalid/upload-data")); body = "{}";
			} else if (request.uri().getPath().equals("/upload-data")) {
				int id = uploads.incrementAndGet();
				body = "{\"file\":{\"uri\":\"https://gemini.invalid/files/" + id + "\",\"name\":\"files/" + id + "\",\"state\":\"ACTIVE\"}}";
			} else {
				var subscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
				request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
					public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) { subscriber.onSubscribe(subscription); }
					public void onNext(java.nio.ByteBuffer buffer) { subscriber.onNext(List.of(buffer)); }
					public void onError(Throwable error) { subscriber.onError(error); }
					public void onComplete() { subscriber.onComplete(); }
				});
				JsonNode json = JSON.readTree(subscriber.getBody().toCompletableFuture().join());
				requests.add(json);
				int pages = pageCount(json);
				assertTrue(pages > 0);
				JsonNode schema = json.path("generationConfig").path("responseFormat").path("text").path("schema").path("properties");
				assertEquals(pages, schema.path("pages").path("maxItems").asInt());
				assertEquals(pages, schema.path("pages").path("items").path("properties").path("page_number").path("maximum").asInt());
				assertFalse(schema.path("pages").path("items").path("properties").path("transactions")
						.path("items").path("properties").has("page_number"));
				assertFalse(schema.has("statement_amount"));
				assertTrue(schema.path("pages").path("items").path("properties").has("statement_totals"));
				ObjectNode result = grouped(annotation(pages, startPage(json)), pages);
				if (totalsCase) {
					for (int index = 0; index < pages; index++) {
						int originalPage = startPage(json) + index;
						ObjectNode page = (ObjectNode) result.path("pages").get(index);
						if (originalPage == 1 || originalPage == 8) {
							page.put("page_type", originalPage == 1 ? "statement_cover" : "statement_summary");
							addTotal(page, "103817.11", "Total Amount Payable", "amount_payable");
							if (originalPage == 1) addTotal(page, "102940.74", "Net Amount Payable", "discounted_payable");
						} else if (originalPage == 9) {
							page.put("page_type", "tax_summary");
							addTotal(page, "102940.74", "Statement Balance", "closing_balance");
						}
					}
				}
				if (alwaysInvalid || (pages == 4 && startPage(json) == failedStart && failed.compareAndSet(false, true))) {
					((com.fasterxml.jackson.databind.node.ArrayNode) result.path("pages")).remove(pages - 1);
				}
				ObjectNode response = JSON.createObjectNode();
				var candidate = response.putArray("candidates").addObject();
				candidate.put("finishReason", "STOP");
				candidate.putObject("content").putArray("parts").addObject().put("text", result.toString());
				body = response.toString();
			}
			return (HttpResponse<T>) new Response(body, request, HttpHeaders.of(headers, (name, value) -> true));
		}

		@Override public Optional<CookieHandler> cookieHandler() { return defaults.cookieHandler(); }
		@Override public Optional<Duration> connectTimeout() { return defaults.connectTimeout(); }
		@Override public Redirect followRedirects() { return defaults.followRedirects(); }
		@Override public Optional<ProxySelector> proxy() { return defaults.proxy(); }
		@Override public SSLContext sslContext() { return defaults.sslContext(); }
		@Override public SSLParameters sslParameters() { return defaults.sslParameters(); }
		@Override public Optional<Authenticator> authenticator() { return defaults.authenticator(); }
		@Override public Version version() { return defaults.version(); }
		@Override public Optional<Executor> executor() { return defaults.executor(); }
		@Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) { throw new UnsupportedOperationException(); }
		@Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> push) { throw new UnsupportedOperationException(); }
		@Override public void close() { defaults.close(); }
	}

	private record Response(String body, HttpRequest request, HttpHeaders headers) implements HttpResponse<String> {
		public int statusCode() { return 200; }
		public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
		public Optional<SSLSession> sslSession() { return Optional.empty(); }
		public URI uri() { return request.uri(); }
		public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
	}
}
