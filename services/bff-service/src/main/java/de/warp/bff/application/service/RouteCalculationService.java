package de.warp.bff.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.infrastructure.routing.RoutingClient;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationAcceptedResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalStartRouteCalculationRequest;

@Service
public class RouteCalculationService {

    private final ActivityProfileService activityProfileService;
    private final RoutingClient routingClient;

    public RouteCalculationService(
            ActivityProfileService activityProfileService,
            RoutingClient routingClient
    ) {
        this.activityProfileService =
                activityProfileService;
        this.routingClient = routingClient;
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

        return mapResult(
                response,
                activityProfile
        );
    }

    public RouteCalculationResult getRouteCalculation(
            String calculationId
    ) {
        return getRouteCalculation(null, calculationId);
    }

    private RouteCalculationResult mapResult(
            InternalRouteCalculationResponse response,
            ActivityProfileResponse activityProfile
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