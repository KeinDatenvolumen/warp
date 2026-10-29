package de.warp.bff.graphql;

import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.application.exception.StorageValidationException;
import de.warp.bff.application.service.ActivityProfileService;
import de.warp.bff.application.service.SavedRouteService;
import de.warp.bff.graphql.SavedRoutePayloads.DeleteSavedRoutePayload;
import de.warp.bff.graphql.SavedRoutePayloads.PageInput;
import de.warp.bff.graphql.SavedRoutePayloads.SaveRouteInput;
import de.warp.bff.graphql.SavedRoutePayloads.SaveRoutePayload;
import de.warp.bff.graphql.SavedRoutePayloads.SavedRouteFilterInput;
import de.warp.bff.graphql.SavedRoutePayloads.SavedRoutePagePayload;
import de.warp.bff.graphql.SavedRoutePayloads.SavedRoutePayload;
import de.warp.bff.graphql.SavedRoutePayloads.UpdateSavedRouteInput;
import de.warp.bff.graphql.SavedRoutePayloads.UpdateSavedRoutePayload;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoute;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRouteSummary;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.StorageFieldError;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedRouteGraphQlControllerTest {

    private static final AuthenticatedSessionPrincipal PRINCIPAL =
            new AuthenticatedSessionPrincipal(
                    "usr_123",
                    Instant.parse("2026-10-23T08:00:00Z"),
                    Instant.parse("2026-10-23T20:00:00Z"),
                    true
            );

    @Mock
    private SavedRouteService savedRouteService;

    @Mock
    private ActivityProfileService activityProfileService;

    private SavedRouteGraphQlController controller;

    @BeforeEach
    void setUp() {
        controller = new SavedRouteGraphQlController(
                savedRouteService,
                activityProfileService
        );
    }

    @Test
    void savedRoutesRequiresAuthentication() {
        assertThatThrownBy(() -> controller.savedRoutes(null, null, null))
                .isInstanceOf(SessionInvalidException.class);
    }

    @Test
    void savedRoutesReturnsPaginatedList() {
        when(activityProfileService.getActivityProfiles()).thenReturn(List.of(
                bicycleProfile()
        ));

        OffsetDateTime now = OffsetDateTime.parse("2026-10-23T10:00:00Z");
        SavedRouteSummary item = new SavedRouteSummary(
                "route_1",
                "Fahrradtour",
                new StorageApiDtos.Place("Mosbach", 49.35, 9.15),
                new StorageApiDtos.Place("Bad Mergentheim", 49.49, 9.77),
                "BICYCLE",
                78,
                false,
                now
        );

        when(savedRouteService.listSavedRoutes(eq("usr_123"), any(), any(), any()))
                .thenReturn(new SavedRoutePage(List.of(item), 0, 20, 1, 1));

        SavedRoutePagePayload result = controller.savedRoutes(
                new SavedRouteFilterInput(false),
                new PageInput(0, 20),
                PRINCIPAL
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().name()).isEqualTo("Fahrradtour");
        assertThat(result.items().getFirst().activityProfile().displayName())
                .isEqualTo("Fahrrad");
        assertThat(result.pageInfo().totalElements()).isEqualTo(1);
    }

    @Test
    void savedRouteRequiresAuthentication() {
        assertThatThrownBy(() -> controller.savedRoute("route_1", null))
                .isInstanceOf(SessionInvalidException.class);
    }

    @Test
    void savedRouteReturnsDetail() {
        when(activityProfileService.getActivityProfiles()).thenReturn(List.of(
                bicycleProfile()
        ));

        SavedRoute route = createSampleRoute("route_1", "Fahrradtour");
        when(savedRouteService.getSavedRoute("usr_123", "route_1")).thenReturn(route);

        SavedRoutePayload result = controller.savedRoute("route_1", PRINCIPAL);

        assertThat(result.id()).isEqualTo("route_1");
        assertThat(result.name()).isEqualTo("Fahrradtour");
        assertThat(result.weatherScore()).isEqualTo(78);
        assertThat(result.weatherSegments()).hasSize(1);
    }

    @Test
    void saveRouteRequiresAuthentication() {
        SaveRouteInput input = new SaveRouteInput("Tour", "calc_1", "weather_1", false);
        assertThatThrownBy(() -> controller.saveRoute(input, null))
                .isInstanceOf(SessionInvalidException.class);
    }

    @Test
    void saveRouteValidatesMissingOrBlankName() {
        SaveRouteInput input = new SaveRouteInput("   ", "calc_1", "weather_1", false);

        SaveRoutePayload payload = controller.saveRoute(input, PRINCIPAL);

        assertThat(payload.route()).isNull();
        assertThat(payload.errors()).hasSize(1);
        assertThat(payload.errors().getFirst().field()).isEqualTo("name");
        assertThat(payload.errors().getFirst().code()).isEqualTo("INVALID_NAME");
    }

    @Test
    void saveRouteValidatesNameTooLong() {
        String longName = "A".repeat(101);
        SaveRouteInput input = new SaveRouteInput(longName, "calc_1", "weather_1", false);

        SaveRoutePayload payload = controller.saveRoute(input, PRINCIPAL);

        assertThat(payload.route()).isNull();
        assertThat(payload.errors()).hasSize(1);
        assertThat(payload.errors().getFirst().field()).isEqualTo("name");
        assertThat(payload.errors().getFirst().code()).isEqualTo("NAME_TOO_LONG");
    }

    @Test
    void saveRouteValidatesMissingCalculationOrWeatherId() {
        SaveRouteInput input = new SaveRouteInput("Tour", "", null, false);

        SaveRoutePayload payload = controller.saveRoute(input, PRINCIPAL);

        assertThat(payload.route()).isNull();
        assertThat(payload.errors()).hasSize(2);
        assertThat(payload.errors()).extracting("field")
                .containsExactlyInAnyOrder("routeCalculationId", "weatherEvaluationId");
    }

    @Test
    void saveRouteSuccessfullySavesRoute() {
        when(activityProfileService.getActivityProfiles()).thenReturn(List.of(
                bicycleProfile()
        ));

        SavedRoute savedRoute = createSampleRoute("route_new", "Neue Tour");
        when(savedRouteService.saveRoute(
                eq("usr_123"),
                eq("Neue Tour"),
                eq("calc_1"),
                eq("weather_1"),
                eq(false)
        )).thenReturn(savedRoute);

        SaveRouteInput input = new SaveRouteInput("Neue Tour", "calc_1", "weather_1", false);
        SaveRoutePayload payload = controller.saveRoute(input, PRINCIPAL);

        assertThat(payload.errors()).isEmpty();
        assertThat(payload.route()).isNotNull();
        assertThat(payload.route().id()).isEqualTo("route_new");
        assertThat(payload.route().name()).isEqualTo("Neue Tour");
    }

    @Test
    void saveRouteTranslatesStorageValidationException() {
        when(savedRouteService.saveRoute(
                anyString(), anyString(), anyString(), anyString(), anyBoolean()
        )).thenThrow(new StorageValidationException(
                List.of(new StorageFieldError("name", "INVALID_NAME", "Ungültiger Name")),
                new RuntimeException()
        ));

        SaveRouteInput input = new SaveRouteInput("Tour", "calc_1", "weather_1", false);
        SaveRoutePayload payload = controller.saveRoute(input, PRINCIPAL);

        assertThat(payload.route()).isNull();
        assertThat(payload.errors()).hasSize(1);
        assertThat(payload.errors().getFirst().field()).isEqualTo("name");
    }

    @Test
    void updateSavedRouteRequiresAuthentication() {
        UpdateSavedRouteInput input = new UpdateSavedRouteInput("Neuer Name", true);
        assertThatThrownBy(() -> controller.updateSavedRoute("route_1", input, null))
                .isInstanceOf(SessionInvalidException.class);
    }

    @Test
    void updateSavedRouteValidatesEmptyInput() {
        UpdateSavedRouteInput input = new UpdateSavedRouteInput(null, null);

        UpdateSavedRoutePayload payload = controller.updateSavedRoute("route_1", input, PRINCIPAL);

        assertThat(payload.route()).isNull();
        assertThat(payload.errors()).hasSize(1);
        assertThat(payload.errors().getFirst().code()).isEqualTo("AT_LEAST_ONE_FIELD_REQUIRED");
    }

    @Test
    void updateSavedRouteValidatesBlankName() {
        UpdateSavedRouteInput input = new UpdateSavedRouteInput("   ", null);

        UpdateSavedRoutePayload payload = controller.updateSavedRoute("route_1", input, PRINCIPAL);

        assertThat(payload.route()).isNull();
        assertThat(payload.errors()).hasSize(1);
        assertThat(payload.errors().getFirst().code()).isEqualTo("INVALID_NAME");
    }

    @Test
    void updateSavedRouteSuccessfullyUpdates() {
        when(activityProfileService.getActivityProfiles()).thenReturn(List.of(
                bicycleProfile()
        ));

        SavedRoute updated = createSampleRoute("route_1", "Aktualisierter Name");
        when(savedRouteService.updateSavedRoute("usr_123", "route_1", "Aktualisierter Name", true))
                .thenReturn(updated);

        UpdateSavedRouteInput input = new UpdateSavedRouteInput("Aktualisierter Name", true);
        UpdateSavedRoutePayload payload = controller.updateSavedRoute("route_1", input, PRINCIPAL);

        assertThat(payload.errors()).isEmpty();
        assertThat(payload.route()).isNotNull();
        assertThat(payload.route().name()).isEqualTo("Aktualisierter Name");
    }

    @Test
    void deleteSavedRouteRequiresAuthentication() {
        assertThatThrownBy(() -> controller.deleteSavedRoute("route_1", null))
                .isInstanceOf(SessionInvalidException.class);
    }

    @Test
    void deleteSavedRouteDeletesRoute() {
        DeleteSavedRoutePayload payload = controller.deleteSavedRoute("route_1", PRINCIPAL);

        verify(savedRouteService).deleteSavedRoute("usr_123", "route_1");
        assertThat(payload.deletedRouteId()).isEqualTo("route_1");
        assertThat(payload.errors()).isEmpty();
    }

    private ActivityProfileResponse bicycleProfile() {
        return new ActivityProfileResponse(
                "BICYCLE",
                "Fahrrad",
                15.0,
                "cycling-regular",
                10,
                null
        );
    }

    private SavedRoute createSampleRoute(String id, String name) {
        OffsetDateTime now = OffsetDateTime.parse("2026-10-23T10:00:00Z");
        return new SavedRoute(
                id,
                name,
                false,
                new StorageApiDtos.RouteSnapshot(
                        "calc_1",
                        new StorageApiDtos.Place("Start", 49.35, 9.15),
                        new StorageApiDtos.Place("Ziel", 49.49, 9.77),
                        "BICYCLE",
                        now,
                        now.plusHours(1),
                        10000,
                        3600,
                        new StorageApiDtos.GeoJsonLineString(
                                "LineString",
                                List.of(List.of(9.15, 49.35), List.of(9.77, 49.49))
                        )
                ),
                new StorageApiDtos.WeatherSnapshot(
                        "weather_1",
                        78,
                        "1.0",
                        false,
                        List.of(
                                new StorageApiDtos.WeatherSegment(
                                        0,
                                        now.plusMinutes(30),
                                        78,
                                        "#55AA40",
                                        null,
                                        null
                                )
                        )
                ),
                now,
                now
        );
    }
}
