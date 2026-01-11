package com.veritynow.ms.ocr.tesseract;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * Thin wrapper around Tess4J to centralize configuration.
 */
public class TesseractEngine {

    private static final Logger log = LoggerFactory.getLogger(TesseractEngine.class);

    private final Tesseract tesseract;
    private final String dataPath;
    private final String language;

    public TesseractEngine(String dataPath, String language) {
        this.dataPath = dataPath;
        this.language = language;

        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(dataPath);  // parent folder containing tessdata
        this.tesseract.setLanguage(language);  // "eng" or "eng+spa" etc.

        log.info("Initialized TesseractEngine with datapath='{}', language='{}'",
                dataPath, language);
    }

    /**
     * Perform OCR on the given image and return the extracted text.
     */
    public String ocr(BufferedImage image) {
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            throw new RuntimeException("Tesseract OCR failed", e);
        }
    }

    /** Getter for logging / diagnostics */
    public String getLanguage() {
        return language;
    }

    /** Getter for logging / diagnostics */
    public String getDataPath() {
        return dataPath;
    }
}
