package utils;

import models.ScannedInvoice;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static models.InvoiceReconciliation.Status.*;
import static org.junit.jupiter.api.Assertions.*;

class StatementReconcilerTest {
    private static ScannedInvoice row(String supplier, String reference, long amount) {
        return new ScannedInvoice("source.pdf", supplier, reference, LocalDate.of(2026, 9, 1), null,
                amount < 0 ? ScannedInvoice.DocumentType.CREDIT : ScannedInvoice.DocumentType.INVOICE, amount, .99);
    }

    @Test void comparesEnteredAmountsAndKeepsTheVarianceDirectionAndSignedCredits() {
        var statement = List.of(row("Supplier", "1", 10000), row("Supplier", "C1", -2000));
        var entered = List.of(row("Supplier", "1", 10500), row("Supplier", "C1", -2500));
        var results = StatementReconciler.reconcile(statement, entered, 0);
        assertEquals(List.of(AMOUNT_MISMATCH, AMOUNT_MISMATCH), results.stream().map(value -> value.getStatus()).toList());
        assertEquals("$5.00", results.getFirst().getVarianceString());
        assertEquals("-$5.00", results.getLast().getVarianceString());
        assertEquals(-25, results.getLast().getImported().getImportedInvoiceAmount());
    }

    @Test void missingInvoicesNeverMatchAnotherSupplier() {
        var result = StatementReconciler.reconcile(List.of(row("Supplier A", "123", 100)),
                List.of(row("Supplier B", "123", 100)), 0).getFirst();
        assertEquals(MISSING_SCANNED_INVOICE, result.getStatus());
        assertFalse(result.isAccepted());
        assertNull(result.getImported());
    }

    @Test void incompleteSupplierQueuesWaitEvenWhenEveryKnownAmountMatches() {
        var rows = List.of(row("Supplier", "123", 100));
        var result = StatementReconciler.reconcile(rows, rows, 1).getFirst();
        assertEquals(WAITING_FOR_INVOICES, result.getStatus());
        assertTrue(result.hasExpectedAmount());
        assertFalse(result.isAccepted());
        assertEquals(MATCHED, StatementReconciler.reconcile(rows, rows, 0).getFirst().getStatus());
    }

    @Test void duplicateStatementRowsCannotConsumeTheSameInvoiceTwice() {
        var row = row("Supplier", "123", 100);
        assertTrue(StatementReconciler.reconcile(List.of(row, row), List.of(row), 0).stream()
                .allMatch(result -> result.getStatus() == AMBIGUOUS));
    }

    @Test void duplicatedEnteredDocumentsAreAmbiguousEvenIfAmountsAgree() {
        var row = row("Supplier", "123", 100);
        assertEquals(AMBIGUOUS, StatementReconciler.reconcile(List.of(row), List.of(row, row), 0).getFirst().getStatus());
    }

    @Test void symbionAliasesPreserveLeadingZeroesAndCannotBeCountedTwice() {
        var statement = row("Symbion Pty Ltd", "I00123", 100);
        var entered = row("Symbion", "00123", 100);
        assertEquals(MATCHED, StatementReconciler.reconcile(List.of(statement), List.of(entered), 0).getFirst().getStatus());
        assertEquals(MISSING_SCANNED_INVOICE, StatementReconciler.reconcile(List.of(statement),
                List.of(row("Symbion", "123", 100)), 0).getFirst().getStatus());
        assertTrue(StatementReconciler.reconcile(List.of(statement, entered), List.of(entered), 0).stream()
                .allMatch(result -> result.getStatus() == AMBIGUOUS));
    }

    @Test void ch2RiStatementRowsRecogniseSavedNumbersAndCannotBeCountedTwice() {
        var statement = row("CH2", "000123 RI", 100);
        var entered = row("CH2", "000123", 100);
        assertEquals(MATCHED, StatementReconciler.reconcile(List.of(statement), List.of(entered), 0).getFirst().getStatus());
        assertEquals(MATCHED, StatementReconciler.reconcile(List.of(entered), List.of(statement), 0).getFirst().getStatus());
        assertTrue(StatementReconciler.sameDocument(statement, entered));
        assertEquals(MISSING_SCANNED_INVOICE, StatementReconciler.reconcile(List.of(statement),
                List.of(row("CH2", "123", 100)), 0).getFirst().getStatus());
        assertTrue(StatementReconciler.reconcile(List.of(statement, entered), List.of(entered), 0).stream()
                .allMatch(result -> result.getStatus() == AMBIGUOUS));
        assertEquals(AMBIGUOUS, StatementReconciler.reconcile(List.of(statement), List.of(statement, entered), 0).getFirst().getStatus());
        assertFalse(StatementReconciler.sameDocument(row("CH2", "000123 RI", -100), row("CH2", "000123", -100)));
        assertTrue(StatementReconciler.sameDocument(row("Sigma", "000123 RI", 100), row("Sigma", "000123", 100)));
    }

    @Test void genericStatementFallbackKeepsExactReferencesAndUncertainDatesSeparate() {
        var bare = row("New Supplier", "74040054", 100);
        var prefixed = row("New Supplier", "INV-74040054", 100);
        var suffixed = row("New Supplier", "74040054 XYZ", 100);
        assertEquals(MATCHED, StatementReconciler.reconcile(List.of(bare), List.of(prefixed), 0).getFirst().getStatus());
        assertEquals(AMBIGUOUS, StatementReconciler.reconcile(List.of(bare), List.of(prefixed, suffixed), 0).getFirst().getStatus());
        assertTrue(StatementReconciler.reconcile(List.of(prefixed, suffixed), List.of(prefixed, suffixed), 0).stream()
                .allMatch(result -> result.getStatus() == MATCHED));
        var differentDate = new ScannedInvoice(bare.sourceFile(), bare.supplierName(), bare.invoiceNo(), bare.invoiceDate().plusDays(1), null,
                bare.documentType(), bare.amountCents(), .99);
        assertEquals(NEEDS_REVIEW, StatementReconciler.reconcile(List.of(differentDate), List.of(prefixed), 0).getFirst().getStatus());
        assertFalse(StatementReconciler.sameDocument(differentDate, prefixed));
    }

    @Test void invoicesAndCreditsHaveSeparateIdentities() {
        var invoice = row("Supplier", "123", 100);
        var credit = row("Supplier", "123", -100);
        assertTrue(StatementReconciler.reconcile(List.of(invoice, credit), List.of(invoice, credit), 0).stream()
                .allMatch(result -> result.getStatus() == MATCHED));
    }

    @Test void uncertainStatementAmountsAndBlankReferencesNeedReview() {
        var entered = row("Supplier", "123", 100);
        var uncertain = new ScannedInvoice("statement.pdf", "Supplier", "123", entered.invoiceDate(), null,
                entered.documentType(), 100, .5);
        assertEquals(NEEDS_REVIEW, StatementReconciler.reconcile(List.of(uncertain), List.of(entered), 0).getFirst().getStatus());
        assertEquals(NEEDS_REVIEW, StatementReconciler.reconcile(List.of(row("Supplier", "", 100)), List.of(entered), 0).getFirst().getStatus());
    }
}
