// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.application.exception;

public class TooManyLoginAttemptsException
        extends RuntimeException {

    public static final int RETRY_AFTER_SECONDS = 60;

    public TooManyLoginAttemptsException() {
        super(
                "Es wurden zu viele Anmeldeversuche durchgeführt. "
                        + "Bitte versuche es später erneut."
        );
    }
}