// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.domain.model;

public enum WeatherModel {

    BEST_MATCH(null),

    ICON_SEAMLESS("icon_seamless"),

    ECMWF_IFS_025("ecmwf_ifs025"),

    GFS_SEAMLESS("gfs_seamless");

    private final String openMeteoValue;

    WeatherModel(String openMeteoValue) {
        this.openMeteoValue = openMeteoValue;
    }

    public String openMeteoValue() {
        return openMeteoValue;
    }

    public boolean isBestMatch() {
        return this == BEST_MATCH;
    }
}