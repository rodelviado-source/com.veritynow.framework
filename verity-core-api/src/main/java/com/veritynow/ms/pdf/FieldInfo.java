package com.veritynow.ms.pdf;

import com.fasterxml.jackson.annotation.JsonIgnore;

	
public record FieldInfo(
	@JsonIgnore
    String name,
    String type, 
    @JsonIgnore				// e.g. "text", "checkbox", "radio"
    String value,        // current value as string (may be null/empty)
    Zone zone,        // [page, x, y, width, height] or null
    Integer maxLength,
    String subType,
    boolean required,
    boolean readOnly
) {
	
	
}
