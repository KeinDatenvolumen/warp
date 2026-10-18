package de.warp.bff.api.dto;

public record FieldErrorResponse(
        String field,
        String code,
        String message
) {
}