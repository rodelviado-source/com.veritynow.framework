package com.veritynow.ms.ocr.mistrai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class MistralClient {

    private static final URI MISTRAL_URI = URI.create("https://api.mistral.ai/v1/chat/completions");

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String model;

    public MistralClient(
            @Value("${mistral.apiKey}") String apiKey,
            @Value("${mistral.model:mistral-small-latest}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Calls Mistral with:
     * - Vision input (base64 image_url)
     * - response_format json_schema (strict)
     * Returns the parsed JSON that conforms to the schema.
     */
    public JsonNode extractWithSchema(
            byte[] fileBytes,
            String contentType,
            JsonNode responseFormatWrapper,
            String systemPrompt,
            String userText
    ) throws Exception {

        String mime = (contentType == null || contentType.isBlank())
                ? "image/png"
                : contentType;

        // Mistral vision docs use "image_url" with base64 data URI.
        String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(fileBytes);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", model);

        ArrayNode messages = payload.putArray("messages");

        // system
        messages.add(obj("role", "system").put("content", systemPrompt));

        // user with multimodal content
        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        ArrayNode content = userMsg.putArray("content");

        ObjectNode textPart = mapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", userText);

        ObjectNode imgPart = mapper.createObjectNode();
        imgPart.put("type", "image_url");
        imgPart.put("image_url", dataUri);

        content.add(textPart);
        content.add(imgPart);

        messages.add(userMsg);

        // IMPORTANT: our schema builder returns { "response_format": {...} }
        payload.set("response_format", responseFormatWrapper.get("response_format"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(MISTRAL_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Mistral API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());

        // In chat/completions, the model response is typically in choices[0].message.content.
        // With json_schema mode, content should be valid JSON matching the schema. :contentReference[oaicite:3]{index=3}
        String contentJson = root.path("choices").get(0).path("message").path("content").asText();

        return mapper.readTree(contentJson);
    }

    private ObjectNode obj(String k, String v) {
        ObjectNode o = mapper.createObjectNode();
        o.put(k, v);
        return o;
    }
}
