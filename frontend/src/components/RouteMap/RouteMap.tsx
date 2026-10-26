import { useEffect, useState } from 'react'
import {
    MapContainer,
    Marker,
    Polyline,
    Popup,
    TileLayer,
    useMap
} from 'react-leaflet'
import {divIcon, latLngBounds, type LatLngExpression} from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { Place } from '../../types/searchPlaces'
import type { RouteMapSegment } from '../../types/routeMap'
import './RouteMap.css'

const MAP_CENTER: LatLngExpression = [49.354111, 9.150306]

// Konstante, damit der Viewport-Effekt nicht bei jedem Render neu läuft.
const NO_SEGMENTS: RouteMapSegment[] = []

interface RouteMapProps {
    startPlace: Place | null
    destinationPlace: Place | null
    routeSegments?: RouteMapSegment[]
}

const startIcon = divIcon({
    className: 'route-map__marker route-map__marker--start',
    html: '<span>S</span>',
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36],
})

const destinationIcon = divIcon({
    className: 'route-map__marker route-map__marker--destination',
    html: '<span>Z</span>',
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36],
})

interface MapViewportUpdaterProps {
    startPlace: Place | null
    destinationPlace: Place | null
    routeSegments: RouteMapSegment[]
}

function MapViewportUpdater({
                                startPlace,
                                destinationPlace,
                                routeSegments,
                            }: MapViewportUpdaterProps) {
    const map = useMap()

    useEffect(() => {
        const routePositions = routeSegments.flatMap(
            (segment) => segment.positions,
        )

        if (routePositions.length >= 2) {
            map.fitBounds(latLngBounds(routePositions), {
                padding: [50, 50],
                maxZoom: 15,
            })

            return
        }

        if (startPlace && destinationPlace) {
            const bounds = latLngBounds([
                [startPlace.latitude, startPlace.longitude],
                [
                    destinationPlace.latitude,
                    destinationPlace.longitude,
                ],
            ])

            map.fitBounds(bounds, {
                padding: [50, 50],
                maxZoom: 14,
            })

            return
        }

        const selectedPlace = startPlace ?? destinationPlace

        if (selectedPlace) {
            map.setView(
                [
                    selectedPlace.latitude,
                    selectedPlace.longitude,
                ],
                14,
            )
        }
    }, [map, startPlace, destinationPlace, routeSegments])

    return null
}

export default function RouteMap({
                                     startPlace,
                                     destinationPlace,
                                     routeSegments = NO_SEGMENTS,
                                 }: RouteMapProps) {
    const [hasError, setHasError] = useState(false)
    const apiKey =
        import.meta.env.VITE_MAPTILER_PUBLIC_API_KEY

    if (!apiKey) {
        return (
            <div className="map-error" role="alert">
                Die Karte ist momentan nicht konfiguriert.
            </div>
        )
    }

    return (
        <section
            className="route-map"
            aria-label="Karte des Routenplaners"
        >
            {hasError && (
                <div
                    className="map-error map-error--overlay"
                    role="alert"
                >
                    Die Karte konnte nicht vollständig geladen werden.
                </div>
            )}

            <MapContainer
                center={MAP_CENTER}
                zoom={12}
                minZoom={3}
                maxZoom={19}
                scrollWheelZoom
                dragging
                touchZoom
                doubleClickZoom
                zoomControl
                className="route-map__container"
            >
                <TileLayer
                    url={`https://api.maptiler.com/maps/outdoor-v4/{z}/{x}/{y}.png?key=${apiKey}`}
                    tileSize={512}
                    zoomOffset={-1}
                    minZoom={3}
                    maxZoom={19}
                    crossOrigin
                    attribution={
                        '&copy; <a href="https://www.maptiler.com/copyright/" target="_blank" rel="noopener noreferrer">MapTiler</a> ' +
                        '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors'
                    }
                    eventHandlers={{
                        tileerror: () => setHasError(true),
                    }}
                />

                <MapViewportUpdater
                    startPlace={startPlace}
                    destinationPlace={destinationPlace}
                    routeSegments={routeSegments}
                />

                {routeSegments.map((segment, index) => (
                    <Polyline
                        key={index}
                        positions={segment.positions}
                        pathOptions={{
                            color: segment.color,
                            weight: 6,
                            opacity: 0.9,
                        }}
                    />
                ))}

                {startPlace && (
                    <Marker
                        position={[
                            startPlace.latitude,
                            startPlace.longitude,
                        ]}
                        icon={startIcon}
                    >
                        <Popup>
                            <strong>Startpunkt</strong>
                            <br />
                            {startPlace.displayName}
                        </Popup>
                    </Marker>
                )}

                {destinationPlace && (
                    <Marker
                        position={[
                            destinationPlace.latitude,
                            destinationPlace.longitude,
                        ]}
                        icon={destinationIcon}
                    >
                        <Popup>
                            <strong>Zielpunkt</strong>
                            <br />
                            {destinationPlace.displayName}
                        </Popup>
                    </Marker>
                )}
            </MapContainer>
        </section>
    )
}