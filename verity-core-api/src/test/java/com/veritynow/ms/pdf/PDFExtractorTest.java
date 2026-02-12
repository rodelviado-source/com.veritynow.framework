package com.veritynow.ms.pdf;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veritynow.ms.ocr.mistrai.schema.MistralSchemaFromTemplate;

/**
 * Extracts PDF form fields into a JSON-friendly structure suitable
 * for HTML form prefill.
 *
 * Output format:
 *   • Text fields → "FieldName" : "stringValue"
 *   • Checkboxes → "FieldName::OnValue" : true/false
 *   • Radios     → "FieldName::ExportValue" : true/false (one per option)
 *
 * You can:
 *   - Use the instance method {@link #extractForPrefill(byte[])} from controllers
 *   - Use the static method {@link #extractFromDocument(PDDocument)} offline,
 *     e.g. from tools, tests, or other code that already has a PDDocument
 */
public class PDFExtractorTest {

        
	
	public static void generateTemplate() throws Exception {
		Path path = Path.of("E:\\images\\FullyFilledUpSelect.pdf");
        //Path path = Path.of("E:\\images\\Blank-final.pdf");
        FileInputStream fis = new FileInputStream(path.toFile());
        PDDocument doc = Loader.loadPDF(fis.readAllBytes());
        fis.close();
        String s = PDFExtractorService.generateFormTemplate(
        		doc, 
        		UUID.nameUUIDFromBytes("BDOLoanForm".getBytes()).toString(), 
        		"BDOLoanForm", 
        		200f);
        System.out.println(s);
	}
	
	
	public static FormTemplate xgenerateTemplate() throws Exception {
		Path path = Path.of("E:\\images\\FullyFilledUpSelect.pdf");
        //Path path = Path.of("E:\\images\\Blank-final.pdf");
        FileInputStream fis = new FileInputStream(path.toFile());
        PDDocument doc = Loader.loadPDF(fis.readAllBytes());
        fis.close();
        return PDFExtractorService.xgenerateFormTemplate(
        		doc, 
        		UUID.nameUUIDFromBytes("BDOLoanForm".getBytes()).toString(), 
        		"BDOLoanForm", 
        		200f);
	}
	
	private static void generateFormKVPairs()  throws Exception {
		Path path = Path.of("E:\\images\\FullyFilledUpSelect.pdf");
        //Path path = Path.of("E:\\images\\Blank-final.pdf");
        FileInputStream fis = new FileInputStream(path.toFile());
        PDDocument doc = Loader.loadPDF(fis.readAllBytes());
        fis.close();
        
        Map<String, String> map = PDFExtractorService.extractFormFields(doc);

        System.out.println("=== Prefill map ===");
        map.forEach((k, v) -> System.out.println(k + " = " + v));
	}
	
	private static void generateFormKVPairsForFormParser()  throws Exception {
		Path path = Path.of("E:\\images\\FullyFilledUpSelect.pdf");
        //Path path = Path.of("E:\\images\\Blank-final.pdf");
        FileInputStream fis = new FileInputStream(path.toFile());
        PDDocument doc = Loader.loadPDF(fis.readAllBytes());
        fis.close();
        
        Map<String, String> map = PDFExtractorService.extractFormFieldsTypes(doc);

        System.out.println("=== Prefill map ===");
        map.forEach((k, v) -> System.out.println(k.replaceAll(" ", "_").replaceAll("::", ":") + " = " + v));
	}
	
	
	private static void generateMistraiTemplate()  throws Exception {
		
        
        ObjectNode t = MistralSchemaFromTemplate.buildResponseFormat(xgenerateTemplate()) ;
        		
        
        ObjectMapper om = new ObjectMapper();
		ObjectWriter w = om.writerWithDefaultPrettyPrinter();
		String s = w.writeValueAsString(t);
		System.out.println(s);
	}

    /**
     * Optional: tiny CLI for quick offline inspection.
     * Usage: java ... PdfPrefillExtractorService path/to/form.pdf
     * or Run as Java Application in your IDE
     */
    public static void main(String[] args) throws Exception {
    	generateMistraiTemplate();
    	generateFormKVPairs();
    	generateTemplate();
    	
    	generateFormKVPairsForFormParser();
    	
    }
}
