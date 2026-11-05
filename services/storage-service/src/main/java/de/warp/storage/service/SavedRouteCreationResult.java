// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.storage.service;

import de.warp.storage.dto.SavedRouteResponse;

/**
 * created = false bedeutet: Der Idempotency-Key war bereits bekannt und die
 * bestehende Route wird erneut ausgeliefert.
 */
public record SavedRouteCreationResult(
        SavedRouteResponse route,
        boolean created
) {
}