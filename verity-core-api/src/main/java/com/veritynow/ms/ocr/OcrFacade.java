package com.veritynow.ms.ocr;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
public class OcrFacade implements OcrService {

    
    private String mode;

    private final OcrService tesseract;
    private final OcrService docai;
    private final OcrService mistrai;
   
    public OcrFacade(
    		@Value("${verity.ocr.mode:tesseract}") String mode,
            @Qualifier("tesseractOcr") OcrService tesseract,
            @Qualifier("docAiOcr") OcrService docai,
            @Qualifier("mistraiOcr") OcrService mistrai
            
            
    ) {
        this.tesseract = tesseract;
        this.docai = docai;
        this.mistrai = mistrai;
        this.mode = mode;
    }

    @Override
    public OcrResponse extract(MultipartFile file) {
        return switch (mode.toLowerCase()) {
            case "docai" -> docai.extract(file);
            case "mistrai" -> mistrai.extract(file);
            default      -> tesseract.extract(file);
        };
    }
}