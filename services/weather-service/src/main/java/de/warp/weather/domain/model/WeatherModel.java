// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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