package de.warp.identity.dto;

public record EmailVerificationConfirmRequest(
        String token
) {
}