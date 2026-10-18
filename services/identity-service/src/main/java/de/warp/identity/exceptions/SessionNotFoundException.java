package de.warp.identity.exceptions;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException() {
        super("Die Sitzung wurde nicht gefunden.");
    }
}