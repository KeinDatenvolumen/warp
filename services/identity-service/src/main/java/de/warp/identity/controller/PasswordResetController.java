// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.controller;

import de.warp.identity.dto.PasswordResetConfirmRequest;
import de.warp.identity.dto.PasswordResetSendRequest;
import de.warp.identity.service.PasswordResetRequestService;
import de.warp.identity.service.PasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/password-resets")
public class PasswordResetController {

    private final PasswordResetRequestService requestService;
    private final PasswordResetService passwordResetService;

    public PasswordResetController(
            PasswordResetRequestService requestService,
            PasswordResetService passwordResetService
    ) {
        this.requestService = requestService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendPasswordResetEmail(
            @RequestBody PasswordResetSendRequest request
    ) {
        requestService.requestPasswordReset(
                request.email()
        );
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(
            @RequestBody PasswordResetConfirmRequest request
    ) {
        passwordResetService.resetPassword(
                request.token(),
                request.newPassword()
        );
    }
}