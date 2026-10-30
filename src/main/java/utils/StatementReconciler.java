package utils;

import models.Invoice;
import models.InvoiceReconciliation;
import models.ScannedInvoice;
import java.util.*;
import static models.InvoiceReconciliation.Status.*;

/** Compares statement rows only with values entered from independent invoice/credit PDFs. */
public final class StatementReconciler {
    private StatementReconciler() {}

    public static List<InvoiceReconciliation> reconcile(List<ScannedInvoice> statement,
            List<ScannedInvoice> entered, int pendingDocuments) {
        Map<String, List<ScannedInvoice>> entries = new HashMap<>();
        entered.forEach(row -> entries.computeIfAbsent(key(row), ignored -> new ArrayList<>()).add(row));
        Map<String, Long> occurrences = statement.stream().collect(java.util.stream.Collectors.groupingBy(
                StatementReconciler::key, java.util.stream.Collectors.counting()));
        List<InvoiceReconciliation> results = new ArrayList<>();
        for (ScannedInvoice row : statement) {
            var candidates = entries.getOrDefault(key(row), List.of());
            Invoice expected = null;
            Long variance = null;
            InvoiceReconciliation.Status status;
            if (row.supplierName().isBlank() || row.invoiceNo().isBlank()) status = NEEDS_REVIEW;
            else if (occurrences.get(key(row)) > 1 || candidates.size() > 1) status = AMBIGUOUS;
            else if (candidates.isEmpty()) status = MISSING_SCANNED_INVOICE;
            else {
                ScannedInvoice invoice = candidates.getFirst();
                expected = new Invoice();
                expected.setSupplierName(invoice.supplierName());
                expected.setInvoiceNo(invoice.invoiceNo());
                expected.setInvoiceDate(invoice.invoiceDate());
                expected.setReconciliationType(invoice.documentType());
                expected.setImportedInvoiceAmount(invoice.amount());
                expected.setImportExists(true);
                variance = invoice.amountCents() - row.amountCents();
                var fields = row.fieldConfidences();
                boolean confident = row.invoiceDate() != null && confident(fields.supplier())
                        && confident(fields.reference()) && confident(fields.date())
                        && confident(fields.type()) && confident(fields.amount());
                status = !confident ? NEEDS_REVIEW : variance == 0 ? MATCHED
                        : Math.abs(variance) <= InvoiceReconciler.DEFAULT_TOLERANCE_CENTS ? WITHIN_TOLERANCE : AMOUNT_MISMATCH;
            }
            if (pendingDocuments > 0) status = WAITING_FOR_INVOICES;
            results.add(new InvoiceReconciliation(status, row, expected, variance));
        }
        return List.copyOf(results);
    }

    private static boolean confident(Double value) {
        return value != null && value >= InvoiceReconciler.REVIEW_CONFIDENCE;
    }

    public static boolean sameDocument(ScannedInvoice left, ScannedInvoice right) {
        return key(left).equals(key(right));
    }

    static String key(ScannedInvoice row) {
        String reference = InvoiceReconciler.lookupKey(row, row.invoiceNo());
        if (row.documentType() == ScannedInvoice.DocumentType.INVOICE
                && List.of(InvoiceReconciler.supplier(row.supplierName()).split(" ")).contains("SYMBION")) {
            reference = "INVOICE:" + InvoiceReconciler.reference(row.invoiceNo()).replaceFirst("^I(?=[0-9]+$)", "");
        }
        if (row.documentType() == ScannedInvoice.DocumentType.INVOICE && SupplierMatcher.isCh2Supplier(row.supplierName())) {
            reference = "INVOICE:" + InvoiceReconciler.reference(row.invoiceNo()).replaceFirst("^([0-9]+)RI$", "$1");
        }
        return InvoiceReconciler.supplier(row.supplierName()) + ":" + reference;
    }
}
