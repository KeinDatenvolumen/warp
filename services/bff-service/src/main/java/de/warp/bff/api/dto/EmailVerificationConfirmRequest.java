package de.warp.bff.api.dto;

public record EmailVerificationConfirmRequest(
        String token
) {
}