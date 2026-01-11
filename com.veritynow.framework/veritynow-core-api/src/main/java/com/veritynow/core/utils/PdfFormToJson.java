package com.veritynow.core.utils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDFieldTree;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PdfFormToJson {

    public static void main(String[] args) {
    	String pdfFilePath = "E:\\images\\FullyFilledUp.pdf";

         
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonRootNode = objectMapper.createObjectNode();
        
        List<String> l = new LinkedList<>();
        try (PDDocument document = Loader.loadPDF(new File(pdfFilePath))) {
        	pdfToImage(document);
        	
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            	
            if (acroForm != null) {
                PDFieldTree fields = acroForm.getFieldTree();
                for (PDField field : fields) {
                    String fieldName = field.getPartialName();
                    String fieldValue = field.getValueAsString();
                    if (fieldName != null ) {
                    	fieldName = fieldName.substring(3).replace(" ","_").toLowerCase();
                    	
                    	l.add(fieldName);
                    	
                        jsonRootNode.put(fieldName, fieldValue);
                    }
                }
            } else {
                System.out.println("No AcroForm found in the PDF.");
            }
            
            Collections.sort(l);
            
            System.out.println(l.size());
            l.forEach(e -> {
            	System.out.println(e);
            });
            System.out.println(l);
            
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonRootNode));

        } catch (IOException e) {
            System.err.println("Error processing PDF: " + e.getMessage());
        }
    }
    
    
    public static void pdfToImage(PDDocument document) {
    	PDFRenderer pdfRenderer = new PDFRenderer(document);
    	
    	for (int page = 0; page < document.getNumberOfPages(); ++page)
    	{
    		BufferedImage bim;
    		
    		try  { 
    			bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
    			ImageIOUtil.writeImage(bim, "E:\\images\\rodel-test-" + (page+1) + ".png", 300);
    		} catch (Exception e) {
    			
    		}
    		
    	}
    }
}