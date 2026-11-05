// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
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