package de.warp.bff.api.controller;

import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.api.dto.ApiErrorResponse;
import de.warp.bff.api.dto.RouteWeatherAiAnalysisApiRequest;
import de.warp.bff.api.dto.RouteWeatherOptimizationApiRequest;
import de.warp.bff.application.service.ActivityProfileService;
import de.warp.bff.infrastructure.routing.RoutingClient;
import de.warp.bff.infrastructure.routing.dto.InternalRouteCalculationResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.ActivityProfileSnapshotRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteSnapshotRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationResponse;
import de.warp.bff.infrastructure.weather.WeatherClient;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteWeatherOptimizationController {

    private final WeatherClient weatherClient;
    private final ActivityProfileService activityProfileService;
    private final RoutingClient routingClient;

    public RouteWeatherOptimizationController(
            WeatherClient weatherClient,
            ActivityProfileService activityProfileService,
            RoutingClient routingClient
    ) {
        this.weatherClient = weatherClient;
        this.activityProfileService = activityProfileService;
        this.routingClient = routingClient;
    }

    @PostMapping("/weather-optimization")
    public ResponseEntity<RouteWeatherOptimizationResponse> optimizeRouteWeather(
            @RequestBody RouteWeatherOptimizationApiRequest request,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        String userId = principal != null ? principal.userId() : "anonymous";
        List<List<Double>> coords = request.coordinates();
        String profileCode = request.activityProfile();
        Instant depTime = request.departureTime();
        int durSec = request.durationSeconds() != null ? (int) Math.round(request.durationSeconds()) : 0;
        int distMeters = request.distanceMeters() != null ? (int) Math.round(request.distanceMeters()) : 0;
        String calcId = request.routeCalculationId() != null ? request.routeCalculationId() : "direct";

        if ((coords == null || coords.size() < 2) && request.routeCalculationId() != null) {
            try {
                InternalRouteCalculationResponse routingResp =
                        routingClient.getRouteCalculation(userId, request.routeCalculationId());
                if (routingResp != null) {
                    calcId = routingResp.id();
                    if (depTime == null) depTime = routingResp.departureTime();
                    if (durSec <= 0 && routingResp.durationSeconds() != null) {
                        durSec = (int) Math.round(routingResp.durationSeconds());
                    }
                    if (distMeters <= 0 && routingResp.distanceMeters() != null) {
                        distMeters = (int) Math.round(routingResp.distanceMeters());
                    }
                    if (profileCode == null && routingResp.activityProfile() != null) {
                        profileCode = routingResp.activityProfile().code();
                    }
                    coords = extractCoordinates(routingResp.geometry());
                }
            } catch (Exception ignored) {
                // fall through
            }
        }

        if (coords == null || coords.size() < 2) {
            return ResponseEntity.badRequest().build();
        }

        if (profileCode == null || profileCode.isBlank()) {
            profileCode = "BICYCLE";
        }

        double speed = 20.0;
        int interval = 5;
        try {
            ActivityProfileResponse profile = activityProfileService.getActivityProfile(profileCode);
            if (profile != null) {
                if (profile.averageSpeedKmh() > 0) {
                    speed = profile.averageSpeedKmh();
                }
                if (profile.weatherQueryIntervalKm() != null && profile.weatherQueryIntervalKm() > 0) {
                    interval = profile.weatherQueryIntervalKm();
                }
            }
        } catch (Exception ignored) {
            // fallback defaults
        }

        RouteSnapshotRequest snapshot = new RouteSnapshotRequest(
                calcId,
                depTime != null ? depTime : Instant.now(),
                durSec,
                distMeters,
                coords,
                new ActivityProfileSnapshotRequest(profileCode, speed, interval)
        );

        RouteWeatherOptimizationRequest weatherReq = new RouteWeatherOptimizationRequest(
                userId,
                snapshot,
                request.windowStart(),
                request.windowEnd()
        );

        RouteWeatherOptimizationResponse response = weatherClient.optimizeDeparture(weatherReq);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/weather-ai-analysis")
    public ResponseEntity<?> analyzeRouteWeatherAi(
            @RequestBody(required = false) RouteWeatherAiAnalysisApiRequest request,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(new ApiErrorResponse(
                            URI.create("https://warp.local/problems/session-invalid"),
                            "Unauthorized",
                            HttpStatus.UNAUTHORIZED.value(),
                            "SESSION_INVALID",
                            "Für die KI-gestützte Wetteranalyse ist eine gültige Anmeldung erforderlich.",
                            "/api/v1/routes/weather-ai-analysis",
                            List.of()
                    ));
        }

        if (!principal.emailVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(new ApiErrorResponse(
                            URI.create("https://warp.local/problems/email-not-verified"),
                            "Forbidden",
                            HttpStatus.FORBIDDEN.value(),
                            "EMAIL_NOT_VERIFIED",
                            "Die KI-gestützte Wetteranalyse steht ausschließlich für verifizierte Benutzerkonten zur Verfügung.",
                            "/api/v1/routes/weather-ai-analysis",
                            List.of()
                    ));
        }

        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        String userId = principal.userId();
        List<List<Double>> coords = request.coordinates();
        String profileCode = request.activityProfile();
        Instant depTime = request.departureTime();
        int durSec = request.durationSeconds() != null ? (int) Math.round(request.durationSeconds()) : 0;
        int distMeters = request.distanceMeters() != null ? (int) Math.round(request.distanceMeters()) : 0;
        String calcId = request.routeCalculationId() != null ? request.routeCalculationId() : "direct";
        String startLabel = request.startLabel();
        String destLabel = request.destinationLabel();

        if ((coords == null || coords.size() < 2) && request.routeCalculationId() != null) {
            try {
                InternalRouteCalculationResponse routingResp =
                        routingClient.getRouteCalculation(userId, request.routeCalculationId());
                if (routingResp != null) {
                    calcId = routingResp.id();
                    if (depTime == null) depTime = routingResp.departureTime();
                    if (durSec <= 0 && routingResp.durationSeconds() != null) {
                        durSec = (int) Math.round(routingResp.durationSeconds());
                    }
                    if (distMeters <= 0 && routingResp.distanceMeters() != null) {
                        distMeters = (int) Math.round(routingResp.distanceMeters());
                    }
                    if (profileCode == null && routingResp.activityProfile() != null) {
                        profileCode = routingResp.activityProfile().code();
                    }
                    if (startLabel == null && routingResp.start() != null) {
                        startLabel = routingResp.start().label();
                    }
                    if (destLabel == null && routingResp.destination() != null) {
                        destLabel = routingResp.destination().label();
                    }
                    coords = extractCoordinates(routingResp.geometry());
                }
            } catch (Exception ignored) {
                // fall through
            }
        }

        if (coords == null || coords.size() < 2) {
            return ResponseEntity.badRequest().build();
        }

        if (profileCode == null || profileCode.isBlank()) {
            profileCode = "BICYCLE";
        }

        double speed = 20.0;
        int interval = 5;
        try {
            ActivityProfileResponse profile = activityProfileService.getActivityProfile(profileCode);
            if (profile != null) {
                if (profile.averageSpeedKmh() > 0) {
                    speed = profile.averageSpeedKmh();
                }
                if (profile.weatherQueryIntervalKm() != null && profile.weatherQueryIntervalKm() > 0) {
                    interval = profile.weatherQueryIntervalKm();
                }
            }
        } catch (Exception ignored) {
            // fallback defaults
        }

        RouteSnapshotRequest snapshot = new RouteSnapshotRequest(
                calcId,
                depTime != null ? depTime : Instant.now(),
                durSec,
                distMeters,
                coords,
                new ActivityProfileSnapshotRequest(profileCode, speed, interval)
        );

        RouteWeatherAiAnalysisRequest aiRequest = new RouteWeatherAiAnalysisRequest(
                userId,
                startLabel,
                destLabel,
                snapshot
        );

        RouteWeatherAiAnalysisResponse response = weatherClient.analyzeRouteWeatherAi(aiRequest);
        return ResponseEntity.ok(response);
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
}
