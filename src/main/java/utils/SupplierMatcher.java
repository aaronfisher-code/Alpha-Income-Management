package utils;

import models.InvoiceSupplier;
import models.ScannedInvoice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Matches a document supplier name to the store's existing supplier contacts. */
public final class SupplierMatcher {
	public static final double CONFIDENT_MATCH = InvoiceReconciler.REVIEW_CONFIDENCE;
	private static final double MINIMUM_LEAD = 0.05;
	private static final double MINIMUM_PROPOSAL = 0.55;

	private SupplierMatcher() {}

	/**
	 * Returns the strongest supplier candidate. A candidate can still be returned
	 * when it is not sufficiently confident so the review screen can offer the
	 * best guess without allowing the row to be accepted automatically.
	 */
	public static Match findBest(String extractedName, List<InvoiceSupplier> suppliers) {
		String extracted = InvoiceReconciler.supplier(extractedName);
		if (extracted.isBlank() || suppliers == null || suppliers.isEmpty()) return Match.none();

		List<Candidate> candidates = new ArrayList<>();
		for (InvoiceSupplier supplier : suppliers) {
			if (supplier == null || supplier.getContactID() <= 0) continue;
			String name = InvoiceReconciler.supplier(supplier.getSupplierName());
			if (name.isBlank()) continue;
			candidates.add(new Candidate(supplier, name, similarity(extracted, name)));
		}
		candidates.sort(Comparator.comparingDouble(Candidate::score).reversed()
				.thenComparing(Candidate::normalizedName));
		if (candidates.isEmpty() || candidates.getFirst().score() < MINIMUM_PROPOSAL) return Match.none();

		Candidate best = candidates.getFirst();
		double runnerUp = candidates.size() > 1 ? candidates.get(1).score() : 0;
		boolean exact = best.score() == 1.0 && runnerUp < 1.0;
		boolean unambiguous = exact || best.score() - runnerUp >= MINIMUM_LEAD;
		boolean confident = best.score() >= CONFIDENT_MATCH && unambiguous;
		// An ambiguous high-scoring result must remain below the review threshold.
		double confidence = confident ? best.score() : Math.min(best.score(), Math.nextDown(CONFIDENT_MATCH));
		return new Match(best.supplier(), confidence, confident);
	}

	/** Uses the saved contact's canonical name and folds match quality into supplier confidence. */
	public static ScannedInvoice correlate(ScannedInvoice scanned, List<InvoiceSupplier> suppliers) {
		if (scanned == null) return null;
		Match match = findBest(scanned.supplierName(), suppliers);
		String supplierName = match.supplier() == null
				? scanned.supplierName() : match.supplier().getSupplierName();
		ScannedInvoice.FieldConfidences fields = scanned.fieldConfidences();
		Double supplierConfidence = fields.supplier() == null
				? match.confidence() : Math.min(fields.supplier(), match.confidence());
		return new ScannedInvoice(scanned.sourceFile(), supplierName, scanned.invoiceNo(),
				scanned.invoiceDate(), scanned.dueDate(), scanned.documentType(), scanned.amountCents(),
				new ScannedInvoice.FieldConfidences(supplierConfidence, fields.reference(), fields.date(),
						fields.dueDate(), fields.type(), fields.amount()));
	}

	private static double similarity(String left, String right) {
		if (left.equals(right)) return 1.0;
		String leftCompact = compact(left);
		String rightCompact = compact(right);
		if (isShorthandOf(leftCompact, right) || isShorthandOf(rightCompact, left)) return 0.97;

		double score = jaroWinkler(leftCompact, rightCompact);
		Set<String> leftTokens = tokens(left);
		Set<String> rightTokens = tokens(right);
		Set<String> shared = new HashSet<>(leftTokens);
		shared.retainAll(rightTokens);
		if (!shared.isEmpty()) {
			double containment = shared.size() / (double) Math.min(leftTokens.size(), rightTokens.size());
			double balance = Math.min(leftCompact.length(), rightCompact.length())
					/ (double) Math.max(leftCompact.length(), rightCompact.length());
			if (containment == 1.0) score = Math.max(score, 0.90 + 0.08 * balance);
			else {
				double dice = 2.0 * shared.size() / (leftTokens.size() + rightTokens.size());
				score = Math.max(score, 0.75 * dice + 0.15 * balance);
			}
		}
		return Math.max(0, Math.min(1, score));
	}

	private static boolean isShorthandOf(String possibleShorthand, String fullName) {
		String[] words = fullName.split("\\s+");
		if (words.length < 2) return false;
		StringBuilder initials = new StringBuilder(words.length);
		for (String word : words) {
			if (!word.isBlank()) initials.append(word.charAt(0));
		}
		String expanded = expandNumericShorthand(possibleShorthand);
		return initials.length() >= 2
				&& (possibleShorthand.equals(initials.toString()) || expanded.equals(initials.toString()));
	}

	/** Expands repeat-count acronyms such as CH2 -> CHH and 3M -> MMM. */
	private static String expandNumericShorthand(String value) {
		StringBuilder expanded = new StringBuilder();
		for (int index = 0; index < value.length();) {
			char current = value.charAt(index);
			if (!Character.isDigit(current)) {
				expanded.append(current);
				index++;
				continue;
			}
			int digitEnd = index + 1;
			while (digitEnd < value.length() && Character.isDigit(value.charAt(digitEnd))) digitEnd++;
			int count;
			try {
				count = Integer.parseInt(value.substring(index, digitEnd));
			} catch (NumberFormatException exception) {
				return value;
			}
			if (count < 2 || count > 20) return value;
			if (expanded.isEmpty()) {
				if (digitEnd >= value.length() || !Character.isLetter(value.charAt(digitEnd))) return value;
				char repeated = value.charAt(digitEnd);
				expanded.append(String.valueOf(repeated).repeat(count));
				index = digitEnd + 1;
			} else {
				char repeated = expanded.charAt(expanded.length() - 1);
				expanded.append(String.valueOf(repeated).repeat(count - 1));
				index = digitEnd;
			}
		}
		return expanded.toString();
	}

	private static Set<String> tokens(String value) {
		return new HashSet<>(List.of(value.split("\\s+")));
	}

	private static String compact(String value) {
		return value.replace(" ", "").toUpperCase(Locale.ROOT);
	}

	private static double jaroWinkler(String left, String right) {
		if (left.equals(right)) return 1.0;
		if (left.isEmpty() || right.isEmpty()) return 0.0;
		int range = Math.max(left.length(), right.length()) / 2 - 1;
		boolean[] leftMatches = new boolean[left.length()];
		boolean[] rightMatches = new boolean[right.length()];
		int matches = 0;
		for (int i = 0; i < left.length(); i++) {
			int start = Math.max(0, i - range);
			int end = Math.min(i + range + 1, right.length());
			for (int j = start; j < end; j++) {
				if (rightMatches[j] || left.charAt(i) != right.charAt(j)) continue;
				leftMatches[i] = true;
				rightMatches[j] = true;
				matches++;
				break;
			}
		}
		if (matches == 0) return 0.0;
		int transpositions = 0;
		for (int i = 0, j = 0; i < left.length(); i++) {
			if (!leftMatches[i]) continue;
			while (!rightMatches[j]) j++;
			if (left.charAt(i) != right.charAt(j)) transpositions++;
			j++;
		}
		double m = matches;
		double jaro = (m / left.length() + m / right.length()
				+ (m - transpositions / 2.0) / m) / 3.0;
		int prefix = 0;
		while (prefix < Math.min(4, Math.min(left.length(), right.length()))
				&& left.charAt(prefix) == right.charAt(prefix)) prefix++;
		return jaro + prefix * 0.1 * (1.0 - jaro);
	}

	public record Match(InvoiceSupplier supplier, double confidence, boolean sufficientlyConfident) {
		private static Match none() {
			return new Match(null, 0.0, false);
		}
	}

	private record Candidate(InvoiceSupplier supplier, String normalizedName, double score) {}
}
