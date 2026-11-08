// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.domain.model;

import de.warp.weather.api.EvaluateRouteWeatherRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RouteSnapshot and ActivityProfileSnapshot Domain Tests")
class RouteSnapshotTest {

    @Nested
    @DisplayName("ActivityProfileSnapshot Tests")
    class ActivityProfileSnapshotTests {

        @Test
        @DisplayName("Rejects null or blank code")
        void rejectsNullOrBlankCode() {
            assertThatThrownBy(() -> new RouteSnapshot.ActivityProfileSnapshot(null, 10.0, 5))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("code must not be null");

            assertThatThrownBy(() -> new RouteSnapshot.ActivityProfileSnapshot("   ", 10.0, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must not be blank");
        }

        @Test
        @DisplayName("Uses custom speed and interval when positive")
        void preservesExplicitPositiveValues() {
            RouteSnapshot.ActivityProfileSnapshot snapshot =
                    new RouteSnapshot.ActivityProfileSnapshot("CUSTOM", 22.5, 8);

            assertThat(snapshot.code()).isEqualTo("CUSTOM");
            assertThat(snapshot.averageSpeedKmh()).isEqualTo(22.5);
            assertThat(snapshot.weatherQueryIntervalKm()).isEqualTo(8);
        }

        @ParameterizedTest(name = "Profile {0} defaults to speed={1} and interval={2}")
        @CsvSource({
                "HIKING, 5.0, 2",
                "hiking, 5.0, 2",
                "MOTORCYCLE, 60.0, 15",
                "motorcycle, 60.0, 15",
                "ROAD_CYCLING, 18.0, 5",
                "BICYCLE, 18.0, 5",
                "UNKNOWN_PROFILE, 18.0, 5"
        })
        void appliesDefaultsWhenZeroOrNegative(String code, double expectedSpeed, int expectedInterval) {
            RouteSnapshot.ActivityProfileSnapshot snapshot =
                    new RouteSnapshot.ActivityProfileSnapshot(code, 0.0, 0);

            assertThat(snapshot.averageSpeedKmh()).isEqualTo(expectedSpeed);
            assertThat(snapshot.weatherQueryIntervalKm()).isEqualTo(expectedInterval);

            // Also test negative values
            RouteSnapshot.ActivityProfileSnapshot negSnapshot =
                    new RouteSnapshot.ActivityProfileSnapshot(code, -1.0, -5);
            assertThat(negSnapshot.averageSpeedKmh()).isEqualTo(expectedSpeed);
            assertThat(negSnapshot.weatherQueryIntervalKm()).isEqualTo(expectedInterval);
        }
    }

    @Nested
    @DisplayName("RouteSnapshot Tests")
    class RouteSnapshotValidationTests {

        private final Instant now = Instant.parse("2026-10-22T08:00:00Z");
        private final RouteSnapshot.ActivityProfileSnapshot validProfile =
                new RouteSnapshot.ActivityProfileSnapshot("ROAD_CYCLING", 25.0, 5);

        @Test
        @DisplayName("Valid RouteSnapshot is created successfully")
        void createsValidSnapshot() {
            RouteSnapshot snapshot = new RouteSnapshot(
                    "calc_1",
                    now,
                    3600,
                    25000,
                    List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)),
                    validProfile
            );

            assertThat(snapshot.routeCalculationId()).isEqualTo("calc_1");
            assertThat(snapshot.departureTime()).isEqualTo(now);
            assertThat(snapshot.durationSeconds()).isEqualTo(3600);
            assertThat(snapshot.distanceMeters()).isEqualTo(25000);
            assertThat(snapshot.coordinates()).hasSize(2);
            assertThat(snapshot.activityProfile()).isEqualTo(validProfile);
        }

        @Test
        @DisplayName("Rejects null or blank routeCalculationId")
        void rejectsInvalidRouteCalculationId() {
            assertThatThrownBy(() -> new RouteSnapshot(null, now, 100, 100,
                    List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)), validProfile))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new RouteSnapshot("  ", now, 100, 100,
                    List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)), validProfile))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rejects null departureTime")
        void rejectsNullDepartureTime() {
            assertThatThrownBy(() -> new RouteSnapshot("calc_1", null, 100, 100,
                    List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)), validProfile))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Rejects negative durationSeconds or distanceMeters")
        void rejectsNegativeDurationOrDistance() {
            assertThatThrownBy(() -> new RouteSnapshot("calc_1", now, -1, 100,
                    List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)), validProfile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("durationSeconds must not be negative");

            assertThatThrownBy(() -> new RouteSnapshot("calc_1", now, 100, -1,
                    List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)), validProfile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("distanceMeters must not be negative");
        }

        @Test
        @DisplayName("Rejects null, empty or undersized coordinates")
        void rejectsInvalidCoordinates() {
            assertThatThrownBy(() -> new RouteSnapshot("calc_1", now, 100, 100, null, validProfile))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new RouteSnapshot("calc_1", now, 100, 100, List.of(), validProfile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2 points");

            assertThatThrownBy(() -> new RouteSnapshot("calc_1", now, 100, 100,
                    List.of(List.of(9.15, 49.35)), validProfile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2 points");

            // Point is null
            List<List<Double>> listWithNullPoint = java.util.Arrays.asList(List.of(9.15, 49.35), null);
            assertThatThrownBy(() -> new RouteSnapshot("calc_1", now, 100, 100,
                    listWithNullPoint, validProfile))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("coordinate point must not be null");

            // Point has fewer than 2 elements
            List<List<Double>> listWithOneDimPoint = List.of(List.of(9.15, 49.35), List.of(9.20));
            assertThatThrownBy(() -> new RouteSnapshot("calc_1", now, 100, 100,
                    listWithOneDimPoint, validProfile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[longitude, latitude]");
        }

        @Test
        @DisplayName("Rejects null activityProfile")
        void rejectsNullActivityProfile() {
            assertThatThrownBy(() -> new RouteSnapshot("calc_1", now, 100, 100,
                    List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("EvaluateRouteWeatherRequest DTO toDomain Tests")
    class EvaluateRouteWeatherRequestDtoTests {

        private final Instant now = Instant.parse("2026-10-22T08:00:00Z");

        @Test
        @DisplayName("toDomain maps valid request with explicit speed and interval correctly")
        void mapsValidRequestWithExplicitValuesToDomain() {
            EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                    "usr_1",
                    new EvaluateRouteWeatherRequest.RouteSnapshotRequest(
                            "calc_1",
                            now,
                            1800,
                            12000,
                            List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)),
                            new EvaluateRouteWeatherRequest.ActivityProfileSnapshotRequest(
                                    "HIKING",
                                    6.5,
                                    3
                            )
                    )
            );

            RouteSnapshot domain = request.toDomain();
            assertThat(domain.activityProfile().averageSpeedKmh()).isEqualTo(6.5);
            assertThat(domain.activityProfile().weatherQueryIntervalKm()).isEqualTo(3);
        }

        @Test
        @DisplayName("toDomain maps valid request correctly")
        void mapsValidRequestToDomain() {
            EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                    "usr_1",
                    new EvaluateRouteWeatherRequest.RouteSnapshotRequest(
                            "calc_1",
                            now,
                            1800,
                            12000,
                            List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)),
                            new EvaluateRouteWeatherRequest.ActivityProfileSnapshotRequest(
                                    "HIKING",
                                    null, // defaults to 0.0 -> will become 5.0
                                    null  // defaults to 0 -> will become 2
                            )
                    )
            );

            RouteSnapshot domain = request.toDomain();
            assertThat(domain.routeCalculationId()).isEqualTo("calc_1");
            assertThat(domain.activityProfile().code()).isEqualTo("HIKING");
            assertThat(domain.activityProfile().averageSpeedKmh()).isEqualTo(5.0);
            assertThat(domain.activityProfile().weatherQueryIntervalKm()).isEqualTo(2);
        }

        @Test
        @DisplayName("toDomain rejects null routeSnapshot or null activityProfile")
        void rejectsNullComponents() {
            EvaluateRouteWeatherRequest reqNullSnapshot = new EvaluateRouteWeatherRequest("usr_1", null);
            assertThatThrownBy(reqNullSnapshot::toDomain)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("routeSnapshot must not be null");

            EvaluateRouteWeatherRequest.RouteSnapshotRequest snapshotReqNullProfile =
                    new EvaluateRouteWeatherRequest.RouteSnapshotRequest(
                            "calc_1", now, 1800, 12000,
                            List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)),
                            null
                    );
            assertThatThrownBy(snapshotReqNullProfile::toDomain)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("activityProfile must not be null");
        }
    }
}
