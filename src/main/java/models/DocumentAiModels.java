package models;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Client-side contracts for Alpha API's persistent Drive document queue. */
public final class DocumentAiModels {
    private DocumentAiModels() {}

    public enum FolderKind { INVOICE, STATEMENT }
    public enum BatchKind { INVOICE_BATCH, STATEMENT }
    public enum BatchStatus { QUEUED, SCANNING, REVIEW_REQUIRED, FAILED, COMPLETED, DISMISSED }

    public record DriveStatus(boolean configured, boolean connected, String account, boolean scannerConfigured) {}
    public record AuthorizationResponse(String authorizationUrl) {}
    public record DriveFolder(String id, String name) {
        @Override public String toString() { return name; }
    }
    public record WatchFolder(long id, String folderId, String folderName, FolderKind kind) {
        @Override public String toString() {
            return (kind == FolderKind.INVOICE ? "Invoices · " : "Statements · ") + folderName;
        }
    }
    public record WatchFolderInput(String folderId, String folderName, FolderKind kind) {}
    public record ScanSchedule(boolean enabled, int intervalMinutes, Instant nextRunAt,
            Instant lastRunAt, boolean running, String lastError) {}
    public record ScanScheduleInput(boolean enabled, int intervalMinutes) {}
    /** A reviewed invoice document and the date used for its Drive filing month. */
    public record EnteredDocument(long documentId, LocalDate invoiceDate, List<ScannedInvoice> rows) {
        public EnteredDocument(long documentId, LocalDate invoiceDate) { this(documentId, invoiceDate, List.of()); }
        public EnteredDocument { rows = rows == null ? List.of() : List.copyOf(rows); }
    }
    public record StatementInvoice(long documentId, int rowIndex, ScannedInvoice values, String notes) {
        public StatementInvoice { notes = notes == null ? "" : notes; }
    }
    public record StatementInvoiceEdit(StatementInvoice original, LocalDate invoiceDate,
            LocalDate dueDate, Long amountCents, String notes) {}
    public record StatementCompletion(List<StatementInvoiceEdit> edits) {
        public StatementCompletion { edits = edits == null ? List.of() : List.copyOf(edits); }
    }
    public record StatementComparison(List<ScannedInvoice> enteredRows, int pendingDocuments, List<StatementInvoice> enteredInvoices) {
        public StatementComparison(List<ScannedInvoice> enteredRows, int pendingDocuments) { this(enteredRows, pendingDocuments, List.of()); }
        public StatementComparison { enteredInvoices = enteredInvoices == null ? List.of() : List.copyOf(enteredInvoices); }
    }
    public record BatchSummary(long id, BatchKind kind, BatchStatus status, String sourceLabel,
            int documentCount, int transactionCount, Instant createdAt, String error) {
        public String getSourceLabel() { return sourceLabel; }
        public String getKindLabel() { return kind == BatchKind.STATEMENT ? "Statement" : "Invoice batch"; }
        public String getStatusLabel() {
            return switch (status) {
                case QUEUED -> "Queued";
                case SCANNING -> "Scanning";
                case REVIEW_REQUIRED -> "Review required";
                case FAILED -> "Failed";
                case COMPLETED -> "Completed";
                case DISMISSED -> "Dismissed";
            };
        }
        public String getCountLabel() {
            if (status == BatchStatus.QUEUED || status == BatchStatus.SCANNING) return documentCount + " PDF(s)";
            return transactionCount + " transaction(s)";
        }
        public String getStatusDetail() {
            return switch (status) {
                case QUEUED -> "Waiting for an available scanner";
                case SCANNING -> "Gemini extraction is in progress";
                case REVIEW_REQUIRED -> error == null || error.isBlank() ? "Ready to open and review" : error;
                case FAILED -> error == null || error.isBlank() ? "Scan failed; retry is available" : error;
                case COMPLETED -> "Accepted and saved";
                case DISMISSED -> "Dismissed";
            };
        }
        public String getCreatedLabel() {
            return createdAt == null ? "—" : DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm")
                    .withZone(ZoneId.systemDefault()).format(createdAt);
        }
    }
    public record ExtractedDocument(long id, String fileName, List<ScannedInvoice> rows,
            List<Map<PdfEvidenceField, PdfEvidenceLocation>> evidenceByRow,
            LocalDate periodStart, LocalDate periodEnd, Long statementAmountCents,
            Double statementAmountConfidence, String error, boolean entered) {
        public ExtractedDocument(long id, String fileName, List<ScannedInvoice> rows,
                List<Map<PdfEvidenceField, PdfEvidenceLocation>> evidenceByRow,
                LocalDate periodStart, LocalDate periodEnd, Long statementAmountCents,
                Double statementAmountConfidence, String error) {
            this(id, fileName, rows, evidenceByRow, periodStart, periodEnd, statementAmountCents,
                    statementAmountConfidence, error, false);
        }
    }
    public record BatchDetail(BatchSummary batch, List<ExtractedDocument> documents) {}
}
