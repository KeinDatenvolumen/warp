// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.identity.exceptions;

public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException() {
        super("Die Sitzung ist abgelaufen oder wurde invalidiert.");
    }
}