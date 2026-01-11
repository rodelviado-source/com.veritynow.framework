package com.veritynow.ms.ocr.mistrai.model;

import java.util.List;

public record TemplateDescriptor(
        String id,
        String name,
        float dpi,
        int totalPages,
        List<FieldDescriptor> fields
) {}
