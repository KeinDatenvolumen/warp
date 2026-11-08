// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-200] (c059ad7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: require verified email for route calculation (WARP-030)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.graphql;

import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionGraphQlControllerTest {

    private SessionGraphQlController controller;

    @BeforeEach
    void setUp() {
        controller = new SessionGraphQlController();
    }

    @Test
    void shouldReturnCurrentSession() {
        Instant createdAt =
                Instant.parse("2026-10-17T08:00:00Z");

        Instant expiresAt =
                Instant.parse("2026-10-17T20:00:00Z");

        AuthenticatedSessionPrincipal principal =
                new AuthenticatedSessionPrincipal(
                        "user-123",
                        createdAt,
                        expiresAt,
                        true
                );

        SessionGraphQlController.CurrentSessionPayload response =
                controller.currentSession(principal);

        assertThat(response.userId())
                .isEqualTo("user-123");

        assertThat(response.createdAt().toInstant())
                .isEqualTo(createdAt);

        assertThat(response.expiresAt().toInstant())
                .isEqualTo(expiresAt);

        assertThat(response.emailVerified())
                .isTrue();
    }

    @Test
    void shouldRejectMissingPrincipal() {
        assertThatThrownBy(
                () -> controller.currentSession(null)
        )
                .isInstanceOf(SessionInvalidException.class)
                .hasMessage("Die Sitzung ist ungültig.");
    }
}