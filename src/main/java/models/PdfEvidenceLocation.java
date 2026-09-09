package models;

/** A top-left-origin rectangle in PDF points on a zero-based page. */
public record PdfEvidenceLocation(
		int pageIndex,
		double x,
		double y,
		double width,
		double height,
		double pageWidth,
		double pageHeight) {
}
