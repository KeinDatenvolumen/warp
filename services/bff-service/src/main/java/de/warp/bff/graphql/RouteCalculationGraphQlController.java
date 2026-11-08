// [KI-Log Nr. KI-163] (41862d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-179] (5bc6d66) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-200] (c059ad7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: require verified email for route calculation (WARP-030)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
import de.warp.bff.application.exception.EmailNotVerifiedException;
import tools.jackson.databind.JsonNode;

// [KI-Log Nr. KI-15 & KI-17] KI-generiert (Google Antigravity Gemini 3.8 Flash High / Gemini 4 Argon Medium)
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
            if (!principal.emailVerified()) {
                throw new EmailNotVerifiedException();
            }

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
        } catch (EmailNotVerifiedException exception) {
            return new RouteCalculationPayload(
                    null,
                    List.of(
                            new RouteCalculationMutationError(
                                    null,
                                    "EMAIL_NOT_VERIFIED",
                                    "Die E-Mail-Adresse ist noch nicht verifiziert."
                            )
                    )
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
                routeCalculationService.getRouteCalculation(
                        principal.userId(),
                        id
                );

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
            List<RouteSegmentPayload> segments,
            String weatherEvaluationId,
            Integer weatherScore,
            String weatherScoreVersion,
            Boolean thunderstormCapApplied,
            List<WeatherSegmentPayload> weatherSegments,
            WeatherEvaluationPayload weatherEvaluation,
            OffsetDateTime createdAt,
            RouteCalculationErrorPayload error
    ) {

        private static RouteCalculationPayloadResult from(
                RouteCalculationResult calculation
        ) {
            WeatherEvaluationPayload weatherEvalPayload =
                    WeatherEvaluationPayload.from(
                            calculation.weatherEvaluation()
                    );

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
                    RouteSegmentPayload.from(
                            calculation.segments()
                    ),
                    weatherEvalPayload == null
                            ? null
                            : weatherEvalPayload.evaluationId(),
                    weatherEvalPayload == null
                            ? null
                            : weatherEvalPayload.overallScore(),
                    weatherEvalPayload == null
                            ? null
                            : weatherEvalPayload.scoreVersion(),
                    weatherEvalPayload == null
                            ? null
                            : weatherEvalPayload.thunderstormCapApplied(),
                    weatherEvalPayload == null
                            ? null
                            : weatherEvalPayload.segments(),
                    weatherEvalPayload,
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

    public record WeatherEvaluationPayload(
            String evaluationId,
            String routeCalculationId,
            RouteCalculationStatus status,
            OffsetDateTime departureTime,
            int overallScore,
            String scoreVersion,
            boolean thunderstormCapApplied,
            OffsetDateTime weatherDataGeneratedAt,
            OffsetDateTime weatherDataExpiresAt,
            OffsetDateTime createdAt,
            List<WeatherSegmentPayload> segments
    ) {

        private static WeatherEvaluationPayload from(
                RouteCalculationResult.WeatherEvaluationSnapshot snapshot
        ) {
            if (snapshot == null) {
                return null;
            }

            List<WeatherSegmentPayload> segments =
                    snapshot.segments() == null
                            ? List.of()
                            : snapshot.segments().stream()
                            .map(WeatherSegmentPayload::from)
                            .toList();

            RouteCalculationStatus status;
            try {
                status = snapshot.status() == null
                        ? RouteCalculationStatus.COMPLETED
                        : RouteCalculationStatus.valueOf(snapshot.status());
            } catch (IllegalArgumentException e) {
                status = RouteCalculationStatus.COMPLETED;
            }

            return new WeatherEvaluationPayload(
                    snapshot.evaluationId(),
                    snapshot.routeCalculationId(),
                    status,
                    RouteCalculationPayloadResult.toOffsetDateTime(
                            snapshot.departureTime()
                    ),
                    snapshot.overallScore(),
                    snapshot.scoreVersion(),
                    snapshot.thunderstormCapApplied(),
                    RouteCalculationPayloadResult.toOffsetDateTime(
                            snapshot.weatherDataGeneratedAt()
                    ),
                    RouteCalculationPayloadResult.toOffsetDateTime(
                            snapshot.weatherDataExpiresAt()
                    ),
                    RouteCalculationPayloadResult.toOffsetDateTime(
                            snapshot.createdAt()
                    ),
                    segments
            );
        }
    }

    public record WeatherSegmentPayload(
            int segmentIndex,
            Integer distanceFromStartMeters,
            OffsetDateTime expectedArrivalTime,
            int score,
            String color,
            String limitingFactor,
            String reasonLabel,
            SegmentWeatherPayload weather,
            RouteGeometryPayload geometry
    ) {

        private static WeatherSegmentPayload from(
                RouteCalculationResult.WeatherSegmentSnapshot segment
        ) {
            RouteGeometryPayload geometry =
                    segment.geometryCoordinates() == null
                            || segment.geometryCoordinates().isEmpty()
                            ? null
                            : new RouteGeometryPayload(
                            "LineString",
                            segment.geometryCoordinates()
                    );

            return new WeatherSegmentPayload(
                    segment.segmentIndex(),
                    segment.distanceFromStartMeters(),
                    RouteCalculationPayloadResult.toOffsetDateTime(
                            segment.expectedArrivalTime()
                    ),
                    segment.score(),
                    segment.color(),
                    segment.limitingFactor(),
                    segment.reasonLabel(),
                    SegmentWeatherPayload.from(segment.weather(), segment.limitingFactor(), segment.reasonLabel()),
                    geometry
            );
        }
    }

    public record SegmentWeatherPayload(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double windGustsKmh,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double wetRoadIndex,
            Double thunderstormProbabilityPercent,
            Double temperatureCelsius,
            Double apparentTemperatureCelsius,
            Integer weatherCode,
            String limitingFactor,
            String reasonLabel
    ) {

        private static SegmentWeatherPayload from(
                RouteCalculationResult.WeatherConditionSnapshot weather,
                String limitingFactor,
                String reasonLabel
        ) {
            if (weather == null) {
                return null;
            }
            return new SegmentWeatherPayload(
                    weather.windSpeedKmh(),
                    weather.windDirectionDegrees(),
                    weather.windGustsKmh(),
                    weather.precipitationProbabilityPercent(),
                    weather.precipitationMm(),
                    weather.wetRoadIndex(),
                    weather.thunderstormProbabilityPercent(),
                    weather.temperatureCelsius(),
                    weather.apparentTemperatureCelsius(),
                    weather.weatherCode(),
                    limitingFactor,
                    reasonLabel
            );
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
                            : typeNode.asString(),
                    List.copyOf(coordinates)
            );
        }
    }

    public record RouteSegmentPayload(
            double distanceMeters,
            double durationSeconds
    ) {

        private static List<RouteSegmentPayload> from(
                JsonNode segments
        ) {
            if (segments == null || !segments.isArray()) {
                return null;
            }

            List<RouteSegmentPayload> list =
                    new java.util.ArrayList<>();

            for (JsonNode segment : segments) {
                if (segment == null || !segment.isObject()) {
                    continue;
                }

                JsonNode distanceNode = segment.get("distance");
                JsonNode durationNode = segment.get("duration");

                if (distanceNode != null && durationNode != null) {
                    list.add(
                            new RouteSegmentPayload(
                                    distanceNode.asDouble(),
                                    durationNode.asDouble()
                            )
                    );
                }
            }

            return List.copyOf(list);
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
