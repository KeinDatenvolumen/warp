// [KI-Log Nr. KI-163] (41862d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-179] (5bc6d66) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.infrastructure.routing.RoutingClient;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationAcceptedResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalStartRouteCalculationRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos
        .ActivityProfileSnapshotRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos
        .EvaluateRouteWeatherRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos
        .RouteSnapshotRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos
        .WeatherEvaluationResponse;
import de.warp.bff.infrastructure.weather.WeatherClient;
import tools.jackson.databind.JsonNode;

@Service
public class RouteCalculationService {

    private final ActivityProfileService activityProfileService;
    private final RoutingClient routingClient;
    private final WeatherClient weatherClient;

    public RouteCalculationService(
            ActivityProfileService activityProfileService,
            RoutingClient routingClient
    ) {
        this(activityProfileService, routingClient, null);
    }

    @Autowired
    public RouteCalculationService(
            ActivityProfileService activityProfileService,
            RoutingClient routingClient,
            @Autowired(required = false)
            WeatherClient weatherClient
    ) {
        this.activityProfileService =
                activityProfileService;
        this.routingClient = routingClient;
        this.weatherClient = weatherClient;
    }

    public StartedRouteCalculation startRouteCalculation(
            String userId,
            RouteCalculationInput input
    ) {
        validateUserId(userId);
        validateInput(input);

        ActivityProfileResponse activityProfile =
                activityProfileService.getActivityProfile(
                        input.activityProfile()
                );

        InternalStartRouteCalculationRequest request =
                new InternalStartRouteCalculationRequest(
                        userId,
                        new InternalStartRouteCalculationRequest
                                .RoutePoint(
                                input.start().label(),
                                input.start().latitude(),
                                input.start().longitude()
                        ),
                        new InternalStartRouteCalculationRequest
                                .RoutePoint(
                                input.destination().label(),
                                input.destination().latitude(),
                                input.destination().longitude()
                        ),
                        input.departureTime(),
                        new InternalStartRouteCalculationRequest
                                .ActivityProfileSnapshot(
                                activityProfile.code(),
                                activityProfile.routingProfile()
                        )
                );

        InternalRouteCalculationAcceptedResponse response =
                routingClient.startRouteCalculation(
                        generateIdempotencyKey(),
                        request
                );

        return new StartedRouteCalculation(
                response.id(),
                response.status(),
                response.createdAt()
        );
    }

    public RouteCalculationResult getRouteCalculation(
            String userId,
            String calculationId
    ) {
        if (calculationId == null
                || calculationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Die Berechnungs-ID darf nicht leer sein."
            );
        }

        InternalRouteCalculationResponse response =
                routingClient.getRouteCalculation(
                        userId,
                        calculationId
                );

        ActivityProfileResponse activityProfile =
                activityProfileService.getActivityProfile(
                        response.activityProfile().code()
                );

        RouteCalculationResult.WeatherEvaluationSnapshot weatherEvaluation =
                evaluateWeatherIfCompleted(
                        userId,
                        response,
                        activityProfile
                );

        return mapResult(
                response,
                activityProfile,
                weatherEvaluation
        );
    }

    public RouteCalculationResult getRouteCalculation(
            String calculationId
    ) {
        return getRouteCalculation(null, calculationId);
    }

    private RouteCalculationResult.WeatherEvaluationSnapshot
    evaluateWeatherIfCompleted(
            String userId,
            InternalRouteCalculationResponse response,
            ActivityProfileResponse activityProfile
    ) {
        if (weatherClient == null
                || !"COMPLETED".equals(response.status())
                || response.geometry() == null) {
            return null;
        }

        List<List<Double>> coordinates = extractCoordinates(
                response.geometry()
        );
        if (coordinates.size() < 2) {
            return null;
        }

        int intervalKm = activityProfile.weatherQueryIntervalKm() != null
                && activityProfile.weatherQueryIntervalKm() > 0
                ? activityProfile.weatherQueryIntervalKm()
                : defaultIntervalKm(activityProfile.code());

        double speedKmh = activityProfile.averageSpeedKmh() > 0.0
                ? activityProfile.averageSpeedKmh()
                : defaultSpeedKmh(activityProfile.code());

        int durationSeconds = response.durationSeconds() != null
                ? (int) Math.round(response.durationSeconds())
                : 0;

        int distanceMeters = response.distanceMeters() != null
                ? (int) Math.round(response.distanceMeters())
                : 0;

        try {
            EvaluateRouteWeatherRequest weatherRequest =
                    new EvaluateRouteWeatherRequest(
                            userId,
                            new RouteSnapshotRequest(
                                    response.id(),
                                    response.departureTime(),
                                    durationSeconds,
                                    distanceMeters,
                                    coordinates,
                                    new ActivityProfileSnapshotRequest(
                                            activityProfile.code(),
                                            speedKmh,
                                            intervalKm
                                    )
                            )
                    );

            WeatherEvaluationResponse evaluationResponse =
                    weatherClient.evaluateRouteWeather(
                            weatherRequest
                    );

            return mapWeatherEvaluation(evaluationResponse);
        } catch (Exception ignored) {
            return null;
        }
    }

    private RouteCalculationResult.WeatherEvaluationSnapshot
    mapWeatherEvaluation(
            WeatherEvaluationResponse response
    ) {
        if (response == null) {
            return null;
        }

        List<RouteCalculationResult.WeatherSegmentSnapshot> segments =
                response.segments() == null
                        ? List.of()
                        : response.segments().stream()
                        .map(seg -> new RouteCalculationResult
                                .WeatherSegmentSnapshot(
                                seg.segmentIndex(),
                                seg.distanceFromStartMeters(),
                                seg.expectedArrivalTime(),
                                seg.score(),
                                seg.color(),
                                seg.limitingFactor(),
                                seg.reasonLabel(),
                                seg.weather() == null
                                        ? null
                                        : new RouteCalculationResult
                                        .WeatherConditionSnapshot(
                                        seg.weather().windSpeedKmh(),
                                        seg.weather().windDirectionDegrees(),
                                        seg.weather().windGustsKmh(),
                                        seg.weather().precipitationProbabilityPercent(),
                                        seg.weather().precipitationMm(),
                                        seg.weather().wetRoadIndex(),
                                        seg.weather().thunderstormProbabilityPercent(),
                                        seg.weather().temperatureCelsius(),
                                        seg.weather().apparentTemperatureCelsius(),
                                        seg.weather().weatherCode()
                                ),
                                seg.geometry() == null
                                        ? List.of()
                                        : seg.geometry().coordinates()
                        ))
                        .toList();

        return new RouteCalculationResult.WeatherEvaluationSnapshot(
                response.evaluationId(),
                response.routeCalculationId(),
                response.status(),
                response.departureTime(),
                response.overallScore(),
                response.scoreVersion(),
                response.thunderstormCapApplied(),
                response.weatherDataGeneratedAt(),
                response.weatherDataExpiresAt(),
                response.createdAt(),
                segments
        );
    }

    private List<List<Double>> extractCoordinates(
            JsonNode geometry
    ) {
        if (geometry == null || geometry.isNull()) {
            return List.of();
        }
        JsonNode coordsNode = geometry.get("coordinates");
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
    }

    private int defaultIntervalKm(String code) {
        if (code == null) {
            return 5;
        }
        return switch (code.toUpperCase()) {
            case "HIKING" -> 2;
            case "MOTORCYCLE" -> 15;
            default -> 5;
        };
    }

    private double defaultSpeedKmh(String code) {
        if (code == null) {
            return 18.0;
        }
        return switch (code.toUpperCase()) {
            case "HIKING" -> 5.0;
            case "MOTORCYCLE" -> 60.0;
            default -> 18.0;
        };
    }

    private RouteCalculationResult mapResult(
            InternalRouteCalculationResponse response,
            ActivityProfileResponse activityProfile,
            RouteCalculationResult.WeatherEvaluationSnapshot weatherEvaluation
    ) {
        RouteCalculationResult.RouteCalculationError error =
                response.error() == null
                        ? null
                        : new RouteCalculationResult
                        .RouteCalculationError(
                        response.error().code(),
                        response.error().message()
                );

        return new RouteCalculationResult(
                response.id(),
                response.status(),
                new RouteCalculationResult.RoutePoint(
                        response.start().label(),
                        response.start().latitude(),
                        response.start().longitude()
                ),
                new RouteCalculationResult.RoutePoint(
                        response.destination().label(),
                        response.destination().latitude(),
                        response.destination().longitude()
                ),
                new RouteCalculationResult
                        .ActivityProfileSnapshot(
                        activityProfile.code(),
                        activityProfile.displayName()
                ),
                response.departureTime(),
                response.arrivalTime(),
                response.distanceMeters(),
                response.durationSeconds(),
                response.geometry(),
                response.segments(),
                weatherEvaluation,
                response.createdAt(),
                error
        );
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "Die Benutzer-ID darf nicht leer sein."
            );
        }
    }

    private void validateInput(
            RouteCalculationInput input
    ) {
        if (input == null) {
            throw new IllegalArgumentException(
                    "Die Routenanfrage darf nicht fehlen."
            );
        }

        if (input.start() == null) {
            throw new IllegalArgumentException(
                    "Der Startpunkt darf nicht fehlen."
            );
        }

        if (input.destination() == null) {
            throw new IllegalArgumentException(
                    "Der Zielpunkt darf nicht fehlen."
            );
        }

        if (input.departureTime() == null) {
            throw new IllegalArgumentException(
                    "Die Abfahrtszeit darf nicht fehlen."
            );
        }

        if (input.activityProfile() == null
                || input.activityProfile().isBlank()) {
            throw new IllegalArgumentException(
                    "Das Aktivitätsprofil darf nicht fehlen."
            );
        }
    }

    private String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}
