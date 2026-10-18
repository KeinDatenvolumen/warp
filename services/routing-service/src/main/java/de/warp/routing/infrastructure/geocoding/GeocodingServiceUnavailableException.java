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
