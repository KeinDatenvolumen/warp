// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.application.exception;

public class DownstreamServiceUnavailableException
        extends RuntimeException {

    public DownstreamServiceUnavailableException() {
        super(
                "Der Identitätsdienst ist momentan nicht verfügbar. "
                        + "Bitte versuche es später erneut."
        );
    }

    public DownstreamServiceUnavailableException(
            Throwable cause
    ) {
        super(
                "Der Identitätsdienst ist momentan nicht verfügbar. "
                        + "Bitte versuche es später erneut.",
                cause
        );
    }
}