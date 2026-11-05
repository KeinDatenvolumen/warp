// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
package de.warp.identity.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Für diese E-Mail-Adresse existiert bereits ein Benutzerkonto.");
    }
}