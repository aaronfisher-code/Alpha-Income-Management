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
import java.awt.image.BufferedImage;

/** Renders a single PDF page for the JavaFX evidence preview. */
public final class PdfPreviewService {
	private static final float PREVIEW_DPI = 144;

	private PdfPreviewService() {}

	public static RenderedPage render(File pdf, int requestedPage) throws IOException {
		return render(pdf, requestedPage, 0);
	}

	/** Renders a page and applies the requested number of clockwise quarter-turns. */
	public static RenderedPage render(File pdf, int requestedPage, int rotationQuarterTurns) throws IOException {
		try (PDDocument document = Loader.loadPDF(pdf)) {
			if (document.getNumberOfPages() == 0) throw new IOException("The PDF has no pages");
			int pageIndex = Math.max(0, Math.min(requestedPage, document.getNumberOfPages() - 1));
			PDFRenderer renderer = new PDFRenderer(document);
			renderer.setSubsamplingAllowed(true);
			var image = renderer.renderImageWithDPI(pageIndex, PREVIEW_DPI, ImageType.RGB);
			image = rotate(image, Math.floorMod(rotationQuarterTurns, 4));
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

	private static BufferedImage rotate(BufferedImage source, int quarterTurns) {
		if (quarterTurns == 0) return source;
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();
		int targetWidth = quarterTurns % 2 == 0 ? sourceWidth : sourceHeight;
		int targetHeight = quarterTurns % 2 == 0 ? sourceHeight : sourceWidth;
		BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < sourceHeight; y++) {
			for (int x = 0; x < sourceWidth; x++) {
				int targetX;
				int targetY;
				switch (quarterTurns) {
					case 1 -> { targetX = sourceHeight - 1 - y; targetY = x; }
					case 2 -> { targetX = sourceWidth - 1 - x; targetY = sourceHeight - 1 - y; }
					case 3 -> { targetX = y; targetY = sourceWidth - 1 - x; }
					default -> { targetX = x; targetY = y; }
				}
				target.setRGB(targetX, targetY, source.getRGB(x, y));
			}
		}
		return target;
	}

	public record RenderedPage(byte[] png, int pageIndex, int pageCount, double pageWidth, double pageHeight) {}
}
