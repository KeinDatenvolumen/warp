// [KI-Log Nr. KI-089] (f6d1b8f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: forward registration problem details to frontend (WARP-005)
// [KI-Log Nr. KI-110] (7f53895) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: forward registration problem details to frontend (WARP-005)
// [KI-Log Nr. KI-117] (76887ed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve overall handling of registration (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@RestControllerAdvice
public class DownstreamApiExceptionHandler {

    private final ObjectMapper objectMapper;

    public DownstreamApiExceptionHandler(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(DownstreamApiException.class)
    public ResponseEntity<JsonNode> handleDownstreamApiException(
            DownstreamApiException exception
    ) {
        try {
            JsonNode parsedBody = objectMapper.readTree(
                    exception.getResponseBody()
            );

            if (parsedBody instanceof ObjectNode objectNode) {
                objectNode.put(
                        "instance",
                        "/api/v1/users"
                );

                return ResponseEntity
                        .status(exception.getStatusCode())
                        .contentType(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                        .body(objectNode);
            }
        } catch (Exception ignored) {
            // Fallback weiter unten.
        }

        ObjectNode fallbackBody = objectMapper.createObjectNode();
        fallbackBody.put("status", 502);
        fallbackBody.put(
                "code",
                "DOWNSTREAM_SERVICE_ERROR"
        );
        fallbackBody.put(
                "detail",
                "Die Registrierung konnte nicht abgeschlossen werden."
        );
        fallbackBody.putArray("fieldErrors");

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(fallbackBody);
    }
}