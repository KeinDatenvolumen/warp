// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.infrastructure.openmeteo.exception;

public class OpenMeteoUnavailableException
        extends RuntimeException {

    public OpenMeteoUnavailableException(String message) {
        super(message);
    }

    public OpenMeteoUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}