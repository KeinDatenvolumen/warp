// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Wetter- & Routen-Plattform
package de.warp.identity.exceptions;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("Der Benutzer wurde nicht gefunden.");
    }
}