// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
import type {LatLng, RouteMapSegment} from '../../types/routeMap'
import type {SavedRoute} from '../../types/savedRoute'

export const DEFAULT_ROUTE_COLOR = '#4263eb'

/** Wandelt GeoJSON [Länge, Breite] in Leaflet [Breite, Länge] um. */
export function toLatLngPositions(coordinates: number[][]): LatLng[] {
    const positions: LatLng[] = []

    for (const coordinate of coordinates) {
        const longitude = coordinate[0]
        const latitude = coordinate[1]

        if (typeof longitude === 'number' && typeof latitude === 'number') {
            positions.push([latitude, longitude])
        }
    }

    return positions
}

/**
 * Bevorzugt die farbigen Abschnitte (Wetterscore). Fehlen deren Geometrien,
 * wird die gesamte Route in einer neutralen Farbe gezeichnet.
 */
export function buildRouteMapSegments(
    route: Pick<SavedRoute, 'geometry' | 'weatherSegments'>
): RouteMapSegment[] {
    const colored = [...route.weatherSegments]
        .sort((a, b) => a.segmentIndex - b.segmentIndex)
        .flatMap((segment) => {
            const positions = segment.geometry
                ? toLatLngPositions(segment.geometry.coordinates)
                : []

            return positions.length >= 2
                ? [{positions, color: segment.color}]
                : []
        })

    if (colored.length > 0) {
        return colored
    }

    const positions = toLatLngPositions(route.geometry.coordinates)

    return positions.length >= 2
        ? [{positions, color: DEFAULT_ROUTE_COLOR}]
        : []
}