package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import io.github.se_be.pdf2dom.PDFDomTree;

public class PDF2HTML {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		File f = new File("E:\\images\\Blank-final.pdf");

		// load the PDF file using PDFBox
		try (FileInputStream is = new FileInputStream(f)) {
			try (PDDocument pdf = Loader.loadPDF(is.readAllBytes());
					Writer output = new PrintWriter("E:\\images\\pdf2pdf.html", "utf-8");

			) {

				new PDFDomTree().writeText(pdf, output);
				pdf.close();

			} catch (Exception e) {

			}
		}

	}

}
