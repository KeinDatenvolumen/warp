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