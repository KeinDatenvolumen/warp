// [KI-Log Nr. KI-096] (77d0318) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add public BFF login endpoint
// [KI-Log Nr. KI-097] (a952d0a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add public BFF logout endpoint
// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-200] (c059ad7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: require verified email for route calculation (WARP-030)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.application.service;

import de.warp.bff.api.dto.CreateSessionRequest;
import de.warp.bff.infrastructure.client.IdentityClient;
import de.warp.bff.infrastructure.client.dto.InternalCreateSessionRequest;
import de.warp.bff.infrastructure.client.dto.InternalCreatedSessionResponse;
import de.warp.bff.infrastructure.client.dto.InternalSessionResponse;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final IdentityClient identityClient;

    public SessionService(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    public CreatedSession createSession(
            CreateSessionRequest request
    ) {
        InternalCreateSessionRequest internalRequest =
                new InternalCreateSessionRequest(
                        request.email(),
                        request.password()
                );

        InternalCreatedSessionResponse internalResponse =
                identityClient.createSession(internalRequest);

        return new CreatedSession(
                internalResponse.sessionToken(),
                internalResponse.userId(),
                internalResponse.expiresAt()
        );
    }

    public ValidatedSession validateSession(String sessionToken) {
        InternalSessionResponse internalResponse =
                identityClient.validateSession(sessionToken);

        return new ValidatedSession(
                internalResponse.userId(),
                internalResponse.createdAt(),
                internalResponse.expiresAt(),
                internalResponse.emailVerified()
        );
    }

    public void invalidateSession(String sessionToken) {
        identityClient.invalidateSession(sessionToken);
    }
}

