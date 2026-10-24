package de.warp.bff.api.dto;

public record PasswordResetConfirmRequest(
        String token,
        String newPassword
) {
}