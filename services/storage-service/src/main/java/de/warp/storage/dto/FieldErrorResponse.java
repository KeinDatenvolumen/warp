package de.warp.storage.dto;

public record FieldErrorResponse(
        String field,
        String code,
        String message
) {
}