package com.veritynow.ms.pdf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ms/pdf")
public class PDFController {

    private final PDFExtractorService extractor;

    public PDFController(PDFExtractorService extractor) {
        this.extractor = extractor;
    }

    /**
     * Parse Form fields and extract available field attributes  .
     */
    @PostMapping(
        path = "/fields-info/parse",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, FieldInfo> fromUpload(
        @RequestPart("file") MultipartFile file
    ) throws Exception {
    	List<FieldInfo> l = extractor.extractFieldsInfo(file.getBytes());
    	Map<String, FieldInfo> result = new LinkedHashMap<String, FieldInfo>();
    	l.forEach((fi) -> result.put(fi.name(), fi) );
    	return result; 
    }
    
    /*
     * Parse Form fields as key-value pair 
     */
    @PostMapping(
    	    path = "/fields-key-value/parse",
    	    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    	    produces = MediaType.APPLICATION_JSON_VALUE
    	)
    	public Map<String, String> parse(
    	        @RequestPart("file") MultipartFile file
    	) throws Exception {

    	    PDFExtractorService extractor = new PDFExtractorService();
    	    byte[] bytes = file.getBytes();
    	    return extractor.extractFormFields(bytes);
    	}

}
