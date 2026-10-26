package de.warp.weather.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "warp.weather.scoring")
public record WeatherScoringProperties(
        String scoreVersion,
        double windWeight,
        double precipitationWeight,
        double thunderstormWeight,
        double thunderstormRiskThresholdPercent,
        int thunderstormMaxOverallScore
) {

    public WeatherScoringProperties {
        if (scoreVersion == null || scoreVersion.isBlank()) {
            scoreVersion = "1.0";
        }
        if (windWeight <= 0.0 && precipitationWeight <= 0.0 && thunderstormWeight <= 0.0) {
            windWeight = 0.30;
            precipitationWeight = 0.45;
            thunderstormWeight = 0.25;
        }
        if (thunderstormRiskThresholdPercent <= 0.0 || thunderstormRiskThresholdPercent > 100.0) {
            thunderstormRiskThresholdPercent = 50.0;
        }
        if (thunderstormMaxOverallScore <= 0 || thunderstormMaxOverallScore > 100) {
            thunderstormMaxOverallScore = 50;
        }
    }

    public static WeatherScoringProperties defaults() {
        return new WeatherScoringProperties(
                "1.0",
                0.30,
                0.45,
                0.25,
                50.0,
                50
        );
    }
}
