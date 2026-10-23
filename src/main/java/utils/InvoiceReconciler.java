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
	public static final double REVIEW_CONFIDENCE = 0.90;
	private static final Set<String> COMPANY_SUFFIXES = Set.of("PTY", "LTD", "LIMITED", "AUSTRALIA", "THE");

	private InvoiceReconciler() {}

	public static List<InvoiceReconciliation> reconcile(List<ScannedInvoice> scannedRows,
			List<Invoice> importedRows) {
		return reconcile(scannedRows, importedRows, DEFAULT_TOLERANCE_CENTS);
	}

	public static List<InvoiceReconciliation> reconcile(List<ScannedInvoice> scannedRows,
			List<Invoice> importedRows, long toleranceCents) {
		Map<String, List<Invoice>> byReference = indexImportedRows(importedRows);
		List<InvoiceReconciliation> results = new ArrayList<>();

		for (ScannedInvoice scanned : scannedRows) {
			if (scanned.invoiceNo().isBlank()) {
				results.add(new InvoiceReconciliation(NEEDS_REVIEW, scanned, null, null));
				continue;
			}

			List<Invoice> candidates = matchingImportedRows(scanned, byReference);

			if (candidates.isEmpty()) {
				results.add(new InvoiceReconciliation(NOT_IN_Z_OFFICE, scanned, null, null));
			} else if (candidates.size() > 1) {
				results.add(new InvoiceReconciliation(AMBIGUOUS, scanned, null, null));
			} else {
				Invoice imported = candidates.getFirst();
				long importedCents = Math.round(imported.getImportedInvoiceAmount() * 100);
				long variance = scanned.amountCents() - importedCents;
				InvoiceReconciliation.Status status;
				if (scanned.amountCents() == 0 && !confident(scanned.fieldConfidences().amount())) {
					// Missing/uncertain amounts may stand for zero, but require a reliable
					// identity and an actual Z-Office zero before automatic acceptance.
					status = !confidentIdentity(scanned) ? NEEDS_REVIEW : importedCents == 0 ? MATCHED : AMOUNT_MISMATCH;
				} else if (scanned.supplierName().isBlank() || scanned.requiredFieldConfidence() == null
						|| scanned.requiredFieldConfidence() < REVIEW_CONFIDENCE) {
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

	private static boolean confidentIdentity(ScannedInvoice scanned) {
		ScannedInvoice.FieldConfidences fields = scanned.fieldConfidences();
		return !scanned.supplierName().isBlank() && !scanned.invoiceNo().isBlank() && scanned.invoiceDate() != null
				&& confident(fields.supplier()) && confident(fields.reference()) && confident(fields.date()) && confident(fields.type());
	}

	private static boolean confident(Double value) { return value != null && value >= REVIEW_CONFIDENCE; }

	public static Map<String, List<Invoice>> indexImportedRows(List<Invoice> importedRows) {
		return importedRows.stream().filter(Invoice::isImportExists)
				.filter(invoice -> invoice.getInvoiceNo() != null && !invoice.getInvoiceNo().isBlank())
				.collect(Collectors.groupingBy(invoice -> reference(invoice.getInvoiceNo()), LinkedHashMap::new, Collectors.toList()));
	}

	/** Exact references come first; only Symbion invoices allow an optional I before a numeric reference. */
	public static List<String> lookupReferences(ScannedInvoice scanned) {
		String original = text(scanned.invoiceNo());
		if (original.isBlank()) return List.of();
		if (!isSymbionInvoice(scanned)) return List.of(original);
		String normalized = reference(original);
		if (normalized.matches("I[0-9]+")) return List.of(original, normalized.substring(1));
		if (normalized.matches("[0-9]+")) return List.of(original, "I" + normalized);
		return List.of(original);
	}

	public static List<Invoice> matchingImportedRows(ScannedInvoice scanned, Map<String, List<Invoice>> byReference) {
		for (String candidateReference : lookupReferences(scanned)) {
			List<Invoice> candidates = byReference.getOrDefault(reference(candidateReference), List.of());
			if (isSymbionInvoice(scanned)) {
				List<Invoice> named = candidates.stream().filter(invoice -> isSymbionSupplier(invoice.getSupplierName())).toList();
				candidates = named.isEmpty()
						? candidates.stream().filter(invoice -> text(invoice.getSupplierName()).isBlank()).toList() : named;
			} else if (candidates.size() > 1 && !scanned.supplierName().isBlank()) {
				List<Invoice> named = candidates.stream()
						.filter(invoice -> supplier(invoice.getSupplierName()).equals(supplier(scanned.supplierName()))).toList();
				if (!named.isEmpty()) candidates = named;
			}
			if (!candidates.isEmpty()) return candidates;
		}
		return List.of();
	}

	/** Keep the matched internal reference when entering a Symbion invoice; retain OCR text on the scanned row. */
	public static String entryReference(InvoiceReconciliation row) {
		ScannedInvoice scanned = row.getScanned();
		if (scanned == null) return row.getInvoiceNo();
		Invoice imported = row.getImported();
		if (isSymbionInvoice(scanned) && imported != null && !text(imported.getInvoiceNo()).isBlank()
				&& (text(imported.getSupplierName()).isBlank() || isSymbionSupplier(imported.getSupplierName()))
				&& lookupReferences(scanned).stream().anyMatch(value -> reference(value).equals(reference(imported.getInvoiceNo())))) {
			return imported.getInvoiceNo();
		}
		return scanned.invoiceNo();
	}

	private static boolean isSymbionInvoice(ScannedInvoice scanned) {
		return scanned != null && scanned.documentType() == ScannedInvoice.DocumentType.INVOICE && isSymbionSupplier(scanned.supplierName());
	}

	private static boolean isSymbionSupplier(String name) {
		return List.of(supplier(name).split(" ")).contains("SYMBION");
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
