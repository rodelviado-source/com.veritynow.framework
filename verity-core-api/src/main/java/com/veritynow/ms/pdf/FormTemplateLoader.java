package com.veritynow.ms.pdf;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import util.JSON;

@Component
public class FormTemplateLoader {

	@Value("${verity.pdf.template.path}")
	String path;
	
    public FormTemplate load() {
        try {

            InputStream in = this.getClass().getResourceAsStream(path);
            if (in == null) {
                throw new RuntimeException("OCR template not found: " + path);
            }

            return JSON.MAPPER.readValue(in, FormTemplate.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed loading OCR template config", e);
        }
    }
}
