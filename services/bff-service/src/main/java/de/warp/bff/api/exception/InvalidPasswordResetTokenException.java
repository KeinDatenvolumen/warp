package de.warp.bff.api.exception;

public class InvalidPasswordResetTokenException
        extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super(
                "Der Passwort-Rücksetzungslink ist "
                        + "ungültig oder abgelaufen"
        );
    }
}