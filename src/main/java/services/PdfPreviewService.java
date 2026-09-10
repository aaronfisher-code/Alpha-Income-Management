package services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/** Renders a single PDF page for the JavaFX evidence preview. */
public final class PdfPreviewService {
	private static final float PREVIEW_DPI = 144;

	private PdfPreviewService() {}

	public static RenderedPage render(File pdf, int requestedPage) throws IOException {
		try (PDDocument document = Loader.loadPDF(pdf)) {
			if (document.getNumberOfPages() == 0) throw new IOException("The PDF has no pages");
			int pageIndex = Math.max(0, Math.min(requestedPage, document.getNumberOfPages() - 1));
			PDFRenderer renderer = new PDFRenderer(document);
			renderer.setSubsamplingAllowed(true);
			var image = renderer.renderImageWithDPI(pageIndex, PREVIEW_DPI, ImageType.RGB);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			if (!ImageIO.write(image, "png", output)) throw new IOException("PNG preview encoding is unavailable");

			PDPage page = document.getPage(pageIndex);
			double pageWidth = page.getCropBox().getWidth();
			double pageHeight = page.getCropBox().getHeight();
			if (Math.floorMod(page.getRotation(), 180) != 0) {
				double swap = pageWidth;
				pageWidth = pageHeight;
				pageHeight = swap;
			}
			return new RenderedPage(output.toByteArray(), pageIndex, document.getNumberOfPages(), pageWidth, pageHeight);
		}
	}

	public record RenderedPage(byte[] png, int pageIndex, int pageCount, double pageWidth, double pageHeight) {}
}
