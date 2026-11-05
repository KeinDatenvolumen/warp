// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
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