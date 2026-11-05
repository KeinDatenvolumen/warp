// [KI-Log Nr. KI-15] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Routenplaner-UI & Polling (WARP-013)
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