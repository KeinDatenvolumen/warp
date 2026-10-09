package de.warp.identity.dto;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<FieldErrorResponse> fieldErrors
) {
}
