// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
