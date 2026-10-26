package controllers;

import application.Main;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import models.DocumentAiModels.*;
import models.Invoice;
import models.InvoiceReconciliation;
import models.InvoiceSupplier;
import models.ScannedInvoice;
import models.Store;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import services.DocumentPdfCache;
import services.InvoiceService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.YearMonth;
import java.nio.file.Files;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AiInvoiceScanControllerTest {
	private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
	private static boolean toolkitStarted;

	@BeforeAll
	static void startJavaFx() throws Exception {
		System.setProperty("java.awt.headless", "true");
		System.setProperty("glass.platform", "Headless");
		System.setProperty("prism.order", "sw");
		FutureTask<Void> started = new FutureTask<>(() -> { Platform.setImplicitExit(false); return null; });
		Platform.startup(started);
		started.get(10, TimeUnit.SECONDS);
		toolkitStarted = true;
	}

	@AfterAll
	static void stopJavaFx() { if (toolkitStarted) Platform.exit(); }

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void previewRefreshUpdatesTheExpectedAmountOrRetainsTheCacheOnFailureWithoutLosingDraftEdits(boolean succeeds) throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(1);
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getCachedInvoiceAmounts(int store, List<ScannedInvoice> rows) { return List.of(symbionImport("000123")); }
						@Override public List<Invoice> refreshInvoiceAmount(int store, ScannedInvoice row) {
							assertEquals(1, store); assertEquals("000123", row.invoiceNo());
							started.countDown();
							try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Refresh was not released"); }
							catch (InterruptedException exception) { throw new RuntimeException(exception); }
							if (!succeeds) throw new IllegalStateException("agent offline");
							var updated = symbionImport("000123"); updated.setImportedInvoiceAmount(150.25); return List.of(updated);
						}
					});
					setField(controller, "executor", workers);
					var scanned = new ScannedInvoice("invoice.pdf", "Symbion", "I000123", DATE, null, ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
					invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, List.of(scanned))));
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					invoke(controller, "review", InvoiceReconciliation.class, rows.getFirst());
					TextField amount = field(controller, "reviewAmountField"); amount.setText("105.00");
					TextArea notes = field(controller, "reviewNotesField"); notes.setText("Keep my draft");
					Label message = field(controller, "invoiceAmountRefreshMessageLabel");
					message.textProperty().addListener((_, _, value) -> { if (value.contains("refreshed") || value.contains("failed")) finished.countDown(); });
					Button refresh = field(controller, "refreshInvoiceAmountButton"); refresh.fire();
					assertTrue(refresh.isDisabled());
					Button accept = field(controller, "reviewAcceptButton"); assertTrue(accept.isDisabled());
				});
				assertTrue(started.await(5, TimeUnit.SECONDS)); release.countDown();
				assertTrue(finished.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0];
					Label expected = field(controller, "reviewExpectedAmountLabel");
					assertEquals(succeeds ? "$150.25" : "$100.00", expected.getText());
					TextField amount = field(controller, "reviewAmountField"); assertEquals("105.00", amount.getText());
					TextArea notes = field(controller, "reviewNotesField"); assertEquals("Keep my draft", notes.getText());
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					assertEquals(succeeds ? InvoiceReconciliation.Status.AMOUNT_MISMATCH : InvoiceReconciliation.Status.MATCHED, rows.getFirst().getStatus());
					Button refresh = field(controller, "refreshInvoiceAmountButton"); assertFalse(refresh.isDisabled());
					Button accept = field(controller, "reviewAcceptButton"); assertFalse(accept.isDisabled());
				});
			} finally {
				release.countDown();
				if (holder[0] != null) onFxThread(holder[0]::dispose);
			}
		}
	}

	@Test
	void aLateRefreshDoesNotReplaceTheExpectedAmountForTheNextPreviewedInvoice() throws Exception {
		CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), finished = new CountDownLatch(1);
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					var other = symbionImport("000456"); other.setImportedInvoiceAmount(200);
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getCachedInvoiceAmounts(int store, List<ScannedInvoice> rows) { return List.of(symbionImport("000123"), other); }
						@Override public List<Invoice> refreshInvoiceAmount(int store, ScannedInvoice row) {
							started.countDown();
							try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Refresh was not released"); }
							catch (InterruptedException exception) { throw new RuntimeException(exception); }
							var updated = symbionImport("000123"); updated.setImportedInvoiceAmount(150); return List.of(updated);
						}
					});
					setField(controller, "executor", workers);
					var first = new ScannedInvoice("first.pdf", "Symbion", "I000123", DATE, null, ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
					var second = new ScannedInvoice("second.pdf", "Symbion", "I000456", DATE, null, ScannedInvoice.DocumentType.INVOICE, 20000, 0.99);
					invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, List.of(first, second))));
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					invoke(controller, "review", InvoiceReconciliation.class, rows.getFirst());
					Button refresh = field(controller, "refreshInvoiceAmountButton"); refresh.fire();
					invoke(controller, "review", InvoiceReconciliation.class, rows.get(1));
					refresh.disabledProperty().addListener((_, _, disabled) -> { if (!disabled) finished.countDown(); });
				});
				assertTrue(started.await(5, TimeUnit.SECONDS)); release.countDown(); assertTrue(finished.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					Label expected = field(holder[0], "reviewExpectedAmountLabel"); assertEquals("$200.00", expected.getText());
					InvoiceReconciliation current = field(holder[0], "currentReview"); assertEquals("I000456", current.getInvoiceNo());
				});
			} finally { release.countDown(); if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@Test
	void correctingAnOcrReferenceCanRefreshItsAmountBeforeAcceptingTheDraft() throws Exception {
		CountDownLatch finished = new CountDownLatch(1);
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getCachedInvoiceAmounts(int store, List<ScannedInvoice> rows) { return List.of(); }
						@Override public List<Invoice> refreshInvoiceAmount(int store, ScannedInvoice row) {
							assertEquals("000123", row.invoiceNo()); return List.of(symbionImport("000123"));
						}
					});
					setField(controller, "executor", workers);
					var scanned = new ScannedInvoice("invoice.pdf", "Symbion", "OCR-WRONG", DATE, null, ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
					invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, List.of(scanned))));
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					invoke(controller, "review", InvoiceReconciliation.class, rows.getFirst());
					TextField reference = field(controller, "reviewReferenceField"); reference.setText("000123");
					Label message = field(controller, "invoiceAmountRefreshMessageLabel");
					message.textProperty().addListener((_, _, value) -> { if (value.contains("refreshed")) finished.countDown(); });
					Button refresh = field(controller, "refreshInvoiceAmountButton"); refresh.fire();
				});
				assertTrue(finished.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0];
					Label expected = field(controller, "reviewExpectedAmountLabel"); assertEquals("$100.00", expected.getText());
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, rows.getFirst().getStatus());
					Button accept = field(controller, "reviewAcceptButton"); accept.fire();
					assertEquals(InvoiceReconciliation.Status.ACCEPTED, rows.getFirst().getStatus());
					assertNull(field(controller, "currentReview"));
					Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments");
					assertEquals("000123", invoke(approvals.values().iterator().next(), "reference"));
				});
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@Test
	void acceptingPreviewAdvancesInVisibleOrderSkipsAcceptedRowsAndReturnsAfterTheLastPendingItem() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			InvoiceReconciliation first = pending("INV-1", 10000);
			InvoiceReconciliation second = pending("INV-2", 20000);
			InvoiceReconciliation third = pending("INV-3", 30000);
			try {
				showAcceptanceRows(controller, List.of(third, first, second,
						pending("INV-8", 80000).withStatus(InvoiceReconciliation.Status.SAVED),
						pending("INV-9", 90000).withStatus(InvoiceReconciliation.Status.MATCHED)), BatchKind.INVOICE_BATCH);
				CheckBox showAccepted = field(controller, "showMatchedCheck");
				showAccepted.setSelected(true);
				TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
				TableColumn<InvoiceReconciliation, ?> reference = table.getColumns().get(2);
				reference.setSortType(TableColumn.SortType.ASCENDING);
				table.getSortOrder().setAll(List.of(reference));
				table.sort();
				assertEquals(List.of("INV-1", "INV-2", "INV-3", "INV-8", "INV-9"),
						table.getItems().stream().map(InvoiceReconciliation::getInvoiceNo).toList());
				invoke(controller, "review", InvoiceReconciliation.class, second);
				TextField amount = field(controller, "reviewAmountField");
				TextArea notes = field(controller, "reviewNotesField");
				amount.setText("123.45");
				notes.setText("Checked in preview");
				Button accept = field(controller, "reviewAcceptButton");
				accept.fire();
				assertSame(third, field(controller, "currentReview"));
				assertEquals("300.00", amount.getText());
				assertEquals("", notes.getText());
				Map<ScannedInvoice, ?> accepted = field(controller, "acceptedDocuments");
				assertEquals(123.45, invoke(accepted.get(second.getScanned()), "amount"));
				assertEquals("Checked in preview", invoke(accepted.get(second.getScanned()), "notes"));
				accept.fire();
				assertSame(first, field(controller, "currentReview"));
				assertEquals("Accept", accept.getText());
				accept.fire();
				assertNull(field(controller, "currentReview"));
				javafx.scene.layout.BorderPane preview = field(controller, "reviewView");
				javafx.scene.layout.VBox results = field(controller, "resultsView");
				assertFalse(preview.isVisible());
				assertTrue(results.isVisible());
				assertEquals(3, accepted.size());
			} finally { controller.dispose(); }
		});
	}

	@Test
	void invalidPreviewValuesKeepTheSameItemOpenUntilCorrected() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			InvoiceReconciliation first = pending("INV-1", 10000);
			InvoiceReconciliation second = pending("INV-2", 20000);
			try {
				showAcceptanceRows(controller, List.of(first, second), BatchKind.STATEMENT);
				invoke(controller, "review", InvoiceReconciliation.class, first);
				TextField reference = field(controller, "reviewReferenceField");
				reference.clear();
				Button accept = field(controller, "reviewAcceptButton");
				accept.fire();
				assertSame(first, field(controller, "currentReview"));
				Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments");
				assertTrue(approvals.isEmpty());
				reference.setText("CORRECTED-1");
				accept.fire();
				assertSame(second, field(controller, "currentReview"));
				assertEquals("CORRECTED-1", invoke(approvals.get(first.getScanned()), "reference"));
			} finally { controller.dispose(); }
		});
	}

	@ParameterizedTest
	@EnumSource(BatchKind.class)
	void multiSelectionAcceptsOnlyChosenRowsThenSelectAllAcceptsTheRemainingCredit(BatchKind kind) throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			InvoiceReconciliation first = pending("INV-1", 10000);
			InvoiceReconciliation credit = pending("CR-1", -2550);
			InvoiceReconciliation last = pending("INV-3", 30000);
			try {
				showAcceptanceRows(controller, List.of(first, credit, last), kind);
				TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
				assertEquals(SelectionMode.MULTIPLE, table.getSelectionModel().getSelectionMode());
				table.getSelectionModel().clearSelection();
				table.getSelectionModel().selectIndices(0, 2);
				Button accept = field(controller, "acceptSelectedButton");
				assertEquals("Accept selected (2)", accept.getText());
				accept.fire();
				Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments");
				assertEquals(2, approvals.size());
				assertFalse(approvals.containsKey(credit.getScanned()));
				assertEquals(List.of(credit), List.copyOf(table.getItems()));
				assertTrue(accept.isDisabled());
				Button selectAll = field(controller, "selectAllResultsButton");
				selectAll.fire();
				assertEquals("Accept selected (1)", accept.getText());
				Button clear = field(controller, "clearResultSelectionButton");
				clear.fire();
				assertTrue(accept.isDisabled());
				selectAll.fire();
				accept.fire();
				assertEquals(3, approvals.size());
				assertEquals(ScannedInvoice.DocumentType.CREDIT, invoke(approvals.get(credit.getScanned()), "documentType"));
				assertEquals(25.50, invoke(approvals.get(credit.getScanned()), "amount"));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertTrue(rows.stream().allMatch(row -> row.getStatus() == InvoiceReconciliation.Status.ACCEPTED));
				Button save = field(controller, "saveAcceptedButton");
				assertFalse(save.isDisabled());
			} finally { controller.dispose(); }
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "supplier", "date", "reference" })
	void bulkAcceptanceValidatesEverySelectedRowBeforeChangingAnyOfThem(String missing) throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			ScannedInvoice invalid = new ScannedInvoice("needs-correction.pdf", missing.equals("supplier") ? "Unknown" : "Supplier",
					missing.equals("reference") ? "" : "INV-2", missing.equals("date") ? null : DATE, null,
					ScannedInvoice.DocumentType.INVOICE, 20000, 0.99);
			try {
				showAcceptanceRows(controller, List.of(pending("INV-1", 10000), new InvoiceReconciliation(
						InvoiceReconciliation.Status.NEEDS_REVIEW, invalid, null, null)), BatchKind.STATEMENT);
				Button select = field(controller, "selectAllResultsButton");
				select.fire();
				Button accept = field(controller, "acceptSelectedButton");
				accept.fire();
				Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments");
				assertTrue(approvals.isEmpty());
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertTrue(rows.stream().noneMatch(InvoiceReconciliation::isAccepted));
				Label message = field(controller, "bulkReviewMessageLabel");
				assertTrue(message.isVisible());
				assertTrue(message.getText().contains("Nothing accepted"));
				TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
				assertEquals(2, table.getSelectionModel().getSelectedItems().size());
			} finally { controller.dispose(); }
		});
	}

	@Test
	void bulkAcceptanceSkipsAcceptedEditsAndSavedRowsAndIsDisabledDuringSaving() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			InvoiceReconciliation edited = pending("EDITED", 10000).withStatus(InvoiceReconciliation.Status.ACCEPTED);
			InvoiceReconciliation saved = pending("SAVED", 10000).withStatus(InvoiceReconciliation.Status.SAVED);
			try {
				showAcceptanceRows(controller, List.of(edited, saved, pending("INV-3", 30000)), BatchKind.INVOICE_BATCH);
				Object existing = record("AcceptedDocument", 1, "Supplier", ScannedInvoice.DocumentType.INVOICE,
						"EDITED", DATE, DATE.plusDays(30), 42.00, "Keep my notes");
				Map<ScannedInvoice, Object> approvals = field(controller, "acceptedDocuments");
				approvals.put(edited.getScanned(), existing);
				CheckBox show = field(controller, "showMatchedCheck");
				show.setSelected(true);
				Button select = field(controller, "selectAllResultsButton");
				select.fire();
				TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
				assertEquals(1, table.getSelectionModel().getSelectedItems().size());
				invoke(controller, "setCommitBusy", boolean.class, true);
				Button accept = field(controller, "acceptSelectedButton");
				assertTrue(accept.isDisabled());
				invoke(controller, "acceptSelected");
				assertEquals(1, approvals.size());
				invoke(controller, "setCommitBusy", boolean.class, false);
				accept.fire();
				assertSame(existing, approvals.get(edited.getScanned()));
				assertEquals(2, approvals.size());
				assertFalse(approvals.containsKey(saved.getScanned()));
			} finally { controller.dispose(); }
		});
	}

	@ParameterizedTest
	@CsvSource({ "STATEMENT,true", "INVOICE_BATCH,true", "INVOICE_BATCH,false" })
	void folderWarningsRemainVisibleAndPreventAutomaticAcceptance(BatchKind kind, boolean flagged) throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			Invoice imported = new Invoice();
			imported.setInvoiceNo("INV-1");
			imported.setImportExists(true);
			imported.setImportedInvoiceAmount(100);
			configureReconciliation(controller, new InvoiceService() {
				@Override public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> requestedRows) { return List.of(imported); }
				@Override public Invoice getInvoice(String reference, int storeId) { throw new AssertionError("Already loaded"); }
			});
			String warning = flagged ? "Wrong folder: check the detected document type before saving." : null;
			BatchSummary batch = new BatchSummary(17, kind, BatchStatus.REVIEW_REQUIRED, "Source", 1, 1, null, warning);
			assertEquals(flagged ? warning : "Ready to open and review", batch.getStatusDetail());
			BatchDetail detail = new BatchDetail(batch, List.of(new ExtractedDocument(101, "source.pdf",
					List.of(scanned("INV-1", 10000)), List.of(), null, null, null, null, warning)));
			try (DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> false)) {
				setField(controller, "pdfCache", cache);
				Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
				load.setAccessible(true);
				Object loaded = load.invoke(controller, detail, List.of(new InvoiceSupplier(1, "Supplier", 1)));
				invoke(controller, "showLoadedBatches", List.class, List.of(loaded));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				InvoiceReconciliation row = rows.getFirst();
				assertEquals(flagged ? InvoiceReconciliation.Status.NEEDS_REVIEW : InvoiceReconciliation.Status.MATCHED, row.getStatus());
				Button save = field(controller, "saveAcceptedButton");
				assertEquals(flagged, save.isDisabled());
				invoke(controller, "review", InvoiceReconciliation.class, row);
				Label label = field(controller, "reviewClassificationWarningLabel");
				assertEquals(flagged, label.isVisible());
				if (flagged) {
					assertEquals(warning, label.getText());
					invoke(controller, "acceptReview");
					assertEquals(InvoiceReconciliation.Status.ACCEPTED, rows.getFirst().getStatus());
					assertFalse(save.isDisabled());
				}
			} finally {
				controller.dispose();
			}
		});
	}

	@Test
	void emptyStatementPrimaryActionRequeuesAndStartsScanningInsteadOfOpeningBlankResults() throws Exception {
		List<String> calls = new CopyOnWriteArrayList<>();
		CountDownLatch scanned = new CountDownLatch(1);
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try {
			onFxThread(() -> {
				AiInvoiceScanController controller = controller();
				holder[0] = controller;
				configureReconciliation(controller, new InvoiceService());
				setField(controller, "executor", Executors.newSingleThreadExecutor());
				var rest = new org.springframework.web.client.RestTemplate() {
					@Override @SuppressWarnings("unchecked")
					public <T> org.springframework.http.ResponseEntity<T> exchange(String url,
							org.springframework.http.HttpMethod method, org.springframework.http.HttpEntity<?> entity,
							Class<T> type, Object... variables) {
						if (url.endsWith("/review-batches/17/retry?storeId=1")) calls.add("retry:17:1");
						else if (url.endsWith("/stores/1/scan-now")) { calls.add("scan:1"); scanned.countDown(); }
						else if (!url.endsWith("/stores/1/review-queue")) throw new AssertionError("Unexpected request: " + url);
						return org.springframework.http.ResponseEntity.ok(type == String.class ? (T) "[]" : null);
					}
				};
				var constructor = services.DocumentAiService.class.getDeclaredConstructor(
						org.springframework.web.client.RestTemplate.class, java.util.Properties.class);
				constructor.setAccessible(true);
				setField(controller, "documentAiService", constructor.newInstance(rest, new java.util.Properties()));
				BatchSummary empty = new BatchSummary(17, BatchKind.STATEMENT, BatchStatus.REVIEW_REQUIRED,
						"CH2 statement", 1, 0, null, null);
				invoke(controller, "handleBatchPrimary", BatchSummary.class, empty);
			});
			assertTrue(scanned.await(5, TimeUnit.SECONDS));
			assertEquals(List.of("retry:17:1", "scan:1"), calls);
		} finally {
			if (holder[0] != null) onFxThread(holder[0]::dispose);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void hillsideTransferSelectsOnlyTheHillsideContactOrRequiresSupplierReview(boolean hillsideAvailable) throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			Invoice imported = new Invoice();
			imported.setInvoiceNo("1419");
			imported.setImportExists(true);
			imported.setImportedInvoiceAmount(519.49);
			configureReconciliation(controller, new InvoiceService() {
				@Override public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> requestedRows) { return List.of(imported); }
				@Override public Invoice getInvoice(String reference, int storeId) { throw new AssertionError("The match is already loaded"); }
			});
			InvoiceSupplier gladstone = new InvoiceSupplier(20, "Direct Chemist Outlet Gladstone", 1);
			InvoiceSupplier hillside = new InvoiceSupplier(21, "Direct Chemist Outlet Hillside", 1);
			List<InvoiceSupplier> suppliers = hillsideAvailable ? List.of(gladstone, hillside) : List.of(gladstone);
			try (DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> false)) {
				setField(controller, "pdfCache", cache);
				ScannedInvoice scanned = new ScannedInvoice("transfer.pdf", "DCO HILLSIDE", "1419", DATE, null,
						ScannedInvoice.DocumentType.INVOICE, 51954, 0.99);
				BatchSummary batch = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED,
						"Transfers", 1, 1, null, null);
				BatchDetail detail = new BatchDetail(batch, List.of(new ExtractedDocument(101, "transfer.pdf", List.of(scanned),
						List.of(), null, null, null, null, null)));
				Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
				load.setAccessible(true);
				invoke(controller, "showLoadedBatches", List.class, List.of(load.invoke(controller, detail, suppliers)));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				InvoiceReconciliation row = rows.getFirst();
				assertEquals(hillsideAvailable ? hillside.getSupplierName() : "DCO HILLSIDE", row.getScanned().supplierName());
				assertEquals(hillsideAvailable ? InvoiceReconciliation.Status.WITHIN_TOLERANCE : InvoiceReconciliation.Status.NEEDS_REVIEW,
						row.getStatus());
				invoke(controller, "review", InvoiceReconciliation.class, row);
				ComboBox<InvoiceSupplier> choice = field(controller, "reviewSupplierChoice");
				assertSame(hillsideAvailable ? hillside : null, choice.getValue());
			} finally {
				controller.dispose();
			}
		});
	}

	@Test
	void zeroAmountsConfirmedByZOfficeAreReadyToSaveAndCanBeAcceptedInTheReviewForm() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			Invoice imported = symbionImport("000123");
			imported.setImportedInvoiceAmount(0);
			List<Invoice> saved = new ArrayList<>();
			configureReconciliation(controller, new InvoiceService() {
				@Override public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> requestedRows) { return List.of(imported); }
				@Override public Invoice getInvoice(String reference, int storeId) { throw new AssertionError("The match is already loaded"); }
				@Override public void saveOrUpdateInvoice(Invoice invoice, String originalReference, int originalSupplierId) { saved.add(invoice); }
			});
			try {
				ScannedInvoice scanned = new ScannedInvoice("statement.pdf", "Symbion", "I000123", DATE, null,
						ScannedInvoice.DocumentType.INVOICE, 0,
						new ScannedInvoice.FieldConfidences(0.99, 0.99, 0.99, null, 0.99, 0.0));
				invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, List.of(scanned))));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				InvoiceReconciliation row = rows.getFirst();
				assertEquals(InvoiceReconciliation.Status.MATCHED, row.getStatus());
				Button saveButton = field(controller, "saveAcceptedButton");
				assertTrue(saveButton.isVisible());
				assertFalse(saveButton.isDisabled());
				Object accepted = invoke(controller, "acceptedDocumentFromScanned", InvoiceReconciliation.class, row);
				Method save = AiInvoiceScanController.class.getDeclaredMethod("saveDocument", InvoiceReconciliation.class, accepted.getClass());
				save.setAccessible(true);
				save.invoke(controller, row, accepted);
				assertEquals(0, saved.getFirst().getUnitAmount());
				assertEquals("000123", saved.getFirst().getInvoiceNo());

				invoke(controller, "review", InvoiceReconciliation.class, row);
				TextField amount = field(controller, "reviewAmountField");
				assertEquals("0.00", amount.getText());
				amount.setText("-0.01");
				invoke(controller, "acceptReview");
				Label error = field(controller, "reviewErrorLabel");
				assertTrue(error.isVisible());
				Map<ScannedInvoice, ?> acceptedByRow = field(controller, "acceptedDocuments");
				assertTrue(acceptedByRow.isEmpty());
				amount.setText("0.00");
				invoke(controller, "acceptReview");
				assertFalse(error.isVisible());
				assertEquals(InvoiceReconciliation.Status.ACCEPTED, rows.getFirst().getStatus());
				assertNotNull(acceptedByRow.get(row.getScanned()));
			} finally { controller.dispose(); }
		});
	}

	@Test
	void symbionPrefixMatchesLoadedDataAndKeepsTheNumericReferenceForReviewAndSaving() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			Invoice imported = symbionImport("000123");
			List<Invoice> saved = new ArrayList<>();
			InvoiceService service = new InvoiceService() {
				@Override public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> requestedRows) { return List.of(imported); }
				@Override public Invoice getInvoice(String reference, int storeId) {
					throw new AssertionError("The numeric reference is already loaded; no extra query is needed");
				}
				@Override public void saveOrUpdateInvoice(Invoice invoice, String originalReference, int originalSupplierId) {
					assertEquals("000123", originalReference);
					assertEquals(9, originalSupplierId);
					saved.add(invoice);
				}
			};
			try {
				configureReconciliation(controller, service);
				ScannedInvoice scanned = new ScannedInvoice("statement.pdf", "Symbion", "I000123", DATE, null,
						ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
				invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, List.of(scanned))));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				InvoiceReconciliation row = rows.getFirst();
				assertEquals(InvoiceReconciliation.Status.MATCHED, row.getStatus());
				assertEquals("I000123", row.getScanned().invoiceNo());
				Object accepted = invoke(controller, "acceptedDocumentFromScanned", InvoiceReconciliation.class, row);
				Method save = AiInvoiceScanController.class.getDeclaredMethod("saveDocument", InvoiceReconciliation.class, accepted.getClass());
				save.setAccessible(true);
				save.invoke(controller, row, accepted);
				assertEquals("000123", saved.getFirst().getInvoiceNo());
				assertEquals(9, saved.getFirst().getSupplierID());

				invoke(controller, "review", InvoiceReconciliation.class, row);
				TextField reference = field(controller, "reviewReferenceField");
				assertEquals("000123", reference.getText());
				reference.setText("USER-EDITED");
				invoke(controller, "acceptReview");
				Map<ScannedInvoice, ?> acceptedByRow = field(controller, "acceptedDocuments");
				save.invoke(controller, rows.getFirst(), acceptedByRow.get(row.getScanned()));
				assertEquals("USER-EDITED", saved.getLast().getInvoiceNo());
			} finally { controller.dispose(); }
		});
	}

	@Test
	void denseStatementMatchesTwoHundredAndTwoRowsUsingOneMonthlyRequest() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			List<ScannedInvoice> scanned = new ArrayList<>();
			List<Invoice> imported = new ArrayList<>();
			for (int index = 0; index < 202; index++) {
				String number = "000" + index;
				Invoice invoice = symbionImport(number);
				if (index < 45) invoice.setImportedInvoiceAmount(0);
				imported.add(invoice);
				scanned.add(new ScannedInvoice("statement.pdf", "Symbion", "I" + number, DATE, null,
						ScannedInvoice.DocumentType.INVOICE, index < 45 ? 0 : 10000,
						new ScannedInvoice.FieldConfidences(0.99, 0.99, 0.99, null, 0.99, index < 45 ? 0.0 : 0.99)));
			}
			AtomicInteger requests = new AtomicInteger();
			configureReconciliation(controller, new InvoiceService() {
				@Override public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> requestedRows) {
					assertEquals(1, storeId);
					assertFalse(requestedRows.isEmpty());
					requests.incrementAndGet();
					return imported;
				}
				@Override public Invoice getInvoice(String reference, int storeId) {
					throw new AssertionError("A monthly match must not cause a per-row network request");
				}
			});
			try {
				BatchSummary summary = new BatchSummary(1, BatchKind.STATEMENT, BatchStatus.REVIEW_REQUIRED,
						"Statement", 1, scanned.size(), null, null);
				BatchDetail detail = new BatchDetail(summary, List.of(new ExtractedDocument(101, "statement.pdf", scanned,
						List.of(), DATE.withDayOfMonth(1), DATE.withDayOfMonth(30), null, null, null)));
				Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
				load.setAccessible(true);
				Object loaded = load.invoke(controller, detail, List.of(new InvoiceSupplier(9, "Symbion Pty Ltd", 1)));
				invoke(controller, "showLoadedBatches", List.class, List.of(loaded));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertEquals(202, rows.size());
				assertTrue(rows.stream().allMatch(row -> row.getStatus() == InvoiceReconciliation.Status.MATCHED));
				assertEquals(1, requests.get());
			} finally { controller.dispose(); }
		});
	}

	@Test
	void combinedReviewSharesCachedReferencesAndMissesWithoutIndividualLookups() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			AtomicInteger requests = new AtomicInteger();
			List<String> missing = new ArrayList<>();
			configureReconciliation(controller, new InvoiceService() {
				@Override public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> requestedRows) {
					requests.incrementAndGet();
					return List.of(symbionImport("000123"));
				}
				@Override public Invoice getInvoice(String reference, int storeId) { missing.add(reference); return null; }
			});
			try {
				Class<?> contextType = Class.forName("controllers.AiInvoiceScanController$ReviewLoadContext");
				Constructor<?> constructor = contextType.getDeclaredConstructor(int.class, YearMonth.class);
				constructor.setAccessible(true);
				Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class, contextType);
				load.setAccessible(true);
				Object context = constructor.newInstance(1, YearMonth.from(DATE));
				List<ScannedInvoice> scanned = List.of(
						new ScannedInvoice("invoice.pdf", "Symbion", "I000123", DATE, null, ScannedInvoice.DocumentType.INVOICE, 10000, 0.99),
						new ScannedInvoice("invoice.pdf", "Symbion", "I999", DATE, null, ScannedInvoice.DocumentType.INVOICE, 10000, 0.99));
				BatchSummary summary = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED, "Test", 1, 2, null, null);
				BatchDetail detail = new BatchDetail(summary, List.of(new ExtractedDocument(101, "invoice.pdf", scanned,
						List.of(), null, null, null, null, null)));
				List<InvoiceSupplier> suppliers = List.of(new InvoiceSupplier(9, "Symbion Pty Ltd", 1));
				load.invoke(controller, detail, suppliers, context);
				load.invoke(controller, detail, suppliers, context);
				assertEquals(1, requests.get());
				assertTrue(missing.isEmpty());
				load.invoke(controller, detail, suppliers, constructor.newInstance(1, YearMonth.from(DATE)));
				assertEquals(2, requests.get());
				assertTrue(missing.isEmpty());
			} finally { controller.dispose(); }
		});
	}

	@Test
	void cachedSymbionCanonicalReferencesAndMissesDoNotNeedIndividualLookups() throws Exception {
		onFxThread(() -> {
			for (boolean found : List.of(true, false)) {
				AiInvoiceScanController controller = controller();
				List<String> requested = new ArrayList<>();
				Invoice imported = symbionImport("000123");
				configureReconciliation(controller, new InvoiceService() {
					@Override public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> requestedRows) {
                        requestedRows.forEach(row -> requested.add(row.invoiceNo()));
                        return found ? List.of(imported) : List.of();
                    }
					@Override public Invoice getInvoice(String reference, int storeId) {
						assertEquals(1, storeId);
						requested.add(reference);
						return found && reference.equals("000123") ? imported : null;
					}
				});
				try {
					ScannedInvoice scanned = new ScannedInvoice("statement.pdf", "Symbion", "I000123", DATE, null,
							ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
					invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, List.of(scanned, scanned))));
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					assertEquals(2, rows.size());
					assertTrue(rows.stream().allMatch(row -> row.getStatus() == (found
							? InvoiceReconciliation.Status.MATCHED : InvoiceReconciliation.Status.NOT_IN_Z_OFFICE)));
					assertEquals(List.of("I000123"), requested);
				} finally { controller.dispose(); }
			}
		});
	}

	@Test
	void actionButtonsSurviveRapidAcceptedRowTogglesAndCellReuse() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
			Parent root = table;
			while (root.getParent() != null) root = root.getParent();
			Stage stage = new Stage();
			try {
				stage.setScene(new Scene(root, 2200, 900));
				stage.show();
				invoke(controller, "showLoadedBatches", List.class, List.of(
						loaded(1, 101, BatchKind.INVOICE_BATCH, List.of(scanned("ISSUE-1", 10000),
								scanned("ISSUE-2", 20000), scanned("ISSUE-3", 30000),
								scanned("ISSUE-4", 40000), scanned("ISSUE-5", 50000)), null,
								InvoiceReconciliation.Status.NOT_IN_Z_OFFICE),
						loaded(2, 102, BatchKind.INVOICE_BATCH, List.of(scanned("MATCH-1", 10000),
								scanned("MATCH-2", 20000), scanned("MATCH-3", 30000), scanned("MATCH-4", 40000)), null),
						loaded(3, 103, BatchKind.INVOICE_BATCH, List.of(scanned("SAVED", 10000)), null,
								InvoiceReconciliation.Status.SAVED)));
				CheckBox showAccepted = field(controller, "showMatchedCheck");
				for (int toggle = 0; toggle < 30; toggle++) {
					showAccepted.setSelected(toggle % 2 != 0);
					root.applyCss();
					root.layout();
					assertEquals(showAccepted.isSelected() ? 10 : 5, table.getItems().size());
					assertActionButtons(table);
				}

				// Reuse cells after replacing the rows and their saved/unsaved states.
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				List<InvoiceReconciliation> reversed = List.copyOf(rows.reversed());
				rows.setAll(reversed);
				invoke(controller, "applyResultFilter");
				root.applyCss();
				root.layout();
				assertActionButtons(table);
				TableCell<?, ?> reviewCell = actionCells(table).stream()
						.filter(cell -> table.getItems().get(cell.getIndex()).getStatus() != InvoiceReconciliation.Status.SAVED)
						.findFirst().orElseThrow();
				InvoiceReconciliation expected = table.getItems().get(reviewCell.getIndex());
				((Button) ((HBox) reviewCell.getGraphic()).getChildren().getFirst()).fire();
				assertSame(expected, field(controller, "currentReview"));
			} finally {
				controller.dispose();
				stage.close();
			}
		});
	}

	@Test
	void listWarmsTheSelectedInvoiceAndReviewFetchesOnlyTheNextDistinctVisiblePdf() throws Exception {
		byte[] pdf = pdf(72);
		List<Long> requested = new CopyOnWriteArrayList<>();
		CountDownLatch firstStarted = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CountDownLatch nextStarted = new CountDownLatch(1);
		CountDownLatch rendered = new CountDownLatch(1);
		try (var workers = Executors.newCachedThreadPool();
			 DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
				 assertEquals(7, store);
				 requested.add(document);
				 if (document == 101) {
					 firstStarted.countDown();
					 try {
						 if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new IOException("Test download timed out");
					 } catch (InterruptedException exception) { throw new IOException(exception); }
				 }
				 Files.write(path, pdf);
				 if (document == 102) nextStarted.countDown();
				 return true;
			 })) {
			AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
			try {
				onFxThread(() -> {
					AiInvoiceScanController controller = controller();
					holder[0] = controller;
					setField(controller, "executor", workers);
					setField(controller, "pdfCache", cache);
					setField(controller, "reviewStoreId", 7);
					invoke(controller, "showLoadedBatches", List.class, List.of(
							loaded(1, 101, BatchKind.INVOICE_BATCH,
									List.of(scanned("INV-1", 10000), scanned("INV-2", 20000)), null,
									InvoiceReconciliation.Status.NEEDS_REVIEW),
							loaded(2, 999, BatchKind.INVOICE_BATCH, List.of(scanned("SAVED", 10000)), null,
									InvoiceReconciliation.Status.SAVED),
							loaded(3, 102, BatchKind.INVOICE_BATCH, List.of(scanned("INV-3", 30000)), null,
									InvoiceReconciliation.Status.NEEDS_REVIEW),
							loaded(4, 103, BatchKind.INVOICE_BATCH, List.of(scanned("INV-4", 40000)), null,
									InvoiceReconciliation.Status.NEEDS_REVIEW)));
					TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
					assertTrue(table.isVisible());
					assertEquals(4, table.getItems().size());
					ImageView preview = field(controller, "pdfPreviewImage");
					assertNull(preview.getImage());
					preview.imageProperty().addListener((_, _, image) -> { if (image != null) rendered.countDown(); });
				});
				assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
				assertEquals(List.of(101L), requested);
				onFxThread(() -> {
					TableView<InvoiceReconciliation> table = field(holder[0], "resultsTable");
					invoke(holder[0], "review", InvoiceReconciliation.class, table.getItems().getFirst());
				});
				releaseFirst.countDown();
				assertTrue(rendered.await(10, TimeUnit.SECONDS));
				assertTrue(nextStarted.await(5, TimeUnit.SECONDS));
				assertEquals(List.of(101L, 102L), requested);
			} finally {
				releaseFirst.countDown();
				if (holder[0] != null) onFxThread(holder[0]::dispose);
			}
		}
	}

	@Test
	void loadingBatchBuildsReconciliationWithoutDownloadingSourcePdfs() throws Exception {
		AtomicInteger downloads = new AtomicInteger();
		try (DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
			downloads.incrementAndGet();
			throw new IOException("Source PDF unavailable");
		})) {
			onFxThread(() -> {
				AiInvoiceScanController controller = controller();
				Main main = new Main();
				Store store = new Store();
				store.setStoreID(1);
				main.setCurrentStore(store);
				main.setCurrentDate(DATE);
				controller.setMain(main);
				Invoice imported = new Invoice();
				imported.setInvoiceNo("INV-1");
				imported.setImportExists(true);
				imported.setImportedInvoiceAmount(100);
				setField(controller, "invoiceService", new InvoiceService() {
					@Override public List<Invoice> getCachedInvoiceAmounts(int storeId, List<ScannedInvoice> requestedRows) {
						assertEquals(1, storeId);
						assertFalse(requestedRows.isEmpty());
						return List.of(imported);
					}
					@Override public Invoice getInvoice(String reference, int storeId) {
						throw new AssertionError("The imported reference should already be available");
					}
				});
				setField(controller, "pdfCache", cache);
				BatchSummary batch = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED,
						"Invoices", 2, 2, null, null);
				BatchDetail detail = new BatchDetail(batch, List.of(
						new ExtractedDocument(101, "first.pdf", List.of(scanned("INV-1", 10000)),
								List.of(), null, null, null, null, null),
						new ExtractedDocument(102, "second.pdf", List.of(scanned("INV-1", 10000)),
								List.of(), null, null, null, null, null)));
				Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
				load.setAccessible(true);
				Object loaded = load.invoke(controller, detail, List.of(new InvoiceSupplier(1, "Supplier", 1)));
				assertEquals(0, downloads.get());
				invoke(controller, "showLoadedBatches", List.class, List.of(loaded));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertEquals(2, rows.size());
				assertTrue(rows.stream().allMatch(row -> row.getStatus() == InvoiceReconciliation.Status.MATCHED));
				Map<ScannedInvoice, Long> ids = field(controller, "documentIdsByRow");
				assertEquals(101L, ids.get(rows.get(0).getScanned()));
				assertEquals(102L, ids.get(rows.get(1).getScanned()));
			});
		}
	}

	@Test
	void switchingInvoicesKeepsTheLatestPreviewWhenAnEarlierDownloadFinishesLate() throws Exception {
		byte[] firstPdf = pdf(72);
		byte[] secondPdf = pdf(144);
		CountDownLatch firstStarted = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CountDownLatch secondRendered = new CountDownLatch(1);
		try (var workers = Executors.newCachedThreadPool();
			 DocumentPdfCache cache = new DocumentPdfCache((document, store, path, limit) -> {
				 assertEquals(7, store);
				 if (document == 101) {
					 firstStarted.countDown();
					 try {
						 if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new IOException("Test download timed out");
					 } catch (InterruptedException exception) { throw new IOException(exception); }
					 Files.write(path, firstPdf);
					 return true;
				 }
				 Files.write(path, secondPdf);
				 return true;
			 })) {
			AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
			try {
				onFxThread(() -> {
					AiInvoiceScanController controller = controller();
					holder[0] = controller;
					setField(controller, "executor", workers);
					setField(controller, "pdfCache", cache);
					setField(controller, "reviewStoreId", 7);
					invoke(controller, "showLoadedBatches", List.class, List.of(
							loaded(1, 101, BatchKind.INVOICE_BATCH, List.of(scanned("INV-1", 10000)), null),
							loaded(2, 102, BatchKind.INVOICE_BATCH, List.of(scanned("INV-2", 20000)), null)));
					ImageView preview = field(controller, "pdfPreviewImage");
					preview.imageProperty().addListener((_, _, image) -> {
						if (image != null && image.getWidth() == 288) secondRendered.countDown();
					});
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					invoke(controller, "showEvidence", InvoiceReconciliation.class, rows.getFirst());
				});
				assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					ObservableList<InvoiceReconciliation> rows = field(holder[0], "allResults");
					invoke(holder[0], "showEvidence", InvoiceReconciliation.class, rows.getLast());
				});
				assertTrue(secondRendered.await(10, TimeUnit.SECONDS));
				releaseFirst.countDown();
				workers.shutdown();
				assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
				onFxThread(() -> {
					ImageView preview = field(holder[0], "pdfPreviewImage");
					assertEquals(288, preview.getImage().getWidth());
					assertEquals(102L, (Long) field(holder[0], "currentPreviewDocumentId"));
				});
			} finally {
				releaseFirst.countDown();
				if (holder[0] != null) onFxThread(holder[0]::dispose);
			}
		}
	}

	@Test
	void reviewAllIncludesOnlyReadyInvoiceBatchesAndRequiresExceptionsToBeAccepted() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			ObservableList<BatchSummary> queue = field(controller, "queuedBatches");
			queue.setAll(new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED, "Ready", 1, 1, null, null),
					new BatchSummary(2, BatchKind.INVOICE_BATCH, BatchStatus.FAILED, "Failed", 1, 1, null, "Failure"),
					new BatchSummary(3, BatchKind.INVOICE_BATCH, BatchStatus.SCANNING, "Scanning", 1, 1, null, null),
					new BatchSummary(4, BatchKind.STATEMENT, BatchStatus.REVIEW_REQUIRED, "Statement", 1, 1, null, null));
			invoke(controller, "updateQueueVisualState");
			Button reviewAll = field(controller, "reviewAllButton");
			assertEquals("Review all invoices (1)", reviewAll.getText());
			assertFalse(reviewAll.isDisabled());
			assertEquals(List.of(queue.getFirst()), invoke(controller, "readyInvoiceBatches"));
			queue.removeFirst();
			invoke(controller, "updateQueueVisualState");
			assertEquals(List.of(), invoke(controller, "readyInvoiceBatches"));
			assertEquals("Review all invoices", reviewAll.getText());
			assertTrue(reviewAll.isDisabled());

			invoke(controller, "showLoadedBatches", List.class,
					List.of(loaded(1, 101, BatchKind.INVOICE_BATCH, List.of(scanned("INV-1", 10000)), null)));
			ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
			rows.set(0, rows.getFirst().withStatus(InvoiceReconciliation.Status.NEEDS_REVIEW));
			invoke(controller, "updateAcceptanceControls");
			Button save = field(controller, "saveAcceptedButton");
			assertFalse(save.isVisible());
			assertTrue(save.isDisabled());
		});
	}

	@Test
	void combinedReviewKeepsIdenticalScannedRowsAttachedToTheirOwnPdfs() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			ScannedInvoice first = scanned("INV-1", 10000);
			ScannedInvoice second = scanned("INV-1", 10000);
			assertEquals(first, second);
			invoke(controller, "showLoadedBatches", List.class, List.of(
					loaded(1, 101, BatchKind.INVOICE_BATCH, List.of(first), null),
					loaded(2, 201, BatchKind.INVOICE_BATCH, List.of(second), null)));

			Map<ScannedInvoice, Long> ids = field(controller, "documentIdsByRow");
			assertEquals(2, ids.size());
			assertEquals(101L, ids.get(first));
			assertEquals(201L, ids.get(second));
			ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
			assertEquals(2, rows.size());
			Button save = field(controller, "saveAcceptedButton");
			assertTrue(save.isVisible());
			assertFalse(save.isDisabled());
			Label hint = field(controller, "combinedReviewHint");
			assertTrue(hint.getText().contains("2 invoice batches"));

			rows.replaceAll(row -> row.withStatus(InvoiceReconciliation.Status.SAVED));
			Set<Long> completed = field(controller, "completedBatchIds");
			completed.add(1L);
			invoke(controller, "updateAcceptanceControls");
			assertEquals("Retry completion", save.getText());
			assertTrue(save.isVisible());
			assertEquals(List.of(new EnteredDocument(101, DATE), new EnteredDocument(201, DATE)),
					invoke(controller, "buildEnteredDocuments"));
		});
	}

	@Test
	void individualStatementsKeepTheirOwnPrintedAndExtractedTotals() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			invoke(controller, "showLoadedBatches", List.class, List.of(loaded(2, 201, BatchKind.STATEMENT,
					List.of(scanned("INV-2", 30000), scanned("CR-2", -5000)), 25000L)));

			Label total = field(controller, "extractedStatementTotalLabel");
			Label printed = field(controller, "expectedStatementAmountLabel");
			Label variance = field(controller, "statementVarianceLabel");
			assertEquals("$250.00", total.getText());
			assertEquals("$250.00", printed.getText());
			assertEquals("$0.00", variance.getText());
			invoke(controller, "showLoadedBatches", List.class, List.of(
					loaded(3, 301, BatchKind.STATEMENT, List.of(scanned("INV-3", 75000)), 74000L)));
			assertEquals("$750.00", total.getText());
			assertEquals("$740.00", printed.getText());
			assertEquals("$10.00", variance.getText());
		});
	}

	@Test
	void individualBatchRetainsItsExistingSaveFlow() throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			invoke(controller, "showLoadedBatches", List.class,
					List.of(loaded(1, 101, BatchKind.INVOICE_BATCH, List.of(scanned("INV-1", 10000)), null)));
			Label hint = field(controller, "combinedReviewHint");
			assertFalse(hint.isVisible());
			Button save = field(controller, "saveAcceptedButton");
			assertTrue(save.isVisible());
			assertEquals("Save accepted documents", save.getText());
		});
	}

	private static InvoiceReconciliation pending(String reference, long cents) {
		return new InvoiceReconciliation(InvoiceReconciliation.Status.NEEDS_REVIEW, scanned(reference, cents), null, null);
	}

	private static void showAcceptanceRows(AiInvoiceScanController controller, List<InvoiceReconciliation> rows,
			BatchKind kind) throws Exception {
		Map<ScannedInvoice, Long> ids = new IdentityHashMap<>();
		rows.forEach(row -> ids.put(row.getScanned(), 101L));
		Object run = record("ScanRun", rows.stream().map(InvoiceReconciliation::getScanned).toList(), rows,
				Map.of(), ids, DATE.withDayOfMonth(1), DATE.withDayOfMonth(30), null, null,
				List.of(new InvoiceSupplier(1, "Supplier", 1)));
		BatchSummary batch = new BatchSummary(1, kind, BatchStatus.REVIEW_REQUIRED, "Review test", 1, rows.size(), null, null);
		invoke(controller, "showLoadedBatches", List.class, List.of(record("LoadedBatch", batch, run, List.of(101L))));
	}

	private static AiInvoiceScanController controller() throws Exception {
		FXMLLoader loader = new FXMLLoader(AiInvoiceScanControllerTest.class.getResource("/views/FXML/AiInvoiceScan.fxml"));
		Parent root = loader.load();
		assertNotNull(root);
		return loader.getController();
	}

	private static void configureReconciliation(AiInvoiceScanController controller, InvoiceService service) throws Exception {
		Main main = new Main();
		Store store = new Store();
		store.setStoreID(1);
		main.setCurrentStore(store);
		main.setCurrentDate(DATE);
		controller.setMain(main);
		setField(controller, "invoiceService", service);
		setField(controller, "availableSuppliers", List.of(new InvoiceSupplier(9, "Symbion Pty Ltd", 1)));
	}

	private static Object loadTestBatch(AiInvoiceScanController controller, List<ScannedInvoice> scanned) throws Exception {
		BatchSummary summary = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED, "Test", 1, scanned.size(), null, null);
		BatchDetail detail = new BatchDetail(summary, List.of(new ExtractedDocument(101, "statement.pdf", scanned,
				List.of(), null, null, null, null, null)));
		Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
		load.setAccessible(true);
		return load.invoke(controller, detail, List.of(new InvoiceSupplier(9, "Symbion Pty Ltd", 1)));
	}

	private static Invoice symbionImport(String reference) {
		Invoice invoice = new Invoice();
		invoice.setInvoiceNo(reference);
		invoice.setSupplierID(9);
		invoice.setSupplierName("SYMBION");
		invoice.setStoreID(1);
		invoice.setImportedInvoiceAmount(100);
		invoice.setImportExists(true);
		return invoice;
	}

	private static List<TableCell<?, ?>> actionCells(TableView<?> table) {
		return table.lookupAll(".table-cell").stream()
				.filter(node -> node instanceof TableCell<?, ?> cell
						&& cell.getTableColumn() == table.getColumns().getLast()
						&& cell.isVisible() && !cell.isEmpty() && cell.getIndex() >= 0
						&& cell.getIndex() < table.getItems().size())
				.<TableCell<?, ?>>map(node -> (TableCell<?, ?>) node).toList();
	}

	private static void assertActionButtons(TableView<InvoiceReconciliation> table) {
		List<TableCell<?, ?>> cells = actionCells(table);
		assertEquals(table.getItems().size(), cells.size(), "Every visible invoice should have an action cell");
		for (TableCell<?, ?> cell : cells) {
			InvoiceReconciliation row = table.getItems().get(cell.getIndex());
			HBox buttons = assertInstanceOf(HBox.class, cell.getGraphic(),
					"Missing actions for invoice " + row.getInvoiceNo());
			Button review = assertInstanceOf(Button.class, buttons.getChildren().getFirst());
			Button trash = assertInstanceOf(Button.class, buttons.getChildren().getLast());
			boolean saved = row.getStatus() == InvoiceReconciliation.Status.SAVED;
			assertEquals(saved ? "Saved" : "Review", review.getText());
			assertEquals(saved, review.isDisabled());
			assertEquals(saved, trash.isDisabled());
			assertEquals("Trash", trash.getText());
		}
	}

	private static Object loaded(long batchId, long documentId, BatchKind kind, List<ScannedInvoice> scanned, Long total) throws Exception {
		return loaded(batchId, documentId, kind, scanned, total, InvoiceReconciliation.Status.MATCHED);
	}

	private static Object loaded(long batchId, long documentId, BatchKind kind, List<ScannedInvoice> scanned,
			Long total, InvoiceReconciliation.Status status) throws Exception {
		Map<ScannedInvoice, Long> ids = new IdentityHashMap<>();
		scanned.forEach(row -> ids.put(row, documentId));
		List<InvoiceReconciliation> rows = scanned.stream().map(row ->
				new InvoiceReconciliation(status, row, null, null)).toList();
		Object run = record("ScanRun", scanned, rows, Map.of(), ids,
				DATE.withDayOfMonth(1), DATE.withDayOfMonth(30), total, 0.99, List.of());
		BatchSummary batch = new BatchSummary(batchId, kind, BatchStatus.REVIEW_REQUIRED,
				"Batch " + batchId, 1, rows.size(), null, null);
		return record("LoadedBatch", batch, run, List.of(documentId));
	}

	private static ScannedInvoice scanned(String reference, long cents) {
		return new ScannedInvoice("invoice.pdf", "Supplier", reference, DATE, null,
				cents < 0 ? ScannedInvoice.DocumentType.CREDIT : ScannedInvoice.DocumentType.INVOICE, cents, 0.99);
	}

	private static Object record(String name, Object... args) throws Exception {
		Class<?> type = Class.forName(AiInvoiceScanController.class.getName() + "$" + name);
		Constructor<?> constructor = type.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		return constructor.newInstance(args);
	}

	private static byte[] pdf(float width) throws IOException {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			document.addPage(new PDPage(new PDRectangle(width, 72)));
			document.save(output);
			return output.toByteArray();
		}
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T field(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(target);
	}

	private static Object invoke(Object target, String name) throws Exception {
		Method method = target.getClass().getDeclaredMethod(name);
		method.setAccessible(true);
		return method.invoke(target);
	}

	private static Object invoke(Object target, String name, Class<?> parameterType, Object argument) throws Exception {
		Method method = target.getClass().getDeclaredMethod(name, parameterType);
		method.setAccessible(true);
		return method.invoke(target, argument);
	}

	private static void onFxThread(ThrowingRunnable action) throws Exception {
		FutureTask<Void> task = new FutureTask<>(() -> { action.run(); return null; });
		Platform.runLater(task);
		task.get(15, TimeUnit.SECONDS);
	}

	@FunctionalInterface
	private interface ThrowingRunnable { void run() throws Exception; }
}
