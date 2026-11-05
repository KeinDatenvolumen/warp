// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
package de.warp.bff.api.controller;

import de.warp.bff.api.dto.PasswordResetConfirmRequest;
import de.warp.bff.api.dto.PasswordResetSendRequest;
import de.warp.bff.infrastructure.client.IdentityPasswordResetClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/password-resets")
public class PasswordResetController {

    private final IdentityPasswordResetClient identityClient;

    public PasswordResetController(
            IdentityPasswordResetClient identityClient
    ) {
        this.identityClient = identityClient;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendPasswordResetEmail(
            @RequestBody PasswordResetSendRequest request
    ) {
        identityClient.sendPasswordResetEmail(
                request.email()
        );
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(
            @RequestBody PasswordResetConfirmRequest request
    ) {
        identityClient.confirmPasswordReset(
                request.token(),
                request.newPassword()
        );
    }
}