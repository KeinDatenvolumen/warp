import {
    useEffect,
    useMemo,
    useState,
} from 'react'
import {
    MapContainer,
    Marker,
    Polyline,
    Popup,
    TileLayer,
    Tooltip,
    useMap,
} from 'react-leaflet'
import {
    divIcon,
    latLngBounds,
    type LatLngExpression,
} from 'leaflet'
import 'leaflet/dist/leaflet.css'

import type {
    RouteGeometry,
    RouteWeatherSegment,
} from '../../types/routeCalculation'
import type {
    LatLng,
    RouteMapSegment,
} from '../../types/routeMap'
import type { Place } from '../../types/searchPlaces'
import { getScoreColor } from '../../utils/weatherColorScale'
import { toLeafletRoutePositions } from './routeGeometry'
import './RouteMap.css'

const MAP_CENTER: LatLngExpression = [
    49.354111,
    9.150306,
]

const NO_SEGMENTS: RouteMapSegment[] = []

export interface RouteMapProps {
    startPlace: Place | null
    destinationPlace: Place | null
    routeSegments?: RouteMapSegment[]
    geometry?: RouteGeometry | null
    weatherSegments?: RouteWeatherSegment[]
    hoveredSegmentIndex?: number | null
    onHoverSegment?: (index: number | null) => void
}

const startIcon = divIcon({
    className:
        'route-map__marker route-map__marker--start',
    html: '<span aria-hidden="true">S</span>',
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36],
})

const destinationIcon = divIcon({
    className:
        'route-map__marker route-map__marker--destination',
    html: '<span aria-hidden="true">Z</span>',
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36],
})

interface BottleneckBadge {
    segmentIndex: number
    position: LatLng
    icon: string
    label: string
    color: string
}

interface ProcessedWeatherSegment extends RouteWeatherSegment {
    positions: LatLng[]
    midpoint: LatLng | null
    color: string
}

function escapeHtml(value: string): string {
    return value.replace(
        /[&<>'"]/g,
        (character) => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;',
        })[character] ?? character,
    )
}

function resolveBottleneckBadge(
    segment: RouteWeatherSegment,
    position: LatLng,
): BottleneckBadge | null {
    const score = segment.score
    const factor =
        segment.limitingFactor ??
        segment.weather?.limitingFactor ??
        ''
    const reason =
        segment.reasonLabel ??
        segment.weather?.reasonLabel ??
        ''
    const normalizedReason = reason.toLocaleLowerCase('de-DE')
    const rain = segment.weather?.precipitationMm ?? 0
    const wind = segment.weather?.windSpeedKmh ?? 0
    const wetRoadIndex = segment.weather?.wetRoadIndex ?? 0

    const hasHardCap = [
        'THUNDER_CAP',
        'ICE_CAP',
        'STORM_CAP',
    ].includes(factor)

    if (score >= 65 && !hasHardCap) {
        return null
    }

    const common = {
        segmentIndex: segment.segmentIndex,
        position,
        color: getScoreColor(score),
    }

    if (
        factor === 'THUNDER_CAP' ||
        normalizedReason.includes('gewitter')
    ) {
        return {
            ...common,
            icon: '⛈️',
            label: reason || 'MAX 50 · Gewitterrisiko',
        }
    }

    if (
        factor === 'ICE_CAP' ||
        normalizedReason.includes('glätte') ||
        normalizedReason.includes('frost')
    ) {
        return {
            ...common,
            icon: '🧊',
            label: reason || 'MAX 30 · Glättegefahr',
        }
    }

    if (
        factor === 'STORM_CAP' ||
        normalizedReason.includes('sturm')
    ) {
        return {
            ...common,
            icon: '🌪️',
            label: reason || 'MAX 40 · Sturm',
        }
    }

    if (
        factor === 'WET_ROAD' ||
        (
            wetRoadIndex >= 0.5 &&
            normalizedReason.includes('nass')
        )
    ) {
        return {
            ...common,
            icon: '🏍️',
            label:
                reason ||
                `Score ${score} · Nasse Fahrbahn (${Math.round(wetRoadIndex * 100)}%)`,
        }
    }

    if (
        factor === 'PRECIP' ||
        rain > 0.5 ||
        normalizedReason.includes('schauer') ||
        normalizedReason.includes('regen')
    ) {
        return {
            ...common,
            icon: '🌧️',
            label:
                reason ||
                `Score ${score} · Schauer ${rain.toFixed(1)} mm/h`,
        }
    }

    if (
        factor === 'WIND' ||
        normalizedReason.includes('wind')
    ) {
        return {
            ...common,
            icon: '💨',
            label:
                reason ||
                `Score ${score} · Gegenwind ${Math.round(wind)} km/h`,
        }
    }

    return {
        ...common,
        icon: '⚠️',
        label: reason || `Score ${score}`,
    }
}

function createHoverMarkerIcon(score: number) {
    const color = getScoreColor(score)

    return divIcon({
        className: 'route-map__hover-marker',
        html: `
            <div
                class="route-map__hover-crosshair"
                style="--marker-color: ${color};"
                aria-hidden="true"
            >
                <div class="route-map__hover-pulse"></div>
                <div class="route-map__hover-core"></div>
            </div>
        `,
        iconSize: [28, 28],
        iconAnchor: [14, 14],
    })
}

function createBottleneckIcon(badge: BottleneckBadge) {
    const label = escapeHtml(badge.label)
    const icon = escapeHtml(badge.icon)

    return divIcon({
        className: 'route-map__bottleneck-wrapper',
        html: `
            <div
                class="route-map__bottleneck-pill"
                style="--pill-color: ${badge.color};"
                title="${label}"
            >
                <span
                    class="route-map__bottleneck-icon"
                    aria-hidden="true"
                >${icon}</span>
                <span class="route-map__bottleneck-text">${label}</span>
            </div>
        `,
        iconAnchor: [0, 14],
    })
}

interface MapViewportUpdaterProps {
    startPlace: Place | null
    destinationPlace: Place | null
    routePositions: LatLng[]
}

function MapViewportUpdater({
                                startPlace,
                                destinationPlace,
                                routePositions,
                            }: MapViewportUpdaterProps) {
    const map = useMap()

    useEffect(() => {
        if (routePositions.length >= 2) {
            map.fitBounds(latLngBounds(routePositions), {
                padding: [50, 50],
                maxZoom: 16,
            })
            return
        }

        if (startPlace && destinationPlace) {
            const bounds = latLngBounds([
                [
                    startPlace.latitude,
                    startPlace.longitude,
                ],
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

        const selectedPlace =
            startPlace ?? destinationPlace

        if (selectedPlace) {
            map.setView(
                [
                    selectedPlace.latitude,
                    selectedPlace.longitude,
                ],
                14,
            )
        }
    }, [
        map,
        startPlace,
        destinationPlace,
        routePositions,
    ])

    return null
}

export default function RouteMap({
                                     startPlace,
                                     destinationPlace,
                                     routeSegments = NO_SEGMENTS,
                                     geometry,
                                     weatherSegments,
                                     hoveredSegmentIndex,
                                     onHoverSegment,
                                 }: RouteMapProps) {
    const [hasError, setHasError] = useState(false)

    const processedWeatherSegments =
        useMemo<ProcessedWeatherSegment[]>(() => {
            if (!weatherSegments?.length) {
                return []
            }

            return [...weatherSegments]
                .sort(
                    (first, second) =>
                        first.segmentIndex -
                        second.segmentIndex,
                )
                .map((segment) => {
                    const positions = segment.geometry
                        ? toLeafletRoutePositions(
                            segment.geometry,
                        )
                        : []

                    return {
                        ...segment,
                        positions,
                        midpoint:
                            positions[
                                Math.floor(
                                    positions.length / 2,
                                )
                                ] ?? null,
                        color: getScoreColor(segment.score),
                    }
                })
                .filter(
                    (segment) =>
                        segment.positions.length >= 2,
                )
        }, [weatherSegments])

    const fallbackSegments =
        useMemo<RouteMapSegment[]>(() => {
            if (processedWeatherSegments.length > 0) {
                return NO_SEGMENTS
            }

            if (routeSegments.length > 0) {
                return routeSegments
            }

            if (
                geometry?.coordinates &&
                geometry.coordinates.length >= 2
            ) {
                const positions =
                    toLeafletRoutePositions(geometry)

                if (positions.length >= 2) {
                    return [
                        {
                            positions,
                            color: 'var(--color-route-line)',
                        },
                    ]
                }
            }

            return NO_SEGMENTS
        }, [
            processedWeatherSegments,
            routeSegments,
            geometry,
        ])

    const allRoutePositions = useMemo<LatLng[]>(() => {
        if (
            geometry?.coordinates &&
            geometry.coordinates.length >= 2
        ) {
            return toLeafletRoutePositions(geometry)
        }

        if (processedWeatherSegments.length > 0) {
            return processedWeatherSegments.flatMap(
                (segment) => segment.positions,
            )
        }

        return fallbackSegments.flatMap(
            (segment) => segment.positions,
        )
    }, [
        geometry,
        processedWeatherSegments,
        fallbackSegments,
    ])

    const bottleneckBadges = useMemo<BottleneckBadge[]>(() => {
        const badges: BottleneckBadge[] = []
        let lastBadgeLabel = ''

        for (const segment of processedWeatherSegments) {
            if (!segment.midpoint) {
                continue
            }

            const badge = resolveBottleneckBadge(
                segment,
                segment.midpoint,
            )

            if (
                !badge ||
                (
                    badge.label === lastBadgeLabel &&
                    badges.length > 0
                )
            ) {
                continue
            }

            badges.push(badge)
            lastBadgeLabel = badge.label
        }

        return badges
    }, [processedWeatherSegments])

    const activeHoveredSegment = useMemo(() => {
        if (
            hoveredSegmentIndex === null ||
            hoveredSegmentIndex === undefined
        ) {
            return null
        }

        return (
            processedWeatherSegments.find(
                (segment) =>
                    segment.segmentIndex ===
                    hoveredSegmentIndex,
            ) ?? null
        )
    }, [
        hoveredSegmentIndex,
        processedWeatherSegments,
    ])

    const apiKey =
        import.meta.env.VITE_MAPTILER_PUBLIC_API_KEY ||
        import.meta.env.VITE_MAPTILER_API_KEY

    const hasSelectedPlaces =
        startPlace !== null || destinationPlace !== null
    const hasRoute = allRoutePositions.length >= 2

    if (!apiKey) {
        return (
            <div className="map-error" role="alert">
                <span
                    className="map-error__icon"
                    aria-hidden="true"
                >
                    !
                </span>

                <div>
                    <strong>Karte nicht konfiguriert</strong>
                    <p>
                        Für die Kartendarstellung fehlt der
                        öffentliche MapTiler-API-Key.
                    </p>
                </div>
            </div>
        )
    }

    return (
        <section
            className="route-map"
            aria-label="Karte des Routenplaners"
        >
            <div className="route-map__header">
                <div>
                    <p className="route-map__eyebrow">
                        Kartendarstellung
                    </p>
                    <h2 className="route-map__title">
                        Routenübersicht
                    </h2>
                </div>

                <div
                    className="route-map__legend"
                    aria-label="Kartenlegende"
                >
                    <span className="route-map__legend-item">
                        <span
                            className="route-map__legend-marker route-map__legend-marker--start"
                            aria-hidden="true"
                        >
                            S
                        </span>
                        Start
                    </span>

                    <span className="route-map__legend-item">
                        <span
                            className="route-map__legend-marker route-map__legend-marker--destination"
                            aria-hidden="true"
                        >
                            Z
                        </span>
                        Ziel
                    </span>
                </div>
            </div>

            <div className="route-map__viewport">
                {hasError && (
                    <div
                        className="map-error map-error--overlay"
                        role="alert"
                    >
                        <span
                            className="map-error__icon"
                            aria-hidden="true"
                        >
                            !
                        </span>

                        <div>
                            <strong>Kartenfehler</strong>
                            <p>
                                Die Karte konnte nicht
                                vollständig geladen werden.
                            </p>
                        </div>
                    </div>
                )}

                {!hasSelectedPlaces && !hasRoute && (
                    <div
                        className="route-map__empty-hint"
                        role="status"
                    >
                        <span
                            className="route-map__empty-icon"
                            aria-hidden="true"
                        >
                            ⌖
                        </span>
                        <span>
                            Wähle Start und Ziel aus, um die
                            Route auf der Karte anzuzeigen.
                        </span>
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
                            tileload: () => {
                                setHasError(false)
                            },
                            tileerror: () => {
                                setHasError(true)
                            },
                        }}
                    />

                    <MapViewportUpdater
                        startPlace={startPlace}
                        destinationPlace={destinationPlace}
                        routePositions={allRoutePositions}
                    />

                    {allRoutePositions.length >= 2 && (
                        <Polyline
                            positions={allRoutePositions}
                            pathOptions={{
                                color:
                                    'var(--color-route-line-outline)',
                                weight: 10,
                                opacity: 0.95,
                                lineCap: 'round',
                                lineJoin: 'round',
                            }}
                        />
                    )}

                    {processedWeatherSegments.map((segment) => {
                        const isHovered =
                            hoveredSegmentIndex ===
                            segment.segmentIndex

                        return (
                            <Polyline
                                key={segment.segmentIndex}
                                positions={segment.positions}
                                pathOptions={{
                                    color: segment.color,
                                    weight: isHovered ? 9 : 6,
                                    opacity: isHovered ? 1 : 0.95,
                                    lineCap: 'round',
                                    lineJoin: 'round',
                                }}
                                eventHandlers={{
                                    mouseover: () => {
                                        onHoverSegment?.(
                                            segment.segmentIndex,
                                        )
                                    },
                                    mouseout: () => {
                                        onHoverSegment?.(null)
                                    },
                                    click: () => {
                                        onHoverSegment?.(
                                            segment.segmentIndex,
                                        )
                                    },
                                }}
                            />
                        )
                    })}

                    {processedWeatherSegments.length === 0 &&
                        fallbackSegments.map(
                            (segment, index) => (
                                <Polyline
                                    key={`${index}-${segment.color}`}
                                    positions={segment.positions}
                                    pathOptions={{
                                        color: segment.color,
                                        weight: 6,
                                        opacity: 0.9,
                                        lineCap: 'round',
                                        lineJoin: 'round',
                                    }}
                                />
                            ),
                        )}

                    {bottleneckBadges.map((badge) => (
                        <Marker
                            key={`bottleneck-${badge.segmentIndex}`}
                            position={badge.position}
                            icon={createBottleneckIcon(badge)}
                            zIndexOffset={400}
                            title={badge.label}
                            alt={badge.label}
                            eventHandlers={{
                                mouseover: () => {
                                    onHoverSegment?.(
                                        badge.segmentIndex,
                                    )
                                },
                                mouseout: () => {
                                    onHoverSegment?.(null)
                                },
                                click: () => {
                                    onHoverSegment?.(
                                        badge.segmentIndex,
                                    )
                                },
                            }}
                        />
                    ))}

                    {activeHoveredSegment?.midpoint && (
                        <Marker
                            position={
                                activeHoveredSegment.midpoint
                            }
                            icon={createHoverMarkerIcon(
                                activeHoveredSegment.score,
                            )}
                            zIndexOffset={1000}
                            interactive={false}
                        >
                            <Tooltip
                                permanent
                                direction="top"
                                offset={[0, -14]}
                                className="route-map__diagnostic-tooltip"
                            >
                                <div className="route-map__tooltip-container">
                                    <div className="route-map__tooltip-header">
                                        <strong>
                                            Segment{' '}
                                            {activeHoveredSegment.segmentIndex +
                                                1}
                                        </strong>

                                        {activeHoveredSegment.distanceFromStartMeters !=
                                            null && (
                                                <span>
                                                ·{' '}
                                                    {(
                                                        activeHoveredSegment.distanceFromStartMeters /
                                                        1000
                                                    ).toFixed(1)}{' '}
                                                    km
                                            </span>
                                            )}

                                        <span
                                            className="route-map__tooltip-score-pill"
                                            style={{
                                                backgroundColor:
                                                    getScoreColor(
                                                        activeHoveredSegment.score,
                                                    ),
                                            }}
                                        >
                                            Score{' '}
                                            {activeHoveredSegment.score}
                                        </span>
                                    </div>

                                    {activeHoveredSegment.reasonLabel && (
                                        <div className="route-map__tooltip-reason">
                                            ⚠️{' '}
                                            {
                                                activeHoveredSegment.reasonLabel
                                            }
                                        </div>
                                    )}

                                    <div className="route-map__tooltip-details">
                                        {activeHoveredSegment.weather
                                            ?.windSpeedKmh != null && (
                                            <div>
                                                Wind:{' '}
                                                {Math.round(
                                                    activeHoveredSegment
                                                        .weather
                                                        .windSpeedKmh,
                                                )}{' '}
                                                km/h
                                                {activeHoveredSegment
                                                    .weather
                                                    .windGustsKmh !=
                                                null
                                                    ? ` (Böen ${Math.round(
                                                        activeHoveredSegment
                                                            .weather
                                                            .windGustsKmh,
                                                    )} km/h)`
                                                    : ''}
                                            </div>
                                        )}

                                        {activeHoveredSegment.weather
                                            ?.precipitationMm != null && (
                                            <div>
                                                Regen:{' '}
                                                {activeHoveredSegment.weather.precipitationMm.toFixed(
                                                    1,
                                                )}{' '}
                                                mm/h
                                                {activeHoveredSegment
                                                    .weather
                                                    .precipitationProbabilityPercent !=
                                                null
                                                    ? ` (${activeHoveredSegment.weather.precipitationProbabilityPercent}%)`
                                                    : ''}
                                            </div>
                                        )}

                                        {activeHoveredSegment.weather
                                                ?.wetRoadIndex != null &&
                                            activeHoveredSegment.weather
                                                .wetRoadIndex > 0 && (
                                                <div>
                                                    Fahrbahn:{' '}
                                                    {Math.round(
                                                        activeHoveredSegment
                                                            .weather
                                                            .wetRoadIndex *
                                                        100,
                                                    )}
                                                    % Nässe
                                                </div>
                                            )}

                                        {activeHoveredSegment.weather
                                                ?.temperatureCelsius !=
                                            null && (
                                                <div>
                                                    Temperatur:{' '}
                                                    {Math.round(
                                                        activeHoveredSegment
                                                            .weather
                                                            .temperatureCelsius,
                                                    )}
                                                    °C
                                                    {activeHoveredSegment
                                                        .weather
                                                        .apparentTemperatureCelsius !=
                                                    null
                                                        ? ` (Gefühlt ${Math.round(
                                                            activeHoveredSegment
                                                                .weather
                                                                .apparentTemperatureCelsius,
                                                        )}°C)`
                                                        : ''}
                                                </div>
                                            )}
                                    </div>
                                </div>
                            </Tooltip>
                        </Marker>
                    )}

                    {startPlace && (
                        <Marker
                            position={[
                                startPlace.latitude,
                                startPlace.longitude,
                            ]}
                            icon={startIcon}
                            title={`Startpunkt: ${startPlace.displayName}`}
                            alt={`Startpunkt: ${startPlace.displayName}`}
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
                            title={`Zielpunkt: ${destinationPlace.displayName}`}
                            alt={`Zielpunkt: ${destinationPlace.displayName}`}
                        >
                            <Popup>
                                <strong>Zielpunkt</strong>
                                <br />
                                {destinationPlace.displayName}
                            </Popup>
                        </Marker>
                    )}
                </MapContainer>
            </div>
        </section>
    )
}
