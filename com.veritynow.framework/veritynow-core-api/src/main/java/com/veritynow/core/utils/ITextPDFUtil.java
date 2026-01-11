package com.veritynow.core.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfLiteral;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfOutputStream;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfResources;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.IContentOperator;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

public class ITextPDFUtil {
	
  static public PdfDocument golablpdf;
  static public Boolean tfRemoved = false;

  public static final String SRC = "E:\\images\\test123.pdf";
  public static final String DEST = "E:\\images\\test123Updated.pdf";
  

  public static void main(String[] args) throws Exception {
    String src = "input.pdf";
    String dest = "output.no-bg.pdf";
    
    removeAllImages(SRC, DEST);
  }

  

  /** Removes *all* images (if that’s what you need instead of only backgrounds). */
  public static void removeAllImages(String src, String dest) throws IOException {
	  
    try (PdfReader reader = new PdfReader(src);
         PdfWriter writer = new PdfWriter(new FileOutputStream(dest));
         PdfDocument pdf = new PdfDocument(reader, writer)) {
    	
    	PdfCanvasEditor editor = new PdfCanvasEditor() {
    		
    	protected boolean tfFound = false;	
    		
        @Override
        protected void write(PdfCanvasProcessor proc, PdfLiteral operator, List<PdfObject> operands) {
       
        	
        	if ("TJ".equalsIgnoreCase(operator.toString()) || "Tf".equalsIgnoreCase(operator.toString()))  {
        	
            
        	if (tfFound && "TJ".equalsIgnoreCase(operator.toString())) {
    			tfFound = false;
    			System.out.println("=============REMOVING==============");
    			System.out.println(operator.toString() + " : " + operands);
    			return;
    		}
    		
	    	Iterator<PdfObject> it = operands.iterator();
	    	
	    	while (it.hasNext()) {
	    		
	    		PdfObject o = it.next();
	               		
	    		if (o.isName()) {
	    			PdfName nam = (PdfName) o;
	    			
	    			if ("C0_0".equals(nam.getValue())) {
	    
	    				tfFound = true;
	    				System.out.println("===============FOOUND====================");
	    				System.out.println(operator.toString() + " : " + operands);
	    				
	    				return;

	    			}	
	    		}
	    	}
            	
        }
          super.write(proc, operator, operands);
        }
      };

      for (int p = 1; p <= pdf.getNumberOfPages(); p++) {
        editor.editPage(pdf, p);
      }
    }
  }

  // == Implementation details ==

  /** Minimal content-stream editor for iText7; forwards operators unless we skip them. */
  static class PdfCanvasEditor extends PdfCanvasProcessor {
    private PdfCanvas canvas;
    private PdfResources resources;

    public PdfCanvasEditor() {
      super(new DummyListener());
    }

    public void editPage(PdfDocument pdfDocument, int pageNumber) throws IOException {
      if (pdfDocument.getReader() == null || pdfDocument.getWriter() == null) {
        throw new PdfException("PdfDocument must be opened in stamping mode.");
      }
      PdfPage page = pdfDocument.getPage(pageNumber);
      this.resources = page.getResources();
      PdfCanvas target = new PdfCanvas(new PdfStream(), resources, pdfDocument);
      this.canvas = target;
      processContent(page.getContentBytes(), resources);
      this.canvas = null;
      this.resources = null;
      page.put(PdfName.Contents, target.getContentStream());
    }

    protected PdfResources getCurrentResources() { return resources; }

    /** Default behavior: write operators verbatim. Override to filter. */
    protected void write(PdfCanvasProcessor processor, PdfLiteral operator, List<PdfObject> operands) {
      PdfOutputStream out = canvas.getContentStream().getOutputStream();
      for (int i = 0; i < operands.size(); i++) {
        out.write(operands.get(i));
        if (i + 1 < operands.size()) out.writeSpace(); else out.writeNewLine();
      }
    }

    @Override
    public IContentOperator registerContentOperator(String opString, IContentOperator operator) {
      ContentOperatorWrapper wrapper = new ContentOperatorWrapper(operator);
      IContentOperator prev = super.registerContentOperator(opString, wrapper);
      return prev instanceof ContentOperatorWrapper ? ((ContentOperatorWrapper) prev).getOriginalOperator() : prev;
    }

    class ContentOperatorWrapper implements IContentOperator {
      private final IContentOperator original;
      ContentOperatorWrapper(IContentOperator original) { this.original = original; }
      IContentOperator getOriginalOperator() { return original; }
      @Override public void invoke(PdfCanvasProcessor proc, PdfLiteral op, List<PdfObject> operands) {
        if (original != null && !"Do".equals(op.toString())) original.invoke(proc, op, operands);
        write(proc, op, operands);
      }
    }

    static class DummyListener implements IEventListener {
      @Override public void eventOccurred(IEventData data, EventType type) {}
      @Override public java.util.Set<EventType> getSupportedEvents() { return null; }
    }
  }

  
}
