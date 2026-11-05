// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
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