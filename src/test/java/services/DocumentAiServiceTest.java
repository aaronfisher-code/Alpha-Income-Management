package services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import models.DocumentAiModels.BatchStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DocumentAiServiceTest {
	@TempDir Path temporary;

	@ParameterizedTest
	@CsvSource({ "0, COMPLETED, true", "409, COMPLETED, true", "502, COMPLETED, true",
			"0, REVIEW_REQUIRED, false", "409, REVIEW_REQUIRED, false", "409, DISMISSED, false",
			"403, COMPLETED, false" })
	void uncertainCompletionOnlySucceedsWhenBackendConfirmsCompleted(int failureStatus, BatchStatus status,
			boolean completed) {
		List<String> requests = new ArrayList<>();
		RestTemplate rest = new RestTemplate();
		rest.getInterceptors().add((request, body, execution) -> {
			assertEquals("storeId=7", request.getURI().getQuery());
			requests.add(request.getMethod() + " " + request.getURI().getPath());
			if (request.getMethod() == HttpMethod.POST) {
				if (failureStatus == 0) throw new ResourceAccessException("Read timed out");
				return jsonResponse(HttpStatus.valueOf(failureStatus), "{}");
			}
			return jsonResponse(HttpStatus.OK, "{\"batch\":{\"id\":17,\"kind\":\"INVOICE_BATCH\",\"status\":\""
					+ status + "\"},\"documents\":[]}");
		});
		var properties = new Properties();
		properties.setProperty("api.base.url", "https://example.invalid");
		var service = new DocumentAiService(rest, properties);
		if (completed) assertDoesNotThrow(() -> service.complete(17, 7, List.of()));
		else assertThrows(RestClientException.class, () -> service.complete(17, 7, List.of()));
		assertEquals(failureStatus == 403
				? List.of("POST /document-ai/review-batches/17/complete")
				: List.of("POST /document-ai/review-batches/17/complete", "GET /document-ai/review-batches/17"), requests);
	}

	@Test
	void failedCompletionStatusLookupPreservesTheOriginalError() {
		var original = new ResourceAccessException("Completion timed out");
		var lookup = new ResourceAccessException("Status lookup failed");
		var rest = new RestTemplate();
		rest.getInterceptors().add((request, body, execution) -> {
			throw request.getMethod() == HttpMethod.POST ? original : lookup;
		});
		var service = new DocumentAiService(rest, new Properties());
		assertSame(original, assertThrows(ResourceAccessException.class, () -> service.complete(17, 7, List.of())));
		assertArrayEquals(new Throwable[] { lookup }, original.getSuppressed());
	}

	private static ClientHttpResponse jsonResponse(HttpStatus status, String json) {
		return new ClientHttpResponse() {
			public HttpStatus getStatusCode() { return status; }
			public String getStatusText() { return status.getReasonPhrase(); }
			public HttpHeaders getHeaders() {
				var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); return headers;
			}
			public InputStream getBody() { return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)); }
			public void close() {}
		};
	}

	@Test
	void streamsPreviewDownloadsWithStoreAndSessionHeadersAndClosesTheResponse() throws Exception {
		byte[] pdf = new byte[32 * 1024];
		AtomicBoolean closed = new AtomicBoolean();
		DocumentAiService service = service(new ByteArrayInputStream(pdf), pdf.length, closed);
		Path destination = temporary.resolve("source.pdf");
		assertTrue(service.downloadPdf(101, 7, destination, Long.MAX_VALUE));
		assertArrayEquals(pdf, Files.readAllBytes(destination));
		assertTrue(closed.get());
	}

	@Test
	void skipsKnownOversizedSourcesBeforeReadingTheirBody() throws Exception {
		AtomicInteger read = new AtomicInteger();
		AtomicBoolean closed = new AtomicBoolean();
		DocumentAiService service = service(countingBody(read), 100_000, closed);
		Path destination = temporary.resolve("source.pdf");
		assertFalse(service.downloadPdf(101, 7, destination, 1024));
		assertEquals(0, read.get());
		assertFalse(Files.exists(destination));
		assertTrue(closed.get());
	}

	@Test
	void limitsSpeculativeDownloadsEvenWithoutAContentLength() throws Exception {
		AtomicInteger read = new AtomicInteger();
		AtomicBoolean closed = new AtomicBoolean();
		DocumentAiService service = service(countingBody(read), -1, closed);
		Path destination = temporary.resolve("source.pdf");
		assertFalse(service.downloadPdf(101, 7, destination, 10_000));
		assertTrue(Files.size(destination) <= 10_000);
		assertTrue(read.get() <= 10_000 + 8192);
		assertTrue(closed.get());
	}

	private static InputStream countingBody(AtomicInteger read) {
		return new InputStream() {
			@Override public int read() { read.incrementAndGet(); return 0; }
			@Override public int read(byte[] buffer, int offset, int length) {
				read.addAndGet(length);
				return length;
			}
		};
	}

	private static DocumentAiService service(InputStream body, long contentLength, AtomicBoolean closed) {
		RestTemplate rest = new RestTemplate();
		rest.getInterceptors().add((request, requestBody, execution) -> {
			assertEquals(HttpMethod.GET, request.getMethod());
			assertEquals("/document-ai/review-documents/101/pdf", request.getURI().getPath());
			assertEquals("storeId=7", request.getURI().getQuery());
			assertEquals("Bearer test-token", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
			assertEquals(UserService.getDocumentAiSession(), request.getHeaders().getFirst("X-Alpha-Session"));
			return new ClientHttpResponse() {
				public HttpStatus getStatusCode() { return HttpStatus.OK; }
				public String getStatusText() { return "OK"; }
				public HttpHeaders getHeaders() {
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_PDF);
					if (contentLength >= 0) headers.setContentLength(contentLength);
					return headers;
				}
				public InputStream getBody() { return body; }
				public void close() { closed.set(true); }
			};
		});
		Properties properties = new Properties();
		properties.setProperty("api.base.url", "https://example.invalid/");
		properties.setProperty("api.token", "test-token");
		return new DocumentAiService(rest, properties);
	}
}
