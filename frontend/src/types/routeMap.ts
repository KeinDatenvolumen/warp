/** [Breitengrad, Längengrad], wie von Leaflet erwartet. */
export type LatLng = [number, number]

export interface RouteMapSegment {
    positions: LatLng[]
    color: string
}