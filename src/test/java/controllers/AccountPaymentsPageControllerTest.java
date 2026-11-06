package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import components.ActionableFilterComboBox;
import com.jfoenix.controls.JFXNodesList;
import io.github.palexdev.materialfx.controls.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import services.AccountPaymentService;
import services.AccountPaymentContactService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountPaymentsPageControllerTest {
    @BeforeAll static void startFx() throws Exception { utils.FxTestSupport.start(); }

    @Test void failedRenameAndRetryKeepTheOriginalRecordAndNeverTargetTheOtherInvoice() throws Exception {
        var paths = new CopyOnWriteArrayList<String>();
        var bodies = new CopyOnWriteArrayList<String>();
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            paths.add(request.getURI().getPath());
            bodies.add(new String(body, StandardCharsets.UTF_8));
            return response(HttpStatus.CONFLICT, "{}");
        });
        try (var workers = Executors.newCachedThreadPool()) {
            Fixture f = fx(() -> fixture(rest, workers));
            for (int attempt = 0; attempt < 2; attempt++) {
                fx(() -> { f.controller.editPayment(f.original); return null; });
                var error = f.dialogs.next();
                assertEquals("Unable to edit payment", error.getTitle());
                fx(() -> {
                    assertEquals("19133 1", f.original.getInvoiceNumber());
                    assertEquals(12.34, f.original.getUnitAmount());
                    assertEquals("Original description", f.original.getDescription());
                    assertEquals(LocalDate.of(2026, 9, 1), f.original.getInvDate());
                    assertFalse(f.popover.isDisabled());
                    return null;
                });
            }
            assertEquals(List.of("/account-payments/19133 1", "/account-payments/19133 1"), paths);
            assertTrue(bodies.stream().allMatch(body -> body.contains("239791") && body.contains("1377.09")));
        }
    }

    @Test void editsCannotBeSubmittedAgainWhileTheSaveIsPending() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var paths = new CopyOnWriteArrayList<String>(); var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            paths.add(request.getURI().getPath()); entered.countDown(); await(release);
            return response(HttpStatus.CONFLICT, "{}");
        });
        try (var workers = Executors.newCachedThreadPool()) {
            Fixture f = fx(() -> fixture(rest, workers));
            try {
                fx(() -> { f.controller.editPayment(f.original); return null; });
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                fx(() -> {
                    assertTrue(f.popover.isDisabled());
                    f.controller.editPayment(f.original);
                    f.controller.setDate(LocalDate.of(2026, 7, 1));
                    assertEquals(LocalDate.of(2026, 9, 15), f.controller.main.getCurrentDate());
                    return null;
                });
            } finally { release.countDown(); }
            assertEquals("Unable to edit payment", f.dialogs.next().getTitle());
            assertEquals(1, paths.size());
        }
    }

    @Test void editSuccessWaitsForTheRefreshAndARefreshFailureIsReported() throws Exception {
        var refreshing = new CountDownLatch(1); var release = new CountDownLatch(1);
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> {
            if (request.getMethod() == HttpMethod.PUT) return response(HttpStatus.OK, "");
            refreshing.countDown(); await(release); return response(HttpStatus.SERVICE_UNAVAILABLE, "{}");
        });
        try (var workers = Executors.newCachedThreadPool()) {
            Fixture f = fx(() -> fixture(rest, workers));
            try {
                fx(() -> { f.controller.editPayment(f.original); return null; });
                assertTrue(refreshing.await(5, TimeUnit.SECONDS));
                fx(() -> { assertTrue(f.dialogs.shown.isEmpty()); assertTrue(f.popover.isDisabled()); return null; });
            } finally { release.countDown(); }
            assertEquals("Refresh failed", f.dialogs.next().getTitle());
            fx(() -> { assertFalse(f.popover.isDisabled()); assertFalse(f.spinner.isVisible()); return null; });
        }
    }

    @Test void successfulEditRefreshesRowsAndTotalsBeforeSuccessIsShown() throws Exception {
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> response(HttpStatus.OK,
                request.getMethod() == HttpMethod.PUT ? "" : "[{\"storeID\":1,\"contactID\":16,\"invoiceNumber\":\"239791-1\",\"unitAmount\":1377.09}]"));
        try (var workers = Executors.newCachedThreadPool()) {
            Fixture f = fx(() -> fixture(rest, workers));
            fx(() -> { f.controller.editPayment(f.original); return null; });
            assertEquals("Success", f.dialogs.next().getTitle());
            fx(() -> {
                assertEquals("239791-1", f.table.getItems().getFirst().getInvoiceNumber());
                assertEquals("$1,377.09", f.total.getText());
                assertEquals(1377.09, f.contactTotals.getItems().getFirst().getTotalValue());
                assertFalse(f.spinner.isVisible()); return null;
            });
        }
    }

    @Test void deletionCannotClaimSuccessWhenAnOlderApiLeavesThePaymentPresent() throws Exception {
        var rest = new RestTemplate();
        rest.getInterceptors().add((request, body, execution) -> response(HttpStatus.OK,
                request.getMethod() == HttpMethod.DELETE ? "" : "[{\"storeID\":1,\"invoiceNumber\":\"19133 1\"}]"));
        try (var workers = Executors.newCachedThreadPool()) {
            Fixture f = fx(() -> fixture(rest, workers));
            fx(() -> { f.controller.deletePayment(f.original); return null; });
            var confirmation = f.dialogs.next();
            fx(() -> { confirmation.getOnClose().accept(ButtonType.OK); return null; });
            assertEquals("Unable to delete payment", f.dialogs.next().getTitle());
        }
    }

    private Fixture fixture(RestTemplate rest, ExecutorService workers) throws Exception {
        var controller = new AccountPaymentsPageController() { @Override public void closePopover() {} };
        var dialogs = new RecordingDialogs(); controller.dialogPane = dialogs; controller.executor = workers;
        var main = new Main(); var store = new Store(); store.setStoreID(1);
        main.setCurrentStore(store); main.setCurrentDate(LocalDate.of(2026, 9, 15)); controller.setMain(main);
        var props = new Properties(); props.setProperty("api.base.url", "https://example.invalid"); props.setProperty("api.token", "test-token");
        var constructor = AccountPaymentService.class.getDeclaredConstructor(RestTemplate.class, Properties.class);
        constructor.setAccessible(true); set(controller, "accountPaymentService", constructor.newInstance(rest, props));
        var contact = new AccountPaymentContactDataPoint(16, "In-store account", 1); contact.setAccountCode("200");
        var contactRest = new RestTemplate();
        contactRest.getInterceptors().add((request, body, execution) -> response(HttpStatus.OK,
                "[{\"contactID\":16,\"contactName\":\"In-store account\",\"storeID\":1,\"accountCode\":\"200\"}]"));
        var contactConstructor = AccountPaymentContactService.class.getDeclaredConstructor(RestTemplate.class, Properties.class);
        contactConstructor.setAccessible(true);
        set(controller, "accountPaymentContactService", contactConstructor.newInstance(contactRest, props));
        var afx = new ActionableFilterComboBox<AccountPaymentContactDataPoint>(new MFXButton(), new MFXButton());
        afx.getItems().add(contact); afx.getSelectionModel().selectItem(contact); set(controller, "afx", afx);
        set(controller, "invoiceNoField", new MFXTextField("239791"));
        set(controller, "descriptionField", new MFXTextField("September draft"));
        set(controller, "amountField", new MFXTextField("1377.09"));
        var invoiceDate = new MFXDatePicker(); invoiceDate.setValue(LocalDate.of(2026, 9, 15)); set(controller, "invoiceDateField", invoiceDate);
        var dueDate = new MFXDatePicker(); dueDate.setValue(LocalDate.of(2026, 9, 30)); set(controller, "dueDateField", dueDate);
        set(controller, "invoiceDateValidationLabel", new Label());
        set(controller, "accountAdjustedBox", new MFXCheckbox());
        var tax = new MFXComboBox<String>(); tax.setText("Gst Free Income"); set(controller, "taxRateField", tax);
        var popover = new VBox(); set(controller, "addPaymentPopover", popover); set(controller, "addList", new JFXNodesList());
        var table = new TableView<AccountPayment>(); set(controller, "accountPaymentTable", table);
        var totals = new MFXTableView<AccountPaymentContactDataPoint>(); set(controller, "accountTotalsTable", totals);
        var total = new Label(); set(controller, "supplierTotalLabel", total);
        var spinner = new MFXProgressSpinner(); set(controller, "progressSpinner", spinner); controller.monthSelector = new Region();
        var original = new AccountPayment("In-store account",16,1,"19133 1",LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),"Original description",1,12.34,true,"200","Gst Free Income");
        return new Fixture(controller, dialogs, original, popover, table, totals, total, spinner);
    }
    private record Fixture(AccountPaymentsPageController controller, RecordingDialogs dialogs, AccountPayment original,
                           VBox popover, TableView<AccountPayment> table, MFXTableView<AccountPaymentContactDataPoint> contactTotals,
                           Label total, MFXProgressSpinner spinner) {}
    private static class RecordingDialogs extends DialogPane {
        final BlockingQueue<Dialog<?>> shown = new LinkedBlockingQueue<>();
        @Override public void showDialog(Dialog<?> dialog) { shown.add(dialog); }
        Dialog<?> next() throws InterruptedException { var dialog = shown.poll(5, TimeUnit.SECONDS); assertNotNull(dialog, "Expected a dialog"); return dialog; }
    }
    private static void set(Object target, String name, Object value) throws Exception {
        var field = AccountPaymentsPageController.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static <T> T fx(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action); Platform.runLater(task); return task.get(10, TimeUnit.SECONDS);
    }
    private static void await(CountDownLatch latch) throws IOException {
        try { if (!latch.await(5, TimeUnit.SECONDS)) throw new IOException("Timed out waiting for test release"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
    }
    private static ClientHttpResponse response(HttpStatus status, String body) {
        return new ClientHttpResponse() {
            public HttpStatus getStatusCode() { return status; }
            public String getStatusText() { return status.getReasonPhrase(); }
            public HttpHeaders getHeaders() { var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); return headers; }
            public InputStream getBody() { return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)); }
            public void close() {}
        };
    }
}
