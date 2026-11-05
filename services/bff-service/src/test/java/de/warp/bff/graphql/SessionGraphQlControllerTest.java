// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
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