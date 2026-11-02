package de.warp.weather.application;

import de.warp.weather.api.dto.CriticalSectionHazardDto;
import de.warp.weather.api.dto.HiddenParameterInsightDto;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisRequest;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisResponse;
import de.warp.weather.application.WeatherScoringService.ScoringActivityProfile;
import de.warp.weather.domain.model.CachedForecast;
import de.warp.weather.domain.model.EvaluationStatus;
import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherSegment;
import de.warp.weather.infrastructure.gemini.GeminiClient;
import de.warp.weather.infrastructure.gemini.GeminiClient.GeminiStructuredOutput;
import de.warp.weather.infrastructure.mcp.McpToolRegistry;
import de.warp.weather.infrastructure.mcp.WeatherMcpTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RouteWeatherAiAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteWeatherAiAnalysisService.class);
    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(BERLIN_ZONE);
    private static final Duration AI_CACHE_TTL = Duration.ofMinutes(30);

    private static final List<String> MCP_TOOLS_USED = List.of(
            "get_route_weather_telemetry",
            "detect_hidden_meteorological_anomalies",
            "get_activity_profile_vulnerability_matrix"
    );

    private static final String SYSTEM_INSTRUCTION = """
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

    private final RouteSegmenter routeSegmenter;
    private final WeatherCacheService weatherCacheService;
    private final WeatherScoringService weatherScoringService;
    private final WeatherMcpTools weatherMcpTools;
    private final McpToolRegistry mcpToolRegistry;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final Map<String, CachedAiAnalysis> analysisCache = new ConcurrentHashMap<>();

    @Autowired
    public RouteWeatherAiAnalysisService(
            RouteSegmenter routeSegmenter,
            WeatherCacheService weatherCacheService,
            WeatherScoringService weatherScoringService,
            WeatherMcpTools weatherMcpTools,
            McpToolRegistry mcpToolRegistry,
            GeminiClient geminiClient,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.routeSegmenter = Objects.requireNonNull(routeSegmenter);
        this.weatherCacheService = Objects.requireNonNull(weatherCacheService);
        this.weatherScoringService = Objects.requireNonNull(weatherScoringService);
        this.weatherMcpTools = Objects.requireNonNull(weatherMcpTools);
        this.mcpToolRegistry = Objects.requireNonNull(mcpToolRegistry);
        this.geminiClient = Objects.requireNonNull(geminiClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public RouteWeatherAiAnalysisResponse analyzeRouteWeather(RouteWeatherAiAnalysisRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        RouteSnapshot routeSnapshot = request.toDomain();
        Instant now = Instant.now(clock);

        String cacheKey = buildCacheKey(routeSnapshot);
        CachedAiAnalysis cached = analysisCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.response();
        }

        WeatherEvaluation evaluation = buildEvaluationForSnapshot(
                request.requestedByUserId(),
                routeSnapshot,
                now
        );

        String evalLookupKey = routeSnapshot.routeCalculationId() != null && !routeSnapshot.routeCalculationId().isBlank()
                ? routeSnapshot.routeCalculationId()
                : "ai_" + Math.abs(cacheKey.hashCode());
        weatherMcpTools.registerEphemeralEvaluation(evalLookupKey, evaluation);

        String profileCode = ScoringActivityProfile.fromCode(routeSnapshot.activityProfile().code()).name();
        String profileDisplayName = resolveProfileDisplayName(profileCode);

        // Execute MCP tools via McpToolRegistry
        Object telemetryOutput = mcpToolRegistry.callTool(
                "get_route_weather_telemetry",
                Map.of(
                        "routeCalculationId", evalLookupKey,
                        "activityProfileCode", profileCode
                )
        );
        Object anomaliesOutput = mcpToolRegistry.callTool(
                "detect_hidden_meteorological_anomalies",
                Map.of(
                        "routeCalculationId", evalLookupKey,
                        "activityProfileCode", profileCode
                )
        );
        Object vulnerabilityOutput = mcpToolRegistry.callTool(
                "get_activity_profile_vulnerability_matrix",
                Map.of("activityProfileCode", profileCode)
        );

        List<HiddenParameterInsightDto> deterministicHiddenInsights =
                buildDeterministicHiddenInsights(evaluation, profileCode);
        List<CriticalSectionHazardDto> deterministicCriticalSections =
                buildDeterministicCriticalSections(evaluation, routeSnapshot.distanceMeters(), profileCode);

        RouteWeatherAiAnalysisResponse response;
        if (geminiClient.isConfigured()) {
            try {
                String prompt = buildMcpPrompt(
                        request.startLabel(),
                        request.destinationLabel(),
                        routeSnapshot,
                        evaluation,
                        telemetryOutput,
                        anomaliesOutput,
                        vulnerabilityOutput
                );
                GeminiStructuredOutput geminiOutput = geminiClient.generateStructuredAnalysis(
                        SYSTEM_INSTRUCTION,
                        prompt,
                        buildGeminiResponseSchema()
                );
                response = mapGeminiOutputToResponse(
                        geminiOutput,
                        evalLookupKey,
                        profileCode,
                        profileDisplayName,
                        evaluation,
                        deterministicHiddenInsights,
                        deterministicCriticalSections,
                        now
                );
            } catch (Exception ex) {
                LOGGER.warn("Gemini API call failed, using deterministic MCP physics synthesis: {}", ex.getMessage());
                response = buildPhysicsFallbackResponse(
                        evalLookupKey,
                        profileCode,
                        profileDisplayName,
                        request.startLabel(),
                        request.destinationLabel(),
                        routeSnapshot,
                        evaluation,
                        deterministicHiddenInsights,
                        deterministicCriticalSections,
                        now,
                        "warp-physics-engine"
                );
            }
        } else {
            response = buildPhysicsFallbackResponse(
                    evalLookupKey,
                    profileCode,
                    profileDisplayName,
                    request.startLabel(),
                    request.destinationLabel(),
                    routeSnapshot,
                    evaluation,
                    deterministicHiddenInsights,
                    deterministicCriticalSections,
                    now,
                    "warp-physics-engine"
            );
        }

        analysisCache.put(cacheKey, new CachedAiAnalysis(response, now.plus(AI_CACHE_TTL)));
        return response;
    }

    private WeatherEvaluation buildEvaluationForSnapshot(
            String requestedByUserId,
            RouteSnapshot routeSnapshot,
            Instant now
    ) {
        if (routeSnapshot.routeCalculationId() != null && !routeSnapshot.routeCalculationId().isBlank()) {
            WeatherEvaluation existing = weatherMcpTools.resolveEvaluation(routeSnapshot.routeCalculationId());
            if (existing != null
                    && existing.segments() != null
                    && !existing.segments().isEmpty()
                    && existing.departureTime() != null
                    && Math.abs(Duration.between(existing.departureTime(), routeSnapshot.departureTime()).toMinutes()) < 15) {
                return existing;
            }
        }

        List<RouteSegmenter.PlannedRouteSegment> plannedSegments =
                routeSegmenter.segmentRoute(routeSnapshot);
        List<CachedForecast> cachedForecasts;
        try {
            cachedForecasts = weatherCacheService.getForecastsForSegments(plannedSegments);
        } catch (RuntimeException ex) {
            if (routeSnapshot.routeCalculationId() != null && !routeSnapshot.routeCalculationId().isBlank()) {
                WeatherEvaluation fallbackExisting = weatherMcpTools.resolveEvaluation(routeSnapshot.routeCalculationId());
                if (fallbackExisting != null && fallbackExisting.segments() != null && !fallbackExisting.segments().isEmpty()) {
                    LOGGER.info("Using persisted route evaluation after forecast provider exception: {}", ex.getMessage());
                    return fallbackExisting;
                }
            }
            throw ex;
        }

        String profileCode = routeSnapshot.activityProfile().code();
        double fallbackSpeedKmh = routeSnapshot.activityProfile().averageSpeedKmh();

        List<WeatherSegment> evaluatedSegments = new ArrayList<>(plannedSegments.size());
        List<Double> segmentDurationsSeconds = new ArrayList<>(plannedSegments.size());

        for (int i = 0; i < plannedSegments.size(); i++) {
            RouteSegmenter.PlannedRouteSegment planned = plannedSegments.get(i);
            CachedForecast forecast = cachedForecasts.get(i);
            WeatherCondition condition = forecast.condition();

            double segmentSpeedKmh = planned.segmentSpeedKmh(fallbackSpeedKmh);
            WeatherScoringService.SegmentScoreBreakdown breakdown =
                    weatherScoringService.evaluateSegmentBreakdown(
                            condition,
                            planned.movementBearingDegrees(),
                            ScoringActivityProfile.fromCode(profileCode),
                            segmentSpeedKmh
                    );
            int segmentScore = breakdown.score();
            String color = weatherScoringService.resolveScoreColor(segmentScore);

            evaluatedSegments.add(new WeatherSegment(
                    planned.segmentIndex(),
                    planned.expectedArrivalTime(),
                    segmentScore,
                    color,
                    planned.distanceFromStartMeters(),
                    condition,
                    planned.geometryCoordinates(),
                    breakdown.limitingFactor(),
                    breakdown.reasonLabel()
            ));
            segmentDurationsSeconds.add(planned.segmentDurationSeconds());
        }

        WeatherScoringService.OverallScoreResult overallResult =
                weatherScoringService.calculateOverallScore(
                        evaluatedSegments,
                        profileCode,
                        segmentDurationsSeconds
                );

        return new WeatherEvaluation(
                "weval_ai_" + Math.abs(routeSnapshot.hashCode()),
                routeSnapshot.routeCalculationId() != null ? routeSnapshot.routeCalculationId() : "direct",
                requestedByUserId != null ? requestedByUserId : "verified-user",
                EvaluationStatus.COMPLETED,
                routeSnapshot.departureTime(),
                overallResult.overallScore(),
                overallResult.scoreVersion(),
                overallResult.thunderstormCapApplied(),
                now,
                now.plusSeconds(3600),
                now,
                evaluatedSegments
        );
    }

    private String buildMcpPrompt(
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

    private Map<String, Object> buildGeminiResponseSchema() {
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

    private RouteWeatherAiAnalysisResponse mapGeminiOutputToResponse(
            GeminiStructuredOutput geminiOutput,
            String routeCalculationId,
            String profileCode,
            String profileDisplayName,
            WeatherEvaluation evaluation,
            List<HiddenParameterInsightDto> fallbackHiddenInsights,
            List<CriticalSectionHazardDto> fallbackCriticalSections,
            Instant now
    ) {
        JsonNode json = geminiOutput.jsonNode();
        String riskLevel = json.path("riskLevel").asString(resolveDefaultRiskLevel(evaluation));
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

        return new RouteWeatherAiAnalysisResponse(
                routeCalculationId,
                profileCode,
                profileDisplayName,
                evaluation.departureTime(),
                evaluation.overallScore(),
                evaluation.thunderstormCapApplied(),
                riskLevel,
                headline.isBlank() ? "KI-Wetteranalyse für deine " + profileDisplayName + "-Route" : headline,
                overallSummary,
                profileAdvice,
                hiddenInsights,
                criticalSections,
                recommendations,
                MCP_TOOLS_USED,
                geminiOutput.modelName(),
                now
        );
    }

    private List<HiddenParameterInsightDto> buildDeterministicHiddenInsights(
            WeatherEvaluation evaluation,
            String profileCode
    ) {
        List<WeatherSegment> segments = evaluation.segments();
        String profileLabel = resolveProfileDisplayName(profileCode);

        double maxWetRoad = 0.0;
        double minDewSpread = 99.0;
        double maxHumidity = 0.0;
        double minSoilTemp = 99.0;
        double maxGust = 0.0;
        double maxWind = 0.0;
        double maxCape = 0.0;
        double minVis = 24000.0;
        double maxUv = 0.0;
        double maxRadiation = 0.0;
        double minAppTemp = 99.0;
        double maxAppTemp = -99.0;

        for (WeatherSegment seg : segments) {
            WeatherCondition c = seg.condition();
            if (c == null) {
                continue;
            }
            double t2m = c.temperatureCelsius() != null ? c.temperatureCelsius() : 18.0;
            double tApp = c.apparentTemperatureCelsius() != null ? c.apparentTemperatureCelsius() : t2m;
            double rh = c.relativeHumidityPercent() != null ? c.relativeHumidityPercent() : 60.0;
            double td = c.dewPointCelsius() != null ? c.dewPointCelsius() : (t2m - (100.0 - rh) / 5.0);
            double spread = Math.max(0.0, t2m - td);
            double wet = c.wetRoadIndex() != null ? c.wetRoadIndex() * 100.0 : 0.0;
            double soil = c.soilTemperatureCelsius() != null ? c.soilTemperatureCelsius() : t2m;
            double wind = c.windSpeedKmh() != null ? c.windSpeedKmh() : 0.0;
            double gust = c.windGustsKmh() != null ? Math.max(wind, c.windGustsKmh()) : wind;
            double cape = c.capeJkg() != null ? c.capeJkg() : 0.0;
            double vis = c.visibilityMeters() != null ? c.visibilityMeters() : 24000.0;
            double uv = c.uvIndex() != null ? c.uvIndex() : 2.0;
            double rad = c.shortwaveRadiationWm2() != null ? c.shortwaveRadiationWm2() : 0.0;

            maxWetRoad = Math.max(maxWetRoad, wet);
            minDewSpread = Math.min(minDewSpread, spread);
            maxHumidity = Math.max(maxHumidity, rh);
            minSoilTemp = Math.min(minSoilTemp, soil);
            maxGust = Math.max(maxGust, gust);
            maxWind = Math.max(maxWind, wind);
            maxCape = Math.max(maxCape, cape);
            minVis = Math.min(minVis, vis);
            maxUv = Math.max(maxUv, uv);
            maxRadiation = Math.max(maxRadiation, rad);
            minAppTemp = Math.min(minAppTemp, tApp);
            maxAppTemp = Math.max(maxAppTemp, tApp);
        }

        if (segments.isEmpty()) {
            minDewSpread = 5.0;
            minSoilTemp = 14.0;
            minAppTemp = 16.0;
            maxAppTemp = 18.0;
        }

        List<HiddenParameterInsightDto> insights = new ArrayList<>();

        // 1. Wet Road Index (2h Lookback)
        String wetStatus = maxWetRoad >= 60 ? "WARNING" : maxWetRoad >= 25 ? "NOTICE" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "WET_ROAD_INDEX",
                "Fahrbahnnässe-Index (2h-Rückblick)",
                "ROAD_SURFACE",
                String.format(Locale.GERMAN, "Max. %.0f %% Oberflächennässe", maxWetRoad),
                wetStatus,
                maxWetRoad >= 25
                        ? String.format(Locale.GERMAN,
                        "Durch vorangegangene Niederschläge und Verdunstungsträgheit bleibt der Untergrund bis zu %.0f %% nass – für %s bedeutet das reduzierten Reifengrip und längere Bremswege selbst in regenfreien Phasen.",
                        maxWetRoad, profileLabel)
                        : "Durchgehend trockene Straßen- und Wegoberfläche ohne versteckte Restnässe aus den Vorstunden."
        ));

        // 2. Dew Point Spread & Humidity
        String dewStatus = minDewSpread <= 2.0 ? "WARNING" : minDewSpread <= 4.0 ? "NOTICE" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "DEW_POINT_SPREAD",
                "Taupunkt-Spread & Beschlagneigung",
                "VISIBILITY_RADIATION",
                String.format(Locale.GERMAN, "Spread %.1f °C (max. %.0f %% Feuchte)", minDewSpread, maxHumidity),
                dewStatus,
                minDewSpread <= 3.0
                        ? String.format(Locale.GERMAN,
                        "Lufttemperatur und Taupunkt liegen zeitweise nur %.1f °C auseinander. Hohes Risiko für beschlagene Brillen/Visiere und feuchte Fahrbahnfilme in Senken.",
                        minDewSpread)
                        : String.format(Locale.GERMAN,
                        "Komfortabler Taupunkt-Abstand (%.1f °C) – kein Beschlagen von Visier oder Sportbrille zu erwarten.",
                        minDewSpread)
        ));

        // 3. Gust Shear & Aerodynamics
        double gustDelta = Math.max(0.0, maxGust - maxWind);
        String gustStatus = maxGust >= 45 ? "CRITICAL" : maxGust >= 30 || gustDelta >= 15 ? "WARNING" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "WIND_GUST_SHEAR",
                "Böenscherung & Seitenwind-Impuls",
                "AERODYNAMICS",
                String.format(Locale.GERMAN, "Böen bis %.0f km/h (Δ +%.0f km/h)", maxGust, gustDelta),
                gustStatus,
                maxGust >= 30
                        ? String.format(Locale.GERMAN,
                        "Während der mittlere Wind moderat wirkt, treten Böenspitzen bis %.0f km/h auf. Beim Profil %s führt dies an Waldschneisen und Brücken zu plötzlichem Seitenversatz.",
                        maxGust, profileLabel)
                        : String.format(Locale.GERMAN,
                        "Geringe Böenscherung (Spitzen bis %.0f km/h) – ruhiges und spurstabiles Verhalten auf der gesamten Strecke.",
                        maxGust)
        ));

        // 4. Soil Temperature 0cm & Thermal Feel
        String soilStatus = minSoilTemp <= 1.0 ? "CRITICAL" : minSoilTemp <= 4.0 ? "WARNING" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "SOIL_AND_THERMAL",
                "Bodentemperatur (0 cm) & Effektivklima",
                "THERMAL",
                String.format(Locale.GERMAN, "Boden min. %.1f °C · Gefühlt %.1f–%.1f °C", minSoilTemp, minAppTemp, maxAppTemp),
                soilStatus,
                minSoilTemp <= 3.5
                        ? String.format(Locale.GERMAN,
                        "Die unmittelbare Bodentemperatur sinkt auf %.1f °C. In schattigen Lagen droht Reif- oder Bodenfrostglätte, obwohl die 2m-Lufttemperatur höher liegt.",
                        minSoilTemp)
                        : String.format(Locale.GERMAN,
                        "Thermisch stabiler Untergrund (min. %.1f °C) bei einer gefühlten Temperaturspanne von %.1f bis %.1f °C unter Berücksichtigung von Fahrtwind und Strahlung.",
                        minSoilTemp, minAppTemp, maxAppTemp)
        ));

        // 5. CAPE & Visibility / Radiation
        String capeStatus = maxCape >= 600 ? "CRITICAL" : maxCape >= 250 || minVis < 3000 ? "WARNING" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "CAPE_AND_VISIBILITY",
                "Konvektionsenergie (CAPE) & Sicht/UV",
                "ATMOSPHERIC",
                String.format(Locale.GERMAN, "CAPE %.0f J/kg · Sicht %.1f km · UV %.1f", maxCape, minVis / 1000.0, maxUv),
                capeStatus,
                maxCape >= 300
                        ? String.format(Locale.GERMAN,
                        "Erhöhte atmosphärische Labilität (%.0f J/kg CAPE) deutet auf rasch aufquellende Schauer- oder Gewitterzellen und thermische Fallböen hin.",
                        maxCape)
                        : String.format(Locale.GERMAN,
                        "Atmosphärisch stabile Schichtung (%.0f J/kg CAPE) mit guter Fernsicht (min. %.1f km) und max. %.0f W/m² Globalstrahlung.",
                        maxCape, minVis / 1000.0, maxRadiation)
        ));

        return insights;
    }

    private List<CriticalSectionHazardDto> buildDeterministicCriticalSections(
            WeatherEvaluation evaluation,
            int totalDistanceMeters,
            String profileCode
    ) {
        List<WeatherSegment> segments = evaluation.segments();
        if (segments.isEmpty()) {
            return List.of();
        }

        List<CriticalSectionHazardDto> hazards = new ArrayList<>();
        String profileLabel = resolveProfileDisplayName(profileCode);

        for (int i = 0; i < segments.size(); i++) {
            WeatherSegment seg = segments.get(i);
            int nextDist = (i + 1 < segments.size())
                    ? segments.get(i + 1).distanceFromStartMeters()
                    : Math.max(totalDistanceMeters, seg.distanceFromStartMeters() + 5000);
            Instant endTime = (i + 1 < segments.size())
                    ? segments.get(i + 1).expectedArrivalTime()
                    : seg.expectedArrivalTime().plus(Duration.ofMinutes(15));

            String distRange = String.format(
                    Locale.GERMAN,
                    "km %.1f – %.1f",
                    seg.distanceFromStartMeters() / 1000.0,
                    nextDist / 1000.0
            );
            String timeWin = TIME_FORMATTER.format(seg.expectedArrivalTime())
                    + " – " + TIME_FORMATTER.format(endTime) + " Uhr";

            WeatherCondition c = seg.condition();
            int score = seg.score();
            String severity = score < 50 ? "DANGER" : score < 75 ? "WARNING" : "INFO";

            if (score < 80 || hazards.isEmpty()) {
                double gust = c != null && c.windGustsKmh() != null ? c.windGustsKmh() : 0.0;
                double wetPct = c != null && c.wetRoadIndex() != null ? c.wetRoadIndex() * 100.0 : 0.0;
                double tApp = c != null && c.apparentTemperatureCelsius() != null
                        ? c.apparentTemperatureCelsius()
                        : (c != null && c.temperatureCelsius() != null ? c.temperatureCelsius() : 18.0);

                String title = seg.reasonLabel() != null && !seg.reasonLabel().isBlank()
                        ? "Abschnitt " + (i + 1) + ": " + seg.reasonLabel()
                        : "Abschnitt " + (i + 1) + " (" + distRange + ")";

                String description = String.format(
                        Locale.GERMAN,
                        "Score %d/100 für %s. Gefühlte Temperatur %.1f °C, Böenspitzen bis %.0f km/h und %.0f %% berechnete Fahrbahnnässe.",
                        score, profileLabel, tApp, gust, wetPct
                );

                String recommendation = switch (seg.limitingFactor() != null ? seg.limitingFactor() : "") {
                    case "THUNDER_CAP" -> "Exponierte Höhenlagen meiden und Schutzmöglichkeit einplanen (Gewitter-Kappung aktiv).";
                    case "ICE_CAP" -> "Akute Glättegefahr durch niedrige Bodentemperatur – Geschwindigkeit stark reduzieren.";
                    case "WIND", "STORM_CAP" -> "Seitenwind- und Böenimpulse beachten, Lenker locker führen und Überholabstand vergrößern.";
                    case "PRECIP", "WET_ROAD" -> "Erhöhte Rutschgefahr auf nassem Belag – Schräglage in Kurven reduzieren und früher bremsen.";
                    default -> "Gleichmäßiges Tempo wählen und Sicht-/Wetterentwicklung im Blick behalten.";
                };

                hazards.add(new CriticalSectionHazardDto(
                        seg.segmentIndex(),
                        seg.segmentIndex(),
                        distRange,
                        timeWin,
                        score,
                        severity,
                        seg.limitingFactor() != null ? seg.limitingFactor() : "NONE",
                        title,
                        description,
                        recommendation
                ));
            }
        }

        return hazards.stream()
                .sorted(Comparator.comparingInt(CriticalSectionHazardDto::minSegmentScore))
                .limit(4)
                .toList();
    }

    private RouteWeatherAiAnalysisResponse buildPhysicsFallbackResponse(
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
            if (c == null) {
                continue;
            }
            double t = c.temperatureCelsius() != null ? c.temperatureCelsius() : 18.0;
            minTemp = Math.min(minTemp, t);
            maxTemp = Math.max(maxTemp, t);
            if (c.windSpeedKmh() != null) {
                maxWind = Math.max(maxWind, c.windSpeedKmh());
            }
            if (c.windGustsKmh() != null) {
                maxGust = Math.max(maxGust, c.windGustsKmh());
            }
            if (c.precipitationMm() != null) {
                maxPrecip = Math.max(maxPrecip, c.precipitationMm());
            }
            if (c.wetRoadIndex() != null) {
                maxWetRoad = Math.max(maxWetRoad, c.wetRoadIndex() * 100.0);
            }
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

        String overallSummary = summaryBuilder.toString().trim();
        String profileAdvice = "";

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
                overallSummary,
                profileAdvice,
                hiddenInsights,
                criticalSections,
                recommendations,
                MCP_TOOLS_USED,
                modelName,
                now
        );
    }

    private String resolveDefaultRiskLevel(WeatherEvaluation evaluation) {
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

    private String resolveProfileDisplayName(String profileCode) {
        return switch (ScoringActivityProfile.fromCode(profileCode)) {
            case MOTORCYCLE -> "Motorrad";
            case HIKING -> "Wandern";
            case BICYCLE -> "Fahrrad";
        };
    }

    private String buildCacheKey(RouteSnapshot snapshot) {
        String firstCoord = snapshot.coordinates().isEmpty()
                ? "0,0"
                : snapshot.coordinates().getFirst().get(0) + "," + snapshot.coordinates().getFirst().get(1);
        String lastCoord = snapshot.coordinates().isEmpty()
                ? "0,0"
                : snapshot.coordinates().getLast().get(0) + "," + snapshot.coordinates().getLast().get(1);
        return String.format(
                Locale.ROOT,
                "%s:%s:%s:%s:%d",
                snapshot.routeCalculationId(),
                snapshot.activityProfile().code(),
                WeatherCacheService.normalizeToQuarterHour(snapshot.departureTime()),
                firstCoord + "->" + lastCoord,
                snapshot.distanceMeters()
        );
    }

    private record CachedAiAnalysis(
            RouteWeatherAiAnalysisResponse response,
            Instant expiresAt
    ) {
    }
}
