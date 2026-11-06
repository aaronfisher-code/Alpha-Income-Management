package services;

import models.AccountPayment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountPaymentServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"19133 1", "A+B", "A%B", "A@B", "A#B", "A?B", "A&B"})
    void deleteEncodesReferenceOnceAndVerifiesTheOriginalMonth(String reference) {
        var methods = new ArrayList<HttpMethod>();
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            methods.add(request.getMethod());
            assertEquals("Bearer test-token", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            if (request.getMethod() == HttpMethod.DELETE) {
                assertEquals("/account-payments/1/" + reference, request.getURI().getPath());
                if (reference.contains(" ")) assertTrue(request.getURI().getRawPath().endsWith("19133%201"));
                return response(HttpStatus.OK, "");
            }
            assertEquals("storeId=1&yearMonth=2026-09", request.getURI().getQuery());
            return response(HttpStatus.OK, "[]");
        });
        service(rest).deleteAccountPayment(payment(reference));
        assertEquals(List.of(HttpMethod.DELETE, HttpMethod.GET), methods);
    }

    @Test void anOlderApiThatClaimsSuccessWithoutRemovingTheRowIsRejected() {
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> response(HttpStatus.OK,
                request.getMethod() == HttpMethod.DELETE ? "" : "[{\"storeID\":1,\"invoiceNumber\":\"19133 1\"}]"));
        assertThrows(IllegalStateException.class, () -> service(rest).deleteAccountPayment(payment("19133 1")));
    }

    @Test void failedDeleteAndFailedVerificationNeverReturnSuccess() {
        for (HttpStatus status : List.of(HttpStatus.NOT_FOUND, HttpStatus.SERVICE_UNAVAILABLE)) {
            var rest = new RestTemplate();
            rest.getInterceptors().add((request, body, execution) -> response(status, "{}"));
            assertThrows(RestClientResponseException.class, () -> service(rest).deleteAccountPayment(payment("19133 1")));
        }
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> response(
                request.getMethod() == HttpMethod.DELETE ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE, "{}"));
        assertThrows(HttpServerErrorException.class, () -> service(rest).deleteAccountPayment(payment("19133 1")));
    }

    @Test void editPreservesRawOriginalReferenceAndPropagatesConflict() {
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            assertEquals(HttpMethod.PUT, request.getMethod());
            assertEquals("/account-payments/19133 1", request.getURI().getPath());
            assertTrue(new String(body, StandardCharsets.UTF_8).contains("239791"));
            return response(HttpStatus.CONFLICT, "{\"message\":\"Duplicate invoice\"}");
        });
        assertThrows(HttpClientErrorException.Conflict.class, () -> service(rest).updateAccountPayment("19133 1", payment("239791")));
    }

    private AccountPaymentService service(RestTemplate rest) {
        var properties = new Properties();
        properties.setProperty("api.base.url", "https://example.invalid");
        properties.setProperty("api.token", "test-token");
        return new AccountPaymentService(rest, properties);
    }
    private AccountPayment payment(String reference) {
        var payment = new AccountPayment(); payment.setStoreID(1); payment.setInvoiceNumber(reference);
        payment.setInvDate(LocalDate.of(2026, 9, 15));
        payment.setDueDate(LocalDate.of(2026, 9, 30)); return payment;
    }
    private ClientHttpResponse response(HttpStatus status, String body) {
        return new ClientHttpResponse() {
            public HttpStatus getStatusCode() { return status; }
            public String getStatusText() { return status.getReasonPhrase(); }
            public HttpHeaders getHeaders() { var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); return headers; }
            public InputStream getBody() { return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)); }
            public void close() {}
        };
    }
}
