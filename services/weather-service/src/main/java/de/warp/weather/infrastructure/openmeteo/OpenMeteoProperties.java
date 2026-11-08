// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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