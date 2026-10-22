package de.warp.bff.application.exception;

public class InvalidRouteRequestException
        extends RuntimeException {

    public InvalidRouteRequestException() {
        super("Die Routenanfrage ist ungültig.");
    }
}