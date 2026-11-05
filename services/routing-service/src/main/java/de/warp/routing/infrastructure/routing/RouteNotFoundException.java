// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.infrastructure.routing;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String message) {
        super(message);
    }

    public RouteNotFoundException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}