package de.warp.identity.dto;

public record ChangeEmailRequest(
        String newEmail,
        String password
) {
}