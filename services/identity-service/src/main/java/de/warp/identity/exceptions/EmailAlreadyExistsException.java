package de.warp.identity.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Für diese E-Mail-Adresse existiert bereits ein Benutzerkonto.");
    }
}