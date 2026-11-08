// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.ai;

import de.warp.weather.api.dto.CriticalSectionHazardDto;
import de.warp.weather.api.dto.HiddenParameterInsightDto;
import de.warp.weather.domain.model.WeatherEvaluation;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class WeatherAiResponseValidator {

    public ValidatedAiAnalysisResult validateAndSanitize(
            JsonNode json,
            WeatherEvaluation evaluation,
            List<HiddenParameterInsightDto> fallbackHiddenInsights,
            List<CriticalSectionHazardDto> fallbackCriticalSections,
            String defaultRiskLevel,
            String profileDisplayName
    ) {
        String riskLevel = json.path("riskLevel").asString(defaultRiskLevel);
        String headline = json.path("headline").asString("");
        String overallSummary = json.path("overallSummary").asString("");
        String profileAdvice = json.path("activityProfileAdvice").asString("");

        List<HiddenParameterInsightDto> hiddenInsights = new ArrayList<>();
        JsonNode hiddenNode = json.path("hiddenParameterInsights");
        if (hiddenNode.isArray()) {
            for (JsonNode item : hiddenNode) {
                hiddenInsights.add(new HiddenParameterInsightDto(
                        item.path("parameterKey").asString("HIDDEN_METRIC"),
                        item.path("parameterLabel").asString("Tiefenparameter"),
                        item.path("category").asString("ATMOSPHERIC"),
                        item.path("observedValue").asString("-"),
                        item.path("status").asString("NOTICE"),
                        item.path("interpretation").asString("")
                ));
            }
        }
        if (hiddenInsights.isEmpty()) {
            hiddenInsights = fallbackHiddenInsights;
        }

        int maxSegIdx = Math.max(0, evaluation.segments().size() - 1);
        List<CriticalSectionHazardDto> criticalSections = new ArrayList<>();
        JsonNode sectionsNode = json.path("criticalSections");
        if (sectionsNode.isArray()) {
            for (JsonNode item : sectionsNode) {
                int startIdx = Math.max(0, Math.min(maxSegIdx, item.path("segmentIndexStart").asInt(0)));
                int endIdx = Math.max(startIdx, Math.min(maxSegIdx, item.path("segmentIndexEnd").asInt(startIdx)));
                criticalSections.add(new CriticalSectionHazardDto(
                        startIdx,
                        endIdx,
                        item.path("distanceRangeKm").asString("Abschnitt " + (startIdx + 1)),
                        item.path("timeWindow").asString(""),
                        item.path("minSegmentScore").asInt(evaluation.overallScore()),
                        item.path("severity").asString("INFO"),
                        item.path("primaryFactor").asString("WEATHER"),
                        item.path("title").asString("Wetterhinweis"),
                        item.path("description").asString(""),
                        item.path("recommendation").asString("")
                ));
            }
        }
        if (criticalSections.isEmpty()) {
            criticalSections = fallbackCriticalSections;
        }

        List<String> recommendations = new ArrayList<>();
        JsonNode recsNode = json.path("recommendations");
        if (recsNode.isArray()) {
            for (JsonNode r : recsNode) {
                String text = r.asString("").trim();
                if (!text.isEmpty()) {
                    recommendations.add(text);
                }
            }
        }

        String effectiveHeadline = headline.isBlank()
                ? "KI-Wetteranalyse für deine " + profileDisplayName + "-Route"
                : headline;

        return new ValidatedAiAnalysisResult(
                riskLevel,
                effectiveHeadline,
                overallSummary,
                profileAdvice,
                hiddenInsights,
                criticalSections,
                recommendations
        );
    }

    public record ValidatedAiAnalysisResult(
            String riskLevel,
            String headline,
            String overallSummary,
            String profileAdvice,
            List<HiddenParameterInsightDto> hiddenInsights,
            List<CriticalSectionHazardDto> criticalSections,
            List<String> recommendations
    ) {
    }
}
