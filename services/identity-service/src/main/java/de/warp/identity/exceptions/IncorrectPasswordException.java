package de.warp.identity.exceptions;

public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException() {
        super("Das Passwort ist nicht korrekt.");
    }
}