import {useState} from 'react'
import {MapContainer, TileLayer} from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import './RouteMap.css'

const MAP_CENTER = [49.354111, 9.150306]

export default function RouteMap() {
    const [hasError, setHasError] = useState(false)
    const apiKey = import.meta.env.VITE_MAPTILER_PUBLIC_API_KEY

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
            </MapContainer>
        </section>
    )
}