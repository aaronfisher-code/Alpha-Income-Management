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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
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
	@ValueSource(booleans = { false, true })
	void savingSeventySevenDocumentsFinishesAndFilingTimeoutCanRetryWithoutResaving(boolean filingTimeout) throws Exception {
		var saving = new CountDownLatch(1);
		var releaseSave = new CountDownLatch(1);
		var filing = new CountDownLatch(1);
		var releaseFiling = new CountDownLatch(1);
		var stopped = new CountDownLatch(1);
		var queueRefreshed = new CountDownLatch(1);
		var saves = new AtomicInteger();
		var completions = new AtomicInteger();
		var tableRefreshes = new AtomicInteger();
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public void saveOrUpdateInvoice(Invoice invoice, String original, int supplier) {
							assertFalse(Platform.isFxApplicationThread());
							if (saves.incrementAndGet() == 38) {
								saving.countDown(); awaitRelease(releaseSave);
							}
						}
					});
					setField(controller, "parent", new InvoiceEntryController() {
						{ dialogPane = new com.dlsc.gemsfx.DialogPane(); }
						@Override public void fillInvoiceTable() { tableRefreshes.incrementAndGet(); }
						@Override public void fillCreditTable() { tableRefreshes.incrementAndGet(); }
					});
					setField(controller, "executor", workers);
					setField(controller, "documentAiService", documentService(url -> {
						if (url.contains("/complete?")) {
							assertEquals(77, saves.get());
							int attempt = completions.incrementAndGet();
							filing.countDown(); awaitRelease(releaseFiling);
							if (filingTimeout && attempt == 1) throw new org.springframework.web.client.ResourceAccessException(
									"Read timed out", new java.net.SocketTimeoutException("Read timed out"));
							return null;
						}
						if (url.endsWith("/review-batches/1?storeId=1")) {
							return new BatchDetail(new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED,
									"Review test", 1, 77, null, null), List.of());
						}
						assertTrue(url.endsWith("/review-queue"));
						queueRefreshed.countDown();
						return "[]";
					}));
					List<InvoiceReconciliation> rows = new ArrayList<>();
					for (int i = 0; i < 77; i++) rows.add(pending("INV-" + i, 10000).withStatus(InvoiceReconciliation.Status.ACCEPTED));
					showAcceptanceRows(controller, rows, BatchKind.INVOICE_BATCH);
					TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
					table.getColumns().get(2).setSortType(TableColumn.SortType.DESCENDING);
					table.getSortOrder().setAll(table.getColumns().get(2));
					table.getSelectionModel().selectAll();
					ProgressIndicator spinner = field(controller, "saveAcceptedProgress");
					spinner.visibleProperty().addListener((_, _, visible) -> { if (!visible) stopped.countDown(); });
					invoke(controller, "saveAccepted");
				});
				assertTrue(saving.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					ProgressIndicator spinner = field(holder[0], "saveAcceptedProgress");
					assertTrue(spinner.isVisible());
					assertTrue(spinner.getProgress() > 0 && spinner.getProgress() < 1);
					Label summary = field(holder[0], "summaryLabel");
					assertTrue(summary.getText().contains("Saving documents"));
				});
				releaseSave.countDown();
				assertTrue(filing.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					ProgressIndicator spinner = field(holder[0], "saveAcceptedProgress");
					assertTrue(spinner.isVisible());
					Label summary = field(holder[0], "summaryLabel");
					assertTrue(summary.getText().contains("Filing source PDFs"));
				});
				releaseFiling.countDown();
				assertTrue(stopped.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0];
					assertEquals(Boolean.FALSE, field(controller, "commitBusy"));
					ProgressIndicator spinner = field(controller, "saveAcceptedProgress");
					assertFalse(spinner.isVisible()); assertFalse(spinner.isManaged());
					assertFalse(spinner.progressProperty().isBound());
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					assertEquals(77, rows.size());
					assertTrue(rows.stream().allMatch(row -> row.getStatus() == InvoiceReconciliation.Status.SAVED));
					if (filingTimeout) {
						Button save = field(controller, "saveAcceptedButton");
						assertEquals("Retry completion", save.getText()); assertFalse(save.isDisabled());
						invoke(controller, "saveAccepted");
					}
				});
				assertTrue(queueRefreshed.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					javafx.scene.layout.VBox queue = field(holder[0], "queueView");
					assertTrue(queue.isVisible());
					assertEquals(Boolean.FALSE, field(holder[0], "commitBusy"));
				});
				assertEquals(77, saves.get());
				assertEquals(filingTimeout ? 2 : 1, completions.get());
				assertEquals(2, tableRefreshes.get());
			} finally {
				releaseSave.countDown(); releaseFiling.countDown();
				if (holder[0] != null) onFxThread(holder[0]::dispose);
			}
		}
	}

	@Test
	void reviewAllRefreshesItsSnapshotAndCompletesEverySourceBatchDespiteOneFilingFailure() throws Exception {
		var opened = new CountDownLatch(1);
		var stopped = new CountDownLatch(1);
		var queueLoaded = new CountDownLatch(1);
		var savedReferences = new CopyOnWriteArrayList<String>();
		var attempts = new CopyOnWriteArrayList<Long>();
		var completed = java.util.concurrent.ConcurrentHashMap.<Long>newKeySet();
		var details = new ArrayList<BatchDetail>();
		for (long batch = 1; batch <= 6; batch++) {
			var documents = new ArrayList<ExtractedDocument>();
			for (int document = 0; document < (batch == 6 ? 12 : 13); document++) {
				documents.add(new ExtractedDocument(batch * 100 + document, "invoice.pdf",
						List.of(scanned("INV-" + batch + "-" + document, 10000)), List.of(), null, null, null, null, null));
			}
			details.add(new BatchDetail(new BatchSummary(batch, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED,
					"Batch " + batch, documents.size(), documents.size(), null, null), documents));
		}
		var failed = new BatchSummary(7, BatchKind.INVOICE_BATCH, BatchStatus.FAILED, "Failed scan", 1, 0, null, "Scan failed");
		var newlyReady = new BatchSummary(8, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED, "Later scan", 1, 1, null, null);
		var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
				.setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.GETTER, com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
				.setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.FIELD, com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);
		var initialQueue = new ArrayList<>(details.stream().map(BatchDetail::batch).toList()); initialQueue.add(failed);
		String initialJson = mapper.writeValueAsString(initialQueue);
		String remainingJson = mapper.writeValueAsString(List.of(failed, newlyReady));
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) { return List.of(); }
						@Override public void saveOrUpdateInvoice(Invoice invoice, String original, int supplier) {
							savedReferences.add(invoice.getInvoiceNo());
						}
					});
					setField(controller, "supplierLoad", java.util.concurrent.CompletableFuture.completedFuture(
							List.of(new InvoiceSupplier(1, "Supplier", 1))));
					setField(controller, "parent", new InvoiceEntryController() {
						{ dialogPane = new com.dlsc.gemsfx.DialogPane(); }
						@Override public void fillInvoiceTable() {}
						@Override public void fillCreditTable() {}
					});
					setField(controller, "executor", workers);
					setField(controller, "documentAiService", documentService((url, entity) -> {
						if (url.endsWith("/review-queue")) {
							if (completed.size() == 6) { queueLoaded.countDown(); return remainingJson; }
							return initialJson;
						}
						BatchDetail detail = details.stream().filter(item -> url.contains("/review-batches/" + item.batch().id() + "?")
								|| url.contains("/review-batches/" + item.batch().id() + "/complete?")).findFirst().orElseThrow();
						long id = detail.batch().id();
						if (url.contains("/complete?")) {
							assertEquals(77, savedReferences.size());
							List<?> entered = assertInstanceOf(List.class, entity.getBody());
							assertEquals(detail.documents().stream().map(ExtractedDocument::id).toList(),
                                    entered.stream().map(value -> ((EnteredDocument) value).documentId()).toList());
                            assertTrue(entered.stream().allMatch(value -> !((EnteredDocument) value).rows().isEmpty()));
							attempts.add(id);
							if (id == 2 && java.util.Collections.frequency(attempts, id) == 1) {
								throw new org.springframework.web.client.ResourceAccessException("Drive completion timed out");
							}
							completed.add(id); return null;
						}
						return detail;
					}));
					// The displayed queue is stale: five further batches have become ready.
					ObservableList<BatchSummary> queue = field(controller, "queuedBatches"); queue.setAll(details.getFirst().batch());
					javafx.scene.layout.VBox results = field(controller, "resultsView");
					results.visibleProperty().addListener((_, _, visible) -> { if (visible) opened.countDown(); });
					invoke(controller, "reviewAll");
				});
				assertTrue(opened.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					ObservableList<InvoiceReconciliation> rows = field(holder[0], "allResults"); assertEquals(77, rows.size());
					Label hint = field(holder[0], "combinedReviewHint");
					assertTrue(hint.getText().contains("6 invoice batches"));
					assertTrue(hint.getText().contains("1 failed or unfinished"));
					TableView<InvoiceReconciliation> table = field(holder[0], "resultsTable"); table.getSelectionModel().selectAll();
					invoke(holder[0], "acceptSelected");
					ProgressIndicator spinner = field(holder[0], "saveAcceptedProgress");
					spinner.visibleProperty().addListener((_, _, visible) -> { if (!visible) stopped.countDown(); });
					invoke(holder[0], "saveAccepted");
				});
				assertTrue(stopped.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L), attempts);
					assertEquals(Set.of(1L, 3L, 4L, 5L, 6L), field(holder[0], "completedBatchIds"));
					Button save = field(holder[0], "saveAcceptedButton"); assertEquals("Retry completion", save.getText());
					assertFalse(save.isDisabled()); invoke(holder[0], "saveAccepted");
				});
				assertTrue(queueLoaded.await(5, TimeUnit.SECONDS));
				// Drain the worker's queue response and its FX completion callback.
				workers.submit(() -> {}).get(5, TimeUnit.SECONDS);
				onFxThread(() -> {
					ObservableList<BatchSummary> queue = field(holder[0], "queuedBatches"); assertEquals(List.of(failed, newlyReady), queue);
					Label hint = field(holder[0], "combinedReviewHint");
					assertTrue(hint.getText().contains("All batches from the saved review have cleared"));
					assertTrue(hint.getText().contains("1 other invoice batch(es) are ready"));
				});
				assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 2L), attempts);
				assertEquals(77, savedReferences.size()); assertEquals(77, Set.copyOf(savedReferences).size());
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	private static void awaitRelease(CountDownLatch release) {
		try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Save was not released"); }
		catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new RuntimeException(exception); }
	}

	@ParameterizedTest
	@ValueSource(strings = { "Clifford Hallam Healthcare Eastern Creek NSW", "ParagonCare" })
	void ch2InvoiceIssuerSelectsAndSavesTheExistingCh2Contact(String extractedSupplier) throws Exception {
		onFxThread(() -> {
			var controller = controller();
			try {
				var ch2 = new InvoiceSupplier(4, "CH2", 1);
				var saved = new ArrayList<Invoice>();
				configureReconciliation(controller, new InvoiceService() {
					@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> requested) {
						assertEquals("CH2", requested.getFirst().supplierName());
						var invoice = new Invoice();
						invoice.setInvoiceNo("73646116"); invoice.setSupplierName("CH2"); invoice.setSupplierID(4);
						invoice.setImportedInvoiceAmount(570.19); invoice.setImportExists(true);
						return List.of(invoice);
					}
					@Override public void saveOrUpdateInvoice(Invoice invoice, String original, int supplier) {
						saved.add(invoice);
					}
				});
				var scanned = new ScannedInvoice("statement.pdf", extractedSupplier, "73646116", DATE, null,
						ScannedInvoice.DocumentType.INVOICE, 57019, 0.99);
				var summary = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED, "CH2 invoice", 1, 1, null, null);
				var detail = new BatchDetail(summary, List.of(new ExtractedDocument(101, "statement.pdf", List.of(scanned),
						List.of(), null, null, null, null, null)));
				Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
				load.setAccessible(true);
				Object loaded = load.invoke(controller, detail, List.of(ch2, new InvoiceSupplier(2, "Sigma Healthcare", 1)));
				invoke(controller, "showLoadedBatches", List.class, List.of(loaded));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertEquals("CH2", rows.getFirst().getSupplierName());
				assertEquals(InvoiceReconciliation.Status.MATCHED, rows.getFirst().getStatus());
				invoke(controller, "review", InvoiceReconciliation.class, rows.getFirst());
				ComboBox<InvoiceSupplier> supplier = field(controller, "reviewSupplierChoice");
				assertSame(ch2, supplier.getValue());
				List<?> pending = (List<?>) invoke(controller, "buildPendingSaves");
				Object document = invoke(pending.getFirst(), "document");
				assertEquals(4, invoke(document, "supplierId"));
				assertEquals("CH2", invoke(document, "supplierName"));
				Method save = AiInvoiceScanController.class.getDeclaredMethod("saveDocument", InvoiceReconciliation.class, document.getClass());
				save.setAccessible(true); save.invoke(controller, rows.getFirst(), document);
				assertEquals(4, saved.getFirst().getSupplierID());
			} finally { controller.dispose(); }
		});
	}

	@Test
	void doubleClickingAnEmptyStatementQueueRowOpensReviewWithoutRescanning() throws Exception {
		CountDownLatch opened = new CountDownLatch(1);
		List<String> calls = new CopyOnWriteArrayList<>();
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService());
					setField(controller, "executor", workers);
					BatchSummary batch = new BatchSummary(17, BatchKind.STATEMENT, BatchStatus.REVIEW_REQUIRED, "Empty", 1, 0, null, null);
					setField(controller, "documentAiService", documentService(url -> {
						calls.add(url);
						if (url.contains("/statement-comparison")) return new StatementComparison(List.of(), 0);
						assertTrue(url.endsWith("/review-batches/17?storeId=1"));
						return new BatchDetail(batch, List.of());
					}));
					javafx.scene.layout.VBox results = field(controller, "resultsView");
					results.visibleProperty().addListener((_, _, visible) -> { if (visible) opened.countDown(); });
					TableView<BatchSummary> queue = field(controller, "queueTable");
					queue.getItems().setAll(batch);
					javafx.scene.control.TableRow<BatchSummary> row = queue.getRowFactory().call(queue);
					row.updateTableView(queue); row.updateIndex(0);
					assertFalse(row.isEmpty());
					row.getOnMouseClicked().handle(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
							0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 2,
							false, false, false, false, false, false, false, false, false, false, null));
				});
				assertTrue(opened.await(5, TimeUnit.SECONDS));
				assertEquals(2, calls.size());
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void largeReviewsShowActualProgressAcrossAllBatchesAndHideItWhenFinished(boolean combined) throws Exception {
		var firstStarted = new CountDownLatch(1);
		var releaseFirst = new CountDownLatch(1);
		var laterStarted = new CountDownLatch(1);
		var releaseLater = new CountDownLatch(1);
		var opened = new CountDownLatch(1);
		var requests = new AtomicInteger();
		var details = new ArrayList<BatchDetail>();
		int batchCount = combined ? 2 : 1;
		for (int batch = 0; batch < batchCount; batch++) {
			var rows = new ArrayList<ScannedInvoice>();
			for (int row = 0; row < 500 / batchCount; row++) rows.add(scanned("INV-" + batch + "-" + row, 10000));
			var summary = new BatchSummary(batch + 1, BatchKind.INVOICE_BATCH,
					BatchStatus.REVIEW_REQUIRED, "Batch " + batch, 1, rows.size(), null, null);
			details.add(new BatchDetail(summary, List.of(new ExtractedDocument(batch + 101, "statement.pdf", rows,
					List.of(), null, null, null, null, null))));
		}
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) {
							int request = requests.incrementAndGet();
							CountDownLatch release = null;
							if (request == 1) { firstStarted.countDown(); release = releaseFirst; }
							if (request >= (combined ? 6 : 2)) { laterStarted.countDown(); release = releaseLater; }
							try {
								if (release != null && !release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Load was not released");
							} catch (InterruptedException exception) { throw new RuntimeException(exception); }
							return List.of();
						}
					});
					setField(controller, "executor", workers);
					setField(controller, "documentAiService", documentService(url -> details.stream()
							.filter(detail -> url.endsWith("/review-batches/" + detail.batch().id() + "?storeId=1"))
							.findFirst().orElseThrow()));
					javafx.scene.layout.VBox results = field(controller, "resultsView");
					results.visibleProperty().addListener((_, _, visible) -> { if (visible) opened.countDown(); });
					invoke(controller, "openBatches", List.class, details.stream().map(BatchDetail::batch).toList());
					ProgressBar progress = field(controller, "reviewLoadProgress");
					assertTrue(progress.isVisible());
					assertTrue(progress.isManaged());
					assertEquals(-1, progress.getProgress(), "Preparation is indeterminate until record counts are known");
				});
				assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					ProgressBar progress = field(holder[0], "reviewLoadProgress");
					assertEquals(0, progress.getProgress());
				});
				releaseFirst.countDown();
				assertTrue(laterStarted.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					ProgressBar progress = field(holder[0], "reviewLoadProgress");
					assertTrue(progress.isVisible());
					assertEquals(combined ? 0.5 : 0.1, progress.getProgress(), 0.001);
					Label summary = field(holder[0], "summaryLabel");
					assertEquals("Loading document records… " + (combined ? 250 : 50) + " of 500", summary.getText());
				});
				releaseLater.countDown();
				assertTrue(opened.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					ProgressBar progress = field(holder[0], "reviewLoadProgress");
					assertFalse(progress.isVisible());
					assertFalse(progress.isManaged());
					assertFalse(progress.progressProperty().isBound());
					ObservableList<InvoiceReconciliation> rows = field(holder[0], "allResults");
					assertEquals(500, rows.size());
				});
				assertEquals(10, requests.get());
			} finally {
				releaseFirst.countDown(); releaseLater.countDown();
				if (holder[0] != null) onFxThread(holder[0]::dispose);
			}
		}
	}

	@Test
	void rowRescanTargetsItsSourcePdfAndRestoresApprovalsForOtherUnchangedPdfs() throws Exception {
		CountDownLatch queued = new CountDownLatch(1);
		List<String> calls = new CopyOnWriteArrayList<>();
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService());
					setField(controller, "executor", workers);
					setField(controller, "documentAiService", documentService(url -> {
						calls.add(url);
						if (url.endsWith("/review-documents/101/rescan?storeId=1")) return null;
						assertTrue(url.endsWith("/stores/1/review-queue")); return "[]";
					}));
					var first = scanned("RESCAN", 10000);
					var credit = scanned("RESCAN-CREDIT", -1000);
					var other = scanned("KEEP", 20000);
					invoke(controller, "showLoadedBatches", List.class, List.of(
							loaded(1, 101, BatchKind.INVOICE_BATCH, List.of(first, credit), null, InvoiceReconciliation.Status.ACCEPTED),
							loaded(2, 102, BatchKind.INVOICE_BATCH, List.of(other), null, InvoiceReconciliation.Status.ACCEPTED)));
					Map<ScannedInvoice, Object> approvals = field(controller, "acceptedDocuments");
					approvals.put(other, record("AcceptedDocument", 1, "Supplier", ScannedInvoice.DocumentType.INVOICE,
							"KEEP", DATE, DATE.plusDays(30), 42.0, "Keep this approval"));
					javafx.scene.layout.VBox queue = field(controller, "queueView");
					queue.visibleProperty().addListener((_, _, visible) -> { if (visible) queued.countDown(); });
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					invoke(controller, "rescanScannedRow", InvoiceReconciliation.class, rows.getFirst());
				});
				assertTrue(queued.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0];
					var refreshed = scanned("RESCAN", 11000);
					var refreshedCredit = scanned("RESCAN-CREDIT", -1500);
					var sameOther = scanned("KEEP", 20000);
					invoke(controller, "showLoadedBatches", List.class, List.of(
							loaded(1, 101, BatchKind.INVOICE_BATCH, List.of(refreshed, refreshedCredit), null, InvoiceReconciliation.Status.NEEDS_REVIEW),
							loaded(2, 102, BatchKind.INVOICE_BATCH, List.of(sameOther), null, InvoiceReconciliation.Status.NEEDS_REVIEW)));
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW, rows.getFirst().getStatus());
					assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW, rows.get(1).getStatus());
					assertEquals(InvoiceReconciliation.Status.ACCEPTED, rows.get(2).getStatus());
					Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments");
					assertEquals(1, approvals.size());
					assertEquals("Keep this approval", invoke(approvals.get(sameOther), "notes"));
				});
				assertEquals(1, calls.stream().filter(url -> url.endsWith("/review-documents/101/rescan?storeId=1")).count());
				assertTrue(calls.stream().noneMatch(url -> url.contains("/review-batches/1/rescan")));
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@ParameterizedTest
	@CsvSource({ "INVOICE,true", "INVOICE,false", "CREDIT,true", "CREDIT,false" })
	void previewRefreshUpdatesTheExpectedAmountOrRetainsTheCacheOnFailureWithoutLosingDraftEdits(ScannedInvoice.DocumentType type, boolean succeeds) throws Exception {
        boolean credit = type == ScannedInvoice.DocumentType.CREDIT;
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(1);
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) {
                            var expected = symbionImport("000123");
                            if (credit) { expected.setReconciliationType(type); expected.setImportedInvoiceAmount(-100); }
                            return List.of(expected);
                        }
						@Override public List<Invoice> refreshInvoiceAmount(int store, ScannedInvoice row) {
							assertEquals(1, store); assertEquals("000123", row.invoiceNo()); assertEquals(type, row.documentType());
							started.countDown();
							try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Refresh was not released"); }
							catch (InterruptedException exception) { throw new RuntimeException(exception); }
							if (!succeeds) throw new IllegalStateException("agent offline");
							var updated = symbionImport("000123"); updated.setImportedInvoiceAmount(credit ? -150.25 : 150.25);
                            if (credit) updated.setReconciliationType(type);
                            return List.of(updated);
						}
					});
					setField(controller, "executor", workers);
					var scanned = new ScannedInvoice("invoice.pdf", "Symbion", credit ? "000123" : "I000123", DATE, null, type, 10000, 0.99);
					invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, List.of(scanned))));
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					invoke(controller, "review", InvoiceReconciliation.class, rows.getFirst());
                    javafx.scene.control.ChoiceBox<String> typeChoice = field(controller, "reviewTypeChoice");
                    typeChoice.setValue(credit ? "Invoice" : "Credit");
                    Label expected = field(controller, "reviewExpectedAmountLabel"); assertEquals("N/A", expected.getText());
                    typeChoice.setValue(credit ? "Credit" : "Invoice"); assertEquals("$100.00", expected.getText());
					TextField amount = field(controller, "reviewAmountField"); amount.setText("105.00");
                    Label variance = field(controller, "reviewVarianceLabel");
                    assertEquals(credit ? "-$5.00" : "$5.00", variance.getText());
					TextArea notes = field(controller, "reviewNotesField"); notes.setText("Keep my draft");
					Label message = field(controller, "invoiceAmountRefreshMessageLabel");
					message.textProperty().addListener((_, _, value) -> { if (value.contains("refreshed") || value.contains("failed")) finished.countDown(); });
					Button refresh = field(controller, "refreshInvoiceAmountButton"); refresh.fire();
					assertTrue(refresh.isDisabled());
					assertEquals("Refreshing Z-Office…", refresh.getText());
					javafx.scene.control.ProgressIndicator progress = field(controller, "amountRefreshProgress");
					assertTrue(progress.isVisible());
					assertTrue(progress.isManaged());
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
					assertEquals("Refresh Z-Office amount", refresh.getText());
					javafx.scene.control.ProgressIndicator progress = field(controller, "amountRefreshProgress");
					assertFalse(progress.isVisible());
					assertFalse(progress.isManaged());
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
						@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) { return List.of(symbionImport("000123"), other); }
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
						@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) { return List.of(); }
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

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void bulkSupplierEditCorrectsFiveHundredRowsUsingOneReconciliationRequestAndIsAtomicOnFailure(boolean succeeds) throws Exception {
		var completed = new CountDownLatch(1);
		var requests = new CopyOnWriteArrayList<List<ScannedInvoice>>();
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) {
							assertEquals(1, store); requests.add(rows);
							if (!succeeds) throw new IllegalStateException("cache unavailable");
							List<Invoice> imports = new ArrayList<>();
							for (var row : rows) {
								assertEquals("Direct Chemist Outlet Hillside", row.supplierName());
								if (row.documentType() == ScannedInvoice.DocumentType.CREDIT) continue;
								for (String name : List.of("Direct Chemist Outlet Gladstone", row.supplierName())) {
									if (row.invoiceNo().equals("ROW-498") && name.endsWith("Hillside")) continue;
									var imported = new Invoice(); imported.setInvoiceNo(row.invoiceNo());
									imported.setSupplierName(name); imported.setImportExists(true);
									imported.setImportedInvoiceAmount(name.endsWith("Gladstone") ? 999 : row.amount());
									imports.add(imported);
								}
							}
							return imports;
						}
						@Override public Invoice getInvoice(String reference, int store) { throw new AssertionError("No individual desktop lookup in bulk edit"); }
					});
					setField(controller, "executor", workers);
					List<InvoiceReconciliation> rows = new ArrayList<>();
					for (int i = 0; i < 500; i++) {
						var scanned = new ScannedInvoice("statement.pdf", "Direct Chemist Outlet Gladstone", "ROW-" + i,
								DATE, null, i == 499 ? ScannedInvoice.DocumentType.CREDIT : ScannedInvoice.DocumentType.INVOICE,
								10000 + i, 0.99);
						rows.add(new InvoiceReconciliation(InvoiceReconciliation.Status.MATCHED, scanned, null, null));
					}
					showAcceptanceRows(controller, rows, BatchKind.STATEMENT);
					var supplier = new InvoiceSupplier(21, "Direct Chemist Outlet Hillside", 1);
					setField(controller, "availableSuppliers", List.of(supplier));
					ComboBox<InvoiceSupplier> choice = field(controller, "bulkSupplierChoice"); choice.getItems().setAll(supplier); choice.setValue(supplier);
					CheckBox show = field(controller, "showMatchedCheck"); show.setSelected(true);
					invoke(controller, "selectAllResults");
					TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
					assertEquals(500, table.getSelectionModel().getSelectedItems().size());
					invoke(controller, "openBulkEdit");
					Label message = field(controller, "bulkReviewMessageLabel");
					message.textProperty().addListener((_, _, value) -> { if (value.startsWith("Updated") || value.startsWith("No edits applied")) completed.countDown(); });
					invoke(controller, "applyBulkEdit");
					Button apply = field(controller, "applyBulkEditButton"); assertTrue(apply.isDisabled());
				});
				assertTrue(completed.await(10, TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0];
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					Map<ScannedInvoice, ?> drafts = field(controller, "pendingEdits");
					Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments");
					assertEquals(succeeds ? 2 : 0, drafts.size());
					assertEquals(succeeds ? 498 : 0, approvals.size());
					TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
					assertEquals(500, table.getSelectionModel().getSelectedItems().size());
					for (int i = 0; i < 500; i++) {
						var row = rows.get(i);
						assertEquals("Direct Chemist Outlet Gladstone", row.getScanned().supplierName(), "Original evidence identity retained");
						assertEquals(succeeds ? "Direct Chemist Outlet Hillside" : "Direct Chemist Outlet Gladstone", row.getSupplierName());
						assertEquals("ROW-" + i, row.getInvoiceNo());
						assertEquals(row.getScanned().amountCents(), row.getReviewValues().amountCents());
						assertEquals(succeeds && i >= 498 ? InvoiceReconciliation.Status.NOT_IN_Z_OFFICE : InvoiceReconciliation.Status.MATCHED, row.getStatus());
						if (succeeds && i == 498) assertFalse(row.hasExpectedAmount(), "A single cached invoice from another branch must not match");
						if (succeeds && i < 498) { assertEquals(row.getScanned().amount(), row.getImported().getImportedInvoiceAmount()); assertEquals("$0.00", row.getVarianceString()); }
					}
					if (succeeds) {
						invoke(controller, "acceptSelected"); assertEquals(500, approvals.size()); assertTrue(drafts.isEmpty());
						List<?> saves = (List<?>) invoke(controller, "buildPendingSaves"); assertEquals(500, saves.size());
						for (var approval : approvals.values()) { assertEquals(21, invoke(approval, "supplierId")); assertEquals(DATE, invoke(approval, "documentDate")); }
						assertEquals(ScannedInvoice.DocumentType.CREDIT, rows.getLast().getReviewValues().documentType());
						assertEquals(-10499L, rows.getLast().getReviewValues().amountCents());
					}
				});
				assertEquals(1, requests.size()); assertEquals(500, requests.getFirst().size());
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void bulkSupplierEditAutomaticallyAcceptsMatchesAndLeavesOtherIssuesForReview(boolean classificationWarning) throws Exception {
		var completed = new CountDownLatch(1);
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		var references = List.of("MATCHED", "TOLERANCE", "CREDIT", "MISMATCH", "MISSING", "AMBIGUOUS", "STALE",
				"REFERENCE", "DATE", "TYPE", "AMOUNT", "NO-DATE", "", "UNSELECTED", "SAVED");
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) {
							assertEquals(13, rows.size(), "Only changed, selected, unsaved rows are looked up");
							var imported = new ArrayList<Invoice>();
							for (var row : rows) {
								if (row.invoiceNo().isBlank() || row.invoiceNo().equals("MISSING")) continue;
								var invoice = new Invoice();
								invoice.setInvoiceNo(row.invoiceNo()); invoice.setSupplierName(row.supplierName());
								invoice.setImportExists(true); invoice.setReconciliationType(row.documentType());
								invoice.setImportedInvoiceAmount(row.amount() + switch (row.invoiceNo()) {
									case "TOLERANCE" -> 0.20;
									case "MISMATCH" -> 0.21;
									default -> 0;
								});
								invoice.setReconciliationStale(row.invoiceNo().equals("STALE"));
								imported.add(invoice);
								if (row.invoiceNo().equals("AMBIGUOUS")) imported.add(invoice);
							}
							return imported;
						}
					});
					setField(controller, "executor", workers);
					var rows = new ArrayList<InvoiceReconciliation>();
					for (String reference : references) {
						var scanned = new ScannedInvoice("statement.pdf", "Incorrect supplier", reference,
								reference.equals("NO-DATE") ? null : DATE, null,
								reference.equals("CREDIT") ? ScannedInvoice.DocumentType.CREDIT : ScannedInvoice.DocumentType.INVOICE,
								10000, new ScannedInvoice.FieldConfidences(0.68, reference.equals("REFERENCE") ? 0.68 : 0.99,
									reference.equals("DATE") ? 0.68 : 0.99, null, reference.equals("TYPE") ? 0.68 : 0.99,
									reference.equals("AMOUNT") ? 0.68 : 0.99));
						rows.add(new InvoiceReconciliation(reference.equals("SAVED") ? InvoiceReconciliation.Status.SAVED
								: InvoiceReconciliation.Status.NEEDS_REVIEW, scanned, null, null));
					}
					showAcceptanceRows(controller, rows, BatchKind.STATEMENT);
					if (classificationWarning) {
						List<Object> batches = field(controller, "currentBatches");
						Object run = invoke(batches.getFirst(), "run");
						batches.set(0, record("LoadedBatch", new BatchSummary(1, BatchKind.STATEMENT, BatchStatus.REVIEW_REQUIRED,
								"Review test", 1, rows.size(), null, "Wrong folder"), run, List.of(101L)));
					}
					Map<ScannedInvoice, Object> drafts = field(controller, "pendingEdits");
					drafts.put(rows.getFirst().getScanned(), record("AcceptedDocument", 1, "Incorrect supplier",
							ScannedInvoice.DocumentType.INVOICE, "MATCHED", DATE.plusDays(2), DATE.plusDays(45), 100.0, "Keep my note"));
					Map<ScannedInvoice, Object> approvals = field(controller, "acceptedDocuments");
					approvals.put(rows.get(3).getScanned(), record("AcceptedDocument", 1, "Incorrect supplier",
							ScannedInvoice.DocumentType.INVOICE, "MISMATCH", DATE, DATE.plusDays(30), 100.0, ""));
					var supplier = new InvoiceSupplier(21, "Correct supplier", 1);
					setField(controller, "availableSuppliers", List.of(supplier));
					ComboBox<InvoiceSupplier> choice = field(controller, "bulkSupplierChoice"); choice.getItems().setAll(supplier); choice.setValue(supplier);
					TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
					table.getSelectionModel().selectRange(0, 13);
					Label message = field(controller, "bulkReviewMessageLabel");
					message.textProperty().addListener((_, _, value) -> { if (value.startsWith("Updated")) completed.countDown(); });
					invoke(controller, "applyBulkEdit");
				});
				assertTrue(completed.await(10, TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0];
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments");
					Map<ScannedInvoice, ?> drafts = field(controller, "pendingEdits");
					assertEquals(classificationWarning ? 0 : 3, approvals.size());
					assertEquals(classificationWarning ? 13 : 10, drafts.size());
					var expected = List.of(InvoiceReconciliation.Status.MATCHED, InvoiceReconciliation.Status.WITHIN_TOLERANCE,
							InvoiceReconciliation.Status.MATCHED, InvoiceReconciliation.Status.AMOUNT_MISMATCH,
							InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, InvoiceReconciliation.Status.AMBIGUOUS);
					for (int index = 0; index < 13; index++) {
						var row = rows.get(index);
						var status = index < expected.size() ? expected.get(index) : InvoiceReconciliation.Status.NEEDS_REVIEW;
						if (classificationWarning && index < 3) status = InvoiceReconciliation.Status.NEEDS_REVIEW;
						assertEquals(status, row.getStatus(), references.get(index));
						assertEquals("Correct supplier", row.getSupplierName());
						assertEquals(1.0, row.getSupplierConfidence());
						assertEquals("Incorrect supplier", row.getScanned().supplierName());
						Object document = row.isAccepted() ? approvals.get(row.getScanned()) : drafts.get(row.getScanned());
						assertEquals(21, invoke(document, "supplierId"));
					}
					assertEquals("Incorrect supplier", rows.get(13).getSupplierName());
					assertEquals(InvoiceReconciliation.Status.SAVED, rows.getLast().getStatus());
					Object document = classificationWarning ? drafts.get(rows.getFirst().getScanned()) : approvals.get(rows.getFirst().getScanned());
					assertEquals("Keep my note", invoke(document, "notes"));
					assertEquals(DATE.plusDays(2), invoke(document, "documentDate"));
					assertEquals(DATE.plusDays(45), invoke(document, "dueDate"));
					TableView<InvoiceReconciliation> table = field(controller, "resultsTable");
					assertEquals(classificationWarning ? 14 : 11, table.getItems().size(), "Only outstanding rows remain visible");
					assertEquals(classificationWarning ? 13 : 10, table.getSelectionModel().getSelectedItems().size());
					Label summary = field(controller, "summaryLabel");
					assertTrue(summary.getText().contains(classificationWarning ? "1 accepted" : "4 accepted"));
					Label message = field(controller, "bulkReviewMessageLabel");
					assertTrue(message.getText().contains(classificationWarning ? "0 automatically accepted; 13 still need review"
							: "3 automatically accepted; 10 still need review"));
					Button save = field(controller, "saveAcceptedButton"); assertTrue(save.isDisabled());
				});
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void bulkCustomSupplierIsExplicitReusesExistingNamesAndCreatesOnlyOnceAtSave(boolean existing) throws Exception {
		var finished = new CountDownLatch(1);
		var requests = new AtomicInteger();
		var created = new AtomicInteger();
		var savedIds = new ArrayList<Integer>();
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		String name = "Fresh Supplier Pty Ltd";
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) {
							requests.incrementAndGet();
							assertTrue(rows.stream().allMatch(row -> row.supplierName().equals(existing ? "Fresh Supplier" : name)));
							return List.of();
						}
						@Override public void saveOrUpdateInvoice(Invoice invoice, String original, int supplier) {
							savedIds.add(invoice.getSupplierID());
						}
					});
					setField(controller, "creditService", new services.CreditService() {
						@Override public void saveOrUpdateCredit(models.Credit credit) { savedIds.add(credit.getSupplierID()); }
					});
					setField(controller, "invoiceSupplierService", new services.InvoiceSupplierService() {
						@Override public List<InvoiceSupplier> getAllInvoiceSuppliers(int store) {
							return created.get() == 0 ? List.of() : List.of(new InvoiceSupplier(77, name, store));
						}
						@Override public void addInvoiceSupplier(InvoiceSupplier supplier) {
							assertEquals(name, supplier.getSupplierName()); assertEquals(1, supplier.getStoreID());
							created.incrementAndGet();
						}
					});
					setField(controller, "executor", workers);
					// The typed custom name can equal the OCR text and must still count as an explicit choice.
					var invoice = new ScannedInvoice("invoice.pdf", name, "INV", DATE, null, ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
					var credit = new ScannedInvoice("credit.pdf", name, "CR", DATE, null, ScannedInvoice.DocumentType.CREDIT, -1000, 0.99);
					showAcceptanceRows(controller, List.of(new InvoiceReconciliation(InvoiceReconciliation.Status.NEEDS_REVIEW, invoice, null, null),
							new InvoiceReconciliation(InvoiceReconciliation.Status.NEEDS_REVIEW, credit, null, null)), BatchKind.STATEMENT);
					setField(controller, "availableSuppliers", existing ? List.of(new InvoiceSupplier(21, "Fresh Supplier", 1)) : List.of());
					invoke(controller, "selectAllResults"); invoke(controller, "openBulkEdit");
					CheckBox custom = field(controller, "bulkCustomSupplierCheck"); custom.setSelected(true);
					TextField input = field(controller, "bulkCustomSupplierNameField");
					assertTrue(input.isVisible());
					invoke(controller, "applyBulkEdit");
					assertEquals(0, requests.get());
					Label message = field(controller, "bulkReviewMessageLabel");
					assertTrue(message.getText().contains("Enter a supplier name"));
					input.setText("  " + name + "  ");
					message.textProperty().addListener((_, _, text) -> { if (text.startsWith("Updated")) finished.countDown(); });
					invoke(controller, "applyBulkEdit");
				});
				assertTrue(finished.await(5, TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0];
					assertEquals(0, created.get(), "Applying a draft must not create suppliers");
					if (!existing) {
						ObservableList<InvoiceReconciliation> before = field(controller, "allResults");
						List<InvoiceReconciliation> reloaded = before.stream().map(row -> new InvoiceReconciliation(
								InvoiceReconciliation.Status.NEEDS_REVIEW, row.getScanned(), null, null)).toList();
						invoke(controller, "retainUnaffectedApprovals", long.class, -1L);
						showAcceptanceRows(controller, reloaded, BatchKind.STATEMENT);
						invoke(controller, "selectAllResults");
					}
					invoke(controller, "acceptSelected");
					Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments");
					assertEquals(2, approvals.size());
					List<?> pending = (List<?>) invoke(controller, "buildPendingSaves"); assertEquals(2, pending.size());
					assertEquals(0, created.get(), "Acceptance must not create suppliers either");
					ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
					for (var row : rows) {
						Object document = approvals.get(row.getScanned());
						var save = AiInvoiceScanController.class.getDeclaredMethod("saveDocument", InvoiceReconciliation.class, document.getClass());
						save.setAccessible(true); save.invoke(controller, row, document);
					}
					assertEquals(existing ? 0 : 1, created.get());
					assertEquals(List.of(existing ? 21 : 77, existing ? 21 : 77), savedIds);
				});
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@Test
	void editingNotesDoesNotImplicitlyApproveAnUnknownSupplierAsCustom() throws Exception {
		onFxThread(() -> {
			var controller = controller();
			try {
				var scanned = new ScannedInvoice("invoice.pdf", "Unknown supplier", "INV", DATE, null,
						ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
				showAcceptanceRows(controller, List.of(new InvoiceReconciliation(InvoiceReconciliation.Status.NEEDS_REVIEW, scanned, null, null)), BatchKind.STATEMENT);
				invoke(controller, "selectAllResults");
				javafx.scene.control.ChoiceBox<String> choice = field(controller, "bulkEditFieldChoice"); choice.setValue("Notes");
				TextField notes = field(controller, "bulkNotesField"); notes.setText("A note");
				invoke(controller, "applyBulkEdit"); invoke(controller, "acceptSelected");
				Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments"); assertTrue(approvals.isEmpty());
			} finally { controller.dispose(); }
		});
	}

	@Test
	void bulkEditsPreserveOtherCorrectionsSkipSavedRowsAndHandleInvoiceAndCreditDates() throws Exception {
		onFxThread(() -> {
			var controller = controller();
			try {
				var invoice = pending("ORIGINAL", 10000);
				var credit = pending("CREDIT", -2000);
				var saved = pending("SAVED", 30000).withStatus(InvoiceReconciliation.Status.SAVED);
				showAcceptanceRows(controller, List.of(invoice, credit, saved), BatchKind.STATEMENT);
				CheckBox show = field(controller, "showMatchedCheck"); show.setSelected(true);
				Map<ScannedInvoice, Object> approvals = field(controller, "acceptedDocuments");
				approvals.put(invoice.getScanned(), record("AcceptedDocument", 1, "Supplier", ScannedInvoice.DocumentType.INVOICE,
						"CORRECTED", DATE, DATE.plusDays(45), 42.25, "Existing note"));
				TableView<InvoiceReconciliation> table = field(controller, "resultsTable"); table.getSelectionModel().selectAll();
				javafx.scene.control.ChoiceBox<String> fieldChoice = field(controller, "bulkEditFieldChoice"); fieldChoice.setValue("Document date");
				javafx.scene.control.DatePicker date = field(controller, "bulkDatePicker"); date.setValue(DATE.plusDays(5));
				invoke(controller, "applyBulkEdit");
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertEquals(DATE.plusDays(45), rows.getFirst().getReviewValues().dueDate(), "Explicit due date retained");
				assertEquals(DATE.plusDays(5), rows.get(1).getReviewValues().invoiceDate());
				assertNull(rows.get(1).getReviewValues().dueDate());
				assertSame(saved, rows.getLast());
				assertEquals("CORRECTED", rows.getFirst().getInvoiceNo()); assertEquals("$42.25", rows.getFirst().getScannedAmountString());
				Map<ScannedInvoice, ?> drafts = field(controller, "pendingEdits");
				assertEquals("Existing note", invoke(drafts.get(invoice.getScanned()), "notes"));
				fieldChoice.setValue("Due date"); date.setValue(DATE.plusDays(60)); invoke(controller, "applyBulkEdit");
				assertEquals(DATE.plusDays(60), rows.getFirst().getReviewValues().dueDate()); assertFalse(rows.getFirst().isDueDateEstimated());
				assertNull(rows.get(1).getReviewValues().dueDate());
				fieldChoice.setValue("Notes"); TextField notes = field(controller, "bulkNotesField"); notes.setText("");
				invoke(controller, "applyBulkEdit"); assertEquals("", invoke(drafts.get(invoice.getScanned()), "notes"));
				invoke(controller, "acceptSelected");
				assertEquals(2, approvals.size()); assertFalse(approvals.containsKey(saved.getScanned()));
				assertEquals("CORRECTED", invoke(approvals.get(invoice.getScanned()), "reference"));
				assertEquals(42.25, invoke(approvals.get(invoice.getScanned()), "amount"));
			} finally { controller.dispose(); }
		});
	}

	@Test
	void selectedDateEditsRecalculateEstimatedDueDatesAndIncompleteRowsStillRequireReview() throws Exception {
		onFxThread(() -> {
			var controller = controller();
			try {
				var first = pending("ONE", 10000); var second = pending("", 20000); var third = pending("THREE", 30000);
				showAcceptanceRows(controller, List.of(first, second, third), BatchKind.STATEMENT);
				TableView<InvoiceReconciliation> table = field(controller, "resultsTable"); table.getSelectionModel().selectIndices(0, 1);
				javafx.scene.control.ChoiceBox<String> choice = field(controller, "bulkEditFieldChoice"); choice.setValue("Document date");
				javafx.scene.control.DatePicker date = field(controller, "bulkDatePicker"); date.setValue(DATE.plusDays(7));
				invoke(controller, "applyBulkEdit");
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertEquals(DATE.plusDays(37), rows.getFirst().getReviewValues().dueDate()); assertTrue(rows.getFirst().isDueDateEstimated());
				assertSame(third, rows.getLast());
				invoke(controller, "acceptSelected");
				Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments"); assertTrue(approvals.isEmpty());
				Map<ScannedInvoice, ?> drafts = field(controller, "pendingEdits"); assertEquals(2, drafts.size());
				Label message = field(controller, "bulkReviewMessageLabel"); assertTrue(message.getText().contains("Nothing accepted"));
			} finally { controller.dispose(); }
		});
	}

	@Test
	void bulkDraftsSurviveRescanningADifferentSourcePdf() throws Exception {
		onFxThread(() -> {
			var controller = controller();
			try {
				var first = scanned("RESCAN", 10000); var other = scanned("KEEP", 20000);
				invoke(controller, "showLoadedBatches", List.class, List.of(
						loaded(1, 101, BatchKind.INVOICE_BATCH, List.of(first), null, InvoiceReconciliation.Status.NEEDS_REVIEW),
						loaded(2, 102, BatchKind.INVOICE_BATCH, List.of(other), null, InvoiceReconciliation.Status.NEEDS_REVIEW)));
				TableView<InvoiceReconciliation> table = field(controller, "resultsTable"); table.getSelectionModel().select(1);
				javafx.scene.control.ChoiceBox<String> choice = field(controller, "bulkEditFieldChoice"); choice.setValue("Notes");
				TextField notes = field(controller, "bulkNotesField"); notes.setText("Preserve bulk draft"); invoke(controller, "applyBulkEdit");
				invoke(controller, "retainUnaffectedApprovals", long.class, 101L);
				var reloaded = scanned("KEEP", 20000);
				invoke(controller, "showLoadedBatches", List.class, List.of(loaded(2, 102, BatchKind.INVOICE_BATCH, List.of(reloaded), null, InvoiceReconciliation.Status.NEEDS_REVIEW)));
				Map<ScannedInvoice, ?> drafts = field(controller, "pendingEdits");
				assertEquals(1, drafts.size()); assertEquals("Preserve bulk draft", invoke(drafts.get(reloaded), "notes"));
				Map<ScannedInvoice, ?> approvals = field(controller, "acceptedDocuments"); assertTrue(approvals.isEmpty());
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
				assertEquals(2, table.getSelectionModel().getSelectedItems().size());
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
				@Override public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> requestedRows) { return List.of(imported); }
				@Override public Invoice getInvoice(String reference, int storeId) { throw new AssertionError("Already loaded"); }
			});
			setField(controller, "documentAiService", documentService(url -> new StatementComparison(List.of(scanned("INV-1", 10000)), 0)));
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
	void explicitBatchRescanUsesTheRescanEndpointWithoutAnExtraScanNowCall() throws Exception {
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
						if (url.endsWith("/review-batches/17/rescan?storeId=1")) { calls.add("rescan:17:1"); scanned.countDown(); }
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
				invoke(controller, "rescanBatch", BatchSummary.class, empty);
			});
			assertTrue(scanned.await(5, TimeUnit.SECONDS));
			assertEquals(List.of("rescan:17:1"), calls);
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
				@Override public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> requestedRows) { return List.of(imported); }
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
				@Override public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> requestedRows) { return List.of(imported); }
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

	@ParameterizedTest
	@CsvSource({"Symbion,I000123,Symbion", "CH2,000123 RI,CH2", "ParagonCare,000123RI,CH2", "New Supplier,000123 XYZ,New Supplier", "New Supplier,INV-000123,New Supplier"})
	void supplierAffixesMatchLoadedDataAndKeepTheInternalReferenceForReviewAndSaving(
			String supplier, String printedReference, String storedSupplier) throws Exception {
		onFxThread(() -> {
			AiInvoiceScanController controller = controller();
			Invoice imported = symbionImport("000123");
			imported.setSupplierName(storedSupplier);
			List<Invoice> saved = new ArrayList<>();
			InvoiceService service = new InvoiceService() {
				@Override public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> requestedRows) { return List.of(imported); }
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
				var contacts = List.of(new InvoiceSupplier(9, storedSupplier, 1));
				setField(controller, "availableSuppliers", contacts);
				ScannedInvoice scanned = new ScannedInvoice("statement.pdf", supplier, printedReference, DATE, null,
						ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
				invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, List.of(scanned), contacts)));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				InvoiceReconciliation row = rows.getFirst();
				assertEquals(InvoiceReconciliation.Status.MATCHED, row.getStatus());
				assertEquals(printedReference, row.getScanned().invoiceNo());
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
	void mixedStatementKeepsInvoiceAndCreditMatchesWithTheSameNumber() throws Exception {
		onFxThread(() -> {
			var controller = controller();
			try {
				var invoice = symbionImport("000123");
				var credit = symbionImport("000123"); credit.setReconciliationType(ScannedInvoice.DocumentType.CREDIT);
				credit.setImportedInvoiceAmount(-25); credit.setReferenceInvoiceNo("INV-42");
				configureReconciliation(controller, new InvoiceService() {
					@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) {
						assertEquals(2, rows.size()); return List.of(invoice, credit);
					}
				});
				var scans = List.of(
						new ScannedInvoice("statement.pdf", "Symbion", "I000123", DATE, null, ScannedInvoice.DocumentType.INVOICE, 10000, 0.99),
						new ScannedInvoice("statement.pdf", "Symbion", "000123", DATE, null, ScannedInvoice.DocumentType.CREDIT, -2500, 0.99));
				invoke(controller, "showLoadedBatches", List.class, List.of(loadTestBatch(controller, scans)));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertTrue(rows.stream().allMatch(row -> row.getStatus() == InvoiceReconciliation.Status.MATCHED));
				assertSame(invoice, rows.getFirst().getImported()); assertSame(credit, rows.getLast().getImported());
				var saved = new ArrayList<models.Credit>();
				setField(controller, "creditService", new services.CreditService() {
					@Override public void saveOrUpdateCredit(models.Credit value) { saved.add(value); }
				});
				Object approved = record("AcceptedDocument", 9, "Symbion Pty Ltd", ScannedInvoice.DocumentType.CREDIT,
						"000123", DATE, null, 25.0, "Credit note");
				var save = AiInvoiceScanController.class.getDeclaredMethod("saveDocument", InvoiceReconciliation.class, approved.getClass());
				save.setAccessible(true); save.invoke(controller, rows.getLast(), approved);
				assertEquals("INV-42", saved.getFirst().getReferenceInvoiceNo()); assertEquals(25, saved.getFirst().getCreditAmount());
			} finally { controller.dispose(); }
		});
	}

	@Test
	void denseInvoiceBatchMatchesTwoHundredAndTwoRowsUsingBoundedRequests() throws Exception {
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
				@Override public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> requestedRows) {
					assertEquals(1, storeId);
					assertFalse(requestedRows.isEmpty());
					assertTrue(requestedRows.size() <= 50);
					requests.incrementAndGet();
					return imported;
				}
				@Override public Invoice getInvoice(String reference, int storeId) {
					throw new AssertionError("A monthly match must not cause a per-row network request");
				}
			});
			try {
				BatchSummary summary = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED,
						"Invoices", 1, scanned.size(), null, null);
				BatchDetail detail = new BatchDetail(summary, List.of(new ExtractedDocument(101, "statement.pdf", scanned,
						List.of(), DATE.withDayOfMonth(1), DATE.withDayOfMonth(30), null, null, null)));
				Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
				load.setAccessible(true);
				Object loaded = load.invoke(controller, detail, List.of(new InvoiceSupplier(9, "Symbion Pty Ltd", 1)));
				invoke(controller, "showLoadedBatches", List.class, List.of(loaded));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertEquals(202, rows.size());
				assertTrue(rows.stream().allMatch(row -> row.getStatus() == InvoiceReconciliation.Status.MATCHED));
				assertEquals(5, requests.get());
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
				@Override public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> requestedRows) {
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

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void combinedReviewRechecksDifferentScannedAmountsAndReplacesStaleContextValues(boolean found) throws Exception {
		onFxThread(() -> {
			var controller = controller(); var requests = new AtomicInteger();
			try {
				configureReconciliation(controller, new InvoiceService() {
					@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> requested) {
						int call = requests.incrementAndGet();
						if (call > 1 && !found) return List.of();
						var result = symbionImport("000123"); result.setImportedInvoiceAmount(call == 1 ? 100 : 150);
						return List.of(result);
					}
				});
				Class<?> contextType = Class.forName("controllers.AiInvoiceScanController$ReviewLoadContext");
				var constructor = contextType.getDeclaredConstructor(int.class, YearMonth.class); constructor.setAccessible(true);
				var context = constructor.newInstance(1, YearMonth.from(DATE));
				var load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class, contextType); load.setAccessible(true);
				List<InvoiceSupplier> suppliers = List.of(new InvoiceSupplier(9, "Symbion Pty Ltd", 1));
				var batch = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED, "Test", 1, 1, null, null);
				for (int amount : List.of(10000, 15000, 15000)) {
					var row = new ScannedInvoice("invoice.pdf", "Symbion", "I000123", DATE, null, ScannedInvoice.DocumentType.INVOICE, amount, 0.99);
					var detail = new BatchDetail(batch, List.of(new ExtractedDocument(101, "invoice.pdf", List.of(row), List.of(), null, null, null, null, null)));
					var loaded = load.invoke(controller, detail, suppliers, context);
					Object run = invoke(loaded, "run");
					@SuppressWarnings("unchecked") List<InvoiceReconciliation> rows = (List<InvoiceReconciliation>) invoke(run, "reconciliations");
					assertEquals(amount == 10000 || found ? InvoiceReconciliation.Status.MATCHED : InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, rows.getFirst().getStatus());
					if (amount == 15000 && found) assertEquals(150, rows.getFirst().getImported().getImportedInvoiceAmount());
				}
				assertEquals(2, requests.get(), "A different scanned amount is rechecked; an identical repeat shares the result");
			} finally { controller.dispose(); }
		});
	}

	@Test
	void resolvedSymbionCanonicalReferencesAndMissesDoNotNeedIndividualDesktopLookups() throws Exception {
		onFxThread(() -> {
			for (boolean found : List.of(true, false)) {
				AiInvoiceScanController controller = controller();
				List<String> requested = new ArrayList<>();
				Invoice imported = symbionImport("000123");
				configureReconciliation(controller, new InvoiceService() {
					@Override public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> requestedRows) {
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
					@Override public List<Invoice> getInvoiceAmountsForReview(int storeId, List<ScannedInvoice> requestedRows) {
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
			Label hint = field(controller, "combinedReviewHint");
			assertTrue(hint.isVisible());
			assertTrue(hint.getText().contains("2 failed or unfinished"));
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
			@SuppressWarnings("unchecked")
			List<EnteredDocument> entered = (List<EnteredDocument>) invoke(controller, "buildEnteredDocuments");
			assertEquals(List.of(101L, 201L), entered.stream().map(EnteredDocument::documentId).toList());
			assertTrue(entered.stream().allMatch(document -> DATE.equals(document.invoiceDate()) && !document.rows().isEmpty()));
		});
	}

	@Test
	void statementUsesEnteredInvoiceEvidenceAndCannotCreateInvoiceEntries() throws Exception {
		onFxThread(() -> {
			var controller = controller();
			try {
				configureReconciliation(controller, new InvoiceService() {
					@Override public List<Invoice> getInvoiceAmountsForReview(int store, List<ScannedInvoice> rows) {
						throw new AssertionError("Statements must not query Z-Office");
					}
				});
				var source = scanned("INV-1", 10000);
				var entered = scanned("INV-1", 12000);
				setField(controller, "documentAiService", documentService(url -> {
					assertTrue(url.contains("/statement-comparison?storeId=1"));
					return new StatementComparison(List.of(entered), 0);
				}));
				var summary = new BatchSummary(17, BatchKind.STATEMENT, BatchStatus.REVIEW_REQUIRED, "Statement", 1, 1, null, null);
				var detail = new BatchDetail(summary, List.of(new ExtractedDocument(101, "statement.pdf", List.of(source), List.of(),
						DATE.withDayOfMonth(1), DATE.withDayOfMonth(30), 10000L, .99, null)));
				Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
				load.setAccessible(true);
				invoke(controller, "showLoadedBatches", List.class, List.of(load.invoke(controller, detail, List.of(new InvoiceSupplier(1, "Supplier", 1)))));
				ObservableList<InvoiceReconciliation> rows = field(controller, "allResults");
				assertEquals(InvoiceReconciliation.Status.AMOUNT_MISMATCH, rows.getFirst().getStatus());
				assertEquals("$20.00", rows.getFirst().getVarianceString());
				assertEquals(List.of(), invoke(controller, "buildPendingSaves"));
				assertEquals(List.of(), invoke(controller, "buildEnteredDocuments"));
				invoke(controller, "review", InvoiceReconciliation.class, rows.getFirst());
				TextField amount = field(controller, "reviewAmountField");
				assertTrue(amount.isDisabled());
				Button refresh = field(controller, "refreshInvoiceAmountButton"); assertFalse(refresh.isVisible());
				invoke(controller, "acceptReview");
				assertEquals(InvoiceReconciliation.Status.ACCEPTED, rows.getFirst().getStatus());
				Button complete = field(controller, "saveAcceptedButton"); assertEquals("Complete reconciliation", complete.getText());
				assertFalse(complete.isDisabled());
				assertEquals(List.of(), invoke(controller, "buildPendingSaves"));
			} finally { controller.dispose(); }
		});
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void statementInvoiceEditsAreDraftedThenSentToTheirOriginalEntryOnFinalSave(boolean credit) throws Exception {
		var saved = new CountDownLatch(1); var refreshed = new CountDownLatch(1);
		var requests = new CopyOnWriteArrayList<StatementCompletion>();
		var tableRefreshes = new AtomicInteger();
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newSingleThreadExecutor()) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller, new InvoiceService() {
						@Override public void saveOrUpdateInvoice(Invoice invoice, String ref, int supplier) {
							throw new AssertionError("Statement edits must use the atomic completion endpoint");
						}
					});
					setField(controller,"executor",workers);
					setField(controller,"parent",new InvoiceEntryController() {
						{ dialogPane = new com.dlsc.gemsfx.DialogPane(); }
						@Override public void fillInvoiceTable() { tableRefreshes.incrementAndGet(); }
						@Override public void fillCreditTable() { tableRefreshes.incrementAndGet(); }
					});
					var source = scanned(credit ? "C1" : "INV-1", credit ? -10000 : 10000);
					var original = new StatementInvoice(7,2,scanned(source.invoiceNo(),credit ? -12000 : 12000),"original note");
					setField(controller,"documentAiService",documentService((url,entity) -> {
						if (url.contains("/statement-comparison")) return new StatementComparison(List.of(original.values()),0,List.of(original));
						if (url.contains("/complete-statement?")) {
							assertFalse(Platform.isFxApplicationThread());
							requests.add(assertInstanceOf(StatementCompletion.class,entity.getBody())); saved.countDown(); return null;
						}
						assertTrue(url.endsWith("/review-queue")); refreshed.countDown(); return "[]";
					}));
					var summary = new BatchSummary(17,BatchKind.STATEMENT,BatchStatus.REVIEW_REQUIRED,"Statement",1,1,null,null);
					var detail = new BatchDetail(summary,List.of(new ExtractedDocument(101,"statement.pdf",List.of(source),List.of(),
							DATE.withDayOfMonth(1),DATE.withDayOfMonth(30),10000L,.99,null)));
					Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch",BatchDetail.class,List.class); load.setAccessible(true);
					invoke(controller,"showLoadedBatches",List.class,List.of(load.invoke(controller,detail,List.of(new InvoiceSupplier(1,"Supplier",1)))));
					ObservableList<InvoiceReconciliation> rows = field(controller,"allResults");
					invoke(controller,"review",InvoiceReconciliation.class,rows.getFirst());
					TextField amount = field(controller,"reviewAmountField"); assertFalse(amount.isDisabled()); assertEquals("120.00",amount.getText());
					TextField reference = field(controller,"reviewReferenceField"); assertTrue(reference.isDisabled());
					amount.setText("135.50");
					javafx.scene.control.DatePicker date = field(controller,"reviewDateField"); date.setValue(DATE.minusDays(1));
					TextArea notes = field(controller,"reviewNotesField"); notes.setText("corrected note");
					invoke(controller,"acceptReview");
					assertTrue(requests.isEmpty(),"Accepting a row must only create a draft");
					assertEquals(credit ? -135.5 : 135.5,rows.getFirst().getImported().getImportedInvoiceAmount());
					assertEquals(source.amountCents(),rows.getFirst().getScanned().amountCents(),"Statement evidence stays unchanged");
					invoke(controller,"review",InvoiceReconciliation.class,rows.getFirst());
					assertEquals("135.50",amount.getText()); assertEquals("corrected note",notes.getText());
					invoke(controller,"backToResults");
					invoke(controller,"saveAccepted");
				});
				assertTrue(saved.await(5,TimeUnit.SECONDS)); assertTrue(refreshed.await(5,TimeUnit.SECONDS));
				workers.submit(() -> {}).get(5,TimeUnit.SECONDS);
				onFxThread(() -> {
					assertEquals(1,requests.size()); var edit = requests.getFirst().edits().getFirst();
					assertEquals(7,edit.original().documentId()); assertEquals(2,edit.original().rowIndex());
					assertEquals(credit ? -13550L : 13550L,edit.amountCents()); assertEquals(DATE.minusDays(1),edit.invoiceDate());
					assertEquals("corrected note",edit.notes()); assertEquals(2,tableRefreshes.get());
				});
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void statementReviewCanSwitchToTheMatchedPdfAndBackWithoutLosingInvoiceEdits(boolean credit) throws Exception {
		var statementRendered = new CountDownLatch(1); var invoiceRendered = new CountDownLatch(1); var statementReturned = new CountDownLatch(1);
		var statementRenders = new AtomicInteger(); var requested = new CopyOnWriteArrayList<Long>();
		byte[] statementPdf = pdf(72), invoicePdf = pdf(144);
		AiInvoiceScanController[] holder = new AiInvoiceScanController[1];
		try (var workers = Executors.newCachedThreadPool(); var cache = new DocumentPdfCache((document,store,path,limit) -> {
			assertEquals(1,store); assertTrue(document == 101 || document == 7); requested.add(document);
			Files.write(path,document == 101 ? statementPdf : invoicePdf); return true;
		})) {
			try {
				onFxThread(() -> {
					var controller = controller(); holder[0] = controller;
					configureReconciliation(controller,new InvoiceService());
					setField(controller,"executor",workers); setField(controller,"pdfCache",cache); setField(controller,"reviewStoreId",1);
					var source = new ScannedInvoice("statement.pdf","Supplier",credit ? "C1" : "INV-1",DATE,null,
							credit ? ScannedInvoice.DocumentType.CREDIT : ScannedInvoice.DocumentType.INVOICE,credit ? -10000 : 10000,.99);
					var entered = new StatementInvoice(7,2,new ScannedInvoice("archived invoice.pdf","Supplier",source.invoiceNo(),DATE,null,
							source.documentType(),credit ? -12000 : 12000,.99),"original note");
					setField(controller,"documentAiService",documentService(url -> new StatementComparison(List.of(entered.values()),0,List.of(entered))));
					var summary = new BatchSummary(17,BatchKind.STATEMENT,BatchStatus.REVIEW_REQUIRED,"Statement",1,1,null,null);
					var evidence = Map.of(models.PdfEvidenceField.REFERENCE,new models.PdfEvidenceLocation(0,5,5,20,10,72,72));
					var detail = new BatchDetail(summary,List.of(new ExtractedDocument(101,"statement.pdf",List.of(source),List.of(evidence),
							DATE.withDayOfMonth(1),DATE.withDayOfMonth(30),10000L,.99,null)));
					Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch",BatchDetail.class,List.class); load.setAccessible(true);
					invoke(controller,"showLoadedBatches",List.class,List.of(load.invoke(controller,detail,List.of(new InvoiceSupplier(1,"Supplier",1)))));
					ImageView preview = field(controller,"pdfPreviewImage"); preview.imageProperty().addListener((_,_,image) -> {
						if (image == null) return;
						if (image.getWidth() == 288) invoiceRendered.countDown();
						else if (statementRenders.incrementAndGet() == 1) statementRendered.countDown(); else statementReturned.countDown();
					});
					TableView<InvoiceReconciliation> table = field(controller,"resultsTable");
					var row = table.getRowFactory().call(table); row.updateTableView(table); row.updateIndex(0);
					row.getOnMouseClicked().handle(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
							0,0,0,0,javafx.scene.input.MouseButton.PRIMARY,2,
							false,false,false,false,false,false,false,false,false,false,null));
				});
				assertTrue(statementRendered.await(10,TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0];
					Button toggle = field(controller,"toggleStatementPdfButton"); assertTrue(toggle.isVisible());
					TextField amount = field(controller,"reviewAmountField"); amount.setText("135.50");
					TextArea notes = field(controller,"reviewNotesField"); notes.setText("Keep my draft while reading the PDF");
					javafx.scene.control.DatePicker date = field(controller,"reviewDateField"); date.setValue(DATE.minusDays(1));
					Button invoice = field(controller,"toggleStatementPdfButton"); assertEquals(credit ? "Show credit PDF" : "Show invoice PDF",invoice.getText());
					assertFalse(invoice.isDisabled()); invoice.fire();
				});
				assertTrue(invoiceRendered.await(10,TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0]; assertEquals(7L,(Long)field(controller,"currentPreviewDocumentId"));
					Map<?,?> evidence = field(controller,"currentEvidence"); assertTrue(evidence.isEmpty());
					javafx.scene.layout.Pane overlay = field(controller,"pdfOverlayPane"); assertTrue(overlay.getChildren().isEmpty());
					Label label = field(controller,"previewDocumentLabel"); assertTrue(label.getText().contains("archived invoice.pdf"));
					assertTrue(label.getText().startsWith(credit ? "Credit" : "Invoice"));
					TextField amount = field(controller,"reviewAmountField"); assertEquals("135.50",amount.getText());
					Button statement = field(controller,"toggleStatementPdfButton"); assertEquals("Show statement PDF",statement.getText()); statement.fire();
				});
				assertTrue(statementReturned.await(10,TimeUnit.SECONDS));
				onFxThread(() -> {
					var controller = holder[0]; assertEquals(101L,(Long)field(controller,"currentPreviewDocumentId"));
					javafx.scene.layout.Pane overlay = field(controller,"pdfOverlayPane"); assertEquals(1,overlay.getChildren().size());
					Label label = field(controller,"previewDocumentLabel"); assertTrue(label.getText().startsWith("Statement"));
					TextArea notes = field(controller,"reviewNotesField"); assertEquals("Keep my draft while reading the PDF",notes.getText());
					javafx.scene.control.DatePicker date = field(controller,"reviewDateField"); assertEquals(DATE.minusDays(1),date.getValue());
					invoke(controller,"acceptReview");
					Map<ScannedInvoice,StatementInvoiceEdit> edits = field(controller,"statementInvoiceEdits"); assertEquals(1,edits.size());
					var edit = edits.values().iterator().next(); assertEquals(7,edit.original().documentId());
					assertEquals(credit ? -13550L : 13550L,edit.amountCents());
				});
				assertTrue(requested.containsAll(List.of(101L,7L)));
			} finally { if (holder[0] != null) onFxThread(holder[0]::dispose); }
		}
	}

	@ParameterizedTest
	@EnumSource(value = InvoiceReconciliation.Status.class,names = {"MISSING_SCANNED_INVOICE","AMBIGUOUS"})
	void statementRowsWithoutAUniqueInvoiceCannotOpenAnUnrelatedPdf(InvoiceReconciliation.Status status) throws Exception {
		onFxThread(() -> {
			var controller = controller();
			try {
				var source = scanned("INV-1",10000);
				invoke(controller,"showLoadedBatches",List.class,List.of(loaded(17,101,BatchKind.STATEMENT,List.of(source),10000L,status)));
				Map<ScannedInvoice,StatementInvoice> invoices = field(controller,"statementInvoicesByRow");
				invoices.put(source,new StatementInvoice(7,0,source,""));
				ObservableList<InvoiceReconciliation> rows = field(controller,"allResults"); invoke(controller,"review",InvoiceReconciliation.class,rows.getFirst());
				invoke(controller,"showEvidence",InvoiceReconciliation.class,rows.getFirst());
				Button invoice = field(controller,"toggleStatementPdfButton"); assertTrue(invoice.isDisabled());
				invoke(controller,"toggleStatementPdf"); assertEquals(101L,(Long)field(controller,"currentPreviewDocumentId"));
				assertTrue(invoice.getTooltip().getText().contains("No uniquely matched"));
			} finally { controller.dispose(); }
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
		BatchSummary batch = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED, "Review test", 1, rows.size(), null, null);
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
		return loadTestBatch(controller, scanned, List.of(new InvoiceSupplier(9, "Symbion Pty Ltd", 1)));
	}

	private static Object loadTestBatch(AiInvoiceScanController controller, List<ScannedInvoice> scanned,
			List<InvoiceSupplier> suppliers) throws Exception {
		BatchSummary summary = new BatchSummary(1, BatchKind.INVOICE_BATCH, BatchStatus.REVIEW_REQUIRED, "Test", 1, scanned.size(), null, null);
		BatchDetail detail = new BatchDetail(summary, List.of(new ExtractedDocument(101, "statement.pdf", scanned,
				List.of(), null, null, null, null, null)));
		Method load = AiInvoiceScanController.class.getDeclaredMethod("loadBatch", BatchDetail.class, List.class);
		load.setAccessible(true);
		return load.invoke(controller, detail, suppliers);
	}

	private static Invoice symbionImport(String reference) {
		Invoice invoice = new Invoice();
		invoice.setInvoiceNo(reference);
		invoice.setSupplierID(9);
		invoice.setSupplierName("SYMBION");
		invoice.setInvoiceDate(DATE);
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
			assertEquals(3, buttons.getChildren().size());
			Button rescan = assertInstanceOf(Button.class, buttons.getChildren().get(1));
			boolean saved = row.getStatus() == InvoiceReconciliation.Status.SAVED;
			assertEquals(saved ? "Saved" : "Review", review.getText());
			assertEquals(saved, review.isDisabled());
			assertEquals(saved, trash.isDisabled());
			assertEquals(saved, rescan.isDisabled());
			assertEquals("Rescan source PDF", rescan.getAccessibleText());
			assertEquals("Trash", trash.getText());
		}
	}

	private static Object loaded(long batchId, long documentId, BatchKind kind, List<ScannedInvoice> scanned, Long total) throws Exception {
		return loaded(batchId, documentId, kind, scanned, total, InvoiceReconciliation.Status.MATCHED);
	}

	private static services.DocumentAiService documentService(java.util.function.Function<String, Object> response) throws Exception {
		return documentService((url, _) -> response.apply(url));
	}

	private static services.DocumentAiService documentService(java.util.function.BiFunction<String, org.springframework.http.HttpEntity<?>, Object> response) throws Exception {
		var rest = new org.springframework.web.client.RestTemplate() {
			@Override public <T> org.springframework.http.ResponseEntity<T> exchange(String url,
					org.springframework.http.HttpMethod method, org.springframework.http.HttpEntity<?> entity,
					Class<T> type, Object... variables) {
				return org.springframework.http.ResponseEntity.ok(type.cast(response.apply(url, entity)));
			}
		};
		var constructor = services.DocumentAiService.class.getDeclaredConstructor(
				org.springframework.web.client.RestTemplate.class, java.util.Properties.class);
		constructor.setAccessible(true);
		return constructor.newInstance(rest, new java.util.Properties());
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
