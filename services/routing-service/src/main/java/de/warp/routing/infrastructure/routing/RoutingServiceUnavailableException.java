// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.infrastructure.routing;

public class RoutingServiceUnavailableException
        extends RuntimeException {

    public RoutingServiceUnavailableException(String message) {
        super(message);
    }

    public RoutingServiceUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}