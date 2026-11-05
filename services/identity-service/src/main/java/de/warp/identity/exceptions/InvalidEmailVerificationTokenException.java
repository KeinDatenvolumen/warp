// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
package de.warp.identity.exceptions;

public class InvalidEmailVerificationTokenException
        extends RuntimeException {

    public InvalidEmailVerificationTokenException() {
        super("Der E-Mail-Verifikationslink ist ungültig oder abgelaufen");
    }
}