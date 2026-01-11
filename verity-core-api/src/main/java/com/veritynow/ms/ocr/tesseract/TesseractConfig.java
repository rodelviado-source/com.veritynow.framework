package com.veritynow.ms.ocr.tesseract;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for OCR-related beans.
 */
@Configuration
public class TesseractConfig {

    @Bean
    public TesseractEngine tesseractEngine(
            @Value("${ocr.tesseract.datapath}") String dataPath,
            @Value("${ocr.tesseract.language:eng}") String language
    ) {
        return new TesseractEngine(dataPath, language);
    }

    // DocumentNormalizer is provided by BasicDocumentNormalizer @Component
    // so no explicit bean needed unless you want conditional profiles, etc.
}
