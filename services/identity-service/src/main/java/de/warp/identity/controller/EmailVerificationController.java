// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.controller;

import de.warp.identity.dto.EmailVerificationConfirmRequest;
import de.warp.identity.dto.EmailVerificationResendRequest;
import de.warp.identity.service.EmailVerificationResendService;
import de.warp.identity.service.EmailVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/email-verifications")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationResendService resendService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService,
            EmailVerificationResendService resendService
    ) {
        this.emailVerificationService = emailVerificationService;
        this.resendService = resendService;
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmEmail(
            @RequestBody EmailVerificationConfirmRequest request
    ) {
        emailVerificationService.confirmEmail(request.token());
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerificationEmail(
            @RequestBody EmailVerificationResendRequest request
    ) {
        resendService.resendVerificationEmail(request.email());
    }
}