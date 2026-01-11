package com.veritynow.ms.ocr.tesseract;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.veritynow.ms.ocr.OcrField;
import com.veritynow.ms.ocr.OcrResponse;
import com.veritynow.ms.ocr.OcrService;
import com.veritynow.ms.pdf.FormTemplate;
import com.veritynow.ms.pdf.FormTemplateLoader;
import com.veritynow.ms.pdf.Zone;

@Service("tesseractOcr")
public class TesseractService implements OcrService {

    private final TesseractEngine tesseractEngine;
    private final FormTemplateLoader templateLoader;

    public TesseractService(
            TesseractEngine tesseractEngine,
            FormTemplateLoader templateLoader
    ) {
        this.tesseractEngine = tesseractEngine;
        this.templateLoader = templateLoader;
    }
    @Override
    public OcrResponse extract(MultipartFile file) {

        try {
          

            // 2) Load template zone config first so we know the DPI
            FormTemplate cfg = templateLoader.load();
            int dpi = (int) cfg.dpi();

            // 1) Render PDF/image into pages at the SAME dpi
            List<BufferedImage> rawPages = PdfOrImageUtil.toImages(file, dpi);
            List<BufferedImage> normalized = rawPages;
            BufferedImage page = normalized.get(0); // assume template always 1st page

            System.out.println("PAGE SIZE: " + page.getWidth() + "x" + page.getHeight());

            Map<String, OcrField> results = new LinkedHashMap<>();

            // 3) Iterate through zones
            cfg.fields().forEach((id,f) -> {

                String fieldId = id;
                Zone z = f.zone();

                // --- Clamp to page bounds to avoid RasterFormatException ---
                int maxX = page.getWidth();
                int maxY = page.getHeight();

                int x = Math.max(0, Math.min((int) z.x(), maxX - 1));
                int y = Math.max(0, Math.min((int)z.y(), maxY - 1));
                int w = Math.max(1, Math.min((int)z.w(), maxX - x));
                int h = Math.max(1, Math.min((int)z.h(), maxY - y));

                // Optional debug:
                // System.out.printf("FIELD %s -> rect (%d,%d) %dx%d%n", fieldId, x, y, w, h);

                BufferedImage cropped = page.getSubimage(x, y, w, h);

                // OCR the region
                String text = tesseractEngine.ocr(cropped);
                text = (text == null ? "" : text.trim());

                results.put(fieldId, new OcrField(fieldId, text, null));
            });

            System.out.println("============ OCR Done ============");
            return new OcrResponse(
                    cfg.id(),
                    "local-ocr-zones",
                    results
            );

        } catch (IOException ex) {
            throw new RuntimeException("OCR extraction failed", ex);
        }
    }

	}

