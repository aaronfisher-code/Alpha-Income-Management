package utils;

import models.InvoiceSupplier;
import models.Invoice;
import models.InvoiceReconciliation;
import models.ScannedInvoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
	private static final InvoiceSupplier DCO_GLADSTONE = supplier(20, "Direct Chemist Outlet Gladstone");
	private static final InvoiceSupplier DCO_HILLSIDE = supplier(21, "Direct Chemist Outlet Hillside");
	private static final InvoiceSupplier DCO_TAYLORS_LAKES = supplier(22, "Direct Chemist Outlet Taylors Lakes");

	@ParameterizedTest
	@ValueSource(strings = { "DCO HILLSIDE", "dco hillside", "D.C.O. Hillside", "Direct Chemist Outlet Hillside",
			"Hillside Direct Chemist Outlet Pharmacy Pty Ltd" })
	void matchesDcoHillsideWithoutConfusingOtherBranches(String extracted) {
		for (List<InvoiceSupplier> contacts : List.of(
				List.of(DCO_GLADSTONE, DCO_HILLSIDE, DCO_TAYLORS_LAKES),
				List.of(DCO_TAYLORS_LAKES, DCO_HILLSIDE, DCO_GLADSTONE))) {
			SupplierMatcher.Match match = SupplierMatcher.findBest(extracted, contacts);

			assertSame(DCO_HILLSIDE, match.supplier());
			assertTrue(match.sufficientlyConfident());
		}
	}

	@Test
	void matchesDcoAcronymContactsAndMultiwordBranchesInEitherDirection() {
		InvoiceSupplier acronym = supplier(23, "DCO Hillside");
		assertSame(acronym, SupplierMatcher.findBest("Direct Chemist Outlet Hillside",
				List.of(DCO_GLADSTONE, acronym)).supplier());
		assertSame(DCO_TAYLORS_LAKES, SupplierMatcher.findBest("DCO Taylors Lakes",
				List.of(DCO_GLADSTONE, DCO_HILLSIDE, DCO_TAYLORS_LAKES)).supplier());
		assertSame(DCO_GLADSTONE, SupplierMatcher.findBest("DCO Gladstone",
				List.of(DCO_GLADSTONE, DCO_HILLSIDE, DCO_TAYLORS_LAKES)).supplier());
	}

	@ParameterizedTest
	@ValueSource(strings = { "DCO", "Direct Chemist Outlet", "DCO Unknown Branch", "DCO Hillside",
			"Direct Chemist Outlet Hillside", "Direct Chemist Outlet Gladstone Park" })
	void neverSubstitutesTheOnlyAvailableDcoBranch(String extracted) {
		SupplierMatcher.Match match = SupplierMatcher.findBest(extracted, List.of(DCO_GLADSTONE, SIGMA));

		assertNull(match.supplier());
		assertFalse(match.sufficientlyConfident());
	}

	@Test
	void genericDcoNameDoesNotPickOneOfSeveralBranches() {
		SupplierMatcher.Match match = SupplierMatcher.findBest("DCO",
				List.of(DCO_GLADSTONE, DCO_HILLSIDE, DCO_TAYLORS_LAKES));
		assertNull(match.supplier());
		assertFalse(match.sufficientlyConfident());
	}

	@Test
	void uncertainCorrelationKeepsExtractedNameAcrossRepeatedMatching() {
		List<InvoiceSupplier> contacts = List.of(supplier(7, "Acme Health Group"), supplier(8, "Acme Health Services"));
		ScannedInvoice scanned = new ScannedInvoice("invoice.pdf", "Acme Health", "INV-2",
				LocalDate.of(2026, 9, 10), null, ScannedInvoice.DocumentType.INVOICE, 12345,
				new ScannedInvoice.FieldConfidences(0.99, 0.99, 0.99, null, 0.99, 0.99));

		ScannedInvoice correlated = SupplierMatcher.correlate(scanned, contacts);

		assertEquals("Acme Health", correlated.supplierName());
		assertFalse(SupplierMatcher.findBest(correlated.supplierName(), contacts).sufficientlyConfident());
		assertTrue(correlated.fieldConfidences().supplier() < InvoiceReconciler.REVIEW_CONFIDENCE);
	}

	@Test
	void missingDcoBranchKeepsHillsideEvidenceAndRequiresReview() {
		ScannedInvoice scanned = new ScannedInvoice("transfer.pdf", "DCO HILLSIDE", "1419",
				LocalDate.of(2026, 9, 17), null, ScannedInvoice.DocumentType.INVOICE, 51954,
				new ScannedInvoice.FieldConfidences(0.99, 0.99, 0.99, null, 0.99, 0.99));
		ScannedInvoice correlated = SupplierMatcher.correlate(scanned, List.of(DCO_GLADSTONE));
		Invoice imported = new Invoice();
		imported.setInvoiceNo("1419");
		imported.setImportedInvoiceAmount(519.54);
		imported.setImportExists(true);

		assertEquals("DCO HILLSIDE", correlated.supplierName());
		assertEquals(InvoiceReconciliation.Status.NEEDS_REVIEW,
				InvoiceReconciler.reconcile(List.of(correlated), List.of(imported)).getFirst().getStatus());
	}

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

	@ParameterizedTest
	@ValueSource(strings = { "Clifford Hallam Healthcare Eastern Creek NSW",
			"Clifford Hallam Healthcare Pty Ltd 5 Eucalyptus Place Eastern Creek NSW 2766",
			"Clifford Hallom Healthcare Eastern Creek NSW", "ParagonCare", "Paragon Care Pty Ltd",
			"ParagonCare Clifford Hallam Healthcare Pty Ltd", "C.H.²" })
	void matchesCh2StatementIssuerAndBrandToTheExistingContact(String extracted) {
		SupplierMatcher.Match match = SupplierMatcher.findBest(extracted, List.of(SIGMA, CH2, SYMBION, API));
		assertSame(CH2, match.supplier());
		assertTrue(match.sufficientlyConfident());
	}

	@ParameterizedTest
	@ValueSource(strings = { "ParagonCare CH2", "Paragon Care CH2", "CH2 ParagonCare", "ParagonCare (CH2)" })
	void combinedLabelsMustIdentifyTheSameSupplierInBothParts(String label) {
		assertTrue(SupplierMatcher.sameSupplier("CH2", label));
		assertTrue(SupplierMatcher.sameSupplier(label, "CH2"));
		assertTrue(SupplierMatcher.findBest(label, List.of(CH2, SIGMA)).sufficientlyConfident());
		assertTrue(SupplierMatcher.sameSupplier("API", "Australian Pharmaceutical Industries API"));
		assertTrue(SupplierMatcher.sameSupplier("AB", "Acme Brand AB"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Another Supplier", "Supplier South", "ParagonCare Diagnostics CH2", "ParagonCare CH2 South" })
	void combinedLabelsCannotIgnoreAnUnrelatedNameOrBranch(String label) {
		assertFalse(SupplierMatcher.sameSupplier("CH2", label));
		assertFalse(SupplierMatcher.sameSupplier("Supplier", label));
		assertFalse(SupplierMatcher.sameSupplier("AB", "Acme Brand South AB"));
		assertFalse(SupplierMatcher.sameSupplier("DCO Hillside", "DCO Gladstone"));
	}

	@Test
	void ch2CorrelationUsesTheSavedContactNameWithoutIncreasingOcrConfidence() {
		ScannedInvoice scanned = new ScannedInvoice("statement.pdf", "Clifford Hallam Healthcare Eastern Creek NSW",
				"73646116", LocalDate.of(2026, 8, 3), null, ScannedInvoice.DocumentType.INVOICE, 57019,
				new ScannedInvoice.FieldConfidences(0.7, 0.99, 0.99, null, 0.99, 0.99));
		ScannedInvoice correlated = SupplierMatcher.correlate(scanned, List.of(CH2, SIGMA));
		assertEquals("CH2", correlated.supplierName());
		assertEquals(0.7, correlated.fieldConfidences().supplier());
		assertEquals(scanned.invoiceNo(), correlated.invoiceNo());
		assertEquals(scanned.amountCents(), correlated.amountCents());
	}

	@Test
	void exactParagonCareContactTakesPrecedenceOverCh2Alias() {
		InvoiceSupplier paragon = supplier(5, "ParagonCare");
		SupplierMatcher.Match match = SupplierMatcher.findBest("ParagonCare", List.of(CH2, paragon, SIGMA));
		assertSame(paragon, match.supplier());
		assertTrue(match.sufficientlyConfident());
	}

	@Test
	void ch2AliasOnlyUsesAnExistingStoreContact() {
		ScannedInvoice scanned = new ScannedInvoice("statement.pdf", "Clifford Hallam Healthcare Eastern Creek NSW",
				"73646116", LocalDate.of(2026, 8, 3), null, ScannedInvoice.DocumentType.INVOICE, 57019, 0.99);
		ScannedInvoice correlated = SupplierMatcher.correlate(scanned, List.of(SIGMA, SYMBION));
		assertEquals(scanned.supplierName(), correlated.supplierName());
		assertTrue(correlated.fieldConfidences().supplier() < InvoiceReconciler.REVIEW_CONFIDENCE);
	}

	@Test
	void duplicateCh2ContactsRemainAmbiguous() {
		SupplierMatcher.Match match = SupplierMatcher.findBest("Clifford Hallam Healthcare Eastern Creek NSW",
				List.of(CH2, supplier(5, "CH2"), SIGMA));
		assertFalse(match.sufficientlyConfident());
		assertTrue(match.confidence() < InvoiceReconciler.REVIEW_CONFIDENCE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Eastern Creek NSW", "5 Eucalyptus Place Eastern Creek NSW 2766",
			"Clifford Healthcare", "Hallam Healthcare", "ParagonCare Diagnostics", "Paragon Medical" })
	void addressOrUnrelatedSimilarNameDoesNotConfidentlySelectCh2(String extracted) {
		assertFalse(SupplierMatcher.findBest(extracted, List.of(CH2, SIGMA, SYMBION)).sufficientlyConfident());
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
		assertTrue(correlated.dueDateEstimated());
		assertEquals(LocalDate.of(2026, 10, 10), correlated.dueDate());
	}

	@Test
	void legacyFallbackDueDateIsStillRecognizedAsEstimated() {
		LocalDate invoiceDate = LocalDate.of(2026, 9, 10);
		ScannedInvoice legacy = new ScannedInvoice("invoice.pdf", "Sigma", "INV-legacy",
				invoiceDate, invoiceDate.plusDays(30), ScannedInvoice.DocumentType.INVOICE, 12345,
				new ScannedInvoice.FieldConfidences(0.99, 0.99, 0.99, null, 0.99, 0.99));

		InvoiceReconciliation row = new InvoiceReconciliation(InvoiceReconciliation.Status.NEEDS_REVIEW,
				legacy, null, null);

		assertFalse(legacy.dueDateEstimated());
		assertTrue(row.isDueDateEstimated());
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
