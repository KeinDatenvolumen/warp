// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTokenGeneratorTest {

    private final PasswordResetTokenGenerator tokenGenerator =
            new PasswordResetTokenGenerator();

    @Test
    void shouldGeneratePlainTokenAndMatchingHash() {
        PasswordResetTokenGenerator.GeneratedToken generatedToken =
                tokenGenerator.generate();

        assertThat(generatedToken.plainToken())
                .isNotBlank()
                .doesNotContain("=");

        assertThat(generatedToken.tokenHash())
                .hasSize(64)
                .matches("[0-9a-f]{64}");

        assertThat(tokenGenerator.hash(generatedToken.plainToken()))
                .isEqualTo(generatedToken.tokenHash());
    }

    @Test
    void shouldGenerateDifferentTokens() {
        PasswordResetTokenGenerator.GeneratedToken firstToken =
                tokenGenerator.generate();

        PasswordResetTokenGenerator.GeneratedToken secondToken =
                tokenGenerator.generate();

        assertThat(firstToken.plainToken())
                .isNotEqualTo(secondToken.plainToken());

        assertThat(firstToken.tokenHash())
                .isNotEqualTo(secondToken.tokenHash());
    }

    @Test
    void shouldCreateDeterministicHash() {
        String plainToken = "ein-fester-test-token";

        String firstHash = tokenGenerator.hash(plainToken);
        String secondHash = tokenGenerator.hash(plainToken);

        assertThat(firstHash)
                .isEqualTo(secondHash)
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    @Test
    void shouldRejectNullTokenWhenHashing() {
        assertThatThrownBy(() -> tokenGenerator.hash(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("plainToken darf nicht null sein");
    }

    @Test
    void shouldRejectBlankTokenWhenHashing() {
        assertThatThrownBy(() -> tokenGenerator.hash("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plainToken darf nicht leer sein");
    }
}