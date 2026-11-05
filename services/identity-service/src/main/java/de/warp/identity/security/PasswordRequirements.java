// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
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