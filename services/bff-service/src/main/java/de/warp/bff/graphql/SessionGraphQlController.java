// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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
                principal.expiresAt().atOffset(ZoneOffset.UTC),
                principal.emailVerified()
        );
    }

    public record CurrentSessionPayload(
            String userId,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt,
            boolean emailVerified
    ) {
    }
}