package de.warp.storage.dto;

import java.net.URI;
import java.util.List;

public record ApiErrorResponse(
        URI type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String correlationId,
        List<FieldErrorResponse> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }
}