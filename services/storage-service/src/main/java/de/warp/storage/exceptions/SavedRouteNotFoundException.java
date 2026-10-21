package de.warp.storage.exceptions;

public class SavedRouteNotFoundException extends RuntimeException {

    public SavedRouteNotFoundException() {
        super("Die gespeicherte Route wurde nicht gefunden.");
    }
}