package models;

import java.time.LocalDate;

/**
 * A single invoice or credit extracted from a PDF by the configured OCR model.
 * Amounts are represented as signed cents so reconciliation is not affected by
 * floating-point rounding.
 */
public record ScannedInvoice(
		String sourceFile,
		String supplierName,
		String invoiceNo,
		LocalDate invoiceDate,
		LocalDate dueDate,
		DocumentType documentType,
		long amountCents,
		FieldConfidences fieldConfidences) {

	public enum DocumentType {
		INVOICE,
		CREDIT
	}

	/** Gemini's confidence for each independently extracted value. */
	public record FieldConfidences(
			Double supplier,
			Double reference,
			Double date,
			Double dueDate,
			Double type,
			Double amount) {

		public FieldConfidences {
			supplier = validConfidence(supplier);
			reference = validConfidence(reference);
			date = validConfidence(date);
			dueDate = validConfidence(dueDate);
			type = validConfidence(type);
			amount = validConfidence(amount);
		}

		/** Lowest available confidence, retained for reconciliation compatibility. */
		public Double minimum() {
			Double result = null;
			for (Double value : new Double[] {supplier, reference, date, dueDate, type, amount}) {
				if (value != null && (result == null || value < result)) result = value;
			}
			return result;
		}

		/** Lowest confidence for fields that determine whether reconciliation needs review. */
		public Double minimumRequired() {
			Double result = null;
			for (Double value : new Double[] {supplier, reference, date, type, amount}) {
				if (value != null && (result == null || value < result)) result = value;
			}
			return result;
		}
	}

	public ScannedInvoice {
		sourceFile = valueOrEmpty(sourceFile);
		supplierName = valueOrEmpty(supplierName);
		invoiceNo = valueOrEmpty(invoiceNo);
		documentType = documentType == null ? DocumentType.INVOICE : documentType;
		fieldConfidences = fieldConfidences == null
				? new FieldConfidences(null, null, null, null, null, null) : fieldConfidences;
		if (documentType == DocumentType.INVOICE && dueDate == null && invoiceDate != null) {
			dueDate = invoiceDate.plusDays(30);
		}
		if (documentType == DocumentType.CREDIT) {
			amountCents = -Math.abs(amountCents);
		}
	}

	/** Compatibility constructor for callers that only have one legacy score. */
	public ScannedInvoice(String sourceFile, String supplierName, String invoiceNo, LocalDate invoiceDate,
			LocalDate dueDate, DocumentType documentType, long amountCents, Double confidence) {
		this(sourceFile, supplierName, invoiceNo, invoiceDate, dueDate, documentType, amountCents,
				new FieldConfidences(confidence, confidence, confidence, confidence, confidence, confidence));
	}

	/** Lowest per-field score for legacy reconciliation rules. */
	public Double confidence() {
		return fieldConfidences.minimum();
	}

	public Double requiredFieldConfidence() {
		return fieldConfidences.minimumRequired();
	}

	public double amount() {
		return amountCents / 100.0;
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value.trim();
	}

	private static Double validConfidence(Double value) {
		return value != null && Double.isFinite(value) && value >= 0 && value <= 1 ? value : null;
	}
}
