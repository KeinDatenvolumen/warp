package de.warp.bff.infrastructure.identity.dto;

import de.warp.bff.api.dto.FieldErrorResponse;
import java.util.List;

public record IdentityProblemResponse(
        String type,
        String title,
        Integer status,
        String code,
        String detail,
        String instance,
        String correlationId,
        List<FieldErrorResponse> fieldErrors
) {
    public List<FieldErrorResponse> safeFieldErrors() {
        return fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }
}