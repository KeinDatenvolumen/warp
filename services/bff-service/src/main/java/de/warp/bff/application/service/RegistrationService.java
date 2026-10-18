package de.warp.bff.application.service;

import de.warp.bff.api.dto.RegistrationRequest;
import de.warp.bff.api.dto.RegistrationResponse;
import de.warp.bff.infrastructure.client.IdentityClient;
import de.warp.bff.infrastructure.client.dto.InternalRegistrationRequest;
import de.warp.bff.infrastructure.client.dto.InternalRegistrationResponse;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private static final String SUCCESS_MESSAGE =
            "Das Benutzerkonto wurde erfolgreich erstellt.";

    private final IdentityClient identityClient;

    public RegistrationService(
            IdentityClient identityClient
    ) {
        this.identityClient = identityClient;
    }

    public RegistrationResponse register(
            RegistrationRequest request
    ) {
        InternalRegistrationRequest internalRequest =
                new InternalRegistrationRequest(
                        request.name(),
                        request.email(),
                        request.password()
                );

        InternalRegistrationResponse internalResponse =
                identityClient.register(internalRequest);

        return new RegistrationResponse(
                internalResponse.id(),
                internalResponse.name(),
                internalResponse.email(),
                internalResponse.emailVerified(),
                internalResponse.createdAt(),
                SUCCESS_MESSAGE
        );
    }
}