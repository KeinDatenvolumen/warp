package de.warp.weather.infrastructure.openmeteo;

import de.warp.weather.domain.model.WeatherModel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(
        prefix = "warp.weather.open-meteo"
)
public record OpenMeteoProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int maximumLocationsPerRequest,
        int forecastHours,
        List<WeatherModel> comparisonModels
) {

    public OpenMeteoProperties {
        comparisonModels = List.copyOf(comparisonModels);
    }
}