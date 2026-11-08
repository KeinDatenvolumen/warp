// [KI-Log Nr. KI-188] (c4f557c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-013): route calculation, departure time selection, and result display
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import type { LatLng } from '../../types/routeMap'
import type { RouteGeometry } from '../../types/routeCalculation'

export function toLeafletRoutePositions(
    geometry: RouteGeometry | null,
): LatLng[] {
    if (
        !geometry ||
        geometry.type !== 'LineString'
    ) {
        return []
    }

    return geometry.coordinates
        .filter(
            ([longitude, latitude]) =>
                Number.isFinite(longitude) &&
                Number.isFinite(latitude) &&
                longitude >= -180 &&
                longitude <= 180 &&
                latitude >= -90 &&
                latitude <= 90,
        )
        .map(
            ([longitude, latitude]) =>
                [latitude, longitude] as LatLng,
        )
}