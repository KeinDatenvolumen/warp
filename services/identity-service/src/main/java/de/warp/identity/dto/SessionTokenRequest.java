package de.warp.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionTokenRequest(
        @NotBlank
        String sessionToken
) {
}