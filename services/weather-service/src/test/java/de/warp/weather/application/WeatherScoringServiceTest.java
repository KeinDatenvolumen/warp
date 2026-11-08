// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application;

import de.warp.weather.application.WeatherScoringService.ScoringActivityProfile;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherSegment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherScoringServiceTest {

    private final WeatherScoringService scoringService = new WeatherScoringService(
            new WeatherScoringProperties("2.0", 0.35, 0.35, 0.30, 50.0, 50)
    );

    @Test
    void scoresCalmDryIdealWeatherAt100AcrossAllProfiles() {
        WeatherCondition ideal = new WeatherCondition(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                20.0,
                0
        );

        int bikeScore = scoringService.calculateSegmentScore(
                ideal, 90.0, ScoringActivityProfile.BICYCLE, 20.0
        );
        int motoScore = scoringService.calculateSegmentScore(
                ideal, 90.0, ScoringActivityProfile.MOTORCYCLE, 60.0
        );
        int hikeScore = scoringService.calculateSegmentScore(
                ideal, 90.0, ScoringActivityProfile.HIKING, 5.0
        );

        assertThat(bikeScore).isEqualTo(100);
        assertThat(motoScore).isEqualTo(100);
        assertThat(hikeScore).isEqualTo(100);
        assertThat(scoringService.resolveScoreColor(bikeScore)).isEqualTo("#00B4D8");
        assertThat(scoringService.resolveScoreColor(85)).isEqualTo("#52B788");
        assertThat(scoringService.resolveScoreColor(50)).isEqualTo("#F26419");
        assertThat(scoringService.resolveScoreColor(0)).isEqualTo("#2A0845");
    }

    @Test
    void penalizesHeadwindAndCrosswindMoreThanTailwindForBicycleAndAppliesTailwindBoost() {
        // Wind blowing FROM North (0 degrees) at 24 km/h
        WeatherCondition northWind = new WeatherCondition(
                24.0,
                0.0,
                0.0,
                0.0,
                0.0,
                19.0,
                1
        );

        // Rider moving North (0 degrees) -> direct headwind (deltaTheta = 0°)
        WeatherScoringService.SegmentScoreBreakdown headwind = scoringService.evaluateSegmentBreakdown(
                northWind, 0.0, ScoringActivityProfile.BICYCLE, 20.0
        );

        // Rider moving East (90 degrees) -> direct crosswind (deltaTheta = 90°)
        WeatherScoringService.SegmentScoreBreakdown crosswind = scoringService.evaluateSegmentBreakdown(
                northWind, 90.0, ScoringActivityProfile.BICYCLE, 20.0
        );

        // Rider moving South (180 degrees) -> direct tailwind (deltaTheta = 180°)
        WeatherScoringService.SegmentScoreBreakdown tailwind = scoringService.evaluateSegmentBreakdown(
                northWind, 180.0, ScoringActivityProfile.BICYCLE, 20.0
        );

        // At 24 km/h (= v_crit,head for BICYCLE), S_wind for pure headwind is exactly 50.0
        assertThat(headwind.windSubScore()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.1));
        assertThat(tailwind.tailwindBoost()).isGreaterThan(3.0);
        assertThat(headwind.score()).isLessThan(crosswind.score());
        assertThat(crosswind.score()).isLessThan(tailwind.score());
    }

    @Test
    void penalizesWetRoadAndRainMoreSeverelyForMotorcycleThanHiking() {
        // Wet road (W_road = 1.0) after rain with 2.0 mm/h current rain intensity and 80% probability
        WeatherCondition wetRainyRoad = new WeatherCondition(
                10.0,
                180.0,
                15.0,
                80.0,
                2.0,
                1.0,
                0.0,
                0.0,
                18.0,
                18.0,
                85.0,
                15.0,
                50.0,
                16.0,
                10_000.0,
                0.0,
                1.0,
                61
        );

        WeatherScoringService.SegmentScoreBreakdown moto = scoringService.evaluateSegmentBreakdown(
                wetRainyRoad, 180.0, ScoringActivityProfile.MOTORCYCLE, 60.0
        );
        WeatherScoringService.SegmentScoreBreakdown hike = scoringService.evaluateSegmentBreakdown(
                wetRainyRoad, 180.0, ScoringActivityProfile.HIKING, 5.0
        );

        assertThat(moto.precipSubScore()).isLessThan(hike.precipSubScore());
        assertThat(moto.score()).isLessThan(hike.score());
    }

    @Test
    void appliesHardCapsForThunderstormIceAndStorm() {
        // 1. Severe hail thunderstorm (WMO = 99) -> capped at 5
        WeatherCondition hailStorm = new WeatherCondition(
                20.0, 180.0, 80.0, 3.0, 95.0, 20.0, 99
        );
        assertThat(scoringService.calculateSegmentScore(
                hailStorm, 0.0, ScoringActivityProfile.MOTORCYCLE, 60.0
        )).isEqualTo(5);

        // 2. Latent CAPE thunderstorm risk [FR-WETT-03]: CAPE = 700 J/kg & precipProb = 40% -> capped <= 50
        WeatherCondition latentThunder = new WeatherCondition(
                5.0, 180.0, 8.0, 40.0, 0.0, 0.0, 0.0, 50.0,
                21.0, 21.0, 65.0, 14.0, 300.0, 19.0, 20_000.0, 700.0, 3.0, 2
        );
        int latentScore = scoringService.calculateSegmentScore(
                latentThunder, 180.0, ScoringActivityProfile.BICYCLE, 20.0
        );
        assertThat(latentScore).isLessThanOrEqualTo(50);
        assertThat(scoringService.isElevatedThunderstormRisk(latentThunder)).isTrue();

        // 3. Black ice risk (W_road = 0.5, T_soil = 0.0°C, T_2m = 1.5°C) -> 0 for MOTORCYCLE
        WeatherCondition blackIce = new WeatherCondition(
                5.0, 180.0, 5.0, 10.0, 0.0, 0.5, 0.0, 0.0,
                1.5, 1.5, 80.0, -1.0, 100.0, 0.0, 15_000.0, 0.0, 1.0, 2
        );
        assertThat(scoringService.calculateSegmentScore(
                blackIce, 180.0, ScoringActivityProfile.MOTORCYCLE, 60.0
        )).isEqualTo(0);
    }

    @Test
    void matchesReferenceScenariosForThreeStageRiskSensitiveRouteAggregation() {
        Instant base = Instant.parse("2026-10-27T10:00:00Z");
        WeatherCondition calm = new WeatherCondition(0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 0);
        WeatherCondition thunder = new WeatherCondition(15.0, 180.0, 50.0, 1.0, 65.0, 19.0, 95);

        // Scenario 1: Traumwetter (16 x 95) -> 95
        List<WeatherSegment> dreamWeather = buildSegments(base, calm, 16, 95, 0, 0, calm);
        assertThat(scoringService.calculateOverallScore(
                dreamWeather, ScoringActivityProfile.MOTORCYCLE, null
        ).overallScore()).isEqualTo(95);

        // Scenario 2: Kurzer Schauer (14 x 90, 2 x 60) -> 82
        List<WeatherSegment> shortShower = buildSegments(base, calm, 14, 90, 2, 60, calm);
        assertThat(scoringService.calculateOverallScore(
                shortShower, ScoringActivityProfile.MOTORCYCLE, null
        ).overallScore()).isEqualTo(82);

        // Scenario 3: Gewitter-Warnung [FR-WETT-03] (14 x 90, 2 x 48 with thunder risk) -> 50
        List<WeatherSegment> thunderWarning = buildSegments(base, calm, 14, 90, 2, 48, thunder);
        WeatherScoringService.OverallScoreResult thunderResult = scoringService.calculateOverallScore(
                thunderWarning, ScoringActivityProfile.MOTORCYCLE, null
        );
        assertThat(thunderResult.overallScore()).isEqualTo(50);
        assertThat(thunderResult.thunderstormCapApplied()).isTrue();

        // Scenario 4: Alpenpass-Unwetter (14 x 95, 2 x 10) -> 24
        List<WeatherSegment> alpineStorm = buildSegments(base, calm, 14, 95, 2, 10, calm);
        assertThat(scoringService.calculateOverallScore(
                alpineStorm, ScoringActivityProfile.MOTORCYCLE, null
        ).overallScore()).isEqualTo(24);

        // Scenario 5: Lokales Glatteis (15 x 85, 1 x 0) -> 0
        List<WeatherSegment> localBlackIce = buildSegments(base, calm, 15, 85, 1, 0, calm);
        assertThat(scoringService.calculateOverallScore(
                localBlackIce, ScoringActivityProfile.MOTORCYCLE, null
        ).overallScore()).isEqualTo(0);
    }

    @Test
    void calculatesFiftySegmentRouteScoringWithinFiftyMilliseconds() {
        Instant base = Instant.parse("2026-10-27T10:00:00Z");
        WeatherCondition condition = new WeatherCondition(
                18.0, 210.0, 28.0, 35.0, 0.4, 0.3, 0.0, 15.0,
                16.0, 15.0, 70.0, 10.5, 250.0, 14.0, 18_000.0, 150.0, 3.0, 3
        );

        long startNanos = System.nanoTime();
        List<WeatherSegment> segments = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            int segScore = scoringService.calculateSegmentScore(
                    condition, (i * 15.0) % 360.0, ScoringActivityProfile.BICYCLE, 18.0
            );
            String color = scoringService.resolveScoreColor(segScore);
            segments.add(new WeatherSegment(
                    i,
                    base.plusSeconds(i * 600L),
                    segScore,
                    color,
                    i * 5000,
                    condition,
                    List.of()
            ));
        }
        WeatherScoringService.OverallScoreResult overall = scoringService.calculateOverallScore(
                segments, ScoringActivityProfile.BICYCLE, null
        );
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        assertThat(overall.overallScore()).isBetween(0, 100);
        assertThat(elapsedMillis).isLessThan(50L);
    }

    private static List<WeatherSegment> buildSegments(
            Instant base,
            WeatherCondition primaryCondition,
            int primaryCount,
            int primaryScore,
            int secondaryCount,
            int secondaryScore,
            WeatherCondition secondaryCondition
    ) {
        List<WeatherSegment> list = new ArrayList<>(primaryCount + secondaryCount);
        int idx = 0;
        for (int i = 0; i < primaryCount; i++) {
            list.add(new WeatherSegment(
                    idx,
                    base.plusSeconds(idx * 900L),
                    primaryScore,
                    "#2F9E44",
                    idx * 5000,
                    primaryCondition,
                    List.of()
            ));
            idx++;
        }
        for (int i = 0; i < secondaryCount; i++) {
            list.add(new WeatherSegment(
                    idx,
                    base.plusSeconds(idx * 900L),
                    secondaryScore,
                    "#E03131",
                    idx * 5000,
                    secondaryCondition,
                    List.of()
            ));
            idx++;
        }
        return list;
    }

    @Test
    void testCalculateIceAndSnowCapExhaustive() {
        // freezing rain wmo codes
        for (int wmo : List.of(56, 57, 66, 67)) {
            assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.MOTORCYCLE, wmo, 0.0, 5.0, 5.0, 0.0, 10.0)).isEqualTo(0.0);
            assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.BICYCLE, wmo, 0.0, 5.0, 5.0, 0.0, 10.0)).isEqualTo(5.0);
            assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.HIKING, wmo, 0.0, 5.0, 5.0, 0.0, 10.0)).isEqualTo(20.0);
        }

        // black ice risk
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.MOTORCYCLE, 0, 0.30, 0.0, 5.0, 0.0, 10.0)).isEqualTo(0.0);
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.BICYCLE, 0, 0.30, 5.0, -1.0, 0.0, 10.0)).isEqualTo(5.0);

        // snow code 71 (light snow)
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.MOTORCYCLE, 71, 0.0, 5.0, 5.0, 0.0, 10.0)).isEqualTo(5.0);
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.BICYCLE, 71, 0.0, 5.0, 5.0, 0.0, 10.0)).isEqualTo(15.0);
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.HIKING, 71, 0.0, 5.0, 5.0, 0.0, 20.0)).isEqualTo(65.0);
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.HIKING, 71, 0.0, 5.0, 5.0, 0.0, 40.0)).isEqualTo(35.0);

        // snow codes 75 and 86 (heavy snow)
        for (int wmo : List.of(75, 86)) {
            assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.HIKING, wmo, 0.0, 5.0, 5.0, 0.0, 20.0)).isEqualTo(15.0);
            assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.BICYCLE, wmo, 0.0, 5.0, 5.0, 0.0, 20.0)).isEqualTo(5.0);
        }

        // other snow code (e.g. 73)
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.HIKING, 73, 0.0, 5.0, 5.0, 0.0, 20.0)).isEqualTo(35.0);

        // pSnow > 0 with non-snow wmo
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.MOTORCYCLE, 0, 0.0, 5.0, 5.0, 1.0, 10.0)).isEqualTo(5.0);

        // no ice or snow
        assertThat(WeatherScoringService.calculateIceAndSnowCap(ScoringActivityProfile.MOTORCYCLE, 0, 0.0, 15.0, 15.0, 0.0, 10.0)).isEqualTo(100.0);
    }

    @Test
    void testCalculateThunderstormCapExhaustive() {
        // severe thunderstorm 96/99
        assertThat(scoringService.calculateThunderstormCap(ScoringActivityProfile.MOTORCYCLE, 96, 0.0, 0.0, null)).isEqualTo(5.0);
        assertThat(scoringService.calculateThunderstormCap(ScoringActivityProfile.BICYCLE, 99, 0.0, 0.0, null)).isEqualTo(5.0);

        // moderate thunderstorm 95
        assertThat(scoringService.calculateThunderstormCap(ScoringActivityProfile.HIKING, 95, 0.0, 0.0, null)).isEqualTo(10.0);
        assertThat(scoringService.calculateThunderstormCap(ScoringActivityProfile.MOTORCYCLE, 95, 0.0, 0.0, null)).isEqualTo(15.0);

        // latent thunder risk: wmo 82
        assertThat(scoringService.calculateThunderstormCap(ScoringActivityProfile.BICYCLE, 82, 0.0, 0.0, null)).isEqualTo(50.0);

        // latent thunder risk: CAPE >= 600 and pPrecip > 0.30
        assertThat(scoringService.calculateThunderstormCap(ScoringActivityProfile.BICYCLE, 0, 1200.0, 0.50, null)).isLessThan(50.0);

        // latent thunder risk: thunderstormProbabilityPercent >= threshold
        assertThat(scoringService.calculateThunderstormCap(ScoringActivityProfile.BICYCLE, 0, 0.0, 0.0, 60.0)).isEqualTo(50.0);

        // no thunder risk
        assertThat(scoringService.calculateThunderstormCap(ScoringActivityProfile.BICYCLE, 0, 0.0, 0.0, null)).isEqualTo(100.0);
    }

    @Test
    void testDryingTimeConstantEdgeCases() {
        // extreme low temperature and radiation
        double tauCold = WeatherScoringService.calculateDryingTimeConstantHours(-5.0, 0.0, 95.0);
        assertThat(tauCold).isGreaterThan(4.0);

        // high temperature and high radiation
        double tauHot = WeatherScoringService.calculateDryingTimeConstantHours(35.0, 800.0, 30.0);
        assertThat(tauHot).isLessThan(1.0);

        // fallback wet road index
        assertThat(WeatherScoringService.estimateFallbackWetRoadIndex(0.0, 20.0, 200.0, 50.0)).isEqualTo(0.0);
        assertThat(WeatherScoringService.estimateFallbackWetRoadIndex(5.0, 20.0, 200.0, 50.0)).isGreaterThan(0.0);
    }

    @Test
    void testCalculateOverallScoreWithVariousDurations() {
        WeatherCondition cond = new WeatherCondition(
                10.0, 180.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 20.0, 50.0, 8.0, 400.0, 18.0, 30000.0, 20.0, 3.0, 1
        );
        List<WeatherSegment> segs = List.of(
                new WeatherSegment(0, Instant.now(), 80, "#52B788", 0, cond, List.of()),
                new WeatherSegment(1, Instant.now().plusSeconds(900), 60, "#F26419", 5000, cond, List.of())
        );

        // segmentDurationsSeconds == null -> uniform weights
        WeatherScoringService.OverallScoreResult r1 = scoringService.calculateOverallScore(segs, "BICYCLE", null);
        assertThat(r1.overallScore()).isEqualTo(68);

        // segmentDurationsSeconds mismatch size -> uniform weights
        WeatherScoringService.OverallScoreResult r2 = scoringService.calculateOverallScore(segs, "BICYCLE", List.of(100.0));
        assertThat(r2.overallScore()).isEqualTo(68);

        // totalDuration == 0.0 -> uniform weights
        WeatherScoringService.OverallScoreResult r3 = scoringService.calculateOverallScore(segs, "BICYCLE", List.of(0.0, 0.0));
        assertThat(r3.overallScore()).isEqualTo(68);

        // normal durations
        WeatherScoringService.OverallScoreResult r4 = scoringService.calculateOverallScore(segs, "BICYCLE", List.of(1800.0, 600.0));
        assertThat(r4.overallScore()).isGreaterThan(68);
    }
}
