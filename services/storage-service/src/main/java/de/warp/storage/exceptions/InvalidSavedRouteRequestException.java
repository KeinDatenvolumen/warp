package de.warp.storage.exceptions;

import de.warp.storage.dto.FieldErrorResponse;

import java.util.List;

public class InvalidSavedRouteRequestException extends RuntimeException {

    private final transient List<FieldErrorResponse> fieldErrors;

    public InvalidSavedRouteRequestException(
            String field,
            String code,
            String message
    ) {
        super(message);
        this.fieldErrors = List.of(
                new FieldErrorResponse(field, code, message)
        );
    }

    public List<FieldErrorResponse> getFieldErrors() {
        return fieldErrors;
    }
}