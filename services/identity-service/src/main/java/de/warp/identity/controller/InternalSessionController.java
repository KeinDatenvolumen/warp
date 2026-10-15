package de.warp.identity.controller;

import de.warp.identity.dto.CreateSessionRequest;
import de.warp.identity.dto.CreatedSessionResponse;
import de.warp.identity.service.SessionService;
import jakarta.validation.Valid;
import de.warp.identity.dto.SessionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/internal/v1/sessions")
public class InternalSessionController {

    private final SessionService sessionService;

    public InternalSessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public CreatedSessionResponse createSession(
            @Valid @RequestBody CreateSessionRequest request
    ) {
        return sessionService.createSession(request);
    }

    @GetMapping("/{sessionToken}")
    public SessionResponse validateSession(
            @PathVariable String sessionToken
    ) {
        return sessionService.validateSession(sessionToken);
    }

    @DeleteMapping("/{sessionToken}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invalidateSession(
            @PathVariable String sessionToken
    ) {
        sessionService.invalidateSession(sessionToken);
    }
}