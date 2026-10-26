package de.warp.weather.application;

import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherSegment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherScoringServiceTest {

    private final WeatherScoringService scoringService = new WeatherScoringService(
            new WeatherScoringProperties("1.0", 0.30, 0.45, 0.25, 50.0, 50)
    );

    @Test
    void scoresCalmDryWeatherNear100() {
        WeatherCondition ideal = new WeatherCondition(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                20.0,
                0
        );

        int score = scoringService.calculateSegmentScore(ideal, 90.0);

        assertThat(score).isEqualTo(100);
        assertThat(scoringService.resolveScoreColor(score)).isEqualTo("#2F9E44");
    }

    @Test
    void penalizesHeadwindMoreStronglyThanTailwindForSameWindSpeed() {
        // Wind blowing FROM North (0 degrees) at 30 km/h
        WeatherCondition northWind = new WeatherCondition(
                30.0,
                0.0,
                0.0,
                0.0,
                0.0,
                18.0,
                1
        );

        // Rider moving North (0 degrees) -> direct headwind
        int headwindScore = scoringService.calculateSegmentScore(northWind, 0.0);

        // Rider moving East (90 degrees) -> crosswind
        int crosswindScore = scoringService.calculateSegmentScore(northWind, 90.0);

        // Rider moving South (180 degrees) -> direct tailwind
        int tailwindScore = scoringService.calculateSegmentScore(northWind, 180.0);

        assertThat(headwindScore).isLessThan(crosswindScore);
        assertThat(crosswindScore).isLessThan(tailwindScore);
    }

    @Test
    void evaluatesPrecipitationProbabilityAndIntensity() {
        WeatherCondition lightRain = new WeatherCondition(
                5.0,
                180.0,
                30.0,
                0.5,
                0.0,
                15.0,
                61
        );

        WeatherCondition heavyRain = new WeatherCondition(
                5.0,
                180.0,
                95.0,
                5.0,
                0.0,
                15.0,
                65
        );

        int lightRainScore = scoringService.calculateSegmentScore(lightRain, 0.0);
        int heavyRainScore = scoringService.calculateSegmentScore(heavyRain, 0.0);

        assertThat(lightRainScore).isBetween(0, 100);
        assertThat(heavyRainScore).isBetween(0, 100);
        assertThat(heavyRainScore).isLessThan(lightRainScore);
    }

    @Test
    void calculatesOverallScoreAsArithmeticMeanWhenNoThunderstormRisk() {
        Instant now = Instant.parse("2026-10-27T10:00:00Z");
        WeatherCondition calm = new WeatherCondition(5.0, 90.0, 10.0, 0.0, 0.0, 18.0, 1);

        List<WeatherSegment> segments = List.of(
                new WeatherSegment(0, now, 90, "#2F9E44", 0, calm, List.of()),
                new WeatherSegment(1, now.plusSeconds(600), 80, "#2F9E44", 5000, calm, List.of()),
                new WeatherSegment(2, now.plusSeconds(1200), 70, "#82C91E", 10000, calm, List.of())
        );

        WeatherScoringService.OverallScoreResult result =
                scoringService.calculateOverallScore(segments);

        assertThat(result.overallScore()).isEqualTo(80);
        assertThat(result.thunderstormCapApplied()).isFalse();
        assertThat(result.scoreVersion()).isEqualTo("1.0");
    }

    @Test
    void capsOverallScoreAt50WhenThunderstormRiskExceedsThresholdAtAnySegment() {
        Instant now = Instant.parse("2026-10-27T10:00:00Z");
        WeatherCondition calm = new WeatherCondition(0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 0);
        WeatherCondition thunderstorm = new WeatherCondition(15.0, 180.0, 40.0, 1.0, 65.0, 19.0, 95);

        List<WeatherSegment> segments = List.of(
                new WeatherSegment(0, now, 100, "#2F9E44", 0, calm, List.of()),
                new WeatherSegment(1, now.plusSeconds(600), 100, "#2F9E44", 5000, calm, List.of()),
                new WeatherSegment(2, now.plusSeconds(1200), 50, "#F59F00", 10000, thunderstorm, List.of())
        );

        // Mean of (100, 100, 50) would be 83, but thunderstorm cap limits overallScore to 50
        WeatherScoringService.OverallScoreResult result =
                scoringService.calculateOverallScore(segments);

        assertThat(result.overallScore()).isEqualTo(50);
        assertThat(result.thunderstormCapApplied()).isTrue();
    }
}
