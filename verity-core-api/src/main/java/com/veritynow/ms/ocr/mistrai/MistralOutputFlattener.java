package com.veritynow.ms.ocr.mistrai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.veritynow.ms.ocr.OcrField;

public final class MistralOutputFlattener {

    private MistralOutputFlattener() {
    }

    public static Map<String, OcrField> flattenToOcrFields(JsonNode extracted) {
        Map<String, OcrField> out = new LinkedHashMap<>();

        if (extracted == null || extracted.isNull())
            return out;

        JsonNode values = extracted.get("values");
        if (values != null && values.isObject()) {
            Set<Entry<String, JsonNode>> it = values.properties();
            it.stream().forEach(e -> {
                String k = e.getKey();
                JsonNode v = e.getValue();
                out.put(
                        k, new OcrField(k, v == null || v.isNull() ? "" : v.asText(), 1.0));
            });
        }

        JsonNode choices = extracted.get("choices");
        if (choices != null && choices.isObject()) {
            Set<Map.Entry<String, JsonNode>> it = choices.properties();
            it.stream().forEach(e -> {
                String k = e.getKey();
                JsonNode group = e.getValue();
                JsonNode sel = (group != null) ? group.get("selected") : null;
                out.put(k, new OcrField(k, sel == null || sel.isNull() ? "" : sel.asText(), 1.0));
            });
        }

        return out;
    }
}
