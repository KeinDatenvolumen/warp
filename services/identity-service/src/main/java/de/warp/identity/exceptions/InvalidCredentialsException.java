package de.warp.identity.exceptions;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Die E-Mail-Adresse oder das Passwort ist nicht korrekt.");
    }
}
