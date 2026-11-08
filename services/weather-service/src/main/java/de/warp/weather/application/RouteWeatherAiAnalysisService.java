// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application;

import de.warp.weather.api.dto.CriticalSectionHazardDto;
import de.warp.weather.api.dto.HiddenParameterInsightDto;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisRequest;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisResponse;
import de.warp.weather.application.WeatherScoringService.ScoringActivityProfile;
import de.warp.weather.application.ai.WeatherAiPromptBuilder;
import de.warp.weather.application.ai.WeatherAiResponseValidator;
import de.warp.weather.application.ai.WeatherAiResultMapper;
import de.warp.weather.application.cache.CacheFreshnessPolicy;
import de.warp.weather.application.hazard.WeatherHazardDetector;
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
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class RouteWeatherAiAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteWeatherAiAnalysisService.class);
    private static final Duration AI_CACHE_TTL = Duration.ofMinutes(30);
    public static final Duration DEFAULT_AI_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private Duration aiRequestTimeout = DEFAULT_AI_REQUEST_TIMEOUT;

    private final RouteSegmenter routeSegmenter;
    private final WeatherCacheService weatherCacheService;
    private final WeatherScoringService weatherScoringService;
    private final WeatherMcpTools weatherMcpTools;
    private final McpToolRegistry mcpToolRegistry;
    private final GeminiClient geminiClient;
    private final Clock clock;

    private final WeatherAiPromptBuilder promptBuilder;
    private final WeatherAiResponseValidator responseValidator;
    private final WeatherAiResultMapper resultMapper;
    private final WeatherHazardDetector hazardDetector;
    private final CacheFreshnessPolicy cacheFreshnessPolicy;

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
            Clock clock,
            WeatherAiPromptBuilder promptBuilder,
            WeatherAiResponseValidator responseValidator,
            WeatherAiResultMapper resultMapper,
            WeatherHazardDetector hazardDetector,
            CacheFreshnessPolicy cacheFreshnessPolicy
    ) {
        this.routeSegmenter = Objects.requireNonNull(routeSegmenter);
        this.weatherCacheService = Objects.requireNonNull(weatherCacheService);
        this.weatherScoringService = Objects.requireNonNull(weatherScoringService);
        this.weatherMcpTools = Objects.requireNonNull(weatherMcpTools);
        this.mcpToolRegistry = Objects.requireNonNull(mcpToolRegistry);
        this.geminiClient = Objects.requireNonNull(geminiClient);
        this.clock = Objects.requireNonNull(clock);
        this.promptBuilder = Objects.requireNonNull(promptBuilder);
        this.responseValidator = Objects.requireNonNull(responseValidator);
        this.resultMapper = Objects.requireNonNull(resultMapper);
        this.hazardDetector = Objects.requireNonNull(hazardDetector);
        this.cacheFreshnessPolicy = Objects.requireNonNull(cacheFreshnessPolicy);
    }

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
        this(
                routeSegmenter,
                weatherCacheService,
                weatherScoringService,
                weatherMcpTools,
                mcpToolRegistry,
                geminiClient,
                objectMapper,
                clock,
                new WeatherAiPromptBuilder(objectMapper),
                new WeatherAiResponseValidator(),
                new WeatherAiResultMapper(),
                new WeatherHazardDetector(),
                new CacheFreshnessPolicy()
        );
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
        String profileDisplayName = resultMapper.resolveProfileDisplayName(profileCode);

        // Execute MCP tools via McpToolRegistry
        Object telemetryOutput = mcpToolRegistry.callTool(
                "get_route_weather_telemetry",
                Map.of("routeCalculationId", evalLookupKey, "activityProfileCode", profileCode)
        );
        Object anomaliesOutput = mcpToolRegistry.callTool(
                "detect_hidden_meteorological_anomalies",
                Map.of("routeCalculationId", evalLookupKey, "activityProfileCode", profileCode)
        );
        Object vulnerabilityOutput = mcpToolRegistry.callTool(
                "get_activity_profile_vulnerability_matrix",
                Map.of("activityProfileCode", profileCode)
        );

        List<HiddenParameterInsightDto> deterministicHiddenInsights =
                hazardDetector.buildDeterministicHiddenInsights(evaluation, profileDisplayName);
        List<CriticalSectionHazardDto> deterministicCriticalSections =
                hazardDetector.buildDeterministicCriticalSections(evaluation, routeSnapshot.distanceMeters(), profileDisplayName);

        RouteWeatherAiAnalysisResponse response;
        if (geminiClient.isConfigured()) {
            try {
                String prompt = promptBuilder.buildMcpPrompt(
                        request.startLabel(),
                        request.destinationLabel(),
                        routeSnapshot,
                        evaluation,
                        telemetryOutput,
                        anomaliesOutput,
                        vulnerabilityOutput
                );
                Map<String, Object> schema = promptBuilder.buildGeminiResponseSchema();
                CompletableFuture<GeminiStructuredOutput> geminiFuture =
                        CompletableFuture.supplyAsync(() -> geminiClient.generateStructuredAnalysis(
                                WeatherAiPromptBuilder.SYSTEM_INSTRUCTION,
                                prompt,
                                schema
                        ));
                GeminiStructuredOutput geminiOutput;
                try {
                    geminiOutput = geminiFuture.get(
                            aiRequestTimeout.toMillis(),
                            TimeUnit.MILLISECONDS
                    );
                } catch (TimeoutException timeoutEx) {
                    geminiFuture.cancel(true);
                    throw timeoutEx;
                }
                WeatherAiResponseValidator.ValidatedAiAnalysisResult validated = responseValidator.validateAndSanitize(
                        geminiOutput.jsonNode(),
                        evaluation,
                        deterministicHiddenInsights,
                        deterministicCriticalSections,
                        resultMapper.resolveDefaultRiskLevel(evaluation),
                        profileDisplayName
                );
                response = resultMapper.mapAiResponse(
                        evalLookupKey,
                        profileCode,
                        profileDisplayName,
                        evaluation,
                        validated,
                        geminiOutput.modelName() + " (MCP)",
                        now
                );
            } catch (Exception ex) {
                LOGGER.warn("Gemini API call failed or timed out after {}s, using deterministic MCP physics synthesis: {}",
                        aiRequestTimeout.toSeconds(), ex.getMessage());
                response = resultMapper.buildPhysicsFallbackResponse(
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
                        "warp-mcp-physics-engine (Fallback)"
                );
            }
        } else {
            response = resultMapper.buildPhysicsFallbackResponse(
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
                    "warp-mcp-physics-engine"
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

        List<RouteSegmenter.PlannedRouteSegment> plannedSegments = routeSegmenter.segmentRoute(routeSnapshot);
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
                cacheFreshnessPolicy.normalizeToQuarterHour(snapshot.departureTime()),
                firstCoord + "->" + lastCoord,
                snapshot.distanceMeters()
        );
    }

    void setAiRequestTimeout(Duration aiRequestTimeout) {
        this.aiRequestTimeout = Objects.requireNonNull(aiRequestTimeout);
    }

    private record CachedAiAnalysis(
            RouteWeatherAiAnalysisResponse response,
            Instant expiresAt
    ) {
    }
}
