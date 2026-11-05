// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
package de.warp.weather.api.dto;

import java.time.Instant;
import java.util.List;

public record RouteWeatherAiAnalysisResponse(
        String routeCalculationId,
        String activityProfileCode,
        String activityProfileDisplayName,
        Instant departureTime,
        int overallScore,
        boolean thunderstormCapApplied,
        String riskLevel,
        String headline,
        String overallSummary,
        String activityProfileAdvice,
        List<HiddenParameterInsightDto> hiddenParameterInsights,
        List<CriticalSectionHazardDto> criticalSections,
        List<String> recommendations,
        List<String> mcpToolsUsed,
        String modelName,
        Instant generatedAt
) {
}
