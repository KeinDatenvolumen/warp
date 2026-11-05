// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.bff.application.service;

import de.warp.bff.infrastructure.client.StorageClient;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.CreateSavedRouteRequest;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.UpdateSavedRouteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedRouteServiceTest {

    @Mock
    private StorageClient storageClient;

    @Mock
    private RouteCalculationService routeCalculationService;

    private SavedRouteService savedRouteService;

    @BeforeEach
    void setUp() {
        savedRouteService = new SavedRouteService(storageClient, routeCalculationService);
    }

    @Test
    void listSavedRoutesValidatesPage() {
        assertThatThrownBy(() -> savedRouteService.listSavedRoutes("usr_1", null, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listSavedRoutesValidatesSize() {
        assertThatThrownBy(() -> savedRouteService.listSavedRoutes("usr_1", null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> savedRouteService.listSavedRoutes("usr_1", null, 0, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listSavedRoutesCallsStorageClient() {
        when(storageClient.listSavedRoutes("usr_1", true, 0, 20))
                .thenReturn(new SavedRoutePage(List.of(), 0, 20, 0, 0));

        SavedRoutePage page = savedRouteService.listSavedRoutes("usr_1", true, 0, 20);

        assertThat(page.items()).isEmpty();
        verify(storageClient).listSavedRoutes("usr_1", true, 0, 20);
    }

    @Test
    void getSavedRouteCallsStorageClient() {
        savedRouteService.getSavedRoute("usr_1", "route_1");
        verify(storageClient).getSavedRoute("usr_1", "route_1");
    }

    @Test
    void updateSavedRouteCallsStorageClient() {
        savedRouteService.updateSavedRoute("usr_1", "route_1", "Neue Tour", true);
        verify(storageClient).updateSavedRoute(
                eq("usr_1"),
                eq("route_1"),
                eq(new UpdateSavedRouteRequest("Neue Tour", true))
        );
    }

    @Test
    void deleteSavedRouteCallsStorageClient() {
        savedRouteService.deleteSavedRoute("usr_1", "route_1");
        verify(storageClient).deleteSavedRoute("usr_1", "route_1");
    }

    @Test
    void saveRouteValidatesUserIdAndName() {
        assertThatThrownBy(() -> savedRouteService.saveRoute(
                "", "Tour", "calc_1", "weather_1", false
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> savedRouteService.saveRoute(
                "usr_1", "  ", "calc_1", "weather_1", false
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveRouteAssemblesSnapshotsAndCallsStorageClient() {
        ObjectMapper mapper = new ObjectMapper();
        var geometryNode = mapper.readTree(
                "{\"type\":\"LineString\",\"coordinates\":[[9.15,49.35],[9.77,49.49]]}"
        );

        RouteCalculationResult calculationResult = new RouteCalculationResult(
                "calc_1",
                "COMPLETED",
                new RouteCalculationResult.RoutePoint("Start", 49.35, 9.15),
                new RouteCalculationResult.RoutePoint("Ziel", 49.49, 9.77),
                new RouteCalculationResult.ActivityProfileSnapshot(
                        "BICYCLE",
                        "Fahrrad"
                ),
                Instant.parse("2026-10-24T08:00:00Z"),
                Instant.parse("2026-10-24T09:00:00Z"),
                57600.0,
                11520.0,
                geometryNode,
                null,
                Instant.parse("2026-10-24T07:55:00Z"),
                null
        );

        when(routeCalculationService.getRouteCalculation("usr_1", "calc_1"))
                .thenReturn(calculationResult);

        savedRouteService.saveRoute("usr_1", "Meine Wochenendtour", "calc_1", "weather_1", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor =
                ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        ArgumentCaptor<String> idempCaptor = ArgumentCaptor.forClass(String.class);

        verify(storageClient).createSavedRoute(eq("usr_1"), idempCaptor.capture(), captor.capture());

        assertThat(idempCaptor.getValue()).startsWith("save_");
        CreateSavedRouteRequest request = captor.getValue();
        assertThat(request.name()).isEqualTo("Meine Wochenendtour");
        assertThat(request.favorite()).isFalse();
        assertThat(request.routeSnapshot().start().label()).isEqualTo("Start");
        assertThat(request.routeSnapshot().destination().label()).isEqualTo("Ziel");
        assertThat(request.routeSnapshot().distanceMeters()).isEqualTo(57600);
        assertThat(request.weatherSnapshot().weatherEvaluationId()).isEqualTo("weather_1");
        assertThat(request.routeSnapshot().geometry().coordinates()).hasSize(2);
    }

    @Test
    void saveRouteFallsBackToSensibleSnapshotWhenCalculationNotFound() {
        when(routeCalculationService.getRouteCalculation("usr_1", "calc_unknown"))
                .thenThrow(new RuntimeException("not found"));

        savedRouteService.saveRoute("usr_1", "Fallback Tour", "calc_unknown", "weather_1", true);

        ArgumentCaptor<CreateSavedRouteRequest> captor =
                ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest request = captor.getValue();
        assertThat(request.name()).isEqualTo("Fallback Tour");
        assertThat(request.favorite()).isTrue();
        assertThat(request.routeSnapshot().routeCalculationId()).isEqualTo("calc_unknown");
        assertThat(request.weatherSnapshot().weatherEvaluationId()).isEqualTo("weather_1");
    }

    @Test
    void saveRouteCapsWeatherEvaluationIdToAtMost40Characters() {
        when(routeCalculationService.getRouteCalculation(anyString(), anyString()))
                .thenThrow(new RuntimeException("not found"));

        String longWeatherId = "weather_calc_c8ba750ca8d44a7b975f28fe96b5f86c";
        savedRouteService.saveRoute("usr_1", "Long Weather Tour", "calc_1", longWeatherId, false);

        ArgumentCaptor<CreateSavedRouteRequest> captor =
                ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest request = captor.getValue();
        assertThat(request.weatherSnapshot().weatherEvaluationId()).hasSizeLessThanOrEqualTo(40);
        assertThat(request.weatherSnapshot().weatherEvaluationId())
                .isEqualTo(longWeatherId.substring(0, 40));
    }
}
