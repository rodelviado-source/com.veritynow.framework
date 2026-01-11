package com.veritynow.ms.ocr;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface OcrService {

    /**
     * Extracts structured fields from an uploaded image/PDF using the local OCR engine.
     * For now this is a stub that will later call normalization + Tesseract.
     */
    OcrResponse extract(MultipartFile file);
}
