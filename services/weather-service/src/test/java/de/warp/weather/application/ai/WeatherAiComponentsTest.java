// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.ai;

import de.warp.weather.api.dto.CriticalSectionHazardDto;
import de.warp.weather.api.dto.HiddenParameterInsightDto;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisResponse;
import de.warp.weather.domain.model.EvaluationStatus;
import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherEvaluation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherAiComponentsTest {

    private ObjectMapper objectMapper;
    private WeatherAiPromptBuilder promptBuilder;
    private WeatherAiResponseValidator responseValidator;
    private WeatherAiResultMapper resultMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        promptBuilder = new WeatherAiPromptBuilder(objectMapper);
        responseValidator = new WeatherAiResponseValidator();
        resultMapper = new WeatherAiResultMapper();
    }

    @Test
    void buildsPromptAndResponseSchema() {
        Instant now = Instant.parse("2026-05-16T08:00:00Z");
        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-1", now, 3600, 20000,
                List.of(List.of(13.4, 52.5), List.of(13.2, 52.4)),
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );
        WeatherEvaluation eval = new WeatherEvaluation(
                "wev-1", "calc-1", "user-1", EvaluationStatus.COMPLETED,
                now, 85, "1.0", false, now, now.plusSeconds(3600), now, List.of()
        );

        String prompt = promptBuilder.buildMcpPrompt("A", "B", snapshot, eval, Map.of(), Map.of(), Map.of());
        assertThat(prompt).contains("routeMeta").contains("distanceKm");

        Map<String, Object> schema = promptBuilder.buildGeminiResponseSchema();
        assertThat(schema).containsKey("properties");
    }

    @Test
    void validatesAndSanitizesGeminiJson() throws Exception {
        String jsonStr = """
                {
                  "riskLevel": "HIGH",
                  "headline": "Vorsicht!",
                  "overallSummary": "Regnerisch",
                  "activityProfileAdvice": "Regenjacke",
                  "hiddenParameterInsights": [],
                  "criticalSections": [],
                  "recommendations": ["Tempo drosseln", ""]
                }
                """;
        JsonNode jsonNode = objectMapper.readTree(jsonStr);

        Instant now = Instant.parse("2026-05-16T08:00:00Z");
        WeatherEvaluation eval = new WeatherEvaluation(
                "wev-1", "calc-1", "user-1", EvaluationStatus.COMPLETED,
                now, 85, "1.0", false, now, now.plusSeconds(3600), now, List.of()
        );

        List<HiddenParameterInsightDto> fallbackHidden = List.of(new HiddenParameterInsightDto("K", "L", "C", "V", "S", "I"));
        List<CriticalSectionHazardDto> fallbackSections = List.of(new CriticalSectionHazardDto(0, 0, "km 0", "win", 80, "INFO", "F", "T", "D", "R"));

        WeatherAiResponseValidator.ValidatedAiAnalysisResult result = responseValidator.validateAndSanitize(
                jsonNode, eval, fallbackHidden, fallbackSections, "LOW", "Fahrrad"
        );

        assertThat(result.riskLevel()).isEqualTo("HIGH");
        assertThat(result.hiddenInsights()).isEqualTo(fallbackHidden);
        assertThat(result.criticalSections()).isEqualTo(fallbackSections);
        assertThat(result.recommendations()).containsExactly("Tempo drosseln");
    }

    @Test
    void resultMapperCreatesResponsesAndResolvesProfiles() {
        assertThat(resultMapper.resolveProfileDisplayName("MOTORCYCLE")).isEqualTo("Motorrad");
        assertThat(resultMapper.resolveProfileDisplayName("HIKING")).isEqualTo("Wandern");
        assertThat(resultMapper.resolveProfileDisplayName("BICYCLE")).isEqualTo("Fahrrad");

        Instant now = Instant.parse("2026-05-16T08:00:00Z");
        WeatherEvaluation eval = new WeatherEvaluation(
                "wev-1", "calc-1", "user-1", EvaluationStatus.COMPLETED,
                now, 90, "1.0", false, now, now.plusSeconds(3600), now, List.of()
        );
        assertThat(resultMapper.resolveDefaultRiskLevel(eval)).isEqualTo("LOW");

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-1", now, 3600, 20000,
                List.of(List.of(13.4, 52.5), List.of(13.2, 52.4)),
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );

        RouteWeatherAiAnalysisResponse response = resultMapper.buildPhysicsFallbackResponse(
                "calc-1", "BICYCLE", "Fahrrad", "A", "B",
                snapshot, eval, List.of(), List.of(), now, "physics"
        );

        assertThat(response.overallScore()).isEqualTo(90);
        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.headline()).contains("gute Wetterbedingungen");
    }
}
