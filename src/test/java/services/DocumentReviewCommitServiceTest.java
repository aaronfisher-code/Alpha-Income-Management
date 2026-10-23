package services;

import models.DocumentAiModels.BatchKind;
import models.DocumentAiModels.BatchStatus;
import models.DocumentAiModels.BatchSummary;
import models.DocumentAiModels.EnteredDocument;
import models.InvoiceReconciliation;
import models.ScannedInvoice;
import org.junit.jupiter.api.Test;
import services.DocumentReviewCommitService.Completion;
import services.DocumentReviewCommitService.SaveOperation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DocumentReviewCommitServiceTest {
	private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
	private static final BatchSummary FIRST = batch(1, BatchKind.INVOICE_BATCH);
	private static final BatchSummary SECOND = batch(2, BatchKind.INVOICE_BATCH);

	@Test
	void eachBatchReceivesOnlyItsOwnDocumentsAndReviewedDates() {
		List<EnteredDocument> documents = List.of(new EnteredDocument(101, DATE),
				new EnteredDocument(102, DATE.plusMonths(1)), new EnteredDocument(201, DATE.minusMonths(1)));
		List<Completion> plan = DocumentReviewCommitService.completions(List.of(FIRST, SECOND),
				Map.of(101L, 1L, 102L, 1L, 201L, 2L), documents, Set.of());

		assertEquals(List.of(new Completion(FIRST, documents.subList(0, 2)),
				new Completion(SECOND, documents.subList(2, 3))), plan);
	}

	@Test
	void savesAllRowsBeforeCompletingOriginalBatches() {
		List<String> events = new ArrayList<>();
		var result = DocumentReviewCommitService.commit(List.of(
				new SaveOperation(row("A"), () -> events.add("save A")),
				new SaveOperation(row("B"), () -> events.add("save B"))),
				List.of(new Completion(FIRST, List.of()), new Completion(SECOND, List.of())),
				completion -> events.add("complete " + completion.batch().id()));

		assertEquals(List.of("save A", "save B", "complete 1", "complete 2"), events);
		assertEquals(2, result.savedRows().size());
		assertEquals(List.of(1L, 2L), result.completedBatchIds());
		assertNull(result.failureMessage());
	}

	@Test
	void failedSaveRetainsProgressAndDoesNotCompleteUnsavedBatches() {
		InvoiceReconciliation first = row("A");
		var result = DocumentReviewCommitService.commit(List.of(
				new SaveOperation(first, () -> {}),
				new SaveOperation(row("B"), () -> { throw new IllegalStateException("API unavailable"); })),
				List.of(new Completion(FIRST, List.of())), completion -> fail("Must not complete after failed save"));

		assertEquals(List.of(first), result.savedRows());
		assertTrue(result.completedBatchIds().isEmpty());
		assertTrue(result.failureMessage().contains("Could not save B"));
	}

	@Test
	void completionRetryKeepsSavedDocumentDatesAndSkipsCompletedBatches() {
		List<EnteredDocument> documents = List.of(new EnteredDocument(101, DATE), new EnteredDocument(201, DATE.plusMonths(1)));
		Map<Long, Long> owners = Map.of(101L, 1L, 201L, 2L);
		List<Completion> plan = DocumentReviewCommitService.completions(List.of(FIRST, SECOND), owners, documents, Set.of());
		var first = DocumentReviewCommitService.commit(List.of(new SaveOperation(row("A"), () -> {})), plan,
				completion -> { if (completion.batch().id() == 2) throw new IllegalStateException("Drive unavailable"); });

		assertEquals(List.of(1L), first.completedBatchIds());
		assertTrue(first.failureMessage().contains("Retry completion"));
		List<Completion> remaining = DocumentReviewCommitService.completions(List.of(FIRST, SECOND),
				owners, documents, Set.copyOf(first.completedBatchIds()));
		List<Completion> requests = new ArrayList<>();
		var retry = DocumentReviewCommitService.commit(List.of(), remaining, requests::add);

		assertEquals(List.of(new Completion(SECOND, List.of(documents.get(1)))), requests);
		assertEquals(List.of(2L), retry.completedBatchIds());
		assertNull(retry.failureMessage());
	}

	@Test
	void fullyTrashedBatchIsSkippedAndStatementCanCompleteWithoutInvoiceDates() {
		BatchSummary statement = batch(3, BatchKind.STATEMENT);
		List<Completion> plan = DocumentReviewCommitService.completions(List.of(FIRST, statement),
				Map.of(301L, 3L), List.of(), Set.of());
		assertEquals(List.of(new Completion(statement, List.of())), plan);
	}

	private static BatchSummary batch(long id, BatchKind kind) {
		return new BatchSummary(id, kind, BatchStatus.REVIEW_REQUIRED, "Batch " + id, 1, 1, null, null);
	}

	private static InvoiceReconciliation row(String reference) {
		ScannedInvoice scanned = new ScannedInvoice("invoice.pdf", "Supplier", reference, DATE, null,
				ScannedInvoice.DocumentType.INVOICE, 10000, 0.99);
		return new InvoiceReconciliation(InvoiceReconciliation.Status.ACCEPTED, scanned, null, null);
	}
}
