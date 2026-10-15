package de.warp.bff.api.dto;

public record RegistrationRequest(
        String name,
        String email,
        String password
) {
}