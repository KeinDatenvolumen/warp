package de.warp.bff.graphql;

public class SessionInvalidException extends RuntimeException {

    public SessionInvalidException() {
        super("Die Sitzung ist ungültig.");
    }

    public SessionInvalidException(Throwable cause) {
        super("Die Sitzung ist ungültig.", cause);
    }
}