// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-190] (89bd601) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): fix(saved-routes): truncate weatherEvaluationId and provide fallback weather snapshot (WARP-021)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.application.service;

import de.warp.bff.infrastructure.client.StorageClient;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.CreateSavedRouteRequest;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.UpdateSavedRouteRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos;
import de.warp.bff.infrastructure.weather.WeatherClient;
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
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private WeatherClient weatherClient;

    private SavedRouteService savedRouteService;

    @BeforeEach
    void setUp() {
        savedRouteService = new SavedRouteService(storageClient, routeCalculationService, weatherClient);
    }

    @Test
    void testConstructors() {
        SavedRouteService oneArg = new SavedRouteService(storageClient);
        assertThat(oneArg).isNotNull();
        SavedRouteService twoArg = new SavedRouteService(storageClient, routeCalculationService);
        assertThat(twoArg).isNotNull();
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
                null, "Tour", "calc_1", "weather_1", false
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> savedRouteService.saveRoute(
                "usr_1", "  ", "calc_1", "weather_1", false
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> savedRouteService.saveRoute(
                "usr_1", null, "calc_1", "weather_1", false
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

    @Test
    void saveRouteFetchesWeatherEvaluationFromWeatherClientWhenIdStartsWithWev() {
        when(routeCalculationService.getRouteCalculation("usr_1", "calc_1")).thenReturn(null);

        WeatherApiDtos.WeatherEvaluationResponse evalResp = new WeatherApiDtos.WeatherEvaluationResponse(
                "wev_12345678",
                "calc_1",
                "usr_1",
                "COMPLETED",
                Instant.parse("2026-10-24T08:00:00Z"),
                82,
                "2.0",
                false,
                Instant.parse("2026-10-24T07:55:00Z"),
                Instant.parse("2026-10-24T09:00:00Z"),
                Instant.parse("2026-10-24T07:55:00Z"),
                List.of(
                        new WeatherApiDtos.WeatherSegmentResponse(
                                0,
                                0,
                                Instant.parse("2026-10-24T08:00:00Z"),
                                82,
                                "#00FF00",
                                "NONE",
                                "Sonnig",
                                new WeatherApiDtos.WeatherConditionResponse(
                                        12.0, 180.0, 15.0, 0.0, 0.0, 18.0, 1
                                ),
                                new WeatherApiDtos.GeoJsonLineStringResponse(
                                        "LineString",
                                        List.of(List.of(9.15, 49.35), List.of(9.77, 49.49))
                                )
                        )
                )
        );

        when(weatherClient.getEvaluationById("wev_12345678")).thenReturn(evalResp);

        savedRouteService.saveRoute("usr_1", "Weather Route", "calc_1", "wev_12345678", true);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().weatherEvaluationId()).isEqualTo("wev_12345678");
        assertThat(req.weatherSnapshot().overallScore()).isEqualTo(82);
        assertThat(req.weatherSnapshot().segments()).hasSize(1);
        assertThat(req.weatherSnapshot().segments().getFirst().weather().windSpeedKmh()).isEqualTo(12.0);
    }

    @Test
    void saveRouteFetchesWeatherByRouteWhenWeatherIdDoesNotStartWithWev() {
        when(routeCalculationService.getRouteCalculation("usr_1", "calc_by_route")).thenReturn(null);

        WeatherApiDtos.WeatherEvaluationResponse evalResp = new WeatherApiDtos.WeatherEvaluationResponse(
                "eval_from_route",
                "calc_by_route",
                "usr_1",
                "COMPLETED",
                Instant.parse("2026-10-24T08:00:00Z"),
                90,
                "1.0",
                false,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now(),
                List.of(new WeatherApiDtos.WeatherSegmentResponse(
                        0, 0, Instant.now(), 90, "#00FF00", "NONE", "Gut", null, null
                ))
        );
        when(weatherClient.getEvaluationByRoute("calc_by_route")).thenReturn(evalResp);

        savedRouteService.saveRoute("usr_1", "By Route Tour", "calc_by_route", "custom_id", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().overallScore()).isEqualTo(90);
    }

    @Test
    void listSavedRoutesWithNullPageAndSizeUsesDefaults() {
        when(storageClient.listSavedRoutes("usr_1", null, 0, 20))
                .thenReturn(new SavedRoutePage(List.of(), 0, 20, 0, 0));

        SavedRoutePage page = savedRouteService.listSavedRoutes("usr_1", null, null, null);

        assertThat(page.items()).isEmpty();
        verify(storageClient).listSavedRoutes("usr_1", null, 0, 20);
    }

    @Test
    void saveRouteWithNullOrBlankWeatherEvaluationIdDefaultsToWeatherDummy() {
        savedRouteService.saveRoute("usr_1", "Tour", null, null, false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().weatherEvaluationId()).isEqualTo("weather_dummy");
        assertThat(req.routeSnapshot().routeCalculationId()).isNull();
    }

    @Test
    void saveRouteWithBlankWeatherEvaluationIdDefaultsToWeatherDummy() {
        savedRouteService.saveRoute("usr_1", "Tour", "calc_1", "   ", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().weatherEvaluationId()).isEqualTo("weather_dummy");
    }

    @Test
    void saveRouteWithoutRouteCalculationServiceUsesFallbackRouteSnapshot() {
        SavedRouteService serviceWithoutCalc = new SavedRouteService(storageClient, null, weatherClient);
        serviceWithoutCalc.saveRoute("usr_1", "No Calc Tour", "calc_1", "wev_1", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.routeSnapshot().start().label()).isEqualTo("Start");
        assertThat(req.routeSnapshot().destination().label()).isEqualTo("Ziel");
        assertThat(req.routeSnapshot().activityProfileCode()).isEqualTo("BICYCLE");
    }

    @Test
    void saveRouteWithCalculationHavingNullOptionalFieldsFallsBackToDefaults() {
        RouteCalculationResult calculationResult = new RouteCalculationResult(
                "calc_nulls",
                "COMPLETED",
                new RouteCalculationResult.RoutePoint("Start", 49.35, 9.15),
                new RouteCalculationResult.RoutePoint("Ziel", 49.49, 9.77),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                null
        );

        when(routeCalculationService.getRouteCalculation("usr_1", "calc_nulls"))
                .thenReturn(calculationResult);

        savedRouteService.saveRoute("usr_1", "Defaults Tour", "calc_nulls", "wev_1", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.routeSnapshot().activityProfileCode()).isEqualTo("BICYCLE");
        assertThat(req.routeSnapshot().distanceMeters()).isZero();
        assertThat(req.routeSnapshot().durationSeconds()).isZero();
        assertThat(req.routeSnapshot().geometry().coordinates()).hasSize(2);
    }

    @Test
    void saveRouteWithCalculationHavingMissingStartOrDestinationUsesFallbackSnapshot() {
        RouteCalculationResult calcMissingStart = new RouteCalculationResult(
                "calc_no_start",
                "COMPLETED",
                null,
                new RouteCalculationResult.RoutePoint("Ziel", 49.49, 9.77),
                null, null, null, null, null, null, null, Instant.now(), null
        );

        when(routeCalculationService.getRouteCalculation("usr_1", "calc_no_start"))
                .thenReturn(calcMissingStart);

        savedRouteService.saveRoute("usr_1", "No Start Tour", "calc_no_start", "wev_1", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.routeSnapshot().start().label()).isEqualTo("Start");
    }

    @Test
    void saveRouteWithVariousMalformedGeometryFormatsFallsBackToDefaultLineString() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Geometry without coordinates field
        var geomNoCoords = mapper.readTree("{\"type\":\"LineString\"}");
        RouteCalculationResult calc1 = new RouteCalculationResult(
                "calc_g1", "COMPLETED",
                new RouteCalculationResult.RoutePoint("Start", 49.35, 9.15),
                new RouteCalculationResult.RoutePoint("Ziel", 49.49, 9.77),
                null, null, null, null, null, geomNoCoords, null, Instant.now(), null
        );
        when(routeCalculationService.getRouteCalculation("usr_1", "calc_g1")).thenReturn(calc1);
        savedRouteService.saveRoute("usr_1", "Tour G1", "calc_g1", "wev_1", false);

        // 2. Geometry with coordinates not an array
        var geomNotArray = mapper.readTree("{\"coordinates\":\"not-array\"}");
        RouteCalculationResult calc2 = new RouteCalculationResult(
                "calc_g2", "COMPLETED",
                new RouteCalculationResult.RoutePoint("Start", 49.35, 9.15),
                new RouteCalculationResult.RoutePoint("Ziel", 49.49, 9.77),
                null, null, null, null, null, geomNotArray, null, Instant.now(), null
        );
        when(routeCalculationService.getRouteCalculation("usr_1", "calc_g2")).thenReturn(calc2);
        savedRouteService.saveRoute("usr_1", "Tour G2", "calc_g2", "wev_1", false);

        // 3. Geometry with coordinates containing invalid points (e.g. size < 2 or non-array)
        var geomBadPoints = mapper.readTree("{\"coordinates\":[123, [9.15], null]}");
        RouteCalculationResult calc3 = new RouteCalculationResult(
                "calc_g3", "COMPLETED",
                new RouteCalculationResult.RoutePoint("Start", 49.35, 9.15),
                new RouteCalculationResult.RoutePoint("Ziel", 49.49, 9.77),
                null, null, null, null, null, geomBadPoints, null, Instant.now(), null
        );
        when(routeCalculationService.getRouteCalculation("usr_1", "calc_g3")).thenReturn(calc3);
        savedRouteService.saveRoute("usr_1", "Tour G3", "calc_g3", "wev_1", false);

        // 4. Geometry with empty coordinates array
        var geomEmpty = mapper.readTree("{\"coordinates\":[]}");
        RouteCalculationResult calc4 = new RouteCalculationResult(
                "calc_g4", "COMPLETED",
                new RouteCalculationResult.RoutePoint("Start", 49.35, 9.15),
                new RouteCalculationResult.RoutePoint("Ziel", 49.49, 9.77),
                null, null, null, null, null, geomEmpty, null, Instant.now(), null
        );
        when(routeCalculationService.getRouteCalculation("usr_1", "calc_g4")).thenReturn(calc4);
        savedRouteService.saveRoute("usr_1", "Tour G4", "calc_g4", "wev_1", false);
    }

    @Test
    void saveRouteWithoutWeatherClientUsesFallbackWeatherSnapshot() {
        SavedRouteService serviceWithoutWeather = new SavedRouteService(storageClient, routeCalculationService, null);
        serviceWithoutWeather.saveRoute("usr_1", "No Weather Tour", "calc_1", "wev_123", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().overallScore()).isEqualTo(78);
        assertThat(req.weatherSnapshot().scoreVersion()).isEqualTo("1.0");
    }

    @Test
    void saveRouteWhenWeatherClientThrowsExceptionUsesFallback() {
        when(weatherClient.getEvaluationById("wev_fail")).thenThrow(new RuntimeException("Weather service down"));

        savedRouteService.saveRoute("usr_1", "Fail Tour", "calc_1", "wev_fail", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().overallScore()).isEqualTo(78);
    }

    @Test
    void saveRouteWhenEvaluationHasEmptyOrNullSegmentsUsesFallback() {
        WeatherApiDtos.WeatherEvaluationResponse emptySegmentsResp = new WeatherApiDtos.WeatherEvaluationResponse(
                "wev_empty", "calc_1", "usr_1", "COMPLETED",
                Instant.now(), 85, "1.0", false, Instant.now(), Instant.now(), Instant.now(),
                List.of()
        );
        when(weatherClient.getEvaluationById("wev_empty")).thenReturn(emptySegmentsResp);

        savedRouteService.saveRoute("usr_1", "Empty Segments Tour", "calc_1", "wev_empty", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().overallScore()).isEqualTo(78);
    }

    @Test
    void saveRouteWhenEvaluationIdIsNullUsesEffectiveWeatherId() {
        WeatherApiDtos.WeatherEvaluationResponse nullIdResp = new WeatherApiDtos.WeatherEvaluationResponse(
                null, "calc_1", "usr_1", "COMPLETED",
                Instant.now(), 85, null, false, Instant.now(), Instant.now(), Instant.now(),
                List.of(new WeatherApiDtos.WeatherSegmentResponse(
                        0, 0, Instant.now(), 85, "#00FF00", null, null
                ))
        );
        when(weatherClient.getEvaluationById("wev_null_id")).thenReturn(nullIdResp);

        savedRouteService.saveRoute("usr_1", "Null Eval ID Tour", "calc_1", "wev_null_id", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().weatherEvaluationId()).isEqualTo("wev_null_id");
        assertThat(req.weatherSnapshot().scoreVersion()).isEqualTo("1.0");
    }

    @Test
    void saveRouteWhenEvaluationIdIsTooLongUsesEffectiveWeatherId() {
        String longId = "wev_12345678901234567890123456789012345678901234567890";
        WeatherApiDtos.WeatherEvaluationResponse longIdResp = new WeatherApiDtos.WeatherEvaluationResponse(
                longId, "calc_1", "usr_1", "COMPLETED",
                Instant.now(), 85, "2.1", false, Instant.now(), Instant.now(), Instant.now(),
                List.of(new WeatherApiDtos.WeatherSegmentResponse(
                        0, 0, Instant.now(), 85, "#00FF00", null, null
                ))
        );
        when(weatherClient.getEvaluationById("wev_long_id")).thenReturn(longIdResp);

        savedRouteService.saveRoute("usr_1", "Long Eval ID Tour", "calc_1", "wev_long_id", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().weatherEvaluationId()).isEqualTo("wev_long_id");
        assertThat(req.weatherSnapshot().scoreVersion()).isEqualTo("2.1");
    }

    @Test
    void saveRouteWhenWeatherIdNotWevAndRouteCalculationIdBlankUsesFallback() {
        savedRouteService.saveRoute("usr_1", "No Route Calc Tour", "   ", "custom_id", false);

        ArgumentCaptor<CreateSavedRouteRequest> captor = ArgumentCaptor.forClass(CreateSavedRouteRequest.class);
        verify(storageClient).createSavedRoute(eq("usr_1"), anyString(), captor.capture());

        CreateSavedRouteRequest req = captor.getValue();
        assertThat(req.weatherSnapshot().overallScore()).isEqualTo(78);
    }
}
