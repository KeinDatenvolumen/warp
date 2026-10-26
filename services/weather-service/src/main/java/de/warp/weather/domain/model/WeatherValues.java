package de.warp.weather.domain.model;

public record WeatherValues(
        Double temperatureCelsius,
        Double apparentTemperatureCelsius,
        Double relativeHumidityPercent,
        Double dewPointCelsius,
        Double wetBulbTemperatureCelsius,

        Double precipitationProbabilityPercent,
        Double precipitationMillimeters,
        Double rainMillimeters,
        Double showersMillimeters,
        Double snowfallCentimeters,
        Double snowDepthMeters,

        Double windSpeedKmh,
        Double windDirectionDegrees,
        Double windGustsKmh,

        Double pressureMslHpa,
        Double surfacePressureHpa,

        Double cloudCoverPercent,
        Double cloudCoverLowPercent,
        Double cloudCoverMidPercent,
        Double cloudCoverHighPercent,
        Double visibilityMeters,

        Double uvIndex,
        Double uvIndexClearSky,
        Double shortwaveRadiationWattsPerSquareMeter,
        Double directRadiationWattsPerSquareMeter,
        Double diffuseRadiationWattsPerSquareMeter,
        Double sunshineDurationSeconds,
        Boolean daylight,

        Double capeJoulesPerKilogram,
        Double liftedIndex,
        Double convectiveInhibitionJoulesPerKilogram,
        Double freezingLevelHeightMeters,

        Double vapourPressureDeficitKpa,
        Double evapotranspirationMillimeters,
        Double soilTemperatureSurfaceCelsius,

        Integer weatherCode
) {
}