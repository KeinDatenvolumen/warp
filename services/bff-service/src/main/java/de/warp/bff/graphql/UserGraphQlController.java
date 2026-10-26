package de.warp.bff.graphql;

import de.warp.bff.infrastructure.client.IdentityUserClient;
import de.warp.bff.infrastructure.client.dto.InternalUserResponse;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Controller
public class UserGraphQlController {

    private final IdentityUserClient identityUserClient;

    public UserGraphQlController(IdentityUserClient identityUserClient) {
        this.identityUserClient = identityUserClient;
    }

    @QueryMapping
    public CurrentUserPayload currentUser(
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        if (principal == null) {
            throw new SessionInvalidException();
        }

        InternalUserResponse user =
                identityUserClient.getUser(principal.userId());

        return new CurrentUserPayload(
                user.id(),
                user.name(),
                user.email(),
                user.emailVerified(),
                user.createdAt().atOffset(ZoneOffset.UTC)
        );
    }

    public record CurrentUserPayload(
            String id,
            String name,
            String email,
            boolean emailVerified,
            OffsetDateTime createdAt
    ) {
    }
}