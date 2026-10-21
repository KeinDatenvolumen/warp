package de.warp.routing.route.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.warp.routing.profile.domain.ActivityProfileCode;
import de.warp.routing.route.domain.RouteCalculationStatus;

class RouteCalculationEntityTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-10-04T08:00:00Z");

    private RouteCalculationEntity calculation;

    @BeforeEach
    void setUp() {
        calculation = new RouteCalculationEntity(
                "calc_123",
                "usr_123",
                "Mosbach",
                49.3536,
                9.1511,
                "Bad Mergentheim",
                49.4924,
                9.7736,
                Instant.parse("2026-10-04T06:00:00Z"),
                ActivityProfileCode.BICYCLE,
                "cycling-regular",
                CREATED_AT
        );
    }

    @Test
    void createsPendingRouteCalculation() {
        assertThat(calculation.getId())
                .isEqualTo("calc_123");

        assertThat(calculation.getRequestedByUserId())
                .isEqualTo("usr_123");

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.PENDING);

        assertThat(calculation.getCreatedAt())
                .isEqualTo(CREATED_AT);

        assertThat(calculation.getUpdatedAt())
                .isEqualTo(CREATED_AT);

        assertThat(calculation.getDistanceMeters())
                .isNull();

        assertThat(calculation.getDurationSeconds())
                .isNull();

        assertThat(calculation.getErrorCode())
                .isNull();
    }

    @Test
    void transitionsFromPendingToProcessing() {
        Instant processingAt =
                Instant.parse("2026-10-04T08:00:01Z");

        calculation.markProcessing(processingAt);

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.PROCESSING);

        assertThat(calculation.getUpdatedAt())
                .isEqualTo(processingAt);
    }

    @Test
    void completesProcessingRouteCalculation() {
        calculation.markProcessing(
                Instant.parse("2026-10-04T08:00:01Z")
        );

        Instant completedAt =
                Instant.parse("2026-10-04T08:00:03Z");

        Instant arrivalTime =
                Instant.parse("2026-10-04T09:12:00Z");

        String geometry = """
                {
                  "type": "LineString",
                  "coordinates": [
                    [9.1511, 49.3536],
                    [9.7736, 49.4924]
                  ]
                }
                """;

        String segments = """
                [
                  {
                    "distance": 57600,
                    "duration": 4320
                  }
                ]
                """;

        calculation.complete(
                arrivalTime,
                57600,
                4320,
                geometry,
                segments,
                completedAt
        );

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.COMPLETED);

        assertThat(calculation.getArrivalTime())
                .isEqualTo(arrivalTime);

        assertThat(calculation.getDistanceMeters())
                .isEqualTo(57600);

        assertThat(calculation.getDurationSeconds())
                .isEqualTo(4320);

        assertThat(calculation.getGeometry())
                .isEqualTo(geometry);

        assertThat(calculation.getSegments())
                .isEqualTo(segments);

        assertThat(calculation.getErrorCode())
                .isNull();

        assertThat(calculation.getErrorMessage())
                .isNull();

        assertThat(calculation.getUpdatedAt())
                .isEqualTo(completedAt);
    }

    @Test
    void marksPendingRouteCalculationAsFailed() {
        Instant failedAt =
                Instant.parse("2026-10-04T08:00:02Z");

        calculation.fail(
                "ROUTE_NOT_FOUND",
                "Für Start, Ziel und Aktivitätsprofil "
                        + "konnte keine Route gefunden werden.",
                failedAt
        );

        assertThat(calculation.getStatus())
                .isEqualTo(RouteCalculationStatus.FAILED);

        assertThat(calculation.getErrorCode())
                .isEqualTo("ROUTE_NOT_FOUND");

        assertThat(calculation.getErrorMessage())
                .isEqualTo(
                        "Für Start, Ziel und Aktivitätsprofil "
                                + "konnte keine Route gefunden werden."
                );

        assertThat(calculation.getDistanceMeters())
                .isNull();

        assertThat(calculation.getGeometry())
                .isNull();

        assertThat(calculation.getUpdatedAt())
                .isEqualTo(failedAt);
    }

    @Test
    void rejectsCompletionWhilePending() {
        assertThatThrownBy(
                () -> calculation.complete(
                        Instant.parse("2026-10-04T09:12:00Z"),
                        57600,
                        4320,
                        "{}",
                        "[]",
                        Instant.parse("2026-10-04T08:00:03Z")
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "Ungültiger Statusübergang"
                );
    }

    @Test
    void rejectsProcessingCompletedCalculationAgain() {
        calculation.markProcessing(
                Instant.parse("2026-10-04T08:00:01Z")
        );

        calculation.complete(
                Instant.parse("2026-10-04T09:12:00Z"),
                57600,
                4320,
                "{}",
                "[]",
                Instant.parse("2026-10-04T08:00:03Z")
        );

        assertThatThrownBy(
                () -> calculation.markProcessing(
                        Instant.parse("2026-10-04T08:00:04Z")
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "Ungültiger Statusübergang"
                );
    }
}