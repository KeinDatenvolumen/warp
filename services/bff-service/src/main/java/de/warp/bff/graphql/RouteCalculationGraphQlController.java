package de.warp.bff.graphql;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import de.warp.bff.application.exception
        .InvalidRouteRequestException;
import de.warp.bff.application.service
        .RouteCalculationInput;
import de.warp.bff.application.service
        .RouteCalculationResult;
import de.warp.bff.application.service
        .RouteCalculationService;
import de.warp.bff.application.service
        .StartedRouteCalculation;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import tools.jackson.databind.JsonNode;

@Controller
public class RouteCalculationGraphQlController {

    private final RouteCalculationService routeCalculationService;

    public RouteCalculationGraphQlController(
            RouteCalculationService routeCalculationService
    ) {
        this.routeCalculationService =
                routeCalculationService;
    }

    @MutationMapping
    public RouteCalculationPayload startRouteCalculation(
            @Argument RouteCalculationGraphQlInput input,
            @AuthenticationPrincipal
            AuthenticatedSessionPrincipal principal
    ) {
        if (principal == null) {
            throw new SessionInvalidException();
        }

        try {
            StartedRouteCalculation calculation =
                    routeCalculationService.startRouteCalculation(
                            principal.userId(),
                            toApplicationInput(input)
                    );

            return new RouteCalculationPayload(
                    StartedRouteCalculationPayload.from(
                            calculation
                    ),
                    List.of()
            );
        } catch (InvalidRouteRequestException exception) {
            return new RouteCalculationPayload(
                    null,
                    List.of(
                            new RouteCalculationMutationError(
                                    null,
                                    "INVALID_ROUTE_REQUEST",
                                    "Die Routenanfrage ist ungültig."
                            )
                    )
            );
        }
    }

    @QueryMapping
    public RouteCalculationPayloadResult routeCalculation(
            @Argument String id,
            @AuthenticationPrincipal
            AuthenticatedSessionPrincipal principal
    ) {
        if (principal == null) {
            throw new SessionInvalidException();
        }

        RouteCalculationResult calculation =
                routeCalculationService.getRouteCalculation(id);

        return RouteCalculationPayloadResult.from(
                calculation
        );
    }

    private RouteCalculationInput toApplicationInput(
            RouteCalculationGraphQlInput input
    ) {
        if (input == null) {
            throw new InvalidRouteRequestException();
        }

        return new RouteCalculationInput(
                toApplicationPoint(input.start()),
                toApplicationPoint(input.destination()),
                input.departureTime() == null
                        ? null
                        : input.departureTime().toInstant(),
                input.activityProfile() == null
                        ? null
                        : input.activityProfile().name()
        );
    }

    private RouteCalculationInput.RoutePoint toApplicationPoint(
            RoutePointGraphQlInput point
    ) {
        if (point == null) {
            return null;
        }

        return new RouteCalculationInput.RoutePoint(
                point.label(),
                point.latitude(),
                point.longitude()
        );
    }

    public record RouteCalculationGraphQlInput(
            RoutePointGraphQlInput start,
            RoutePointGraphQlInput destination,
            OffsetDateTime departureTime,
            ActivityProfileCode activityProfile
    ) {
    }

    public record RoutePointGraphQlInput(
            String label,
            double latitude,
            double longitude
    ) {
    }

    public enum ActivityProfileCode {
        BICYCLE,
        MOTORCYCLE,
        HIKING
    }

    public record RouteCalculationPayload(
            StartedRouteCalculationPayload calculation,
            List<RouteCalculationMutationError> errors
    ) {
    }

    public record StartedRouteCalculationPayload(
            String id,
            RouteCalculationStatus status,
            OffsetDateTime createdAt
    ) {

        private static StartedRouteCalculationPayload from(
                StartedRouteCalculation calculation
        ) {
            return new StartedRouteCalculationPayload(
                    calculation.id(),
                    RouteCalculationStatus.valueOf(
                            calculation.status()
                    ),
                    calculation.createdAt()
                            .atOffset(ZoneOffset.UTC)
            );
        }
    }

    public record RouteCalculationMutationError(
            String field,
            String code,
            String message
    ) {
    }

    public record RouteCalculationPayloadResult(
            String id,
            RouteCalculationStatus status,
            RoutePointPayload start,
            RoutePointPayload destination,
            ActivityProfilePayload activityProfile,
            OffsetDateTime departureTime,
            OffsetDateTime arrivalTime,
            Double distanceMeters,
            Double durationSeconds,
            RouteGeometryPayload geometry,
            OffsetDateTime createdAt,
            RouteCalculationErrorPayload error
    ) {

        private static RouteCalculationPayloadResult from(
                RouteCalculationResult calculation
        ) {
            return new RouteCalculationPayloadResult(
                    calculation.id(),
                    RouteCalculationStatus.valueOf(
                            calculation.status()
                    ),
                    RoutePointPayload.from(
                            calculation.start()
                    ),
                    RoutePointPayload.from(
                            calculation.destination()
                    ),
                    ActivityProfilePayload.from(
                            calculation.activityProfile()
                    ),
                    toOffsetDateTime(
                            calculation.departureTime()
                    ),
                    toOffsetDateTime(
                            calculation.arrivalTime()
                    ),
                    calculation.distanceMeters(),
                    calculation.durationSeconds(),
                    RouteGeometryPayload.from(
                            calculation.geometry()
                    ),
                    toOffsetDateTime(
                            calculation.createdAt()
                    ),
                    RouteCalculationErrorPayload.from(
                            calculation.error()
                    )
            );
        }

        private static OffsetDateTime toOffsetDateTime(
                java.time.Instant instant
        ) {
            return instant == null
                    ? null
                    : instant.atOffset(ZoneOffset.UTC);
        }
    }

    public record RoutePointPayload(
            String label,
            double latitude,
            double longitude
    ) {

        private static RoutePointPayload from(
                RouteCalculationResult.RoutePoint point
        ) {
            if (point == null) {
                return null;
            }

            return new RoutePointPayload(
                    point.label(),
                    point.latitude(),
                    point.longitude()
            );
        }
    }

    public record ActivityProfilePayload(
            ActivityProfileCode code,
            String displayName
    ) {

        private static ActivityProfilePayload from(
                RouteCalculationResult
                        .ActivityProfileSnapshot profile
        ) {
            if (profile == null) {
                return null;
            }

            return new ActivityProfilePayload(
                    ActivityProfileCode.valueOf(
                            profile.code()
                    ),
                    profile.displayName()
            );
        }
    }

    public record RouteGeometryPayload(
            String type,
            List<List<Double>> coordinates
    ) {

        private static RouteGeometryPayload from(
                JsonNode geometry
        ) {
            if (geometry == null || geometry.isNull()) {
                return null;
            }

            JsonNode coordinatesNode =
                    geometry.get("coordinates");

            if (coordinatesNode == null
                    || !coordinatesNode.isArray()) {
                return null;
            }

            List<List<Double>> coordinates =
                    new java.util.ArrayList<>();

            for (JsonNode coordinate : coordinatesNode) {
                if (!coordinate.isArray()
                        || coordinate.size() < 2) {
                    continue;
                }

                coordinates.add(
                        List.of(
                                coordinate.get(0).asDouble(),
                                coordinate.get(1).asDouble()
                        )
                );
            }

            JsonNode typeNode = geometry.get("type");

            return new RouteGeometryPayload(
                    typeNode == null
                            ? "LineString"
                            : typeNode.asText(),
                    List.copyOf(coordinates)
            );
        }
    }

    public record RouteCalculationErrorPayload(
            String code,
            String message
    ) {

        private static RouteCalculationErrorPayload from(
                RouteCalculationResult
                        .RouteCalculationError error
        ) {
            if (error == null) {
                return null;
            }

            return new RouteCalculationErrorPayload(
                    error.code(),
                    error.message()
            );
        }
    }

    public enum RouteCalculationStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}