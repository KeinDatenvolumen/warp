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
}
