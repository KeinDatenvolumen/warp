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
