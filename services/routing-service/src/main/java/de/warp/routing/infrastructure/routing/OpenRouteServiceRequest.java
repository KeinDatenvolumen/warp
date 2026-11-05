// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.infrastructure.routing;

import java.util.List;

public record OpenRouteServiceRequest(
        List<List<Double>> coordinates,
        boolean instructions
) {

    public static OpenRouteServiceRequest between(
            double startLatitude,
            double startLongitude,
            double destinationLatitude,
            double destinationLongitude
    ) {
        return new OpenRouteServiceRequest(
                List.of(
                        List.of(startLongitude, startLatitude),
                        List.of(
                                destinationLongitude,
                                destinationLatitude
                        )
                ),
                true
        );
    }
}