package de.warp.identity.exceptions;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("Der Benutzer wurde nicht gefunden.");
    }
}