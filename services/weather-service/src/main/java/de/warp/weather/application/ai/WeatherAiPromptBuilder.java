// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.ai;

import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherEvaluation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class WeatherAiPromptBuilder {

    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(BERLIN_ZONE);

    public static final String SYSTEM_INSTRUCTION = """
            Du erstellst eine präzise, gut verständliche Wetter-Gesamtzusammenfassung für eine geplante Outdoor-Route (Fahrrad, Motorrad oder Wandern).
            Dir liegen über die bereitgestellten Daten sowohl die klassischen Wetterwerte (Temperatur, Wind, Niederschlag, Gewitterwahrscheinlichkeit) als auch ergänzende Umgebungsdaten (Restnässe auf Straßen/Wegen durch vorherigen Regen, Böenspitzen, gefühlte Temperatur, Luftfeuchtigkeit/Beschlagneigung, Bodentemperatur, Sichtweite und UV-Belastung) entlang der gesamten Strecke vor.

            Wichtige Vorgaben für deine Antwort:
            - Verfasse in `overallSummary` eine zusammenhängende, flüssig lesbare Gesamtzusammenfassung (3 bis 5 Sätze) für die gesamte Route.
            - Geh direkt und konkret auf die tatsächlichen Bedingungen dieser Route und das gewählte Fortbewegungsmittel ein: Wie ist das Wetter insgesamt, wo oder wann wird es entlang der Strecke unangenehm oder brenzlig (z. B. noch nasse Fahrbahn/Wege, spürbare Seitenwindböen auf bestimmten Kilometern, Auskühlung durch Fahrtwind, Schauer- oder Gewitterneigung), und worauf sollte man achten?
            - Gib in `recommendations` 2 bis 3 kurze, konkrete Praxistipps für genau diese Tour.
            - Verwende KEINE Emojis oder Sonderzeichen-Icons.
            - Verwende KEINE internen Systembegriffe, Abkürzungen oder Fachjargon (schreibe niemals Begriffe wie "MCP", "Model Context Protocol", "FR-WETT-03", "CAPE", "Taupunkt-Spread", "Tiefenparameter" oder "geheime Parameter"). Übersetze solche Messwerte immer in natürliche Alltagssprache (z. B. "feuchte Straßenoberfläche", "hohe Luftfeuchtigkeit", "Gewitterneigung").
            - Schreibe keine allgemeinen Lehrbuch-Sätze darüber, was für Wanderer oder Radfahrer theoretisch wichtig ist, sondern beschreibe nur das konkrete Wetter auf dieser Route.
            Antworte ausschließlich im vorgegebenen JSON-Schema auf Deutsch.
            """;

    private final ObjectMapper objectMapper;

    @Autowired
    public WeatherAiPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public String buildMcpPrompt(
            String startLabel,
            String destinationLabel,
            RouteSnapshot snapshot,
            WeatherEvaluation evaluation,
            Object telemetryOutput,
            Object anomaliesOutput,
            Object vulnerabilityOutput
    ) {
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("routeMeta", Map.of(
                    "start", startLabel != null ? startLabel : "Startpunkt",
                    "destination", destinationLabel != null ? destinationLabel : "Zielpunkt",
                    "distanceKm", Math.round(snapshot.distanceMeters() / 100.0) / 10.0,
                    "durationMinutes", Math.round(snapshot.durationSeconds() / 60.0),
                    "departureTimeLocal", TIME_FORMATTER.format(snapshot.departureTime()),
                    "overallScore", evaluation.overallScore(),
                    "thunderstormCapApplied", evaluation.thunderstormCapApplied()
            ));
            envelope.put("mcpTool_get_activity_profile_vulnerability_matrix", vulnerabilityOutput);
            envelope.put("mcpTool_detect_hidden_meteorological_anomalies", anomaliesOutput);
            envelope.put("mcpTool_get_route_weather_telemetry", telemetryOutput);

            return "Analysiere die folgende Route anhand der MCP-Tool-Daten (bekannte + geheime Wetterparameter) für das gewählte Aktivitätsprofil:\n"
                    + objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize MCP prompt context", e);
        }
    }

    public Map<String, Object> buildGeminiResponseSchema() {
        Map<String, Object> hiddenItem = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "parameterKey", Map.of("type", "STRING"),
                        "parameterLabel", Map.of("type", "STRING"),
                        "category", Map.of("type", "STRING"),
                        "observedValue", Map.of("type", "STRING"),
                        "status", Map.of("type", "STRING", "enum", List.of("OPTIMAL", "NOTICE", "WARNING", "CRITICAL")),
                        "interpretation", Map.of("type", "STRING")
                ),
                "required", List.of("parameterKey", "parameterLabel", "category", "observedValue", "status", "interpretation")
        );

        Map<String, Object> sectionItem = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "segmentIndexStart", Map.of("type", "INTEGER"),
                        "segmentIndexEnd", Map.of("type", "INTEGER"),
                        "distanceRangeKm", Map.of("type", "STRING"),
                        "timeWindow", Map.of("type", "STRING"),
                        "minSegmentScore", Map.of("type", "INTEGER"),
                        "severity", Map.of("type", "STRING", "enum", List.of("INFO", "WARNING", "DANGER")),
                        "primaryFactor", Map.of("type", "STRING"),
                        "title", Map.of("type", "STRING"),
                        "description", Map.of("type", "STRING"),
                        "recommendation", Map.of("type", "STRING")
                ),
                "required", List.of(
                        "segmentIndexStart",
                        "segmentIndexEnd",
                        "distanceRangeKm",
                        "timeWindow",
                        "minSegmentScore",
                        "severity",
                        "primaryFactor",
                        "title",
                        "description",
                        "recommendation"
                )
        );

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "OBJECT");
        root.put("properties", Map.of(
                "riskLevel", Map.of("type", "STRING", "enum", List.of("LOW", "MODERATE", "HIGH", "CRITICAL")),
                "headline", Map.of("type", "STRING"),
                "overallSummary", Map.of("type", "STRING"),
                "activityProfileAdvice", Map.of("type", "STRING"),
                "hiddenParameterInsights", Map.of("type", "ARRAY", "items", hiddenItem),
                "criticalSections", Map.of("type", "ARRAY", "items", sectionItem),
                "recommendations", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
        ));
        root.put("required", List.of(
                "riskLevel",
                "headline",
                "overallSummary",
                "activityProfileAdvice",
                "hiddenParameterInsights",
                "criticalSections",
                "recommendations"
        ));
        return root;
    }
}
