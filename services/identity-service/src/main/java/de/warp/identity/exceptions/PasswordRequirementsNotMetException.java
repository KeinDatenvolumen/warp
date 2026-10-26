package de.warp.identity.exceptions;

public class PasswordRequirementsNotMetException
        extends RuntimeException {

    public PasswordRequirementsNotMetException() {
        super(
                "Das Passwort muss mindestens zwölf Zeichen lang sein"
        );
    }
}