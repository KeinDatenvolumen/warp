// [KI-Log Nr. KI-203] (e8a363f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add user lookup and account change endpoints to identity service (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.api.controller;

import de.warp.bff.api.dto.ChangeEmailRequest;
import de.warp.bff.api.dto.ChangePasswordRequest;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.client.IdentityUserClient;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class AccountController {

    private final IdentityUserClient identityUserClient;

    public AccountController(IdentityUserClient identityUserClient) {
        this.identityUserClient = identityUserClient;
    }

    @PostMapping("/password-changes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        identityUserClient.changePassword(
                requireUserId(principal),
                request.currentPassword(),
                request.newPassword()
        );
    }

    @PostMapping("/email-changes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEmail(
            @RequestBody ChangeEmailRequest request,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        identityUserClient.changeEmail(
                requireUserId(principal),
                request.newEmail(),
                request.password()
        );
    }

    private String requireUserId(AuthenticatedSessionPrincipal principal) {
        if (principal == null) {
            throw new SessionInvalidException();
        }

        return principal.userId();
    }
}