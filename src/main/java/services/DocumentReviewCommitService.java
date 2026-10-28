package services;

import models.DocumentAiModels.BatchSummary;
import models.DocumentAiModels.EnteredDocument;
import models.InvoiceReconciliation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

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
		return commit(saves, completions, complete, (_, _) -> {});
	}

	/** Reports completed operations and the current phase without touching UI state. */
	public static Result commit(List<SaveOperation> saves, List<Completion> completions,
			Consumer<Completion> complete, BiConsumer<Integer, String> progress) {
		List<InvoiceReconciliation> saved = new ArrayList<>();
		List<Long> completed = new ArrayList<>();
		List<String> completionFailures = new ArrayList<>();
		for (SaveOperation save : saves) {
			try {
				progress.accept(saved.size(), "Saving documents… " + saved.size() + " of " + saves.size());
				save.action().run();
				saved.add(save.row());
			} catch (Exception exception) {
				return new Result(List.copyOf(saved), List.copyOf(completed),
						"Could not save " + save.row().getInvoiceNo() + ": " + errorMessage(exception));
			}
		}
		int attempted = 0;
		for (Completion completion : completions) {
			try {
				progress.accept(saved.size() + attempted, "Documents saved. Filing source PDFs… "
						+ attempted + " of " + completions.size() + " batches");
				complete.accept(completion);
				completed.add(completion.batch().id());
			} catch (Exception exception) {
				// Filing one source batch must not prevent independent batches from completing.
				completionFailures.add("“" + completion.batch().sourceLabel() + "” (batch "
						+ completion.batch().id() + "): " + errorMessage(exception));
			}
			attempted++;
		}
		if (!completionFailures.isEmpty()) {
			String message = "Documents were saved. Completed " + completed.size() + " of " + completions.size()
					+ " batches; " + completionFailures.size() + " could not be completed:\n"
					+ String.join("\n", completionFailures)
					+ "\nUse Retry completion to finish filing only the remaining batches.";
			progress.accept(saved.size() + attempted, message);
			return new Result(List.copyOf(saved), List.copyOf(completed), message);
		}
		progress.accept(saved.size() + completed.size(), "Documents saved and source PDFs filed.");
		return new Result(List.copyOf(saved), List.copyOf(completed), null);
	}

	private static String errorMessage(Exception exception) {
		Throwable root = exception;
		while (root.getCause() != null && root.getCause() != root) root = root.getCause();
		return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
	}
}
