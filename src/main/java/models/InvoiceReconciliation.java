package models;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A review row comparing OCR evidence with the imported Z-Office amount. */
public final class InvoiceReconciliation {
	public enum Status {
		SAVED("Saved"),
		ACCEPTED("Accepted"),
		MATCHED("Matched"),
		WITHIN_TOLERANCE("Within tolerance"),
		AMOUNT_MISMATCH("Amount mismatch"),
		NOT_IN_Z_OFFICE("Not in Z-Office"),
		MISSING_SCANNED_INVOICE("Missing entered invoice"),
		WAITING_FOR_INVOICES("Waiting for invoices"),
		AMBIGUOUS("Ambiguous reference"),
		NEEDS_REVIEW("Needs review");

		private final String label;

		Status(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}

	private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

	private final Status status;
	private final ScannedInvoice scanned;
	private final Invoice imported;
	private final Long varianceCents;
	private final ScannedInvoice reviewed;

	public InvoiceReconciliation(Status status, ScannedInvoice scanned, Invoice imported, Long varianceCents) {
		this(status, scanned, imported, varianceCents, null);
	}

	private InvoiceReconciliation(Status status, ScannedInvoice scanned, Invoice imported, Long varianceCents,
			ScannedInvoice reviewed) {
		this.status = status;
		this.scanned = scanned;
		this.imported = imported;
		this.varianceCents = varianceCents;
		this.reviewed = reviewed;
	}

	public Status getStatus() {
		return status;
	}

	public String getStatusLabel() {
		return status.label();
	}

	public ScannedInvoice getScanned() {
		return scanned;
	}

	/** Editable values for display; getScanned() retains the immutable source/evidence identity. */
	public ScannedInvoice getReviewValues() { return reviewed == null ? scanned : reviewed; }

	public InvoiceReconciliation withReviewValues(ScannedInvoice values) {
		Long variance = hasExpectedAmount() ? values.amountCents() - Math.round(imported.getImportedInvoiceAmount() * 100) : null;
		return new InvoiceReconciliation(status, scanned, imported, variance, values);
	}

	public Invoice getImported() {
		return imported;
	}

	public String getSupplierName() {
		if (getReviewValues() != null && !getReviewValues().supplierName().isBlank()) return getReviewValues().supplierName();
		return imported == null ? "" : imported.getSupplierName();
	}

	public String getInvoiceNo() {
		if (getReviewValues() != null && !getReviewValues().invoiceNo().isBlank()) return getReviewValues().invoiceNo();
		return imported == null ? "" : imported.getInvoiceNo();
	}

	public String getDocumentType() {
		return getReviewValues() == null ? "Invoice" : title(getReviewValues().documentType().name());
	}

	public String getScannedDateString() {
		return getReviewValues() == null || getReviewValues().invoiceDate() == null ? "—" : DATE.format(getReviewValues().invoiceDate());
	}

	public String getSourceFile() {
		return scanned == null ? "Z-Office import" : scanned.sourceFile();
	}

	public String getScannedAmountString() {
		return getReviewValues() == null ? "—" : CURRENCY.format(getReviewValues().amount());
	}

	public String getImportedAmountString() {
		return imported == null ? "—" : CURRENCY.format(imported.getImportedInvoiceAmount());
	}

	public String getVarianceString() {
		return varianceCents == null ? "—" : CURRENCY.format(varianceCents / 100.0);
	}

	public InvoiceReconciliation withStatus(Status newStatus) {
		return new InvoiceReconciliation(newStatus, scanned, imported, varianceCents, reviewed);
	}

	public boolean isAccepted() {
		return status == Status.SAVED
				|| status == Status.ACCEPTED
				|| status == Status.MATCHED
				|| status == Status.WITHIN_TOLERANCE;
	}

	public String getConfidenceString() {
		if (scanned == null || scanned.confidence() == null) return "—";
		return Math.round(scanned.confidence() * 100) + "%";
	}

	public Double getSupplierConfidence() {
		return getReviewValues() == null ? null : getReviewValues().fieldConfidences().supplier();
	}

	public Double getReferenceConfidence() {
		return getReviewValues() == null ? null : getReviewValues().fieldConfidences().reference();
	}

	public Double getDateConfidence() {
		return getReviewValues() == null ? null : getReviewValues().fieldConfidences().date();
	}

	public Double getDueDateConfidence() {
		return getReviewValues() == null ? null : getReviewValues().fieldConfidences().dueDate();
	}

	public boolean isDueDateEstimated() {
		ScannedInvoice values = getReviewValues();
		if (values == null || values.documentType() != ScannedInvoice.DocumentType.INVOICE
				|| values.invoiceDate() == null || values.dueDate() == null) return false;
		return values.dueDateEstimated() || values.fieldConfidences().dueDate() == null
				&& values.dueDate().equals(values.invoiceDate().plusDays(30));
	}

	public boolean hasExpectedAmount() {
		return imported != null && imported.isImportExists();
	}

	public Double getTypeConfidence() {
		return scanned == null ? null : scanned.fieldConfidences().type();
	}

	public Double getAmountConfidence() {
		return scanned == null ? null : scanned.fieldConfidences().amount();
	}

	public boolean isNotable() {
		return !isAccepted();
	}

	private static String title(String value) {
		return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
	}
}
