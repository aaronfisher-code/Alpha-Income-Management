package models;

/** OCR fields that can be highlighted in the source PDF review pane. */
public enum PdfEvidenceField {
	SUPPLIER("Supplier", "#2f80ed"),
	REFERENCE("Invoice reference", "#e05260"),
	DATE("Invoice date", "#8e5bd9"),
	AMOUNT("Invoice amount", "#2ca46f");

	private final String label;
	private final String color;

	PdfEvidenceField(String label, String color) {
		this.label = label;
		this.color = color;
	}

	public String label() {
		return label;
	}

	public String color() {
		return color;
	}
}
