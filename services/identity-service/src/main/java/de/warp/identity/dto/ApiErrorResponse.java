package de.warp.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldErrorResponse> fieldErrors
) {
}
