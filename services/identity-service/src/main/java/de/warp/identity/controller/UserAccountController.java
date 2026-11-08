// [KI-Log Nr. KI-202] (d555a4e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add user lookup and account change endpoints to identity service (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.controller;

import de.warp.identity.dto.ChangeEmailRequest;
import de.warp.identity.dto.ChangePasswordRequest;
import de.warp.identity.dto.UserResponse;
import de.warp.identity.service.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users/{userId}")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public UserResponse getUser(@PathVariable("userId") String userId) {
        return userAccountService.getUser(userId);
    }

    @PostMapping("/password-changes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @PathVariable("userId") String userId,
            @RequestBody ChangePasswordRequest request
    ) {
        userAccountService.changePassword(userId, request);
    }

    @PostMapping("/email-changes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEmail(
            @PathVariable("userId") String userId,
            @RequestBody ChangeEmailRequest request
    ) {
        userAccountService.changeEmail(userId, request);
    }
}