package com.veritynow.ms.ocr.tesseract;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Simple startup check to verify Tesseract can load its traineddata.
 * It LOGS errors but NEVER fails the application startup.
 */
@Component
public class TesseractStartupCheck {

    private static final Logger log = LoggerFactory.getLogger(TesseractStartupCheck.class);

    private final TesseractEngine tesseractEngine;
    private final String mode; 

    public TesseractStartupCheck(TesseractEngine tesseractEngine, @Value("verity.ocr.mode:tesseract") String mode) {
        this.tesseractEngine = tesseractEngine;
        this.mode = mode;
    }

    @PostConstruct
    public void verifyTesseract() {
        try {
        	if (!"tesseract".equalsIgnoreCase(mode)) {
        		return;
        	}
            // quick test image
            BufferedImage img = new BufferedImage(120, 40, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("TEST", 10, 25);
            g.dispose();

            String text = tesseractEngine.ocr(img);

            log.info(
                "Tesseract warm-up OK (language='{}', datapath='{}'). Sample OCR result='{}'",
                tesseractEngine.getLanguage(),
                tesseractEngine.getDataPath(),
                text.trim()
            );
        } catch (Throwable t) { // catch EVERYTHING so startup never fails
            log.error(
                "Tesseract warm-up FAILED. Check ocr.tesseract.datapath and tessdata folder. " +
                "Application will still start, but OCR calls may fail.",
                t
            );
            // DO NOT rethrow
        }
    }
}
