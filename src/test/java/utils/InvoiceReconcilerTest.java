package utils;

import models.Invoice;
import models.InvoiceReconciliation;
import models.ScannedInvoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceReconcilerTest {
	private static final LocalDate DATE = LocalDate.of(2026, 9, 10);

	@Test
	void invoiceAndCreditWithTheSameNumberMatchOnlyTheirOwnDocumentType() {
		var invoice = scanned("Supplier", "000123", ScannedInvoice.DocumentType.INVOICE);
		var credit = scanned("Supplier", "000123", ScannedInvoice.DocumentType.CREDIT);
		var expectedInvoice = imported("Supplier", "000123", 100);
		var expectedCredit = imported("Supplier", "000123", -100);
		expectedCredit.setReconciliationType(ScannedInvoice.DocumentType.CREDIT);
		var results = InvoiceReconciler.reconcile(List.of(invoice, credit), List.of(expectedInvoice, expectedCredit));
		assertTrue(results.stream().allMatch(row -> row.getStatus() == InvoiceReconciliation.Status.MATCHED));
		assertSame(expectedInvoice, results.getFirst().getImported());
		assertSame(expectedCredit, results.getLast().getImported());
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(credit, List.of(expectedInvoice)).getStatus());
	}

	@Test
	void creditReferencesPreservePunctuationAndRequireTheCorrectSupplier() {
		var scanned = scanned("Supplier", "CR-0001", ScannedInvoice.DocumentType.CREDIT);
		var expected = imported("Supplier", "CR0001", -100); expected.setReconciliationType(ScannedInvoice.DocumentType.CREDIT);
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(scanned, List.of(expected)).getStatus());
		expected.setInvoiceNo("CR-0001"); expected.setSupplierName("Another Supplier");
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(scanned, List.of(expected)).getStatus());
		expected.setSupplierName("Supplier Pty Ltd");
		assertEquals(InvoiceReconciliation.Status.MATCHED, reconcile(scanned, List.of(expected)).getStatus());
		assertEquals(InvoiceReconciliation.Status.AMBIGUOUS, reconcile(scanned, List.of(expected, expected)).getStatus());
	}

	@Test
	void aRealZeroCreditMatchesAndCreditVarianceKeepsItsSign() {
		var credit = new ScannedInvoice("credit.pdf", "Supplier", "ZERO", DATE, null, ScannedInvoice.DocumentType.CREDIT, 0, 0.99);
		var zero = imported("Supplier", "ZERO", 0); zero.setReconciliationType(ScannedInvoice.DocumentType.CREDIT);
		assertEquals(InvoiceReconciliation.Status.MATCHED, reconcile(credit, List.of(zero)).getStatus());
		zero.setImportedInvoiceAmount(-25);
		assertEquals("$25.00", reconcile(credit, List.of(zero)).getVarianceString());
	}

	@Test
	void staleCreditAmountsCannotAutomaticallyAcceptRows() {
		var credit = scanned("Supplier", "CR-1", ScannedInvoice.DocumentType.CREDIT);
		var expected = imported("Supplier", "CR-1", -100);
		expected.setReconciliationType(ScannedInvoice.DocumentType.CREDIT); expected.setReconciliationStale(true);
		var row = reconcile(credit, List.of(expected));
		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW, row.getStatus());
		assertFalse(row.isAccepted()); assertTrue(row.hasExpectedAmount());
		expected.setReconciliationStale(false);
		assertEquals(InvoiceReconciliation.Status.MATCHED, reconcile(credit, List.of(expected)).getStatus());
	}

	@ParameterizedTest
	@CsvSource({"I222534680,222534680", "222534680,I222534680", "i000123,000123", "000123,I000123"})
	void matchesSymbionInvoicesInEitherFormatAndKeepsTheInternalEntryReference(String scannedReference, String storedReference) {
		ScannedInvoice scanned = scanned("Symbion Pty Ltd", scannedReference, ScannedInvoice.DocumentType.INVOICE);
		Invoice imported = imported("SYMBION", storedReference, 100);
		InvoiceReconciliation row = reconcile(scanned, List.of(imported));
		assertEquals(InvoiceReconciliation.Status.MATCHED, row.getStatus());
		assertSame(imported, row.getImported());
		assertEquals(scannedReference, row.getScanned().invoiceNo());
		assertEquals(storedReference, InvoiceReconciler.entryReference(row));
	}

	@Test
	void exactReferenceWinsWithoutChoosingAnAlternateByAmount() {
		ScannedInvoice scanned = scanned("Symbion", "I123", ScannedInvoice.DocumentType.INVOICE);
		Invoice exact = imported("Symbion", "I123", 200);
		Invoice alternate = imported("Symbion", "123", 100);
		InvoiceReconciliation row = reconcile(scanned, List.of(alternate, exact));
		assertSame(exact, row.getImported());
		assertEquals(InvoiceReconciliation.Status.AMOUNT_MISMATCH, row.getStatus());
	}

	@Test
	void wrongSupplierDoesNotHideTheSymbionMatchAndAmbiguousMatchesRequireReview() {
		ScannedInvoice scanned = scanned("Symbion", "I123", ScannedInvoice.DocumentType.INVOICE);
		Invoice other = imported("Sigma", "I123", 100);
		Invoice symbion = imported("Symbion Healthcare Pty Ltd", "123", 100);
		assertSame(symbion, reconcile(scanned, List.of(other, symbion)).getImported());
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(scanned,
				List.of(imported("Sigma", "123", 100))).getStatus());
		Invoice duplicate = imported("Symbion", "123", 100);
		assertEquals(InvoiceReconciliation.Status.AMBIGUOUS, reconcile(scanned, List.of(symbion, duplicate)).getStatus());
	}

	@Test
	void amountOnlyImportsRemainUsableAndNamedSupplierMatchesTakePrecedence() {
		ScannedInvoice scanned = scanned("Symbion", "I123", ScannedInvoice.DocumentType.INVOICE);
		Invoice snapshot = imported("", "123", 100);
		assertSame(snapshot, reconcile(scanned, List.of(snapshot)).getImported());
		Invoice named = imported("Symbion Pty Ltd", "123", 100);
		assertSame(named, reconcile(scanned, List.of(snapshot, named)).getImported());
	}

	@Test
	void leadingZerosOtherPrefixesOtherSuppliersAndCreditsRemainDistinct() {
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(
				scanned("Symbion", "I000123", ScannedInvoice.DocumentType.INVOICE),
				List.of(imported("Symbion", "123", 100))).getStatus());
		for (String reference : List.of("C123", "A123", "II123", "INV123")) {
			assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(
					scanned("Symbion", reference, ScannedInvoice.DocumentType.INVOICE),
					List.of(imported("Symbion", "123", 100))).getStatus());
		}
		assertEquals(List.of("I123"), InvoiceReconciler.lookupReferences(scanned("Sigma", "I123", ScannedInvoice.DocumentType.INVOICE)));
		assertEquals(List.of("I123"), InvoiceReconciler.lookupReferences(scanned("", "I123", ScannedInvoice.DocumentType.INVOICE)));
		assertEquals(List.of("I123"), InvoiceReconciler.lookupReferences(scanned("Symbion", "I123", ScannedInvoice.DocumentType.CREDIT)));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(doubles = {0, 0.5, 0.89})
	void uncertainZeroAmountsMatchWhenZOfficeConfirmsZeroAndIdentityIsConfident(Double amountConfidence) {
		ScannedInvoice scanned = uncertainAmount(0, amountConfidence);
		InvoiceReconciliation row = reconcile(scanned, List.of(imported("Symbion", "123", 0)));
		assertEquals(InvoiceReconciliation.Status.MATCHED, row.getStatus());
		assertTrue(row.isAccepted());
		assertFalse(row.isNotable());
		assertEquals("$0.00", row.getVarianceString());
		assertEquals(amountConfidence, row.getAmountConfidence()); // Keep the original extraction confidence.
	}

	@ParameterizedTest
	@ValueSource(doubles = {0.01, 0.10, 0.20, 12.34, -5.00})
	void assumedZeroAmountsNeverAutoAcceptAgainstANonzeroZOfficeValue(double expected) {
		InvoiceReconciliation row = reconcile(uncertainAmount(0, 0.0), List.of(imported("Symbion", "123", expected)));
		assertEquals(InvoiceReconciliation.Status.AMOUNT_MISMATCH, row.getStatus());
		assertFalse(row.isAccepted());
		assertEquals(java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).format(-expected), row.getVarianceString());
	}

	@Test
	void assumedZerosStillRequireAUniqueRealZOfficeMatchAndReliableIdentity() {
		ScannedInvoice scanned = uncertainAmount(0, 0.0);
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(scanned, List.of()).getStatus());
		Invoice unconfirmed = imported("Symbion", "123", 0);
		unconfirmed.setImportExists(false);
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(scanned, List.of(unconfirmed)).getStatus());
		assertEquals(InvoiceReconciliation.Status.AMBIGUOUS, reconcile(scanned,
				List.of(imported("Symbion", "123", 0), imported("Symbion", "123", 0))).getStatus());
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE,
				reconcile(scanned, List.of(imported("Sigma", "123", 0))).getStatus());
		for (int field = 0; field < 4; field++) {
			for (Double uncertain : new Double[] {null, 0.79}) {
				Double[] scores = {0.99, 0.99, 0.99, 0.99};
				scores[field] = uncertain;
				ScannedInvoice row = new ScannedInvoice("statement.pdf", "Symbion", "I123", DATE, null,
						ScannedInvoice.DocumentType.INVOICE, 0,
						new ScannedInvoice.FieldConfidences(scores[0], scores[1], scores[2], null, scores[3], 0.0));
				assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW,
						reconcile(row, List.of(imported("Symbion", "123", 0))).getStatus());
			}
		}
		ScannedInvoice missingDate = new ScannedInvoice("statement.pdf", "Symbion", "I123", null, null,
				ScannedInvoice.DocumentType.INVOICE, 0, scanned.fieldConfidences());
		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW,
				reconcile(missingDate, List.of(imported("Symbion", "123", 0))).getStatus());
	}

	@Test
	void nonzeroAmountsStillNeedAmountConfidenceAndExplicitZerosKeepNormalTolerance() {
		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW,
				reconcile(uncertainAmount(12345, 0.0), List.of(imported("Symbion", "123", 123.45))).getStatus());
		assertEquals(InvoiceReconciliation.Status.WITHIN_TOLERANCE,
				reconcile(uncertainAmount(0, 0.99), List.of(imported("Symbion", "123", 0.10))).getStatus());
	}

	private static ScannedInvoice uncertainAmount(long amountCents, Double confidence) {
		return new ScannedInvoice("statement.pdf", "Symbion Pty Ltd", "I123", DATE, null,
				ScannedInvoice.DocumentType.INVOICE, amountCents,
				new ScannedInvoice.FieldConfidences(0.99, 0.99, 0.99, null, 0.99, confidence));
	}

	private static InvoiceReconciliation reconcile(ScannedInvoice scanned, List<Invoice> imported) {
		return InvoiceReconciler.reconcile(List.of(scanned), imported).getFirst();
	}

	private static ScannedInvoice scanned(String supplier, String reference, ScannedInvoice.DocumentType type) {
		return new ScannedInvoice("statement.pdf", supplier, reference, DATE, null, type, 10000, 0.99);
	}

	private static Invoice imported(String supplier, String reference, double amount) {
		Invoice invoice = new Invoice();
		invoice.setSupplierName(supplier);
		invoice.setInvoiceNo(reference);
		invoice.setImportedInvoiceAmount(amount);
		invoice.setImportExists(true);
		return invoice;
	}
}
