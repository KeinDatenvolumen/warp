package de.warp.weather.infrastructure.openmeteo.exception;

public class OpenMeteoRateLimitException
        extends RuntimeException {

    public OpenMeteoRateLimitException(String message) {
        super(message);
    }
}