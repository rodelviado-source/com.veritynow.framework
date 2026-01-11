package com.veritynow.ms.ocr.mistrai;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veritynow.ms.ocr.OcrField;
import com.veritynow.ms.ocr.OcrResponse;
import com.veritynow.ms.ocr.OcrService;
import com.veritynow.ms.ocr.mistrai.schema.MistralSchemaFromTemplate;
import com.veritynow.ms.pdf.FormTemplate;
import com.veritynow.ms.pdf.FormTemplateLoader;
import com.veritynow.ms.pdf.PDFExtractorService;

@Service("mistraiOcr")
public class MistralService implements OcrService {

    private final MistralClient client;
    
    private final FormTemplateLoader templateLoader;
    public MistralService(
            MistralClient client,
            FormTemplateLoader templateLoader,
            MistralSchemaFromTemplate schemaBuilder
    ) {
        this.client = client;
        this.templateLoader = templateLoader;
    }

    @Override
  
public OcrResponse extract(MultipartFile file) {
    try {
        // EXACTLY like DocAI
        FormTemplate template = templateLoader.load();
        if (template == null) {
            throw new IllegalStateException("FormTemplate not found");
        }

        String contentType = file.getContentType();

        // === PDF FAST PATH (AcroForm) ===
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            try (PDDocument doc = Loader.loadPDF(file.getBytes())) {

                Map<String, String> extracted =
                        PDFExtractorService.extractFormFields(doc);

                if (!extracted.isEmpty()) {
                    Map<String, OcrField> out = new LinkedHashMap<>();
                    extracted.forEach((k, v) ->
                        out.put(k, new OcrField(k, v, 1.0))
                    );
                    return new OcrResponse(template.id(), "mistral", out);
                }
                // else: scanned PDF → fall through to Mistral
            }
        }
        
        // === AI PATH (image OR scanned PDF) ===
        ObjectNode responseFormat = MistralSchemaFromTemplate.buildResponseFormat(template);

        JsonNode extracted =
                client.extractWithSchema(
                        file.getBytes(),
                        contentType,
                        responseFormat,
                        systemPrompt(),
                        userPrompt()
                );

        Map<String, OcrField> results =
                MistralOutputFlattener.flattenToOcrFields(extracted);

        return new OcrResponse(template.id(), "mistral", results);

    } catch (Exception e) {
        throw new RuntimeException("Mistral OCR failed", e);
    }
}


    private String systemPrompt() {
        return """
               You extract form values from a document image.
               Rules:
               - Use ONLY what is visible in the image.
               - If a value is not present or not legible, return null.
               - Do not infer or guess.
               - Follow the provided JSON schema exactly.
               """;
    }

    private String userPrompt() {
        // Keep it minimal; schema already defines keys and structure.
        return "Extract the form fields according to the schema.";
    }
}
