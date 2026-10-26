package de.warp.identity.exceptions;

public class InvalidPasswordResetTokenException
        extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super(
                "Der Passwort-Rücksetzungslink ist ungültig oder abgelaufen"
        );
    }
}