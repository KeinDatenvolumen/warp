package de.warp.bff.api.controller;

import de.warp.bff.api.dto.CreateSessionRequest;
import de.warp.bff.api.dto.CreateSessionResponse;
import de.warp.bff.application.service.CreatedSession;
import de.warp.bff.application.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<CreateSessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request
    ) {
        CreatedSession createdSession =
                sessionService.createSession(request);

        ResponseCookie sessionCookie = ResponseCookie
                .from("session", createdSession.sessionToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
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
}