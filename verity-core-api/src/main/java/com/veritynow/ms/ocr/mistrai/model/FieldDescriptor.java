package com.veritynow.ms.ocr.mistrai.model;

import java.util.List;

public record FieldDescriptor(
        String key,                 // canonical stable key for schema + output
        String pdfName,              // full PDF field name
        String type,                 // text, checkbox, radio, combobox, listbox, signature
        String subType,              // from FieldSubTypeDetector
        Integer maxLength,
        boolean required,
        boolean readOnly,
        List<WidgetDescriptor> widgets,
        List<OptionDescriptor> options // only for radio/checkbox/choice
) {}
