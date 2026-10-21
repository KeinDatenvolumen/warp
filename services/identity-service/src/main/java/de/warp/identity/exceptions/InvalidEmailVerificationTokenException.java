package de.warp.identity.exceptions;

public class InvalidEmailVerificationTokenException
        extends RuntimeException {

    public InvalidEmailVerificationTokenException() {
        super("Der E-Mail-Verifikationslink ist ungültig oder abgelaufen");
    }
}