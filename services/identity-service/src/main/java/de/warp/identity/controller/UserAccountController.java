// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Wetter- & Routen-Plattform
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