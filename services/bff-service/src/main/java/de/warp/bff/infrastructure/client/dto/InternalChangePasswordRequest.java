package de.warp.bff.infrastructure.client.dto;

public record InternalChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}