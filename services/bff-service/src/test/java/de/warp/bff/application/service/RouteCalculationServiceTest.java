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
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RouteCalculationServiceTest {

    @Mock
    private ActivityProfileService activityProfileService;

    @Mock
    private RoutingClient routingClient;

    private RouteCalculationService service;

    @BeforeEach
    void setUp() {
        service = new RouteCalculationService(
                activityProfileService,
                routingClient
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