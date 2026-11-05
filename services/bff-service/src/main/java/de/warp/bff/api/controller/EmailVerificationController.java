// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
package de.warp.bff.api.controller;

import de.warp.bff.api.dto.EmailVerificationConfirmRequest;
import de.warp.bff.api.dto.EmailVerificationResendRequest;
import de.warp.bff.infrastructure.client.IdentityEmailVerificationClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email-verifications")
public class EmailVerificationController {

    private final IdentityEmailVerificationClient identityClient;

    public EmailVerificationController(
            IdentityEmailVerificationClient identityClient
    ) {
        this.identityClient = identityClient;
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmEmail(
            @RequestBody EmailVerificationConfirmRequest request
    ) {
        identityClient.confirmEmail(request.token());
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerificationEmail(
            @RequestBody EmailVerificationResendRequest request
    ) {
        identityClient.resendVerificationEmail(request.email());
    }
}