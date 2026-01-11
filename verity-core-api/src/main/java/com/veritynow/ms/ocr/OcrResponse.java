package com.veritynow.ms.ocr;

import java.util.Map;

public record OcrResponse (
		String templateId,
		String source,
		Map<String, OcrField> fields) {}

    

