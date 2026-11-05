// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
package de.warp.identity.exceptions;

public class PasswordRequirementsNotMetException
        extends RuntimeException {

    public PasswordRequirementsNotMetException() {
        super(
                "Das Passwort muss mindestens zwölf Zeichen lang sein"
        );
    }
}