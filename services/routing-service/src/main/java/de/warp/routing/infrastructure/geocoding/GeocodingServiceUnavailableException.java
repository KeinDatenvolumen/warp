// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
package de.warp.routing.infrastructure.geocoding;

public class GeocodingServiceUnavailableException extends RuntimeException {

    public GeocodingServiceUnavailableException(String message) {
        super(message);
    }

    public GeocodingServiceUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
