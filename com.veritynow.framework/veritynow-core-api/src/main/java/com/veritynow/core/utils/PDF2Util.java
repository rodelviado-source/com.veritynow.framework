package com.veritynow.core.utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.commons.utils.Base64.OutputStream;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.svg.converter.SvgConverter;

public class PDF2Util {
	
	

	    public static final String SRC = "E:\\images\\test123.pdf";
	    public static final String DEST = "E:\\images\\test123Updated.pdf";
	    
	    
	    
	    
	    
	    
	    
	
	    
	    
	    
	    
	    
	    
	    
	    

	    public static void main(String[] args) {
	        // Create a PdfReader for the source PDF and a PdfWriter for the output
	    	File f = new File(DEST);
	    	if (f.exists()) {
	    		f.delete();
	    	} 
	    	
	    	
	    		try {
					f.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	
	    	
	    	
	    	
	    	try ( 
	        		
	        		
	        		FileOutputStream outStrem = new FileOutputStream(f);
	        		PdfReader reader = new PdfReader(SRC);    
	        		PdfWriter writer = new PdfWriter(outStrem);   
	        		PdfDocument pdfDoc = new PdfDocument(reader, writer)) {

	            // Iterate over every page in the existing PDF
	            int numberOfPages = pdfDoc.getNumberOfPages();
	            for (int i = 1; i <= numberOfPages; i++) {
	            	
	            	
	            	PdfFormXObject svgForm;
	 	            try (FileInputStream svgStream = new FileInputStream("E:\\images\\background\\" + i + ".svg")) {
	 	           
	 	                svgForm = SvgConverter.convertToXObject(svgStream, pdfDoc);
	 	            }
	            	
	            	//Image img = new Image(ImageDataFactory.create("E:\\images\\background\\" + i + ".png"));
	                PdfPage page = pdfDoc.getPage(i);
	                Rectangle pageSize = page.getPageSize();

	                // Get a PdfCanvas that allows drawing "under" the existing content
	                PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);

	                // Add the SVG to the canvas, scaling it to fit the whole page
	                
	           	                
	                canvas.addXObject(svgForm);
	                //canvas.addXObject(svgForm, pageSize.getWidth(), 0, 0, pageSize.getHeight(), 0, 0);
	            }
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        
	        
	    }
	}



