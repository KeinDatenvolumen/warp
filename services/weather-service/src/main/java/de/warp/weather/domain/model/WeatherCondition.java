// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.domain.model;

public record WeatherCondition(
        Double windSpeedKmh,
        Double windDirectionDegrees,
        Double windGustsKmh,
        Double precipitationProbabilityPercent,
        Double precipitationMm,
        Double wetRoadIndex,
        Double snowfallCm,
        Double thunderstormProbabilityPercent,
        Double temperatureCelsius,
        Double apparentTemperatureCelsius,
        Double relativeHumidityPercent,
        Double dewPointCelsius,
        Double shortwaveRadiationWm2,
        Double soilTemperatureCelsius,
        Double visibilityMeters,
        Double capeJkg,
        Double uvIndex,
        Integer weatherCode
) {

    public WeatherCondition(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double thunderstormProbabilityPercent,
            Double temperatureCelsius,
            Integer weatherCode
    ) {
        this(
                windSpeedKmh,
                windDirectionDegrees,
                null,
                precipitationProbabilityPercent,
                precipitationMm,
                null,
                null,
                thunderstormProbabilityPercent,
                temperatureCelsius,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                weatherCode
        );
    }
}
