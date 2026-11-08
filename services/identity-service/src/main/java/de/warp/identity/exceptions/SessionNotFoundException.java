// [KI-Log Nr. KI-086] (f458eed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add session validation and invalidation
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.exceptions;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException() {
        super("Die Sitzung wurde nicht gefunden.");
    }
}