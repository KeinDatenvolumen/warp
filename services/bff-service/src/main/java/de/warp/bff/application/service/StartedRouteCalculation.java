// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.bff.application.service;

import java.time.Instant;

public record StartedRouteCalculation(
        String id,
        String status,
        Instant createdAt
) {
}