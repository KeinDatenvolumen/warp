// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.warp.routing.infrastructure.routing.OpenRouteServiceClient;
import de.warp.routing.infrastructure.routing.OpenRouteServiceResponse;
import de.warp.routing.infrastructure.routing.RouteNotFoundException;
import de.warp.routing.infrastructure.routing.RoutingServiceUnavailableException;
import de.warp.routing.profile.domain.ActivityProfileCode;
import de.warp.routing.route.domain.RouteCalculationStatus;
import de.warp.routing.route.persistence.RouteCalculationEntity;
import de.warp.routing.route.persistence.RouteCalculationRepository;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RouteCalculationProcessorTest {

    private static final Instant NOW =
            Instant.parse("2026-10-21T18:00:00Z");

    @Mock
    private RouteCalculationRepository repository;

    @Mock
    private OpenRouteServiceClient openRouteServiceClient;

    private RouteCalculationProcessor processor;
    private RouteCalculationEntity calculation;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                NOW,
                ZoneOffset.UTC
        );

        processor = new RouteCalculationProcessor(
                repository,
                openRouteServiceClient,
                new ObjectMapper(),
                clock
        );

        calculation = new RouteCalculationEntity(
                "calc_123",
                "usr_123",
                "test-idempotency-key",
                "Mosbach",
                49.3536,
                9.1511,
                "Bad Mergentheim",
                49.4924,
                9.7736,
                Instant.parse("2026-10-22T06:00:00Z"),
                ActivityProfileCode.BICYCLE,
                "cycling-regular",
                Instant.parse("2026-10-21T17:59:00Z")
        );

        when(repository.findById("calc_123"))
                .thenReturn(Optional.of(calculation));
    }

    @Test
    void completesRouteCalculation() {
        OpenRouteServiceResponse.Feature route =
                createRoute();

        when(
                openRouteServiceClient.calculateRoute(
                        "cycling-regular",
                        49.3536,
                        9.1511,
                        49.4924,
                        9.7736
                )
        ).thenReturn(route);

        processor.process("calc_123");

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.COMPLETED);

        assertThat(calculation.getDistanceMeters())
                .isEqualTo(57600);

        assertThat(calculation.getDurationSeconds())
                .isEqualTo(4320);

        assertThat(calculation.getArrivalTime())
                .isEqualTo(
                        Instant.parse("2026-10-22T07:12:00Z")
                );

        assertThat(calculation.getGeometry())
                .contains("\"type\":\"LineString\"")
                .contains("9.1511")
                .contains("49.3536");

        assertThat(calculation.getSegments())
                .contains("\"distance\":57600.0")
                .contains("\"duration\":4320.0");

        assertThat(calculation.getErrorCode())
                .isNull();

        verify(repository, times(2))
                .saveAndFlush(calculation);
    }

    @Test
    void storesRouteNotFoundFailure() {
        when(
                openRouteServiceClient.calculateRoute(
                        "cycling-regular",
                        49.3536,
                        9.1511,
                        49.4924,
                        9.7736
                )
        ).thenThrow(
                new RouteNotFoundException(
                        "Keine Route gefunden."
                )
        );

        processor.process("calc_123");

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.FAILED);

        assertThat(calculation.getErrorCode())
                .isEqualTo("ROUTE_NOT_FOUND");

        assertThat(calculation.getErrorMessage())
                .contains("keine Route gefunden");

        assertThat(calculation.getGeometry())
                .isNull();

        verify(repository, times(2))
                .saveAndFlush(calculation);
    }

    @Test
    void storesUnavailableFailure() {
        when(
                openRouteServiceClient.calculateRoute(
                        "cycling-regular",
                        49.3536,
                        9.1511,
                        49.4924,
                        9.7736
                )
        ).thenThrow(
                new RoutingServiceUnavailableException(
                        "Timeout"
                )
        );

        processor.process("calc_123");

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.FAILED);

        assertThat(calculation.getErrorCode())
                .isEqualTo(
                        "ROUTING_SERVICE_UNAVAILABLE"
                );

        assertThat(calculation.getErrorMessage())
                .contains("vorübergehend nicht verfügbar");

        verify(repository, times(2))
                .saveAndFlush(calculation);
    }

    @Test
    void storesUnavailableFailureWhenSegmentsMismatchSummary() {
        OpenRouteServiceResponse.Geometry geometry =
                new OpenRouteServiceResponse.Geometry(
                        "LineString",
                        List.of(
                                List.of(9.1511, 49.3536),
                                List.of(9.7736, 49.4924)
                        )
                );

        OpenRouteServiceResponse.Summary summary =
                new OpenRouteServiceResponse.Summary(
                        57600,
                        4320
                );

        // Segment distance 1000 != summary 57600
        OpenRouteServiceResponse.Segment segment =
                new OpenRouteServiceResponse.Segment(
                        1000,
                        4320
                );

        OpenRouteServiceResponse.Feature route =
                new OpenRouteServiceResponse.Feature(
                        "Feature",
                        geometry,
                        new OpenRouteServiceResponse.Properties(
                                summary,
                                List.of(segment)
                        )
                );

        when(
                openRouteServiceClient.calculateRoute(
                        "cycling-regular",
                        49.3536,
                        9.1511,
                        49.4924,
                        9.7736
                )
        ).thenReturn(route);

        processor.process("calc_123");

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.FAILED);

        assertThat(calculation.getErrorCode())
                .isEqualTo("ROUTING_SERVICE_UNAVAILABLE");

        verify(repository, times(2))
                .saveAndFlush(calculation);
    }

    @Test
    void storesUnavailableFailureWhenSegmentHasNegativeValues() {
        OpenRouteServiceResponse.Geometry geometry =
                new OpenRouteServiceResponse.Geometry(
                        "LineString",
                        List.of(
                                List.of(9.1511, 49.3536),
                                List.of(9.7736, 49.4924)
                        )
                );

        OpenRouteServiceResponse.Summary summary =
                new OpenRouteServiceResponse.Summary(
                        57600,
                        4320
                );

        OpenRouteServiceResponse.Segment segment =
                new OpenRouteServiceResponse.Segment(
                        -10,
                        4320
                );

        OpenRouteServiceResponse.Feature route =
                new OpenRouteServiceResponse.Feature(
                        "Feature",
                        geometry,
                        new OpenRouteServiceResponse.Properties(
                                summary,
                                List.of(segment)
                        )
                );

        when(
                openRouteServiceClient.calculateRoute(
                        "cycling-regular",
                        49.3536,
                        9.1511,
                        49.4924,
                        9.7736
                )
        ).thenReturn(route);

        processor.process("calc_123");

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.FAILED);

        assertThat(calculation.getErrorCode())
                .isEqualTo("ROUTING_SERVICE_UNAVAILABLE");

        verify(repository, times(2))
                .saveAndFlush(calculation);
    }

    private OpenRouteServiceResponse.Feature createRoute() {
        OpenRouteServiceResponse.Geometry geometry =
                new OpenRouteServiceResponse.Geometry(
                        "LineString",
                        List.of(
                                List.of(9.1511, 49.3536),
                                List.of(9.7736, 49.4924)
                        )
                );

        OpenRouteServiceResponse.Summary summary =
                new OpenRouteServiceResponse.Summary(
                        57600,
                        4320
                );

        OpenRouteServiceResponse.Segment segment =
                new OpenRouteServiceResponse.Segment(
                        57600,
                        4320
                );

        OpenRouteServiceResponse.Properties properties =
                new OpenRouteServiceResponse.Properties(
                        summary,
                        List.of(segment)
                );

        return new OpenRouteServiceResponse.Feature(
                "Feature",
                geometry,
                properties
        );
    }
}