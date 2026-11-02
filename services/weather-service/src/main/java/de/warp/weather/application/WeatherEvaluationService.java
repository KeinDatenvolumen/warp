package de.warp.weather.application;

import de.warp.weather.domain.model.CachedForecast;
import de.warp.weather.domain.model.EvaluationStatus;
import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherSegment;
import de.warp.weather.infrastructure.persistence.WeatherEvaluationEntity;
import de.warp.weather.infrastructure.persistence.WeatherEvaluationRepository;
import de.warp.weather.infrastructure.persistence.WeatherSegmentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class WeatherEvaluationService {

    private final RouteSegmenter routeSegmenter;
    private final WeatherCacheService weatherCacheService;
    private final WeatherScoringService weatherScoringService;
    private final WeatherEvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public WeatherEvaluationService(
            RouteSegmenter routeSegmenter,
            WeatherCacheService weatherCacheService,
            WeatherScoringService weatherScoringService,
            WeatherEvaluationRepository evaluationRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.routeSegmenter = Objects.requireNonNull(routeSegmenter);
        this.weatherCacheService = Objects.requireNonNull(weatherCacheService);
        this.weatherScoringService = Objects.requireNonNull(weatherScoringService);
        this.evaluationRepository = Objects.requireNonNull(evaluationRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public WeatherEvaluation evaluateRoute(
            String requestedByUserId,
            RouteSnapshot routeSnapshot
    ) {
        Objects.requireNonNull(routeSnapshot, "routeSnapshot must not be null");

        Instant now = Instant.now(clock);

        Optional<WeatherEvaluationEntity> existing = evaluationRepository
                .findFirstByRouteCalculationIdOrderByCreatedAtDesc(
                        routeSnapshot.routeCalculationId()
                );

        if (existing.isPresent()) {
            WeatherEvaluationEntity existingEntity = existing.get();
            if (existingEntity.getStatus() == EvaluationStatus.COMPLETED
                    && existingEntity.getDepartureTime().equals(routeSnapshot.departureTime())
                    && existingEntity.getWeatherDataExpiresAt().isAfter(now)) {
                return toDomain(existingEntity);
            }
        }

        List<RouteSegmenter.PlannedRouteSegment> plannedSegments =
                routeSegmenter.segmentRoute(routeSnapshot);

        List<CachedForecast> cachedForecasts =
                weatherCacheService.getForecastsForSegments(plannedSegments);

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
                            WeatherScoringService.ScoringActivityProfile.fromCode(profileCode),
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

        Instant generatedAt = cachedForecasts.stream()
                .map(CachedForecast::cachedAt)
                .min(Comparator.naturalOrder())
                .orElse(now);

        Instant expiresAt = cachedForecasts.stream()
                .map(CachedForecast::expiresAt)
                .min(Comparator.naturalOrder())
                .orElse(now.plusSeconds(3600));

        String evaluationId = generateEvaluationId();

        WeatherEvaluationEntity entity = new WeatherEvaluationEntity(
                evaluationId,
                routeSnapshot.routeCalculationId(),
                requestedByUserId,
                EvaluationStatus.COMPLETED,
                routeSnapshot.departureTime(),
                overallResult.overallScore(),
                overallResult.scoreVersion(),
                overallResult.thunderstormCapApplied(),
                generatedAt,
                expiresAt,
                now
        );

        for (WeatherSegment seg : evaluatedSegments) {
            entity.addSegment(new WeatherSegmentEntity(
                    seg.segmentIndex(),
                    seg.distanceFromStartMeters(),
                    seg.expectedArrivalTime(),
                    seg.score(),
                    seg.color(),
                    seg.condition(),
                    serializeGeometry(seg.geometryCoordinates()),
                    seg.limitingFactor(),
                    seg.reasonLabel()
            ));
        }

        WeatherEvaluationEntity saved = evaluationRepository.save(entity);
        return toDomain(saved);
    }

    @Transactional(readOnly = true)
    public WeatherEvaluation getEvaluationById(String evaluationId) {
        if (evaluationId == null || evaluationId.isBlank()) {
            throw new WeatherEvaluationNotFoundException("Evaluation ID must not be blank");
        }

        return evaluationRepository.findById(evaluationId)
                .map(this::toDomain)
                .orElseThrow(() -> new WeatherEvaluationNotFoundException(
                        "Weather evaluation not found: " + evaluationId
                ));
    }

    @Transactional(readOnly = true)
    public WeatherEvaluation getEvaluationByRouteCalculationId(String routeCalculationId) {
        if (routeCalculationId == null || routeCalculationId.isBlank()) {
            throw new WeatherEvaluationNotFoundException("Route calculation ID must not be blank");
        }

        return evaluationRepository
                .findFirstByRouteCalculationIdOrderByCreatedAtDesc(routeCalculationId)
                .map(this::toDomain)
                .orElseThrow(() -> new WeatherEvaluationNotFoundException(
                        "Weather evaluation not found for route: " + routeCalculationId
                ));
    }

    private WeatherEvaluation toDomain(WeatherEvaluationEntity entity) {
        List<WeatherSegment> segments = entity.getSegments().stream()
                .map(seg -> new WeatherSegment(
                        seg.getSegmentIndex(),
                        seg.getExpectedArrivalTime(),
                        seg.getScore(),
                        seg.getColorHex(),
                        seg.getDistanceFromStartMeters(),
                        seg.toCondition(),
                        deserializeGeometry(seg.getGeometryGeojson()),
                        seg.getLimitingFactor(),
                        seg.getReasonLabel()
                ))
                .toList();

        return new WeatherEvaluation(
                entity.getEvaluationId(),
                entity.getRouteCalculationId(),
                entity.getRequestedByUserId(),
                entity.getStatus(),
                entity.getDepartureTime(),
                entity.getOverallScore(),
                entity.getScoreVersion(),
                entity.isThunderstormCapApplied(),
                entity.getWeatherDataGeneratedAt(),
                entity.getWeatherDataExpiresAt(),
                entity.getCreatedAt(),
                segments
        );
    }

    private String serializeGeometry(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "LineString",
                    "coordinates", coordinates
            ));
        } catch (Exception e) {
            return null;
        }
    }

    private List<List<Double>> deserializeGeometry(String geojson) {
        if (geojson == null || geojson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(geojson);
            JsonNode coordsNode = root.get("coordinates");
            if (coordsNode == null || !coordsNode.isArray()) {
                return List.of();
            }
            List<List<Double>> coords = new ArrayList<>();
            for (JsonNode point : coordsNode) {
                if (point.isArray() && point.size() >= 2) {
                    coords.add(List.of(
                            point.get(0).asDouble(),
                            point.get(1).asDouble()
                    ));
                }
            }
            return List.copyOf(coords);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String generateEvaluationId() {
        return "wev_" + UUID.randomUUID().toString().replace("-", "");
    }
}
