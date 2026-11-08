// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.security;

public final class PasswordRequirements {

    public static final int MIN_LENGTH = 12;

    private PasswordRequirements() {
    }

    public static boolean areMetBy(String password) {
        return password != null
                && password.length() >= MIN_LENGTH;
    }
}