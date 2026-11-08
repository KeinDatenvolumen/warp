// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.hazard;

import de.warp.weather.api.dto.CriticalSectionHazardDto;
import de.warp.weather.api.dto.HiddenParameterInsightDto;
import de.warp.weather.domain.model.EvaluationStatus;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherForecastSeries;
import de.warp.weather.domain.model.WeatherForecastTimeStep;
import de.warp.weather.domain.model.WeatherModel;
import de.warp.weather.domain.model.WeatherSegment;
import de.warp.weather.domain.model.WeatherValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherHazardDetectorTest {

    private WeatherHazardDetector hazardDetector;

    @BeforeEach
    void setUp() {
        hazardDetector = new WeatherHazardDetector();
    }

    @Test
    void computesWetRoadIndexFromSeries() {
        WeatherValues values = new WeatherValues(
                18.0, 18.0, 60.0, 10.0, 14.0, 0.0, 2.0, 2.0, 0.0, 0.0, 0.0,
                10.0, 180.0, 15.0, 1013.0, 1013.0, 10.0, 0.0, 10.0, 0.0, 25000.0,
                3.0, 3.0, 300.0, 200.0, 100.0, 900.0, true, 0.0, 0.0, 0.0, 2500.0,
                0.5, 0.2, 18.0, 61
        );
        Instant now = Instant.parse("2026-05-16T08:00:00Z");
        WeatherForecastSeries series = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 49.0, 9.0, 100.0,
                List.of(new WeatherForecastTimeStep(now, values))
        );

        double wetRoad = hazardDetector.computeWetRoadIndex(series, now, values);
        assertThat(wetRoad).isGreaterThan(0.0);

        // Null series
        assertThat(hazardDetector.computeWetRoadIndex(null, now, values)).isEqualTo(0.0);

        // Hourly steps series
        WeatherForecastSeries hourlySeries = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 49.0, 9.0, 100.0,
                List.of(new WeatherForecastTimeStep(now, values)),
                List.of()
        );
        double wetHourly = hazardDetector.computeWetRoadIndex(hourlySeries, now, values);
        assertThat(wetHourly).isGreaterThan(0.0);
    }

    @Test
    void derivesThunderstormProbability() {
        assertThat(hazardDetector.deriveThunderstormProbability(99, null, null, 0.0)).isEqualTo(100.0);
        assertThat(hazardDetector.deriveThunderstormProbability(96, null, null, 0.0)).isEqualTo(90.0);
        assertThat(hazardDetector.deriveThunderstormProbability(95, null, null, 50.0)).isEqualTo(80.0);
        assertThat(hazardDetector.deriveThunderstormProbability(82, null, null, 0.0)).isEqualTo(55.0);
        assertThat(hazardDetector.deriveThunderstormProbability(1, 2100.0, 0.0, 0.0)).isEqualTo(85.0);
        assertThat(hazardDetector.deriveThunderstormProbability(1, 700.0, 0.0, 40.0)).isGreaterThanOrEqualTo(50.0);
        assertThat(hazardDetector.deriveThunderstormProbability(1, 700.0, 0.0, 20.0)).isLessThan(50.0);
        assertThat(hazardDetector.deriveThunderstormProbability(1, 300.0, 0.0, 0.0)).isBetween(5.0, 30.0);
        assertThat(hazardDetector.deriveThunderstormProbability(1, 300.0, -5.0, 0.0)).isGreaterThan(15.0);
    }

    @Test
    void buildsDeterministicInsightsAndCriticalSections() {
        Instant now = Instant.parse("2026-05-16T08:00:00Z");
        WeatherCondition cond = new WeatherCondition(
                35.0, 180.0, 50.0, 90.0, 5.0, 0.7, 0.0, 80.0,
                3.0, 1.0, 90.0, 2.0, 100.0, 0.5, 2000.0, 700.0, 1.0, 95
        );
        WeatherSegment seg = new WeatherSegment(
                0, now, 45, "#FF0000", 0, cond, List.of(), "ICE_CAP", "Glätte"
        );
        WeatherEvaluation evaluation = new WeatherEvaluation(
                "wev-1", "calc-1", "user-1", EvaluationStatus.COMPLETED,
                now, 45, "1.0", false, now, now.plusSeconds(3600), now, List.of(seg)
        );

        List<HiddenParameterInsightDto> insights =
                hazardDetector.buildDeterministicHiddenInsights(evaluation, "Fahrrad");
        assertThat(insights).hasSize(5);

        List<CriticalSectionHazardDto> criticalSections =
                hazardDetector.buildDeterministicCriticalSections(evaluation, 10000, "Fahrrad");
        assertThat(criticalSections).hasSize(1);
        assertThat(criticalSections.getFirst().severity()).isEqualTo("DANGER");
    }

    @Test
    void testsAllLimitingFactorsAndInsightStatusBranches() {
        Instant now = Instant.parse("2026-05-16T08:00:00Z");

        // Segment with THUNDER_CAP
        WeatherSegment segThunder = new WeatherSegment(
                0, now, 40, "#FF0000", 0,
                new WeatherCondition(20.0, 180.0, 30.0, 80.0, 2.0, 0.26, 0.0, 80.0, 18.0, 18.0, 75.0, 15.0, 200.0, 15.0, 8000.0, 260.0, 3.0, 95),
                List.of(), "THUNDER_CAP", "Gewitterkappung"
        );

        // Segment with WIND
        WeatherSegment segWind = new WeatherSegment(
                1, now.plusSeconds(900), 65, "#FFAA00", 5000,
                new WeatherCondition(35.0, 180.0, 48.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 12.0, 60.0, 10.0, 300.0, 3.8, 15000.0, 100.0, 2.0, 1),
                List.of(), "WIND", "Windböen"
        );

        // Segment with PRECIP
        WeatherSegment segPrecip = new WeatherSegment(
                2, now.plusSeconds(1800), 74, "#FFFF00", 10000,
                new WeatherCondition(15.0, 180.0, 18.0, 50.0, 1.5, 0.50, 0.0, 10.0, 16.0, 16.0, 85.0, 14.0, 150.0, 12.0, 12000.0, 50.0, 1.0, 61),
                List.of(), "PRECIP", "Niederschlag"
        );

        // Segment with default limiting factor
        WeatherSegment segDefault = new WeatherSegment(
                3, now.plusSeconds(2700), 78, "#FFFF00", 15000,
                new WeatherCondition(10.0, 180.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 20.0, 50.0, 8.0, 400.0, 18.0, 30000.0, 20.0, 3.0, 1),
                List.of(), null, null
        );

        WeatherEvaluation evaluation = new WeatherEvaluation(
                "wev-all", "calc-all", "user-1", EvaluationStatus.COMPLETED,
                now, 60, "1.0", false, now, now.plusSeconds(3600), now,
                List.of(segThunder, segWind, segPrecip, segDefault)
        );

        List<HiddenParameterInsightDto> insights =
                hazardDetector.buildDeterministicHiddenInsights(evaluation, "Motorrad");
        assertThat(insights).hasSize(5);

        List<CriticalSectionHazardDto> criticalSections =
                hazardDetector.buildDeterministicCriticalSections(evaluation, 20000, "Motorrad");
        assertThat(criticalSections).hasSize(4);
        assertThat(criticalSections.get(0).recommendation()).contains("Gewitter-Kappung");
        assertThat(criticalSections.get(1).recommendation()).contains("Seitenwind");
        assertThat(criticalSections.get(2).recommendation()).contains("Rutschgefahr");
        assertThat(criticalSections.get(3).recommendation()).contains("Gleichmäßiges Tempo");

        // Empty segments edge case
        WeatherEvaluation emptyEval = new WeatherEvaluation(
                "wev-empty", "calc-empty", "user-1", EvaluationStatus.COMPLETED,
                now, 100, "1.0", false, now, now.plusSeconds(3600), now, List.of()
        );
        assertThat(hazardDetector.buildDeterministicHiddenInsights(emptyEval, "Fahrrad")).hasSize(5);
        assertThat(hazardDetector.buildDeterministicCriticalSections(emptyEval, 0, "Fahrrad")).isEmpty();
    }

    @Test
    void testComputeWetRoadIndexEdgeCases() {
        WeatherValues nullishValues = new WeatherValues(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        // null series
        assertThat(hazardDetector.computeWetRoadIndex(null, Instant.now(), nullishValues)).isEqualTo(0.0);

        // empty series
        WeatherForecastSeries emptySeries = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 52.5, 13.4, 100.0, List.of(), List.of()
        );
        assertThat(hazardDetector.computeWetRoadIndex(emptySeries, Instant.now(), nullishValues)).isEqualTo(0.0);

        // series with minutely15 steps
        Instant now = Instant.parse("2026-05-16T10:00:00Z");
        WeatherForecastSeries minutelySeries = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 52.5, 13.4, 100.0,
                List.of(new WeatherForecastTimeStep(now, nullishValues)),
                List.of()
        );
        assertThat(hazardDetector.computeWetRoadIndex(minutelySeries, now, nullishValues)).isEqualTo(0.0);
    }

    @Test
    void testDeriveThunderstormProbabilityExtendedBranches() {
        // weatherCode 82
        assertThat(hazardDetector.deriveThunderstormProbability(82, 0.0, 0.0, 20.0)).isEqualTo(55.0);

        // null weatherCode
        assertThat(hazardDetector.deriveThunderstormProbability(null, 0.0, 0.0, 0.0)).isEqualTo(0.0);

        // CAPE >= 2000
        assertThat(hazardDetector.deriveThunderstormProbability(0, 2500.0, null, 10.0)).isEqualTo(85.0);

        // CAPE >= 600 with precip <= 30
        assertThat(hazardDetector.deriveThunderstormProbability(0, 1000.0, null, 10.0)).isGreaterThan(35.0);

        // CAPE < 600 but > 100
        assertThat(hazardDetector.deriveThunderstormProbability(0, 400.0, null, 0.0)).isGreaterThan(0.0);

        // LiftedIndex <= -4.0 boost
        assertThat(hazardDetector.deriveThunderstormProbability(0, 400.0, -5.0, 0.0)).isGreaterThan(15.0);
    }

    @Test
    void testAdditionalLimitingFactorsAndInsightStatusBranches() {
        Instant now = Instant.parse("2026-05-16T08:00:00Z");

        WeatherSegment segIce = new WeatherSegment(
                0, now, 25, "#FF0000", 0,
                new WeatherCondition(10.0, 180.0, 15.0, 50.0, 1.0, 0.8, 0.0, 0.0, 0.5, -2.0, 95.0, 0.0, 50.0, -1.0, 2000.0, 0.0, 0.0, 71),
                List.of(), "ICE_CAP", "Eisglätte"
        );
        WeatherSegment segStorm = new WeatherSegment(
                1, now.plusSeconds(900), 45, "#FFAA00", 5000,
                new WeatherCondition(45.0, 180.0, 65.0, 0.0, 0.0, 0.0, 0.0, 0.0, 32.0, 35.0, 40.0, 15.0, 600.0, 32.0, 10000.0, 700.0, 6.0, 1),
                List.of(), "STORM_CAP", "Sturmböen"
        );
        WeatherSegment segWet = new WeatherSegment(
                2, now.plusSeconds(1800), 60, "#FFFF00", 10000,
                new WeatherCondition(12.0, 180.0, 18.0, 60.0, 0.5, 0.9, 0.0, 0.0, 15.0, 15.0, 80.0, 12.0, 200.0, 15.0, 2500.0, 150.0, 2.0, 51),
                List.of(), "WET_ROAD", "Nasse Fahrbahn"
        );
        WeatherCondition nullishCond = new WeatherCondition(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        WeatherSegment segNullCondition = new WeatherSegment(
                3, now.plusSeconds(2700), 70, "#FFAA00", 15000,
                nullishCond, List.of(), null, ""
        );

        WeatherEvaluation evaluation = new WeatherEvaluation(
                "wev-ext", "calc-ext", "user-1", EvaluationStatus.COMPLETED,
                now, 40, "1.0", false, now, now.plusSeconds(3600), now,
                List.of(segIce, segStorm, segWet, segNullCondition)
        );

        List<CriticalSectionHazardDto> criticalSections =
                hazardDetector.buildDeterministicCriticalSections(evaluation, 15000, "Motorrad");
        assertThat(criticalSections).hasSize(4);
        assertThat(criticalSections.get(0).recommendation()).contains("Glättegefahr");
        assertThat(criticalSections.get(0).severity()).isEqualTo("DANGER");
        assertThat(criticalSections.get(1).recommendation()).contains("Seitenwind");
        assertThat(criticalSections.get(2).recommendation()).contains("Rutschgefahr");

        List<HiddenParameterInsightDto> insights =
                hazardDetector.buildDeterministicHiddenInsights(evaluation, "Motorrad");
        assertThat(insights).isNotEmpty();
    }
}
