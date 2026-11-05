// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.bff.application.exception;

public class InvalidRouteRequestException
        extends RuntimeException {

    public InvalidRouteRequestException() {
        super("Die Routenanfrage ist ungültig.");
    }
}