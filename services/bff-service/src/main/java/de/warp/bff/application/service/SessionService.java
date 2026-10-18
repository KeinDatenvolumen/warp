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
                internalResponse.expiresAt()
        );
    }

    public void invalidateSession(String sessionToken) {
        identityClient.invalidateSession(sessionToken);
    }
}

