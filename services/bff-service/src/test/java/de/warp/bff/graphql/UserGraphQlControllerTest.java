// [KI-Log Nr. KI-203] (e8a363f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add user lookup and account change endpoints to identity service (WARP-033)
// [KI-Log Nr. KI-218] (e9946b9) KI-generiert (Google Antigravity (Gemini 4 Argon Medium)): Update UserGraphQlControllerTest.java (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.graphql;

import de.warp.bff.infrastructure.client.IdentityUserClient;
import de.warp.bff.infrastructure.client.dto.InternalUserResponse;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGraphQlControllerTest {

    @Mock
    private IdentityUserClient identityUserClient;

    @InjectMocks
    private UserGraphQlController controller;

    @Test
    void shouldReturnCurrentUser() {
        Instant createdAt = Instant.parse("2026-10-03T10:30:00Z");

        when(identityUserClient.getUser("usr_1")).thenReturn(
                new InternalUserResponse(
                        "usr_1",
                        "Max Mustermann",
                        "max@example.de",
                        true,
                        createdAt
                )
        );

        AuthenticatedSessionPrincipal principal =
                new AuthenticatedSessionPrincipal(
                        "usr_1",
                        Instant.parse("2026-10-17T08:00:00Z"),
                        Instant.parse("2026-10-17T20:00:00Z"),
                        true
                );

        UserGraphQlController.CurrentUserPayload result =
                controller.currentUser(principal);

        assertThat(result.id()).isEqualTo("usr_1");
        assertThat(result.name()).isEqualTo("Max Mustermann");
        assertThat(result.email()).isEqualTo("max@example.de");
        assertThat(result.emailVerified()).isTrue();
        assertThat(result.createdAt().toInstant()).isEqualTo(createdAt);
    }

    @Test
    void shouldRejectMissingPrincipal() {
        assertThatThrownBy(() -> controller.currentUser(null))
                .isInstanceOf(SessionInvalidException.class);

        verifyNoInteractions(identityUserClient);
    }
}
