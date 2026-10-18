package de.warp.bff.api.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    @GetMapping("/api/v1/csrf")
    public CsrfToken csrfToken(
            CsrfToken csrfToken
    ) {
        return csrfToken;
    }
}