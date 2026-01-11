package com.veritynow.ms.ocr.mistrai.model;

import java.util.List;

public record OptionDescriptor(
        String value,                    // export value / label
        List<WidgetDescriptor> widgets
) {}
