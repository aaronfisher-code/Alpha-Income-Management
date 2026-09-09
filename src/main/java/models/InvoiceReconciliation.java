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

	public InvoiceReconciliation(Status status, ScannedInvoice scanned, Invoice imported, Long varianceCents) {
		this.status = status;
		this.scanned = scanned;
		this.imported = imported;
		this.varianceCents = varianceCents;
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

	public Invoice getImported() {
		return imported;
	}

	public String getSupplierName() {
		if (scanned != null && !scanned.supplierName().isBlank()) return scanned.supplierName();
		return imported == null ? "" : imported.getSupplierName();
	}

	public String getInvoiceNo() {
		if (scanned != null && !scanned.invoiceNo().isBlank()) return scanned.invoiceNo();
		return imported == null ? "" : imported.getInvoiceNo();
	}

	public String getDocumentType() {
		return scanned == null ? "Invoice" : title(scanned.documentType().name());
	}

	public String getScannedDateString() {
		return scanned == null || scanned.invoiceDate() == null ? "—" : DATE.format(scanned.invoiceDate());
	}

	public String getSourceFile() {
		return scanned == null ? "Z-Office import" : scanned.sourceFile();
	}

	public String getScannedAmountString() {
		return scanned == null ? "—" : CURRENCY.format(scanned.amount());
	}

	public String getImportedAmountString() {
		return imported == null ? "—" : CURRENCY.format(imported.getImportedInvoiceAmount());
	}

	public String getVarianceString() {
		return varianceCents == null ? "—" : CURRENCY.format(varianceCents / 100.0);
	}

	public InvoiceReconciliation withStatus(Status newStatus) {
		return new InvoiceReconciliation(newStatus, scanned, imported, varianceCents);
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
		return scanned == null ? null : scanned.fieldConfidences().supplier();
	}

	public Double getReferenceConfidence() {
		return scanned == null ? null : scanned.fieldConfidences().reference();
	}

	public Double getDateConfidence() {
		return scanned == null ? null : scanned.fieldConfidences().date();
	}

	public Double getDueDateConfidence() {
		return scanned == null ? null : scanned.fieldConfidences().dueDate();
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
