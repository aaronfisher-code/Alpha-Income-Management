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

	@ParameterizedTest
	@CsvSource({"CH2,74040054 RI,74040054", "CH2,74040054RI,74040054", "CH2,74040054 ri,74040054",
			"ParagonCare,74040054 RI,74040054", "Paragon Care Pty Ltd,74040054,74040054 RI",
			"Clifford Hallam Healthcare Eastern Creek NSW,000123RI,000123", "CH2,000123,000123RI"})
	void ch2ReferencesMatchWithOrWithoutRiAndRetainTheInternalReference(String supplier, String printed, String stored) {
		var scanned = scanned(supplier, printed, ScannedInvoice.DocumentType.INVOICE);
		var expected = imported("CH2", stored, 100);
		var result = reconcile(scanned, List.of(expected));
		assertEquals(InvoiceReconciliation.Status.MATCHED, result.getStatus());
		assertSame(expected, result.getImported());
		assertEquals(stored, InvoiceReconciler.entryReference(result));
		assertEquals(printed, result.getScanned().invoiceNo());
	}

	@Test
	void screenshotInvoiceMatchesTheCombinedZOfficeSupplierLabel() {
		var date = LocalDate.of(2026, 10, 1);
		var scanned = new ScannedInvoice("invoice.pdf", "CH2", "74180032 RI", date, null,
				ScannedInvoice.DocumentType.INVOICE, 181306, .99);
		var stored = imported("ParagonCare CH2", "74180032", 1813.06);
		stored.setInvoiceDate(date);
		assertEquals(List.of("74180032 RI", "74180032"), InvoiceReconciler.lookupReferences(scanned));
		var result = reconcile(scanned, List.of(stored));
		assertEquals(InvoiceReconciliation.Status.MATCHED, result.getStatus());
		assertEquals("74180032", InvoiceReconciler.entryReference(result));
		assertTrue(result.hasExpectedAmount());
		assertEquals("$0.00", result.getVarianceString());
		stored.setInvoiceDate(null);
		result = reconcile(scanned, List.of(stored));
		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW, result.getStatus());
		assertTrue(result.hasExpectedAmount(), "A missing Z-Office date must not hide a found amount");
	}

	@Test
	void ch2ExactReferenceWinsAndDuplicateOrWrongSupplierMatchesCannotAutoAccept() {
		var scanned = scanned("CH2", "74040054 RI", ScannedInvoice.DocumentType.INVOICE);
		var exact = imported("CH2", "74040054 RI", 200);
		var alternate = imported("Paragon Care", "74040054", 100);
		var result = reconcile(scanned, List.of(alternate, exact));
		assertSame(exact, result.getImported());
		assertEquals(InvoiceReconciliation.Status.AMOUNT_MISMATCH, result.getStatus());
		assertEquals(InvoiceReconciliation.Status.AMBIGUOUS,
				reconcile(scanned, List.of(alternate, imported("CH2", "74040054", 100))).getStatus());
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE,
				reconcile(scanned, List.of(imported("Sigma", "74040054", 100))).getStatus());
		assertSame(alternate, reconcile(scanned, List.of(imported("Sigma", "74040054 RI", 100), alternate)).getImported());
		var unnamed = imported("", "74040054", 100);
		assertSame(unnamed, reconcile(scanned, List.of(unnamed)).getImported());
		assertSame(alternate, reconcile(scanned, List.of(unnamed, alternate)).getImported());
	}

	@Test
	void genericFallbackPreservesLeadingZerosAndKeepsCreditsAndMultiPartNumbersDistinct() {
		for (String supplier : List.of("Sigma", "", "Other Paragon Supplier")) {
			assertEquals(List.of("74040054 RI", "74040054"), InvoiceReconciler.lookupReferences(
					scanned(supplier, "74040054 RI", ScannedInvoice.DocumentType.INVOICE)));
		}
		assertEquals(List.of("74040054 RI"), InvoiceReconciler.lookupReferences(
				scanned("CH2", "74040054 RI", ScannedInvoice.DocumentType.CREDIT)));
		for (String reference : List.of("INV2026-74040054", "74040054/2", "RI", "")) {
			assertEquals(reference.isBlank() ? List.of() : List.of(reference), InvoiceReconciler.lookupReferences(scanned("CH2", reference, ScannedInvoice.DocumentType.INVOICE)));
		}
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(
				scanned("CH2", "000123 RI", ScannedInvoice.DocumentType.INVOICE), List.of(imported("CH2", "123", 100))).getStatus());
		var missing = reconcile(scanned("CH2", "74040054 RI", ScannedInvoice.DocumentType.INVOICE), List.of());
		assertEquals("74040054 RI", InvoiceReconciler.entryReference(missing));
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
		Invoice symbion = imported("Symbion Pty Ltd", "123", 100);
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
	void genericPrefixesWorkForEverySupplierWhileLeadingZerosAndCreditsRemainDistinct() {
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(
				scanned("Symbion", "I000123", ScannedInvoice.DocumentType.INVOICE),
				List.of(imported("Symbion", "123", 100))).getStatus());
		for (String reference : List.of("C123", "A123", "II123", "INV123")) {
			assertEquals(InvoiceReconciliation.Status.MATCHED, reconcile(
					scanned("Symbion", reference, ScannedInvoice.DocumentType.INVOICE),
					List.of(imported("Symbion", "123", 100))).getStatus());
		}
		assertEquals(List.of("I123", "123"), InvoiceReconciler.lookupReferences(scanned("Sigma", "I123", ScannedInvoice.DocumentType.INVOICE)));
		assertEquals(List.of("I123", "123"), InvoiceReconciler.lookupReferences(scanned("", "I123", ScannedInvoice.DocumentType.INVOICE)));
		assertEquals(List.of("I123"), InvoiceReconciler.lookupReferences(scanned("Symbion", "I123", ScannedInvoice.DocumentType.CREDIT)));
	}

	@ParameterizedTest
	@CsvSource({"New Supplier,INV-74040054,74040054", "New Supplier,74040054 ABC,74040054",
			"New Supplier,74040054,REF-74040054", "Another Supplier,000123-XYZ,000123"})
	void genericAffixesNeedNoSupplierOrAffixConfiguration(String supplier, String scannedReference, String storedReference) {
		var scan = scanned(supplier, scannedReference, ScannedInvoice.DocumentType.INVOICE);
		var result = reconcile(scan, List.of(imported(supplier, storedReference, 100)));
		assertEquals(InvoiceReconciliation.Status.MATCHED, result.getStatus());
		assertEquals(storedReference, InvoiceReconciler.entryReference(result));
	}

	@Test
	void genericFallbackRequiresSupplierAndDateEvidenceAndCannotChooseBetweenDistinctAffixesByAmount() {
		var scan = scanned("New Supplier", "74040054", ScannedInvoice.DocumentType.INVOICE);
		var first = imported("New Supplier", "A74040054", 100);
		var second = imported("New Supplier", "74040054B", 200);
		assertEquals(InvoiceReconciliation.Status.AMBIGUOUS, reconcile(scan, List.of(first, second)).getStatus());
		first.setInvoiceDate(null);
		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW, reconcile(scan, List.of(first)).getStatus());
		first.setInvoiceDate(DATE.plusDays(1));
		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW, reconcile(scan, List.of(first)).getStatus());
		first.setInvoiceDate(DATE); first.setSupplierName("");
		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW, reconcile(scan, List.of(first)).getStatus());
		first.setSupplierName("Unrelated Supplier");
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(scan, List.of(first)).getStatus());
		assertEquals(InvoiceReconciliation.Status.NOT_IN_Z_OFFICE, reconcile(
				scanned("New Supplier", "B74040054", ScannedInvoice.DocumentType.INVOICE), List.of(second)).getStatus());
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
		invoice.setInvoiceDate(DATE);
		invoice.setImportedInvoiceAmount(amount);
		invoice.setImportExists(true);
		return invoice;
	}
}
