package controllers;

import application.Main;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
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
					@Override public List<Invoice> getInvoiceTableData(int storeId, YearMonth month) {
						assertEquals(1, storeId);
						assertEquals(YearMonth.from(DATE), month);
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

	private static AiInvoiceScanController controller() throws Exception {
		FXMLLoader loader = new FXMLLoader(AiInvoiceScanControllerTest.class.getResource("/views/FXML/AiInvoiceScan.fxml"));
		Parent root = loader.load();
		assertNotNull(root);
		return loader.getController();
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

	private static void invoke(Object target, String name, Class<?> parameterType, Object argument) throws Exception {
		Method method = target.getClass().getDeclaredMethod(name, parameterType);
		method.setAccessible(true);
		method.invoke(target, argument);
	}

	private static void onFxThread(ThrowingRunnable action) throws Exception {
		FutureTask<Void> task = new FutureTask<>(() -> { action.run(); return null; });
		Platform.runLater(task);
		task.get(15, TimeUnit.SECONDS);
	}

	@FunctionalInterface
	private interface ThrowingRunnable { void run() throws Exception; }
}
