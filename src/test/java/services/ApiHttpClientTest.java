package services;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import models.Credit;
import models.Invoice;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.*;

class ApiHttpClientTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void savedInvoiceOrCreditWithMissingResponseTimesOutInsteadOfHanging(boolean credit) throws Exception {
        var releaseResponse = new CountDownLatch(1);
        var persisted = new AtomicBoolean();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                if (exchange.getRequestMethod().equals("POST")) {
                    persisted.set(true);
                    // Simulate the API committing the save but never returning its response.
                    try { releaseResponse.await(5, TimeUnit.SECONDS); }
                    catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
                } else {
                    byte[] body = (credit ? "[]" : "false").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
            }
        });
        server.start();
        try {
            var properties = new Properties();
            properties.setProperty("api.base.url", "http://127.0.0.1:" + server.getAddress().getPort());
            properties.setProperty("api.read.timeout.seconds", "1");
            // Exercise the production constructor, including its timeout configuration.
            var type = credit ? CreditService.class : InvoiceService.class;
            var constructor = type.getDeclaredConstructor(Properties.class);
            constructor.setAccessible(true);
            Object service = constructor.newInstance(properties);
            ResourceAccessException failure = assertTimeoutPreemptively(Duration.ofSeconds(4), () ->
                    assertThrows(ResourceAccessException.class, () -> {
                        if (service instanceof CreditService credits) {
                            var document = new Credit();
                            document.setStoreID(1); document.setSupplierID(1);
                            document.setCreditNo("CR-1"); document.setCreditDate(LocalDate.of(2026, 10, 1));
                            credits.saveOrUpdateCredit(document);
                        } else {
                            var document = new Invoice();
                            document.setStoreID(1); document.setSupplierID(1); document.setInvoiceNo("INV-1");
                            document.setInvoiceDate(LocalDate.of(2026, 10, 1));
                            document.setDueDate(LocalDate.of(2026, 10, 31));
                            ((InvoiceService) service).saveOrUpdateInvoice(document, null, 0);
                        }
                    }));
            assertInstanceOf(SocketTimeoutException.class, failure.getCause());
            assertTrue(persisted.get(), "The backend had already saved the document before its response stalled");
        } finally {
            releaseResponse.countDown();
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "invalid" })
    void rejectsTimeoutSettingsThatCouldAllowAnUnboundedWait(String value) {
        var properties = new Properties();
        properties.setProperty("api.read.timeout.seconds", value);
        assertThrows(IOException.class, () -> ApiHttpClient.create(properties));
    }
}
