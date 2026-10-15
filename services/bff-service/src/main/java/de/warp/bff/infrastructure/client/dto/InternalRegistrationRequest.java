package de.warp.bff.infrastructure.client.dto;

public record InternalRegistrationRequest(
        String name,
        String email,
        String password
) {
}