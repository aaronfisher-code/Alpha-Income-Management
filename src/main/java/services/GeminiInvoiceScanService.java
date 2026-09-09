package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.PdfEvidenceField;
import models.PdfEvidenceLocation;
import models.ScannedInvoice;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Gemini Flash document adapter for invoice and supplier-statement extraction.
 *
 * <p>The Gemini Files API is used instead of embedding PDF bytes in requests.
 * Supplier statements are physically divided into small page chunks so every
 * page must produce an explicit completion record; independent chunks are
 * extracted concurrently and mapped back to the original PDF. The structured
 * response includes best-estimate field boxes in the image-understanding
 * coordinate system ([ymin, xmin, ymax, xmax], normalized to 0..1000).</p>
 */
public final class GeminiInvoiceScanService {
	public enum ScanKind {
		STATEMENT,
		INDIVIDUAL_INVOICE
	}

	/** Receives completion updates for a single PDF, including statement chunks. */
	@FunctionalInterface
	public interface ScanProgressListener {
		void onProgress(int completed, int total);
	}

	private static final URI DEFAULT_API_BASE = URI.create("https://generativelanguage.googleapis.com");
	public static final long MAX_PDF_BYTES = 50L * 1024 * 1024;
	public static final int MAX_PDF_PAGES = 1_000;
	private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(10);
	private static final Duration DEFAULT_FILE_PROCESSING_TIMEOUT = Duration.ofMinutes(5);
	private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 5;
	private static final int DEFAULT_RETRY_INITIAL_DELAY_SECONDS = 2;
	private static final int DEFAULT_RETRY_MAX_DELAY_SECONDS = 30;
	private static final int DEFAULT_PARALLEL_SCAN_LIMIT = 3;
	private static final int DEFAULT_STATEMENT_PAGES_PER_REQUEST = 4;
	private static final int DEFAULT_STATEMENT_COMPLETENESS_ATTEMPTS = 2;
	private static final String DEFAULT_MODEL = "gemini-3.8-flash";
	private static final String DEFAULT_STATEMENT_MODEL = "gemini-3.8-flash";
	private static final int DEFAULT_STATEMENT_MAX_OUTPUT_TOKENS = 65_536;
	private static final String DEFAULT_THINKING_LEVEL = "low";
	private static final String DEFAULT_SERVICE_TIER = "standard";
	private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
			DateTimeFormatter.ISO_LOCAL_DATE,
			DateTimeFormatter.ofPattern("d/M/uuuu"),
			DateTimeFormatter.ofPattern("d-M-uuuu"),
			DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("d.MM.uuuu"));

	/**
	 * Gemini's structured-output schema is deliberately self-contained. Some
	 * Gemini API versions support a smaller JSON-Schema subset, so field boxes
	 * are repeated rather than expressed through $defs/$ref.
	 */
	private static final String SCHEMA = """
			{
			  "type": "object",
			  "additionalProperties": false,
			  "required": ["supplier_name", "document_date", "statement_period_start", "statement_period_end", "statement_amount", "statement_amount_confidence", "page_checks", "transactions"],
			  "properties": {
			    "supplier_name": {"type": "string", "description": "Supplier or issuer name"},
			    "document_date": {"type": ["string", "null"], "description": "Statement or invoice date in YYYY-MM-DD when visible"},
			    "statement_period_start": {"type": ["string", "null"], "description": "Inferred supplier-statement reporting-period start in YYYY-MM-DD; null for an individual invoice"},
			    "statement_period_end": {"type": ["string", "null"], "description": "Inferred supplier-statement reporting-period end in YYYY-MM-DD; null for an individual invoice"},
			    "statement_amount": {"type": ["string", "null"], "description": "Signed printed overall statement amount, closing balance, or amount due for the complete statement; null when this chunk does not show one or for an individual invoice"},
			    "statement_amount_confidence": {"type": ["number", "null"], "minimum": 0, "maximum": 1, "description": "Confidence in the extracted statement_amount"},
			    "page_checks": {
			      "type": "array",
			      "description": "Exactly one entry for every page in the supplied PDF, including pages with zero relevant rows",
			      "items": {
			        "type": "object",
			        "additionalProperties": false,
			        "required": ["page_number", "relevant_transaction_count"],
			        "properties": {
			          "page_number": {"type": "integer", "minimum": 1},
			          "relevant_transaction_count": {"type": "integer", "minimum": 0}
			        }
			      }
			    },
			    "transactions": {
			      "type": "array",
			      "items": {
			        "type": "object",
			        "additionalProperties": false,
			        "required": ["supplier_name", "reference", "transaction_date", "due_date", "transaction_type", "amount", "field_confidence", "page_number", "evidence"],
			        "properties": {
			          "supplier_name": {"type": ["string", "null"]},
			          "reference": {"type": "string"},
			          "transaction_date": {"type": ["string", "null"]},
			          "due_date": {"type": ["string", "null"]},
			          "transaction_type": {"type": "string", "enum": ["invoice", "credit"]},
			          "amount": {"type": "string", "description": "Signed gross total including GST, as a decimal string"},
			          "field_confidence": {
			            "type": "object",
			            "additionalProperties": false,
			            "required": ["supplier", "reference", "date", "due_date", "type", "amount"],
			            "properties": {
			              "supplier": {"type": ["number", "null"], "minimum": 0, "maximum": 1},
			              "reference": {"type": ["number", "null"], "minimum": 0, "maximum": 1},
			              "date": {"type": ["number", "null"], "minimum": 0, "maximum": 1},
			              "due_date": {"type": ["number", "null"], "minimum": 0, "maximum": 1},
			              "type": {"type": ["number", "null"], "minimum": 0, "maximum": 1},
			              "amount": {"type": ["number", "null"], "minimum": 0, "maximum": 1}
			            }
			          },
			          "page_number": {"type": ["integer", "null"], "minimum": 1, "description": "One-based PDF page containing this transaction"},
			          "evidence": {
			            "type": "object",
			            "additionalProperties": false,
			            "required": ["supplier", "reference", "date", "amount"],
			            "properties": {
			              "supplier": {
			                "type": ["object", "null"],
			                "additionalProperties": false,
			                "required": ["page_number", "box_2d"],
			                "properties": {
			                  "page_number": {"type": "integer", "minimum": 1},
			                  "box_2d": {"type": "array", "items": {"type": "integer"}, "minItems": 4, "maxItems": 4, "description": "[ymin, xmin, ymax, xmax] normalized to 0..1000"}
			                }
			              },
			              "reference": {
			                "type": ["object", "null"],
			                "additionalProperties": false,
			                "required": ["page_number", "box_2d"],
			                "properties": {
			                  "page_number": {"type": "integer", "minimum": 1},
			                  "box_2d": {"type": "array", "items": {"type": "integer"}, "minItems": 4, "maxItems": 4, "description": "[ymin, xmin, ymax, xmax] normalized to 0..1000"}
			                }
			              },
			              "date": {
			                "type": ["object", "null"],
			                "additionalProperties": false,
			                "required": ["page_number", "box_2d"],
			                "properties": {
			                  "page_number": {"type": "integer", "minimum": 1},
			                  "box_2d": {"type": "array", "items": {"type": "integer"}, "minItems": 4, "maxItems": 4, "description": "[ymin, xmin, ymax, xmax] normalized to 0..1000"}
			                }
			              },
			              "amount": {
			                "type": ["object", "null"],
			                "additionalProperties": false,
			                "required": ["page_number", "box_2d"],
			                "properties": {
			                  "page_number": {"type": "integer", "minimum": 1},
			                  "box_2d": {"type": "array", "items": {"type": "integer"}, "minItems": 4, "maxItems": 4, "description": "[ymin, xmin, ymax, xmax] normalized to 0..1000"}
			                }
			              }
			            }
			          }
			        }
			      }
			    }
			  }
			}
			""";

	private final String apiKey;
	private final String model;
	private final URI apiBase;
	private final HttpClient http;
	private final ObjectMapper mapper;
	private final Duration requestTimeout;
	private final Duration fileProcessingTimeout;
	private final String thinkingLevel;
	private final String serviceTier;

	public GeminiInvoiceScanService() {
		this(
				runtimeSetting("gemini.api.key", "GEMINI_API_KEY", "gemini.api.key", ""),
				runtimeSetting("gemini.model", "GEMINI_MODEL", "gemini.model", DEFAULT_MODEL),
				URI.create(trimTrailingSlash(runtimeSetting("gemini.api.base-url", "GEMINI_API_BASE_URL", "gemini.api.base-url", DEFAULT_API_BASE.toString()))),
				HttpClient.newBuilder()
						.connectTimeout(Duration.ofSeconds(30))
						.followRedirects(HttpClient.Redirect.NORMAL)
						.build(),
				new ObjectMapper(),
				requestTimeout(),
				fileProcessingTimeout(),
				runtimeSetting("gemini.thinking-level", "GEMINI_THINKING_LEVEL", "gemini.thinking-level", DEFAULT_THINKING_LEVEL),
				runtimeSetting("gemini.service-tier", "GEMINI_SERVICE_TIER", "gemini.service-tier", DEFAULT_SERVICE_TIER));
	}

	GeminiInvoiceScanService(String apiKey, String model, URI apiBase, HttpClient http, ObjectMapper mapper,
			Duration requestTimeout, Duration fileProcessingTimeout, String thinkingLevel) {
		this(apiKey, model, apiBase, http, mapper, requestTimeout, fileProcessingTimeout, thinkingLevel,
				DEFAULT_SERVICE_TIER);
	}

	GeminiInvoiceScanService(String apiKey, String model, URI apiBase, HttpClient http, ObjectMapper mapper,
			Duration requestTimeout, Duration fileProcessingTimeout, String thinkingLevel, String serviceTier) {
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
		this.apiBase = apiBase == null ? DEFAULT_API_BASE : URI.create(trimTrailingSlash(apiBase.toString()));
		this.http = http == null ? HttpClient.newHttpClient() : http;
		this.mapper = mapper == null ? new ObjectMapper() : mapper;
		this.requestTimeout = validDuration(requestTimeout, DEFAULT_REQUEST_TIMEOUT);
		this.fileProcessingTimeout = validDuration(fileProcessingTimeout, DEFAULT_FILE_PROCESSING_TIMEOUT);
		this.thinkingLevel = thinkingLevel == null || thinkingLevel.isBlank() ? DEFAULT_THINKING_LEVEL : thinkingLevel.trim().toLowerCase(Locale.ROOT);
		this.serviceTier = normalizeServiceTier(serviceTier);
	}

	/** Convenience constructor useful for local tests and provider smoke checks. */
	GeminiInvoiceScanService(String apiKey, String model, URI apiBase, HttpClient http, ObjectMapper mapper,
			Duration requestTimeout) {
		this(apiKey, model, apiBase, http, mapper, requestTimeout, DEFAULT_FILE_PROCESSING_TIMEOUT, DEFAULT_THINKING_LEVEL);
	}

	public boolean isConfigured() {
		return !apiKey.isBlank();
	}

	/** Bounded concurrency for independent invoice PDFs submitted together. */
	public int parallelScanLimit() {
		return configuredInteger("gemini.parallel.max-concurrent", "GEMINI_PARALLEL_MAX_CONCURRENT",
				DEFAULT_PARALLEL_SCAN_LIMIT, 1, 8);
	}

	/**
	 * Uploads and scans one complete PDF. The caller should run this method off
	 * the JavaFX application thread because PDF upload and model inference are
	 * intentionally allowed to take several minutes for large statements.
	 */
	public ScanResult scan(File pdf, ScanKind kind) throws IOException, InterruptedException {
		return scan(pdf, kind, (ScanProgressListener) null);
	}

	/**
	 * Uploads and scans one PDF while reporting completed chunks. For an
	 * unchunked invoice or statement this reports a single completed unit.
	 */
	public ScanResult scan(File pdf, ScanKind kind, ScanProgressListener progress)
			throws IOException, InterruptedException {
		int pageCount = validatePdf(pdf);
		if (!isConfigured()) {
			throw new IllegalStateException("Gemini Flash is not configured. Set gemini.api.key in application.properties or GEMINI_API_KEY in the environment.");
		}
		int pagesPerRequest = configuredInteger("gemini.statement.pages-per-request",
				"GEMINI_STATEMENT_PAGES_PER_REQUEST", DEFAULT_STATEMENT_PAGES_PER_REQUEST, 1, 12);
		if (kind == ScanKind.STATEMENT && pageCount > pagesPerRequest) {
			return scanStatementChunks(pdf, pageCount, pagesPerRequest, progress);
		}
		ScanResult result = scanUploadedPdf(pdf, kind, null, 0, pageCount, kind == ScanKind.STATEMENT);
		notifyProgress(progress, 1, 1);
		return result;
	}

	private ScanResult scanUploadedPdf(File pdf, ScanKind kind, StatementPeriod expectedPeriod,
			int originalPageOffset, int pageCount, boolean requirePageChecks)
			throws IOException, InterruptedException {
		UploadedFile uploaded = upload(pdf);
		try {
			int completenessAttempts = requirePageChecks
					? configuredInteger("gemini.statement.completeness-attempts",
							"GEMINI_STATEMENT_COMPLETENESS_ATTEMPTS",
							DEFAULT_STATEMENT_COMPLETENESS_ATTEMPTS, 1, 4)
					: 1;
			String completenessFailure = "";
			for (int attempt = 1; attempt <= completenessAttempts; attempt++) {
				String response = generate(uploaded.uri(), kind, expectedPeriod, pageCount,
						originalPageOffset, attempt > 1);
				ScanResult parsed = parseResponse(response, pdf);
				if (expectedPeriod != null) {
					parsed = new ScanResult(parsed.rows(), parsed.locations(), expectedPeriod.start(),
							expectedPeriod.end(), parsed.statementAmountCents(),
							parsed.statementAmountConfidence(), parsed.pageTransactionCounts());
				}
				if (!requirePageChecks || (completenessFailure = pageCompletenessFailure(parsed, pageCount)).isBlank()) {
					ScanResult filtered = kind == ScanKind.STATEMENT ? forPeriod(parsed) : parsed;
					return shiftToOriginalPages(filtered, originalPageOffset);
				}
				if (attempt < completenessAttempts) {
					System.err.println("Gemini statement chunk did not pass page coverage validation ("
							+ completenessFailure + "); retrying extraction attempt " + (attempt + 1)
							+ " of " + completenessAttempts + ".");
				}
			}
			throw new IOException("Gemini could not verify every page in statement pages "
					+ (originalPageOffset + 1) + "–" + (originalPageOffset + pageCount)
					+ " after " + completenessAttempts + " attempts: " + completenessFailure);
		} finally {
			// Uploaded Files API objects are temporary inputs. Delete them as soon
			// as extraction completes instead of retaining invoice data for the
			// service's automatic 48-hour expiry window.
			if (!uploaded.name().isBlank()) deleteFile(uploaded.name());
		}
	}

	private ScanResult scanStatementChunks(File sourcePdf, int pageCount, int pagesPerRequest,
			ScanProgressListener progress)
			throws IOException, InterruptedException {
		List<PdfChunk> chunks = createPdfChunks(sourcePdf, pageCount, pagesPerRequest);
		try {
			PdfChunk firstChunk = chunks.get(0);
			ScanResult first = scanUploadedPdf(firstChunk.file(), ScanKind.STATEMENT, null,
					firstChunk.startPageIndex(), firstChunk.pageCount(), true);
			notifyProgress(progress, 1, chunks.size());
			if (first.periodStart() == null || first.periodEnd() == null) {
				throw new IOException("Gemini could not determine the statement period from pages 1–"
						+ firstChunk.pageCount());
			}
			StatementPeriod period = new StatementPeriod(first.periodStart(), first.periodEnd());
			List<ScanResult> ordered = new ArrayList<>(Collections.nCopies(chunks.size(), null));
			ordered.set(0, first);
			if (chunks.size() > 1) scanRemainingChunks(chunks, period, ordered, progress);
			return withSourceFile(mergeChunkResults(ordered, period), sourcePdf.getName());
		} finally {
			for (PdfChunk chunk : chunks) {
				try {
					Files.deleteIfExists(chunk.file().toPath());
				} catch (IOException ignored) {
					// Temporary chunk cleanup is best effort.
				}
			}
			if (!chunks.isEmpty()) {
				try {
					Files.deleteIfExists(chunks.get(0).file().toPath().getParent());
				} catch (IOException ignored) {
					// Temporary directory cleanup is best effort.
				}
			}
		}
	}

	private static List<PdfChunk> createPdfChunks(File sourcePdf, int pageCount, int pagesPerRequest)
			throws IOException {
		Path temporaryDirectory = Files.createTempDirectory("alpha-gemini-statement-");
		List<PdfChunk> chunks = new ArrayList<>();
		try (PDDocument source = PDDocument.load(sourcePdf)) {
			for (int start = 0; start < pageCount; start += pagesPerRequest) {
				int count = Math.min(pagesPerRequest, pageCount - start);
				Path chunkPath = temporaryDirectory.resolve(String.format(Locale.ROOT,
						"statement-pages-%04d-%04d.pdf", start + 1, start + count));
				try (PDDocument chunk = new PDDocument()) {
					for (int page = start; page < start + count; page++) {
						chunk.importPage(source.getPage(page));
					}
					chunk.save(chunkPath.toFile());
				}
				chunks.add(new PdfChunk(chunkPath.toFile(), start, count));
			}
			return chunks;
		} catch (IOException | RuntimeException exception) {
			for (PdfChunk chunk : chunks) {
				try {
					Files.deleteIfExists(chunk.file().toPath());
				} catch (IOException ignored) {
					// Preserve the original chunking error.
				}
			}
			try {
				Files.deleteIfExists(temporaryDirectory);
			} catch (IOException ignored) {
				// Preserve the original chunking error.
			}
			if (exception instanceof IOException io) throw io;
			throw new IOException("Could not split the supplier statement into page chunks", exception);
		}
	}

	private static String pageCompletenessFailure(ScanResult result, int expectedPageCount) {
		Map<Integer, Integer> counts = result.pageTransactionCounts();
		if (counts.size() != expectedPageCount) {
			return "Gemini accounted for " + counts.size() + " of " + expectedPageCount + " pages";
		}
		long total = 0;
		for (int page = 1; page <= expectedPageCount; page++) {
			Integer count = counts.get(page);
			if (count == null) return "page " + page + " is missing from page_checks";
			if (count < 0) return "page " + page + " has a negative transaction count";
			total += count;
		}
		if (total != result.rows().size()) {
			return "page_checks total " + total + " does not match " + result.rows().size()
					+ " returned transactions";
		}
		return "";
	}

	private static ScanResult shiftToOriginalPages(ScanResult result, int originalPageOffset) {
		if (originalPageOffset == 0) return result;
		Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new LinkedHashMap<>();
		result.locations().forEach((row, fields) -> {
			EnumMap<PdfEvidenceField, PdfEvidenceLocation> shifted = new EnumMap<>(PdfEvidenceField.class);
			fields.forEach((field, location) -> shifted.put(field, new PdfEvidenceLocation(
					location.pageIndex() + originalPageOffset, location.x(), location.y(),
					location.width(), location.height(), location.pageWidth(), location.pageHeight())));
			locations.put(row, shifted);
		});
		Map<Integer, Integer> pageCounts = new LinkedHashMap<>();
		result.pageTransactionCounts().forEach((page, count) ->
				pageCounts.put(page + originalPageOffset, count));
		return new ScanResult(result.rows(), locations, result.periodStart(), result.periodEnd(),
				result.statementAmountCents(), result.statementAmountConfidence(), pageCounts);
	}

	private static ScanResult mergeChunkResults(List<ScanResult> chunks, StatementPeriod period)
			throws IOException {
		List<ScannedInvoice> rows = new ArrayList<>();
		Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new LinkedHashMap<>();
		Map<Integer, Integer> pageCounts = new LinkedHashMap<>();
		Long statementAmountCents = null;
		Double statementAmountConfidence = null;
		for (ScanResult chunk : chunks) {
			if (chunk == null) throw new IOException("A statement page chunk completed without a result");
			rows.addAll(chunk.rows());
			locations.putAll(chunk.locations());
			for (Map.Entry<Integer, Integer> entry : chunk.pageTransactionCounts().entrySet()) {
				if (pageCounts.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
					throw new IOException("Statement page " + entry.getKey() + " was extracted more than once");
				}
			}
			// Overall totals most commonly appear on the final statement page, so
			// a later explicit value takes precedence over a repeated page header.
			if (chunk.statementAmountCents() != null) {
				statementAmountCents = chunk.statementAmountCents();
				statementAmountConfidence = chunk.statementAmountConfidence();
			}
		}
		return new ScanResult(rows, locations, period.start(), period.end(), statementAmountCents,
				statementAmountConfidence, pageCounts);
	}

	private static ScanResult withSourceFile(ScanResult result, String sourceFile) {
		List<ScannedInvoice> rows = new ArrayList<>(result.rows().size());
		Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new LinkedHashMap<>();
		for (ScannedInvoice row : result.rows()) {
			ScannedInvoice renamed = new ScannedInvoice(sourceFile, row.supplierName(), row.invoiceNo(),
					row.invoiceDate(), row.dueDate(), row.documentType(), row.amountCents(), row.fieldConfidences());
			rows.add(renamed);
			Map<PdfEvidenceField, PdfEvidenceLocation> evidence = result.locations().get(row);
			if (evidence != null && !evidence.isEmpty()) locations.put(renamed, evidence);
		}
		return new ScanResult(rows, locations, result.periodStart(), result.periodEnd(),
				result.statementAmountCents(), result.statementAmountConfidence(),
				result.pageTransactionCounts());
	}

	private void scanRemainingChunks(List<PdfChunk> chunks, StatementPeriod period, List<ScanResult> ordered,
			ScanProgressListener progress)
			throws IOException, InterruptedException {
		int parallelism = Math.min(chunks.size() - 1, configuredInteger(
				"gemini.statement.parallel.max-concurrent", "GEMINI_STATEMENT_PARALLEL_MAX_CONCURRENT",
				DEFAULT_PARALLEL_SCAN_LIMIT, 1, 6));
		ExecutorService executor = Executors.newFixedThreadPool(parallelism, runnable -> {
			Thread thread = new Thread(runnable, "gemini-statement-chunk");
			thread.setDaemon(true);
			return thread;
		});
		CompletionService<ChunkScan> completion = new ExecutorCompletionService<>(executor);
		List<Future<ChunkScan>> futures = new ArrayList<>();
		try {
			for (int index = 1; index < chunks.size(); index++) {
				int chunkIndex = index;
				PdfChunk chunk = chunks.get(index);
				futures.add(completion.submit(() -> new ChunkScan(chunkIndex,
						scanUploadedPdf(chunk.file(), ScanKind.STATEMENT, period,
								chunk.startPageIndex(), chunk.pageCount(), true))));
			}
			for (int completed = 1; completed < chunks.size(); completed++) {
				try {
					ChunkScan result = completion.take().get();
					ordered.set(result.index(), result.result());
					notifyProgress(progress, completed + 1, chunks.size());
				} catch (ExecutionException exception) {
					Throwable cause = exception.getCause();
					if (cause instanceof IOException io) throw io;
					if (cause instanceof InterruptedException interrupted) {
						Thread.currentThread().interrupt();
						throw interrupted;
					}
					throw new IOException("Gemini statement chunk extraction failed", cause);
				}
			}
		} finally {
			futures.forEach(future -> future.cancel(true));
			executor.shutdownNow();
		}
	}

	private static void notifyProgress(ScanProgressListener progress, int completed, int total) {
		if (progress == null) return;
		try {
			progress.onProgress(completed, total);
		} catch (RuntimeException ignored) {
			// Progress reporting must never turn a successful OCR result into a
			// failed scan if the UI listener is no longer available.
		}
	}

	/**
	 * Compatibility overload. Statements now infer their period from the PDF, so
	 * the UI-selected month is deliberately ignored.
	 */
	@Deprecated
	public ScanResult scan(File pdf, ScanKind kind, YearMonth ignoredUiMonth)
			throws IOException, InterruptedException {
		return scan(pdf, kind);
	}

	/** Parses a Gemini response and converts normalized boxes into PDF-point locations. */
	public ScanResult parseResponse(String rawResponse, File pdf) throws IOException {
		if (pdf == null || !pdf.isFile()) throw new IOException("The source PDF is no longer available");
		try (PDDocument document = PDDocument.load(pdf)) {
			JsonNode root = mapper.readTree(rawResponse == null ? "" : rawResponse);
			JsonNode annotation = root;
			// This also accepts a response copied from an Interactions API adapter,
			// where the structured object may be wrapped in document_annotation.
			if (root.has("document_annotation")) {
				annotation = root.get("document_annotation");
				if (annotation.isTextual()) annotation = mapper.readTree(annotation.asText());
			}
			if (annotation == null || !annotation.isObject()) {
				throw new IOException("Gemini response did not contain a structured invoice document");
			}
			String supplier = text(annotation, "supplier_name");
			LocalDate documentDate = date(text(annotation, "document_date"));
			List<ScannedInvoice> rows = new ArrayList<>();
			Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new LinkedHashMap<>();
			JsonNode transactions = annotation.get("transactions");
			if (transactions != null && transactions.isArray()) {
				for (JsonNode transaction : transactions) {
					String rowSupplier = text(transaction, "supplier_name");
					if (rowSupplier.isBlank()) rowSupplier = supplier;
					String reference = text(transaction, "reference");
					LocalDate invoiceDate = date(text(transaction, "transaction_date"));
					if (invoiceDate == null) invoiceDate = documentDate;
					LocalDate dueDate = date(text(transaction, "due_date"));
					long amountCents = moneyCents(text(transaction, "amount"));
					ScannedInvoice.DocumentType type = isCredit(text(transaction, "transaction_type")) || amountCents < 0
							? ScannedInvoice.DocumentType.CREDIT : ScannedInvoice.DocumentType.INVOICE;
					if (type == ScannedInvoice.DocumentType.CREDIT) amountCents = -Math.abs(amountCents);
					ScannedInvoice row = new ScannedInvoice(
							pdf.getName(), rowSupplier, reference, invoiceDate, dueDate, type,
							amountCents, fieldConfidences(transaction));
					rows.add(row);
					Map<PdfEvidenceField, PdfEvidenceLocation> rowLocations = evidenceLocations(
							transaction.get("evidence"), transaction.get("page_number"), document);
					if (!rowLocations.isEmpty()) locations.put(row, rowLocations);
				}
			}
			StatementPeriod period = inferredStatementPeriod(annotation, documentDate, rows);
			return new ScanResult(rows, locations, period.start(), period.end(),
					moneyCentsOrNull(text(annotation, "statement_amount")),
					confidence(annotation.get("statement_amount_confidence")),
					pageTransactionCounts(annotation));
		} catch (IOException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new IOException("Gemini returned invalid structured invoice data", exception);
		}
	}

	/** Convenience overload for parser tests that do not need a PDF preview. */
	ScanResult parseResponse(String rawResponse, String sourceFile) throws IOException {
		File source = new File(sourceFile == null || sourceFile.isBlank() ? "scan.pdf" : sourceFile);
		try {
			return parseResponse(rawResponse, source);
		} catch (IOException exception) {
			// Keep parser-only callers useful when no actual PDF is present. The
			// extracted rows remain valid; Gemini boxes simply cannot be projected.
			if (!source.isFile()) return parseRowsWithoutLocations(rawResponse, source.getName());
			throw exception;
		}
	}

	private UploadedFile upload(File pdf) throws IOException, InterruptedException {
		byte[] bytes = Files.readAllBytes(pdf.toPath());
		HttpRequest startRequest = HttpRequest.newBuilder(apiUri("/upload/v1beta/files"))
				.timeout(requestTimeout)
				.header("x-goog-api-key", apiKey)
				.header("X-Goog-Upload-Protocol", "resumable")
				.header("X-Goog-Upload-Command", "start")
				.header("X-Goog-Upload-Header-Content-Length", Long.toString(bytes.length))
				.header("X-Goog-Upload-Header-Content-Type", "application/pdf")
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						"{}",
						StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> startResponse = send(startRequest, "Gemini Files API upload setup");
		String uploadUrl = startResponse.headers().firstValue("X-Goog-Upload-URL")
				.orElseGet(() -> startResponse.headers().firstValue("x-goog-upload-url").orElse(""));
		if (uploadUrl.isBlank()) throw apiFailure("Gemini Files API did not return an upload URL", startResponse);

		HttpRequest dataRequest = HttpRequest.newBuilder(URI.create(uploadUrl.trim()))
				.timeout(requestTimeout)
				.header("X-Goog-Upload-Offset", "0")
				.header("X-Goog-Upload-Command", "upload, finalize")
				.POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
				.build();
		HttpResponse<String> dataResponse = send(dataRequest, "Gemini Files API upload");
		JsonNode uploadRoot = mapper.readTree(dataResponse.body());
		JsonNode file = uploadRoot.has("file") ? uploadRoot.get("file") : uploadRoot;
		String uri = text(file, "uri");
		String name = text(file, "name");
		if (uri.isBlank()) throw apiFailure("Gemini Files API did not return a file URI", dataResponse);
		String state = text(file, "state");
		if ("FAILED".equalsIgnoreCase(state)) {
			throw new IOException("Gemini failed while processing " + pdf.getName() + " for model input");
		}
		if ("PROCESSING".equalsIgnoreCase(state) && !name.isBlank()) awaitFile(name);
		return new UploadedFile(uri, name);
	}

	private void awaitFile(String name) throws IOException, InterruptedException {
		long deadline = System.nanoTime() + fileProcessingTimeout.toNanos();
		while (System.nanoTime() < deadline) {
			HttpRequest request = HttpRequest.newBuilder(apiUri("/v1beta/" + name))
					.timeout(requestTimeout)
					.header("x-goog-api-key", apiKey)
					.GET()
					.build();
			HttpResponse<String> response = send(request, "Gemini Files API status check");
			JsonNode fileRoot = mapper.readTree(response.body());
			JsonNode file = fileRoot.has("file") ? fileRoot.get("file") : fileRoot;
			String state = text(file, "state");
			if ("ACTIVE".equalsIgnoreCase(state) || state.isBlank()) return;
			if ("FAILED".equalsIgnoreCase(state)) {
				throw new IOException("Gemini failed while preparing the uploaded PDF");
			}
			Thread.sleep(1_000);
		}
		throw new IOException("Gemini did not finish preparing the uploaded PDF within "
				+ fileProcessingTimeout.toMinutes() + " minutes");
	}

	private void deleteFile(String name) {
		try {
			HttpRequest request = HttpRequest.newBuilder(apiUri("/v1beta/" + name))
					.timeout(requestTimeout)
					.header("x-goog-api-key", apiKey)
					.DELETE()
					.build();
			http.send(request, HttpResponse.BodyHandlers.discarding());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		} catch (IOException ignored) {
			// Deletion is best effort; the Files API expires objects automatically.
		}
	}

	private String generate(String fileUri, ScanKind kind, StatementPeriod fixedPeriod,
			int pageCount, int originalPageOffset, boolean correctiveRetry)
			throws IOException, InterruptedException {
		String requestModel = kind == ScanKind.STATEMENT
				? runtimeSetting("gemini.statement.model", "GEMINI_STATEMENT_MODEL",
						"gemini.statement.model", DEFAULT_STATEMENT_MODEL)
				: model;
		ObjectNode root = mapper.createObjectNode();
		if (!DEFAULT_SERVICE_TIER.equals(serviceTier)) root.put("serviceTier", serviceTier);
		ArrayNode contents = root.putArray("contents");
		ObjectNode content = contents.addObject();
		content.put("role", "user");
		ArrayNode parts = content.putArray("parts");
		parts.addObject().put("text", prompt(kind, fixedPeriod, pageCount,
				originalPageOffset, correctiveRetry));
		ObjectNode fileData = parts.addObject().putObject("fileData");
		fileData.put("mimeType", "application/pdf");
		fileData.put("fileUri", fileUri);

		ObjectNode generationConfig = root.putObject("generationConfig");
		if (kind == ScanKind.STATEMENT) {
			generationConfig.put("maxOutputTokens", configuredInteger("gemini.statement.max-output-tokens",
					"GEMINI_STATEMENT_MAX_OUTPUT_TOKENS", DEFAULT_STATEMENT_MAX_OUTPUT_TOKENS, 4_096, 65_536));
			// Google's recommended PDF setting keeps both native text and enough
			// rendered-page detail for statement tables and evidence boxes.
			generationConfig.put("mediaResolution", "MEDIA_RESOLUTION_MEDIUM");
		}
		ObjectNode responseFormat = generationConfig.putObject("responseFormat");
		ObjectNode textFormat = responseFormat.putObject("text");
		textFormat.put("mimeType", "APPLICATION_JSON");
		try {
			textFormat.set("schema", mapper.readTree(SCHEMA));
		} catch (IOException exception) {
			throw new IOException("The Gemini invoice schema is invalid", exception);
		}
		if (supportsThinkingLevel(requestModel)) {
			ObjectNode thinkingConfig = generationConfig.putObject("thinkingConfig");
			thinkingConfig.put("thinkingLevel", thinkingLevel);
		}

		HttpRequest request = HttpRequest.newBuilder(apiUri("/v1beta/models/" + urlEncode(requestModel) + ":generateContent"))
				.timeout(requestTimeout)
				.header("x-goog-api-key", apiKey)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response = sendWithRetry(request, "Gemini Flash invoice extraction");
		logPriorityDowngrade(response);
		JsonNode responseRoot = mapper.readTree(response.body());
		JsonNode candidates = responseRoot.path("candidates");
		if (!candidates.isArray() || candidates.isEmpty()) {
			String blockReason = text(responseRoot.path("promptFeedback"), "blockReason");
			throw new IOException(blockReason.isBlank()
					? "Gemini returned no extraction candidate"
					: "Gemini blocked the PDF extraction: " + blockReason);
		}
		String finishReason = text(candidates.get(0), "finishReason");
		if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
			throw new IOException("Gemini stopped because its statement response reached the output-token limit");
		}
		StringBuilder output = new StringBuilder();
		for (JsonNode part : candidates.get(0).path("content").path("parts")) {
			if (part.has("text")) output.append(part.get("text").asText());
		}
		if (output.isEmpty()) throw new IOException("Gemini returned an empty structured invoice response");
		return stripCodeFence(output.toString());
	}

	private HttpResponse<String> send(HttpRequest request, String operation)
			throws IOException, InterruptedException {
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() < 200 || response.statusCode() >= 300) throw apiFailure(operation, response);
		return response;
	}

	/**
	 * Retries repeatable model requests on temporary capacity, rate-limit, gateway,
	 * and transport failures. The uploaded File URI remains valid between attempts,
	 * so a retry does not upload or parse the document again.
	 */
	private HttpResponse<String> sendWithRetry(HttpRequest request, String operation)
			throws IOException, InterruptedException {
		int maxAttempts = configuredInteger("gemini.retry.max-attempts", "GEMINI_RETRY_MAX_ATTEMPTS",
				DEFAULT_RETRY_MAX_ATTEMPTS, 1, 10);
		IOException lastTransportFailure = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			HttpResponse<String> response;
			try {
				response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				lastTransportFailure = null;
			} catch (IOException exception) {
				lastTransportFailure = exception;
				if (attempt == maxAttempts) {
					throw new IOException(operation + " failed after " + maxAttempts
							+ " attempts because the Gemini API could not be reached", exception);
				}
				long delayMillis = retryDelayMillis(null, attempt);
				logRetry(operation, "a network error", attempt, maxAttempts, delayMillis);
				Thread.sleep(delayMillis);
				continue;
			}

			if (response.statusCode() >= 200 && response.statusCode() < 300) return response;
			if (!isTransientStatus(response.statusCode()) || attempt == maxAttempts) {
				String attemptedOperation = isTransientStatus(response.statusCode()) && maxAttempts > 1
						? operation + " failed after " + maxAttempts + " attempts"
						: operation;
				throw apiFailure(attemptedOperation, response);
			}
			long delayMillis = retryDelayMillis(response, attempt);
			logRetry(operation, "HTTP " + response.statusCode(), attempt, maxAttempts, delayMillis);
			Thread.sleep(delayMillis);
		}
		throw new IOException(operation + " failed after " + maxAttempts + " attempts", lastTransportFailure);
	}

	private static boolean isTransientStatus(int statusCode) {
		return statusCode == 408 || statusCode == 429 || statusCode == 500
				|| statusCode == 502 || statusCode == 503 || statusCode == 504;
	}

	private static long retryDelayMillis(HttpResponse<?> response, int failedAttempt) {
		int maximumSeconds = configuredInteger("gemini.retry.max-delay-seconds", "GEMINI_RETRY_MAX_DELAY_SECONDS",
				DEFAULT_RETRY_MAX_DELAY_SECONDS, 1, 120);
		if (response != null) {
			String retryAfter = response.headers().firstValue("Retry-After").orElse("").trim();
			try {
				long seconds = Long.parseLong(retryAfter);
				if (seconds >= 0) return Math.min(seconds, maximumSeconds) * 1_000L;
			} catch (NumberFormatException ignored) {
				// HTTP-date Retry-After values are uncommon here; use exponential backoff.
			}
		}
		int initialSeconds = configuredInteger("gemini.retry.initial-delay-seconds",
				"GEMINI_RETRY_INITIAL_DELAY_SECONDS", DEFAULT_RETRY_INITIAL_DELAY_SECONDS, 1, 60);
		long exponentialSeconds = initialSeconds * (1L << Math.min(failedAttempt - 1, 10));
		long baseMillis = Math.min(exponentialSeconds, maximumSeconds) * 1_000L;
		long jitterBound = Math.max(1, baseMillis / 4);
		return Math.min(maximumSeconds * 1_000L,
				baseMillis + ThreadLocalRandom.current().nextLong(jitterBound));
	}

	private static void logRetry(String operation, String reason, int failedAttempt, int maxAttempts,
			long delayMillis) {
		System.err.println(operation + " received " + reason + "; retrying in "
				+ String.format(Locale.ROOT, "%.1f", delayMillis / 1_000.0) + "s (attempt "
				+ (failedAttempt + 1) + " of " + maxAttempts + ").");
	}

	private void logPriorityDowngrade(HttpResponse<?> response) {
		if (!"priority".equals(serviceTier)) return;
		String actualTier = response.headers().firstValue("x-gemini-service-tier").orElse("").trim();
		if ("standard".equalsIgnoreCase(actualTier)) {
			System.err.println("Gemini priority capacity was unavailable; this invoice scan was gracefully "
					+ "processed at the standard service tier.");
		}
	}

	private IOException apiFailure(String operation, HttpResponse<String> response) {
		String body = response.body() == null ? "" : response.body().trim();
		if (body.length() > 800) body = body.substring(0, 800) + "…";
		return new IOException(operation + " returned HTTP " + response.statusCode()
				+ (body.isBlank() ? "" : ": " + body));
	}

	private static Map<PdfEvidenceField, PdfEvidenceLocation> evidenceLocations(JsonNode evidence,
			JsonNode fallbackPage, PDDocument document) {
		if (evidence == null || !evidence.isObject()) return Map.of();
		EnumMap<PdfEvidenceField, PdfEvidenceLocation> locations = new EnumMap<>(PdfEvidenceField.class);
		putEvidence(locations, PdfEvidenceField.SUPPLIER, evidence.get("supplier"), fallbackPage, document);
		putEvidence(locations, PdfEvidenceField.REFERENCE, evidence.get("reference"), fallbackPage, document);
		putEvidence(locations, PdfEvidenceField.DATE, evidence.get("date"), fallbackPage, document);
		putEvidence(locations, PdfEvidenceField.AMOUNT, evidence.get("amount"), fallbackPage, document);
		return locations.isEmpty() ? Map.of() : Collections.unmodifiableMap(locations);
	}

	private static void putEvidence(Map<PdfEvidenceField, PdfEvidenceLocation> locations, PdfEvidenceField field,
			JsonNode box, JsonNode fallbackPage, PDDocument document) {
		if (box == null || box.isNull() || !box.isObject()) return;
		int pageNumber = integer(box, "page_number", integerValue(fallbackPage, 0));
		if (pageNumber <= 0 || pageNumber > document.getNumberOfPages()) return;
		JsonNode coordinates = box.get("box_2d");
		if (coordinates == null || !coordinates.isArray() || coordinates.size() < 4) return;
		List<Double> values = new ArrayList<>(4);
		for (int index = 0; index < 4; index++) {
			JsonNode value = coordinates.get(index);
			if (!value.isNumber()) return;
			values.add(clamp(value.asDouble(), 0, 1000));
		}
		double top = Math.min(values.get(0), values.get(2));
		double left = Math.min(values.get(1), values.get(3));
		double bottom = Math.max(values.get(0), values.get(2));
		double right = Math.max(values.get(1), values.get(3));
		if (right <= left || bottom <= top) return;
		PDPage page = document.getPage(pageNumber - 1);
		double pageWidth = page.getCropBox().getWidth();
		double pageHeight = page.getCropBox().getHeight();
		if (Math.floorMod(page.getRotation(), 180) != 0) {
			double swap = pageWidth;
			pageWidth = pageHeight;
			pageHeight = swap;
		}
		double x = left / 1000.0 * pageWidth;
		double y = top / 1000.0 * pageHeight;
		double width = (right - left) / 1000.0 * pageWidth;
		double height = (bottom - top) / 1000.0 * pageHeight;
		locations.put(field, new PdfEvidenceLocation(pageNumber - 1, x, y, Math.max(1, width), Math.max(1, height), pageWidth, pageHeight));
	}

	private ScanResult parseRowsWithoutLocations(String rawResponse, String sourceFile) throws IOException {
		JsonNode root = mapper.readTree(stripCodeFence(rawResponse == null ? "" : rawResponse));
		JsonNode annotation = root.has("document_annotation") ? root.get("document_annotation") : root;
		if (annotation.isTextual()) annotation = mapper.readTree(annotation.asText());
		String supplier = text(annotation, "supplier_name");
		LocalDate documentDate = date(text(annotation, "document_date"));
		List<ScannedInvoice> rows = new ArrayList<>();
		for (JsonNode transaction : annotation.path("transactions")) {
			String rowSupplier = text(transaction, "supplier_name");
			if (rowSupplier.isBlank()) rowSupplier = supplier;
			LocalDate invoiceDate = date(text(transaction, "transaction_date"));
			if (invoiceDate == null) invoiceDate = documentDate;
			long cents = moneyCents(text(transaction, "amount"));
			ScannedInvoice.DocumentType type = isCredit(text(transaction, "transaction_type")) || cents < 0
					? ScannedInvoice.DocumentType.CREDIT : ScannedInvoice.DocumentType.INVOICE;
			if (type == ScannedInvoice.DocumentType.CREDIT) cents = -Math.abs(cents);
			rows.add(new ScannedInvoice(sourceFile, rowSupplier, text(transaction, "reference"), invoiceDate,
					date(text(transaction, "due_date")), type, cents, fieldConfidences(transaction)));
		}
		StatementPeriod period = inferredStatementPeriod(annotation, documentDate, rows);
		return new ScanResult(rows, Map.of(), period.start(), period.end(),
				moneyCentsOrNull(text(annotation, "statement_amount")),
				confidence(annotation.get("statement_amount_confidence")),
				pageTransactionCounts(annotation));
	}

	private static String prompt(ScanKind kind, StatementPeriod fixedPeriod,
			int pageCount, int originalPageOffset, boolean correctiveRetry) {
		if (kind == ScanKind.STATEMENT) {
			String periodInstruction = fixedPeriod == null
					? "First determine the statement's intended reporting period. Use an explicitly printed period-from/period-to or period-ending value when present. Otherwise use the full calendar month containing the printed statement date/date of statement. Only if neither is visible, use the full calendar month containing the latest transaction date. Return that inclusive period as statement_period_start and statement_period_end. Do not use today's date, the application's selected month, invoice due dates, payment dates, or PDF metadata to infer the period. "
					: "The statement period has already been established as " + fixedPeriod.start()
							+ " through " + fixedPeriod.end() + " inclusive. Use exactly those dates for statement_period_start and statement_period_end; do not infer or change them. ";
			String retryInstruction = correctiveRetry
					? "A previous extraction did not prove complete page coverage. Start over, inspect each supplied page independently, and account for every qualifying row. "
					: "";
			return retryInstruction
					+ "This PDF is a supplier STATEMENT chunk containing exactly " + pageCount
					+ " physical page" + (pageCount == 1 ? "" : "s") + ", corresponding to original statement pages "
					+ (originalPageOffset + 1) + " through " + (originalPageOffset + pageCount) + ". "
					+ "Within this supplied PDF, page numbering starts at 1. "
					+ periodInstruction
					+ "Inspect every page from first to last and every line of each transaction table. Do not stop after the first matching row or "
					+ "the first page. Return one transactions-array item for EVERY invoice or credit whose printed Invoice Date or Transaction Date "
					+ "falls inside the inferred period, inclusive. "
					+ "Rows can continue across page boundaries and column headings may repeat. Read the invoice/document reference from "
					+ "each row, not the statement number or customer number. Read amount from Original Amount, Invoice Amount, Gross Amount, "
					+ "or the equivalent transaction-value column; do not use Payment, Adjustment, Remaining Amount, account balance, or a page total. "
					+ "Classify rows labelled credit/credit note/CR as credit. Also classify any negative original transaction amount as credit, "
					+ "and return credit amounts as negative. Return positive invoice amounts as positive. Exclude payment-only rows, opening or "
					+ "closing balances, subtotals, remittance advice, account summaries, headers, and footer totals. Preserve references exactly. "
					+ "For each row provide a separate confidence from 0 to 1 for supplier, reference, date, due date, transaction type, and amount; "
					+ "do not return one overall confidence. "
					+ "For every transaction, provide best-estimate evidence boxes for supplier, reference, date, and amount. "
					+ "Each box must use [ymin, xmin, ymax, xmax] normalized to 0..1000 and the one-based PDF page number. "
					+ "Page and evidence page numbers must be local to this supplied PDF; the application will map them back to original pages. "
					+ "Return page_checks with exactly one entry for every local page 1 through " + pageCount
					+ ", including pages with zero qualifying rows. relevant_transaction_count is the number of transactions returned from that page, "
					+ "and the sum of all relevant_transaction_count values must exactly equal transactions.length. "
					+ "If this chunk visibly prints the overall amount due, closing balance, statement total, or total for the complete statement, "
					+ "return that signed value as statement_amount and its confidence as statement_amount_confidence. Do not use a page subtotal, "
					+ "transaction-row Remaining Amount, opening balance, payments total, adjustments total, or an inferred sum. Return null for both "
					+ "statement amount fields when an explicit complete-statement amount is not visible in this chunk. "
					+ "If a value is not visible, use null for its box. Do not invent transactions or values.";
		}
		return "Read this complete PDF and extract each independent invoice or credit document. Return the supplier, invoice/reference number, "
				+ "document date, due date, and signed gross total including GST. Provide a separate confidence from 0 to 1 for every "
				+ "extracted supplier, reference, date, due date, transaction type, and amount value; do not return one overall confidence. "
				+ "Set statement_period_start and statement_period_end to null because this is not a supplier statement. "
				+ "Set statement_amount and statement_amount_confidence to null. "
				+ "Use one transaction per independent invoice or credit; do not treat payment receipts, tax summaries, or line items as separate invoices. "
				+ "For every transaction, provide best-estimate evidence boxes for supplier, reference, date, and amount. "
				+ "Each box must use [ymin, xmin, ymax, xmax] normalized to 0..1000 and the one-based PDF page number. "
				+ "Return page_checks with exactly one entry for every supplied page from 1 through " + pageCount
				+ ", including pages with zero extracted documents; its counts must sum to transactions.length. "
				+ "If a value is not visible, use null for its box. Do not invent values.";
	}

	private static ScanResult forPeriod(ScanResult result) {
		if (result.periodStart() == null || result.periodEnd() == null) return result;
		List<ScannedInvoice> rows = result.rows().stream()
				.filter(row -> row.invoiceDate() == null
						|| (!row.invoiceDate().isBefore(result.periodStart())
								&& !row.invoiceDate().isAfter(result.periodEnd())))
				.toList();
		if (rows.size() == result.rows().size()) return result;
		Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new LinkedHashMap<>();
		for (ScannedInvoice row : rows) {
			Map<PdfEvidenceField, PdfEvidenceLocation> rowLocations = result.locations().get(row);
			if (rowLocations != null && !rowLocations.isEmpty()) locations.put(row, rowLocations);
		}
		return new ScanResult(rows, locations, result.periodStart(), result.periodEnd(),
				result.statementAmountCents(), result.statementAmountConfidence(),
				result.pageTransactionCounts());
	}

	private static StatementPeriod inferredStatementPeriod(JsonNode annotation, LocalDate documentDate,
			List<ScannedInvoice> rows) {
		LocalDate start = date(text(annotation, "statement_period_start"));
		LocalDate end = date(text(annotation, "statement_period_end"));
		if (start != null && end != null && start.isAfter(end)) {
			LocalDate swap = start;
			start = end;
			end = swap;
		}
		if (start != null && end == null) end = start.withDayOfMonth(start.lengthOfMonth());
		if (end != null && start == null) start = end.withDayOfMonth(1);
		LocalDate anchor = documentDate;
		if (anchor == null && rows != null) {
			anchor = rows.stream().map(ScannedInvoice::invoiceDate).filter(java.util.Objects::nonNull)
					.max(LocalDate::compareTo).orElse(null);
		}
		if (start == null && end == null && anchor != null) {
			start = anchor.withDayOfMonth(1);
			end = anchor.withDayOfMonth(anchor.lengthOfMonth());
		}
		return new StatementPeriod(start, end);
	}

	private static boolean supportsThinkingLevel(String model) {
		String normalized = model == null ? "" : model.toLowerCase(Locale.ROOT);
		return normalized.contains("gemini-3.") || normalized.contains("gemini-3-");
	}

	private static boolean isCredit(String type) {
		String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
		return normalized.equals("credit") || normalized.equals("creditnote") || normalized.equals("refund");
	}

	private static String stripCodeFence(String value) {
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
			int firstNewline = trimmed.indexOf('\n');
			if (firstNewline >= 0) trimmed = trimmed.substring(firstNewline + 1, trimmed.length() - 3).trim();
		}
		return trimmed;
	}

	private static String text(JsonNode object, String key) {
		if (object == null || object.isMissingNode() || object.isNull()) return "";
		JsonNode value = object.get(key);
		return value == null || value.isNull() ? "" : value.asText().trim();
	}

	private static long moneyCents(String value) {
		Long parsed = moneyCentsOrNull(value);
		return parsed == null ? 0 : parsed;
	}

	private static Long moneyCentsOrNull(String value) {
		if (value == null || value.isBlank()) return null;
		String amount = value.toUpperCase(Locale.ROOT).replace("AUD", "").replace("$", "").replace(",", "").trim();
		boolean negative = (amount.startsWith("(") && amount.endsWith(")")) || amount.endsWith("CR") || amount.startsWith("-");
		amount = amount.replace("CR", "").replace("(", "").replace(")", "").trim();
		if (amount.startsWith("-")) amount = amount.substring(1).trim();
		try {
			long cents = new BigDecimal(amount).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
			return negative ? -Math.abs(cents) : cents;
		} catch (NumberFormatException | ArithmeticException ignored) {
			return null;
		}
	}

	private static LocalDate date(String value) {
		if (value == null || value.isBlank()) return null;
		String candidate = value.trim();
		for (DateTimeFormatter formatter : DATE_FORMATS) {
			try {
				return LocalDate.parse(candidate, formatter);
			} catch (DateTimeParseException ignored) {
				// Try the next format.
			}
		}
		return null;
	}

	private static Double confidence(JsonNode value) {
		if (value == null || value.isNull() || !value.isNumber()) return null;
		double result = value.asDouble();
		if (result > 1 && result <= 100) result /= 100.0;
		return result >= 0 && result <= 1 ? result : null;
	}

	private static ScannedInvoice.FieldConfidences fieldConfidences(JsonNode transaction) {
		JsonNode fields = transaction == null ? null : transaction.get("field_confidence");
		Double legacy = transaction == null ? null : confidence(transaction.get("confidence"));
		if (fields == null || !fields.isObject()) {
			return new ScannedInvoice.FieldConfidences(legacy, legacy, legacy, legacy, legacy, legacy);
		}
		return new ScannedInvoice.FieldConfidences(
				confidenceOrFallback(fields.get("supplier"), legacy),
				confidenceOrFallback(fields.get("reference"), legacy),
				confidenceOrFallback(fields.get("date"), legacy),
				confidenceOrFallback(fields.get("due_date"), legacy),
				confidenceOrFallback(fields.get("type"), legacy),
				confidenceOrFallback(fields.get("amount"), legacy));
	}

	private static Double confidenceOrFallback(JsonNode value, Double fallback) {
		Double parsed = confidence(value);
		return parsed == null ? fallback : parsed;
	}

	private static int integer(JsonNode object, String key, int fallback) {
		if (object == null || object.isMissingNode() || object.isNull()) return fallback;
		JsonNode value = object.get(key);
		return value != null && value.isIntegralNumber() ? value.asInt() : fallback;
	}

	private static int integerValue(JsonNode value, int fallback) {
		return value != null && value.isIntegralNumber() ? value.asInt() : fallback;
	}

	private static Map<Integer, Integer> pageTransactionCounts(JsonNode annotation) {
		JsonNode checks = annotation == null ? null : annotation.get("page_checks");
		if (checks == null || !checks.isArray()) return Map.of();
		Map<Integer, Integer> declared = new LinkedHashMap<>();
		for (JsonNode check : checks) {
			int page = integer(check, "page_number", 0);
			int count = integer(check, "relevant_transaction_count", -1);
			if (page <= 0 || count < 0 || declared.putIfAbsent(page, count) != null) return Map.of();
		}

		// Do not accept a plausible-looking manifest unless every returned row's
		// own page_number agrees with it. Returning an empty map makes the chunk
		// fail completeness validation and triggers the corrective retry.
		Map<Integer, Integer> observed = new LinkedHashMap<>();
		for (JsonNode transaction : annotation.path("transactions")) {
			int page = integer(transaction, "page_number", 0);
			if (page <= 0) return Map.of();
			observed.merge(page, 1, Integer::sum);
		}
		for (Map.Entry<Integer, Integer> entry : declared.entrySet()) {
			if (observed.getOrDefault(entry.getKey(), 0).intValue() != entry.getValue()) return Map.of();
		}
		if (observed.keySet().stream().anyMatch(page -> !declared.containsKey(page))) return Map.of();
		return declared;
	}

	private static double clamp(double value, double minimum, double maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private URI apiUri(String path) {
		return URI.create(trimTrailingSlash(apiBase.toString()) + path);
	}

	private static String urlEncode(String value) {
		return value == null ? "" : value.replace("/", "%2F").replace(" ", "%20");
	}

	private static int validatePdf(File pdf) throws IOException {
		if (pdf == null || !pdf.isFile()) throw new IOException("The selected PDF no longer exists");
		if (!pdf.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) throw new IOException("Only PDF documents can be scanned");
		long size = Files.size(pdf.toPath());
		if (size == 0) throw new IOException(pdf.getName() + " is empty");
		if (size > MAX_PDF_BYTES) throw new IOException(pdf.getName() + " is larger than Gemini's 50 MB PDF limit");
		try (PDDocument document = PDDocument.load(pdf)) {
			int pages = document.getNumberOfPages();
			if (pages == 0) throw new IOException(pdf.getName() + " has no pages");
			if (pages > MAX_PDF_PAGES) throw new IOException(pdf.getName() + " has " + pages
					+ " pages; Gemini accepts at most " + MAX_PDF_PAGES + " pages per PDF");
			return pages;
		}
	}

	private static Duration requestTimeout() {
		return configuredDuration("gemini.request.timeout-seconds", "GEMINI_REQUEST_TIMEOUT_SECONDS",
				DEFAULT_REQUEST_TIMEOUT, 30, 1_800);
	}

	private static Duration fileProcessingTimeout() {
		return configuredDuration("gemini.file-processing-timeout-seconds", "GEMINI_FILE_PROCESSING_TIMEOUT_SECONDS",
				DEFAULT_FILE_PROCESSING_TIMEOUT, 30, 1_800);
	}

	private static Duration configuredDuration(String systemProperty, String environmentVariable, Duration fallback,
			long minimumSeconds, long maximumSeconds) {
		String value = runtimeSetting(systemProperty, environmentVariable, systemProperty, Long.toString(fallback.toSeconds()));
		try {
			long seconds = Long.parseLong(value);
			if (seconds >= minimumSeconds && seconds <= maximumSeconds) return Duration.ofSeconds(seconds);
		} catch (NumberFormatException ignored) {
			// Use the safe default below when the setting is malformed.
		}
		return fallback;
	}

	private static int configuredInteger(String systemProperty, String environmentVariable, int fallback,
			int minimum, int maximum) {
		String value = runtimeSetting(systemProperty, environmentVariable, systemProperty, Integer.toString(fallback));
		try {
			int configured = Integer.parseInt(value);
			if (configured >= minimum && configured <= maximum) return configured;
		} catch (NumberFormatException ignored) {
			// Use the bounded default below when the setting is malformed.
		}
		return fallback;
	}

	private static Duration validDuration(Duration value, Duration fallback) {
		return value == null || value.isNegative() || value.isZero() ? fallback : value;
	}

	private static String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) return DEFAULT_API_BASE.toString();
		return value.replaceFirst("/+\\z", "");
	}

	private static String normalizeServiceTier(String value) {
		String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "priority", "flex", "standard" -> normalized;
			default -> DEFAULT_SERVICE_TIER;
		};
	}

	private static String runtimeSetting(String systemProperty, String environmentVariable,
			String applicationProperty, String fallback) {
		String property = System.getProperty(systemProperty);
		if (property != null && !property.isBlank()) return property.trim();
		String environment = System.getenv(environmentVariable);
		if (environment != null && !environment.isBlank()) return environment.trim();
		Properties properties = new Properties();
		try (var input = GeminiInvoiceScanService.class.getClassLoader().getResourceAsStream("application.properties")) {
			if (input != null) {
				properties.load(input);
				String configured = properties.getProperty(applicationProperty);
				if (configured != null && !configured.isBlank()) return configured.trim();
			}
		} catch (IOException ignored) {
			// Surface missing configuration through isConfigured()/the UI.
		}
		return fallback;
	}

	public record ScanResult(List<ScannedInvoice> rows,
			Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations,
			LocalDate periodStart, LocalDate periodEnd,
			Long statementAmountCents, Double statementAmountConfidence,
			Map<Integer, Integer> pageTransactionCounts) {
		public ScanResult(List<ScannedInvoice> rows,
				Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations) {
			this(rows, locations, null, null, null, null, Map.of());
		}

		public ScanResult(List<ScannedInvoice> rows,
				Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations,
				LocalDate periodStart, LocalDate periodEnd) {
			this(rows, locations, periodStart, periodEnd, null, null, Map.of());
		}

		public ScanResult {
			rows = rows == null ? List.of() : List.copyOf(rows);
			if (locations == null || locations.isEmpty()) {
				locations = Map.of();
			} else {
				Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> copy = new LinkedHashMap<>();
				locations.forEach((row, values) -> copy.put(row, values == null || values.isEmpty()
						? Map.of() : Map.copyOf(values)));
				locations = Map.copyOf(copy);
			}
			pageTransactionCounts = pageTransactionCounts == null || pageTransactionCounts.isEmpty()
					? Map.of() : Map.copyOf(pageTransactionCounts);
			statementAmountConfidence = confidenceValue(statementAmountConfidence);
		}

		private static Double confidenceValue(Double value) {
			return value != null && Double.isFinite(value) && value >= 0 && value <= 1 ? value : null;
		}
	}

	private record PdfChunk(File file, int startPageIndex, int pageCount) {}
	private record ChunkScan(int index, ScanResult result) {}
	private record StatementPeriod(LocalDate start, LocalDate end) {}
	private record UploadedFile(String uri, String name) {}
}
