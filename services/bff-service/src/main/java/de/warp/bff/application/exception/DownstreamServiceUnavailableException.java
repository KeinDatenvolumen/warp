// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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