package de.warp.identity.dto;

public record FieldErrorResponse(
        String field,
        String code,
        String message
) {
}