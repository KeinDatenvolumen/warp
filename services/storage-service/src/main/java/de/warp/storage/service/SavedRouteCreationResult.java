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