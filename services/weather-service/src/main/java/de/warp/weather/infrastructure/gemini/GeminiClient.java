// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.infrastructure.gemini;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class GeminiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiClient.class);
    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public boolean isConfigured() {
        return properties.hasApiKey();
    }

    public String configuredModel() {
        return properties.model();
    }

    public GeminiStructuredOutput generateStructuredAnalysis(
            String systemInstructionText,
            String userPromptWithMcpContext,
            Map<String, Object> responseSchema
    ) {
        if (!isConfigured()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        List<String> candidateModels = buildCandidateModels();
        RuntimeException lastException = null;

        for (int i = 0; i < candidateModels.size(); i++) {
            String candidateModel = candidateModels.get(i);
            Map<String, Object> payload = buildGenerateContentPayload(
                    candidateModel,
                    systemInstructionText,
                    userPromptWithMcpContext,
                    responseSchema
            );
            try {
                JsonNode root = invokeModel(candidateModel, payload);
                return extractStructuredJson(root, candidateModel);
            } catch (RestClientResponseException ex) {
                lastException = ex;
                int status = ex.getStatusCode().value();
                if ((status == 404 || status == 429 || status == 503) && i + 1 < candidateModels.size()) {
                    LOGGER.info(
                            "Gemini model {} returned {}, falling back to {}",
                            candidateModel,
                            status,
                            candidateModels.get(i + 1)
                    );
                    continue;
                }
                throw ex;
            } catch (RuntimeException ex) {
                lastException = ex;
                if (i + 1 < candidateModels.size()) {
                    LOGGER.info(
                            "Gemini model {} failed ({}), falling back to {}",
                            candidateModel,
                            ex.getMessage(),
                            candidateModels.get(i + 1)
                    );
                    continue;
                }
                throw ex;
            }
        }

        throw lastException != null
                ? lastException
                : new IllegalStateException("No Gemini candidate models available");
    }

    private List<String> buildCandidateModels() {
        Set<String> models = new LinkedHashSet<>();
        if (properties.model() != null && !properties.model().isBlank()) {
            models.add(properties.model().trim());
        }
        if (properties.fallbackModel() != null && !properties.fallbackModel().isBlank()) {
            models.add(properties.fallbackModel().trim());
        }
        models.add("gemini-3.5-flash");
        models.add("gemini-3.6-flash");
        models.add("gemini-3.8-flash");
        return new ArrayList<>(models);
    }

    private JsonNode invokeModel(String modelName, Map<String, Object> payload) {
        return restClient.post()
                .uri("/v1beta/models/{model}:generateContent", modelName)
                .header(API_KEY_HEADER, properties.apiKey().trim())
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    private GeminiStructuredOutput extractStructuredJson(JsonNode responseRoot, String modelUsed) {
        if (responseRoot == null) {
            throw new IllegalStateException("Empty response from Gemini API");
        }
        JsonNode blockReason = responseRoot.path("promptFeedback").path("blockReason");
        if (!blockReason.isMissingNode() && !blockReason.asString("").isBlank()) {
            throw new IllegalStateException("Gemini request was blocked by safety filters: " + blockReason.asString(""));
        }
        JsonNode candidates = responseRoot.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini API returned no candidates");
        }
        JsonNode firstCandidate = candidates.get(0);
        String finishReason = firstCandidate.path("finishReason").asString("");
        if ("SAFETY".equalsIgnoreCase(finishReason)) {
            throw new IllegalStateException("Gemini candidate blocked by safety filter");
        }
        if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
            LOGGER.warn("Gemini candidate response truncated due to MAX_TOKENS");
        }
        JsonNode parts = firstCandidate.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Gemini API candidate contains no parts");
        }
        String text = "";
        for (JsonNode part : parts) {
            if (!part.path("thought").asBoolean(false)) {
                String candidateText = part.path("text").asString("");
                if (!candidateText.isBlank()) {
                    text = candidateText;
                }
            }
        }
        if (text.isBlank()) {
            text = parts.get(parts.size() - 1).path("text").asString("");
        }
        if (text.isBlank()) {
            throw new IllegalStateException("Gemini API returned blank text part");
        }
        JsonNode parsedJson;
        try {
            parsedJson = objectMapper.readTree(text);
        } catch (Exception ex) {
            throw new IllegalStateException("Gemini API returned invalid JSON: " + ex.getMessage(), ex);
        }
        if (!parsedJson.isObject()) {
            throw new IllegalStateException("Gemini API returned non-object JSON structure");
        }
        return new GeminiStructuredOutput(parsedJson, modelUsed);
    }

    private Map<String, Object> buildGenerateContentPayload(
            String modelName,
            String systemInstructionText,
            String userPromptWithMcpContext,
            Map<String, Object> responseSchema
    ) {
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemInstructionText))
        );

        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPromptWithMcpContext))
        );

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", properties.temperature());
        generationConfig.put("responseMimeType", "application/json");
        if (responseSchema != null && !responseSchema.isEmpty()) {
            generationConfig.put("responseSchema", responseSchema);
        }
        if (modelName != null && modelName.startsWith("gemini-3") && !modelName.contains("lite")) {
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", systemInstruction);
        body.put("contents", List.of(userContent));
        body.put("generationConfig", generationConfig);
        return body;
    }

    public record GeminiStructuredOutput(
            JsonNode jsonNode,
            String modelName
    ) {
    }
}
