package services;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfPreviewServiceTest {
	static {
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	void rotationSwapsPreviewDimensionsForQuarterTurns() throws Exception {
		Path pdf = Files.createTempFile("alpha-preview-", ".pdf");
		try {
			try (PDDocument document = new PDDocument()) {
				document.addPage(new PDPage(new PDRectangle(72, 144)));
				document.save(pdf.toFile());
			}

			BufferedImage normal = image(PdfPreviewService.render(pdf.toFile(), 0, 0));
			BufferedImage clockwise = image(PdfPreviewService.render(pdf.toFile(), 0, 1));
			BufferedImage halfTurn = image(PdfPreviewService.render(pdf.toFile(), 0, 2));
			BufferedImage counterClockwise = image(PdfPreviewService.render(pdf.toFile(), 0, 3));

			assertEquals(normal.getWidth(), halfTurn.getWidth());
			assertEquals(normal.getHeight(), halfTurn.getHeight());
			assertEquals(normal.getHeight(), clockwise.getWidth());
			assertEquals(normal.getWidth(), clockwise.getHeight());
			assertEquals(normal.getHeight(), counterClockwise.getWidth());
			assertEquals(normal.getWidth(), counterClockwise.getHeight());
		} finally {
			Files.deleteIfExists(pdf);
		}
	}

	private static BufferedImage image(PdfPreviewService.RenderedPage page) throws Exception {
		return ImageIO.read(new ByteArrayInputStream(page.png()));
	}
}
