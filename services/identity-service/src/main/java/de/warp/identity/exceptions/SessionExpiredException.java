package de.warp.identity.exceptions;

public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException() {
        super("Die Sitzung ist abgelaufen oder wurde invalidiert.");
    }
}