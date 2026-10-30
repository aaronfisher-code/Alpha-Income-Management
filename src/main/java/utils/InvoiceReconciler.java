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
import java.util.regex.Pattern;

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

	private static final Pattern NUMBER_WITH_AFFIXES = Pattern.compile("^[A-Za-z\\s\\p{Punct}]*([0-9]+)[A-Za-z\\s\\p{Punct}]*$");

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
				if (Boolean.TRUE.equals(imported.getReconciliationStale())) {
					status = NEEDS_REVIEW;
				} else if (scanned.amountCents() == 0 && !confident(scanned.fieldConfidences().amount())) {
					// Missing/uncertain amounts may stand for zero, but require a reliable
					// identity and an actual Z-Office zero before automatic acceptance.
					status = !confidentIdentity(scanned) ? NEEDS_REVIEW : importedCents == 0 ? MATCHED : AMOUNT_MISMATCH;
				} else if (scanned.supplierName().isBlank() || scanned.invoiceDate() == null || scanned.requiredFieldConfidence() == null
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
		Map<String, List<Invoice>> index = new LinkedHashMap<>();
		for (Invoice invoice : importedRows) {
			if (!invoice.isImportExists() || text(invoice.getInvoiceNo()).isBlank()) continue;
			index.computeIfAbsent(importKey(invoice), ignored -> new ArrayList<>()).add(invoice);
			if (invoice.getReconciliationType() != ScannedInvoice.DocumentType.CREDIT) {
				String number = numericReference(invoice.getInvoiceNo());
				if (!number.isBlank()) index.computeIfAbsent("INVOICE_NUMBER:" + number, ignored -> new ArrayList<>()).add(invoice);
			}
		}
		return index;
	}

	public static void removeLookupRows(Map<String, List<Invoice>> rows, List<ScannedInvoice> requested) {
		for (ScannedInvoice scanned : requested) {
			rows.entrySet().removeIf(entry -> entry.getKey().equals(lookupKey(scanned, scanned.invoiceNo()))
					|| scanned.documentType() == ScannedInvoice.DocumentType.INVOICE && entry.getValue().stream()
					.anyMatch(invoice -> invoice.getReconciliationType() != ScannedInvoice.DocumentType.CREDIT
							&& alternativeReferences(scanned.invoiceNo(), invoice.getInvoiceNo())));
		}
	}

	public static String importKey(Invoice invoice) {
		boolean credit = invoice.getReconciliationType() == ScannedInvoice.DocumentType.CREDIT;
		return (credit ? "CREDIT:" : "INVOICE:") + (credit ? text(invoice.getInvoiceNo()).toUpperCase(Locale.ROOT) : reference(invoice.getInvoiceNo()));
	}

	public static String lookupKey(ScannedInvoice row, String reference) {
		return row.documentType() + ":" + (row.documentType() == ScannedInvoice.DocumentType.CREDIT
				? text(reference).toUpperCase(Locale.ROOT) : reference(reference));
	}

	/** Probe the printed reference and its numeric part, without guessing a supplier's affixes. */
	public static List<String> lookupReferences(ScannedInvoice scanned) {
		String original = text(scanned.invoiceNo());
		if (original.isBlank()) return List.of();
		String number = numericReference(original);
		return scanned.documentType() == ScannedInvoice.DocumentType.INVOICE && !number.isBlank()
				&& !number.equals(reference(original)) ? List.of(original, number) : List.of(original);
	}

	public static List<Invoice> matchingImportedRows(ScannedInvoice scanned, Map<String, List<Invoice>> byReference) {
		List<Invoice> exact = supplierCandidates(scanned,
				byReference.getOrDefault(lookupKey(scanned, scanned.invoiceNo()), List.of()));
		if (!exact.isEmpty() || scanned.documentType() == ScannedInvoice.DocumentType.CREDIT) return exact;
		String number = numericReference(scanned.invoiceNo());
		if (number.isBlank()) return List.of();
		return supplierCandidates(scanned, byReference.getOrDefault("INVOICE_NUMBER:" + number, List.of()).stream()
				.filter(invoice -> alternativeReferences(scanned.invoiceNo(), invoice.getInvoiceNo())).toList());
	}

	private static List<Invoice> supplierCandidates(ScannedInvoice scanned, List<Invoice> candidates) {
		if (scanned.supplierName().isBlank()) return candidates.stream().filter(invoice -> text(invoice.getSupplierName()).isBlank()).toList();
		if (scanned.documentType() == ScannedInvoice.DocumentType.CREDIT) return candidates.stream()
				.filter(invoice -> supplier(scanned.supplierName()).equals(supplier(invoice.getSupplierName()))).toList();
		List<Invoice> named = candidates.stream().filter(invoice -> SupplierMatcher.sameSupplier(scanned.supplierName(), invoice.getSupplierName())).toList();
		if (!named.isEmpty()) return named;
		return candidates.stream().filter(invoice -> text(invoice.getSupplierName()).isBlank()).toList();
	}

	/** A single uninterrupted number, with optional text outside it. Never merge digit groups or drop zeros. */
	public static String numericReference(String value) {
		var matcher = NUMBER_WITH_AFFIXES.matcher(text(value));
		return matcher.matches() ? matcher.group(1) : "";
	}

	/** At least one side must be the bare number; A123 and B123 remain distinct references. */
	public static boolean alternativeReferences(String left, String right) {
		String number = numericReference(left);
		return !number.isBlank() && number.equals(numericReference(right))
				&& (reference(left).equals(number) || reference(right).equals(number));
	}

	public static boolean isReferenceFallback(ScannedInvoice scanned, Invoice imported) {
		return scanned.documentType() == ScannedInvoice.DocumentType.INVOICE
				&& !reference(scanned.invoiceNo()).equals(reference(imported.getInvoiceNo()));
	}

	/** Retain the internal reference of a unique comparison; OCR evidence remains unchanged. */
	public static String entryReference(InvoiceReconciliation row) {
		ScannedInvoice scanned = row.getScanned();
		if (scanned == null) return row.getInvoiceNo();
		Invoice imported = row.getImported();
		if (scanned.documentType() == ScannedInvoice.DocumentType.INVOICE && imported != null
				&& !text(imported.getInvoiceNo()).isBlank()
				&& (reference(scanned.invoiceNo()).equals(reference(imported.getInvoiceNo()))
						|| alternativeReferences(scanned.invoiceNo(), imported.getInvoiceNo()))) return imported.getInvoiceNo();
		return scanned.invoiceNo();
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
