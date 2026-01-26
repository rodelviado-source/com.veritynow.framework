package com.veritynow.ms.ocr.docai;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;

@Component
public class DocAIClient {

	private static final Logger LOGGER = LogManager.getLogger();
	
	@Value("${docai.projectId}")
	String projectId;
	@Value("${docai.location}")
	String location;
	@Value("${docai.processorId}")
	String processorId;

	
	
	
	public DocAIClient(
			@Value("${docai.projectId}")   String projectId,
            @Value("${docai.location}")    String location,
            @Value("${docai.processorId}") String processorId
            
    ) {
		this.projectId = projectId;
        this.location = location;
        this.processorId = processorId;
	}
	
	public ProcessResponse callDocAI(MultipartFile file) throws IOException {
		byte[] bs = file.getBytes();
		
		LOGGER.info("Performing OCR on {} with size = {}", file.getName(), bs.length);
		return callDocAI(bs, file.getName(), file.getContentType());
	}
	
	public ProcessResponse callDocAI(byte[] file, String filename, String contentType) {

		/**
		 * Uses Google Document AI Form Parser to extract fields, then maps them into
		 * our LocalOcrResponse structure.
		 */
		try {
			// 1) Build endpoint (e.g., "us-documentai.googleapis.com:443")
			String endpoint = String.format("%s-documentai.googleapis.com:443", location);
			DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
					.setEndpoint(endpoint).build();

			try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)) {

				// 2) Full processor name
				String name = String.format("projects/%s/locations/%s/processors/%s", projectId, location, processorId);

				// 3) Read file bytes into RawDocument
				ByteString content = ByteString.copyFrom(file);
			
				String mimeType = contentType;
				if (mimeType == null || mimeType.isBlank()) {
					// fallback, DocAI likes explicit mime types
					mimeType = "application/pdf";
				}

				
				RawDocument rawDoc = RawDocument.newBuilder().setContent(content).setMimeType(mimeType).build();

				ProcessRequest request = ProcessRequest.newBuilder().setName(name).setRawDocument(rawDoc).build();

				// 4) Call DocAI
				 return  client.processDocument(request);
				 

			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("DOCAI Client failed to perform OCR on " + filename);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException("DOCAI Client failed to perform OCR on " + filename);
		}
	}
}