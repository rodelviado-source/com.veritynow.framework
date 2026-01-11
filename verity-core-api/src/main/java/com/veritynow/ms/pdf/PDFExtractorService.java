package com.veritynow.ms.pdf; // ← adjust to your microservice package

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDListBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Service
public class PDFExtractorService {

	/**
	 * Entry point used by controllers.
	 */
	public Map<String, String> extractFormFields(byte[] pdfBytes) throws IOException {
		try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
			return extractFormFields(doc);
		}
	}

	public List<FieldInfo> extractFieldsInfo(byte[] pdfBytes) throws IOException {
		try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
			return extractFieldsInfo(doc);
		}
	}
	
	/**
	 * Extracts PDF form fields into a JSON-friendly key-value structure suitable for HTML
	 * form prefill.
	 *
	 * Output format: 
	 * • Text fields → "FieldName" : "stringValue" 
	 * • Checkboxes →  "FieldName::value" : true/false (one for each member of the group)
	 * • Radios →      "FieldName::value" : true/false (one for each member of the group)
	 *
	 * This eliminates all client-side mapping complexity.
	 */
	public static Map<String, String> extractFormFields(PDDocument doc) throws IOException {
		List<FieldInfo> ff = extractFieldsInfo(doc);
		Map<String, String> res = new LinkedHashMap<String, String>() ;
		ff.forEach((f) -> {res.put(f.name(), f.value());} );
		return res;
	}
	
	public static Map<String, String> extractFormFieldsTypes(PDDocument doc) throws IOException {
		List<FieldInfo> ff = extractFieldsInfo(doc);
		Map<String, String> res = new LinkedHashMap<String, String>() ;
		ff.forEach((f) -> {res.put(f.name(), f.type() + ":" + f.subType() + ":" + f.maxLength() );} );
		return res;
	}
	
	public static String generateFormTemplate(PDDocument doc, String id, String name, float dpi) throws IOException {
		List<FieldInfo> ff = extractFieldsInfo(doc);
		Map<String, FieldInfo> out = new LinkedHashMap<String, FieldInfo>();
		
		ff.sort((f1, f2) -> f1.name().compareTo(f2.name()));
		ff.forEach((f) -> out.put(f.name(), f) );
		
		FormTemplate ft = new FormTemplate(id, name, dpi, doc.getNumberOfPages(),  out );
		ObjectMapper om = new ObjectMapper();
		ObjectWriter w = om.writerWithDefaultPrettyPrinter();
		return  w.writeValueAsString(ft);
	}
	
	public static FormTemplate xgenerateFormTemplate(PDDocument doc, String id, String name, float dpi) throws IOException {
		List<FieldInfo> ff = extractFieldsInfo(doc);
		Map<String, FieldInfo> out = new LinkedHashMap<String, FieldInfo>();
		
		ff.sort((f1, f2) -> f1.name().compareTo(f2.name()));
		ff.forEach((f) -> out.put(f.name(), f) );
		
		return new FormTemplate(id, name, dpi, doc.getNumberOfPages(),  out );
		
	}
	
	
	public static String generateMistrAIResponseJson(PDDocument doc, String id, String name, float dpi) throws IOException {
		List<FieldInfo> ff = extractFieldsInfo(doc);
		Map<String, FieldInfo> out = new LinkedHashMap<String, FieldInfo>();
		
		ff.sort((f1, f2) -> f1.name().compareTo(f2.name()));
		ff.forEach((f) -> out.put(f.name(), f) );
		
		FormTemplate ft = new FormTemplate(id, name, dpi, doc.getNumberOfPages(),  out );
		ObjectMapper om = new ObjectMapper();
		ObjectWriter w = om.writerWithDefaultPrettyPrinter();
		return  w.writeValueAsString(ft);
	}

	/**
	 * Internal extractor that walks the entire AcroForm field tree.
	 * Extracting Information from form fields 
	 * 
	 * @see FieldInfo for extracted attributes
	 * 
	 */
	public static List<FieldInfo> extractFieldsInfo(PDDocument doc) throws IOException {
		List<FieldInfo> result = new ArrayList<>();

		
		PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
		if (acroForm == null)	return result;

		doc.getNumberOfPages();
		
		for (PDField field : acroForm.getFieldTree()) {

			String name = field.getFullyQualifiedName();
			
			if (name == null || name.isBlank()) {
				continue;
			}	
			
			// --- 1) COMBOBOX / LISTBOX---
			if (handleChoice(field, doc, result)) {
				continue;
			}

			// --- 2) CHECKBOX FIELDS /  RADIO BUTTON FIELDS
			if (field instanceof PDButton b) {
				//Expand w.r.t. to options, since PDBBOX always gets this right.
				//The assumption is that all option is in this list otherwise it can't be toggled
				for (String ov : b.getOnValues()) {
					String nameov = name + "::" + ov; 
					result.add(newFieldInfo(nameov, null, ov, field, doc));
				}
				continue;
			}
			
			//--- 3) TEXT FIELDS ---
			if (field instanceof PDTextField) {
				result.add(newFieldInfo(name, null, null, field, doc));
				continue;
			}
			
			// 4) SIGNATURE FIELDS ---
			if (field instanceof PDSignatureField sf) {
				PDSignature s = sf.getSignature();
				byte[] b = s.getContents() ;
				String value = null;
				if (b != null) {		
					value = Base64.getEncoder().encodeToString(b);
				}
				result.add(newFieldInfo(name, value, null, field, doc));
				continue;
			}
		
			//Unhandled type
			System.out.println("----- Unhandled PDF type found -----------");
			System.out.println(name + " -- " +  field.getClass().toString() +" --");
			result.add(newFieldInfo(name, null, null, field, doc));
		}

		result.sort((f1, f2) -> {return f1.name().compareTo(f2.name());});
		return result;
	}



	private static boolean handleChoice(PDField field, PDDocument doc, List<FieldInfo> result) {

		String name = field.getFullyQualifiedName();
		if (name == null || name.isBlank())
			return false;

		if (field instanceof PDChoice choice) {
			String normalized = normalizedListToString(choice.getValue());
			result.add(newFieldInfo(name, normalized, null, field, doc));
			return true;
		} 
		return false;
	}
	
	private static String normalizedListToString(List<String> l) {
		String normalized = "";
		if (l != null) {
			List<String> clean = new ArrayList<String>();
			clean.addAll(l);
			clean.removeIf(e -> Objects.isNull(e) || "".equals(e));
			if (!clean.isEmpty()) {
				normalized = String.join(", ", clean);
			}
		}
		return normalized;
	}
	
	private static String getFieldType(PDField field) {
		
		if (field instanceof PDCheckBox) return "checkbox";
		if (field instanceof PDRadioButton) return "radio";
		if (field instanceof PDPushButton) return "pushbutton";
		if (field instanceof PDComboBox) return "combobox";
		if (field instanceof PDListBox) return "listbox";
		if (field instanceof PDTextField) return "text";
		if (field instanceof PDSignatureField) return "signature";
		// fallback
		return field.getClass().getSimpleName();
	}
	
	//For radiobutton and checkbox, name must be of the form fullyQualifiedName::option
	//Setting normalizedValue will override the getValue() with constraints summarized below:
	//if the field is a button, normalizedValue must be of boolean form 
	// "Yes/No", "On/Off", "true/false", regardless of form the final result is normalized to "true/false"
	
	//if option is supplied normalizedValue is ignored and during 
	//comparison option.equals(getValue()) no prior normalization was done, 
	//meaning what was passed is the one compared
	private static FieldInfo newFieldInfo(String name, String normalizedValue, String option, PDField field, PDDocument doc) {
		
		String value = field.getValueAsString();
		if (field instanceof PDButton pd) {
			if (option != null) {
				if (!name.equals(field.getFullyQualifiedName() + "::" + option)) {
					System.out.println("This will break downstream parsers please fix your code"); 
					System.out.println("Supplied name '" + name + "'  is not equal to required '" + field.getFullyQualifiedName() + "::" + option + "'");
				}
				value = Boolean.toString(option.equals(pd.getValue()));
			} else if (normalizedValue != null){
				value = normalizedBoolean(normalizedValue);
			}
		} else if (normalizedValue != null){
			value = normalizedValue;
		}
		Integer maxLength = -1;
		COSDictionary d = field.getCOSObject();
		if (d != null) {
		  maxLength = d.getInt(COSName.MAX_LEN);
		} 
		
		String fieldType = 	getFieldType(field);	
		
		
		return new FieldInfo(
				name, 
				fieldType, 
				value, 
				getZone(field, doc, option),
				maxLength,
				FieldSubTypeDetector.detectSubType(name, fieldType),
				field.isRequired(), 
				field.isReadOnly() );
	}
	
	private static String normalizedBoolean(String v) {
		String value = Boolean.FALSE.toString();
		 if (v != null && !v.isBlank()){
			switch (v.toLowerCase().trim()) {
			case "true"  : value = Boolean.TRUE.toString();  break;
			case "false" : value = Boolean.FALSE.toString(); break;
			case "yes"   : value = Boolean.TRUE.toString();  break;
			case "no"    : value = Boolean.FALSE.toString(); break;
			case "on"    : value = Boolean.TRUE.toString();  break;
			case "off"   : value = Boolean.FALSE.toString(); break;
			default      : value = Boolean.FALSE.toString();
			}
		}
		return value;
	}
	private static Zone getZone(PDField field, PDDocument doc, String option) {
		Zone rectArr = null;
		
		if (field instanceof PDButton &&  option != null) {
			PDAnnotationWidget w = getRadioOptionWidget(field.getWidgets(), option);
			if (w != null) {
				PDRectangle r = w.getRectangle();
				if (r != null) {
					Integer page = -1;
					PDPage p = w.getPage();
					if (p != null) {
						int pageIndex = doc.getPages().indexOf(p);
						page = pageIndex >= 0 ? pageIndex + 1 : -1;
					}
					
					rectArr = new Zone ( page , r.getLowerLeftX(), r.getLowerLeftY(), r.getWidth(), r.getHeight() );
				}
			}
		}
		
		
		// field is not a Button or 
		// it is a Button but don't have an option match
		// fallback
		if (rectArr == null) {
			// Best-effort: first widget defines page/rect
			for (PDAnnotationWidget w : field.getWidgets()) {
				PDRectangle r = w.getRectangle();
				if (r != null && rectArr == null) {
					Integer page = -1;
					PDPage p = w.getPage();
					if (p != null) {
						int pageIndex = doc.getPages().indexOf(p);
						page = pageIndex >= 0 ? pageIndex + 1 : -1;
					}
					rectArr = new Zone ( page , r.getLowerLeftX(), r.getLowerLeftY(), r.getWidth(), r.getHeight() );
					break;
				}
			}
		}
		return rectArr;
	}
	
	
	
	
	
	private static PDAnnotationWidget getRadioOptionWidget(List<PDAnnotationWidget> widgets, String option) {
		
		for (PDAnnotationWidget widget : widgets) {
	        COSDictionary ap = widget.getCOSObject().getCOSDictionary(COSName.AP);
	        if (ap == null) return null;  
	        COSDictionary normal = ap.getCOSDictionary(COSName.N);
	        if (normal == null) return null;
	
	        for (COSName key : normal.keySet()) {
	            String val = key.getName();
	            if (val.equals(option)) return widget;
	        }
		}
		return null;
    }
}
