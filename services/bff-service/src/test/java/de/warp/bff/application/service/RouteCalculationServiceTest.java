// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.bff.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
    void startsRouteCalculationWithProfileSnapshot() {
        when(
                activityProfileService.getActivityProfile(
                        "BICYCLE"
                )
        ).thenReturn(
                bicycleProfile()
        );

        when(
                routingClient.startRouteCalculation(
                        anyString(),
                        argThat(request ->
                                request.requestedByUserId()
                                        .equals("usr_123")
                                        && request.activityProfile()
                                        .code()
                                        .equals("BICYCLE")
                                        && request.activityProfile()
                                        .routingProfile()
                                        .equals(
                                                "cycling-regular"
                                        )
                        )
                )
        ).thenReturn(
                new InternalRouteCalculationAcceptedResponse(
                        "calc_123",
                        "PENDING",
                        Instant.parse(
                                "2026-10-22T16:00:00Z"
                        )
                )
        );

        StartedRouteCalculation result =
                service.startRouteCalculation(
                        "usr_123",
                        validInput()
                );

        assertThat(result.id())
                .isEqualTo("calc_123");

        assertThat(result.status())
                .isEqualTo("PENDING");

        verify(activityProfileService)
                .getActivityProfile("BICYCLE");

        verify(routingClient)
                .startRouteCalculation(
                        anyString(),
                        argThat(this::hasExpectedSnapshot
                        )
                );
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

        InternalRouteCalculationResponse response =
                new InternalRouteCalculationResponse(
                        "calc_123",
                        "COMPLETED",
                        new InternalRouteCalculationResponse.RoutePoint(
                                "Mosbach",
                                49.3536,
                                9.1511
                        ),
                        new InternalRouteCalculationResponse.RoutePoint(
                                "Bad Mergentheim",
                                49.4924,
                                9.7736
                        ),
                        new InternalRouteCalculationResponse.ActivityProfileSnapshot(
                                "BICYCLE",
                                "cycling-regular"
                        ),
                        Instant.parse("2026-10-22T06:00:00Z"),
                        Instant.parse("2026-10-22T07:12:00Z"),
                        57600.0,
                        4320.0,
                        null,
                        segments,
                        Instant.parse("2026-10-22T05:59:00Z"),
                        Instant.parse("2026-10-22T06:00:00Z"),
                        null
                );

        when(routingClient.getRouteCalculation("usr_123", "calc_123"))
                .thenReturn(response);

        when(activityProfileService.getActivityProfile("BICYCLE"))
                .thenReturn(bicycleProfile());

        RouteCalculationResult result =
                service.getRouteCalculation("usr_123", "calc_123");

        assertThat(result.id()).isEqualTo("calc_123");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.segments()).isNotNull();
        assertThat(result.segments().isArray()).isTrue();
        assertThat(result.segments().get(0).get("distance").asDouble())
                .isEqualTo(57600.0);

        verify(routingClient).getRouteCalculation("usr_123", "calc_123");
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

        InternalRouteCalculationResponse response =
                new InternalRouteCalculationResponse(
                        "calc_123",
                        "COMPLETED",
                        new InternalRouteCalculationResponse.RoutePoint(
                                "Mosbach",
                                49.3536,
                                9.1511
                        ),
                        new InternalRouteCalculationResponse.RoutePoint(
                                "Bad Mergentheim",
                                49.4924,
                                9.7736
                        ),
                        new InternalRouteCalculationResponse.ActivityProfileSnapshot(
                                "BICYCLE",
                                "cycling-regular"
                        ),
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

        when(routingClient.getRouteCalculation("usr_123", "calc_123"))
                .thenReturn(response);
        when(activityProfileService.getActivityProfile("BICYCLE"))
                .thenReturn(bicycleProfile());
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
                                        new WeatherApiDtos.WeatherConditionResponse(
                                                14.0,
                                                180.0,
                                                20.0,
                                                0.5,
                                                35.0,
                                                16.5,
                                                95
                                        ),
                                        new WeatherApiDtos.GeoJsonLineStringResponse(
                                                "LineString",
                                                List.of(
                                                        List.of(9.1511, 49.3536),
                                                        List.of(9.4620, 49.4200)
                                                )
                                        )
                                )
                        )
                ));

        RouteCalculationResult result =
                service.getRouteCalculation("usr_123", "calc_123");

        assertThat(result.weatherEvaluation()).isNotNull();
        assertThat(result.weatherEvaluation().evaluationId()).isEqualTo("weval_123");
        assertThat(result.weatherEvaluation().overallScore()).isEqualTo(48);
        assertThat(result.weatherEvaluation().thunderstormCapApplied()).isTrue();
        assertThat(result.weatherEvaluation().segments()).hasSize(1);
        assertThat(result.weatherEvaluation().segments().getFirst().color()).isEqualTo("#eab308");

        verify(weatherClient).evaluateRouteWeather(argThat(req ->
                req.routeSnapshot().routeCalculationId().equals("calc_123")
                        && req.routeSnapshot().activityProfile().code().equals("BICYCLE")
                        && req.routeSnapshot().coordinates().size() == 3
        ));
    }

    private boolean hasExpectedSnapshot(
            InternalStartRouteCalculationRequest request
    ) {
        return request.requestedByUserId()
                .equals("usr_123")
                && request.activityProfile()
                .code()
                .equals("BICYCLE")
                && request.activityProfile()
                .routingProfile()
                .equals("cycling-regular");
    }

    private RouteCalculationInput validInput() {
        return new RouteCalculationInput(
                new RouteCalculationInput.RoutePoint(
                        "Mosbach",
                        49.3536,
                        9.1511
                ),
                new RouteCalculationInput.RoutePoint(
                        "Bad Mergentheim",
                        49.4924,
                        9.7736
                ),
                Instant.parse(
                        "2026-10-22T06:00:00Z"
                ),
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
                new ActivityProfileResponse
                        .WayPreferencesResponse(
                        true,
                        true,
                        true,
                        false,
                        true,
                        true
                )
        );
    }
}