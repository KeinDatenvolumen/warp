package de.warp.bff.api.exception;

public class InvalidEmailVerificationTokenException
        extends RuntimeException {

    public InvalidEmailVerificationTokenException() {
        super(
                "Der E-Mail-Verifikationslink ist "
                        + "ungültig oder abgelaufen"
        );
    }
}