// [KI-Log Nr. KI-163] (41862d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-179] (5bc6d66) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.infrastructure.routing.RoutingClient;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationAcceptedResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalStartRouteCalculationRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos;
import de.warp.bff.infrastructure.weather.WeatherClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class RouteCalculationServiceTest {

    @Mock
    private ActivityProfileService activityProfileService;

    @Mock
    private RoutingClient routingClient;

    @Mock
    private WeatherClient weatherClient;

    private RouteCalculationService service;

    @BeforeEach
    void setUp() {
        service = new RouteCalculationService(
                activityProfileService,
                routingClient,
                weatherClient
        );
    }

    @Test
    void testConstructors() {
        RouteCalculationService twoArgService = new RouteCalculationService(activityProfileService, routingClient);
        assertThat(twoArgService).isNotNull();
    }

    @Test
    void validatesUserIdOnStartRouteCalculation() {
        assertThatThrownBy(() -> service.startRouteCalculation(null, validInput()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.startRouteCalculation("", validInput()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesInputFieldsOnStartRouteCalculation() {
        assertThatThrownBy(() -> service.startRouteCalculation("usr_123", null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.startRouteCalculation("usr_123", new RouteCalculationInput(
                null, new RouteCalculationInput.RoutePoint("B", 1.0, 1.0), Instant.now(), "BICYCLE"
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.startRouteCalculation("usr_123", new RouteCalculationInput(
                new RouteCalculationInput.RoutePoint("A", 1.0, 1.0), null, Instant.now(), "BICYCLE"
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.startRouteCalculation("usr_123", new RouteCalculationInput(
                new RouteCalculationInput.RoutePoint("A", 1.0, 1.0),
                new RouteCalculationInput.RoutePoint("B", 1.0, 1.0),
                null, "BICYCLE"
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.startRouteCalculation("usr_123", new RouteCalculationInput(
                new RouteCalculationInput.RoutePoint("A", 1.0, 1.0),
                new RouteCalculationInput.RoutePoint("B", 1.0, 1.0),
                Instant.now(), null
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.startRouteCalculation("usr_123", new RouteCalculationInput(
                new RouteCalculationInput.RoutePoint("A", 1.0, 1.0),
                new RouteCalculationInput.RoutePoint("B", 1.0, 1.0),
                Instant.now(), "   "
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesCalculationIdOnGetRouteCalculation() {
        assertThatThrownBy(() -> service.getRouteCalculation("usr_123", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getRouteCalculation("usr_123", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startsRouteCalculationWithProfileSnapshot() {
        when(activityProfileService.getActivityProfile("BICYCLE")).thenReturn(bicycleProfile());

        when(routingClient.startRouteCalculation(anyString(), argThat(this::hasExpectedSnapshot)))
                .thenReturn(new InternalRouteCalculationAcceptedResponse(
                        "calc_123", "PENDING", Instant.parse("2026-10-22T16:00:00Z")
                ));

        StartedRouteCalculation result = service.startRouteCalculation("usr_123", validInput());

        assertThat(result.id()).isEqualTo("calc_123");
        assertThat(result.status()).isEqualTo("PENDING");
        verify(activityProfileService).getActivityProfile("BICYCLE");
        verify(routingClient).startRouteCalculation(anyString(), any());
    }

    @Test
    void returnsRouteCalculationForOwnerWithSegments() {
        var segments = JsonMapper.builder().build().readTree("""
                [
                  {
                    "distance": 57600.0,
                    "duration": 4320.0
                  }
                ]
                """);

        InternalRouteCalculationResponse response = new InternalRouteCalculationResponse(
                "calc_123",
                "COMPLETED",
                new InternalRouteCalculationResponse.RoutePoint("Mosbach", 49.3536, 9.1511),
                new InternalRouteCalculationResponse.RoutePoint("Bad Mergentheim", 49.4924, 9.7736),
                new InternalRouteCalculationResponse.ActivityProfileSnapshot("BICYCLE", "cycling-regular"),
                Instant.parse("2026-10-22T06:00:00Z"),
                Instant.parse("2026-10-22T07:12:00Z"),
                57600.0,
                4320.0,
                null,
                segments,
                Instant.parse("2026-10-22T05:59:00Z"),
                Instant.parse("2026-10-22T06:00:00Z"),
                new InternalRouteCalculationResponse.RouteCalculationError("ROUTING_FAILED", "No route found")
        );

        when(routingClient.getRouteCalculation("usr_123", "calc_123")).thenReturn(response);
        when(activityProfileService.getActivityProfile("BICYCLE")).thenReturn(bicycleProfile());

        RouteCalculationResult result = service.getRouteCalculation("usr_123", "calc_123");

        assertThat(result.id()).isEqualTo("calc_123");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.segments()).isNotNull();
        assertThat(result.error()).isNotNull();
        assertThat(result.error().code()).isEqualTo("ROUTING_FAILED");

        // Overload getRouteCalculation(id)
        when(routingClient.getRouteCalculation(null, "calc_123")).thenReturn(response);
        RouteCalculationResult resultOverload = service.getRouteCalculation("calc_123");
        assertThat(resultOverload.id()).isEqualTo("calc_123");
    }

    @Test
    void evaluatesRouteWeatherWhenCompletedRouteHasGeometry() {
        var geometry = JsonMapper.builder().build().readTree("""
                {
                  "type": "LineString",
                  "coordinates": [
                    [9.1511, 49.3536],
                    [9.4620, 49.4200],
                    [9.7736, 49.4924]
                  ]
                }
                """);

        InternalRouteCalculationResponse response = new InternalRouteCalculationResponse(
                "calc_123",
                "COMPLETED",
                new InternalRouteCalculationResponse.RoutePoint("Mosbach", 49.3536, 9.1511),
                new InternalRouteCalculationResponse.RoutePoint("Bad Mergentheim", 49.4924, 9.7736),
                new InternalRouteCalculationResponse.ActivityProfileSnapshot("BICYCLE", "cycling-regular"),
                Instant.parse("2026-10-22T06:00:00Z"),
                Instant.parse("2026-10-22T07:12:00Z"),
                57600.0,
                4320.0,
                geometry,
                null,
                Instant.parse("2026-10-22T05:59:00Z"),
                Instant.parse("2026-10-22T06:00:00Z"),
                null
        );

        when(routingClient.getRouteCalculation("usr_123", "calc_123")).thenReturn(response);
        when(activityProfileService.getActivityProfile("BICYCLE")).thenReturn(bicycleProfile());
        when(weatherClient.evaluateRouteWeather(any()))
                .thenReturn(new WeatherApiDtos.WeatherEvaluationResponse(
                        "weval_123",
                        "calc_123",
                        "usr_123",
                        "COMPLETED",
                        Instant.parse("2026-10-22T06:00:00Z"),
                        48,
                        "v1.0",
                        true,
                        Instant.parse("2026-10-22T06:00:01Z"),
                        Instant.parse("2026-10-22T07:00:01Z"),
                        Instant.parse("2026-10-22T06:00:01Z"),
                        List.of(
                                new WeatherApiDtos.WeatherSegmentResponse(
                                        0,
                                        0,
                                        Instant.parse("2026-10-22T06:00:00Z"),
                                        48,
                                        "#eab308",
                                        "THUNDER",
                                        "Gewitter",
                                        new WeatherApiDtos.WeatherConditionResponse(
                                                14.0, 180.0, 20.0, 0.5, 35.0, 16.5, 95
                                        ),
                                        new WeatherApiDtos.GeoJsonLineStringResponse(
                                                "LineString",
                                                List.of(List.of(9.1511, 49.3536), List.of(9.4620, 49.4200))
                                        )
                                )
                        )
                ));

        RouteCalculationResult result = service.getRouteCalculation("usr_123", "calc_123");

        assertThat(result.weatherEvaluation()).isNotNull();
        assertThat(result.weatherEvaluation().evaluationId()).isEqualTo("weval_123");
        assertThat(result.weatherEvaluation().overallScore()).isEqualTo(48);
        assertThat(result.weatherEvaluation().thunderstormCapApplied()).isTrue();
        assertThat(result.weatherEvaluation().segments()).hasSize(1);
    }

    @Test
    void skipsWeatherEvaluationWhenNotCompletedOrGeometryEmptyOrClientThrows() {
        InternalRouteCalculationResponse responsePending = new InternalRouteCalculationResponse(
                "calc_pending", "PENDING",
                new InternalRouteCalculationResponse.RoutePoint("A", 1.0, 1.0),
                new InternalRouteCalculationResponse.RoutePoint("B", 2.0, 2.0),
                new InternalRouteCalculationResponse.ActivityProfileSnapshot("HIKING", "walking"),
                Instant.now(), null, null, null, null, null, Instant.now(), null, null
        );
        when(routingClient.getRouteCalculation("usr_123", "calc_pending")).thenReturn(responsePending);
        when(activityProfileService.getActivityProfile("HIKING")).thenReturn(
                new ActivityProfileResponse("HIKING", "Wandern", 0.0, "walking", 0, null)
        );

        RouteCalculationResult result = service.getRouteCalculation("usr_123", "calc_pending");
        assertThat(result.weatherEvaluation()).isNull();
        verify(weatherClient, never()).evaluateRouteWeather(any());

        // When weatherClient throws
        var geometry = JsonMapper.builder().build().readTree("""
                {"type":"LineString","coordinates":[[9.15, 49.35], [9.77, 49.49]]}
                """);
        InternalRouteCalculationResponse responseCompleted = new InternalRouteCalculationResponse(
                "calc_completed", "COMPLETED",
                new InternalRouteCalculationResponse.RoutePoint("A", 1.0, 1.0),
                new InternalRouteCalculationResponse.RoutePoint("B", 2.0, 2.0),
                new InternalRouteCalculationResponse.ActivityProfileSnapshot("MOTORCYCLE", "driving-car"),
                Instant.now(), null, 1000.0, 100.0, geometry, null, Instant.now(), null, null
        );
        when(routingClient.getRouteCalculation("usr_123", "calc_completed")).thenReturn(responseCompleted);
        when(activityProfileService.getActivityProfile("MOTORCYCLE")).thenReturn(
                new ActivityProfileResponse("MOTORCYCLE", "Motorrad", 0.0, "driving-car", 0, null)
        );
        when(weatherClient.evaluateRouteWeather(any())).thenThrow(new RuntimeException("Weather service error"));

        RouteCalculationResult resultWithWeatherError = service.getRouteCalculation("usr_123", "calc_completed");
        assertThat(resultWithWeatherError.weatherEvaluation()).isNull();
    }

    private boolean hasExpectedSnapshot(InternalStartRouteCalculationRequest request) {
        return request.requestedByUserId().equals("usr_123")
                && request.activityProfile().code().equals("BICYCLE")
                && request.activityProfile().routingProfile().equals("cycling-regular");
    }

    private RouteCalculationInput validInput() {
        return new RouteCalculationInput(
                new RouteCalculationInput.RoutePoint("Mosbach", 49.3536, 9.1511),
                new RouteCalculationInput.RoutePoint("Bad Mergentheim", 49.4924, 9.7736),
                Instant.parse("2026-10-22T06:00:00Z"),
                "BICYCLE"
        );
    }

    private ActivityProfileResponse bicycleProfile() {
        return new ActivityProfileResponse(
                "BICYCLE",
                "Fahrrad",
                18.0,
                "cycling-regular",
                15,
                new ActivityProfileResponse.WayPreferencesResponse(true, true, true, false, true, true)
        );
    }
}
