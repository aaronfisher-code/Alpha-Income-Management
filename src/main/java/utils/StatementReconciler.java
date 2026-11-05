package utils;

import models.Invoice;
import models.InvoiceReconciliation;
import models.ScannedInvoice;
import java.util.*;
import static models.InvoiceReconciliation.Status.*;

/** Compares statement rows with saved Alpha invoice/credit values; PDF provenance is optional. */
public final class StatementReconciler {
    private StatementReconciler() {}

    public static List<InvoiceReconciliation> reconcile(List<ScannedInvoice> statement,
            List<ScannedInvoice> entered, int pendingDocuments) {
        return reconcile(statement, entered, pendingDocuments, Map.of());
    }

    /** Choices narrow eligible candidates; they never permit one saved entry to be used twice. */
    public static List<InvoiceReconciliation> reconcile(List<ScannedInvoice> statement,
            List<ScannedInvoice> entered, int pendingDocuments, Map<Integer, ScannedInvoice> selections) {
        Map<String, List<ScannedInvoice>> entries = new HashMap<>();
        entered.forEach(row -> entries.computeIfAbsent(key(row), ignored -> new ArrayList<>()).add(row));
        Map<String, List<ScannedInvoice>> byNumber = new HashMap<>();
        entered.stream().filter(row -> row.documentType() == ScannedInvoice.DocumentType.INVOICE)
                .filter(row -> !InvoiceReconciler.numericReference(row.invoiceNo()).isBlank())
                .forEach(row -> byNumber.computeIfAbsent(numberKey(row), ignored -> new ArrayList<>()).add(row));
        Map<String, Long> occurrences = statement.stream().collect(java.util.stream.Collectors.groupingBy(
                StatementReconciler::key, java.util.stream.Collectors.counting()));
        List<List<ScannedInvoice>> matches = new ArrayList<>();
        for (int index = 0; index < statement.size(); index++) {
            var options = candidates(statement.get(index), entries, byNumber);
            var selection = selections.get(index);
            matches.add(selection == null ? options : options.stream()
                    .filter(option -> entryKey(option).equals(entryKey(selection))).toList());
        }
        Map<String, Integer> uses = new HashMap<>();
        matches.stream().filter(rows -> rows.size() == 1).forEach(rows -> uses.merge(entryKey(rows.getFirst()), 1, Integer::sum));
        List<InvoiceReconciliation> results = new ArrayList<>();
        for (int index = 0; index < statement.size(); index++) {
            ScannedInvoice row = statement.get(index);
            var candidates = matches.get(index);
            Invoice expected = null;
            Long variance = null;
            InvoiceReconciliation.Status status;
            if (row.supplierName().isBlank() || row.invoiceNo().isBlank()) status = NEEDS_REVIEW;
            else if ((!selections.containsKey(index) && occurrences.get(key(row)) > 1) || candidates.size() > 1
                    || candidates.size() == 1 && uses.get(entryKey(candidates.getFirst())) > 1) status = AMBIGUOUS;
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
                        && confident(fields.type()) && confident(fields.amount())
                        && (key(row).equals(key(invoice)) || sameDocument(row, invoice));
                status = !confident ? NEEDS_REVIEW : variance == 0 ? MATCHED
                        : Math.abs(variance) <= InvoiceReconciler.DEFAULT_TOLERANCE_CENTS ? WITHIN_TOLERANCE : AMOUNT_MISMATCH;
            }
            if (pendingDocuments > 0) status = WAITING_FOR_INVOICES;
            results.add(new InvoiceReconciliation(status, row, expected, variance));
        }
        return List.copyOf(results);
    }

    /** Exact saved reference identity, keeping punctuation variants separate for explicit selection. */
    public static String entryKey(ScannedInvoice row) {
        return InvoiceReconciler.supplier(row.supplierName()) + ":" + row.documentType() + ":" + row.invoiceNo().toUpperCase(Locale.ROOT);
    }

    public static List<ScannedInvoice> candidates(ScannedInvoice row, List<ScannedInvoice> entered) {
        Map<String, List<ScannedInvoice>> exact = new HashMap<>(), numbers = new HashMap<>();
        for (var invoice : entered) {
            exact.computeIfAbsent(key(invoice), ignored -> new ArrayList<>()).add(invoice);
            if (invoice.documentType() == ScannedInvoice.DocumentType.INVOICE
                    && !InvoiceReconciler.numericReference(invoice.invoiceNo()).isBlank()) {
                numbers.computeIfAbsent(numberKey(invoice), ignored -> new ArrayList<>()).add(invoice);
            }
        }
        return candidates(row, exact, numbers);
    }

    private static boolean confident(Double value) {
        return value != null && value >= InvoiceReconciler.REVIEW_CONFIDENCE;
    }

    public static boolean sameDocument(ScannedInvoice left, ScannedInvoice right) {
        if (key(left).equals(key(right))) return true;
        return left.documentType() == ScannedInvoice.DocumentType.INVOICE && right.documentType() == left.documentType()
                && InvoiceReconciler.supplier(left.supplierName()).equals(InvoiceReconciler.supplier(right.supplierName()))
                && InvoiceReconciler.alternativeReferences(left.invoiceNo(), right.invoiceNo())
                && left.invoiceDate() != null && left.invoiceDate().equals(right.invoiceDate());
    }

    private static List<ScannedInvoice> candidates(ScannedInvoice row, Map<String, List<ScannedInvoice>> exact,
            Map<String, List<ScannedInvoice>> byNumber) {
        var matches = exact.getOrDefault(key(row), List.of());
        if (row.documentType() != ScannedInvoice.DocumentType.INVOICE
                || InvoiceReconciler.numericReference(row.invoiceNo()).isBlank()) return matches;
        var alternatives = byNumber.getOrDefault(numberKey(row), List.of()).stream()
                .filter(invoice -> !key(row).equals(key(invoice)) && InvoiceReconciler.alternativeReferences(row.invoiceNo(), invoice.invoiceNo()))
                .filter(invoice -> matches.isEmpty() || sameDocument(row, invoice)).toList();
        return java.util.stream.Stream.concat(matches.stream(), alternatives.stream()).toList();
    }

    private static String numberKey(ScannedInvoice row) {
        return InvoiceReconciler.supplier(row.supplierName()) + ":" + InvoiceReconciler.numericReference(row.invoiceNo());
    }

    static String key(ScannedInvoice row) {
        return InvoiceReconciler.supplier(row.supplierName()) + ":" + InvoiceReconciler.lookupKey(row, row.invoiceNo());
    }
}
