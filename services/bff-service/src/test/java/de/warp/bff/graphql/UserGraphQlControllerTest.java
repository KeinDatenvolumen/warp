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
                        Instant.parse("2026-10-17T20:00:00Z")
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