package com.veritynow.ms.ocr.tesseract;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.web.multipart.MultipartFile;

public final class PdfOrImageUtil {

    private PdfOrImageUtil() {}

    /**
     * Render a PDF (or return single image) at the given DPI.
     * For non-PDF images, we just read as-is.
     */
    public static List<BufferedImage> toImages(MultipartFile f, int dpi) throws IOException {
    	
    	  byte[] bytes = f.getBytes();
    	
    	  // crude check: PDF files start with "%PDF"
    	  if ("application/pdf".equals(f.getContentType()) || isPdf(bytes)) {
            return pdfToImages(bytes, dpi);
        } else {
            // assume It's an image
            BufferedImage img = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            List<BufferedImage> list = new ArrayList<>();
            if (img != null) {
                list.add(img);
            }
            return list;
        }
    }

    private static boolean isPdf(byte[] bytes) {
        if (bytes.length < 4) return false;
        return bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    private static List<BufferedImage> pdfToImages(byte[] bytes, int dpi) throws IOException {
        List<BufferedImage> pages = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pageCount = doc.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                // IMPORTANT: use the *same* DPI as used when extracting zones (e.g. 200)
                BufferedImage pageImage =
                        renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                pages.add(pageImage);
            }
        }
        return pages;
    }
}
