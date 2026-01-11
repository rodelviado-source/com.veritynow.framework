package com.veritynow.ms.ocr.mistrai.schema;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veritynow.ms.pdf.FieldInfo;
import com.veritynow.ms.pdf.FormTemplate;

import util.JSON;

@Component
public class MistralSchemaFromTemplate {

    public static ObjectMapper mapper = JSON.MAPPER;

    public static ObjectNode buildResponseFormat(FormTemplate tpl) {
        ObjectNode root = mapper.createObjectNode();

        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", "pdf_form_extraction_" + safeName(tpl.id()));
        jsonSchema.put("strict", true);

        ObjectNode schema = jsonSchema.putObject("schema");
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ArrayNode required = schema.putArray("required");
        required.add("templateId");
        required.add("values");
        required.add("choices");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode templateId = properties.putObject("templateId");
        templateId.put("type", "string");

        ObjectNode values = properties.putObject("values");
        values.put("type", "object");
        values.put("additionalProperties", false);
        ObjectNode valuesProps = values.putObject("properties");

        ObjectNode choices = properties.putObject("choices");
        choices.put("type", "object");
        choices.put("additionalProperties", false);
        ObjectNode choicesProps = choices.putObject("properties");

        Map<String, FieldInfo> fields = tpl.fields();
        if (fields != null) {
            fields.forEach((k, fi) -> {
                String type = (fi != null && fi.type() != null) ? fi.type().toLowerCase() : "text";

                if ("radio".equals(type) || "checkbox".equals(type)) {
                    addChoiceSchema(choicesProps, k, fi);
                } else {
                    addValueSchema(valuesProps, k, fi);
                }
            });
        }

        return root;
    }

    private static void addValueSchema(ObjectNode valuesProps, String key, FieldInfo fi) {
        ObjectNode node = valuesProps.putObject(key);

        // Use nullable string by default; this matches UI expectations and avoids over-constraining.
        ArrayNode types = node.putArray("type");
        types.add("string");
        types.add("null");

        if (fi != null && fi.maxLength() != null && fi.maxLength() > 0) {
            node.put("maxLength", fi.maxLength());
        }
    }

    private static void addChoiceSchema(ObjectNode choicesProps, String key, FieldInfo fi) {
        ObjectNode node = choicesProps.putObject(key);
        node.put("type", "object");
        node.put("additionalProperties", false);

        ObjectNode props = node.putObject("properties");

        ObjectNode selected = props.putObject("selected");
        ArrayNode types = selected.putArray("type");
        types.add("string");
        types.add("null");

        // NOTE: If/when you enhance your template to include explicit option enums,
        // add selected.enum = [null, ...options] here.
        // Today, FormTemplate/FieldInfo does not include option lists.

        ArrayNode req = node.putArray("required");
        req.add("selected");
    }

    private static String safeName(String s) {
        if (s == null || s.isBlank()) return "unknown";
        return s.replaceAll("[^A-Za-z0-9_\\-]+", "_");
    }
}
