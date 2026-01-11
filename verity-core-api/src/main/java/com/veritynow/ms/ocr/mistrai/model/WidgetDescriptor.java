package com.veritynow.ms.ocr.mistrai.model;

public record WidgetDescriptor(
        Integer page,   // 1-based, nullable if unresolved
        float x,
        float y,
        float width,
        float height
) {}
