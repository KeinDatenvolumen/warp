// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationTokenGeneratorTest {

    private final EmailVerificationTokenGenerator generator =
            new EmailVerificationTokenGenerator();

    @Test
    void shouldGenerateTokenAndSha256Hash() {
        var generatedToken = generator.generate();

        assertThat(generatedToken.plainToken())
                .isNotBlank()
                .doesNotContain("=");

        assertThat(generatedToken.tokenHash())
                .hasSize(64)
                .matches("[0-9a-f]{64}");

        assertThat(generator.hash(generatedToken.plainToken()))
                .isEqualTo(generatedToken.tokenHash());
    }

    @Test
    void shouldGenerateDifferentTokens() {
        var firstToken = generator.generate();
        var secondToken = generator.generate();

        assertThat(firstToken.plainToken())
                .isNotEqualTo(secondToken.plainToken());

        assertThat(firstToken.tokenHash())
                .isNotEqualTo(secondToken.tokenHash());
    }

    @Test
    void shouldCreateSameHashForSameToken() {
        String token = "festes-test-token";

        assertThat(generator.hash(token))
                .isEqualTo(generator.hash(token));
    }

    @Test
    void shouldRejectBlankTokenWhenHashing() {
        assertThatThrownBy(() -> generator.hash("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plainToken darf nicht leer sein");
    }

    @Test
    void shouldRejectNullTokenWhenHashing() {
        assertThatThrownBy(() -> generator.hash(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("plainToken darf nicht null sein");
    }
}