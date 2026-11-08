// [KI-Log Nr. KI-084] (280bcc1) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add public registration endpoint (WARP-005)
// [KI-Log Nr. KI-089] (f6d1b8f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: forward registration problem details to frontend (WARP-005)
// [KI-Log Nr. KI-107] (a4fa70f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add public registration endpoint (WARP-005)
// [KI-Log Nr. KI-110] (7f53895) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: forward registration problem details to frontend (WARP-005)
// [KI-Log Nr. KI-117] (76887ed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve overall handling of registration (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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