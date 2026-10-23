package services;

import models.DocumentAiModels.BatchSummary;
import models.DocumentAiModels.EnteredDocument;
import models.InvoiceReconciliation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Saves a combined review while retaining each source batch's filing boundary. */
public final class DocumentReviewCommitService {
	private DocumentReviewCommitService() {}

	public record SaveOperation(InvoiceReconciliation row, Runnable action) {}
	public record Completion(BatchSummary batch, List<EnteredDocument> documents) {}

	public record Result(List<InvoiceReconciliation> savedRows, List<Long> completedBatchIds,
			String failureMessage) {}

	public static List<Completion> completions(List<BatchSummary> batches,
			Map<Long, Long> batchIdsByDocument, List<EnteredDocument> documents, Set<Long> completed) {
		return batches.stream()
				.filter(batch -> !completed.contains(batch.id()))
				.filter(batch -> batchIdsByDocument.containsValue(batch.id()))
				.map(batch -> new Completion(batch, documents.stream()
						.filter(document -> Long.valueOf(batch.id()).equals(batchIdsByDocument.get(document.documentId())))
						.toList()))
				.toList();
	}

	public static Result commit(List<SaveOperation> saves, List<Completion> completions,
			Consumer<Completion> complete) {
		List<InvoiceReconciliation> saved = new ArrayList<>();
		List<Long> completed = new ArrayList<>();
		for (SaveOperation save : saves) {
			try {
				save.action().run();
				saved.add(save.row());
			} catch (Exception exception) {
				return new Result(List.copyOf(saved), List.copyOf(completed),
						"Could not save " + save.row().getInvoiceNo() + ": " + errorMessage(exception));
			}
		}
		for (Completion completion : completions) {
			try {
				complete.accept(completion);
				completed.add(completion.batch().id());
			} catch (Exception exception) {
				return new Result(List.copyOf(saved), List.copyOf(completed),
						"Documents were saved, but “" + completion.batch().sourceLabel()
								+ "” could not be completed: " + errorMessage(exception)
								+ "\nUse Retry completion to finish filing the remaining batches.");
			}
		}
		return new Result(List.copyOf(saved), List.copyOf(completed), null);
	}

	private static String errorMessage(Exception exception) {
		Throwable root = exception;
		while (root.getCause() != null && root.getCause() != root) root = root.getCause();
		return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
	}
}
