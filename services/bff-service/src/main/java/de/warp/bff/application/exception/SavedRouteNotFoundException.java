package de.warp.bff.application.exception;

public class SavedRouteNotFoundException extends RuntimeException {

    public SavedRouteNotFoundException(Throwable cause) {
        super("Die gespeicherte Route wurde nicht gefunden.", cause);
    }
}