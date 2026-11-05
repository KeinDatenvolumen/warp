// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
package de.warp.bff.application.exception;

import de.warp.bff.api.dto.FieldErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public class RegistrationRejectedException
        extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<FieldErrorResponse> fieldErrors;

    public RegistrationRejectedException(
            HttpStatus status,
            String code,
            String message,
            List<FieldErrorResponse> fieldErrors
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldErrorResponse> getFieldErrors() {
        return fieldErrors;
    }
}