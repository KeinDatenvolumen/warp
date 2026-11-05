// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.application;

public class RouteCalculationNotFoundException
        extends RuntimeException {

    public RouteCalculationNotFoundException(
            String calculationId
    ) {
        super(
                "Die Routenberechnung wurde nicht gefunden: "
                        + calculationId
        );
    }
}