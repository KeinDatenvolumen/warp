// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.application.port;

import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherForecastBundle;

import java.util.List;

public interface WeatherProvider {

    WeatherForecastBundle getForecast(
            List<ForecastLocation> locations,
            ForecastPeriod period
    );
}