// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.bff.graphql;

import de.warp.bff.application.exception.StorageValidationException;
import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.application.service.ActivityProfileService;
import de.warp.bff.application.service.SavedRouteService;
import de.warp.bff.graphql.ActivityProfileGraphQlController.ActivityProfileCode;
import de.warp.bff.graphql.ActivityProfileGraphQlController.ActivityProfilePayload;
import de.warp.bff.graphql.SavedRoutePayloads.DeleteSavedRoutePayload;
import de.warp.bff.graphql.SavedRoutePayloads.PageInfoPayload;
import de.warp.bff.graphql.SavedRoutePayloads.PageInput;
import de.warp.bff.graphql.SavedRoutePayloads.RouteGeometryPayload;
import de.warp.bff.graphql.SavedRoutePayloads.RoutePlacePayload;
import de.warp.bff.graphql.SavedRoutePayloads.SaveRouteInput;
import de.warp.bff.graphql.SavedRoutePayloads.SaveRoutePayload;
import de.warp.bff.graphql.SavedRoutePayloads.SavedRouteFilterInput;
import de.warp.bff.graphql.SavedRoutePayloads.SavedRoutePagePayload;
import de.warp.bff.graphql.SavedRoutePayloads.SavedRoutePayload;
import de.warp.bff.graphql.SavedRoutePayloads.SavedRouteSummaryPayload;
import de.warp.bff.graphql.SavedRoutePayloads.SegmentWeatherPayload;
import de.warp.bff.graphql.SavedRoutePayloads.UpdateSavedRouteInput;
import de.warp.bff.graphql.SavedRoutePayloads.UpdateSavedRoutePayload;
import de.warp.bff.graphql.SavedRoutePayloads.UserErrorPayload;
import de.warp.bff.graphql.SavedRoutePayloads.WeatherSegmentPayload;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SavedRouteGraphQlController {

    private static final int MAX_NAME_LENGTH = 100;

    private final SavedRouteService savedRouteService;
    private final ActivityProfileService activityProfileService;

    public SavedRouteGraphQlController(
            SavedRouteService savedRouteService,
            ActivityProfileService activityProfileService
    ) {
        this.savedRouteService = savedRouteService;
        this.activityProfileService = activityProfileService;
    }

    @QueryMapping
    public SavedRoutePagePayload savedRoutes(
            @Argument SavedRouteFilterInput filter,
            @Argument PageInput page,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        String userId = requireUserId(principal);

        StorageApiDtos.SavedRoutePage result =
                savedRouteService.listSavedRoutes(
                        userId,
                        filter == null ? null : filter.favorite(),
                        page == null ? null : page.page(),
                        page == null ? null : page.size()
                );

        Map<String, String> profileNames = profileDisplayNames();

        return new SavedRoutePagePayload(
                result.items().stream()
                        .map(item -> toSummary(item, profileNames))
                        .toList(),
                new PageInfoPayload(
                        result.page(),
                        result.size(),
                        result.totalElements(),
                        result.totalPages()
                )
        );
    }

    @QueryMapping
    public SavedRoutePayload savedRoute(
            @Argument String id,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        String userId = requireUserId(principal);

        return toDetail(
                savedRouteService.getSavedRoute(userId, id),
                profileDisplayNames()
        );
    }

    @MutationMapping
    public SaveRoutePayload saveRoute(
            @Argument SaveRouteInput input,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        String userId = requireUserId(principal);

        List<UserErrorPayload> errors = validate(input);

        if (!errors.isEmpty()) {
            return new SaveRoutePayload(null, errors);
        }

        try {
            StorageApiDtos.SavedRoute saved =
                    savedRouteService.saveRoute(
                            userId,
                            input.name(),
                            input.routeCalculationId(),
                            input.weatherEvaluationId(),
                            Boolean.TRUE.equals(input.favorite())
                    );

            return new SaveRoutePayload(
                    toDetail(saved, profileDisplayNames()),
                    List.of()
            );
        } catch (StorageValidationException exception) {
            return new SaveRoutePayload(
                    null,
                    exception.getFieldErrors().stream()
                            .map(error -> new UserErrorPayload(
                                    error.field(), error.code(), error.message()
                            ))
                            .toList()
            );
        }
    }

    @MutationMapping
    public UpdateSavedRoutePayload updateSavedRoute(
            @Argument String id,
            @Argument UpdateSavedRouteInput input,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        String userId = requireUserId(principal);

        List<UserErrorPayload> errors = validate(input);

        if (!errors.isEmpty()) {
            return new UpdateSavedRoutePayload(null, errors);
        }

        String name = input.name() == null ? null : input.name().trim();

        try {
            StorageApiDtos.SavedRoute updated =
                    savedRouteService.updateSavedRoute(
                            userId, id, name, input.favorite()
                    );

            return new UpdateSavedRoutePayload(
                    toDetail(updated, profileDisplayNames()),
                    List.of()
            );
        } catch (StorageValidationException exception) {
            return new UpdateSavedRoutePayload(
                    null,
                    exception.getFieldErrors().stream()
                            .map(error -> new UserErrorPayload(
                                    error.field(), error.code(), error.message()
                            ))
                            .toList()
            );
        }
    }

    @MutationMapping
    public DeleteSavedRoutePayload deleteSavedRoute(
            @Argument String id,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        String userId = requireUserId(principal);

        savedRouteService.deleteSavedRoute(userId, id);

        return new DeleteSavedRoutePayload(id, List.of());
    }

    private String requireUserId(AuthenticatedSessionPrincipal principal) {
        if (principal == null) {
            throw new SessionInvalidException();
        }

        return principal.userId();
    }

    private List<UserErrorPayload> validate(SaveRouteInput input) {
        if (input == null) {
            return List.of(new UserErrorPayload(
                    "input",
                    "INPUT_REQUIRED",
                    "Die Eingabedaten müssen angegeben werden."
            ));
        }

        List<UserErrorPayload> errors = new ArrayList<>();

        if (input.name() == null || input.name().trim().isEmpty()) {
            errors.add(new UserErrorPayload(
                    "name",
                    "INVALID_NAME",
                    "Der Routenname darf nicht leer sein."
            ));
        } else if (input.name().trim().length() > MAX_NAME_LENGTH) {
            errors.add(new UserErrorPayload(
                    "name",
                    "NAME_TOO_LONG",
                    "Der Routenname darf maximal "
                            + MAX_NAME_LENGTH + " Zeichen enthalten."
            ));
        }

        if (input.routeCalculationId() == null || input.routeCalculationId().isBlank()) {
            errors.add(new UserErrorPayload(
                    "routeCalculationId",
                    "REQUIRED",
                    "Die Routenberechnung muss angegeben werden."
            ));
        }

        if (input.weatherEvaluationId() == null || input.weatherEvaluationId().isBlank()) {
            errors.add(new UserErrorPayload(
                    "weatherEvaluationId",
                    "REQUIRED",
                    "Die Wetterbewertung muss angegeben werden."
            ));
        }

        return errors;
    }

    private List<UserErrorPayload> validate(UpdateSavedRouteInput input) {
        if (input == null
                || (input.name() == null && input.favorite() == null)) {
            return List.of(new UserErrorPayload(
                    "input",
                    "AT_LEAST_ONE_FIELD_REQUIRED",
                    "Mindestens ein Feld muss geändert werden."
            ));
        }

        if (input.name() != null) {
            String trimmed = input.name().trim();

            if (trimmed.isEmpty()) {
                return List.of(new UserErrorPayload(
                        "name",
                        "INVALID_NAME",
                        "Der Routenname darf nicht leer sein."
                ));
            }

            if (trimmed.length() > MAX_NAME_LENGTH) {
                return List.of(new UserErrorPayload(
                        "name",
                        "NAME_TOO_LONG",
                        "Der Routenname darf maximal "
                                + MAX_NAME_LENGTH + " Zeichen enthalten."
                ));
            }
        }

        return List.of();
    }

    /**
     * Ein einziger Aufruf pro Request. Fällt der ActivityProfileService aus, wird
     * der Profilcode als Anzeigename verwendet, damit gespeicherte Routen
     * trotzdem lesbar bleiben.
     */
    private Map<String, String> profileDisplayNames() {
        try {
            return activityProfileService.getActivityProfiles().stream()
                    .collect(Collectors.toMap(
                            ActivityProfileResponse::code,
                            ActivityProfileResponse::displayName,
                            (first, second) -> first
                    ));
        } catch (IllegalStateException exception) {
            return Map.of();
        }
    }

    private ActivityProfilePayload toProfile(
            String code,
            Map<String, String> profileNames
    ) {
        return new ActivityProfilePayload(
                ActivityProfileCode.valueOf(code),
                profileNames.getOrDefault(code, code)
        );
    }

    private SavedRouteSummaryPayload toSummary(
            StorageApiDtos.SavedRouteSummary item,
            Map<String, String> profileNames
    ) {
        return new SavedRouteSummaryPayload(
                item.id(),
                item.name(),
                toPlace(item.start()),
                toPlace(item.destination()),
                toProfile(item.activityProfileCode(), profileNames),
                item.weatherScore(),
                item.favorite(),
                item.createdAt()
        );
    }

    private SavedRoutePayload toDetail(
            StorageApiDtos.SavedRoute route,
            Map<String, String> profileNames
    ) {
        StorageApiDtos.RouteSnapshot snapshot = route.routeSnapshot();
        StorageApiDtos.WeatherSnapshot weather = route.weatherSnapshot();

        return new SavedRoutePayload(
                route.id(),
                route.name(),
                toPlace(snapshot.start()),
                toPlace(snapshot.destination()),
                toProfile(snapshot.activityProfileCode(), profileNames),
                snapshot.departureTime(),
                snapshot.arrivalTime(),
                snapshot.distanceMeters(),
                snapshot.durationSeconds(),
                toGeometry(snapshot.geometry()),
                weather.overallScore(),
                weather.scoreVersion(),
                weather.thunderstormCapApplied(),
                weather.segments().stream().map(this::toSegment).toList(),
                route.favorite(),
                route.createdAt(),
                route.updatedAt()
        );
    }

    private WeatherSegmentPayload toSegment(
            StorageApiDtos.WeatherSegment segment
    ) {
        StorageApiDtos.SegmentWeather weather = segment.weather();

        return new WeatherSegmentPayload(
                segment.segmentIndex(),
                segment.expectedArrivalTime(),
                segment.score(),
                segment.color(),
                weather == null
                        ? null
                        : new SegmentWeatherPayload(
                        weather.windSpeedKmh(),
                        weather.windDirectionDegrees(),
                        weather.precipitationProbabilityPercent(),
                        weather.precipitationMm(),
                        weather.thunderstormProbabilityPercent()
                ),
                segment.geometry() == null
                        ? null
                        : toGeometry(segment.geometry())
        );
    }

    private RoutePlacePayload toPlace(StorageApiDtos.Place place) {
        return new RoutePlacePayload(
                place.label(),
                place.latitude(),
                place.longitude()
        );
    }

    private RouteGeometryPayload toGeometry(
            StorageApiDtos.GeoJsonLineString geometry
    ) {
        return new RouteGeometryPayload(
                geometry.type(),
                geometry.coordinates()
        );
    }
}