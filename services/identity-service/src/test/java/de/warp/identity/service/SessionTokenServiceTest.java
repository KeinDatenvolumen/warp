// [KI-Log Nr. KI-081] (26da463) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add secure session token generation
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokenServiceTest {

    private final SessionTokenService sessionTokenService =
            new SessionTokenService();

    @Test
    void shouldGenerateDifferentTokens() {
        String firstToken = sessionTokenService.generateToken();
        String secondToken = sessionTokenService.generateToken();

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    void shouldGenerateUrlSafeTokensWithExpectedLength() {
        String token = sessionTokenService.generateToken();

        assertThat(token)
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+");
    }

    @Test
    void shouldCreateSameHashForSameToken() {
        String token = sessionTokenService.generateToken();

        String firstHash = sessionTokenService.hashToken(token);
        String secondHash = sessionTokenService.hashToken(token);

        assertThat(firstHash).isEqualTo(secondHash);
    }

    @Test
    void shouldCreateDifferentHashesForDifferentTokens() {
        String firstToken = sessionTokenService.generateToken();
        String secondToken = sessionTokenService.generateToken();

        String firstHash = sessionTokenService.hashToken(firstToken);
        String secondHash = sessionTokenService.hashToken(secondToken);

        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    @Test
    void shouldCreateSha256HashAsLowercaseHexadecimalString() {
        String token = sessionTokenService.generateToken();

        String tokenHash = sessionTokenService.hashToken(token);

        assertThat(tokenHash)
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }
}