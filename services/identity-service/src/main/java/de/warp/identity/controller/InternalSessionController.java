// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.identity.controller;

import de.warp.identity.dto.CreateSessionRequest;
import de.warp.identity.dto.CreatedSessionResponse;
import de.warp.identity.service.SessionService;
import jakarta.validation.Valid;
import de.warp.identity.dto.SessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import de.warp.identity.dto.SessionTokenRequest;

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

    @PostMapping("/validation")
    public SessionResponse validateSession(
            @Valid @RequestBody SessionTokenRequest request
    ) {
        return sessionService.validateSession(
                request.sessionToken()
        );
    }

    @PostMapping("/invalidation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invalidateSession(
            @Valid @RequestBody SessionTokenRequest request
    ) {
        sessionService.invalidateSession(
                request.sessionToken()
        );
    }
}