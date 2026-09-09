package services;

import models.PdfEvidenceField;
import models.PdfEvidenceLocation;
import models.ScannedInvoice;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Locates OCR values in a PDF text layer for precise review highlighting. */
public final class PdfEvidenceLocator {
	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
			DateTimeFormatter.ofPattern("dd-MMM-uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("d.M.uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("d-M-uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
			DateTimeFormatter.ISO_LOCAL_DATE);

	private PdfEvidenceLocator() {}

	/**
	 * Retains the original reference-only API for callers that only need the
	 * primary invoice-number box.
	 */
	public static Map<ScannedInvoice, PdfEvidenceLocation> locate(File pdf, List<ScannedInvoice> rows)
			throws IOException {
		Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> fields = locateFields(pdf, rows);
		Map<ScannedInvoice, PdfEvidenceLocation> references = new LinkedHashMap<>();
		fields.forEach((row, locations) -> {
			PdfEvidenceLocation reference = locations.get(PdfEvidenceField.REFERENCE);
			if (reference != null) references.put(row, reference);
		});
		return Map.copyOf(references);
	}

	/** Locates supplier, reference, date, and amount boxes for every scanned row. */
	public static Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locateFields(
			File pdf, List<ScannedInvoice> rows) throws IOException {
		try (PDDocument document = PDDocument.load(pdf)) {
			GlyphCollector collector = new GlyphCollector();
			collector.setSortByPosition(true);
			collector.getText(document);
			Map<Integer, PageText> pages = pageText(collector.byPage);
			Map<ScannedInvoice, Map<PdfEvidenceField, PdfEvidenceLocation>> locations = new LinkedHashMap<>();

			for (ScannedInvoice row : rows) {
				Match reference = findField(document, pages, PdfEvidenceField.REFERENCE,
						needles(row, PdfEvidenceField.REFERENCE), null);
				EnumMap<PdfEvidenceField, PdfEvidenceLocation> rowLocations = new EnumMap<>(PdfEvidenceField.class);
				if (reference != null) rowLocations.put(PdfEvidenceField.REFERENCE, reference.location());
				for (PdfEvidenceField field : PdfEvidenceField.values()) {
					if (field == PdfEvidenceField.REFERENCE) continue;
					Match match = findField(document, pages, field, needles(row, field), reference);
					if (match != null) rowLocations.put(field, match.location());
				}
				if (!rowLocations.isEmpty()) locations.put(row, Collections.unmodifiableMap(rowLocations));
			}
			return Map.copyOf(locations);
		}
	}

	private static Map<Integer, PageText> pageText(Map<Integer, List<Glyph>> byPage) {
		Map<Integer, PageText> pages = new HashMap<>();
		byPage.forEach((pageIndex, glyphs) -> {
			StringBuilder searchable = new StringBuilder();
			List<Glyph> characterMap = new ArrayList<>();
			for (Glyph glyph : glyphs) {
				String normalized = normalize(glyph.text());
				searchable.append(normalized);
				for (int index = 0; index < normalized.length(); index++) characterMap.add(glyph);
			}
			pages.put(pageIndex, new PageText(searchable.toString(), characterMap));
		});
		return pages;
	}

	private static Match findField(PDDocument document, Map<Integer, PageText> pages, PdfEvidenceField field,
			List<String> needles, Match reference) {
		if (needles.isEmpty()) return null;
		Match best = null;
		double bestScore = Double.MAX_VALUE;
		for (Map.Entry<Integer, PageText> pageEntry : pages.entrySet()) {
			int pageIndex = pageEntry.getKey();
			PageText page = pageEntry.getValue();
			for (int needleIndex = 0; needleIndex < needles.size(); needleIndex++) {
				String needle = needles.get(needleIndex);
				if (needle.isBlank()) continue;
				for (Match candidate : matches(document, pageIndex, page, needle)) {
					double score = score(candidate, reference, needleIndex, field);
					if (score < bestScore) {
						best = candidate;
						bestScore = score;
					}
				}
			}
		}
		return best;
	}

	private static double score(Match candidate, Match reference, int needleIndex, PdfEvidenceField field) {
		if (reference == null) return needleIndex * 0.01 + candidate.pageIndex() * 100_000 + candidate.centerY();
		// Values in a statement row share a baseline with the reference. Prefer
		// that page and row so a repeated date/amount elsewhere is not selected.
		double pagePenalty = candidate.pageIndex() == reference.pageIndex() ? 0 : 100_000;
		double rowDistance = Math.abs(candidate.centerY() - reference.centerY());
		// Supplier names normally live in the document header; retaining the page
		// preference is useful, while the small field penalty keeps matching stable.
		return pagePenalty + rowDistance + needleIndex * 0.01 + (field == PdfEvidenceField.SUPPLIER ? 0.1 : 0);
	}

	private static List<Match> matches(PDDocument document, int pageIndex, PageText page, String needle) {
		List<Match> matches = new ArrayList<>();
		int from = 0;
		while (from < page.searchable().length()) {
			int start = page.searchable().indexOf(needle, from);
			if (start < 0) break;
			int end = Math.min(start + needle.length(), page.characterMap().size());
			if (end > start) {
				List<Glyph> glyphs = page.characterMap().subList(start, end);
				PdfEvidenceLocation location = box(document, pageIndex, glyphs);
				matches.add(new Match(pageIndex, location, centerY(glyphs)));
			}
			from = Math.max(start + 1, end);
		}
		return matches;
	}

	private static PdfEvidenceLocation box(PDDocument document, int pageIndex, List<Glyph> glyphs) {
		double textHeight = glyphs.stream().mapToDouble(Glyph::height).max().orElse(8);
		// Keep the box around the matched value itself. Expanding to every glyph
		// on the same baseline can include labels and unrelated header fields.
		double left = glyphs.stream().mapToDouble(Glyph::x).min().orElse(0);
		double top = glyphs.stream().mapToDouble(glyph -> glyph.baseline() - glyph.height()).min().orElse(0);
		double right = glyphs.stream().mapToDouble(glyph -> glyph.x() + glyph.width()).max().orElse(left);
		double bottom = glyphs.stream().mapToDouble(glyph -> glyph.baseline() + 2).max().orElse(top + textHeight);
		PDPage page = document.getPage(pageIndex);
		double pageWidth = page.getCropBox().getWidth();
		double pageHeight = page.getCropBox().getHeight();
		if (Math.floorMod(page.getRotation(), 180) != 0) {
			double swap = pageWidth;
			pageWidth = pageHeight;
			pageHeight = swap;
		}
		double padding = 4;
		left = Math.max(0, left - padding);
		top = Math.max(0, top - padding);
		right = Math.min(pageWidth, right + padding);
		bottom = Math.min(pageHeight, bottom + padding);
		return new PdfEvidenceLocation(pageIndex, left, top, Math.max(1, right - left),
				Math.max(1, bottom - top), pageWidth, pageHeight);
	}

	private static double centerY(List<Glyph> glyphs) {
		if (glyphs.isEmpty()) return 0;
		double top = glyphs.stream().mapToDouble(glyph -> glyph.baseline() - glyph.height()).min().orElse(0);
		double bottom = glyphs.stream().mapToDouble(glyph -> glyph.baseline() + 2).max().orElse(top);
		return (top + bottom) / 2;
	}

	private static List<String> needles(ScannedInvoice row, PdfEvidenceField field) {
		return switch (field) {
			case SUPPLIER -> distinct(row.supplierName());
			case REFERENCE -> distinct(row.invoiceNo());
			case DATE -> dateNeedles(row.invoiceDate());
			case AMOUNT -> amountNeedles(row.amountCents());
		};
	}

	private static List<String> dateNeedles(LocalDate date) {
		if (date == null) return List.of();
		Set<String> values = new LinkedHashSet<>();
		for (DateTimeFormatter formatter : DATE_FORMATTERS) values.add(normalize(formatter.format(date)));
		return List.copyOf(values);
	}

	private static List<String> amountNeedles(long amountCents) {
		long absolute = Math.abs(amountCents);
		long dollars = absolute / 100;
		long cents = absolute % 100;
		String decimal = String.format(Locale.ROOT, "%d.%02d", dollars, cents);
		String commaDecimal = String.format(Locale.US, "%,d.%02d", dollars, cents);
		Set<String> values = new LinkedHashSet<>();
		for (String value : List.of(decimal, commaDecimal, "$" + decimal, "$" + commaDecimal,
				"-" + decimal, "(" + decimal + ")", decimal + " CR")) {
			values.add(normalize(value));
		}
		return List.copyOf(values);
	}

	private static List<String> distinct(String value) {
		String normalized = normalize(value);
		return normalized.isBlank() ? List.of() : List.of(normalized);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
	}

	private record PageText(String searchable, List<Glyph> characterMap) {}

	private record Match(int pageIndex, PdfEvidenceLocation location, double centerY) {}

	private record Glyph(String text, double x, double baseline, double width, double height) {}

	private static final class GlyphCollector extends PDFTextStripper {
		private final Map<Integer, List<Glyph>> byPage = new HashMap<>();

		private GlyphCollector() throws IOException {
			super();
		}

		@Override
		protected void writeString(String text, List<TextPosition> positions) {
			List<Glyph> page = byPage.computeIfAbsent(getCurrentPageNo() - 1, _ -> new ArrayList<>());
			for (TextPosition position : positions) {
				page.add(new Glyph(position.getUnicode(), position.getXDirAdj(), position.getYDirAdj(),
						position.getWidthDirAdj(), position.getHeightDir()));
			}
		}
	}
}
