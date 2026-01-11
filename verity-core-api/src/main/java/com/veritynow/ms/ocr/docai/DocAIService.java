package com.veritynow.ms.ocr.docai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.documentai.v1.ProcessResponse;
import com.veritynow.ms.ocr.OcrField;
import com.veritynow.ms.ocr.OcrResponse;
import com.veritynow.ms.ocr.OcrService;
import com.veritynow.ms.pdf.FormTemplate;
import com.veritynow.ms.pdf.FormTemplateLoader;
import com.veritynow.ms.pdf.PDFExtractorService;

@Service("docAiOcr")
public class DocAIService implements OcrService {

	DocAIClient client;
	private final FormTemplateLoader templateLoader;

	public DocAIService(DocAIClient client, FormTemplateLoader templateLoader) {
		this.client = client;
		this.templateLoader = templateLoader;
	}

	/**
     * Uses Google Document AI Form Parser to extract fields, then maps them
     * into our LocalOcrResponse structure.
     */
    @Override
    public OcrResponse extract(MultipartFile file) {
		try {
			
			FormTemplate t = templateLoader.load();
			if (t == null) {
				throw new RuntimeException("Failed to load template");
			}
			
			if ("application/pdf".equals(file.getContentType())) {
				 PDDocument doc = Loader.loadPDF(file.getBytes());
			     
				 Map<String, OcrField> results = new LinkedHashMap<String, OcrField>();
				 
				 Map<String, String> map = PDFExtractorService.extractFormFields(doc);
				 map.forEach((k,v) -> {
					 results.put(k, new OcrField(k, v, 1.0));
				 });
				 
				 return new OcrResponse(t.id(), "docai", results);
			}	 
			
			
			
			ProcessResponse response = client.callDocAI(file);
			if (response == null) {
				throw new RuntimeException("Docai responded with a null response");
			}
			
			
			
			Map<String, OcrField> results = DocAiOutputParser.parse(response, t, new ArrayList<String>());
			
			return new OcrResponse(t.id(), "docai", results);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
            
    }

}
