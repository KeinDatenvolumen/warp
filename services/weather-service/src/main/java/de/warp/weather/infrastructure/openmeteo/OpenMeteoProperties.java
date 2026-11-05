// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
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