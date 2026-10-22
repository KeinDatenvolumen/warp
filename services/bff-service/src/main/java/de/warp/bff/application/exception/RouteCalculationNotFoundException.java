package de.warp.bff.application.exception;

public class RouteCalculationNotFoundException
        extends RuntimeException {

    public RouteCalculationNotFoundException() {
        super("Die Routenberechnung wurde nicht gefunden.");
    }
}