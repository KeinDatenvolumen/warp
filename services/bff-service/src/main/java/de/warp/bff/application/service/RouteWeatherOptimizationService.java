// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.application.service;

import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.api.dto.RouteWeatherAiAnalysisApiRequest;
import de.warp.bff.api.dto.RouteWeatherOptimizationApiRequest;
import de.warp.bff.infrastructure.routing.RoutingClient;
import de.warp.bff.infrastructure.routing.dto.InternalRouteCalculationResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.ActivityProfileSnapshotRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteSnapshotRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationResponse;
import de.warp.bff.infrastructure.weather.WeatherClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service("bffRouteWeatherOptimizationService")
public class RouteWeatherOptimizationService {

    private final WeatherClient weatherClient;
    private final ActivityProfileService activityProfileService;
    private final RoutingClient routingClient;

    @Autowired
    public RouteWeatherOptimizationService(
            WeatherClient weatherClient,
            ActivityProfileService activityProfileService,
            RoutingClient routingClient
    ) {
        this.weatherClient = Objects.requireNonNull(weatherClient);
        this.activityProfileService = Objects.requireNonNull(activityProfileService);
        this.routingClient = Objects.requireNonNull(routingClient);
    }

    public RouteWeatherOptimizationResponse optimizeDeparture(
            String userId,
            RouteWeatherOptimizationApiRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        String effectiveUserId = userId != null ? userId : "anonymous";
        ResolvedRouteInput route = resolveRouteDetails(
                effectiveUserId,
                request.routeCalculationId(),
                request.activityProfile(),
                request.departureTime(),
                request.durationSeconds(),
                request.distanceMeters(),
                request.coordinates()
        );

        RouteSnapshotRequest snapshot = buildRouteSnapshot(route);
        RouteWeatherOptimizationRequest weatherReq = new RouteWeatherOptimizationRequest(
                effectiveUserId,
                snapshot,
                request.windowStart(),
                request.windowEnd()
        );

        return weatherClient.optimizeDeparture(weatherReq);
    }

    public RouteWeatherAiAnalysisResponse analyzeRouteWeatherAi(
            String userId,
            RouteWeatherAiAnalysisApiRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        String effectiveUserId = Objects.requireNonNull(userId, "userId must not be null");
        ResolvedRouteInput route = resolveRouteDetails(
                effectiveUserId,
                request.routeCalculationId(),
                request.activityProfile(),
                request.departureTime(),
                request.durationSeconds(),
                request.distanceMeters(),
                request.coordinates()
        );

        String startLabel = request.startLabel() != null ? request.startLabel() : route.startLabel();
        String destLabel = request.destinationLabel() != null ? request.destinationLabel() : route.destinationLabel();

        RouteSnapshotRequest snapshot = buildRouteSnapshot(route);
        RouteWeatherAiAnalysisRequest aiRequest = new RouteWeatherAiAnalysisRequest(
                effectiveUserId,
                startLabel,
                destLabel,
                snapshot
        );

        return weatherClient.analyzeRouteWeatherAi(aiRequest);
    }

    private ResolvedRouteInput resolveRouteDetails(
            String userId,
            String calculationId,
            String profileCode,
            Instant departureTime,
            Double durationSeconds,
            Double distanceMeters,
            List<List<Double>> coordinates
    ) {
        List<List<Double>> coords = coordinates;
        String calcId = calculationId != null ? calculationId : "direct";
        Instant depTime = departureTime;
        int durSec = durationSeconds != null ? (int) Math.round(durationSeconds) : 0;
        int distMeters = distanceMeters != null ? (int) Math.round(distanceMeters) : 0;
        String profile = profileCode;
        String startLabel = null;
        String destLabel = null;

        if ((coords == null || coords.size() < 2) && calculationId != null) {
            try {
                InternalRouteCalculationResponse routingResp = routingClient.getRouteCalculation(userId, calculationId);
                if (routingResp != null) {
                    calcId = routingResp.id();
                    if (depTime == null) depTime = routingResp.departureTime();
                    if (durSec <= 0 && routingResp.durationSeconds() != null) {
                        durSec = (int) Math.round(routingResp.durationSeconds());
                    }
                    if (distMeters <= 0 && routingResp.distanceMeters() != null) {
                        distMeters = (int) Math.round(routingResp.distanceMeters());
                    }
                    if (profile == null && routingResp.activityProfile() != null) {
                        profile = routingResp.activityProfile().code();
                    }
                    if (routingResp.start() != null) {
                        startLabel = routingResp.start().label();
                    }
                    if (routingResp.destination() != null) {
                        destLabel = routingResp.destination().label();
                    }
                    coords = extractCoordinates(routingResp.geometry());
                }
            } catch (Exception ignored) {
                // fall through to validation
            }
        }

        if (coords == null || coords.size() < 2) {
            throw new IllegalArgumentException("Insufficient route coordinates for weather optimization");
        }

        if (profile == null || profile.isBlank()) {
            profile = "BICYCLE";
        }

        return new ResolvedRouteInput(
                calcId,
                depTime != null ? depTime : Instant.now(),
                durSec,
                distMeters,
                profile,
                startLabel,
                destLabel,
                coords
        );
    }

    private RouteSnapshotRequest buildRouteSnapshot(ResolvedRouteInput route) {
        double speed = 20.0;
        int interval = 5;
        try {
            ActivityProfileResponse profileDto = activityProfileService.getActivityProfile(route.profileCode());
            if (profileDto != null) {
                if (profileDto.averageSpeedKmh() > 0) {
                    speed = profileDto.averageSpeedKmh();
                }
                if (profileDto.weatherQueryIntervalKm() != null && profileDto.weatherQueryIntervalKm() > 0) {
                    interval = profileDto.weatherQueryIntervalKm();
                }
            }
        } catch (Exception ignored) {
            // fallback defaults
        }

        return new RouteSnapshotRequest(
                route.calculationId(),
                route.departureTime(),
                route.durationSeconds(),
                route.distanceMeters(),
                route.coordinates(),
                new ActivityProfileSnapshotRequest(route.profileCode(), speed, interval)
        );
    }

    private List<List<Double>> extractCoordinates(JsonNode geometry) {
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

    private record ResolvedRouteInput(
            String calculationId,
            Instant departureTime,
            int durationSeconds,
            int distanceMeters,
            String profileCode,
            String startLabel,
            String destinationLabel,
            List<List<Double>> coordinates
    ) {
    }
}
