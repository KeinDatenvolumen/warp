package de.warp.bff.api.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}