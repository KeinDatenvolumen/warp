package de.warp.weather.infrastructure.openmeteo.exception;

public class OpenMeteoTimeoutException extends RuntimeException {

    public OpenMeteoTimeoutException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}