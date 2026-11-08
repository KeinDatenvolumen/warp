// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.ai;

import de.warp.weather.api.dto.CriticalSectionHazardDto;
import de.warp.weather.api.dto.HiddenParameterInsightDto;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisResponse;
import de.warp.weather.application.WeatherScoringService.ScoringActivityProfile;
import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherSegment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class WeatherAiResultMapper {

    private static final List<String> MCP_TOOLS_USED = List.of(
            "get_route_weather_telemetry",
            "detect_hidden_meteorological_anomalies",
            "get_activity_profile_vulnerability_matrix"
    );

    public RouteWeatherAiAnalysisResponse mapAiResponse(
            String routeCalculationId,
            String profileCode,
            String profileDisplayName,
            WeatherEvaluation evaluation,
            WeatherAiResponseValidator.ValidatedAiAnalysisResult validated,
            String modelName,
            Instant generatedAt
    ) {
        return new RouteWeatherAiAnalysisResponse(
                routeCalculationId,
                profileCode,
                profileDisplayName,
                evaluation.departureTime(),
                evaluation.overallScore(),
                evaluation.thunderstormCapApplied(),
                validated.riskLevel(),
                validated.headline(),
                validated.overallSummary(),
                validated.profileAdvice(),
                validated.hiddenInsights(),
                validated.criticalSections(),
                validated.recommendations(),
                MCP_TOOLS_USED,
                modelName,
                generatedAt
        );
    }

    public RouteWeatherAiAnalysisResponse buildPhysicsFallbackResponse(
            String routeCalculationId,
            String profileCode,
            String profileDisplayName,
            String startLabel,
            String destinationLabel,
            RouteSnapshot snapshot,
            WeatherEvaluation evaluation,
            List<HiddenParameterInsightDto> hiddenInsights,
            List<CriticalSectionHazardDto> criticalSections,
            Instant now,
            String modelName
    ) {
        String riskLevel = resolveDefaultRiskLevel(evaluation);
        String routeTitle = (startLabel != null && destinationLabel != null)
                ? "von " + startLabel + " nach " + destinationLabel
                : "auf der geplanten Strecke";

        List<WeatherSegment> segments = evaluation.segments();
        double minTemp = 99.0;
        double maxTemp = -99.0;
        double maxWind = 0.0;
        double maxGust = 0.0;
        double maxPrecip = 0.0;
        double maxWetRoad = 0.0;
        for (WeatherSegment seg : segments) {
            WeatherCondition c = seg.condition();
            if (c == null) continue;

            double t = c.temperatureCelsius() != null ? c.temperatureCelsius() : 18.0;
            minTemp = Math.min(minTemp, t);
            maxTemp = Math.max(maxTemp, t);
            if (c.windSpeedKmh() != null) maxWind = Math.max(maxWind, c.windSpeedKmh());
            if (c.windGustsKmh() != null) maxGust = Math.max(maxGust, c.windGustsKmh());
            if (c.precipitationMm() != null) maxPrecip = Math.max(maxPrecip, c.precipitationMm());
            if (c.wetRoadIndex() != null) maxWetRoad = Math.max(maxWetRoad, c.wetRoadIndex() * 100.0);
        }
        if (segments.isEmpty()) {
            minTemp = 16.0;
            maxTemp = 18.0;
        }

        String headline = evaluation.thunderstormCapApplied()
                ? "Warnung vor erhöhtem Gewitterrisiko auf der Strecke"
                : evaluation.overallScore() >= 80
                ? "Überwiegend gute Wetterbedingungen für deine Tour"
                : "Wechselhafte Bedingungen entlang der Route";

        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append(String.format(
                Locale.GERMAN,
                "Für deine %s-Route %s (%.1f km) wird ein Gesamtwetterscore von %d von 100 Punkten erreicht, bei Temperaturen zwischen %.0f und %.0f °C. ",
                profileDisplayName,
                routeTitle,
                snapshot.distanceMeters() / 1000.0,
                evaluation.overallScore(),
                minTemp,
                maxTemp
        ));

        if (evaluation.thunderstormCapApplied()) {
            summaryBuilder.append("Entlang der Strecke besteht ein erhöhtes Gewitterrisiko, weshalb exponierte Abschnitte gemieden werden sollten. ");
        } else if (maxPrecip >= 0.5) {
            summaryBuilder.append(String.format(
                    Locale.GERMAN,
                    "Zeitweise muss mit Niederschlag (bis %.1f mm/h) und entsprechend nasser Oberfläche gerechnet werden. ",
                    maxPrecip
            ));
        } else if (maxWetRoad >= 25.0) {
            summaryBuilder.append("Obwohl es weitgehend trocken bleibt, können Straßen und Wege durch vorangegangene Niederschläge stellenweise noch feucht und rutschig sein. ");
        } else {
            summaryBuilder.append("Es wird kein nennenswerter Niederschlag erwartet und der Untergrund bleibt weitgehend trocken. ");
        }

        if (maxGust >= 30.0) {
            summaryBuilder.append(String.format(
                    Locale.GERMAN,
                    "Achte besonders auf offenen Abschnitten auf auffrischenden Wind mit Böen bis zu %.0f km/h.",
                    maxGust
            ));
        } else {
            summaryBuilder.append(String.format(
                    Locale.GERMAN,
                    "Der Wind bleibt mit Spitzen bis %.0f km/h moderat.",
                    Math.max(maxWind, maxGust)
            ));
        }

        if (!criticalSections.isEmpty() && criticalSections.getFirst().minSegmentScore() < 75) {
            CriticalSectionHazardDto worst = criticalSections.getFirst();
            summaryBuilder.append(String.format(
                    Locale.GERMAN,
                    " Vorsicht ist vor allem im Bereich %s (%s) geboten.",
                    worst.distanceRangeKm(),
                    worst.timeWindow()
            ));
        }

        List<String> recommendations = new ArrayList<>();
        if (evaluation.thunderstormCapApplied()) {
            recommendations.add("Startzeit nach Möglichkeit verschieben, um das Gewitterfenster zu umgehen.");
        }
        if (maxPrecip >= 0.3 || maxWetRoad >= 25.0) {
            recommendations.add("Auf feuchten oder schattigen Streckenabschnitten Tempo und Kurvenlage anpassen.");
        }
        if (maxGust >= 30.0) {
            recommendations.add("Auf freien Flächen und an Waldschneisen mit seitlichen Windböen rechnen.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Gleichmäßiges Tempo wählen und die Kleidung an die Temperaturen entlang der Strecke anpassen.");
        }

        return new RouteWeatherAiAnalysisResponse(
                routeCalculationId,
                profileCode,
                profileDisplayName,
                evaluation.departureTime(),
                evaluation.overallScore(),
                evaluation.thunderstormCapApplied(),
                riskLevel,
                headline,
                summaryBuilder.toString().trim(),
                "",
                hiddenInsights,
                criticalSections,
                recommendations,
                MCP_TOOLS_USED,
                modelName,
                now
        );
    }

    public String resolveDefaultRiskLevel(WeatherEvaluation evaluation) {
        if (evaluation.thunderstormCapApplied() || evaluation.overallScore() < 45) {
            return "CRITICAL";
        }
        if (evaluation.overallScore() < 65) {
            return "HIGH";
        }
        if (evaluation.overallScore() < 80) {
            return "MODERATE";
        }
        return "LOW";
    }

    public String resolveProfileDisplayName(String profileCode) {
        return switch (ScoringActivityProfile.fromCode(profileCode)) {
            case MOTORCYCLE -> "Motorrad";
            case HIKING -> "Wandern";
            case BICYCLE -> "Fahrrad";
        };
    }
}
