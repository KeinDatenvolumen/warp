package de.warp.bff.infrastructure.client.dto;

public record InternalChangeEmailRequest(
        String newEmail,
        String password
) {
}