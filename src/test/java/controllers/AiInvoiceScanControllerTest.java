package controllers;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import models.DocumentAiModels.*;
import models.InvoiceReconciliation;
import models.ScannedInvoice;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AiInvoiceScanControllerTest {
	private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
	private static boolean toolkitStarted;

	@BeforeAll
	static void startJavaFx() throws Exception {
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

	private static Object loaded(long batchId, long documentId, BatchKind kind, List<ScannedInvoice> scanned, Long total) throws Exception {
		Map<ScannedInvoice, Long> ids = new IdentityHashMap<>();
		scanned.forEach(row -> ids.put(row, documentId));
		List<InvoiceReconciliation> rows = scanned.stream().map(row ->
				new InvoiceReconciliation(InvoiceReconciliation.Status.MATCHED, row, null, null)).toList();
		Object run = record("ScanRun", scanned, rows, Map.of(), Map.of(), ids,
				DATE.withDayOfMonth(1), DATE.withDayOfMonth(30), total, 0.99, List.of());
		BatchSummary batch = new BatchSummary(batchId, kind, BatchStatus.REVIEW_REQUIRED,
				"Batch " + batchId, 1, rows.size(), null, null);
		return record("LoadedBatch", batch, run, List.of(), List.of(documentId));
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
