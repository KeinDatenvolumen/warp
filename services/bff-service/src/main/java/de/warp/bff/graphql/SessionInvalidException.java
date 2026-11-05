// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.graphql;

public class SessionInvalidException extends RuntimeException {

    public SessionInvalidException() {
        super("Die Sitzung ist ungültig.");
    }

    public SessionInvalidException(Throwable cause) {
        super("Die Sitzung ist ungültig.", cause);
    }
}