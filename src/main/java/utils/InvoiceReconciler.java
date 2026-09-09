package utils;

import models.Invoice;
import models.InvoiceReconciliation;
import models.ScannedInvoice;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static models.InvoiceReconciliation.Status.AMBIGUOUS;
import static models.InvoiceReconciliation.Status.AMOUNT_MISMATCH;
import static models.InvoiceReconciliation.Status.MATCHED;
import static models.InvoiceReconciliation.Status.NEEDS_REVIEW;
import static models.InvoiceReconciliation.Status.NOT_IN_Z_OFFICE;
import static models.InvoiceReconciliation.Status.WITHIN_TOLERANCE;

/** Deterministic, side-effect free comparison of scanned evidence to Z-Office rows. */
public final class InvoiceReconciler {
	public static final long DEFAULT_TOLERANCE_CENTS = 20;
	public static final double REVIEW_CONFIDENCE = 0.75;
	private static final Set<String> COMPANY_SUFFIXES = Set.of("PTY", "LTD", "LIMITED", "AUSTRALIA", "THE");

	private InvoiceReconciler() {}

	public static List<InvoiceReconciliation> reconcile(List<ScannedInvoice> scannedRows,
			List<Invoice> importedRows) {
		return reconcile(scannedRows, importedRows, DEFAULT_TOLERANCE_CENTS);
	}

	public static List<InvoiceReconciliation> reconcile(List<ScannedInvoice> scannedRows,
			List<Invoice> importedRows, long toleranceCents) {
		Map<String, List<Invoice>> byReference = importedRows.stream()
				.filter(Invoice::isImportExists)
				.filter(invoice -> invoice.getInvoiceNo() != null && !invoice.getInvoiceNo().isBlank())
				.collect(Collectors.groupingBy(invoice -> reference(invoice.getInvoiceNo()), LinkedHashMap::new, Collectors.toList()));
		List<InvoiceReconciliation> results = new ArrayList<>();

		for (ScannedInvoice scanned : scannedRows) {
			if (scanned.invoiceNo().isBlank()) {
				results.add(new InvoiceReconciliation(NEEDS_REVIEW, scanned, null, null));
				continue;
			}

			List<Invoice> candidates = new ArrayList<>(byReference.getOrDefault(reference(scanned.invoiceNo()), List.of()));
			if (candidates.size() > 1 && !scanned.supplierName().isBlank()) {
				List<Invoice> supplierMatches = candidates.stream()
						.filter(candidate -> supplier(candidate.getSupplierName()).equals(supplier(scanned.supplierName())))
						.toList();
				if (!supplierMatches.isEmpty()) candidates = supplierMatches;
			}

			if (candidates.isEmpty()) {
				results.add(new InvoiceReconciliation(NOT_IN_Z_OFFICE, scanned, null, null));
			} else if (candidates.size() > 1) {
				results.add(new InvoiceReconciliation(AMBIGUOUS, scanned, null, null));
			} else {
				Invoice imported = candidates.getFirst();
				long importedCents = Math.round(imported.getImportedInvoiceAmount() * 100);
				long variance = scanned.amountCents() - importedCents;
				InvoiceReconciliation.Status status;
				if (scanned.supplierName().isBlank() || scanned.confidence() == null
						|| scanned.confidence() < REVIEW_CONFIDENCE) {
					status = NEEDS_REVIEW;
				} else {
					status = variance == 0
							? MATCHED
							: Math.abs(variance) <= toleranceCents ? WITHIN_TOLERANCE : AMOUNT_MISMATCH;
				}
				results.add(new InvoiceReconciliation(status, scanned, imported, variance));
			}
		}
		return List.copyOf(results);
	}

	public static String reference(String value) {
		return text(value).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
	}

	public static String supplier(String value) {
		String ascii = Normalizer.normalize(text(value), Normalizer.Form.NFKD)
				.replaceAll("\\p{M}", "")
				.toUpperCase(Locale.ROOT)
				.replaceAll("[^A-Z0-9]+", " ")
				.trim();
		return java.util.Arrays.stream(ascii.split("\\s+"))
				.filter(word -> !COMPANY_SUFFIXES.contains(word))
				.reduce((left, right) -> left + " " + right)
				.orElse("");
	}

	private static String text(String value) {
		return value == null ? "" : value.trim();
	}
}
