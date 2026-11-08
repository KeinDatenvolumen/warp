// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
            scoreVersion = "2.0";
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
                "2.0",
                0.30,
                0.45,
                0.25,
                50.0,
                50
        );
    }
}
