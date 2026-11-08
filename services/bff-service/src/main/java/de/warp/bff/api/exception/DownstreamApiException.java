// [KI-Log Nr. KI-089] (f6d1b8f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: forward registration problem details to frontend (WARP-005)
// [KI-Log Nr. KI-110] (7f53895) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: forward registration problem details to frontend (WARP-005)
// [KI-Log Nr. KI-117] (76887ed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve overall handling of registration (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.api.exception;

import org.springframework.http.HttpStatusCode;

public class DownstreamApiException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public DownstreamApiException(
            HttpStatusCode statusCode,
            String responseBody
    ) {
        super(
                "Der nachgelagerte Dienst hat einen Fehler geliefert."
        );
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}