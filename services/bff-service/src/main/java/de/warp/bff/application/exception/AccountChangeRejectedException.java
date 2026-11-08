// [KI-Log Nr. KI-117] (76887ed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve overall handling of registration (WARP-005)
// [KI-Log Nr. KI-203] (e8a363f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add user lookup and account change endpoints to identity service (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.application.exception;

import de.warp.bff.api.dto.FieldErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public class AccountChangeRejectedException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<FieldErrorResponse> fieldErrors;

    public AccountChangeRejectedException(
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