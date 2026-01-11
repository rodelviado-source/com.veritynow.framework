package com.veritynow.ms.pdf;

import java.util.Map;

public record FormTemplate(
	String id,
	String name,
	float dpi,
	int totalPages,
	Map<String, FieldInfo> fields	
) {

}
