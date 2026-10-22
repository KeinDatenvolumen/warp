package de.warp.bff.application.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Die E-Mail-Adresse ist noch nicht verifiziert.");
    }

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
