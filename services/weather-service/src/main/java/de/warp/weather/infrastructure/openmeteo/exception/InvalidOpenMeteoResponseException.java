package de.warp.weather.infrastructure.openmeteo.exception;

public class InvalidOpenMeteoResponseException
        extends RuntimeException {

    public InvalidOpenMeteoResponseException(String message) {
        super(message);
    }

    public InvalidOpenMeteoResponseException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}