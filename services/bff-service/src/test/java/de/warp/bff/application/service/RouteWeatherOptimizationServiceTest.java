// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.application.service;

import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.api.dto.RouteWeatherAiAnalysisApiRequest;
import de.warp.bff.api.dto.RouteWeatherOptimizationApiRequest;
import de.warp.bff.infrastructure.routing.RoutingClient;
import de.warp.bff.infrastructure.routing.dto.InternalRouteCalculationResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationResponse;
import de.warp.bff.infrastructure.weather.WeatherClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteWeatherOptimizationServiceTest {

    @Mock
    private WeatherClient weatherClient;

    @Mock
    private ActivityProfileService activityProfileService;

    @Mock
    private RoutingClient routingClient;

    private RouteWeatherOptimizationService service;

    @BeforeEach
    void setUp() {
        service = new RouteWeatherOptimizationService(
                weatherClient,
                activityProfileService,
                routingClient
        );
    }

    @Test
    void rejectsNullRequests() {
        assertThatThrownBy(() -> service.optimizeDeparture("user-1", null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.analyzeRouteWeatherAi("user-1", null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.analyzeRouteWeatherAi(null, new RouteWeatherAiAnalysisApiRequest(
                "calc-1", "A", "B", "BICYCLE", Instant.now(), 3600.0, 20000.0, List.of(List.of(1.0, 2.0), List.of(3.0, 4.0))
        ))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void optimizesDepartureWithExplicitCoordinatesAndProfile() {
        RouteWeatherOptimizationApiRequest request = new RouteWeatherOptimizationApiRequest(
                "calc-1",
                "MOTORCYCLE",
                Instant.parse("2026-05-16T08:00:00Z"),
                3600.0,
                50000.0,
                List.of(List.of(13.40, 52.52), List.of(13.20, 52.40)),
                Instant.parse("2026-05-16T07:00:00Z"),
                Instant.parse("2026-05-16T12:00:00Z")
        );

        when(activityProfileService.getActivityProfile("MOTORCYCLE")).thenReturn(
                new ActivityProfileResponse("MOTORCYCLE", "Motorrad", 75.0, "driving-car", 10, null)
        );
        when(weatherClient.optimizeDeparture(any())).thenReturn(
                new RouteWeatherOptimizationResponse(0, null, List.of())
        );

        RouteWeatherOptimizationResponse response = service.optimizeDeparture("user-1", request);

        assertThat(response).isNotNull();
        verify(weatherClient).optimizeDeparture(argThat(req ->
                req.requestedByUserId().equals("user-1")
                        && req.routeSnapshot().activityProfile().code().equals("MOTORCYCLE")
                        && req.routeSnapshot().activityProfile().averageSpeedKmh() == 75.0
                        && req.routeSnapshot().activityProfile().weatherQueryIntervalKm() == 10
        ));
    }

    @Test
    void resolvesCoordinatesFromRoutingClientWhenMissingInOptimizationRequest() {
        JsonNode geom = JsonMapper.builder().build().readTree("""
                {"type":"LineString","coordinates":[[9.15, 49.35], [9.45, 49.42], [9.77, 49.49]]}
                """);
        InternalRouteCalculationResponse routeResp = new InternalRouteCalculationResponse(
                "calc-lookup", "COMPLETED",
                new InternalRouteCalculationResponse.RoutePoint("Start", 49.35, 9.15),
                new InternalRouteCalculationResponse.RoutePoint("Ziel", 49.49, 9.77),
                new InternalRouteCalculationResponse.ActivityProfileSnapshot("BICYCLE", "cycling-regular"),
                Instant.parse("2026-05-16T08:00:00Z"), Instant.parse("2026-05-16T09:00:00Z"),
                30000.0, 3600.0, geom, null, Instant.now(), Instant.now(), null
        );

        when(routingClient.getRouteCalculation("user-1", "calc-lookup")).thenReturn(routeResp);
        when(activityProfileService.getActivityProfile("BICYCLE")).thenReturn(
                new ActivityProfileResponse("BICYCLE", "Fahrrad", 20.0, "cycling-regular", 5, null)
        );
        when(weatherClient.optimizeDeparture(any())).thenReturn(
                new RouteWeatherOptimizationResponse(0, null, List.of())
        );

        RouteWeatherOptimizationApiRequest request = new RouteWeatherOptimizationApiRequest(
                "calc-lookup", null, null, null, null, null, null, null
        );

        RouteWeatherOptimizationResponse response = service.optimizeDeparture("user-1", request);

        assertThat(response).isNotNull();
        verify(routingClient).getRouteCalculation("user-1", "calc-lookup");
    }

    @Test
    void throwsExceptionWhenCoordinatesCannotBeResolved() {
        when(routingClient.getRouteCalculation(any(), eq("calc-fail")))
                .thenThrow(new RuntimeException("Not found"));

        RouteWeatherOptimizationApiRequest request = new RouteWeatherOptimizationApiRequest(
                "calc-fail", null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.optimizeDeparture("user-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient route coordinates");
    }

    @Test
    void analyzesRouteWeatherAiWithDefaultsWhenProfileServiceThrows() {
        RouteWeatherAiAnalysisApiRequest request = new RouteWeatherAiAnalysisApiRequest(
                "calc-ai",
                null,
                null,
                "HIKING",
                Instant.parse("2026-05-16T08:00:00Z"),
                7200.0,
                10000.0,
                List.of(List.of(13.40, 52.52), List.of(13.30, 52.45))
        );

        when(activityProfileService.getActivityProfile("HIKING")).thenThrow(new RuntimeException("Service down"));
        when(weatherClient.analyzeRouteWeatherAi(any())).thenReturn(
                new RouteWeatherAiAnalysisResponse(
                        "calc-ai", "HIKING", "Wandern", Instant.now(), 80, false, "LOW",
                        "Headline", "Summary", "Advice", List.of(), List.of(), List.of(), List.of(), "model", Instant.now()
                )
        );

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeatherAi("user-1", request);

        assertThat(response).isNotNull();
        verify(weatherClient).analyzeRouteWeatherAi(argThat(req ->
                req.routeSnapshot().activityProfile().averageSpeedKmh() == 20.0
                        && req.routeSnapshot().activityProfile().weatherQueryIntervalKm() == 5
        ));
    }

    @Test
    void usesAnonymousWhenUserIdIsNullInOptimizeDeparture() {
        RouteWeatherOptimizationApiRequest request = new RouteWeatherOptimizationApiRequest(
                "calc-1", "BICYCLE", Instant.now(), 100.0, 100.0,
                List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)),
                Instant.now(), Instant.now()
        );
        when(weatherClient.optimizeDeparture(any())).thenReturn(new RouteWeatherOptimizationResponse(0, null, List.of()));

        RouteWeatherOptimizationResponse response = service.optimizeDeparture(null, request);
        assertThat(response).isNotNull();
        verify(weatherClient).optimizeDeparture(argThat(req -> "anonymous".equals(req.requestedByUserId())));
    }

    @Test
    void analyzesRouteWeatherAiWithExplicitLabelsAndResolvedRoutingDetails() {
        JsonNode geom = JsonMapper.builder().build().readTree("""
                {"type":"LineString","coordinates":[[13.4, 52.5], [13.2, 52.4]]}
                """);
        InternalRouteCalculationResponse routeResp = new InternalRouteCalculationResponse(
                "calc-ai-lookup", "COMPLETED",
                new InternalRouteCalculationResponse.RoutePoint("Start City", 52.5, 13.4),
                new InternalRouteCalculationResponse.RoutePoint("End City", 52.4, 13.2),
                new InternalRouteCalculationResponse.ActivityProfileSnapshot("MOTORCYCLE", "driving-car"),
                Instant.parse("2026-05-16T10:00:00Z"), Instant.parse("2026-05-16T11:00:00Z"),
                25000.0, 3600.0, geom, null, Instant.now(), Instant.now(), null
        );

        when(routingClient.getRouteCalculation("user-1", "calc-ai-lookup")).thenReturn(routeResp);
        when(weatherClient.analyzeRouteWeatherAi(any())).thenReturn(
                new RouteWeatherAiAnalysisResponse(
                        "calc-ai-lookup", "MOTORCYCLE", "Motorrad", Instant.now(), 90, false, "LOW",
                        "Headline", "Summary", "Advice", List.of(), List.of(), List.of(), List.of(), "model", Instant.now()
                )
        );

        RouteWeatherAiAnalysisApiRequest request = new RouteWeatherAiAnalysisApiRequest(
                "calc-ai-lookup",
                "Custom Start",
                "Custom Dest",
                null,
                null,
                null,
                null,
                null
        );

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeatherAi("user-1", request);

        assertThat(response).isNotNull();
        verify(weatherClient).analyzeRouteWeatherAi(argThat(req ->
                req.startLabel().equals("Custom Start")
                        && req.destinationLabel().equals("Custom Dest")
                        && req.routeSnapshot().routeCalculationId().equals("calc-ai-lookup")
                        && req.routeSnapshot().activityProfile().code().equals("MOTORCYCLE")
        ));
    }

    @Test
    void fallbackToRoutingLabelsWhenRequestLabelsAreNull() {
        JsonNode geom = JsonMapper.builder().build().readTree("""
                {"type":"LineString","coordinates":[[13.4, 52.5], [13.2, 52.4]]}
                """);
        InternalRouteCalculationResponse routeResp = new InternalRouteCalculationResponse(
                "calc-ai-fallback", "COMPLETED",
                new InternalRouteCalculationResponse.RoutePoint("Route Start", 52.5, 13.4),
                new InternalRouteCalculationResponse.RoutePoint("Route Dest", 52.4, 13.2),
                null, null, null, null, null, geom, null, Instant.now(), Instant.now(), null
        );

        when(routingClient.getRouteCalculation("user-1", "calc-ai-fallback")).thenReturn(routeResp);
        when(weatherClient.analyzeRouteWeatherAi(any())).thenReturn(
                new RouteWeatherAiAnalysisResponse(
                        "calc-ai-fallback", "BICYCLE", "Fahrrad", Instant.now(), 70, false, "LOW",
                        "Headline", "Summary", "Advice", List.of(), List.of(), List.of(), List.of(), "model", Instant.now()
                )
        );

        RouteWeatherAiAnalysisApiRequest request = new RouteWeatherAiAnalysisApiRequest(
                "calc-ai-fallback", null, null, null, null, null, null, null
        );

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeatherAi("user-1", request);

        assertThat(response).isNotNull();
        verify(weatherClient).analyzeRouteWeatherAi(argThat(req ->
                "Route Start".equals(req.startLabel())
                        && "Route Dest".equals(req.destinationLabel())
                        && "BICYCLE".equals(req.routeSnapshot().activityProfile().code())
        ));
    }

    @Test
    void handlesInvalidGeometryStructuresGracefully() {
        JsonNode nullGeom = JsonMapper.builder().build().readTree("null");
        InternalRouteCalculationResponse resp1 = new InternalRouteCalculationResponse(
                "calc-null-geom", "COMPLETED", null, null, null, null, null, null, null, nullGeom, null, Instant.now(), Instant.now(), null
        );
        when(routingClient.getRouteCalculation("user-1", "calc-null-geom")).thenReturn(resp1);

        RouteWeatherOptimizationApiRequest req1 = new RouteWeatherOptimizationApiRequest(
                "calc-null-geom", null, null, null, null, null, null, null
        );
        assertThatThrownBy(() -> service.optimizeDeparture("user-1", req1))
                .isInstanceOf(IllegalArgumentException.class);

        JsonNode invalidCoords = JsonMapper.builder().build().readTree("""
                {"type":"LineString","coordinates": "invalid"}
                """);
        InternalRouteCalculationResponse resp2 = new InternalRouteCalculationResponse(
                "calc-inv-coords", "COMPLETED", null, null, null, null, null, null, null, invalidCoords, null, Instant.now(), Instant.now(), null
        );
        when(routingClient.getRouteCalculation("user-1", "calc-inv-coords")).thenReturn(resp2);

        RouteWeatherOptimizationApiRequest req2 = new RouteWeatherOptimizationApiRequest(
                "calc-inv-coords", null, null, null, null, null, null, null
        );
        assertThatThrownBy(() -> service.optimizeDeparture("user-1", req2))
                .isInstanceOf(IllegalArgumentException.class);

        JsonNode mixedPoints = JsonMapper.builder().build().readTree("""
                {"type":"LineString","coordinates":[[13.4, 52.5], [13.2], "bad", [13.1, 52.3]]}
                """);
        InternalRouteCalculationResponse resp3 = new InternalRouteCalculationResponse(
                "calc-mixed-coords", "COMPLETED", null, null, null, null, null, null, null, mixedPoints, null, Instant.now(), Instant.now(), null
        );
        when(routingClient.getRouteCalculation("user-1", "calc-mixed-coords")).thenReturn(resp3);
        when(weatherClient.optimizeDeparture(any())).thenReturn(new RouteWeatherOptimizationResponse(0, null, List.of()));

        RouteWeatherOptimizationApiRequest req3 = new RouteWeatherOptimizationApiRequest(
                "calc-mixed-coords", null, null, null, null, null, null, null
        );
        RouteWeatherOptimizationResponse res3 = service.optimizeDeparture("user-1", req3);
        assertThat(res3).isNotNull();
    }

    @Test
    void optimizesDepartureWithNullCalculationIdAndExplicitDurationDistance() {
        when(activityProfileService.getActivityProfile("WALKING")).thenReturn(
                new ActivityProfileResponse("WALKING", "Spazieren", 5.0, "walking", null, null, null)
        );
        when(weatherClient.optimizeDeparture(any())).thenReturn(new RouteWeatherOptimizationResponse(0, null, List.of()));

        RouteWeatherOptimizationApiRequest request = new RouteWeatherOptimizationApiRequest(
                null, "WALKING", Instant.now(), 1800.0, 3000.0,
                List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)),
                Instant.now(), Instant.now()
        );

        RouteWeatherOptimizationResponse response = service.optimizeDeparture("user-1", request);
        assertThat(response).isNotNull();
        verify(weatherClient).optimizeDeparture(argThat(req ->
                "direct".equals(req.routeSnapshot().routeCalculationId())
                        && req.routeSnapshot().activityProfile().weatherQueryIntervalKm() == 5
        ));
    }

    @Test
    void resolvesRoutingDetailsWhenRequestAlreadyProvidesDurationDistanceAndProfile() {
        JsonNode geom = JsonMapper.builder().build().readTree("""
                {"type":"LineString","coordinates":[[13.4, 52.5], [13.2, 52.4]]}
                """);
        InternalRouteCalculationResponse routeResp = new InternalRouteCalculationResponse(
                "calc-provided", "COMPLETED",
                null, null,
                new InternalRouteCalculationResponse.ActivityProfileSnapshot("MOTORCYCLE", "driving-car"),
                Instant.parse("2026-05-16T10:00:00Z"), Instant.parse("2026-05-16T11:00:00Z"),
                25000.0, 3600.0, geom, null, Instant.now(), Instant.now(), null
        );

        when(routingClient.getRouteCalculation("user-1", "calc-provided")).thenReturn(routeResp);
        when(weatherClient.optimizeDeparture(any())).thenReturn(new RouteWeatherOptimizationResponse(0, null, List.of()));

        // Request provides duration, distance, profile, departureTime, but NO coordinates
        RouteWeatherOptimizationApiRequest request = new RouteWeatherOptimizationApiRequest(
                "calc-provided",
                "CUSTOM_PROFILE",
                Instant.parse("2026-05-16T12:00:00Z"),
                5000.0,
                15000.0,
                null,
                Instant.now(),
                Instant.now()
        );

        RouteWeatherOptimizationResponse response = service.optimizeDeparture("user-1", request);
        assertThat(response).isNotNull();
        verify(weatherClient).optimizeDeparture(argThat(req ->
                req.routeSnapshot().durationSeconds() == 5000
                        && req.routeSnapshot().distanceMeters() == 15000
                        && "CUSTOM_PROFILE".equals(req.routeSnapshot().activityProfile().code())
        ));
    }

    @Test
    void usesFallbackDefaultsWhenActivityProfileHasInvalidValues() {
        when(activityProfileService.getActivityProfile("CUSTOM")).thenReturn(
                new ActivityProfileResponse("CUSTOM", "Custom", -10.0, "driving", 0, null)
        );
        when(weatherClient.optimizeDeparture(any())).thenReturn(new RouteWeatherOptimizationResponse(0, null, List.of()));

        RouteWeatherOptimizationApiRequest request = new RouteWeatherOptimizationApiRequest(
                null, "CUSTOM", Instant.now(), 1000.0, 5000.0,
                List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)),
                Instant.now(), Instant.now()
        );

        RouteWeatherOptimizationResponse response = service.optimizeDeparture("user-1", request);
        assertThat(response).isNotNull();
        verify(weatherClient).optimizeDeparture(argThat(req ->
                req.routeSnapshot().activityProfile().averageSpeedKmh() == 20.0
                        && req.routeSnapshot().activityProfile().weatherQueryIntervalKm() == 5
        ));
    }
}
