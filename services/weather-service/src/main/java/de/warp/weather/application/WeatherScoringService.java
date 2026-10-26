package de.warp.weather.application;

import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class WeatherScoringService {

    private static final double MAX_WIND_SPEED_KMH = 50.0;
    private static final double MAX_PRECIPITATION_MM = 5.0;

    private final WeatherScoringProperties properties;

    @Autowired
    public WeatherScoringService(WeatherScoringProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public WeatherScoringService() {
        this(WeatherScoringProperties.defaults());
    }

    public int calculateSegmentScore(
            WeatherCondition condition,
            double movementBearingDegrees
    ) {
        Objects.requireNonNull(condition, "condition must not be null");

        double windSubScore = scoreWind(
                condition.windSpeedKmh(),
                condition.windDirectionDegrees(),
                movementBearingDegrees
        );

        double rainSubScore = scorePrecipitation(
                condition.precipitationProbabilityPercent(),
                condition.precipitationMm()
        );

        double thunderstormSubScore = scoreThunderstorm(
                condition.thunderstormProbabilityPercent(),
                condition.weatherCode()
        );

        double totalWeight = properties.windWeight()
                + properties.precipitationWeight()
                + properties.thunderstormWeight();

        double weightedScore = (properties.windWeight() * windSubScore
                + properties.precipitationWeight() * rainSubScore
                + properties.thunderstormWeight() * thunderstormSubScore)
                / (totalWeight > 0.0 ? totalWeight : 1.0);

        int rawScore = (int) Math.round(clamp(weightedScore, 0.0, 100.0));

        if (isElevatedThunderstormRisk(condition)) {
            return Math.min(rawScore, properties.thunderstormMaxOverallScore());
        }

        return rawScore;
    }

    public OverallScoreResult calculateOverallScore(List<WeatherSegment> segments) {
        Objects.requireNonNull(segments, "segments must not be null");
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        }

        double average = segments.stream()
                .mapToInt(WeatherSegment::score)
                .average()
                .orElse(0.0);

        int meanScore = clampInt((int) Math.round(average), 0, 100);

        boolean thunderstormRiskDetected = segments.stream()
                .map(WeatherSegment::condition)
                .anyMatch(this::isElevatedThunderstormRisk);

        if (thunderstormRiskDetected) {
            int cappedScore = Math.min(meanScore, properties.thunderstormMaxOverallScore());
            return new OverallScoreResult(
                    cappedScore,
                    true,
                    properties.scoreVersion()
            );
        }

        return new OverallScoreResult(
                meanScore,
                false,
                properties.scoreVersion()
        );
    }

    public boolean isElevatedThunderstormRisk(WeatherCondition condition) {
        if (condition == null) {
            return false;
        }
        if (condition.weatherCode() != null && condition.weatherCode() >= 95) {
            return true;
        }
        double thunderstormProb = condition.thunderstormProbabilityPercent() != null
                ? condition.thunderstormProbabilityPercent()
                : 0.0;
        return thunderstormProb >= properties.thunderstormRiskThresholdPercent();
    }

    public String resolveScoreColor(int score) {
        if (score >= 80) {
            return "#2F9E44";
        }
        if (score >= 60) {
            return "#82C91E";
        }
        if (score >= 40) {
            return "#F59F00";
        }
        if (score >= 20) {
            return "#F76707";
        }
        return "#E03131";
    }

    double scoreWind(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            double movementBearingDegrees
    ) {
        double speed = windSpeedKmh != null ? Math.max(0.0, windSpeedKmh) : 0.0;
        if (speed <= 0.0) {
            return 100.0;
        }

        double directionFactor = 1.0;
        if (windDirectionDegrees != null) {
            double angleDiff = angularDifferenceDegrees(
                    windDirectionDegrees,
                    movementBearingDegrees
            );
            // angleDiff == 0° -> headwind (wind blows FROM movement bearing): factor 1.35
            // angleDiff == 90° -> crosswind: factor 1.00
            // angleDiff == 180° -> tailwind: factor 0.65
            directionFactor = 1.0 + 0.35 * Math.cos(Math.toRadians(angleDiff));
        }

        double basePenalty = (speed / MAX_WIND_SPEED_KMH) * 100.0;
        double effectivePenalty = clamp(basePenalty * directionFactor, 0.0, 100.0);

        return 100.0 - effectivePenalty;
    }

    double scorePrecipitation(
            Double precipitationProbabilityPercent,
            Double precipitationMm
    ) {
        double prob = precipitationProbabilityPercent != null
                ? clamp(precipitationProbabilityPercent, 0.0, 100.0)
                : 0.0;
        double mm = precipitationMm != null
                ? Math.max(0.0, precipitationMm)
                : 0.0;

        double intensityPenalty = clamp((mm / MAX_PRECIPITATION_MM) * 100.0, 0.0, 100.0);
        double combinedPenalty = 0.40 * prob + 0.60 * intensityPenalty;

        return clamp(100.0 - combinedPenalty, 0.0, 100.0);
    }

    double scoreThunderstorm(
            Double thunderstormProbabilityPercent,
            Integer weatherCode
    ) {
        if (weatherCode != null && weatherCode >= 95) {
            return 0.0;
        }

        double prob = thunderstormProbabilityPercent != null
                ? clamp(thunderstormProbabilityPercent, 0.0, 100.0)
                : 0.0;

        double penalty = clamp(prob * 1.25, 0.0, 100.0);
        return 100.0 - penalty;
    }

    static double angularDifferenceDegrees(double angleA, double angleB) {
        double diff = Math.abs(angleA - angleB) % 360.0;
        return diff > 180.0 ? 360.0 - diff : diff;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record OverallScoreResult(
            int overallScore,
            boolean thunderstormCapApplied,
            String scoreVersion
    ) {
    }
}
