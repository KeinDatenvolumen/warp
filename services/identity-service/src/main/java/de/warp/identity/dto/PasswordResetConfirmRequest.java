package de.warp.identity.dto;

public record PasswordResetConfirmRequest(
        String token,
        String newPassword
) {
}