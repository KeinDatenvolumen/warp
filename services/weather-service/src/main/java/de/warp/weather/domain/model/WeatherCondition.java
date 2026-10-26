package de.warp.weather.domain.model;

public record WeatherCondition(
        Double windSpeedKmh,
        Double windDirectionDegrees,
        Double precipitationProbabilityPercent,
        Double precipitationMm,
        Double thunderstormProbabilityPercent,
        Double temperatureCelsius,
        Integer weatherCode
) {
}
