package de.warp.bff.api.dto;

public record ChangeEmailRequest(
        String newEmail,
        String password
) {
}