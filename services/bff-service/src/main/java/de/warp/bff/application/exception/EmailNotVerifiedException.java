// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.application.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Die E-Mail-Adresse ist noch nicht verifiziert.");
    }

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
