package de.warp.bff.infrastructure.client.dto;

public record InternalCreateSessionRequest(
        String email,
        String password
) {
}