package de.warp.bff.graphql;

import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Controller
public class SessionGraphQlController {

    @QueryMapping
    public CurrentSessionPayload currentSession(
            @AuthenticationPrincipal
            AuthenticatedSessionPrincipal principal
    ) {
        if (principal == null) {
            throw new SessionInvalidException();
        }

        return new CurrentSessionPayload(
                principal.userId(),
                principal.createdAt().atOffset(ZoneOffset.UTC),
                principal.expiresAt().atOffset(ZoneOffset.UTC)
        );
    }

    public record CurrentSessionPayload(
            String userId,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt
    ) {
    }
}