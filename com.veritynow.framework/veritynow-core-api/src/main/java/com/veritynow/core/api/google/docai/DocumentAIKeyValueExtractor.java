package com.veritynow.core.api.google.docai;

import java.io.FileInputStream;
import java.io.IOException;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;

public class DocumentAIKeyValueExtractor {

    public static void extractKeyValuePairs(
            String projectId, String location, String processorId, String filePath) throws IOException {

        try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create()) {
            String name = String.format("projects/%s/locations/%s/processors/%s", projectId, location, processorId);

            System.out.println("Processing document: " + filePath);

            FileInputStream fis = new FileInputStream(filePath);
            byte[] fileBytes = fis.readAllBytes();
            fis.close();
            
            ByteString content = ByteString.copyFrom(fileBytes);

            RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent(content)
                    .setMimeType("application/pdf")
                    .build();

            ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(name)
                    .setRawDocument(rawDocument)
                    .build();

            ProcessResponse result = client.processDocument(request);
            Document document = result.getDocument();

            System.out.println("Document processed successfully.");

            System.out.println("\nExtracted Key-Value Pairs:");
            // Check if there are any pages to process form fields from
            if (document.getPagesCount() > 0) {
                for (Document.Page.FormField field : document.getPages(0).getFormFieldsList()) {
                    // Pass the TextAnchor directly to the helper method
                    String fieldName = getText(field.getFieldName().getTextAnchor(), document);
                    String fieldValue = getText(field.getFieldValue().getTextAnchor(), document);

                    System.out.printf("  Key: %s, Value: %s\n", fieldName, fieldValue);
                }
            } else {
                System.out.println("No pages found in the document, cannot extract form fields.");
            }


            if (document.getEntitiesCount() > 0) {
                System.out.println("\nExtracted Entities:");
                for (Document.Entity entity : document.getEntitiesList()) {
                    // Removed hasMentionText(). getMentionText() will return an empty string if not set.
                    System.out.printf("  Type: %s, Value: %s, MentionText: %s\n",
                            entity.getType(), // This is the entity type
                            entity.hasNormalizedValue() ? entity.getNormalizedValue().getText() : "", // For entities that might have a normalized value (e.g., dates)
                            entity.getMentionText()); // This is the text as it appears in the document
                }
            }


        }
    }

    // Helper function to extract text from a Document.TextAnchor
    // Note: The TextAnchor class is directly nested within the Document class.
    private static String getText(Document.TextAnchor textAnchor, Document document) {
        if (textAnchor == null || textAnchor.getTextSegmentsCount() == 0) {
            return "";
        }
        // Assuming single text segment for simplicity, adapt if multiple segments are possible
        long startIndex = textAnchor.getTextSegments(0).getStartIndex();
        long endIndex = textAnchor.getTextSegments(0).getEndIndex();
        return document.getText().substring((int) startIndex, (int) endIndex);
    }

    public static void main(String[] args) {
        // TODO(developer): Replace these variables with your project specific values
        String projectId = "inspired-truth-474512-r8";
        String location = "us"; // e.g., "us", "eu"
        String processorId = "69a0bdee9f7c187c"; // Found in the Document AI console
        String filePath = "E:\\images\\Rodel.pdf"; // Path to your document file

        try {
            extractKeyValuePairs(projectId, location, processorId, filePath);
        } catch (IOException e) {
            System.err.println("Error processing document: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
