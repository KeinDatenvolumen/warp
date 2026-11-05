// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.bff.application.exception;

public class RoutingServiceUnavailableException
        extends RuntimeException {

    public RoutingServiceUnavailableException() {
        super(
                "Der Routing-Dienst ist momentan "
                        + "nicht verfügbar."
        );
    }

    public RoutingServiceUnavailableException(
            Throwable cause
    ) {
        super(
                "Der Routing-Dienst ist momentan "
                        + "nicht verfügbar.",
                cause
        );
    }
}