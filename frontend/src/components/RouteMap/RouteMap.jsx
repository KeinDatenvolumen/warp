import { useState } from "react";
import { MapContainer, TileLayer } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "./RouteMap.css";

const MAP_CENTER = [49.49, 9.54];

export default function RouteMap() {
  const [hasError, setHasError] = useState(false);
  const apiKey = import.meta.env.VITE_MAPTILER_API_KEY;

  if (!apiKey) {
    return (
      <div className="map-error" role="alert">
        Die Karte ist momentan nicht konfiguriert.
      </div>
    );
  }

  return (
    <section className="route-map" aria-label="Karte des Routenplaners">
      {hasError && (
        <div className="map-error map-error--overlay" role="alert">
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
          url={`https://api.maptiler.com/maps/streets-v4/{z}/{x}/{y}.png?key=${apiKey}`}
          tileSize={512}
          zoomOffset={-1}
          minZoom={1}
          crossOrigin
          attribution={
            '&copy; https://www.maptiler.com/copyright/MapTiler</a> ' +
            '&copy; https://www.openstreetmap.org/copyrightOpenStreetMap contributors</a>'
          }
          eventHandlers={{
            tileerror: () => setHasError(true),
            load: () => setHasError(false),
          }}
        />
      </MapContainer>
    </section>
  );
}