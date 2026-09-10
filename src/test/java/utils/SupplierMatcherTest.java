package utils;

import models.InvoiceSupplier;
import models.Invoice;
import models.InvoiceReconciliation;
import models.ScannedInvoice;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierMatcherTest {
	private static final InvoiceSupplier SIGMA = supplier(1, "Sigma Healthcare Pty Ltd");
	private static final InvoiceSupplier SYMBION = supplier(2, "Symbion Pty Ltd");
	private static final InvoiceSupplier API = supplier(3, "Australian Pharmaceutical Industries");
	private static final InvoiceSupplier CH2 = supplier(4, "CH2");

	@Test
	void exactNormalizedMatchUsesDatabaseContact() {
		SupplierMatcher.Match match = SupplierMatcher.findBest("SIGMA HEALTHCARE AUSTRALIA PTY. LTD.",
				List.of(SIGMA, SYMBION));

		assertSame(SIGMA, match.supplier());
		assertTrue(match.sufficientlyConfident());
		assertEquals(1.0, match.confidence());
	}

	@Test
	void matchesCommonShortNameAndAcronym() {
		assertTrue(SupplierMatcher.findBest("Sigma", List.of(SIGMA, SYMBION)).sufficientlyConfident());
		assertSame(API, SupplierMatcher.findBest("API", List.of(API, SIGMA)).supplier());
		assertTrue(SupplierMatcher.findBest("API", List.of(API, SIGMA)).sufficientlyConfident());
	}

	@Test
	void matchesRepeatedInitialNumericShorthand() {
		SupplierMatcher.Match match = SupplierMatcher.findBest(
				"Clifford Hallom Healthcare Pty Ltd", List.of(CH2, SIGMA, SYMBION));

		assertSame(CH2, match.supplier());
		assertTrue(match.sufficientlyConfident());
		assertEquals(0.97, match.confidence());
	}

	@Test
	void numericShorthandWorksInEitherDirectionAndWithSuperscripts() {
		InvoiceSupplier fullName = supplier(5, "Clifford Hallam Healthcare");
		InvoiceSupplier threeM = supplier(6, "3M");

		assertSame(fullName, SupplierMatcher.findBest("C.H.²", List.of(fullName, SIGMA)).supplier());
		assertSame(threeM, SupplierMatcher.findBest("Minnesota Mining Manufacturing", List.of(threeM, SIGMA)).supplier());
	}

	@Test
	void ambiguousMatchIsProposedButRequiresReview() {
		InvoiceSupplier first = supplier(7, "Acme Health Group");
		InvoiceSupplier second = supplier(8, "Acme Health Services");

		SupplierMatcher.Match match = SupplierMatcher.findBest("Acme Health", List.of(first, second));

		assertFalse(match.sufficientlyConfident());
		assertTrue(match.confidence() < InvoiceReconciler.REVIEW_CONFIDENCE);
	}

	@Test
	void unrelatedNameDoesNotSelectAnArbitraryContact() {
		SupplierMatcher.Match match = SupplierMatcher.findBest("Corner Bakery", List.of(SIGMA, SYMBION));

		assertNull(match.supplier());
		assertFalse(match.sufficientlyConfident());
		assertEquals(0.0, match.confidence());
	}

	@Test
	void correlationUsesCanonicalNameAndLowestSupplierConfidence() {
		ScannedInvoice scanned = new ScannedInvoice("invoice.pdf", "Sigma", "INV-1",
				LocalDate.of(2026, 9, 10), null, ScannedInvoice.DocumentType.INVOICE, 12345,
				new ScannedInvoice.FieldConfidences(0.96, 0.99, 0.99, null, 0.99, 0.99));

		ScannedInvoice correlated = SupplierMatcher.correlate(scanned, List.of(SIGMA, SYMBION));

		assertEquals(SIGMA.getSupplierName(), correlated.supplierName());
		assertTrue(correlated.fieldConfidences().supplier() >= InvoiceReconciler.REVIEW_CONFIDENCE);
	}

	@Test
	void uncertainSupplierCorrelationMakesReconciledRowNeedReview() {
		InvoiceSupplier first = supplier(7, "Acme Health Group");
		InvoiceSupplier second = supplier(8, "Acme Health Services");
		ScannedInvoice scanned = new ScannedInvoice("invoice.pdf", "Acme Health", "INV-2",
				LocalDate.of(2026, 9, 10), null, ScannedInvoice.DocumentType.INVOICE, 12345,
				new ScannedInvoice.FieldConfidences(0.99, 0.99, 0.99, null, 0.99, 0.99));
		ScannedInvoice correlated = SupplierMatcher.correlate(scanned, List.of(first, second));
		Invoice imported = new Invoice();
		imported.setInvoiceNo("INV-2");
		imported.setImportedInvoiceAmount(123.45);
		imported.setImportExists(true);

		InvoiceReconciliation result = InvoiceReconciler.reconcile(List.of(correlated), List.of(imported)).getFirst();

		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW, result.getStatus());
	}

	private static InvoiceSupplier supplier(int id, String name) {
		return new InvoiceSupplier(id, name, 10);
	}
}
