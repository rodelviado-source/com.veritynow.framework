package com.veritynow.core.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.aspose.pdf.CheckboxField;
import com.aspose.pdf.ComboBoxField;
import com.aspose.pdf.Document;
import com.aspose.pdf.Field;
import com.aspose.pdf.HtmlSaveOptions;
import com.aspose.pdf.Page;
import com.aspose.pdf.PageCollection;
import com.aspose.pdf.RadioButtonField;
import com.aspose.pdf.RadioButtonOptionField;
import com.aspose.pdf.TextAbsorber;
import com.aspose.pdf.TextBoxField;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;





public class PDFUtil {
	
	public static String TEXT_BOX = TextBoxField.class.getSimpleName(); 
	public static String CHECK_BOX = CheckboxField.class.getSimpleName();
	public static String RADIO_BUTTON_OPTION = RadioButtonOptionField.class.getSimpleName();
	public static String COMBO_BOX = ComboBoxField.class.getSimpleName();
	
	/*
	@JsonAutoDetect(
		    setterVisibility = JsonAutoDetect.Visibility.NONE,
		    getterVisibility = JsonAutoDetect.Visibility.NONE,
		    isGetterVisibility = JsonAutoDetect.Visibility.NONE
		)
	*/
	class FieldMeta {
		
	
		private TextBoxField tbf;
		private ComboBoxField cbf;
		private RadioButtonOptionField rbof;
		private CheckboxField chkbf;

		private String name;
		private String fullName;
		private String activeState;
		private String alternateName;
		private String checkedStateName;
		private String contents;
		private String mappingName;
		private String partialName;
		private String value;
		private boolean multiline;
		private boolean multiSelect;
		private int selected;
		private int[] selectedItems;
		private String optionName;
		private boolean checked;
		private String   exportValue;
		private boolean exportable;
		private String normalCaption;
		private String onState;
		
		private String type;
		
		FieldMeta(Field field) {
			
			type = field.getClass().getSimpleName();
			
			name = field.getName();
			activeState = field.getActiveState();
			fullName = field.getFullName();
			alternateName = field.getAlternateName();
			checkedStateName = field.getCheckedStateName();
			contents = field.getContents();
			mappingName = field.getMappingName();
			partialName = field.getPartialName();
			value = field.getValue();
			name = field.getName();
			
			if (field instanceof TextBoxField) {
	      	    tbf = (TextBoxField) field;
	        } else if (field instanceof ComboBoxField) {
	      	    cbf = (ComboBoxField) field;
	      	 } else if (field instanceof RadioButtonOptionField) {
	      	    rbof = (RadioButtonOptionField) field;
	      	 } else if (field instanceof CheckboxField) {
	      		chkbf = (CheckboxField) field;
	      	 }
			
			
			if (tbf != null) {
				multiline = tbf.getMultiline();
			}
			
			if (cbf != null) {
				multiSelect = cbf.getMultiSelect();
				selected = cbf.getSelected();
				selectedItems = cbf.getSelectedItems();
			}
			
			if (rbof != null) {
				optionName = rbof.getOptionName();
			}	
		    
			if (chkbf != null) {
			    checked = chkbf.getChecked();
			    exportValue = chkbf.getExportValue();
			    exportable = chkbf.getExportable();
			    normalCaption = chkbf.getNormalCaption();
			    onState = chkbf.getOnState();
			}
		}
		
		
		 public String getTrueValue() {
			if (type.equals(CHECK_BOX)) {
				if (checked && getFullName().equalsIgnoreCase(value))  {
					 return value.substring(3);
				 }
				
				return checked ? value : null;
				
			} 
			
			if (type.equals(RADIO_BUTTON_OPTION)) {
				return "Off".equals(activeState) ?  null :  getOptionName();
			} 
			
			if (type.equals(COMBO_BOX)) {
				if ("Please Select".equalsIgnoreCase(value) || cbf.getSelected() <= 1) {
					return null;
				} else {
					return value;
				}
			}
			
			
			
			return value == null || value.isBlank() || value.isEmpty() ? null : value;
		}
		
		public String getType() {
			return type;
		}

		public String getFullName() {
			return fullName;
		}
		
		public Boolean getChecked() {
			return checked;
		}

		public String getActiveState() {
			return activeState;
		}

		public String getAlternateName() {
			return alternateName;
		}


		public String getCheckedStateName() {
			return checkedStateName;
		}

		public String getContents() {
			return contents;
		}

		public String getMappingName() {
			return mappingName;
		}

		public String getPartialName() {
			return partialName;
		}

		public String getValue() {
			return value;
		}
		
		public boolean isMultiline() {
			return multiline;
		}

		public boolean isMultiSelect() {
			return multiSelect;
		}

		public int getSelected() {
			return selected;
		}

		public int[] getSelectedItems() {
			return selectedItems;
		}

		public String getOptionName() {
			return optionName;
		}

		public boolean isChecked() {
			return checked;
		}

		public String getExportValue() {
			return exportValue;
		}

		public boolean isExportable() {
			return exportable;
		}

		public String getNormalCaption() {
			return normalCaption;
		}

		public String getOnState() {
			return onState;
		}

		public String getName() {
        	return name;
        }


		public boolean isCheckBox() {
			return type.equalsIgnoreCase(CHECK_BOX);
		}
		
		public boolean isTextBox() {
			return type.equalsIgnoreCase(TEXT_BOX);
		}
		
		public boolean isComboBox() {
			return type.equalsIgnoreCase(COMBO_BOX);
		}
		
		public boolean isRadioBoxOption() {
			return type.equalsIgnoreCase(RADIO_BUTTON_OPTION);
		}
	}
	
	
	public static  void setFieldsValue(RadioButtonField field, boolean on) {
		 FieldMeta fm = new PDFUtil().new FieldMeta(field);
			
		 if ( fm.isRadioBoxOption() && fm.getTrueValue() != null) {
			 field.setActiveState(on ? field.getFullName() :  "Off");
			 
		 }
		
	}
	
	public static  void setFieldsValue(TextBoxField field) {
		
	}
	
	public static  void setFieldsValue(CheckboxField field) {
		
	}
	
	public static  void setFieldsValue(ComboBoxField field) {
		
	}
	
	public static  Map<String, FieldMeta> getFieldsMeta(Document pdf) {
		Map<String, FieldMeta> map = new HashMap<String, FieldMeta>();  
		
		Field[] fields = pdf.getForm().getFields();
		
	      
		Arrays.asList(fields).forEach(field -> {
			 FieldMeta fm = new PDFUtil().new FieldMeta(field);
			
			 if (fm.getTrueValue() != null) {
				 map.put(fm.getFullName(), fm);
			 }
			 
		});
	      
		return map;
	}
	
	public static  Map<String, String> getFieldsValues(Document pdf) {
		Map<String, String> map = new HashMap<String, String>();  
		
		Field[] fields = pdf.getForm().getFields();
	      
		Arrays.asList(fields).forEach(field -> {
			 FieldMeta fm = new PDFUtil().new FieldMeta(field);
			
			 if (fm.getTrueValue() != null) {
				 
				 String value = fm.getTrueValue();
				 map.put(fm.getFullName(), value);
			 }
			 
		});
	      
		return map;
	}
	
	
	public static  void resetRadioButtons(Document pdf) {
		Field[] fields = pdf.getForm().getFields();
		Arrays.asList(fields).forEach(field -> {
			 FieldMeta fm = new PDFUtil().new FieldMeta(field);
			
			 if ( fm.isRadioBoxOption() && fm.getTrueValue() != null) {
				 field.setActiveState("Off");
				 
			 }
		});
	}
	
	public static  void resetCheckBoxes(Document pdf) {
		Field[] fields = pdf.getForm().getFields();
		Arrays.asList(fields).forEach(field -> {
			 FieldMeta fm = new PDFUtil().new FieldMeta(field);
			 if ( fm.isCheckBox() && fm.getTrueValue() != null) {
				((CheckboxField) field).setChecked(false);
			 }
		});
	}
	
	
	public static void initPdf(String pathToFile, String pathToExport, String pathToJson) {
       
        Document pdf = new Document(pathToFile);
        
        
       PageCollection pages = pdf.getPages();
    		   
	   
		Page page = pages.get_Item(1);
		  
	   
       
       page.getArtifacts().forEach(a -> {
    	   System.out.println(" rodel" + a.getImage().getNameInCollection());
    	});
       
      // BackgroundArtifact bga = new BackgroundArtifact();
       
           
        
       
        /*
        byte[] b = pdf.convertPageToPNGMemoryStream(pdf.getPages().get_Item(1));
        
        
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        try {
			bos.write(b);
			
			FileOutputStream fis = new FileOutputStream("E:\\images\\test.png");
			bos.writeTo(fis);
			
			bos.close();
			fis.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        */
        
        
        
        
        pdf.getEmbeddedFiles().forEach(fsp -> { System.out.println(fsp.getName() + " " + fsp.getMIMEType()); });
        //resetRadioButtons(pdf);
        
       Field[] fields = pdf.getForm().getFields();
       
     
       Set<String> uniqueSet = new HashSet<>();
       
       
	   Arrays.asList(fields).forEach(field -> {
          
          
          FieldMeta fm = new PDFUtil().new FieldMeta(field);
                     
          if (fm != null) {
        	  
        	  
        	  ObjectMapper om = new ObjectMapper();
        	  ObjectWriter ow = om.writer().withDefaultPrettyPrinter();
        	  om.setSerializationInclusion(Include.NON_NULL);
        	  
        	  try {
				String json = ow.writeValueAsString(fm);
				//System.out.println(json);
				if (field.getFullName().contains("_")) {
					field.setName(field.getFullName().replaceAll("_", " "));
					field.setPartialName(field.getFullName().replaceAll("_", " "));
					fm = new PDFUtil().new FieldMeta(field);
					json = ow.writeValueAsString(fm);
					System.out.println(json);
				} else if (!field.getFullName().startsWith("A")){
					fm = new PDFUtil().new FieldMeta(field);
					json = ow.writeValueAsString(fm);
					System.out.println(json);
				}
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	  
          }    
          uniqueSet.add(field.getClass().getSimpleName());
          
          
        
         // }		
       });
	   
	   
	   System.out.println(uniqueSet);
	   
	   
	   ObjectMapper om = new ObjectMapper();
	   om.setSerializationInclusion(Include.NON_NULL);
	   ObjectWriter ow = om.writer().withDefaultPrettyPrinter();
	   String json;
	try {
		json = ow.writeValueAsString(getFieldsValues(pdf));
		 System.out.println(json);
	} catch (JsonProcessingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	   
	   
        
     // Create TextAbsorber object to extract text
        TextAbsorber textAbsorber = new TextAbsorber();

        // Accept the absorber for all the pages
        pdf.getPages().accept(textAbsorber);

        // Get the extracted text
        String extractedText = textAbsorber.getText();

        // Create a writer and open the file
        //System.out.println(extractedText);

        
        //file.Save(stream);
        
     // Instantiate HTML SaveOptions object
        HtmlSaveOptions htmlOptions = new HtmlSaveOptions();

        // Specify to split the output into multiple pages
        htmlOptions.setSplitIntoPages(false);
    

        // Save the document
        
        
        //pdf.save("E:\\images\\BlankAspose.pdf");      
        
        
      
        pdf.close();

    }
	
	
	
	
    
    public static void main(String[] args) {
    	initPdf("E:\\images\\test123Updated.pdf", "", "");
    }

    
    /*
     * 
     
    void FillBoolField(Aspose.Pdf.Facades.Form f, string n, ScriptScopeContext scope)
    {
        //Console.WriteLine(n);
        try
        {
            var b = JS.eval(n, scope);

            if (b != null)
            {
                bool bb = b.ConvertTo<bool>();
                Console.WriteLine($"{n} - {bb}");
                f.FillField(n, (bool)b);
            }
            else
            {
                Console.WriteLine($"{n} didn't parse");
            }

        }
        catch(Exception ex)
        {
            Console.WriteLine("exception" + ex.Message);
        }

    }


    void FillTextField(Aspose.Pdf.Facades.Form f, string n, ScriptScopeContext scope)
    {

        try
        {
            var b = JS.eval(n, scope);

            if (b != null)
            {
                f.FillField(n, b.ToString());
            }
            else
            {
                Console.WriteLine($"{n} didn't parse");
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine("exception" + ex.Message);
        }

    }

    public class CustomMethods : ScriptMethods
{
    public string toCustomString(object text) => text != null ? text.ToString() : string.Empty;
    public string toCustomDate(string text) {
        return DateTime.Parse(text).ToString("dd MMM yyyy");
    }
}

*/
    
}
