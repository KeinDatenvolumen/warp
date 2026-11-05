// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Wetter- & Routen-Plattform
package de.warp.bff.application.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Die E-Mail-Adresse ist noch nicht verifiziert.");
    }

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
