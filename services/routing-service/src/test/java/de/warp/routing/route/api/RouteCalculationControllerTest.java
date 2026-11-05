// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import de.warp.routing.profile.domain.ActivityProfileCode;
import de.warp.routing.route.application.RouteCalculationService;
import de.warp.routing.route.domain.RouteCalculationStatus;
import de.warp.routing.route.persistence.RouteCalculationEntity;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RouteCalculationControllerTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-10-21T18:00:00Z");

    @Mock
    private RouteCalculationService routeCalculationService;

    private RouteCalculationController controller;

    @BeforeEach
    void setUp() {
        controller = new RouteCalculationController(
                routeCalculationService,
                new ObjectMapper()
        );
    }

    @Test
    void startsRouteCalculationWithAcceptedStatus() {
        StartRouteCalculationRequest request =
                createRequest();

        RouteCalculationAcceptedResponse serviceResponse =
                new RouteCalculationAcceptedResponse(
                        "calc_123",
                        RouteCalculationStatus.PENDING,
                        CREATED_AT
                );

        when(
                routeCalculationService.startCalculation(
                        "idempotency-key-123",
                        request
                )
        ).thenReturn(serviceResponse);

        ResponseEntity<RouteCalculationAcceptedResponse> response =
                controller.startRouteCalculation(
                        "idempotency-key-123",
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);

        assertThat(response.getHeaders().getLocation())
                .hasToString(
                        "/internal/v1/route-calculations/calc_123"
                );

        assertThat(response.getBody())
                .isEqualTo(serviceResponse);

        verify(routeCalculationService)
                .startCalculation(
                        "idempotency-key-123",
                        request
                );
    }

    @Test
    void returnsPendingRouteCalculation() {
        RouteCalculationEntity calculation =
                createCalculation();

        when(
                routeCalculationService.getCalculation(
                        "calc_123"
                )
        ).thenReturn(calculation);

        RouteCalculationResponse response =
                controller.getRouteCalculation(
                        "calc_123"
                );

        assertThat(response.id())
                .isEqualTo("calc_123");

        assertThat(response.status())
                .isEqualTo(RouteCalculationStatus.PENDING);

        assertThat(response.start().label())
                .isEqualTo("Mosbach");

        assertThat(response.destination().label())
                .isEqualTo("Bad Mergentheim");

        assertThat(response.activityProfile().code())
                .isEqualTo(ActivityProfileCode.BICYCLE);

        assertThat(response.activityProfile().routingProfile())
                .isEqualTo("cycling-regular");

        assertThat(response.geometry())
                .isNull();

        assertThat(response.segments())
                .isNull();

        assertThat(response.error())
                .isNull();
    }

    @Test
    void returnsCompletedRouteCalculationWithJsonGeometry() {
        RouteCalculationEntity calculation =
                createCalculation();

        calculation.markProcessing(
                Instant.parse("2026-10-21T18:00:01Z")
        );

        calculation.complete(
                Instant.parse("2026-10-22T07:12:00Z"),
                57600,
                4320,
                """
                {
                  "type": "LineString",
                  "coordinates": [
                    [9.1511, 49.3536],
                    [9.7736, 49.4924]
                  ]
                }
                """,
                """
                [
                  {
                    "distance": 57600,
                    "duration": 4320
                  }
                ]
                """,
                Instant.parse("2026-10-21T18:00:03Z")
        );

        when(
                routeCalculationService.getCalculation(
                        "calc_123"
                )
        ).thenReturn(calculation);

        RouteCalculationResponse response =
                controller.getRouteCalculation(
                        "calc_123"
                );

        assertThat(response.status())
                .isEqualTo(RouteCalculationStatus.COMPLETED);

        assertThat(response.distanceMeters())
                .isEqualTo(57600);

        assertThat(response.durationSeconds())
                .isEqualTo(4320);

        assertThat(response.arrivalTime())
                .isEqualTo(
                        Instant.parse("2026-10-22T07:12:00Z")
                );

        assertThat(response.geometry())
                .isNotNull();

        assertThat(
                response.geometry()
                        .get("type")
                        .asText()
        ).isEqualTo("LineString");

        assertThat(
                response.geometry()
                        .get("coordinates")
                        .size()
        ).isEqualTo(2);

        assertThat(response.segments())
                .isNotNull();

        assertThat(response.segments().isArray())
                .isTrue();

        assertThat(response.segments().size())
                .isEqualTo(1);

        assertThat(response.error())
                .isNull();
    }

    @Test
    void returnsFailedRouteCalculationWithStableError() {
        RouteCalculationEntity calculation =
                createCalculation();

        calculation.fail(
                "ROUTE_NOT_FOUND",
                "Für Start, Ziel und Aktivitätsprofil "
                        + "konnte keine Route gefunden werden.",
                Instant.parse("2026-10-21T18:00:03Z")
        );

        when(
                routeCalculationService.getCalculation(
                        "calc_123"
                )
        ).thenReturn(calculation);

        RouteCalculationResponse response =
                controller.getRouteCalculation(
                        "calc_123"
                );

        assertThat(response.status())
                .isEqualTo(RouteCalculationStatus.FAILED);

        assertThat(response.error())
                .isNotNull();

        assertThat(response.error().code())
                .isEqualTo("ROUTE_NOT_FOUND");

        assertThat(response.error().message())
                .contains("keine Route gefunden");

        assertThat(response.geometry())
                .isNull();

        assertThat(response.distanceMeters())
                .isNull();
    }

    private StartRouteCalculationRequest createRequest() {
        return new StartRouteCalculationRequest(
                "usr_123",
                new RoutePointRequest(
                        "Mosbach",
                        49.3536,
                        9.1511
                ),
                new RoutePointRequest(
                        "Bad Mergentheim",
                        49.4924,
                        9.7736
                ),
                Instant.parse(
                        "2026-10-22T06:00:00Z"
                ),
                new ActivityProfileSnapshotRequest(
                        ActivityProfileCode.BICYCLE,
                        "cycling-regular"
                )
        );
    }

    private RouteCalculationEntity createCalculation() {
        return new RouteCalculationEntity(
                "calc_123",
                "usr_123",
                "idempotency-key-123",
                "Mosbach",
                49.3536,
                9.1511,
                "Bad Mergentheim",
                49.4924,
                9.7736,
                Instant.parse(
                        "2026-10-22T06:00:00Z"
                ),
                ActivityProfileCode.BICYCLE,
                "cycling-regular",
                CREATED_AT
        );
    }
}