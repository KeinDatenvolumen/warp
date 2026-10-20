package de.warp.bff.api.controller;

import de.warp.bff.api.dto.CreateSessionRequest;
import de.warp.bff.api.dto.CreateSessionResponse;
import de.warp.bff.application.service.CreatedSession;
import de.warp.bff.application.service.SessionService;
import de.warp.bff.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final LoginRateLimiter loginRateLimiter;

    public SessionController(
            SessionService sessionService,
            LoginRateLimiter loginRateLimiter
    ) {
        this.sessionService = sessionService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping
    public ResponseEntity<CreateSessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest loginRequest,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.checkLoginAllowed(
                loginRequest.email(),
                httpRequest.getRemoteAddr()
        );

        CreatedSession createdSession =
                sessionService.createSession(loginRequest);

        ResponseCookie sessionCookie = ResponseCookie
                .from(
                        "session",
                        createdSession.sessionToken()
                )
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .build();

        CreateSessionResponse response =
                new CreateSessionResponse(
                        createdSession.userId(),
                        createdSession.expiresAt()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        sessionCookie.toString()
                )
                .body(response);
    }


    @DeleteMapping("/current")
    public ResponseEntity<Void> invalidateSession(
            @CookieValue(
                    name = "session",
                    required = false
            )
            String sessionToken
    ) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            sessionService.invalidateSession(sessionToken);
        }

        ResponseCookie deletedSessionCookie = ResponseCookie
                .from("session", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deletedSessionCookie.toString()
                )
                .build();
    }
}
